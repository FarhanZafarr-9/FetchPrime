# FetchPrime 📦

[![Latest Release](https://img.shields.io/github/v/release/FarhanZafarr-9/FetchPrime?include_prereleases&color=007AFF&style=flat-square)](https://github.com/FarhanZafarr-9/FetchPrime/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/github/license/FarhanZafarr-9/FetchPrime?style=flat-square)](LICENSE)

Fast, modern Android companion downloader for [Morphe Manager](https://github.com/MorpheApp/morphe-manager) and standalone APK retriever — reimagined with an overhauled connected UI and enhanced resolver experience.

A refined fork of [`morphe-fetch`](https://github.com/Akash-Sriram/morphe-fetch) by [Akash Sriram](https://github.com/Akash-Sriram) (originally [`helper-for-morphe`](https://github.com/rushiranpise/helper-for-morphe) by [Rushi Ranpise](https://github.com/rushiranpise)), maintained and evolved by [Farhan Zafar](https://github.com/FarhanZafarr-9).

---

## ✨ What's Different in This Fork (FetchPrime)

- **Connected Card UI System** — Redesigned list items and setting groups using cohesive connected card geometry (`Single`, `First`, `Middle`, `Last`) with tight `3.dp` spacing across all screens.
- **Borderless, Elevated Aesthetics** — Replaced flat hard borders with subtle, elevated `surfaceVariant` cards, clean search fields, and borderless dialog surfaces.
- **Pill & Circle Action Geometries** — All touch actions modernized with tactile pill buttons, uniform 44.dp heights, and circular icon controls.
- **Nested In-Card Version History** — Version history and manual fallback actions are embedded directly inside candidate and empty-state cards rather than floating externally.
- **Smart Version Discovery** — Automatically queries available version history when an initial search yields no direct APK match; dynamically expands single actions to full width.
- **Embedded Challenge & Error Cards** — Cloudflare Turnstile verification notices, HTTP 403 blocks, and network errors are hosted inside candidate cards alongside unified action buttons.
- **Markdown Changelog Renderer** — In-app update dialog parses and displays GitHub release notes with rich Markdown formatting (headers, bullet lists, fenced code blocks).
- **Cleaned Navigation & Lists** — Removed intrusive vertical scrollbars, added instant developer and repository info rows in Settings, and polished tablet two-pane layouts.

---

## Key Features

- **Multi-Source Parallel Resolution** — Concurrently queries all enabled sources (Google Play via Aurora, APKMirror, APKPure, and Uptodown) to fetch recommended architectures and compatible versions without manual hunting.
- **Find New Apps (Archive Explorer)** — Built-in catalog of 100+ patchable applications and community repositories. Includes live GitHub catalog sync, offline fallback, favourites bookmarking, and smart installed/patched app status detection.
- **Download Stream & Archive Integrity** — Validates download streams against server content lengths, verifies ZIP/DEX file integrity before handoff, and extracts signing certificates to protect against corrupt or tampered binaries.
- **Headless Cloudflare Solver** — Background Turnstile solver, ad-blocking engine, and session keeper for seamless downloads on protected mirrors.
- **Aurora Store & MicroG-RE Integration** — One-tap anonymous Google Play token acquisition via installed MicroG-RE or verified fallback dispensers for direct Play Store CDN delivery.
- **Universal Format Support** — Fully parses, verifies, and packages standalone `APK` files as well as split bundles (`APKM`, `APKS`, and `XAPK`).
- **Morphe Manager Integration** — Automated handoff via Android intents (`ACTION_DOWNLOAD_ORIGINAL_APK`), routing clean stock binaries directly to Morphe Manager's patching pipeline.
- **Modern Jetpack Compose Architecture** — Material 3 dynamic color, edge-to-edge system bars, pull-to-refresh, adaptive tablet two-pane layout, and responsive navigation.

---

## Supported Sources

| Source | Description | Features |
|---|---|---|
| **Aurora Store** | Direct Google Play CDN | Anonymous check-in via MicroG-RE, architecture filtering, split-APK delivery |
| **APKMirror** | Verified community mirror | Architecture variants, release dates, automated session warmer |
| **APKPure** | Versioned archive API | Fast direct CDN downloads with device-matched ABIs |
| **Uptodown** | Global software directory | Background Turnstile bypass and direct variant downloads |

---

## Credits & Upstream

- **Author / Maintainer**: [Farhan Zafar](https://github.com/FarhanZafarr-9)
- **Forked From**: [Akash-Sriram/morphe-fetch](https://github.com/Akash-Sriram/morphe-fetch)
- **Original Project**: [`helper-for-morphe`](https://github.com/rushiranpise/helper-for-morphe) by [Rushi Ranpise](https://github.com/rushiranpise)

---

## License

Licensed under the [GNU General Public License v3.0](LICENSE).
