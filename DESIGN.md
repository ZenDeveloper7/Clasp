# Clasp design system

## Direction

Clasp should feel immediately comfortable on Nothing OS while remaining recognisably independent. The visual language is monochrome, direct, tactile, and information-led. It uses original assets and layouts rather than reproducing Nothing screens.

The design-system reference library does not include an official Nothing specification. Clasp therefore treats this document—not an unofficial reconstruction of Nothing's private system—as its design source of truth.

## Design promise

**Quiet storage, precise retrieval.**

Captures should feel safely clasped in place. Search should feel fast and exact. Processing should be visible without becoming noisy.

## Palette

| Token | Light | Dark | Purpose |
|---|---:|---:|---|
| `canvas` | `#F7F7F4` | `#000000` | App background |
| `surface` | `#FFFFFF` | `#0A0A0A` | Primary content surfaces |
| `surfaceRaised` | `#ECECE8` | `#171717` | Selected and raised states |
| `ink` | `#111111` | `#F4F4F1` | Primary content |
| `inkMuted` | `#62625E` | `#A5A5A0` | Secondary content |
| `outline` | `#C8C8C2` | `#343434` | Dividers and boundaries |
| `signal` | `#D71921` | `#F04444` | Destructive, recording, and attention states |

Signal red is functional and scarce. It does not decorate ordinary cards or navigation.

Dynamic colour is disabled by default. A later optional setting may enable it without replacing the Clasp identity.

## Typography

- **Display:** an open-licensed dot-matrix-inspired face, selected and vendored only after licence verification. Use sparingly for brand moments, dates, and short section identifiers.
- **Body:** a highly legible open sans-serif or Android system sans for content, forms, search results, and accessibility-critical text.
- **Utility:** medium-weight sans-serif with uppercase labels and modest tracking for metadata and processing states.
- Never bundle or redistribute Nothing's NDot or NType font files.

Typography must remain readable at large font scales. Dot display styling never carries long-form or essential instructions.

## Shape and elevation

- Mostly squared geometry with restrained 4–8 dp rounding where touch affordance benefits.
- Borders and tonal separation before shadows.
- Avoid a screen composed entirely of floating cards.
- Full-round shapes are reserved for compact status indicators and the primary capture action.

## Spacing

Use a 4 dp base grid:

- `4`: icon/internal micro-spacing
- `8`: compact control spacing
- `12`: metadata groups
- `16`: standard content padding
- `24`: section separation
- `32`: major transitions
- `48`: empty-state breathing room

## Signature element: the clasp rail

The memorable Clasp element is a narrow vertical rail formed from paired bracket marks. It connects an original capture to derived OCR, summary, and actions, making provenance visible instead of decorative.

```text
┌ ORIGINAL
│ Screenshot or recording
├ EXTRACTED
│ OCR or transcript
└ ACTIONS
  Tasks, events, links
```

This rail appears in capture detail and processing history, not indiscriminately across every screen.

## Motion

- Capture confirmation closes like two brackets meeting.
- Processing stages advance along the clasp rail.
- Search result changes cross-fade or reposition quickly without elastic spectacle.
- Respect reduced-motion settings.
- No ambient animation that consumes power while idle.

## Core layout

```text
┌──────────────────────────────┐
│ CLASP                 status │
│ ┌──────────────────────────┐ │
│ │ Search what you remember │ │
│ └──────────────────────────┘ │
│                              │
│ FOR YOU                      │
│ Items requiring attention   │
│                              │
│ RECENT                       │
│ Durable capture list         │
│                              │
│ For You  Library  Search  (+)│
└──────────────────────────────┘
```

## Content language

- Use plain verbs: **Save**, **Analyse**, **Create event**, **Delete now**.
- Preserve the same action name through confirmation and completion.
- Explain errors with the failed stage and a recovery action.
- Separate original, user-edited, and generated content explicitly.
- Do not anthropomorphise Clasp or imply certainty for probabilistic output.

## Brand boundary

- App title: **Clasp — Capture & Search**.
- Original Clasp icon based on two interlocking brackets.
- README, About, and release/store descriptions carry the non-affiliation disclosure.
- Nothing may be referenced factually for compatibility; it is not part of the product name or icon.
- No copied screens, official marks, proprietary font binaries, or Glyph artwork.

## Accessibility

- Minimum accessible touch targets.
- TalkBack semantics and deliberate traversal order.
- Font scale testing at 1.5× and larger.
- State is never communicated by colour alone.
- Light, dark, high-contrast, and reduced-motion review.
- Screenshot tests eventually cover compact, medium, and expanded window sizes.
