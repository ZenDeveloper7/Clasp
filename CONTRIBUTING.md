# Contributing to Clasp

Thank you for helping make Clasp useful on unsupported Nothing and CMF phones.

## Before starting

- Search existing issues and pull requests.
- Open an issue before large architecture, permission, provider, data-schema, or visual changes.
- Never include real screenshots, recordings, documents, contacts, credentials, or user queries in fixtures or reports.
- Read [AGENTS.md](AGENTS.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/PRIVACY.md](docs/PRIVACY.md), and [DESIGN.md](DESIGN.md).

## Development

Use Android 12/API 31 or newer and the checked-in Gradle wrapper.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Add tests at the lowest reliable layer. Prefer fakes over mocks, and do not add a dependency-injection or mocking framework without a concrete need.

## Pull requests

- Keep the change focused.
- Describe user-visible behaviour and privacy/permission effects.
- State exactly what was compiled or tested.
- Update data-flow, permission, privacy, architecture, design, or testing documentation when the contract changes.
- Add a Room migration after persistent public schemas exist.
- Do not weaken explicit remote-analysis consent.

By submitting a contribution, you agree that it is licensed under Apache License 2.0.
