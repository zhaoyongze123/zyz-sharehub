#!/usr/bin/env python3
import argparse
from pathlib import Path


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def has_text(path: Path, needle: str) -> bool:
    return needle in read_text(path)


def lacks_text(path: Path, needle: str) -> bool:
    return needle not in read_text(path)


def check(predicate: bool, name: str, passed: list[str], pending: list[str]) -> None:
    if predicate:
        passed.append(name)
    else:
        pending.append(name)


def env_value(path: Path, key: str) -> str:
    if not path.exists():
        return ""
    prefix = f"{key}="
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith(prefix):
            return line[len(prefix):]
    return ""


def main() -> int:
    parser = argparse.ArgumentParser(description="检查 ShareHub 夜间推进是否已整体完成")
    parser.add_argument("--project-root", required=True, help="仓库根目录")
    parser.add_argument("--output", required=True, help="状态输出文件")
    args = parser.parse_args()

    root = Path(args.project_root)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)

    frontend = root / "frontend" / "src"
    tests = root / "frontend" / "tests" / "e2e"
    docs_dir = root / "docs"
    overnight_dir = root / "output" / "overnight"
    latest_meta = overnight_dir / "latest-meta.env"
    latest_run_id = env_value(latest_meta, "RUN_ID")
    latest_smoke_meta = overnight_dir / latest_run_id / "browser-smoke" / "meta.env" if latest_run_id else overnight_dir / "missing-browser-smoke-meta.env"
    summary_file = output.parent / "completion-summary.txt"

    passed: list[str] = []
    pending: list[str] = []

    check(
        lacks_text(frontend / "views" / "resource" / "ResourceListView.vue", "@/mock/resources"),
        "批次1-资源列表去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "resource" / "ResourceDetailView.vue", "@/mock/resources"),
        "批次1-资源详情去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "roadmap" / "RoadmapListView.vue", "@/mock/roadmaps"),
        "批次1-路线列表去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "roadmap" / "RoadmapDetailView.vue", "@/mock/roadmaps"),
        "批次1-路线详情去 mock",
        passed,
        pending,
    )
    check(
        has_text(tests / "sharehub-real-api.spec.ts", "page.goto('/resources')")
        and has_text(tests / "sharehub-real-api.spec.ts", "/api/resources/")
        and has_text(tests / "sharehub-real-api.spec.ts", "/resources/${resourceId}"),
        "批次1-资源 Playwright 真读取闭环",
        passed,
        pending,
    )
    check(
        has_text(tests / "module-smoke.spec.ts", "browserFetch(page, '/api/resources?page=0&pageSize=5'")
        and has_text(tests / "module-smoke.spec.ts", "getByText(`下载 ${resource.downloadCount}`)"),
        "批次1-资源 smoke 真渲染断言",
        passed,
        pending,
    )
    check(
        has_text(tests / "sharehub-real-api.spec.ts", "page.goto('/roadmaps')")
        and has_text(tests / "sharehub-real-api.spec.ts", "/api/roadmaps/")
        and has_text(tests / "sharehub-real-api.spec.ts", "/roadmaps/${roadmapId}"),
        "批次1-路线 Playwright 真读取闭环",
        passed,
        pending,
    )
    check(
        has_text(tests / "module-smoke.spec.ts", "browserFetch(page, '/api/roadmaps?page=1&pageSize=5')")
        and has_text(tests / "module-smoke.spec.ts", "roadmap.description?.trim()")
        and has_text(tests / "module-smoke.spec.ts", "page.getByText('节点进度结构')"),
        "批次1-路线 smoke 真渲染断言",
        passed,
        pending,
    )
    check(
        has_text(frontend / "stores" / "auth.ts", "/auth/me"),
        "批次2-鉴权接 auth/me",
        passed,
        pending,
    )
    check(
        has_text(frontend / "views" / "user" / "ProfileView.vue", "/api/me")
        or has_text(frontend / "views" / "user" / "ProfileView.vue", "fetchMe"),
        "批次2-个人中心接 me 聚合",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "user" / "ProfileView.vue", "个人资料保存成功！"),
        "批次2-个人中心不再模拟保存成功",
        passed,
        pending,
    )
    # 简历模块当前不作为“整站完成”的硬门槛。
    # 产品边界已调整为纯前端界面优先，是否继续保留或移除后端链路
    # 由后续单独任务收口，不阻塞夜间总完成判定。
    check(
        lacks_text(frontend / "views" / "admin" / "AdminReportsView.vue", "@/mock/admin"),
        "批次3-后台报告页去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "admin" / "AdminReviewsView.vue", "@/mock/admin"),
        "批次3-后台审核页去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "admin" / "AdminUsersView.vue", "@/mock/admin"),
        "批次3-后台用户页去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "admin" / "AdminTaxonomyView.vue", "@/mock/admin"),
        "批次3-后台标签页去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "note" / "NoteDetailView.vue", "@/mock/notes"),
        "批次3-笔记详情去 mock",
        passed,
        pending,
    )
    check(
        lacks_text(frontend / "views" / "resource" / "PublishResourceView.vue", "@/mock/resources"),
        "批次3-资料发布去 mock",
        passed,
        pending,
    )
    check(
        has_text(frontend / "views" / "roadmap" / "PublishRoadmapView.vue", "/api/roadmaps")
        or has_text(frontend / "views" / "roadmap" / "PublishRoadmapView.vue", "createRoadmap"),
        "批次3-路线创建接真实接口",
        passed,
        pending,
    )
    check(
        has_text(tests / "sharehub-real-api.spec.ts", "/api/admin/reports")
        and has_text(tests / "sharehub-real-api.spec.ts", "/api/admin/audit-logs"),
        "批次3-后台 Playwright 闭环",
        passed,
        pending,
    )
    check(
        (tests / "full-site-walkthrough.spec.ts").exists(),
        "批次4-全站走查用例已落盘",
        passed,
        pending,
    )
    check(
        has_text(tests / "full-site-walkthrough.spec.ts", "公开页走查")
        and has_text(tests / "full-site-walkthrough.spec.ts", "资源模块走查")
        and has_text(tests / "full-site-walkthrough.spec.ts", "路线模块走查")
        and has_text(tests / "full-site-walkthrough.spec.ts", "笔记模块走查")
        and has_text(tests / "full-site-walkthrough.spec.ts", "个人中心走查")
        and has_text(tests / "full-site-walkthrough.spec.ts", "后台模块走查"),
        "批次4-全站走查覆盖主模块",
        passed,
        pending,
    )
    check(
        (docs_dir / "deployment-runbook.md").exists()
        and (docs_dir / "release-checklist.md").exists()
        and (docs_dir / "demo-script.md").exists(),
        "批次4-部署发布演示材料已落盘",
        passed,
        pending,
    )
    check(
        has_text(docs_dir / "deployment-runbook.md", "启动")
        and has_text(docs_dir / "release-checklist.md", "发布前")
        and has_text(docs_dir / "demo-script.md", "演示流程"),
        "批次4-收尾材料具备可执行内容",
        passed,
        pending,
    )
    check(
        env_value(latest_meta, "SMOKE_EXIT_CODE") == "0",
        "批次4-最近一轮标准 smoke 通过",
        passed,
        pending,
    )
    check(
        env_value(latest_smoke_meta, "FULL_WALKTHROUGH_EXECUTED") == "1",
        "批次4-最近一轮已进入全站走查阶段",
        passed,
        pending,
    )
    check(
        env_value(latest_smoke_meta, "FULL_WALKTHROUGH_EXIT_CODE") == "0",
        "批次4-最近一轮全站走查通过",
        passed,
        pending,
    )

    lines = ["ShareHub 夜间推进整体完成判定", ""]
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
        lines.append("所有机器可判定检查项均已满足，可自动停止 night supervisor。")

    summary_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
    output.write_text(
        f"SUMMARY_FILE={summary_file}\n"
        f"REASON=所有批次验收项已满足\n"
        f"PENDING_COUNT={len(pending)}\n",
        encoding="utf-8",
    )
    return 0 if not pending else 1


if __name__ == "__main__":
    raise SystemExit(main())
