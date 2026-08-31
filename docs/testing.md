# Testing strategy

This strategy follows the official Android testing pyramid: many small local tests, targeted component/feature tests, and a small number of high-fidelity device journeys.

## Current Phase 2 setup

- Single Android application module.
- 100% Jetpack Compose shell.
- JUnit4 and coroutine-test local dependencies.
- AndroidX instrumented JUnit and Espresso dependencies.
- Compose UI test dependencies.
- Room in-memory database test support and instrumented AppSearch `LocalStorage` coverage.
- Room schema migration fixtures for `1 -> 2`.
- WorkManager test support and provider-neutral fake OCR coverage.
- Host tests for domain/ViewModel behaviour.
- Host search fixtures for Unicode normalization, deterministic ranking, filters, excerpts, candidate extraction, and latency.
- Instrumented Room, migration, OCR repository, hostile content-provider, and Compose capture tests.
- No dependency-injection framework.
- No mocking framework, Robolectric, screenshot framework, benchmark module, or device harness yet.

Do not install a mocking framework until a fake cannot reasonably model a dependency. Introduce Hilt when runtime provider graphs and test replacement justify it, not during the empty shell phase.

## Test layers

### Unit

Fast host-side tests for pure logic:

- Capture state transitions.
- URI/type validation policies.
- Search normalisation and ranking.
- Structured AI-output validation.
- Date/location candidate handling.
- Retry and redaction policies.

Run on every commit.

### Component

Local Compose behaviour and screenshot tests after the first real components exist:

- Component states and user actions.
- State restoration.
- Light/dark themes and 1.5× font scale.
- Compact, medium, and expanded window configurations.

Prefer semantic matchers. Add a test tag only when a semantic matcher becomes unreasonably complex.

### Feature

Tests combining repositories, fakes, workers, and feature UI:

- Capture persistence before processing.
- Partial and failed enrichment.
- Search edits/re-indexing.
- Provider disabled, offline, invalid output, and explicit-consent paths.

Use in-memory fakes for slow or external dependencies.

### Instrumented/application

Run on an emulator or device where Android framework fidelity matters:

- Room migrations and in-memory SQLite behaviour.
- `ACTION_SEND` for supported MIME types.
- Temporary/revoked content URIs.
- Photo Picker and Storage Access Framework.
- Microphone and notification permission denial.
- Process recreation and queued WorkManager work.
- External intent confirmation.

### Release candidate

A small set of critical journeys against an optimised release candidate:

- Share and persist a capture.
- OCR and retrieve an image.
- Record and retrieve a voice note.
- Explicitly analyse, review, and accept an action.
- Export and permanently delete a capture.

Nothing Phone (1) is the first physical-device release gate.

## Screenshot matrix

When screen-level screenshot testing is introduced, cover the cross-product of:

- Widths: 400, 610, and 900 dp.
- Heights: 400, 500, and 1000 dp.

Additionally capture a representative phone screen in light/dark themes and at 1.5× font scale. Component screenshots cover meaningful states rather than business behaviour.

## Performance

- Baseline profile after primary navigation and capture flows stabilise.
- Macrobenchmarks for cold startup, search startup, library scrolling, and capture entry.
- Search latency fixtures for small, medium, and large synthetic libraries.
- OCR memory-pressure tests with bounded large images.
- Physical-device measurements for battery, thermal, and release-candidate performance.

The current host-side ranking-merge fixture covers 5,000 synthetic AppSearch hits with a deliberately generous 2,000 ms regression ceiling. AppSearch performs lexical matching, field-weighted relevance ranking, paging, and snippets off the main thread. This is a repeatable regression fixture, not a physical-device performance claim.

## Commands

Local verification gate:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Compile instrumented tests without claiming device validation:

```bash
./gradlew assembleDebugAndroidTest
```

Instrumented tests when an emulator/device is intentionally selected:

```bash
./gradlew connectedDebugAndroidTest
```

Never report device validation from compilation alone.

## Hosted verification

Clasp does not currently configure a hosted Android workflow. Contributors run the local verification gate before opening a pull request. Device tests and release-candidate journeys run only when an emulator or supported device is intentionally selected.

## Fixtures

Only synthetic captures belong in the repository. Test fixtures must not contain personal screenshots, real recordings, credentials, private URLs, contacts, or user documents.
