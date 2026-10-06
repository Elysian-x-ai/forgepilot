# ForgePilot configuration and API reference

This reference lists the supported ForgePilot configuration aliases and Runtime API headers.

## Configuration identifiers

The following identifiers are supported by the current runtime and project tooling:

- `com.paicli` Java packages and `com.paicli:paicli` Maven coordinates
- `PAICLI_*` environment variables and `paicli.*` system properties
- `.paicli/`, `~/.paicli/`, and `PAI.md` project memory files
- `paicli` jar/command names and benchmark evidence paths
- `X-PaiCLI-API-Key` for Runtime API clients

For the renderer and Runtime API settings documented below, the `FORGEPILOT_*` or `forgepilot.*` value has priority when both names are set. The corresponding `PAICLI_*` and `paicli.*` values remain supported as fallbacks.

## Renderer aliases

Renderer configuration:

```bash
FORGEPILOT_RENDERER=inline java -jar target/paicli-16.1.0.jar
```

The `PAICLI_*` form is also supported:

```bash
PAICLI_RENDERER=inline java -jar target/paicli-16.1.0.jar
```

The same precedence rule applies to the system property pair `forgepilot.renderer` and `paicli.renderer`.

## Runtime API aliases

Configure the Runtime API with `FORGEPILOT_RUNTIME_API_KEY` or
`-Dforgepilot.runtime.api.key`. `PAICLI_RUNTIME_API_KEY` and
`-Dpaicli.runtime.api.key` remain valid fallbacks.

## Runtime API header

Runtime API clients may send:

```text
X-ForgePilot-API-Key: <key>
```

The `X-PaiCLI-API-Key` header is also accepted.
