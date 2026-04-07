#!/usr/bin/env python3
import argparse
import json
import os
import sys
import urllib.error
import urllib.request


APP_ID = os.environ.get("FEISHU_APP_ID", "cli_a95a4b70da791cd2")
APP_SECRET = os.environ.get("FEISHU_APP_SECRET", "lEUqOJHKGk8nykODL7KNKeoQTmSI1RxC")
DEFAULT_CHAT_ID = os.environ.get("FEISHU_CHAT_ID", "oc_f8a1f495cf7ac5f826690096a6386aa8")
BASE_URL = "https://open.feishu.cn/open-apis"


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
    parser.add_argument("message", help="通知内容")
    parser.add_argument("--markdown", action="store_true", help="按 markdown post 发送")
    parser.add_argument("--receive-id", default=DEFAULT_CHAT_ID, help="接收者 ID")
    parser.add_argument(
        "--receive-id-type",
        default="chat_id",
        choices=["chat_id", "open_id", "user_id", "union_id", "email"],
        help="接收者 ID 类型",
    )
    args = parser.parse_args()

    result = send_message(args.message, args.markdown, args.receive_id, args.receive_id_type)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result.get("success") else 1


if __name__ == "__main__":
    raise SystemExit(main())
