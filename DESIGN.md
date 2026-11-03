# Nova Visual System

## Direction

Nova Signal is a native Android interface for reading manhwa, webtoons, and light novels. It uses Jetpack Compose, Material 3 semantic roles, and motion that explains navigation or state changes without interrupting reading.

## Principles

- Content leads. Covers, chapter previews, titles, and reading state receive visual priority.
- Controls stay compact, predictable, and reachable with a minimum 48 dp touch target.
- Use graphite and paper surfaces with clear depth steps instead of decorative gradients or floating page sections.
- Mineral teal identifies primary actions. Vermilion, chartreuse, amber, and green are reserved for distinct semantic states.
- Cards are for repeated content and tools. Screen sections use spacing and dividers instead of nested cards.
- Library grids use adaptive cells, retaining three compact columns only when covers and labels remain legible.
- Compact layouts use bottom navigation. Medium and expanded layouts use a navigation rail.
- Reader controls remain temporary overlays and never reduce the reading viewport while hidden.
- The reader exposes one horizontal progress control for both image and text chapters.

## Tokens

- Corners: 4 dp for small details, 6 dp for content cards, 8 dp for controls and sheets.
- Spacing: 4 dp base rhythm, 16 dp compact page padding, 24 dp expanded page padding.
- Motion: 90 ms feedback, 160 ms controls, 220 ms content transitions.
- Typography: zero letter spacing, strong titles, restrained labels, 16 sp primary body copy.

## Accessibility

- Text and interactive boundaries target WCAG AA contrast.
- State is communicated with text or iconography as well as color.
- Interactive elements expose meaningful TalkBack labels and state descriptions.
- Layouts must tolerate font scaling, rotation, tablets, and foldable-width changes without overlap.
- Reduced-motion settings replace movement with short fades while preserving state feedback.

## Android Equivalents

- React component composition: Jetpack Compose composables and immutable UI models.
- Motion and Framer Motion: Compose `AnimatedContent`, `AnimatedVisibility`, state animation, and shared transitions.
- GSAP timelines: coordinated Compose transitions with Nova Signal motion tokens.
- Responsive web breakpoints: adaptive window widths, bottom navigation, and navigation rail.
- AI UI kits: optional on-device ML Kit or Gemini Nano features only when device support is available; core reading remains fully local and deterministic.
