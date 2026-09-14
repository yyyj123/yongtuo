"""Exercise focused negative cases for the Phase 1 CI workflow contract."""

from copy import deepcopy
import json
from pathlib import Path
import subprocess
import sys
import tempfile

import yaml


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW_PATH = ROOT / ".github/workflows/verify.yml"
VERIFIER_PATH = ROOT / "infra/scripts/verify-ci-workflow.py"
FAILURES: list[str] = []


def write_official_lock_fixture(destination: Path) -> None:
    lock = {
        "name": "ci-verifier-fixture",
        "lockfileVersion": 3,
        "packages": {
            "": {"name": "ci-verifier-fixture"},
            "node_modules/example": {
                "resolved": "https://registry.npmjs.org/example/-/example-1.0.0.tgz"
            },
        },
    }
    destination.write_text(json.dumps(lock), encoding="utf-8")


def inject_mirror_resolution(web_lock: Path, _admin_lock: Path) -> None:
    lock = json.loads(web_lock.read_text(encoding="utf-8"))
    lock["packages"]["node_modules/example"]["resolved"] = (
        "https://registry.npmmirror.com/example/-/example-1.0.0.tgz"
    )
    web_lock.write_text(json.dumps(lock), encoding="utf-8")


def run_case(name: str, mutate, expected_message: str, mutate_lock=None) -> None:
    with WORKFLOW_PATH.open(encoding="utf-8") as workflow_file:
        workflow = yaml.safe_load(workflow_file)
    mutated = mutate(deepcopy(workflow))

    with tempfile.TemporaryDirectory() as temporary_directory:
        temporary_path = Path(temporary_directory)
        candidate_path = temporary_path / "workflow.yml"
        web_lock_path = temporary_path / "web-package-lock.json"
        admin_lock_path = temporary_path / "admin-package-lock.json"
        with candidate_path.open("w", encoding="utf-8") as candidate_file:
            yaml.safe_dump(mutated, candidate_file, sort_keys=False)
        write_official_lock_fixture(web_lock_path)
        write_official_lock_fixture(admin_lock_path)
        if mutate_lock is not None:
            mutate_lock(web_lock_path, admin_lock_path)
        result = subprocess.run(
            [
                sys.executable,
                str(VERIFIER_PATH),
                "--workflow",
                str(candidate_path),
                "--web-lock",
                str(web_lock_path),
                "--admin-lock",
                str(admin_lock_path),
            ],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )

    output = result.stdout + result.stderr
    if result.returncode == 0 or expected_message not in output:
        FAILURES.append(f"{name} was not rejected as expected: {output.strip()}")


def step(workflow: dict, job: str, run: str) -> dict:
    return next(item for item in workflow["jobs"][job]["steps"] if item.get("run") == run)


def triggers(workflow: dict) -> dict:
    return workflow.get("on", workflow.get(True))


