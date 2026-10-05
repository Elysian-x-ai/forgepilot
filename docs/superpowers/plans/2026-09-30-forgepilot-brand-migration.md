# ForgePilot Brand Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the public PaiCLI identity with ForgePilot while preserving runtime, benchmark, and user configuration compatibility.

**Architecture:** Keep `com.paicli`, `.paicli`, `PAICLI_*`, and benchmark identifiers as stable compatibility contracts. Add a small central branding/alias layer for display text and configuration precedence, then update public surfaces and add a standalone asset kit.

**Tech Stack:** Java 17, Maven, JUnit 5, Markdown, standalone HTML/CSS/JS, SVG.

**Spec:** `docs/superpowers/specs/2026-09-30-forgepilot-brand-migration-design.md`

## Global Constraints

- Public product name is `ForgePilot`; terminal mark is `◆`; positioning is `A controlled engineering agent for the terminal`.
- Legacy `com.paicli`, `.paicli`, `PAICLI_*`, `paicli.*`, `PAI.md`, and benchmark contracts remain functional.
- New `FORGEPILOT_*`, `forgepilot.*`, `.forgepilot/`, and `~/.forgepilot/` aliases take precedence where implemented.
- Do not change benchmark execution, real model calls, Docker evaluation, or historical evidence.
- Do not commit `.env`, API keys, or `target/` artifacts.

## Review Focus

- A new alias and a legacy alias both present: the ForgePilot value wins deterministically.
- Only the legacy alias is present: behavior remains unchanged and migration notice is emitted at most once per process.
- Public copy in landing/docs/demo and the startup banner: no stale visible PaiCLI brand remains in the new surfaces.
- Historical benchmark and golden-set identifiers: paths and serialized evidence remain byte-compatible.
- GitHub publication: the remote is the requested owner/repository and the pushed branch is `main`.

### Task 1: Brand constants, alias resolution, and tests

**Files:**
- Create: `src/main/java/com/paicli/brand/ForgePilotBrand.java`
- Create: `src/test/java/com/paicli/brand/ForgePilotBrandTest.java`
- Modify: `src/main/java/com/paicli/cli/Main.java`
- Modify: `src/main/java/com/paicli/config/PaiCliConfig.java`

**Interfaces:**
- `ForgePilotBrand.PRODUCT_NAME`, `MARK`, `POSITIONING`, `public static String display(String legacyText)`.
- `ForgePilotBrand.firstNonBlankProperty(String newProperty, String oldProperty)`.
- `ForgePilotBrand.firstNonBlankEnv(String newEnv, String oldEnv)`.
- `ForgePilotBrand.legacyNotice(String legacyKey, String newKey)` returns a once-per-process notice.

- [ ] Write tests for product constants, new-over-legacy precedence, legacy fallback, and one-time notice behavior.
- [ ] Run `mvn test -Dtest=ForgePilotBrandTest -DskipTests=false` and verify the new tests fail before implementation.
- [ ] Implement the constants and alias helpers without changing existing provider behavior.
- [ ] Route the startup banner and user-facing main entry messages through the brand constants.
- [ ] Add only the central `FORGEPILOT_*`/`forgepilot.*` aliases that are used by the migration examples; keep all legacy lookups intact.
- [ ] Run the focused test again and commit `feat: add ForgePilot brand compatibility layer`.

### Task 2: Public product surfaces and asset kit

**Files:**
- Create: `brand/forgepilot-mark.svg`
- Create: `brand/forgepilot-wordmark.svg`
- Create: `brand/forgepilot-social-preview.svg`
- Create: `brand/README.md`
- Modify: `pom.xml`, `README.md`, `ROADMAP.md`, `AGENTS.md`, `PAI.md`, `.env.example`
- Modify: `landing/index.html`, `docs/website/index.html`, `docs/website/script.js`, `docs/website/styles.css`, `demo-step/index.html`, `demo-deepseek/index.html`, `ppt/index.html`
- Modify: `src/main/resources/skills/better-harness/SKILL.md`, `src/main/resources/skills/web-access/SKILL.md`

**Interfaces:**
- Public pages reference `brand/forgepilot-mark.svg` or their existing inline equivalent and use the ForgePilot palette.
- README includes the new GitHub URL `https://github.com/Elysian-x-ai/forgepilot` and a migration note.

- [ ] Add the SVG mark, wordmark, social preview, and asset README with exact palette values.
- [ ] Replace visible product copy, old repository links, old install links, and π branding in public surfaces; keep historical benchmark links and paths explicitly labeled.
- [ ] Update Maven display metadata and environment examples with ForgePilot-first wording and aliases.
- [ ] Run a scripted public-surface scan that fails on old visible brand tokens outside compatibility/history sections.
- [ ] Commit `feat: rebrand public surfaces as ForgePilot`.

### Task 3: Documentation, compatibility reference, and release metadata

**Files:**
- Create: `docs/forgepilot-compatibility.md`
- Modify: `README.md`, `docs/articles/README.md`, selected article titles/links, `.gitignore` if needed
- Modify: GitHub metadata through `gh repo create`, `gh repo edit`, and `git remote add origin`

**Interfaces:**
- Compatibility reference documents ForgePilot-first configuration precedence and stable paths.
- Remote repository is public, named `forgepilot`, has ForgePilot description and relevant topics.

- [ ] Write the compatibility reference with shell examples for aliases and the stable compatibility contract.
- [ ] Update the article index and public links without rewriting historical benchmark records.
- [ ] Run Maven package and focused regression tests; record pre-existing failures separately.
- [ ] Create the GitHub repository, push `main`, set description/topics, and verify the remote URL.
- [ ] Commit `chore: publish ForgePilot repository metadata` if local metadata changes remain.

### Task 4: Final acceptance and clean handoff

- [ ] Run `git diff --check`, public brand scan, `mvn clean package`, and `mvn test -DskipTests=false`.
- [ ] Verify `git status --short`, branch, commit list, and GitHub repository metadata.
- [ ] Report changed files, compatibility behavior, test results, known pre-existing failures, and the repository URL for user acceptance.
