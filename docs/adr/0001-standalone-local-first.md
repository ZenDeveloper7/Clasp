# ADR 0001: Standalone local-first application

- Status: Accepted
- Date: 2026-08-30

## Decision

Clasp is a standalone Android application that runs alongside Nothing Launcher. Core capture, storage, OCR, library, and keyword search are local and account-free.

## Consequences

- No launcher modification, privileged Nothing API, Essential Key, or Nothing Account dependency.
- Android public share, picker, storage, and intent contracts define integration.
- Other Android 12+ devices are not artificially blocked.
- Local failure handling and durable originals are product-critical.
