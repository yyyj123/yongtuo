#!/usr/bin/env bash
set -euo pipefail

base_url="${LOCAL_SITE_URL:-http://localhost}"
timeout_seconds="${SMOKE_TIMEOUT_SECONDS:-60}"
retry_delay_seconds="${SMOKE_RETRY_DELAY_SECONDS:-2}"
deadline=$((SECONDS + timeout_seconds))
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="${COMPOSE_FILE:-${repo_root}/docker-compose.local.yml}"
recreate_upstream=""
reservation_name=""

if (( $# > 0 )); then
  if [[ "$1" != '--recreate-upstream' || $# -ne 2 ]]; then
    printf 'Usage: %s [--recreate-upstream web|admin|api]\n' "$0" >&2
    exit 2
  fi
  recreate_upstream="$2"
  case "${recreate_upstream}" in
    web|admin|api) ;;
    *)
      printf 'Unsupported upstream for recreation: %s\n' "${recreate_upstream}" >&2
      exit 2
      ;;
  esac
fi

wait_for_text() {
  local label="$1"
  local url="$2"
  local expected="$3"
  local body

  while (( SECONDS < deadline )); do
    if body="$(curl --connect-timeout 2 --max-time 5 -fsS "${url}" 2>/dev/null)" \
      && [[ "${body}" == *"${expected}"* ]]; then
      return 0
    fi
    sleep "${retry_delay_seconds}"
  done

  printf 'Timed out after %ss waiting for %s at %s\n' \
    "${timeout_seconds}" "${label}" "${url}" >&2
  return 1
}

content_type_from_headers() {
  awk 'BEGIN { IGNORECASE=1 }
       /^Content-Type:/ {
         sub(/\r$/, "")
         sub(/^[^:]*:[[:space:]]*/, "")
         value=$0
       }
       END { print value }' "$1"
}

require_text() {
  local label="$1"
  local body="$2"
  local expected="$3"

  if [[ "${body}" != *"${expected}"* ]]; then
    printf '%s response did not contain required text: %s\n' \
      "${label}" "${expected}" >&2
    return 1
  fi
}

remove_ip_reservation() {
  if [[ -n "${reservation_name}" ]]; then
    docker rm -f "${reservation_name}" >/dev/null 2>&1 || true
    reservation_name=""
  fi
}

verify_recreated_upstream() {
  local service="$1"
  local route expected nginx_id_before nginx_id_after nginx_pid_before nginx_pid_after
  local upstream_id_before upstream_id_after upstream_ip_before upstream_ip_after network_id
  reservation_name="phase1-upstream-ip-reservation-${service}-$$"

  case "${service}" in
    web)
      route='/'
      expected='YONGTUO'
      ;;
    admin)
      route='/manage/'
      expected='YONGTUO 管理端'
      ;;
    api)
      route='/api/v1/public/site-health'
      expected='"status":"UP"'
      ;;
  esac

  nginx_id_before="$(docker compose -f "${compose_file}" ps -q nginx)"
  upstream_id_before="$(docker compose -f "${compose_file}" ps -q "${service}")"
  if [[ -z "${nginx_id_before}" || -z "${upstream_id_before}" ]]; then
    printf 'Nginx and %s must be running before recreation\n' "${service}" >&2
    return 1
  fi
  nginx_pid_before="$(docker inspect --format '{{.State.Pid}}' "${nginx_id_before}")"
  upstream_ip_before="$(docker inspect --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "${upstream_id_before}")"
  network_id="$(docker inspect --format '{{range .NetworkSettings.Networks}}{{.NetworkID}}{{end}}' "${upstream_id_before}")"
  if [[ -z "${upstream_ip_before}" || -z "${network_id}" ]]; then
    printf 'Could not determine %s container network identity\n' "${service}" >&2
    return 1
  fi

  # Reserve the old address while Compose performs exactly one upstream
  # recreation. This prevents Docker from assigning the same address again,
  # making the stale-DNS regression deterministic.
  docker compose -f "${compose_file}" stop "${service}"
  docker compose -f "${compose_file}" rm -f "${service}"
  docker run -d --name "${reservation_name}" --network "${network_id}" \
    --ip "${upstream_ip_before}" nginx:1.27.4-alpine sh -c 'sleep 120' >/dev/null
  if ! docker compose -f "${compose_file}" up -d --no-deps --force-recreate "${service}"; then
    remove_ip_reservation
    return 1
  fi

  upstream_id_after="$(docker compose -f "${compose_file}" ps -q "${service}")"
  upstream_ip_after="$(docker inspect --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "${upstream_id_after}")"
  nginx_id_after="$(docker compose -f "${compose_file}" ps -q nginx)"
  nginx_pid_after="$(docker inspect --format '{{.State.Pid}}' "${nginx_id_after}")"

  if [[ "${upstream_id_before}" == "${upstream_id_after}" ]]; then
    printf '%s container was not recreated\n' "${service}" >&2
    remove_ip_reservation
    return 1
  fi
  if [[ "${upstream_ip_before}" == "${upstream_ip_after}" ]]; then
    printf '%s container kept stale-test address %s\n' "${service}" "${upstream_ip_after}" >&2
    remove_ip_reservation
    return 1
  fi
  if [[ "${nginx_id_before}" != "${nginx_id_after}" || "${nginx_pid_before}" != "${nginx_pid_after}" ]]; then
    printf 'Nginx changed while recreating %s (container/PID %s/%s -> %s/%s)\n' \
      "${service}" "${nginx_id_before}" "${nginx_pid_before}" \
      "${nginx_id_after}" "${nginx_pid_after}" >&2
    remove_ip_reservation
    return 1
  fi

  deadline=$((SECONDS + timeout_seconds))
  if ! wait_for_text "${service} after force-recreate" "${base_url}${route}" "${expected}"; then
    remove_ip_reservation
    return 1
  fi
  remove_ip_reservation
  printf 'Dynamic upstream regression ok: recreated %s (%s -> %s); Nginx container/PID stayed %s/%s\n' \
    "${service}" "${upstream_ip_before}" "${upstream_ip_after}" \
    "${nginx_id_after}" "${nginx_pid_after}"
}

