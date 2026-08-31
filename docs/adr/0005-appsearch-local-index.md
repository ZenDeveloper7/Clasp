# ADR 0005: App-private AppSearch keyword index

- Status: Accepted
- Date: 2026-08-31

## Decision

Use AndroidX AppSearch `LocalStorage` for Phase 2 keyword indexing and retrieval. Room remains the authoritative capture store. The AppSearch database is app-private, contains only derived searchable projections, and can be rebuilt completely from Room.

Do not use `PlatformStorage`, cross-package visibility, or system-surface display for capture content.

## Consequences

- Search stays offline and inside Clasp's application sandbox.
- AppSearch provides mobile-oriented indexing, prefix matching, relevance ranking, snippets, and paged retrieval.
- Capture, edit, OCR, and deletion paths synchronize the derived index after the Room operation succeeds.
- Startup and search reconcile IDs and content revisions after interruption or an indexing failure, while full rebuild remains available for recovery.
- Deleting a capture removes both the authoritative Room record and its AppSearch projection.
