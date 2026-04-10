#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -d "/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

SPRING_PORT="${SERVER_PORT:-18080}"

cd "${PROJECT_ROOT}/backend"
mvn spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.arguments="--server.port=${SPRING_PORT}"
