#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "${PROJECT_ROOT}/scripts/load-env.sh"
load_env_stack "${PROJECT_ROOT}/backend" ".env.test" ".env.test.local"

bash "${PROJECT_ROOT}/scripts/ensure-test-services.sh"
bash "${PROJECT_ROOT}/scripts/reset-test-services-data.sh"

if [[ -d "/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

cd "${PROJECT_ROOT}/backend"
mvn test "$@"
