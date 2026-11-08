# Visual Novel Engine: Agent Guidelines & Engineering Rules

This repository is the **standalone, reusable engine** for voice-narrated, data-driven Visual Novels on Android (Kotlin, Jetpack Compose, Android TTS & Audio APIs). All narrative content is decoupled and loaded from **drop-in JSON templates** under `app/src/main/assets/game/`. When working on this codebase, adhere strictly to the rules below.

---

## 1. Content Neutrality (Core Rule)

- **Engine code never references game content.** `app/src/main` must not hardcode character names, story chapters, dialogue, or art/music filenames.
- All content lives in `app/src/main/assets/game/**` as JSON, loaded at runtime by `GameContent`:
  - `manifest.json` (title, starting scene/node, title backdrop, phonetic dictionary)
  - `characters/*.json` (speaker identity, portrait, theme color, TTS pitch, gender)
  - `story/scenes.json` (scenes, backdrops, music tracks, chapter metadata)
  - `story/nodes.json` (modular dialogue nodes and scene transitions)
- Bundled art and audio referenced by JSON live under `app/src/main/assets/{portraits,story,audio}`.
- New visual novel content = **JSON + assets only**. If a change requires touching `app/src/main`, it must be a genuinely generic visual novel engine improvement, not content.

## 2. Zero Combat Remnants

- This engine is purely a Visual Novel framework. It contains **no combat systems, no battle views, no HP/MP stats, no attack/spell commands, and no enemy AI**.
- Do not introduce or retain classes, strings, dictionaries, or assets related to RPG combat, damage formulas, or battle encounters.

## 3. Tests Are Content-Agnostic Guarantees

The unit suite (`./gradlew testDebugUnitTest`) must stay **green with any drop-in content**:
1. **Schema and invariant tests** that iterate `GameContent` and assert engine-contract invariants (every node link resolves, every character has valid portrait assets, scenes bind real backgrounds and music, manifest entries are non-blank).
2. **Engine-mechanics tests** that validate playback mechanics, TTS text normalization, audio ducking, and multi-slot save/load persistence.

Never write a test that asserts a specific narrative fiction. The starter template story ("Proving Grounds", "Echo Hall") is a fixture to exercise engine mechanics.

## 4. Narration & Audio Architecture

- Dialogue is read aloud via Android on-device TextToSpeech (`StoryNarrator.kt`) with per-character pitch and voice mapping.
- TTS normalization cleans asterisks (`*whispers*`), scene numbers (`(12)`), applies phonetic replacements, and handles proper nouns.
- Background music (`MusicManager.kt`) dynamically ducks when dialogue is spoken and resumes smoothly afterwards.

## 5. Unique App Identity

- **Every visual novel built from this engine MUST be its own unique Android app:**
  - Unique `applicationId` in `app/build.gradle.kts`.
  - Unique app label in `app/src/main/res/values/strings.xml`.
  - Unique launcher icon in `app/src/main/res/mipmap-*/`.
- The engine's reference app uses `com.voicerpg.engine.reference` for testing and must not be used by released visual novels.

## 6. Verification Before Any Commit

- Always run the full unit suite: `./gradlew testDebugUnitTest` (must be 100% green).
- Verify compilation: `./gradlew assembleDebug` (must build cleanly).