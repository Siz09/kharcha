#!/usr/bin/env bash
# scripts/check.sh
# Full validation gate: runs all guards, build, unit tests, and lint.

set -euo pipefail

echo "==> Running guards..."
bash scripts/guard-manifest.sh
bash scripts/guard-money.sh
bash scripts/guard-migrations.sh
bash scripts/guard-strings.sh
bash scripts/guard-secrets.sh

echo "==> Locating Gradle wrapper..."
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
elif [ -f "./gradlew.bat" ]; then
    GRADLE_CMD="./gradlew.bat"
else
    echo "ERROR: Gradle wrapper not found." >&2
    exit 1
fi

echo "==> Running Unit Tests..."
$GRADLE_CMD testDebugUnitTest

echo "==> Running Android Lint..."
$GRADLE_CMD lintDebug

echo "==> Building Debug APK..."
$GRADLE_CMD assembleDebug

echo "==> ALL CHECKS PASSED SUCCESSFULLY."
exit 0
