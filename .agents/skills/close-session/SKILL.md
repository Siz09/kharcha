---
name: close-session
description: Close out a Kharcha work session — update .agents/memory/ topic files and MEMORY.md index, sync persistent memory stores, verify repository gates, and check git status. Trigger phrase: "close out this session", "wrap up this session", or "close this out".
---

# Close Session — Kharcha

Runs at the end of a Kharcha work session, triggered by "close out this session", "wrap up this session", or similar phrases.

## Steps

1. **Scope the session**:
   - Determine which milestones, features, bugs, or architecture components were touched.
   - Separate completed work from in-progress or deferred work.

2. **Update Persistent Memory (`.agents/memory/`)**:
   - Update `session-progress-and-milestones.md` with current milestone status, completed units, and next steps.
   - Update or add domain memory files (`offline-first-architecture.md`, `receipt-capture-and-parsing.md`, `testing-and-verification-gates.md`, etc.) for durable learnings, platform quirks, or architectural invariants discovered.
   - Keep `MEMORY.md` index synchronized (short link and one-line summary per topic file).
   - Sync `.agents/memory/` files to external mirrors if configured (`~/.gemini/config/memory/D--vscode-kharcha/`, `~/.gemini/antigravity-cli/memory/D--vscode-kharcha/`, `~/.claude/projects/D--vscode-kharcha/memory/`).

3. **Verify Repository Gates**:
   - Run `bash scripts/check.sh` (or `./gradlew assembleDebug testDebugUnitTest lintDebug`).
   - Run guards: `bash scripts/guard-money.sh`, `bash scripts/guard-manifest.sh`, `bash scripts/guard-migrations.sh`, `bash scripts/guard-secrets.sh`.
   - Re-check non-negotiables: Money is Long paisa, zero INTERNET permission, no unconfirmed scans.

4. **Review Git Status**:
   - Check `rtk git status`.
   - Ensure `.agents/memory/` and other local scratch files are ignored by git.
   - Report uncommitted changes and unpushed commits. Do not force push or push without verification.

5. **Terse Closeout Report**:
   - Summarize what got completed.
   - List memory files updated.
   - Report verification status and git state.
