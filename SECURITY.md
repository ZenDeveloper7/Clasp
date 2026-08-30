# Security policy

## Supported versions

Clasp has not published a stable release. Security fixes currently target the latest `main` branch and the most recent GitHub prerelease once releases begin.

## Reporting a vulnerability

Do not open a public issue for vulnerabilities involving credential exposure, capture disclosure, URI access, backup/restore, deletion failure, intent handling, provider requests, or permission abuse.

Report privately to **zenit027@proton.me** with:

- A concise description and impact.
- Affected commit or version.
- Reproduction steps or proof of concept using synthetic data.
- Suggested mitigation if available.

Do not include real user captures or credentials. Allow reasonable time for investigation and coordinated disclosure.

## Security boundaries

- Never commit signing keys, Firebase administrative credentials, Gemini keys, or real user content.
- A Firebase mobile configuration may be committed only when limited to Firebase services; it must never authorise Gemini or unrelated APIs.
- Core features must not require Accessibility Service.
- Remote content processing must remain explicit and visible.
