---
name: room-migration
description: Use this skill when updating the Room database schema, adding an entity field, or creating a new database migration.
---

# Room Schema Migration Workflow

Follow this procedure strictly whenever altering Room database entities or schema version.

## Pre-requisites
- `exportSchema = true` must remain configured in `AppDatabase.kt`.
- KSP schema argument `room.schemaLocation = "$projectDir/schemas"` must remain in `app/build.gradle.kts`.
- Destructive migrations (`fallbackToDestructiveMigration`) are strictly forbidden.

## Step-by-Step Migration Process

1. **Increment Database Version**:
   - In `app/src/main/java/com/sijan/kharcha/data/db/AppDatabase.kt`, increment `@Database(version = N, ...)`.
   - Update entities or DAOs as required.

2. **Define the Migration Object**:
   - In `app/src/main/java/com/sijan/kharcha/data/db/migration/Migrations.kt`:
   ```kotlin
   val MIGRATION_N_TO_M = object : Migration(N, M) {
       override fun migrate(db: SupportSQLiteDatabase) {
           // Explicit SQL statements only (CREATE TABLE, ALTER TABLE, etc.)
       }
   }
   ```
   - Register the migration in `AppDatabase` builder: `.addMigrations(MIGRATION_N_TO_M)`.

3. **Generate and Commit New Schema JSON**:
   - Run compilation to generate the new schema JSON via KSP:
     `./gradlew kspDebugKotlin`
   - Verify that `app/schemas/com.sijan.kharcha.data.db.AppDatabase/<M>.json` is created.
   - Stage and commit the schema JSON alongside your entity and migration code.

4. **Write Migration Test**:
   - In `app/src/test/java/com/sijan/kharcha/data/db/migration/MigrationTest.kt`, write an automated test using Room's `MigrationTestHelper`:
     - Create database at version N.
     - Insert sample test data.
     - Run migration to version M.
     - Validate data integrity and schema conformity.

5. **Verify**:
   - Run the migration test suite:
     `./gradlew testDebugUnitTest --tests "*Migration*"`
   - Run migration guard:
     `bash scripts/guard-migrations.sh`
