# VoiceRPG Engine: Agent Guidelines & Engineering Rules

# VoiceRPG Engine: Agent Guidelines & Engineering Rules

This repository is the **standalone, reusable engine** for voice-commanded JRPGs on Android (Kotlin, Jetpack Compose, Android Speech & TTS APIs). All game content has been stripped out and replaced with **drop-in JSON templates** under `app/src/main/assets/game/`. When working on this codebase, adhere strictly to the rules below.

---

## 1. Content Neutrality (THE core rule)

- **Engine code never references game content.** `app/src/main` must not hardcode character names, story chapters, spell names, dialogue, encounters, or art/music filenames. The completed game (`voice-rpg-android` / *Echoes of the Logos*) is a **separate repo and must never be reintroduced here**.
- All content lives in `app/src/main/assets/game/**` as JSON, loaded at runtime by `GameContent`:
  - `manifest.json` (title, starting scene/node, default encounter, victory/defeat lines)
  - `characters/*.json`, `classes/*.json`, `spells/spells.json`
  - `enemies/enemies.json`, `encounters/encounters.json`
  - `story/scenes.json`, `story/nodes.json`
- Bundled art/music referenced by those JSON files live under `app/src/main/assets/{portraits,story,audio,sprites}`.
- New game content = **JSON + assets only**. If a change requires touching `app/src/main`, it must be a genuinely generic engine improvement, not content.

## 2. Tests Are Content-Agnostic Guarantees

The unit suite (`./gradlew testDebugUnitTest`) must stay **green with any drop-in content**. Two test flavors are required:

1. **Schema/invariant tests** that iterate `GameContent` and assert engine-contract invariants (every node link resolves, no dead-ends except `game_completed`, every choice has a `voiceKeyword`, every battle trigger resolves to an encounter, flag names are non-blank, hub `requiredFlags` resolve, scene entries resolve, full reachability).
2. **Engine-mechanics tests** that exercise behavior on whatever the template data is (resonance calibration, tutorial flow, combat economy, voice resolution, save/persistence).

Never write a test that asserts a specific story/person/place. The template story ("Proving Grounds", "Echo Hall", the Guardian) is a **fixture**, and only tests the state machine's mechanics, not the fiction.

## 3. Story Graph Conventions (engine contract)

- **No endless loops:** hub choices set a `completionFlag`, completed choices are eliminated on return, and a hub whose sub-stories are all complete auto-redirects via `hubCompletion.requiredFlags` + `hubCompletion.redirectToNodeId`.
- **No dead ends:** every node must continue via `nextNodeId`, `choices`, `hubCompletion`, or a battle trigger (`triggerBattleEncounterId` + `nextNodeId`). The **only** terminal node is one that sets `game_completed`.
- Scenes bind background + music + entry node; `chapterIndex`/`actNumber`/milestone fields feed the recap and chapter-warp menu — nothing hardcoded.

## 4. Resonance Calibration Invariant

- `SpellChantPresets` builds generic per-tier chants from any spell name; `AcousticProfile` constants per tier are fixed.
- The engine's thematic matcher (root lexicon in `SpellThesaurus`) must cover each spell's name with **exactly one** root word. Zero-root names are unreachable at TRANSCENDENTAL; two-root names overflow BASIC. Keep the lexicon per-school and generic; `testAllSpellChantPresetsAcrossAllResonanceTiers` guards this for the whole spell catalog.
- **Resonance is sacred:** it is the only skill multiplier (1.0x–3.0x). Affinity and stats may never exceed or replace it.

## 5. ViewModel Tests: The Settle Pattern (livelock guard)

Driving `StoryViewModel`/`TutorialBattleViewModel`/`CombatViewModel` in tests that rapidly cancel + relaunch animation jobs on the same event loop can livelock kotlinx-coroutines' `LockFreeLinkedList.finishAdd` and hang the suite for minutes. Follow the pattern in `TutorialBattleTest`: advance state, then **wait for a ref/state change and settle** (small `delay` loop) before the next step. Never `advance()` in a hot loop without a settle.

## 6. Voice & Audio-First (retained engine pillars)

- All dialogue and combat actions must be operable hands-free: TTS narration, `Read choices`, meta-commands ("Status", "Enemies", "Options", "Pocket mode", "Auto listen", "Toggle narration"), and auto-listen re-arm after TTS.
- Bind distinct physical TTS voices per character via `android.speech.tts.Voice`; characters declare `preferredVoiceId` (fallback: pitch/rate modulation). Only characters with a non-null `preferredVoiceId` appear in `GameContent.preferredVoiceIds` / `CombatNarrator.PREFERRED_VOICE_IDS`.

## 7. Verification Before Any Commit

- Always run the full unit suite: `./gradlew testDebugUnitTest` (must be clean).
- Compilation of engine code: `./gradlew compileDebugKotlin` (must be clean).
- GitHub releases / ADB device installs are the **game project's** choice; do not auto-create releases or install without being asked.

## 8. Unique App Identity (every app is its own unique game)

- **This repository builds a unique reference app** — the engine's own sandbox for exercising and testing the framework. Its `applicationId` (`com.voicerpg.engine.reference`) is **reserved** to this repo and must never be shipped as a game.
- **Every app built from this engine MUST be its own unique Android app and its own unique game:** its own `applicationId` in `app/build.gradle.kts`, its own app label/icon in `app/src/main/res/`, and its own game content (JSON + assets under `app/src/main/assets/game/`). Never ship an app that keeps the engine's reference `applicationId`, the engine's app name, another game's `applicationId`, or another game's name/branding.
- **Why:** Android treats `applicationId` as the install identity. Two APKs that share an `applicationId` silently replace each other on the same device — the newest install overwrites the existing one and wipes its app data. This is exactly what happened to *Commander America* when the engine reference app was installed: both used `com.voicerpg.engine`, so building this repo replaced that game on the phone.
- **Before building or installing any derivative:** change `applicationId` (and, when feasible, the `namespace`) in `app/build.gradle.kts` to an identifier you own, and give the app its own label, icon, and content. Reusing the engine's reference id **or** any other game's `applicationId` is a rule violation — the resulting app must never overwrite or impersonate another VoiceRPG title.