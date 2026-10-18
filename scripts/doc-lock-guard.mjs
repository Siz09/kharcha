#!/usr/bin/env node
// scripts/doc-lock-guard.mjs
// Antigravity PreToolUse hook: prevents edits to locked docs in docs/ without explicit prompt
import { readFileSync } from "node:fs";

let input = "";
process.stdin.setEncoding("utf8");
process.stdin.on("data", (chunk) => { input += chunk; });
process.stdin.on("end", () => {
  try {
    const payload = JSON.parse(input);
    const toolCall = payload.toolCall || {};
    const args = toolCall.args || {};
    const target = args.TargetFile || args.target_file || args.path || "";

    const lockedDocs = [
      "PRD.md",
      "TRD.md",
      "DATA_LAYER_STRUCTURE.md",
      "APP_FLOW.md",
      "FRONTEND_GUIDELINES.md"
    ];

    for (const doc of lockedDocs) {
      if (target.endsWith(doc)) {
        process.stdout.write(JSON.stringify({
          decision: "force_ask",
          reason: `Doc ${doc} is locked in AGENTS.md. Confirm intentional edit.`
        }));
        process.exit(0);
      }
    }

    process.stdout.write(JSON.stringify({ decision: "allow" }));
  } catch (err) {
    process.stdout.write(JSON.stringify({ decision: "allow" }));
  }
});
