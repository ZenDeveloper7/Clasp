# ADR 0004: Disable backup during initial phases

- Status: Accepted
- Date: 2026-08-30

## Decision

Disable Android backup while the capture schema and encrypted export/restore design are undefined.

## Consequences

- Initial data does not migrate automatically to a replacement device.
- Documentation must state this limitation.
- Backup cannot be enabled until restore is tested on a clean installation and sensitive fields are explicitly handled.
