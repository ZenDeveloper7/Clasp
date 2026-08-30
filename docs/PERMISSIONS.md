# Permissions

Clasp requests capabilities when the user invokes the associated feature, not as a first-launch bundle.

| Capability | Android mechanism | Phase | Default |
|---|---|---:|---|
| Receive text/links/images/files | Sharesheet intents and granted URIs | MVP | Enabled |
| Select images | Android Photo Picker | MVP | Enabled without broad media permission |
| Select documents | Storage Access Framework | MVP | Enabled |
| Record voice | Microphone runtime permission | MVP | Ask when recording starts |
| Recording notification | Notification permission where applicable | MVP | Ask only when recording needs it |
| Create calendar event | User-confirmed insert intent first | Phase 3 | No standing write permission |
| Search contacts | Contacts runtime permission | Phase 5 | Disabled |
| Search calendar | Calendar runtime permission | Phase 5 | Disabled |
| Search installed apps | Policy-compliant package queries | Phase 5 | Disabled |
| Assisted screenshots | Accessibility Service | Research | Not part of core app |

## Rules

- Every denial leaves unrelated Clasp features functional.
- Do not request broad media or all-files access for normal capture.
- Do not continuously monitor the clipboard.
- Do not use Accessibility Service for analytics, navigation automation, or unrelated data collection.
- Explain what becomes searchable before enabling a device source.
- Revoking a permission removes that source from new results and background work without deleting user-created Clasp captures.
