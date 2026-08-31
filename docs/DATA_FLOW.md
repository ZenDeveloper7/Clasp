# Data flow

## Local capture

```text
User action
  -> Android share/picker/Clasp capture UI
  -> Validate type and size
  -> Copy temporary URI into app-private storage
  -> Persist Capture + Attachment transaction
  -> Schedule local extraction
  -> Update local search projection
  -> Library/Search UI
```

The source is persisted before enrichment. Temporary provider URIs are not treated as durable storage.

## Local image extraction

```text
Stored image
  -> Unique WorkManager OCR job
  -> Bundled on-device ML Kit text recognizer
  -> Persist extracted text separately from the original
  -> Refresh rebuildable app-private AppSearch projection
  -> Search/detail UI with OCR provenance
```

OCR does not upload an image. Failure preserves the original, records a redacted error code, and exposes an explicit retry action.

## Explicit remote analysis

```text
Stored capture
  -> User taps Analyse
  -> Disclosure names provider and content category
  -> User confirms
  -> Read selected source/derived content
  -> Gemini request using user's key
  -> Validate versioned structured response
  -> Store generated fields separately
  -> Re-index derived fields
```

Configuring a provider never starts background uploads. The original remains authoritative when a request fails or output is invalid.

## Search

```text
Query entered in Clasp
  -> Unicode NFKC normalisation and bounded prefix tokens
  -> AppSearch LocalStorage prefix lookup and relevance ranking
  -> Deterministic field weights, Clasp filters, and AppSearch excerpts
  -> Results
```

MVP search queries and indexed documents stay inside Clasp's app-private `LocalStorage` index. Room remains authoritative, and Clasp reconciles IDs and content revisions after startup or an indexing failure; a full rebuild remains available for recovery. A later web/AI answer mode must be a visually and technically separate action.

## Crash reporting (Phase 1)

```text
Crash/ANR
  -> Sanitised local Crashlytics report
  -> Collection disabled by default
  -> User explicitly opts in
  -> Firebase Crashlytics
```

Reports must not contain captures, OCR, transcripts, search queries, filenames, content URIs, provider endpoints, credentials, user identifiers, or custom user-action breadcrumbs.

## Deletion

Permanent deletion removes:

- Capture and related database rows.
- Attachments and thumbnails.
- Search documents and embeddings.
- Pending work.
- Associated backup/sync data if those features later exist.

Deletion is not complete until all owned representations are handled.
