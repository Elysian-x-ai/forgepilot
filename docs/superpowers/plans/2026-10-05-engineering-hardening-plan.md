# ForgePilot Engineering Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate product and benchmark verification, reduce `Main`/`ToolRegistry` responsibility concentration, add deterministic CI/E2E coverage, and align release/status documentation.

**Architecture:** Keep the existing single Maven project and public APIs. Add explicit Surefire include/exclude profiles, extract cohesive collaborators behind compatibility delegates, use a local deterministic mock OpenAI-compatible server for one ReAct tool round, and centralize public version/evaluation status in repository properties/manifest consumed by docs and tests.

**Tech Stack:** Java 17, Maven Surefire, JUnit 5, JLine, Jackson, OkHttp/Java HTTP server, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-05-engineering-hardening-design.md`

## Global Constraints

- Preserve `com.paicli`, `.paicli`, `PAICLI_*`, benchmark identifiers, existing tool names, policy order, and CLI behavior.
- `mvn clean package` keeps historical `skipTests=true`; product validation is explicit through `-Pcore`/`-Pquick`, and benchmark is explicit through `-Pbenchmark`.
- Product profiles must not select `com.paicli.eval.benchmark`; benchmark profile must select only that package.
- Do not run real provider calls or Docker benchmark episodes as part of implementation verification.
- `Main` and `ToolRegistry` extraction must preserve existing constructors and `executeTools()` behavior.
- Public evaluation state is `25/28`, `88/100`, `NOT_INTEGRATED`, `formalScores=null`, `publishable=false` until the repository itself changes that state.

## Review Focus

- A test class under `com.paicli.eval.benchmark` must never be selected by `core` or `quick`; pin with Surefire reports and a profile smoke test.
- A mock LLM response must produce exactly one allowed tool call and one final answer without reading API keys or making network calls; pin with a deterministic E2E test.
- Existing callers constructing `ToolRegistry` or `Main` must still compile and preserve policy ordering; pin with targeted tool/policy/CLI tests.
- Version/status copies must reject drift; pin with a consistency test over `pom.xml`, Banner, README/AGENTS and the status manifest.
- Platform-specific Seatbelt failures must not make the portable product profile red; pin with an explicit tagged/excluded test profile.

### Task 1: Maven profile separation and baseline tests

**Files:**
- Modify: `pom.xml:20-270`
- Modify: `README.md` testing section
- Create: `src/test/java/com/paicli/build/MavenProfileContractTest.java` (or an equivalent deterministic profile contract check if Maven cannot be inspected from JUnit)
- Test: existing product test suites and selected Surefire reports

**Interfaces:**
- Produces profiles named `core`, `quick`, and `benchmark` with exact package selection rules.
- Keeps default package behavior and existing `phase16-smoke` profile compatible.

- [ ] **Step 1: Add profile contract assertions or a shell-verifiable profile test** for the non-overlap of `eval.benchmark` and product profiles.
- [ ] **Step 2: Configure Surefire includes/excludes** so `core` excludes benchmark and platform-only Seatbelt JVM tests, `quick` is a bounded subset of core, and `benchmark` includes only `**/eval/benchmark/**`.
- [ ] **Step 3: Update README commands** with the exact core/quick/benchmark commands and explain that benchmark is opt-in.
- [ ] **Step 4: Run `mvn -q test -Pcore` and `mvn -q test -Pquick`**, recording counts and remaining platform failures; run only profile selection checks for benchmark without executing long replay unless explicitly requested.
- [ ] **Step 5: Commit** `test: isolate product and benchmark profiles`.

### Task 2: Extract Main and ToolRegistry collaborators

**Files:**
- Create: `src/main/java/com/paicli/cli/CliCommandDispatcher.java`
- Create: `src/main/java/com/paicli/cli/SessionBootstrap.java`
- Create: `src/main/java/com/paicli/tool/BuiltinToolRegistrar.java`
- Create: `src/main/java/com/paicli/tool/ToolExecutionPolicy.java`
- Modify: `src/main/java/com/paicli/cli/Main.java`
- Modify: `src/main/java/com/paicli/tool/ToolRegistry.java`
- Test: `src/test/java/com/paicli/cli/*Test.java`, `src/test/java/com/paicli/tool/*Test.java`, `src/test/java/com/paicli/hitl/*Test.java`

**Interfaces:**
- `CliCommandDispatcher.dispatch(String, CliCommandContext)` routes parsed commands and returns the existing command result/handled signal used by `Main`.
- `SessionBootstrap.create(...)` owns renderer/HITL/sandbox/MCP/agent assembly but does not change lifecycle ownership.
- `BuiltinToolRegistrar.registerInto(ToolRegistry)` registers the existing built-ins.
- `ToolExecutionPolicy.executeBatch(...)` is the internal policy-aware implementation used by `ToolRegistry.executeTools()`; it preserves result order and parallel-safe whitelist behavior.

- [ ] **Step 1: Add characterization tests** for command routing, ToolRegistry construction, parallel read-only ordering, serial write ordering, and policy rejection before extraction.
- [ ] **Step 2: Move command routing and session assembly** into the new CLI collaborators, leaving `Main` as a thin compatibility coordinator.
- [ ] **Step 3: Move built-in registration and batch execution policy** into the new tool collaborators, leaving `ToolRegistry` as the public facade.
- [ ] **Step 4: Run the targeted CLI/tool/policy suites** and inspect public method signatures for compatibility.
- [ ] **Step 5: Commit** `refactor: split cli and tool orchestration responsibilities`.

### Task 3: Deterministic mock LLM E2E and CI

**Files:**
- Create: `src/test/java/com/paicli/e2e/MockLlmServer.java`
- Create: `src/test/java/com/paicli/e2e/MockLlmReActE2ETest.java`
- Create: `.github/workflows/ci.yml`
- Modify: `pom.xml` only if a test-scope HTTP dependency or profile hook is required
- Modify: `README.md` with the local demo command and CI badges/description

**Interfaces:**
- `MockLlmServer` exposes a random local port, request counter, received model/request summaries, deterministic tool-call response, and `close()`.
- The E2E test uses an injected `LlmClient`/factory endpoint and a temporary workspace; it asserts one tool call, one final answer, stable request count, and no API key dependency.

- [ ] **Step 1: Write the failing E2E test** with a fixed `read_file` tool call and final response.
- [ ] **Step 2: Implement the mock server** using existing test dependencies or JDK HTTP server; reject unexpected requests and external URLs.
- [ ] **Step 3: Make the E2E test pass** through the normal ReAct path, without bypassing ToolRegistry.
- [ ] **Step 4: Add CI** for Java 17 compile, core, quick, and E2E; keep benchmark manual/opt-in.
- [ ] **Step 5: Run the E2E twice and execute the same CI commands locally**; commit `test: add deterministic mock llm e2e and ci`.

### Task 4: Version and evaluation status consolidation

**Files:**
- Modify: `pom.xml` project version property
- Modify: `src/main/resources/**` Banner/version source and corresponding test
- Create: `benchmarks/paicli-native-agentbench-v0.1/status.json` (or the repository’s existing machine-readable status location)
- Modify: `AGENTS.md`, `README.md`, `ROADMAP.md` only where status/version claims are duplicated
- Create: `src/test/java/com/paicli/release/ReleaseStatusConsistencyTest.java`

**Interfaces:**
- The status manifest contains version, prototype count, original weight, `formalScores`, `publishable`, and integration state.
- The consistency test checks the canonical values and fails on stale `24/28`/`84/100` or contradictory release strings.

- [ ] **Step 1: Add the canonical version/status source and a failing consistency test** covering Banner, Maven, AGENTS, README, and benchmark status.
- [ ] **Step 2: Update all public copies** to the canonical `v16.1.0`/`1.0-SNAPSHOT` compatibility mapping and current `25/28`, `88/100`, `NOT_INTEGRATED`, `formalScores=null`, `publishable=false` status, clearly labeling the Maven snapshot.
- [ ] **Step 3: Run consistency and targeted documentation tests**, then inspect `git diff` for unsupported score claims.
- [ ] **Step 4: Commit** `docs: align release and benchmark status`.

### Final verification

- [ ] Run `mvn -q -Pcore -DskipTests=false test`.
- [ ] Run `mvn -q -Pquick -DskipTests=false test`.
- [ ] Run the mock E2E twice with no provider keys.
- [ ] Run `mvn -q -DskipTests package`.
- [ ] Verify `mvn -q -Pbenchmark -DskipTests=false test -Dtest=...` selects benchmark only without launching real provider/Docker work.
- [ ] Review `git diff --check`, `git status`, and the final CI workflow.