def main() -> None:
    run_case(
        "filtered pull request trigger",
        lambda workflow: triggers(workflow).update(
            {"pull_request": {"paths-ignore": ["**"]}}
        )
        or workflow,
        "workflow triggers must be unfiltered push and pull_request events",
    )
    run_case(
        "filtered push trigger",
        lambda workflow: triggers(workflow).update(
            {"push": {"branches-ignore": ["**"]}}
        )
        or workflow,
        "workflow triggers must be unfiltered push and pull_request events",
    )
    run_case(
        "workflow test-skipping environment",
        lambda workflow: workflow.update(
            {"env": {"MAVEN_OPTS": "-Dmaven.test.skip=true"}}
        )
        or workflow,
        "workflow keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "job test-skipping environment",
        lambda workflow: workflow["jobs"]["api"].update(
            {"env": {"MAVEN_OPTS": "-Dmaven.test.skip=true"}}
        )
        or workflow,
        "api job keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "workflow custom shell",
        lambda workflow: workflow.update(
            {"defaults": {"run": {"shell": "bash {0}"}}}
        )
        or workflow,
        "workflow keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "job custom shell",
        lambda workflow: workflow["jobs"]["web"].update(
            {"defaults": {"run": {"shell": "bash {0}"}}}
        )
        or workflow,
        "web job keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "empty job matrix",
        lambda workflow: workflow["jobs"]["admin"].update(
            {"strategy": {"matrix": {"include": []}}}
        )
        or workflow,
        "admin job keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "extra run step in middle",
        lambda workflow: workflow["jobs"]["web"]["steps"].insert(
            2, {"name": "Publish", "run": "npm publish"}
        )
        or workflow,
        "web steps must exactly match the Phase 1 allowlist",
    )
    run_case(
        "extra action step at end",
        lambda workflow: workflow["jobs"]["admin"]["steps"].append(
            {"name": "Upload artifact", "uses": "actions/upload-artifact@v4"}
        )
        or workflow,
        "admin steps must exactly match the Phase 1 allowlist",
    )
    run_case(
        "extra deploy step in API job",
        lambda workflow: workflow["jobs"]["api"]["steps"].append(
            {"name": "Deploy", "run": "./deploy.sh"}
        )
        or workflow,
        "api steps must exactly match the Phase 1 allowlist",
    )
    run_case(
        "unexpected job",
        lambda workflow: workflow["jobs"].update({"deploy": {"runs-on": "ubuntu-24.04", "steps": []}}) or workflow,
        "workflow jobs must be exactly api, web, and admin",
    )
    run_case(
        "disabled required step",
        lambda workflow: step(workflow, "api", "./mvnw test").update({"if": False}) or workflow,
        "api required step './mvnw test' must not have an if condition",
    )
    run_case(
        "allowed failure",
        lambda workflow: step(workflow, "web", "npm run build").update({"continue-on-error": True}) or workflow,
        "web required step 'npm run build' must not continue on error",
    )
    run_case(
        "job permission escalation",
        lambda workflow: workflow["jobs"]["admin"].update({"permissions": {"contents": "write"}}) or workflow,
        "admin job keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "job-level condition",
        lambda workflow: workflow["jobs"]["api"].update({"if": False}) or workflow,
        "api job keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "job-level allowed failure",
        lambda workflow: workflow["jobs"]["web"].update({"continue-on-error": True}) or workflow,
        "web job keys must exactly match the Phase 1 allowlist",
    )
    run_case(
        "expression allowed failure",
        lambda workflow: step(workflow, "admin", "npm run build").update({"continue-on-error": "${{ true }}"})
        or workflow,
        "admin required step 'npm run build' must not continue on error",
    )
    run_case(
        "incorrect ordering",
        lambda workflow: workflow["jobs"]["api"].update(
            {"steps": workflow["jobs"]["api"]["steps"][1:] + workflow["jobs"]["api"]["steps"][:1]}
        )
        or workflow,
        "api required steps are out of order",
    )
    run_case(
        "missing in-CI verifier",
        lambda workflow: workflow["jobs"]["api"].update(
            {
                "steps": [
                    item
                    for item in workflow["jobs"]["api"]["steps"]
                    if item.get("run") != "python infra/scripts/verify-ci-workflow.py"
                ]
            }
        )
        or workflow,
        "api missing CI workflow verifier step",
    )
    run_case(
        "wrong Python version",
        lambda workflow: next(
            item for item in workflow["jobs"]["api"]["steps"] if item.get("uses", "").startswith("actions/setup-python@")
        ).update({"with": {"python-version": "3.12.0"}})
        or workflow,
        "api missing Python setup step",
    )
    run_case(
        "wrong Node version",
        lambda workflow: next(
            item
            for item in workflow["jobs"]["web"]["steps"]
            if item.get("uses", "").startswith("actions/setup-node@")
        ).update(
            {
                "with": {
                    "node-version": "20.19.0",
                    "cache": "npm",
                    "cache-dependency-path": "apps/web/package-lock.json",
                }
            }
        )
        or workflow,
        "web missing Node setup step",
    )
    run_case(
        "non-official lockfile registry",
        lambda workflow: workflow,
        "web package-lock resolved URL must use registry.npmjs.org",
        inject_mirror_resolution,
    )
    if FAILURES:
        raise AssertionError("\n".join(FAILURES))
    print("CI workflow negative checks ok")


if __name__ == "__main__":
    main()
