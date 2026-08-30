# Clasp

**Capture anything. Find it later.**

Clasp is an independent, community-built open-source Android companion for Nothing and CMF phones that do not support Nothing Essential Space. It is designed to capture text, links, images, screenshots, files, and voice notes; turn them into searchable information; and surface useful actions without requiring launcher integration.

> [!IMPORTANT]
> Clasp is not affiliated with, endorsed by, or sponsored by Nothing Technology Limited. Nothing, Nothing OS, Essential Space, and related marks belong to their respective owners.

## Status

Clasp is in Phase 1 development. The Android application now supports offline text capture, image and document import, Android Sharesheet input, a durable local library, editable capture details, favourites, export, and permanent deletion. OCR, search, voice, and AI features have not been implemented yet.

The first supported-device baseline is:

- Nothing Phone (1)
- Nothing Phone (2)
- Nothing Phone (2a)
- Nothing Phone (2a) Plus
- CMF Phone 1

Clasp does not artificially block other Android 12+ devices, but those devices are outside the initial validation matrix.

## Product boundaries

- Standalone app alongside Nothing Launcher; no launcher modification or replacement.
- Local-first capture and keyword search.
- No account required for core features.
- No cloud analysis until a user explicitly selects **Analyse**.
- No Accessibility Service requirement for core operation.
- No arbitrary access to other apps' private data.
- Original Clasp assets and layouts; no Nothing logos, proprietary fonts, Glyph artwork, or copied Essential Space screens.

See [Product plan](docs/PRODUCT_PLAN.md), [Architecture](docs/ARCHITECTURE.md), and [Privacy](docs/PRIVACY.md).

## Current Phase 1 capabilities

- Create text captures inside Clasp.
- Import images through Android Photo Picker.
- Import documents through Storage Access Framework.
- Receive shared text, images, and application files.
- Copy attachments immediately into app-private storage under generated filenames.
- Browse, edit, favourite, export, and permanently delete captures.
- Work without an account, Firebase, AI configuration, or network access.

## Planned MVP

- Share text, links, images, and files to Clasp.
- Create text, image, and voice captures in Clasp.
- Store originals durably in app-private storage.
- Extract image text locally with OCR.
- Search titles, notes, extracted text, and transcripts.
- Generate editable summaries and actions through a provider-neutral AI interface.
- Use Gemini Developer API with a user-provided key as the first optional provider.
- Export and permanently delete individual captures.

Semantic search, device sources, widgets, Quick Settings, backup, synchronisation, and experimental screenshot assistance follow after the MVP boundary is reliable.

## Toolchain

- Kotlin 2.2.10
- Jetpack Compose with the Compose BOM
- Android Gradle Plugin 9.3.2
- Gradle 9.5.0
- Minimum Android version: Android 12 / API 31
- Target Android version: API 37
- Date-based versions: `YYYY.MM.DD.PATCH`

## Build

Use the checked-in Gradle wrapper:

```bash
./gradlew assembleDebug
```

Run foundation verification with:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Do not commit `local.properties`, signing material, Gemini keys, user captures, or generated build output.

Firebase Crashlytics remains deferred until a Firebase project exists. It will be disabled by default, explicitly opt-in, and configured without Firebase Analytics or content-bearing logs.

## Contributing

Issues and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md) before contributing.

## Licence

Clasp is licensed under the [Apache License 2.0](LICENSE).
