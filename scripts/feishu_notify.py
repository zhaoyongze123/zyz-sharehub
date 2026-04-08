#!/usr/bin/env python3
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime
from typing import Optional


APP_ID = os.environ.get("FEISHU_APP_ID", "cli_a95a4b70da791cd2")
APP_SECRET = os.environ.get("FEISHU_APP_SECRET", "lEUqOJHKGk8nykODL7KNKeoQTmSI1RxC")
DEFAULT_CHAT_ID = os.environ.get("FEISHU_CHAT_ID", "oc_f8a1f495cf7ac5f826690096a6386aa8")
BASE_URL = "https://open.feishu.cn/open-apis"


def now_text() -> str:
    return datetime.now().astimezone().strftime("%Y-%m-%d %H:%M:%S %z")


def format_duration(seconds: Optional[int]) -> str:
    if seconds is None or seconds < 0:
        return "未知"

    minutes, sec = divmod(seconds, 60)
    hours, minutes = divmod(minutes, 60)
    if hours > 0:
        return f"{hours}小时{minutes}分{sec}秒"
    if minutes > 0:
        return f"{minutes}分{sec}秒"
    return f"{sec}秒"


def build_structured_message(args: argparse.Namespace) -> str:
    if not args.event:
        return args.message

    lines = [f"[{args.event}] ShareHub 夜间自动化"]
    lines.append(f"时间：{args.time_text or now_text()}")

    if args.status:
        lines.append(f"状态：{args.status}")
    if args.run_id:
        lines.append(f"轮次：{args.run_id}")
    if args.branch:
        lines.append(f"分支：{args.branch}")
    if args.module:
        lines.append(f"模块：{args.module}")
    if args.frontend_branch:
        lines.append(f"前端分支：{args.frontend_branch}")
    if args.stage:
        lines.append(f"阶段：{args.stage}")
    if args.duration_seconds is not None:
        lines.append(f"耗时：{format_duration(args.duration_seconds)}")
    if args.commit:
        lines.append(f"提交：{args.commit}")
    if args.smoke:
        lines.append(f"Smoke：{args.smoke}")
    if args.frontend_followup:
        lines.append(f"前端跟进：{args.frontend_followup}")
    if args.push_status:
        lines.append(f"Push：{args.push_status}")
    if args.next_task:
        lines.append(f"下一任务：{args.next_task}")
    if args.reason:
        lines.append(f"原因：{args.reason}")
    if args.action:
        lines.append(f"动作：{args.action}")
    if args.impact:
        lines.append(f"影响：{args.impact}")
    if args.result:
        lines.append(f"结果：{args.result}")
    if args.evidence:
        lines.append(f"证据：{args.evidence}")

    return "\n".join(lines)


def get_token() -> str | None:
    payload = json.dumps({"app_id": APP_ID, "app_secret": APP_SECRET}).encode("utf-8")
    request = urllib.request.Request(
        f"{BASE_URL}/auth/v3/tenant_access_token/internal",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            data = json.loads(response.read().decode("utf-8"))
            if data.get("code") == 0:
                return data.get("tenant_access_token")
            return None
    except Exception:
        return None


def send_message(message: str, markdown: bool, receive_id: str, receive_id_type: str) -> dict:
    token = get_token()
    if not token:
        return {"success": False, "error": "获取 tenant access token 失败"}

    if markdown:
        payload = {
            "receive_id": receive_id,
            "msg_type": "post",
            "content": json.dumps(
                {
                    "zh_cn": {
                        "title": "ShareHub 夜间推进通知",
                        "content": [[{"tag": "text", "text": message}]],
                    }
                },
                ensure_ascii=False,
            ),
        }
    else:
        payload = {
            "receive_id": receive_id,
            "msg_type": "text",
            "content": json.dumps({"text": message}, ensure_ascii=False),
        }

    request = urllib.request.Request(
        f"{BASE_URL}/im/v1/messages?receive_id_type={receive_id_type}",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            data = json.loads(response.read().decode("utf-8"))
            if data.get("code") == 0:
                return {"success": True, "data": data.get("data", {})}
            return {"success": False, "error": data}
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="ignore")
        return {"success": False, "error": f"HTTP {error.code}: {body}"}
    except Exception as error:
        return {"success": False, "error": str(error)}


def main() -> int:
    parser = argparse.ArgumentParser(description="发送 ShareHub 飞书通知")
    parser.add_argument("message", nargs="?", default="", help="通知内容")
    parser.add_argument("--markdown", action="store_true", help="按 markdown post 发送")
    parser.add_argument("--receive-id", default=DEFAULT_CHAT_ID, help="接收者 ID")
    parser.add_argument(
        "--receive-id-type",
        default="chat_id",
        choices=["chat_id", "open_id", "user_id", "union_id", "email"],
        help="接收者 ID 类型",
    )
    parser.add_argument("--event", help="事件类型")
    parser.add_argument("--status", help="状态")
    parser.add_argument("--time-text", help="事件时间文本")
    parser.add_argument("--run-id", help="轮次")
    parser.add_argument("--branch", help="分支")
    parser.add_argument("--module", help="模块")
    parser.add_argument("--frontend-branch", help="前端分支")
    parser.add_argument("--stage", help="阶段")
    parser.add_argument("--duration-seconds", type=int, help="耗时秒数")
    parser.add_argument("--commit", help="提交")
    parser.add_argument("--smoke", help="Smoke 状态")
    parser.add_argument("--frontend-followup", help="前端跟进状态")
    parser.add_argument("--push-status", help="Push 状态")
    parser.add_argument("--next-task", help="下一任务")
    parser.add_argument("--reason", help="原因")
    parser.add_argument("--action", help="动作")
    parser.add_argument("--impact", help="影响")
    parser.add_argument("--result", help="结果")
    parser.add_argument("--evidence", help="证据路径")
    args = parser.parse_args()

    message = build_structured_message(args)
    result = send_message(message, args.markdown, args.receive_id, args.receive_id_type)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result.get("success") else 1


if __name__ == "__main__":
    raise SystemExit(main())
