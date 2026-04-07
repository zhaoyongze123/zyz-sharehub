#!/usr/bin/env python3
import os
import signal
import subprocess
import sys
import time


def main() -> int:
    if len(sys.argv) < 3:
        print("用法: run_with_timeout.py <timeout_seconds> <command...>", file=sys.stderr)
        return 2

    timeout_seconds = int(sys.argv[1])
    command = sys.argv[2:]

    process = subprocess.Popen(command)
    start = time.time()

    while True:
        code = process.poll()
        if code is not None:
            return code

        if time.time() - start > timeout_seconds:
            process.send_signal(signal.SIGTERM)
            try:
                return process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                return 124

        time.sleep(1)


if __name__ == "__main__":
    raise SystemExit(main())
