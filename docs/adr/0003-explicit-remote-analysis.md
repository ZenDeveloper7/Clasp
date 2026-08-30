# ADR 0003: Explicit remote analysis

- Status: Accepted
- Date: 2026-08-30

## Decision

Gemini BYOK is the first optional remote provider. Configuring a key never enables automatic uploads. The user explicitly invokes **Analyse** for each capture and sees the provider and content category before confirming.

## Consequences

- Clasp remains useful without a provider.
- Provider credentials require Keystore-backed protection.
- Generated content remains separate, editable, and traceable to its source.
- Automatic rules require a later, explicit decision.
