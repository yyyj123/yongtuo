"""Validate the exact Phase 1 CI workflow contract."""

import argparse
import json
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlparse

import yaml


DEFAULT_WORKFLOW_PATH = Path(".github/workflows/verify.yml")
REQUIRED_JOBS = {"api", "web", "admin"}
REQUIRED_WORKFLOW_KEYS = {"name", "permissions", "jobs"}
REQUIRED_JOB_KEYS = {"name", "runs-on", "timeout-minutes", "steps"}
REQUIRED_JOB_NAMES = {
    "api": "API tests",
    "web": "Web tests and build",
    "admin": "Admin tests and build",
}
REQUIRED_NODE_VERSION = "24.20.0"
REQUIRED_JAVA_VERSION = "21"
DEFAULT_WEB_LOCK_PATH = Path("apps/web/package-lock.json")
DEFAULT_ADMIN_LOCK_PATH = Path("apps/admin/package-lock.json")
CHECKOUT_ACTION = "actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683"
SETUP_JAVA_ACTION = "actions/setup-java@c5195efecf7bdfc987ee8bae7a71cb8b11521c00"
SETUP_NODE_ACTION = "actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020"
SETUP_PYTHON_ACTION = "actions/setup-python@a26af69be951a213d495a4c3e4e4022e16d87065"


