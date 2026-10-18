#!/usr/bin/env bash
# scripts/guard-strings.sh
# Rule: No hardcoded user-facing strings in UI code. Use stringResource(R.string...).

set -euo pipefail

UI_DIR="app/src/main/java/com/sijan/kharcha/ui"

if [ ! -d "$UI_DIR" ]; then
    echo "OK [guard-strings]: UI directory does not exist yet."
    exit 0
fi

# Look for Text("literal") or Text(text = "literal") outside of Previews and debug code
MATCHES=$(grep -rnE 'Text\s*\(\s*(text\s*=\s*)?"[^"]{3,}"' "$UI_DIR" 2>/dev/null | grep -v -E "guard-ok|@Preview|Greeting" || true)

if [ -n "$MATCHES" ]; then
    echo "WARN [guard-strings]: Possible hard-coded user strings in Composables. Use stringResource(R.string...):" >&2
    echo "$MATCHES" >&2
else
    echo "OK [guard-strings]: No hardcoded UI strings detected."
fi

exit 0
