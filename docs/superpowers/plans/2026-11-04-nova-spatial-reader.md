# Nova Spatial Reader Implementation Plan

## 1. Spatial foundation

- Add cached gradient-mesh and wireframe modifiers.
- Add reusable glass and neon-edge modifiers.
- Add adaptive layout calculations with unit tests.

## 2. App shell

- Apply the spatial surface to bottom navigation and rail.
- Replace width animation with draw-phase kinetic icon/indicator feedback.
- Preserve badges, reselection behavior, and safe-area handling.

## 3. Home cockpit

- Replace the stacked rail feed with an adaptive bento grid.
- Recompose the Rev resume stage as a glass reading packet.
- Keep pinned search, pull-to-refresh, and section drill-in behavior.

## 4. Adaptive library

- Introduce compact and split workspace layouts.
- Keep three columns on standard phones and adapt at narrow/tablet widths.
- Apply spatial depth and progress treatment to covers.

## 5. Verification

- Run focused unit tests and Kotlin compilation.
- Run lint for changed Compose surfaces.
- Review the diff for reader/download/source regressions.
- Ask before producing the final APK, per the project workflow.
