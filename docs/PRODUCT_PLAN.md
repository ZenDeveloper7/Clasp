# Clasp product plan

## Definition

Clasp is a standalone, local-first Android companion for Nothing and CMF phones that do not support Nothing Essential Space. It captures information intentionally shared or created by the user, preserves the original, derives searchable text and optional insights, and helps the user retrieve or act on that information later.

Clasp is unofficial and independent. It does not modify Nothing Launcher, use Nothing Account, emulate an Essential Key, or claim privileged access to Nothing OS.

## Audience

Initial supported-device validation covers:

- Nothing Phone (1)
- Nothing Phone (2)
- Nothing Phone (2a)
- Nothing Phone (2a) Plus
- CMF Phone 1

The minimum platform is Android 12/API 31. Other Android 12+ devices are not blocked but remain community-supported until they join the test matrix.

## Product loop

1. **Capture** text, a link, image, screenshot, file, or voice note.
2. **Preserve** the original before starting enrichment.
3. **Understand** it through local metadata/OCR and optional explicit AI analysis.
4. **Act** on reviewed tasks, events, links, locations, and contact details.
5. **Find** it through fast keyword search and later semantic retrieval.

## Principles

### Local first

Core capture, storage, library, OCR, and keyword search work without an account or network connection.

### Capture before organisation

Saving does not require a title, tag, folder, summary, or AI provider.

### Originals are authoritative

Generated titles, summaries, transcripts, and actions are editable derived data. They never replace the source.

### Explicit remote analysis

Adding a Gemini key does not automatically upload captures. The user taps **Analyse** for an individual capture and sees which content category and provider will receive it.

### Permissions degrade independently

Denied microphone, notification, contacts, calendar, or other permissions cannot break unrelated Clasp features.

### Original public identity

Clasp may visually complement Nothing OS, but uses its own app name, icon, assets, components, layouts, and design specification.

## MVP

### Capture

- In-app text notes.
- In-app image selection.
- In-app voice-note recording.
- Android Share target for text and links.
- Android Share target for a single image or supported file.
- Import screenshots with the system screenshot Share action or Photo Picker.

### Storage and library

- Durable capture database.
- App-private attachment storage.
- Capture history and details.
- Processing status and retry state.
- Favourite, edit, export, and permanent delete.

### Local understanding

- MIME and metadata extraction.
- Latin-script OCR for images.
- Deterministic link, email, phone, and date candidates.
- Unicode-safe keyword index.

### Search

- Search user titles and notes.
- Search original/shared text.
- Search OCR and transcripts.
- Search generated summaries, tags, and accepted actions.
- Filters for type, date, state, favourite, and collection.
- Result excerpts that explain the match.

### Optional intelligence

- Provider-neutral OCR, transcription, insight, and embedding interfaces.
- Gemini Developer API as the first BYOK provider.
- Explicit per-capture **Analyse** action.
- Versioned structured output.
- Editable suggested titles, summaries, tasks, events, links, and locations.
- Provider activity history without raw content logs.

## Not in the MVP

- Automatic background cloud analysis.
- Accessibility-assisted screen capture.
- Semantic/vector search.
- Installed-app, contacts, calendar, or media indexing.
- Quick Settings tile, widgets, or app shortcuts.
- Automatic Smart Collections.
- Speaker-aware meeting transcription.
- Clasp-managed accounts or backend.
- Cloud backup, cross-device sync, or Nothing Account integration.

## Information architecture

### For You

- Captures requiring review.
- Accepted tasks and upcoming events.
- Recently completed and partially failed processing.
- Later: resurfaced relevant captures.

### Library

- Complete capture history.
- Type/date/state filters.
- Favourites and collections.

### Search

- Immediate keyword results.
- Transparent result groups and match explanations.
- Later: semantic and permission-backed device sources.

### Capture

- Text, voice, image, and file entry points.

### Settings

- AI provider and consent.
- Searchable sources and permissions.
- Privacy, storage, export, diagnostics, and Crashlytics opt-in.
- Appearance, accessibility, licences, and About disclaimer.

## Delivery phases

### Phase 0 — Public foundation

- Initialise and publish the repository.
- Establish licence, contribution, security, product, architecture, privacy, design, and testing documentation.
- Set Android 12/API 31 minimum and `YYYY.MM.DD.PATCH` versioning.
- Disable system backup while capture backup semantics remain undefined.
- Document the local build, lint, and unit-test gate.
- Keep the app as a project shell; no feature implementation.

### Phase 1 — Durable capture and library

