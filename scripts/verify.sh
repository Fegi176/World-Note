#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash ./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
if [[ "${1:-}" == "--device" ]]; then
  : "${ANDROID_SERIAL:?Set ANDROID_SERIAL to a dedicated test device}"
  bash ./gradlew :app:connectedDebugAndroidTest --console=plain
else
  echo 'Instrumentation not run by this command; pass --device with ANDROID_SERIAL.'
fi
