# Thaumic Tinkerer porting rules

- This directory contains the addon port for Minecraft 1.21.1, using the existing Thaumcraft port in the main `repo` directory.
- The reference is Thaumic Tinkerer 2.5-1.7.10-164, SHA-1 `e50a70d3f65bdfb59c2fef2db2d6515aa1e640f1`. Do not substitute another release.
- Keep decompiled material, the original JAR, caches and tooling reports outside this directory. Do not include them in Git history or the gameplay JAR.
- Preserve original values, registration order and observed behavior. Discuss gameplay changes and unavoidable deviations with the user first, and record the agreed decision.
- Preserve original authorship and the CC BY-NC-SA 3.0 terms stated in the original source README. Permission to publish Thaumcraft does not follow from the Tinkerer license.
- Accepted user decisions (2026-10-18): the permanent mod ID/namespace is `thaumictinkerer`; the addon uses only Thaumcraft's public API. Missing contracts may be added to the API and core implementation as required by the port. Do not request these approvals again. Ask before making other unagreed product or architecture decisions.
- Copying and adapting original code is explicitly authorized by the user. Preserve reference behavior and attribution; rewrite platform-specific code as needed.
- Use PowerShell. Do not run CMD.exe or `.bat` files without permission. Do not create commits, worktrees or publications unless requested. Do not use subagents unless requested.
- Restrict searches to the relevant directory. Do not scan the whole project without confirmation.
- Report implementation, compilation, automated verification and real-client verification separately. An existing class or successful decompilation does not prove that the port works.
- Keep agreement logs and tooling reports out of Git.
- The active user goal is to port ALL content from build 164, fully compare it with the original, then hand it over for detailed personal acceptance. Do not stop after individual mechanics. Subagents are prohibited by the user's explicit instruction. A partial implementation does not complete this goal.
- Write AGENTS.md files in English and preserve the meaning of their instructions when translating them.
