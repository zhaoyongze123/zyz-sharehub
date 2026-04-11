#!/usr/bin/env python3
import argparse
from pathlib import Path


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def has_text(path: Path, needle: str) -> bool:
    return needle in read_text(path)


def lacks_text(path: Path, needle: str) -> bool:
    return needle not in read_text(path)


def manager_prompt_is_admin_only(path: Path) -> bool:
    text = read_text(path)
    allowed_boundary_lines = (
        "只推进后台管理生产级改造专项，不再推进公开站点、资源广场、路线广场、笔记详情、发布页或全站走查。",
        "只能从后台专项待办里选任务，禁止跨到公开站点或全站走查。",
    )
    sanitized = text
    for line in allowed_boundary_lines:
        sanitized = sanitized.replace(line, "")
    forbidden_terms = (
        "公开站点",
        "资源广场",
        "路线广场",
        "笔记详情",
        "发布页",
        "全站走查",
        "批次 1：资源 + 路线公开读取链路",
    )
    return all(term not in sanitized for term in forbidden_terms)


def env_value(path: Path, key: str) -> str:
    if not path.exists():
        return ""
    prefix = f"{key}="
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith(prefix):
            return line[len(prefix):]
    return ""


def has_env_key(path: Path, key: str) -> bool:
    return env_value(path, key) != ""


def check(predicate: bool, name: str, passed: list[str], pending: list[str]) -> None:
    if predicate:
        passed.append(name)
    else:
        pending.append(name)


