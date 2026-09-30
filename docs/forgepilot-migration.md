# ForgePilot migration guide

ForgePilot is the new public name for the terminal-first Java engineering agent previously presented as PaiCLI. This release changes the product identity, documentation, landing pages, and startup experience while preserving the identifiers that existing projects and benchmark evidence rely on.

## What changes

- The product is named **ForgePilot**.
- The startup mark is `◆` and the positioning is “A controlled engineering agent for the terminal”.
- The canonical repository is [Elysian-x-ai/forgepilot](https://github.com/Elysian-x-ai/forgepilot).
- New examples use the ForgePilot aliases introduced in this release and ForgePilot wording.

## What stays compatible

Existing installations continue to use these stable identifiers:

- `com.paicli` Java packages and `com.paicli:paicli` Maven coordinates
- `PAICLI_*` environment variables and `paicli.*` system properties
- `.paicli/`, `~/.paicli/`, and `PAI.md` project memory files
- `paicli` jar/command names and benchmark evidence paths
- `X-PaiCLI-API-Key` for Runtime API clients

Where the setting has a ForgePilot alias, the new name wins when both values are present. The legacy name is used as a fallback so existing shell profiles and CI jobs keep working.

## Renderer aliases

Use the new name in new scripts:

```bash
FORGEPILOT_RENDERER=inline java -jar target/paicli-1.0-SNAPSHOT.jar
```

The existing form remains valid:

```bash
PAICLI_RENDERER=inline java -jar target/paicli-1.0-SNAPSHOT.jar
```

The same precedence rule applies to the system property pair `forgepilot.renderer` and `paicli.renderer`.

## Runtime API aliases

New deployments may configure the Runtime API with `FORGEPILOT_RUNTIME_API_KEY` or
`-Dforgepilot.runtime.api.key`. Existing `PAICLI_RUNTIME_API_KEY` and
`-Dpaicli.runtime.api.key` settings remain valid fallbacks.

## Runtime API header

New clients may use:

```text
X-ForgePilot-API-Key: <key>
```

The old `X-PaiCLI-API-Key` header is still accepted for existing integrations.

## Historical records

Benchmark reports, golden-set paths, package names, and old screenshots retain their original identifiers for reproducibility. They are historical evidence, not the current product name.