def fail(message: str) -> None:
    print(f"CI workflow contract failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def step_matches(step: object, **expected: object) -> bool:
    return isinstance(step, dict) and all(step.get(key) == value for key, value in expected.items())


def allowed_to_fail(step: dict) -> bool:
    return "continue-on-error" in step and step["continue-on-error"] is not False


def require_exact_steps(job_name: str, steps: object, requirements: list[tuple[str, dict]]) -> None:
    if not isinstance(steps, list):
        fail(f"{job_name} job must define steps")

    indexes: list[int] = []
    for label, expected in requirements:
        matching_indexes = [index for index, step in enumerate(steps) if step_matches(step, **expected)]
        if not matching_indexes:
            fail(f"{job_name} missing {label} step")
        index = matching_indexes[0]
        step = steps[index]
        if "if" in step:
            fail(f"{job_name} required step {expected.get('run', label)!r} must not have an if condition")
        if allowed_to_fail(step):
            fail(f"{job_name} required step {expected.get('run', label)!r} must not continue on error")
        indexes.append(index)

    if indexes != sorted(indexes) or len(indexes) != len(set(indexes)):
        fail(f"{job_name} required steps are out of order")

    if len(steps) != len(requirements):
        fail(f"{job_name} steps must exactly match the Phase 1 allowlist")

    for index, (step, (_label, expected)) in enumerate(zip(steps, requirements), start=1):
        if not isinstance(step, dict):
            fail(f"{job_name} step {index} does not match the Phase 1 allowlist")
        actual = {key: value for key, value in step.items() if key != "name"}
        if actual != expected:
            fail(f"{job_name} step {index} does not match the Phase 1 allowlist")


def require_wrapper_executable() -> None:
    result = subprocess.run(
        ["git", "ls-files", "-s", "apps/api/mvnw"],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0 or not result.stdout.startswith("100755 "):
        fail("apps/api/mvnw must be tracked with executable mode 100755")


def require_job_basics(name: str, job: object) -> dict:
    if not isinstance(job, dict):
        fail(f"{name} job must be a mapping")
    if set(job) != REQUIRED_JOB_KEYS:
        fail(f"{name} job keys must exactly match the Phase 1 allowlist")
    if job.get("name") != REQUIRED_JOB_NAMES[name]:
        fail(f"{name} job name must be {REQUIRED_JOB_NAMES[name]!r}")
    if job.get("runs-on") != "ubuntu-24.04":
        fail(f"{name} job must use ubuntu-24.04")
    if job.get("timeout-minutes") != 15:
        fail(f"{name} job timeout must be 15 minutes")
    return job


def require_official_lock_registry(name: str, lock_path: Path) -> None:
    if not lock_path.is_file():
        fail(f"missing {name} package-lock file: {lock_path}")
    try:
        lock = json.loads(lock_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(f"invalid {name} package-lock file: {error}")

    def inspect(value: object) -> None:
        if isinstance(value, dict):
            resolved = value.get("resolved")
            if resolved is not None:
                if not isinstance(resolved, str) or urlparse(resolved).hostname != "registry.npmjs.org":
                    fail(f"{name} package-lock resolved URL must use registry.npmjs.org: {resolved!r}")
            for child in value.values():
                inspect(child)
        elif isinstance(value, list):
            for child in value:
                inspect(child)

    inspect(lock)


def validate(workflow_path: Path, web_lock_path: Path, admin_lock_path: Path) -> None:
    if not workflow_path.is_file():
        fail(f"missing {workflow_path}")

    with workflow_path.open(encoding="utf-8") as workflow_file:
        workflow = yaml.safe_load(workflow_file)

    if not isinstance(workflow, dict):
        fail("workflow root must be a mapping")

    trigger_keys = [key for key in ("on", True) if key in workflow]
    if len(trigger_keys) != 1 or set(workflow) != REQUIRED_WORKFLOW_KEYS | {trigger_keys[0]}:
        fail("workflow keys must exactly match the Phase 1 allowlist")
    if workflow.get("name") != "CI":
        fail("workflow name must be 'CI'")
    triggers = workflow[trigger_keys[0]]
    if triggers != {"push": None, "pull_request": None}:
        fail("workflow triggers must be unfiltered push and pull_request events")
    if workflow.get("permissions") != {"contents": "read"}:
        fail("workflow must grant only contents: read")

    jobs = workflow.get("jobs")
    if not isinstance(jobs, dict) or set(jobs) != REQUIRED_JOBS:
        fail("workflow jobs must be exactly api, web, and admin")

    api = require_job_basics("api", jobs["api"])
    require_exact_steps(
        "api",
        api.get("steps"),
        [
            ("checkout", {"uses": CHECKOUT_ACTION}),
            ("Python setup", {"uses": SETUP_PYTHON_ACTION, "with": {"python-version": "3.13.1"}}),
            ("PyYAML install", {"run": "python -m pip install --disable-pip-version-check PyYAML==6.0.3"}),
            ("CI workflow verifier", {"run": "python infra/scripts/verify-ci-workflow.py"}),
            ("CI workflow negative checks", {"run": "python infra/scripts/verify-ci-workflow-test.py"}),
            (
                "Java setup",
                {
                    "uses": SETUP_JAVA_ACTION,
                    "with": {
                        "distribution": "temurin",
                        "java-version": REQUIRED_JAVA_VERSION,
                        "cache": "maven",
                    },
                },
            ),
            ("Docker daemon check", {"run": "docker info"}),
            ("API tests", {"working-directory": "apps/api", "run": "./mvnw test"}),
        ],
    )

    for name in ("web", "admin"):
        job = require_job_basics(name, jobs[name])
        require_exact_steps(
            name,
            job.get("steps"),
            [
                ("checkout", {"uses": CHECKOUT_ACTION}),
                (
                    "Node setup",
                    {
                        "uses": SETUP_NODE_ACTION,
                        "with": {
                            "node-version": REQUIRED_NODE_VERSION,
                            "cache": "npm",
                            "cache-dependency-path": f"apps/{name}/package-lock.json",
                        },
                    },
                ),
                ("dependency install", {"working-directory": f"apps/{name}", "run": "npm ci"}),
                ("tests", {"working-directory": f"apps/{name}", "run": "npm test -- --run"}),
                ("build", {"working-directory": f"apps/{name}", "run": "npm run build"}),
            ],
        )

    require_wrapper_executable()
    require_official_lock_registry("web", web_lock_path)
    require_official_lock_registry("admin", admin_lock_path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workflow", type=Path, default=DEFAULT_WORKFLOW_PATH)
    parser.add_argument("--web-lock", type=Path, default=DEFAULT_WEB_LOCK_PATH)
    parser.add_argument("--admin-lock", type=Path, default=DEFAULT_ADMIN_LOCK_PATH)
    arguments = parser.parse_args()
    validate(arguments.workflow, arguments.web_lock, arguments.admin_lock)
    print("CI workflow contract ok")


if __name__ == "__main__":
    main()
