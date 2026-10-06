# ForgePilot Brand Design

**Date:** 2026-09-30  
**Status:** Approved for implementation  
**Product:** ForgePilot

## Intent

ForgePilot is a terminal-first Java engineering agent. The brand should make the product feel like a controlled execution cockpit: model-driven work is fast and extensible, while approvals, policy, memory, snapshots, and audit trails keep the user in control.

Product introductions present ForgePilot directly. Configuration details belong in technical references; runtime and benchmark contracts remain stable.

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

The public UI, landing page, docs website, README, startup screen, and embedded skills use ForgePilot and its visual language. Historical screenshots that are evidence artifacts remain unchanged; assets under `brand/` are used by public pages.

## Compatibility Contract

The following identifiers remain stable in this release:

- Java package namespace `com.paicli` and package-based class names
- Maven artifact coordinates `com.paicli:paicli:16.1.0`
- Existing `PAICLI_*` environment variables and `paicli.*` system properties
- Existing `.paicli/` project directory and `~/.paicli/` user directory
- `PAI.md`, `PAI.local.md`, and benchmark paths/JSON evidence fields
- Existing `paicli` command and jar filename

ForgePilot configuration aliases are additive and take precedence where implemented. `FORGEPILOT_RENDERER` and `forgepilot.renderer` select the renderer; `FORGEPILOT_RUNTIME_API_KEY` and `forgepilot.runtime.api.key` configure the Runtime API. The corresponding `PAICLI_*` and `paicli.*` settings remain valid fallbacks. Do not advertise aliases for paths or settings the implementation does not accept.

Public configuration examples must match the implemented lookup rules. Internal packages, storage directories, and historical benchmark fixtures keep their stable identifiers.

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
