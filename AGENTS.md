# Clasp agent guidance

Clasp handles potentially sensitive screenshots, recordings, documents, search queries, and API credentials. Preserve its local-first and explicit-consent boundaries in every change.

## Working rules

- Inspect the current branch, HEAD, worktree, Gradle setup, and affected flow before editing.
- Preserve unrelated worktree changes.
- Keep core functionality usable without network access, Firebase, AI credentials, contacts, calendar, notifications, or Accessibility Service.
- Store original captures before enrichment starts; processing failure must never delete the original.
- Never log capture text, OCR output, transcripts, search queries, filenames, content URIs, user identifiers, provider keys, or raw provider responses in release builds.
- Remote AI processing requires an explicit user action and visible provider/content disclosure.
- Do not copy Nothing logos, proprietary fonts, Glyph assets, or Essential Space layouts.
- Use original Clasp resources that implement the direction in [DESIGN.md](DESIGN.md).
- Prefer Android platform pickers and share contracts over broad storage permissions.
- Accessibility Service is not a core dependency and requires a separate policy/privacy review.
- Use date versions in `YYYY.MM.DD.PATCH` form.

## Verification

Follow [docs/testing.md](docs/testing.md). Report validation precisely as static-only, compiled, unit-tested, linted, instrumented, or device-tested.

Do not claim emulator or Nothing-device validation unless it was actually run.
