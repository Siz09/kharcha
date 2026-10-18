#!/usr/bin/env bash
# scripts/guard-money.sh
# Rule: Money must be Long in paisa. No Double or Float for money calculations in data or domain.

set -euo pipefail

TARGET_DIRS="app/src/main/java/com/sijan/kharcha/data app/src/main/java/com/sijan/kharcha/domain"

FOUND_ISSUES=0

for dir in $TARGET_DIRS; do
    if [ -d "$dir" ]; then
        # Search for Double or Float occurrences in data/domain layers
        # Exclude lines carrying escape hatch: guard-ok: <reason>
        MATCHES=$(grep -rnE "\b(Double|Float)\b" "$dir" | grep -v "guard-ok:" || true)
        if [ -n "$MATCHES" ]; then
            echo "ERROR [guard-money]: Found Double or Float in $dir:" >&2
            echo "$MATCHES" >&2
            FOUND_ISSUES=1
        fi
    fi
done

if [ "$FOUND_ISSUES" -ne 0 ]; then
    echo "ERROR [guard-money]: All money must be stored and computed as Long in paisa. Floating point forbidden." >&2
    exit 1
fi

echo "OK [guard-money]: No Double/Float in data/domain layers."
exit 0
