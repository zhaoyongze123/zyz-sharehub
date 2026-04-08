#!/usr/bin/env python3
"""
飞书 -> Codex CLI 最小桥接服务

能力：
1. 处理飞书 URL 校验
2. 接收 im.message.receive_v1
3. 调用 codex exec 执行任务
4. 将结果回发到飞书会话
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any


SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from feishu_notify import send_message  # noqa: E402


HOST = os.environ.get("CODEX_BRIDGE_HOST", "127.0.0.1")
PORT = int(os.environ.get("CODEX_BRIDGE_PORT", "8765"))
WORKDIR = os.environ.get("CODEX_BRIDGE_WORKDIR", str(Path.cwd()))
VERIFY_TOKEN = os.environ.get("FEISHU_VERIFICATION_TOKEN", "").strip()
BOT_OPEN_ID = os.environ.get("FEISHU_BOT_OPEN_ID", "").strip()
ALLOWED_SENDERS = {
    item.strip()
    for item in os.environ.get("FEISHU_ALLOWED_OPEN_IDS", "").split(",")
    if item.strip()
}
THREAD_POOL_SIZE = int(os.environ.get("CODEX_BRIDGE_WORKERS", "2"))
PROMPT_PREFIX = os.environ.get(
    "CODEX_BRIDGE_PROMPT_PREFIX",
    (
        "你正在响应一条来自飞书的任务指令。"
        "先完成用户要求，再用简洁中文给出结果。"
        "如果需要说明限制或失败原因，直接明确说明。"
    ),
).strip()

EXECUTOR = ThreadPoolExecutor(max_workers=THREAD_POOL_SIZE)


@dataclass
class FeishuMessage:
    event_id: str
    chat_id: str
    sender_open_id: str
    sender_name: str
    text: str


def log(event: str, **kwargs: Any) -> None:
    payload = {"event": event, **kwargs}
    print(json.dumps(payload, ensure_ascii=False), flush=True)


def parse_text_content(raw_content: str) -> str:
    if not raw_content:
        return ""

    try:
        payload = json.loads(raw_content)
    except json.JSONDecodeError:
        return raw_content.strip()

    text = payload.get("text")
    if isinstance(text, str):
        return text.strip()
    return raw_content.strip()


def parse_feishu_message(body: dict[str, Any]) -> FeishuMessage | None:
    header = body.get("header") or {}
    event = body.get("event") or {}
    message = event.get("message") or {}
    sender = event.get("sender") or {}
    sender_id = sender.get("sender_id") or {}
    sender_sender_id = sender.get("sender_sender_id") or {}

    message_type = message.get("message_type") or message.get("msg_type")
    if message_type != "text":
        return None

    sender_open_id = (
        sender_id.get("open_id")
        or sender_sender_id.get("open_id")
        or ""
    ).strip()
    sender_name = (sender.get("sender_name") or event.get("sender_name") or "").strip()
    chat_id = (message.get("chat_id") or event.get("chat_id") or "").strip()
    text = parse_text_content(message.get("content") or "")
    event_id = (header.get("event_id") or body.get("uuid") or "").strip()

    if not chat_id or not text:
        return None

    return FeishuMessage(
        event_id=event_id,
        chat_id=chat_id,
        sender_open_id=sender_open_id,
        sender_name=sender_name,
        text=text,
    )


def is_allowed_sender(sender_open_id: str) -> bool:
    if not ALLOWED_SENDERS:
        return True
    return sender_open_id in ALLOWED_SENDERS


def should_ignore_message(message: FeishuMessage) -> tuple[bool, str]:
    if BOT_OPEN_ID and message.sender_open_id == BOT_OPEN_ID:
        return True, "ignore_bot_self"
    if not is_allowed_sender(message.sender_open_id):
        return True, "ignore_sender_not_allowed"
    if not message.text.strip():
        return True, "ignore_empty_text"
    return False, ""


def run_codex_exec(user_text: str) -> tuple[str, list[str], int]:
    prompt = f"{PROMPT_PREFIX}\n\n飞书用户任务：\n{user_text.strip()}"
    command = [
        "codex",
        "exec",
        "--json",
        "--skip-git-repo-check",
        "-C",
        WORKDIR,
        prompt,
    ]
    result = subprocess.run(
        command,
        capture_output=True,
        text=True,
        cwd=WORKDIR,
        timeout=1800,
    )

    stdout_lines = [line for line in result.stdout.splitlines() if line.strip()]
    assistant_message = ""
    for line in stdout_lines:
        try:
            item = json.loads(line)
        except json.JSONDecodeError:
            continue
        if item.get("type") != "item.completed":
            continue
        payload = item.get("item") or {}
        if payload.get("type") == "agent_message":
            text = payload.get("text")
            if isinstance(text, str) and text.strip():
                assistant_message = text.strip()

    return assistant_message, stdout_lines, result.returncode


def build_reply(message: FeishuMessage) -> str:
    try:
        assistant_message, stdout_lines, return_code = run_codex_exec(message.text)
    except subprocess.TimeoutExpired:
        return (
            "Codex 任务执行超时。\n"
            f"工作目录：{WORKDIR}\n"
            f"原始任务：{message.text}"
        )
    except Exception as error:
        return (
            "Codex 任务执行失败。\n"
            f"错误：{error}\n"
            f"工作目录：{WORKDIR}"
        )

    if assistant_message:
        return (
            f"Codex 已完成飞书任务。\n"
            f"工作目录：{WORKDIR}\n"
            f"任务：{message.text}\n\n"
            f"{assistant_message}"
        )

    if stdout_lines:
        preview = "\n".join(stdout_lines[-8:])
        return (
            "Codex 已执行任务，但没有提取到最终助手消息。\n"
            f"退出码：{return_code}\n"
            f"工作目录：{WORKDIR}\n"
            "最近输出：\n"
            f"{preview}"
        )

    return (
        "Codex 已执行任务，但没有返回可解析输出。\n"
        f"退出码：{return_code}\n"
        f"工作目录：{WORKDIR}"
    )


def send_reply(chat_id: str, text: str) -> None:
    result = send_message(
        message=text,
        markdown=False,
        receive_id=chat_id,
        receive_id_type="chat_id",
    )
    if result.get("success"):
        log("feishu_reply_sent", chat_id=chat_id)
        return
    log("feishu_reply_failed", chat_id=chat_id, error=result.get("error"))


def handle_message(message: FeishuMessage) -> None:
    ignored, reason = should_ignore_message(message)
    if ignored:
        log(
            reason,
            sender_open_id=message.sender_open_id,
            chat_id=message.chat_id,
            event_id=message.event_id,
        )
        return

    log(
        "codex_task_started",
        event_id=message.event_id,
        chat_id=message.chat_id,
        sender_open_id=message.sender_open_id,
        text=message.text,
    )
    reply = build_reply(message)
    send_reply(message.chat_id, reply)
    log(
        "codex_task_completed",
        event_id=message.event_id,
        chat_id=message.chat_id,
        sender_open_id=message.sender_open_id,
    )


class Handler(BaseHTTPRequestHandler):
    server_version = "FeishuCodexBridge/0.1"

    def log_message(self, format: str, *args: Any) -> None:  # noqa: A003
        log("http_access", path=self.path, detail=format % args)

    def _send_json(self, status: int, payload: dict[str, Any]) -> None:
        raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self) -> None:  # noqa: N802
        if self.path == "/healthz":
            self._send_json(
                200,
                {
                    "ok": True,
                    "host": HOST,
                    "port": PORT,
                    "workdir": WORKDIR,
                },
            )
            return
        self._send_json(404, {"ok": False, "error": "not_found"})

    def do_POST(self) -> None:  # noqa: N802
        if self.path != "/feishu/events":
            self._send_json(404, {"ok": False, "error": "not_found"})
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        raw_body = self.rfile.read(content_length)

        try:
            body = json.loads(raw_body.decode("utf-8"))
        except json.JSONDecodeError:
            self._send_json(400, {"ok": False, "error": "invalid_json"})
            return

        token = (body.get("token") or "").strip()
        if VERIFY_TOKEN and token and token != VERIFY_TOKEN:
            log("invalid_verify_token", token=token)
            self._send_json(403, {"ok": False, "error": "invalid_token"})
            return

        if body.get("type") == "url_verification" and body.get("challenge"):
            self._send_json(200, {"challenge": body["challenge"]})
            return

        message = parse_feishu_message(body)
        if message is None:
            log("ignore_unsupported_event", body=body)
            self._send_json(200, {"ok": True, "ignored": True})
            return

        EXECUTOR.submit(handle_message, message)
        self._send_json(200, {"ok": True, "accepted": True})


def main() -> int:
    log(
        "bridge_starting",
        host=HOST,
        port=PORT,
        workdir=WORKDIR,
        allow_sender_count=len(ALLOWED_SENDERS),
    )
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log("bridge_stopped", reason="keyboard_interrupt")
        return 0
    finally:
        EXECUTOR.shutdown(wait=False, cancel_futures=True)
        server.server_close()


if __name__ == "__main__":
    raise SystemExit(main())
