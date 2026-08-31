# Architecture

## System boundary

Clasp is one standalone Android application. It consumes public Android contracts—Sharesheet, Photo Picker, Storage Access Framework, intents, and runtime permissions—and does not depend on Nothing launcher internals or privileged system APIs.

## Initial structure

Remain a single `:app` module while contracts are evolving. Use package boundaries and extract Gradle modules only when compile isolation, ownership, or reusable implementations justify the cost.

```text
com.zen.clasp
  capture/
  library/
  search/
  foryou/
  detail/
  settings/
  data/
    database/
    files/
    repository/
  processing/
    ocr/
    transcription/
    insights/
    embeddings/
  device/
    apps/
    contacts/
    calendar/
  share/
  common/
    model/
    ui/
    util/
```

## Phase 1 and 2 implementation

The current single-module implementation contains:

- `model`: capture, attachment, processing, extraction, and deletion domain state;
- `data`: Room entities/DAO/database, explicit migrations, repository policy, and app-private attachment storage;
- `processing`: provider-neutral OCR contract, bundled ML Kit implementation, and unique WorkManager jobs;
- `search`: app-private AppSearch indexing, Unicode query normalization, deterministic filters/ranking, excerpts, and candidate extraction;
- `MainViewModel`: lifecycle-aware operation coordination and user-safe error messages;
- `MainActivity`: Compose capture, library, and detail surfaces plus platform picker/share contracts;
- `ClaspApplication`: the deliberately small manual application container.

Room schema JSON is exported under `app/schemas`. Schema version 1 is the first public database baseline. Version 2 adds extraction state through an explicit `MIGRATION_1_2`; every future schema change must also add and device-test an explicit migration before release. AppSearch is a derived index rather than part of the authoritative Room schema.

## Technology baseline

- Kotlin, Coroutines, and Flow.
- Jetpack Compose and Material 3.
- Room for durable relational state.
- App-private files for attachments.
- WorkManager for durable, deferrable enrichment.
- Android Photo Picker and Storage Access Framework.
- Android Sharesheet receive contracts.
- AndroidX AppSearch `LocalStorage` for the app-private Phase 2 keyword index.
- Android Keystore for provider credentials and encryption keys.

Do not add dependency injection solely to satisfy an architecture diagram. Introduce Hilt before complex runtime fakes or provider graphs make manual construction materially worse.

## Layer responsibilities

### UI

- Renders immutable state and sends user actions.
- Does not perform direct database, file, or provider operations.
- Displays original, edited, and generated content distinctly.

### Application/domain

- Coordinates capture, processing, search, and user-confirmed actions.
- Defines policy such as explicit remote analysis and deletion.
- Owns provider-neutral capability contracts.

### Data

- Owns database transactions and attachment lifecycle.
- Maintains search projections.
- Reconciles AppSearch IDs and content revisions from Room after startup or a derived-index failure, updating only changed projections and removing stale ones.
- Provides observable repositories.

### Workers

- Run idempotent processing stages.
- Persist stage state and redacted errors.
- Retry transient failures only.
- Never delete originals because enrichment failed.

## Capture state machine

```text
RECEIVED
  -> COPYING
  -> STORED
  -> EXTRACTING
  -> AWAITING_ANALYSIS (optional)
  -> ANALYSING (explicit action)
  -> INDEXING
  -> READY
```

Every processing stage can produce `PARTIAL`, `RETRYABLE_FAILURE`, or `PERMANENT_FAILURE`. `STORED` is the durability boundary.

## Core models

### Capture

- Stable ID and type.
- Created/modified timestamps.
- Source package and label when supplied.
- User and generated titles stored separately.
- Original/shared text, user note, extracted text, generated summary.
- Favourite/archive state.
- Remote-sharing policy.
- Processing status and revision.

### Attachment

- Capture ownership.
- App-private relative path.
- Original display name, MIME type, size, and content hash.
- Relevant dimensions or duration.

### Insight

- Task, event, location, link, contact detail, topic, or custom type.
- Versioned structured payload.
- Source excerpt or audio time range.
- Suggested, accepted, edited, dismissed, or completed state.

### Search document

- Normalised field projection by capture ID.
- Field source and weights.
- Index revision.

### AI operation

- Operation and capture ID.
- Local/remote execution, provider, and model.
- Start/end time and redacted outcome.
- Never stores credentials in diagnostic fields.

## Capability interfaces

```kotlin
interface OcrEngine
interface TranscriptionEngine
interface InsightEngine
interface EmbeddingEngine
```

Implementations report local/remote execution, supported inputs, implementation/model version, limits, cancellation support, and structured failure categories.

## Work coordination

- Key unique work by capture ID and processing revision.
- Make each stage safe to retry.
- Cancel work and remove index entries when a capture is deleted.
- Re-run only affected stages after edits or provider changes.
- Enforce WorkManager constraints for large/network operations.

## Search

1. Normalise without destroying multilingual content.
2. Fetch lexical candidates.
3. Apply filters and enabled-source policies.
4. Fetch semantic candidates later when configured.
5. Merge and deduplicate.
6. Rank by exact/phrase/fuzzy match, field weight, optional semantic score, favourite, and modest recency.
7. Produce excerpts that explain why a result matched.

Ranking weights belong in one testable policy, not UI code.

## External actions

Calendar insertion, navigation, calls, email, browser launch, and external sharing require visible user confirmation and standard Android intents wherever possible.

## Architecture decisions

Meaningful choices are recorded in [`docs/adr`](adr/). Update or supersede ADRs instead of silently contradicting them.