Implementation status: complete except for device-flow validation. The non-Firebase capture, storage, library, detail, export, and deletion slice is implemented; device-flow validation remains before a release is cut.

- Capture model, Room schema, migrations, and app-owned file storage.
- Text/image/file capture and Share receiver.
- Library, detail, processing states, export, and permanent deletion.
- Add Firebase Crashlytics after the Firebase project is provided:
  - single Firebase project;
  - configuration checked into the project;
  - Firebase-only API-key restrictions;
  - automatic collection disabled;
  - explicit user opt-in;
  - no Analytics or content-bearing logs;
  - available to project builds, including contributor debug builds.

Firebase is explicitly excluded from the current Phase 1 implementation pass and is not an exit-criteria blocker for the local capture slice.

Exit criteria:

- Every accepted capture is stored before enrichment begins.
- Temporary shared URIs are copied safely.
- Process death and enrichment failure do not lose originals.
- Core flows work offline.

### Phase 2 — OCR and keyword search

Implementation status: complete in the current `2026.08.31.0` development build, with instrumented migration/OCR execution pending a selected emulator or physical device.

- OCR capability and deterministic extraction.
- Search projection, query normalisation, filters, ranking, and excerpts.
- Search relevance fixtures and latency baseline.

Implemented decisions:

- Bundled ML Kit Latin text recognition so OCR is available offline on first use.
- Unique WorkManager jobs per image capture, with explicit pending/running/complete/empty/failed state and retry.
- AndroidX AppSearch `LocalStorage` for an app-private, offline, rebuildable keyword index; Room remains authoritative.
- AppSearch relevance ranking with deterministic weights across title, note, original, OCR, and attachment name.
- Type, favourite, age, and OCR-state filters plus matched-field excerpts.
- Deterministic link, email, phone, and date candidates; these are labelled suggestions, not generated facts.
- Host ranking-merge regression fixture with 5,000 synthetic AppSearch hits and a 2,000 ms ceiling. Instrumented AppSearch execution and device benchmarking remain release work.

Exit criteria:

- Image text becomes searchable.
- Index edits/deletion remain consistent.
- Representative ranking tests pass.

### Phase 3 — Voice and reviewed actions

- Voice recorder lifecycle and local audio storage.
- Transcription abstraction and correction UI.
- Suggested task/event/link/location model.
- User-confirmed Calendar, Maps, email, call, and browser intents.

### Phase 4 — Explicit BYOK analysis

- Gemini adapter and fake provider.
- Keystore-backed Gemini credential storage.
- Per-capture consent and structured insight validation.
- AI activity history and redacted diagnostics.

### Phase 5 — Semantic and device search

- Embedding index and hybrid ranking.
- Installed-app search.
- Optional contacts and calendar sources.
- Independent source permissions and failure states.

### Phase 6 — Convenience and public beta

- Quick Settings, widgets, and app shortcuts.
- Deliberately designed backup/export and verified restore.
- Baseline profile, macrobenchmarks, accessibility review, and release signing.
- GitHub release first; Play Store later.

### Separate research track — advanced screenshot capture

Accessibility Service is never required for core operation. Any future screenshot helper needs a separate value comparison, disclosure design, Play policy review, secure-window handling, and decision on whether it belongs only in sideloaded builds.

## MVP acceptance criteria

1. Share text, a link, an image, and a supported file from another app.
2. Create text, image, and voice captures inside Clasp.
3. Store originals durably before enrichment.
4. Search image OCR, transcripts, titles, source text, and notes.
5. Clearly separate original, edited, and generated content.
6. Remain usable without AI setup or a network connection.
7. Never trigger remote analysis without the **Analyse** action.
8. Expose partial and failed processing safely with appropriate retry.
9. Export and permanently delete an individual capture.
10. Require no broad file access or Accessibility Service.
11. Pass unit, database, key instrumentation, lint, and build gates.
12. Complete a documented validation pass on Nothing Phone (1).

## Versioning

Clasp uses calendar versions:

```text
YYYY.MM.DD.PATCH
```

The patch number begins at 1 for the first version produced on a date and increments for additional published versions that day. The Phase 0 baseline is `2026.08.30.1`.

## Success indicators

- Time to persist a capture.
- Percentage of captures reaching ready or useful partial state.
- Successful retrieval within the first few results for a curated query set.
- OCR/transcription correction rate.
- Suggested-action accept/edit/dismiss rate.
- Search latency and startup performance on Nothing Phone (1).
- Crash-free capture and processing sessions from users who explicitly enable Crashlytics.

No captured content becomes analytics data.
