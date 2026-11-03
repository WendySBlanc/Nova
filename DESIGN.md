# Nova Visual System

## Direction

Nova is a reading-first Android application for manhwa and light novels. Its visual system keeps the directness of Viki while using denser, quieter controls suited to long browsing and reading sessions.

## Principles

- Content leads. Covers, chapter previews, titles, and reading state receive visual priority.
- Controls stay compact, predictable, and reachable with a minimum 48 dp touch target.
- Use near-black ink surfaces with clear depth steps instead of decorative gradients or floating page sections.
- Electric cyan identifies primary actions. Coral, amber, and green are reserved for semantic states.
- Cards are for repeated content and tools. Screen sections use spacing and dividers instead of nested cards.
- Phone layouts keep three library columns. Larger screens add columns and cap content width.
- Reader controls remain temporary overlays and never reduce the reading viewport while hidden.

## Tokens

- Corners: 4 dp for small details, 6 dp for content cards, 8 dp for controls and sheets.
- Spacing: 4 dp base rhythm, 14 dp phone page padding, 20 dp expanded page padding.
- Motion: 120 ms feedback, 180 ms controls, 220 ms content transitions.
- Typography: zero letter spacing, strong titles, restrained labels, 16 sp primary body copy.

## Accessibility

- Text and interactive boundaries target WCAG AA contrast.
- State is communicated with text or iconography as well as color.
- Interactive elements expose meaningful TalkBack labels and state descriptions.
- Layouts must tolerate font scaling, rotation, tablets, and foldable-width changes without overlap.
