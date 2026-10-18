---
name: release-checklist
description: Use this skill when preparing a release, validating all build gates, verifying guards, and preparing documentation/screenshots for Kharcha.
---

# Kharcha Release Verification Checklist

Follow this procedure before tagging or distributing any release APK or merging into main.

## 1. Run Repository Guards
Execute all guards locally:
```bash
bash scripts/guard-manifest.sh
bash scripts/guard-money.sh
bash scripts/guard-migrations.sh
bash scripts/guard-strings.sh
bash scripts/guard-secrets.sh
```
All guards must exit with status 0.

## 2. Execute Test Suites
Run all unit and migration tests:
```bash
./gradlew testDebugUnitTest
```
Verify 100% pass rate.

## 3. Run Code Quality & Lint
```bash
./gradlew lintDebug
```
Address all critical lint errors.

## 4. Build Release Bundle / APK
```bash
./gradlew assembleRelease
```
Ensure obfuscation and shrinking (R8) pass without missing symbol warnings.

## 5. Documentation and Presentation Review
- Ensure `README.md` is up to date:
  - Architecture overview matching current code.
  - Setup and build instructions.
  - Screenshots / GIF captures of core flows (Scan, Confirm, Add, Home summary).
- Confirm all deferred ideas are tracked in `docs/IDEAS.md`.
- Ensure clean Git status: no uncommitted files, no debug secrets.
