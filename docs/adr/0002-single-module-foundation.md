# ADR 0002: Single-module foundation

- Status: Accepted
- Date: 2026-08-30

## Decision

Begin with one `:app` module and explicit package boundaries. Extract modules only when stable contracts, build isolation, ownership, or reuse justify them.

## Consequences

- Lower Phase 0/MVP configuration overhead.
- Package boundaries and dependency direction require review discipline.
- Provider and data interfaces must remain separable for fakes and later extraction.
