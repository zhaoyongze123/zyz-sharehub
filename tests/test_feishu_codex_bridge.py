import json
import subprocess
import unittest
from unittest.mock import patch

from scripts import feishu_codex_bridge


class ParseTextContentTest(unittest.TestCase):
    def test_parse_text_content_from_json_text_field(self) -> None:
        raw_content = json.dumps({"text": "  夜间推进任务  "}, ensure_ascii=False)

        self.assertEqual(
            feishu_codex_bridge.parse_text_content(raw_content),
            "夜间推进任务",
        )

    def test_parse_text_content_falls_back_to_raw_string(self) -> None:
        self.assertEqual(
            feishu_codex_bridge.parse_text_content("  直接文本  "),
            "直接文本",
        )


class ParseFeishuMessageTest(unittest.TestCase):
    def test_parse_feishu_message_reads_text_event(self) -> None:
        body = {
            "header": {"event_id": "evt-1"},
            "event": {
                "sender": {
                    "sender_id": {"open_id": "ou_123"},
                    "sender_name": "值守同学",
                },
                "message": {
                    "message_type": "text",
                    "chat_id": "oc_123",
                    "content": json.dumps({"text": "处理这个问题"}, ensure_ascii=False),
                },
            },
        }

        message = feishu_codex_bridge.parse_feishu_message(body)

        self.assertIsNotNone(message)
        assert message is not None
        self.assertEqual(message.event_id, "evt-1")
        self.assertEqual(message.sender_open_id, "ou_123")
        self.assertEqual(message.sender_name, "值守同学")
        self.assertEqual(message.chat_id, "oc_123")
        self.assertEqual(message.text, "处理这个问题")

    def test_parse_feishu_message_rejects_non_text_message(self) -> None:
        body = {
            "event": {
                "message": {
                    "message_type": "image",
                    "chat_id": "oc_123",
                    "content": "{}",
                }
            }
        }

        self.assertIsNone(feishu_codex_bridge.parse_feishu_message(body))


class IgnoreAndReplyTest(unittest.TestCase):
    def test_should_ignore_empty_text(self) -> None:
        message = feishu_codex_bridge.FeishuMessage(
            event_id="evt-1",
            chat_id="oc_123",
            sender_open_id="ou_123",
            sender_name="值守同学",
            text="   ",
        )

        self.assertEqual(
            feishu_codex_bridge.should_ignore_message(message),
            (True, "ignore_empty_text"),
        )

    @patch("scripts.feishu_codex_bridge.run_codex_exec")
    def test_build_reply_returns_assistant_message(self, run_codex_exec) -> None:
        run_codex_exec.return_value = ("已经处理完成", ['{"type":"item.completed"}'], 0)
        message = feishu_codex_bridge.FeishuMessage(
            event_id="evt-1",
            chat_id="oc_123",
            sender_open_id="ou_123",
            sender_name="值守同学",
            text="请继续推进",
        )

        reply = feishu_codex_bridge.build_reply(message)

        self.assertIn("Codex 已完成飞书任务。", reply)
        self.assertIn("任务：请继续推进", reply)
        self.assertIn("已经处理完成", reply)

    @patch("scripts.feishu_codex_bridge.run_codex_exec")
    def test_build_reply_reports_timeout(self, run_codex_exec) -> None:
        run_codex_exec.side_effect = subprocess.TimeoutExpired(
            cmd=["codex", "exec"],
            timeout=1800,
        )
        message = feishu_codex_bridge.FeishuMessage(
            event_id="evt-1",
            chat_id="oc_123",
            sender_open_id="ou_123",
            sender_name="值守同学",
            text="请继续推进",
        )

        reply = feishu_codex_bridge.build_reply(message)

        self.assertIn("Codex 任务执行超时。", reply)


if __name__ == "__main__":
    unittest.main()
