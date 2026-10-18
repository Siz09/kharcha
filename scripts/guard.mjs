#!/usr/bin/env node
// Git-hook guard for Kharcha Android Project.
//   node scripts/guard.mjs precommit   -> scan staged additions for forbidden files, secrets, Double money, manifest INTERNET
//   node scripts/guard.mjs prepush     -> run full gates (check.sh or gradle tasks)
//   node scripts/guard.mjs ci [base]   -> same scan over base...HEAD
//   node scripts/guard.mjs worktree    -> scan uncommitted/untracked additions
import { execFileSync, spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { basename, resolve } from "node:path";

const mode = process.argv[2] || "precommit";
const git = (...args) =>
  execFileSync("git", args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });

const BASE = process.argv[3] || "origin/main";
const SRC = {
  precommit: { diff: ["--cached"], names: ["--cached"] },
  ci: { diff: [`${BASE}...HEAD`], names: [`${BASE}...HEAD`] },
  worktree: { diff: ["HEAD"], names: ["HEAD"] },
}[mode === "worktree" || mode === "ci" ? mode : "precommit"];

function getStagedFiles() {
  try {
    return git("diff", ...SRC.names, "--name-only", "--diff-filter=ACMR")
      .split("\n")
      .map((s) => s.trim())
      .filter(Boolean);
  } catch {
    return [];
  }
}

function getStagedDiff() {
  try {
    return git("diff", ...SRC.diff, "-U0", "--no-color", "--diff-filter=ACMR");
  } catch {
    return "";
  }
}

function runPrecommit() {
  const problems = [];
  const staged = getStagedFiles();

  for (const f of staged) {
    if (/\.(jks|keystore|p12)$/i.test(f)) {
      problems.push(`${f}: Keystore or certificate file must not be committed.`);
    }
    if (/(^|\/)local\.properties$/i.test(f)) {
      problems.push(`${f}: local.properties must remain gitignored.`);
    }
    if (/(^|\/)\.env(\.[^/]*)?$/i.test(f) && !/\.(example|sample)$/i.test(f)) {
      problems.push(`${f}: .env file in changeset. Keep secrets out of VCS.`);
    }
  }

  const diff = getStagedDiff();
  let curFile = "";
  for (const line of diff.split("\n")) {
    if (line.startsWith("+++ ")) {
      curFile = line.slice(4).replace(/^b\//, "").trim();
      continue;
    }
    if (line.startsWith("+") && !line.startsWith("+++")) {
      const added = line.slice(1);
      if (added.includes("guard-ok:")) continue;

      const isManifest = /(^|\/)AndroidManifest\.xml$/i.test(curFile);
      const isCode = /\.(kt|kts|java)$/i.test(curFile);
      const isDoc = /\.(md|txt)$/i.test(curFile) || /(^|\/)docs\//i.test(curFile);

      if (isManifest && /android\.permission\.INTERNET/i.test(added)) {
        problems.push(`${curFile}: android.permission.INTERNET added. Kharcha is strictly offline.`);
      }
      if (isCode && /fallbackToDestructiveMigration/i.test(added)) {
        problems.push(`${curFile}: fallbackToDestructiveMigration added. Destructive Room migrations are forbidden.`);
      }
      if (!isDoc && /AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|ghp_[0-9a-zA-Z]{36}/.test(added)) {
        problems.push(`${curFile}: Potential secret/private key added to commit.`);
      }
    }
  }

  // Double/Float check in data & domain
  for (const f of staged) {
    if (/\.(kt|kts)$/.test(f) && /(^|\/)(data|domain)\//.test(f)) {
      try {
        const content = readFileSync(f, "utf8");
        const lines = content.split("\n");
        lines.forEach((l, idx) => {
          if (l.includes("guard-ok:")) return;
          if (/\b(Double|Float)\b/.test(l)) {
            problems.push(`${f}:${idx + 1}: Double or Float in data/domain. Money must be Long paisa.`);
          }
        });
      } catch {}
    }
  }

  if (problems.length > 0) {
    console.error(`\x1b[31m[GUARD FAILED]\x1b[0m ${problems.length} violation(s) found:`);
    for (const p of problems) console.error(`  - ${p}`);
    process.exit(1);
  }
  console.log(`\x1b[32m[GUARD PASSED]\x1b[0m Precommit checks clean.`);
}

function runPrepush() {
  console.log("==> Running prepush checks...");
  runPrecommit();
  const gradlew = process.platform === "win32" ? "gradlew.bat" : "./gradlew";
  const res = spawnSync(gradlew, ["testDebugUnitTest", "lintDebug"], { stdio: "inherit" });
  if (res.status !== 0) {
    console.error(`\x1b[31m[GUARD FAILED]\x1b[0m Gradle tests or lint failed.`);
    process.exit(res.status || 1);
  }
  console.log(`\x1b[32m[GUARD PASSED]\x1b[0m Prepush checks clean.`);
}

if (mode === "prepush") {
  runPrepush();
} else {
  runPrecommit();
}
