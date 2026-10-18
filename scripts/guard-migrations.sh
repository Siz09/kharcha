#!/usr/bin/env bash
# scripts/guard-migrations.sh
# Rule: No destructive Room migrations allowed. Exported schema JSON must accompany version bumps.

set -euo pipefail

# Check 1: fallbackToDestructiveMigration is forbidden in source code
if grep -rn --exclude-dir=build "fallbackToDestructiveMigration" app/src/ 2>/dev/null; then
    echo "ERROR [guard-migrations]: Found 'fallbackToDestructiveMigration'. Destructive migrations are strictly forbidden." >&2
    exit 1
fi

# Check 2: If AppDatabase.kt exists, extract declared version and verify schema JSON exists
DB_FILE="app/src/main/java/com/sijan/kharcha/data/db/AppDatabase.kt"
if [ -f "$DB_FILE" ]; then
    VERSION=$(grep -oE "version\s*=\s*[0-9]+" "$DB_FILE" | grep -oE "[0-9]+" || true)
    if [ -n "$VERSION" ]; then
        SCHEMA_FILE="app/schemas/com.sijan.kharcha.data.db.AppDatabase/${VERSION}.json"
        if [ ! -f "$SCHEMA_FILE" ]; then
            echo "WARN [guard-migrations]: Database version is $VERSION but schema file $SCHEMA_FILE is missing." >&2
            echo "Ensure schema export is generated and committed with Room version bumps." >&2
        fi
    fi
fi

echo "OK [guard-migrations]: Destructive migrations check passed."
exit 0
