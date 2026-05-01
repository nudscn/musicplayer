# Project Context

This file preserves the working context for Codex after moving this project over from earlier VS Code-based work.

## Project

- Name: `musicplayer`
- Purpose: fully local Android music player
- Root: `D:\Python\musicplayer`
- Repo type: Git repository initialized on `2026-05-01`

## Current Goal

Build a pure local Android player with:

- compact playback-oriented UI
- local library browsing
- offline-first architecture
- maintainable scanning, playback, and persistence layers

## Core Stack

- `Kotlin`
- `Jetpack Compose`
- `Room`
- `Media3`
- `SAF`

## Current Architecture

- package root: `com.example.musicplayer`
- key modules:
  - `data/db`: Room entities and DAO
  - `data/repository`: library coordination
  - `data/scanner`: SAF directory scanning and metadata extraction
  - `playback`: player and playback service
  - `ui`: Compose screens and state
  - `util`: helpers such as artwork, lyrics, and time formatting
  - `widget`: playback widget

## Current Status

Based on source and prior development work, the project already has:

- Android app skeleton
- SAF directory selection and persisted access
- recursive local scan
- local library indexing
- Media3 playback queue
- background playback service
- simple EQ preset support
- embedded artwork loading
- same-name `.lrc` lyrics preview
- first usable playback UI

## Important Local Constraints

- this is intended to be local-only and offline-first
- Android SDK and build tools exist locally but should not be versioned
- signing material and machine-specific files must stay out of Git

## Git Notes

`musicplayer` did not previously have its own `.git` directory. It now does.

Important ignored local-only paths include:

- `.android-home/`
- `.android-user-home/`
- `.downloads/`
- `.gradle/`
- `.keystore/`
- `.tools/`
- `android-sdk/`
- `local.properties`
- `keystore.properties`

## What Prior Chat History Likely Contained

- desired product direction: local, simple, Winamp-like feel
- architecture choice: SAF + Room + Media3 instead of quick hacks
- incremental build-up from scan to library to playback
- Android build-environment setup details on this machine

This file is the durable replacement for relying on earlier chat memory alone.

## Next Recommended Work

1. Clean up or rewrite the current garbled `README.md` into readable UTF-8 Chinese or English.
2. Add scan progress and incremental scan support.
3. Add persistent playback queue behavior.
4. Expand library-detail views and playlist management.
5. Add a clearer release-signing and APK build note.

## Codex Handoff Notes

- Open this folder directly as a Codex project.
- Read this file before editing.
- Treat `android-sdk/` and signing files as local machine resources, not repo content.
