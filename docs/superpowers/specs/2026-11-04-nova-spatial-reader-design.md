# Nova Spatial Reader

## Intent

Nova keeps its proven reading, library, download, and extension flows, but replaces the current stacked interface with a spatial reading workspace. The visual language combines a restrained gradient mesh, perspective wireframe, translucent glass panels, and kinetic state changes. Effects must improve hierarchy or feedback; they must never compete with covers and text.

## Visual system

- **Gradient mesh:** broad diagonal color planes in the app background and hero stage. No decorative gradient orbs.
- **Wireframe:** a cached perspective grid used on large structural surfaces, never inside every scrolling card.
- **Glass:** translucent command surfaces with a maximum 8 dp corner radius and a visible outline.
- **Neon:** thin progress, focus, and selected-state rails only.
- **Kinetic motion:** short press, selection, refresh, and navigation transitions. Motion respects the system animation scale.
- **Particles:** reserved for refresh and empty states. No continuously animated particles behind lists.
- **Clay, isometric, and ASCII:** excluded from primary reading surfaces because they reduce scan speed and visual consistency.

## Architecture

### App shell

Compact layouts use a full-width translucent navigation dock with a luminous selected rail. Expanded layouts use a spatial side rail. Both keep 48 dp targets and expose selection through shape, position, and color.

### Home

Home becomes an adaptive reading cockpit:

1. A pinned glass command header.
2. A full-span resume stage using the existing Rev motion engine.
3. A bento-style grid of new chapters and recently added titles.
4. Section headers remain actionable and open the complete grid.

Local library data remains the source of truth. No new network dependency is introduced.

### Library

On phones, controls form a compact glass packet above a three-column grid when space allows. On larger screens, controls move into a left workspace rail and the library grid occupies the remaining width. Collections and categories remain functional and visible only when relevant.

### Cards

Covers retain high-quality Coil loading. A subtle offset frame provides depth; a neon edge represents reading progress. Read status remains available through saturation, text, and semantics rather than color alone.

## Performance and accessibility

- Structural drawing uses `drawWithCache` so paths and brushes are cached.
- Lazy content uses stable keys and content types.
- Animated scale and alpha are applied in the draw phase with `graphicsLayer`.
- Touch targets remain at least 48 dp.
- Text supports system font scaling and truncates only secondary metadata.
- Reduced motion follows the Android animator duration scale.
- Contrast and spoken semantics remain explicit for reading and archive states.

## Non-goals

- No reader, download, source, migration, or database behavior changes.
- No continuous shader or particle loop.
- No dependency on React, GSAP, or web rendering; all effects are native Compose equivalents.
