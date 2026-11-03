# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Nova is built for frequent French-speaking readers who maintain a personal library of manhwa and light novels, often across several extension sources. They need to resume reading quickly, understand reading and download state at a glance, and organize a large collection without friction.

## Product Purpose

Nova brings discovery, library management, offline downloads, source extensions, manhwa reading, and light-novel reading into one Android application. Success means that a reader can either resume the next chapter immediately or manage the whole collection from the same primary surface.

## Positioning

Nova combines a source-driven manga library with first-class visual and text readers, local/offline continuity, and a reading-centric home experience. Manhwa and light novels share one coherent product while retaining controls suited to each medium.

## Operating Context

- Readers browse and search extension sources, add titles to the library, and organize them with categories.
- They refresh the library, track unread chapters, download chapters for offline use, and restore backups containing local state.
- They move between a visual long-strip reader for manhwa and a configurable text reader for light novels.
- The app is used primarily on phones, while tablets, foldables, landscape layouts, font scaling, and system insets must remain fully supported.

## Capabilities and Constraints

- Existing library data, downloads, backups, extensions, source migration, chapter state, trackers, and reader behavior must remain compatible.
- The implementation remains native Kotlin with Jetpack Compose and the existing domain/data architecture.
- Presentation may be reorganized freely and duplicate actions may be consolidated, but established capabilities must not disappear.
- Navigation, layouts, and controls must adapt to available window size rather than target one fixed phone.
- Motion and transitions are part of the product, with reduced-motion support and no loss of responsiveness.
- Proprietary visual assets must not be copied; use project-owned, platform, or compatible open-source assets.

## Brand Commitments

- Product name: Nova.
- The interface should feel purpose-built for reading rather than like a reskinned upstream manga client.
- French is the primary language for the owner, while user-facing copy must continue to use the app's localization system.

## Evidence on Hand

- Existing cover art and chapter thumbnails come from the reader's configured sources and library.
- Existing Nova application icon, source icons, download state, chapter metadata, and library categories are available in the repository.
- The repository contains working Compose implementations for the library, title page, browse flows, settings, visual reader, and light-novel reader.
- No testimonials, commercial claims, or additional proprietary brand assets should be fabricated.

## Product Principles

- Resume and organize are equal first-class jobs.
- Reading state must be visible before the reader taps.
- Advanced capability belongs behind clear progressive disclosure, not duplicated controls.
- Offline and restored data must look as trustworthy as network data.
- Content leads; interface chrome remains calm, responsive, and predictable.

## Accessibility & Inclusion

- Support TalkBack, logical focus order, 48 dp touch targets, font scaling, high-contrast states, and non-color status indicators.
- Respect Android reduced-motion and system display preferences.
- Avoid relying on gestures alone for essential chapter or download actions.
