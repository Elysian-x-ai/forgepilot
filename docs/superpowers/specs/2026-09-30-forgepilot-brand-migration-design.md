# ForgePilot Brand Migration Design

**Date:** 2026-09-30  
**Status:** Approved for implementation  
**Product:** ForgePilot

## Intent

ForgePilot is the new public identity for this terminal-first Java engineering agent. The brand should make the product feel like a controlled execution cockpit: model-driven work is fast and extensible, while approvals, policy, memory, snapshots, and audit trails keep the user in control.

The migration changes the product-facing identity in one release and preserves the existing runtime and benchmark contracts needed by current users and historical evidence.

## Brand System

- **Name:** ForgePilot
- **Short mark:** `FP`
- **Terminal mark:** `◆`
- **Positioning:** A controlled engineering agent for the terminal
- **Chinese descriptor:** 可控的工程 Agent 工作台
- **Voice:** direct, calm, technically specific, and evidence-led
- **Primary color:** cobalt `#2F6BFF`
- **Approval color:** amber `#F2B84B`
- **Success color:** mint `#39D98A`
- **Surface:** graphite `#10141C`
- **Text:** cloud `#EEF2F7`

The public UI, landing page, docs website, README, startup screen, and embedded skills use the new name and visual language. Historical screenshots that are evidence artifacts remain unchanged; new assets are created under `brand/` and used by new public pages.

## Compatibility Contract

The following identifiers remain stable in this release:

- Java package namespace `com.paicli` and package-based class names
- Maven artifact coordinates `com.paicli:paicli:1.0-SNAPSHOT`
- Existing `PAICLI_*` environment variables and `paicli.*` system properties
- Existing `.paicli/` project directory and `~/.paicli/` user directory
- `PAI.md`, `PAI.local.md`, and benchmark paths/JSON evidence fields
- Existing `paicli` command and jar filename

New ForgePilot aliases are additive. `FORGEPILOT_*`, `forgepilot.*`, `.forgepilot/`, and `~/.forgepilot/` are accepted where a configuration path is touched. New values have precedence; legacy values remain the fallback. The first legacy fallback in a process may emit one concise migration notice to stderr, never a repeated notice per setting.

The initial implementation covers the public configuration examples and central runtime directory resolver. It does not rename every internal package or historical benchmark fixture; that work would be a separate breaking migration.

## Public Surface Changes

1. Maven display name and description, README title and product copy, AGENTS/PAI wording, and startup/runtime messages use ForgePilot.
2. The startup banner uses `FORGEPILOT ◆ v16.1.0` and the approved palette while retaining the existing version value.
3. Landing, documentation website, demos, PPT page, and public skill frontmatter use ForgePilot copy and the new GitHub URL.
4. A `brand/` asset kit contains an SVG mark, wordmark, social preview SVG, and a palette/readme so future surfaces do not invent new colors.
5. The GitHub repository is `Elysian-x-ai/forgepilot`, public, with a ForgePilot description and topics.

## Repository and Release Flow

The local repository is initialized on `main` with a pre-change baseline commit. Branding changes are committed in focused commits for design/compatibility, product surfaces/assets, and verification metadata. After tests pass, the GitHub repository is created and the local `main` branch is pushed with the GitHub URL configured as `origin`.

## Testing and Acceptance

- Build succeeds with `mvn clean package`.
- Branding smoke checks prove public surfaces contain ForgePilot and the startup banner contains `FORGEPILOT` and `◆`.
- Compatibility tests prove legacy and new environment/property aliases resolve in the documented precedence order.
- `mvn test -DskipTests=false` is run; pre-existing failures are recorded separately from regressions.
- `git status` is clean before publishing, and the pushed remote points to `Elysian-x-ai/forgepilot`.

## Out of Scope

- Renaming `com.paicli` packages, benchmark contracts, or artifact coordinates.
- Rewriting historical benchmark reports or screenshots.
- Running real model evaluation or Docker benchmark batches.
- Publishing a release artifact or changing the product version.
