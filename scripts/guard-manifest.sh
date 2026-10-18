#!/usr/bin/env bash
# scripts/guard-manifest.sh
# Rule: Kharcha must stay 100% offline. Zero INTERNET permission allowed.

set -euo pipefail

if grep -rn "android\.permission\.INTERNET" app/src/ 2>/dev/null; then
    echo "ERROR [guard-manifest]: 'android.permission.INTERNET' found in manifest. Kharcha must remain 100% offline." >&2
    exit 1
fi

echo "OK [guard-manifest]: No INTERNET permission found."
exit 0