def main() -> int:
    parser = argparse.ArgumentParser(description="检查 ShareHub 后台专项夜间推进是否已完成")
    parser.add_argument("--project-root", required=True, help="仓库根目录")
    parser.add_argument("--output", required=True, help="状态输出文件")
    args = parser.parse_args()

    root = Path(args.project_root)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)

    tests = root / "frontend" / "tests" / "e2e"
    scripts_dir = root / "scripts"
    docs_dir = root / "docs"
    overnight_dir = root / "output" / "overnight"
    latest_meta = overnight_dir / "latest-meta.env"
    latest_run_id = env_value(latest_meta, "RUN_ID")
    latest_smoke_meta = overnight_dir / latest_run_id / "browser-smoke" / "meta.env" if latest_run_id else overnight_dir / "missing-browser-smoke-meta.env"
    summary_file = output.parent / "completion-summary.txt"

    passed: list[str] = []
    pending: list[str] = []
    manager_prompt = scripts_dir / "overnight-manager-prompt.md"

    check(
        has_text(manager_prompt, "后台管理生产级改造专项")
        and has_text(manager_prompt, "只推进后台管理生产级改造专项")
        and manager_prompt_is_admin_only(manager_prompt),
        "自动化 prompt 已切到后台专项",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-hourly-run.sh", "ADMIN_AUTH_EXIT_CODE")
        and has_text(scripts_dir / "overnight-hourly-run.sh", "ADMIN_SMOKE_EXIT_CODE")
        and has_text(scripts_dir / "overnight-hourly-run.sh", "ADMIN_GATE_EXIT_CODE")
        and has_text(scripts_dir / "overnight-hourly-run.sh", "ADMIN_SMOKE_SCRIPT_EXIT_CODE")
        and lacks_text(scripts_dir / "overnight-hourly-run.sh", 'echo "SMOKE_EXIT_CODE=')
        and has_text(scripts_dir / "overnight-hourly-run.sh", "已禁用公开站点前端跟进子代理")
        and lacks_text(scripts_dir / "overnight-hourly-run.sh", "开始执行前端跟进子代理"),
        "单轮编排输出后台专项状态码",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", "OVERNIGHT_ADMIN_AUTOPILOT")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "PLAYWRIGHT_ADMIN_USER_KEY"),
        "后台 smoke 已切到后台专项模式",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", "ADMIN_SMOKE_SCRIPT_EXIT_CODE")
        and lacks_text(scripts_dir / "overnight-browser-smoke.sh", "STANDARD_SMOKE_EXIT_CODE"),
        "后台 smoke 脚本状态码已收口为后台专项语义",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", 'PLAYWRIGHT_MODULES:-admin,backend')
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "后台专项 smoke 仅允许 admin/backend 模块")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "validate_admin_modules")
        and lacks_text(scripts_dir / "overnight-browser-smoke.sh", "FULL_WALKTHROUGH_ENABLED")
        and lacks_text(scripts_dir / "overnight-browser-smoke.sh", "full-site-walkthrough.spec.ts"),
        "后台 smoke 只聚焦后台模块，不再触发全站走查",
        passed,
        pending,
    )
    check(
        (tests / "admin-smoke.spec.ts").exists()
        and lacks_text(tests / "admin-smoke.spec.ts", "PLAYWRIGHT_ADMIN_TOKEN")
        and has_text(tests / "admin-smoke.spec.ts", "PLAYWRIGHT_ADMIN_USER_KEY"),
        "后台 smoke 不再依赖管理员 token",
        passed,
        pending,
    )
    check(
        lacks_text(tests / "admin-smoke.spec.ts", "/admin/taxonomy")
        and lacks_text(tests / "admin-smoke.spec.ts", "/resources")
        and lacks_text(tests / "admin-smoke.spec.ts", "/roadmaps")
        and lacks_text(tests / "admin-smoke.spec.ts", "/publish")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/reviews')")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/reports')")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/audit-logs')")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/users')"),
        "后台 module smoke 只覆盖 admin/reports/reviews/users/audit-logs",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", "/actuator/health/readiness")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "/actuator/health/liveness"),
        "后台 smoke 已纳入 readiness/liveness probes",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", 'record_failure "${label} 未返回 status=UP"')
        and has_text(scripts_dir / "overnight-browser-smoke.sh", 'check_health_status_up "${BACKEND_BASE_URL}/actuator/health"')
        and has_text(scripts_dir / "overnight-browser-smoke.sh", 'check_health_status_up "${BACKEND_BASE_URL}/actuator/health/readiness"')
        and has_text(scripts_dir / "overnight-browser-smoke.sh", 'check_health_status_up "${BACKEND_BASE_URL}/actuator/health/liveness"'),
        "后台 smoke 会校验 health/readiness/liveness 返回 UP",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", "application.yml 未将 dev token 默认设为关闭且仅允许显式开启")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "application-cloud-dev.yml 未将 dev token 默认设为关闭且仅允许显式开启")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "application-test.yml 未将 dev token 默认设为关闭且仅允许显式开启")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "生产环境错误接受了 X-Admin-Token"),
        "后台门禁已区分生产禁 token 与本地显式开启 dev token",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", "缺少生产禁用 dev token 集成测试配置")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "缺少生产禁用 dev token 集成测试断言")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "缺少本地显式开启 dev token 集成测试配置")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "缺少本地显式开启 dev token 集成测试断言"),
        "后台门禁已要求显式区分 dev token 启停两类集成测试",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", "生产部署仍然包含 MySQL 配置")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "生产部署未显式使用 PostgreSQL JDBC，未满足 PostgreSQL-only"),
        "后台门禁会阻断非 PostgreSQL-only 生产配置",
        passed,
        pending,
    )
    check(
        has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin')")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/reports')")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/reviews')")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/audit-logs')")
        and has_text(tests / "admin-smoke.spec.ts", "await page.goto('/admin/users')")
        and has_text(tests / "admin-smoke.spec.ts", "const dashboardReportsResponsePromise = waitForAdminApiGet(page, '/admin/reports')")
        and has_text(tests / "admin-smoke.spec.ts", "const dashboardAuditResponsePromise = waitForAdminApiGet(page, '/admin/audit-logs')")
        and has_text(tests / "admin-smoke.spec.ts", "const dashboardUsersResponsePromise = waitForAdminApiGet(page, '/admin/users')")
        and has_text(tests / "admin-smoke.spec.ts", "const reviewsPageResponsePromise = waitForAdminApiGet(page, '/admin/reports')")
        and has_text(tests / "admin-smoke.spec.ts", "const reportsPageResponsePromise = waitForAdminApiGet(page, '/admin/reports')")
        and has_text(tests / "admin-smoke.spec.ts", "const auditPageResponsePromise = waitForAdminApiGet(page, '/admin/audit-logs')")
        and has_text(tests / "admin-smoke.spec.ts", "const usersPageResponsePromise = waitForAdminApiGet(page, '/admin/users')")
        and has_text(tests / "admin-smoke.spec.ts", "browserFetch(page, '/api/admin/reports?page=1&pageSize=20'"),
        "后台页面真接口验收覆盖 admin/reports/reviews/users/audit-logs",
        passed,
        pending,
    )
    check(
        has_text(scripts_dir / "overnight-browser-smoke.sh", "后台 smoke 缺少后台页面验收路由")
        and has_text(scripts_dir / "overnight-browser-smoke.sh", "validate_admin_scope"),
        "后台门禁会阻断缺失的后台页面验收路由",
        passed,
        pending,
    )
    check(
        has_text(docs_dir / "admin-automation-runbook.md", "ADMIN_AUTH_EXIT_CODE")
        and has_text(docs_dir / "admin-automation-runbook.md", "ADMIN_SMOKE_EXIT_CODE")
        and has_text(docs_dir / "admin-automation-runbook.md", "ADMIN_GATE_EXIT_CODE"),
        "后台运行手册包含专项状态码复核说明",
        passed,
        pending,
    )
    check(
        has_text(docs_dir / "admin-release-gates.md", "PostgreSQL-only")
        and has_text(docs_dir / "admin-release-gates.md", "readiness / liveness"),
        "后台发布门禁清单包含 PostgreSQL-only 与 probes",
        passed,
        pending,
    )
    check(
        has_text(docs_dir / "admin-rollback-checklist.md", "git checkout <上一稳定提交>")
        and has_text(docs_dir / "admin-rollback-checklist.md", "ADMIN_GATE_EXIT_CODE"),
        "后台回滚检查项包含状态码复核",
        passed,
        pending,
    )
    check(
        (docs_dir / "admin-automation-runbook.md").exists()
        and (docs_dir / "admin-release-gates.md").exists()
        and (docs_dir / "admin-rollback-checklist.md").exists(),
        "后台专项运行文档已落盘",
        passed,
        pending,
    )
    check(
        latest_meta.exists()
        and has_env_key(latest_meta, "RUN_ID")
        and env_value(latest_meta, "ADMIN_AUTH_EXIT_CODE") in {"0", "SKIPPED"},
        "最近一轮后台鉴权线结果已产出",
        passed,
        pending,
    )
    check(
        latest_meta.exists()
        and has_env_key(latest_meta, "RUN_ID")
        and env_value(latest_meta, "ADMIN_SMOKE_EXIT_CODE") == "0",
        "最近一轮后台 smoke 通过",
        passed,
        pending,
    )
    check(
        latest_meta.exists()
        and has_env_key(latest_meta, "RUN_ID")
        and env_value(latest_meta, "ADMIN_GATE_EXIT_CODE") == "0",
        "最近一轮后台门禁验证通过",
        passed,
        pending,
    )
    check(
        latest_smoke_meta.exists()
        and env_value(latest_smoke_meta, "ADMIN_AUTOPILOT_MODE") == "1",
        "最近一轮 smoke 已按后台专项模式执行",
        passed,
        pending,
    )
    check(
        latest_smoke_meta.exists()
        and env_value(latest_smoke_meta, "MODULES") == "admin,backend",
        "最近一轮 smoke 仅执行后台模块",
        passed,
        pending,
    )
    check(
        lacks_text(scripts_dir / "overnight-browser-smoke.sh", "module-smoke.spec.ts")
        and lacks_text(scripts_dir / "overnight-browser-smoke.sh", "sharehub-real-api.spec.ts")
        ,
        "后台专项机器判定已脱离通用 E2E 用例",
        passed,
        pending,
    )

    lines = ["ShareHub 后台专项夜间推进完成判定", ""]
    lines.append(f"已完成检查项：{len(passed)}/{len(passed) + len(pending)}")
    if passed:
        lines.append("")
        lines.append("已满足检查项：")
        for item in passed:
            lines.append(f"- {item}")
    if pending:
        lines.append("")
        lines.append("未满足检查项：")
        for item in pending:
            lines.append(f"- {item}")
    else:
        lines.append("")
        lines.append("后台专项所有机器可判定检查项均已满足，可自动停止 night supervisor。")

    reason = "后台管理专项机器可判定验收已通过"
    if pending:
        reason = "后台管理专项仍有待满足的机器判定项"

    summary_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
    output.write_text(
        f"SUMMARY_FILE={summary_file}\n"
        f"REASON={reason}\n"
        f"PENDING_COUNT={len(pending)}\n",
        encoding="utf-8",
    )
    return 0 if not pending else 1


if __name__ == "__main__":
    raise SystemExit(main())
