#!/usr/bin/env bash
# scripts/guard-secrets.sh
# Rule: Never commit secrets, keystores, private keys, or local.properties.

set -euo pipefail

# Get staged files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACMR 2>/dev/null || git ls-files 2>/dev/null)

FOUND_SECRETS=0

# Check 1: Forbidden filenames
for file in $STAGED_FILES; do
    if [[ "$file" =~ \.(jks|keystore|p12)$ || "$file" =~ (^|/)local\.properties$ || "$file" =~ (^|/)\.env$ ]]; then
        echo "ERROR [guard-secrets]: Forbidden file found in commit/worktree: $file" >&2
        FOUND_SECRETS=1
    fi
done

# Check 2: Pattern scan in staged content
STAGED_DIFF=$(git diff --cached -U0 --no-color 2>/dev/null || true)
if [ -n "$STAGED_DIFF" ]; then
    SECRET_MATCHES=$(echo "$STAGED_DIFF" | grep -E "^\+" | grep -v "^\+\+\+" | grep -E "AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|ghp_[0-9a-zA-Z]{36}|AIza[0-9A-Za-z_-]{35}" || true)
    if [ -n "$SECRET_MATCHES" ]; then
        echo "ERROR [guard-secrets]: Potential API key or private key detected in staged diff:" >&2
        echo "$SECRET_MATCHES" >&2
        FOUND_SECRETS=1
    fi
fi

if [ "$FOUND_SECRETS" -ne 0 ]; then
    echo "ERROR [guard-secrets]: Commit blocked. Remove sensitive files or secrets." >&2
    exit 1
fi

echo "OK [guard-secrets]: No secrets or forbidden files detected."
exit 0
