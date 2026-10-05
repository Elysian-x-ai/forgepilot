# ForgePilot compatibility reference

ForgePilot is the terminal-first Java engineering agent. Its public product name, positioning, documentation, landing pages, and startup experience use ForgePilot consistently. The stable identifiers below remain available so existing projects and benchmark evidence continue to work.

## Product identity

- The product is named **ForgePilot**.
- The startup mark is `◆` and the positioning is “A controlled engineering agent for the terminal”.
- The canonical repository is [Elysian-x-ai/forgepilot](https://github.com/Elysian-x-ai/forgepilot).
- Examples use ForgePilot wording and the ForgePilot configuration aliases where available.

## Stable compatibility identifiers

The following identifiers remain stable for existing projects, integrations, and benchmark evidence:

- `com.paicli` Java packages and `com.paicli:paicli` Maven coordinates
- `PAICLI_*` environment variables and `paicli.*` system properties
- `.paicli/`, `~/.paicli/`, and `PAI.md` project memory files
- `paicli` jar/command names and benchmark evidence paths
- `X-PaiCLI-API-Key` for Runtime API clients

Where a setting has a ForgePilot alias, the ForgePilot name wins when both values are present. The compatible `PAICLI_*` or `paicli.*` name remains a fallback so existing shell profiles and CI jobs keep working.

## Renderer aliases

Use the ForgePilot name in scripts:

```bash
FORGEPILOT_RENDERER=inline java -jar target/paicli-16.1.0.jar
```

The compatible form remains valid:

```bash
PAICLI_RENDERER=inline java -jar target/paicli-16.1.0.jar
```

The same precedence rule applies to the system property pair `forgepilot.renderer` and `paicli.renderer`.

## Runtime API aliases

Configure the Runtime API with `FORGEPILOT_RUNTIME_API_KEY` or
`-Dforgepilot.runtime.api.key`. `PAICLI_RUNTIME_API_KEY` and
`-Dpaicli.runtime.api.key` remain valid fallbacks.

## Runtime API header

New clients may use:

```text
X-ForgePilot-API-Key: <key>
```

The compatible `X-PaiCLI-API-Key` header is still accepted for existing integrations.

## Historical records

Benchmark reports, golden-set paths, package names, and old screenshots retain their original identifiers for reproducibility. They are historical evidence, not the current product name.
