#!/usr/bin/env bash
# scripts/install-hooks.sh
# Configures Git to use versioned repository hooks in .githooks/

set -euo pipefail

git config core.hooksPath .githooks
chmod +x .githooks/* scripts/*.sh 2>/dev/null || true
echo "Git hooks configured to .githooks/ successfully."