wait_for_text 'Nuxt website' "${base_url}/" 'YONGTUO'
wait_for_text 'public API' "${base_url}/api/v1/public/site-health" '"status":"UP"'
wait_for_text 'admin application' "${base_url}/manage/" 'YONGTUO 管理端'

web_html="$(curl --connect-timeout 2 --max-time 5 -fsS "${base_url}/")"
require_text 'Nuxt website' "${web_html}" '勇拓五金实业'
require_text 'Nuxt website' "${web_html}" 'YONGTUO'

api_body="$(curl --connect-timeout 2 --max-time 5 -fsS \
  "${base_url}/api/v1/public/site-health")"
require_text 'public API' "${api_body}" '"code":0'
require_text 'public API' "${api_body}" '"status":"UP"'

manage_html="$(curl --connect-timeout 2 --max-time 5 -fsS "${base_url}/manage/")"
require_text 'admin application' "${manage_html}" 'YONGTUO 管理端'

manage_asset_pattern='src="(/manage/assets/[^"]+\.js)"'
if [[ "${manage_html}" =~ ${manage_asset_pattern} ]]; then
  manage_asset_path="${BASH_REMATCH[1]}"
else
  printf 'Admin HTML did not contain a /manage/assets/*.js entry\n' >&2
  exit 1
fi

temp_dir="$(mktemp -d)"
cleanup() {
  remove_ip_reservation
  rm -rf "${temp_dir}"
}
trap cleanup EXIT

asset_status="$(curl --connect-timeout 2 --max-time 5 -sS \
  -D "${temp_dir}/asset.headers" -o "${temp_dir}/asset.body" \
  -w '%{http_code}' "${base_url}${manage_asset_path}")"
asset_content_type="$(content_type_from_headers "${temp_dir}/asset.headers")"
if [[ "${asset_status}" != '200' ]]; then
  printf 'Admin asset returned HTTP %s instead of 200: %s\n' \
    "${asset_status}" "${manage_asset_path}" >&2
  exit 1
fi
if [[ "${asset_content_type}" != *javascript* ]]; then
  printf 'Admin asset returned non-JavaScript Content-Type: %s\n' \
    "${asset_content_type}" >&2
  exit 1
fi

missing_status="$(curl --connect-timeout 2 --max-time 5 -sS \
  -D "${temp_dir}/missing.headers" -o "${temp_dir}/missing.body" \
  -w '%{http_code}' "${base_url}/manage/assets/definitely-missing.js")"
if [[ "${missing_status}" != '404' ]]; then
  printf 'Missing admin asset returned HTTP %s instead of 404\n' \
    "${missing_status}" >&2
  exit 1
fi
if grep -Fq 'YONGTUO 管理端' "${temp_dir}/missing.body"; then
  printf 'Missing admin asset incorrectly returned the SPA HTML\n' >&2
  exit 1
fi

manage_login_html="$(curl --connect-timeout 2 --max-time 5 -fsS \
  "${base_url}/manage/login")"
require_text 'admin deep link' "${manage_login_html}" 'YONGTUO 管理端'

if [[ -n "${recreate_upstream}" ]]; then
  verify_recreated_upstream "${recreate_upstream}"
fi
