# Privacy architecture

## Default posture

- No account is required.
- Core capture and keyword search are local.
- Image OCR uses the bundled ML Kit Latin model on device; image content and extracted text are not sent to an OCR service.
- System backup is disabled during the initial phases.
- Firebase is not part of Phase 0.
- Crashlytics is disabled by default when introduced in Phase 1.
- Firebase Analytics is not used.
- AI processing is disabled until the user configures a provider.
- Every remote analysis starts with an explicit **Analyse** action.
- No broad file access, clipboard monitoring, or Accessibility dependency.

## Storage

- Attachments live in internal app storage.
- Structured state lives in an app-private database.
- Search documents live in an app-private AppSearch `LocalStorage` index and are never registered for system or cross-app search.
- Android's app sandbox and device file-based encryption are the MVP at-rest boundary.
- Clasp does not initially claim independent database or attachment encryption.
- Gemini credentials use Android Keystore-backed protection and are excluded from backup and logs.

An optional encrypted vault may be evaluated only after capture, process-death, export, deletion, and recovery lifecycles are proven.

## Android backup

Clasp disables Auto Backup in Phase 0 because the future database and attachments can contain sensitive screenshots, recordings, documents, and derived content. A later backup feature requires a versioned export format, encryption, explicit user action, and tested restore before backup is enabled.

Device manufacturers can treat cloud and device-to-device transfer differently, so backup behaviour must be verified on supported devices rather than inferred solely from one manifest flag.

## Gemini BYOK

- The Gemini Developer API key belongs to the user.
- Never commit, bundle, log, export, or upload that key to Clasp/Firebase infrastructure.
- Never reuse the Firebase API key for Gemini.
- Show the provider and content type before each analysis request.
- Validate structured responses before persistence.
- Allow key replacement and deletion.
- Keep the app useful after key deletion.

## Firebase Crashlytics (Phase 1)

One Firebase project will serve project builds, including contributor debug builds. Its configuration may be committed only after:

- the Firebase API key is restricted to required Firebase services;
- Generative Language API and unrelated Google APIs are excluded;
- automatic Crashlytics collection is disabled in the manifest;
- opt-in, opt-out, send-unsent, and delete-unsent controls are implemented;
- Firebase Analytics is absent;
- report keys/logging pass a sensitive-data review.

Allowed diagnostic fields are limited to app version, build type, processing stage, and redacted error category.

Disallowed content includes:

- Original capture text or bytes.
- OCR and transcripts.
- Search queries and result text.
- Filenames and content URIs.
- Email addresses or user identifiers.
- API endpoints, API keys, auth headers, or provider responses.
- User-action breadcrumbs containing content.

## Logging

Release logs must not contain capture content or credentials. Diagnostics use stable internal stage names and redacted error categories. Debug logging follows the same content boundary because contributor builds can eventually include Crashlytics.

## Export

The MVP export action is explicit and uses a user-selected destination. It must warn that exported data leaves Clasp's app-private storage. A later backup archive needs authenticated encryption, a documented schema, integrity validation, and clean-device restore tests.

## Permanent deletion

Delete removes the database graph, attachment files, thumbnails, indexes, embeddings, queued work, and later any remote backup records. Failures remain visible and retryable; the UI cannot claim deletion while owned copies are known to remain.

## Analytics

Clasp does not collect captured content as analytics. Product decisions use synthetic fixtures, opt-in crash stability, local benchmarks, and manual test evidence.
