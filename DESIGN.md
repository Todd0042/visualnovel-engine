# VoiceRPG Engine: Technical Architecture & Design Specification

**Platform:** Android Native (Kotlin, Jetpack Compose Canvas, Android Speech & TTS APIs)
**Target Architecture:** Offline-first, 100% on-device speech processing, screenless/pocket accessibility, high-performance 60 FPS retro graphics.
**Content Model:** 100% data-driven. Every game ships as a set of **JSON content packs + asset files** under `app/src/main/assets/game/`; the engine classes themselves contain **zero game content**.

---

## 1. Data-Driven Content Architecture

### 1.1 Layout
```
app/src/main/assets/game/
├── manifest.json              # Identity: title, initial scene/node, default encounter, victory/defeat lines
├── characters/*.json          # Speakers + combatants: dialogue identity, voice, and combat stats/growth
├── classes/*.json             # Character-creation classes: stats, starter spells, growth, voice keywords
├── spells/spells.json         # The full voice-chantable spell library (single source of truth)
├── enemies/enemies.json       # Reusable enemy templates (moveset, family, sprite)
├── encounters/encounters.json # Combat encounters incl. boss phases, summons, mid-battle joins
└── story/
    ├── scenes.json            # Scene = background + music + entry node + chapter/milestone metadata
    └── nodes.json             # The branching dialogue/state-machine graph
```
Plus runtime assets referenced by those files: `assets/portraits/*`, `assets/story/*`, `assets/audio/*`, `assets/sprites/*`.

### 1.2 Resolution & Accessor Surface
`GameContent` (the `content` package) parses the pack at boot into typed models and exposes map accessors by id: `scenes`, `nodes`, `characters`, `classes`, `spells`, `encounters`, `enemyTemplates`, `speakerById`, `characterById`, `classById`, `spellById`, `encounterById`, `enemyTemplateById`, `heroCharacter`, `heroSpellsFor`, `createDuoParty`, `createMinion`, `preferredVoiceIds`, `allSpeakers`. **Every cross-reference in content resolves through these maps; a dangling id fails the schema tests.**

---

## 2. The Audio-First Pillar (voice is primary, not flavor)

### 2.1 Screenless / Pocket Mode
- Every game state — dialogue, choices, battle actions, hero turns, enemy reinforcements, results — can be narrated via TTS.
- Microphone auto-listen re-arms after each TTS cue for a continuous hands-free loop.
- Meta-commands invoke system state anytime: *"Status"*, *"Enemies"*, *"Fellowship"*, *"Help"*, *"Options"*, *"Pocket mode"*, *"Auto listen"*, *"Toggle narration"*, *"Read choices"*.

### 2.2 Multi-Voice TTS Binding
- The engine enumerates installed `android.speech.tts.Voice` models and binds distinct physical voices to characters.
- Characters declare `preferredVoiceId` (exact TTS voice) with `ttsPitch`/`samplePhrase` fallback modulation when a requested voice isn't installed.
- `GameContent.preferredVoiceIds` / `CombatNarrator.PREFERRED_VOICE_IDS` is derived from content — only characters with a non-null `preferredVoiceId` appear.

---

## 3. The Story State Machine

### 3.1 Scenes
A scene binds `backgroundAsset` + `musicAsset` + `initialNodeId` and carries the metadata feeding the spoken Quest Recap and chapter-warp debug menu: `chapterIndex`, `actNumber`, `actTitle`, `milestoneTitle/Location/Summary/Icon`, `defaultObjective`.

### 3.2 Nodes (the graph)
Each node is one narrative beat:
- `speakerId` → resolves to a character (portrait, title, color, pitch).
- `nextNodeId` → continue.
- `choices[]` → each has `text`, `voiceKeywords`, `nextNodeId`, optional `completionFlag`.
- `setFlagOnEnter` → persists a story flag when the node is entered.
- `triggerBattleEncounterId` + `nextNodeId` → pauses narrative, runs a combat encounter, resumes at `nextNodeId` on victory.
- `changeSceneId` → swaps background/music and enters the target scene's `initialNodeId`.
- `healPartyOnEnter` → `half` or `full` party restore.
- `hubCompletion` (`requiredFlags` + `redirectToNodeId`) → auto-redirect once all required flags are set.

### 3.3 Non-Looping Flow Contract
1. Hub choices set a `completionFlag`; returning to the hub **eliminates completed options**.
2. When every hub sub-story is complete, the hub **auto-redirects** via `hubCompletion`.
3. The only terminal node is one that sets `game_completed`. Every other node continues via `nextNodeId`, `choices`, `hubCompletion`, or a battle trigger. **No dead ends.**

---

## 4. The Resonance Engine (the only skill multiplier)

### 4.1 Scoring pillars (up to 200%)
Incantations are scored on five mathematical pillars, dominated by acoustic + linguistic structure:

| Pillar | Source | Ceiling |
|---|---|---|
| Thematic vocabulary density | `SpellThesaurus` per-school root lexicon | +45 |
| Lexical richness & multi-clause syntax | word count | +45 |
| Vocal volume & projection | `AcousticProfile.volumeScore` | +40 |
| Inflection, pitch variance & crescendo | `AcousticProfile.inflectionScore` | +40 |
| Anti-repetition novelty | `NoveltyCache` | +30 |

### 4.2 Tier brackets
`≥155` TRANSCENDENTAL (2.55x–3.0x), `≥100` MYTHIC (2.0x–2.5x), `≥55` MASTER (1.55x–1.95x), `≥20` ADEPT (1.2x–1.5x), else BASIC (1.0x–1.15x). Bonus computes the damage/heal multiplier; particle count and screen effects scale with score.

### 4.3 Generic preset calibration
For hands-free, simulator, and automated testing, `SpellChantPresets` builds a deterministic chant per tier from **any spell name alone**, paired with that tier's fixed `AcousticProfile`. Constraints:
- BASIC ≈ 10, ADEPT ≈ 28, MASTER ≈ 75, MYTHIC ≈ 119, TRANSCENDENTAL ≈ 157 (landing inside their brackets).
- This is **only sound if each spell name matches exactly one thematic root** — which is why the per-school thesaurus lexicon must stay complete and generic, and why `testAllSpellChantPresetsAcrossAllResonanceTiers` iterates the entire spell catalog (0-root names underflow TRANSCENDENTAL; 2-root names overflow BASIC).

### 4.4 Hard invariant
**Resonance is sacred.** It is the only 1.0x–3.0x multiplier in the game. Affinity modifiers and stats may never exceed or replace it.

---

## 5. Real-Time ATB Combat

- **Initiative:** Agility-driven ATB gauges tick for heroes and enemies concurrently (pause/resume friendly). Gauge speed feeds the turn queue.
- **Ordering:** Heroes on the left, up to 4; enemies in tactical front/back rows, up to 6; reinforcements appear mid-battle up to the hard field cap.
- **Encounters (`encounters.json`):** `party` (character ids, or empty = active campaign party), `enemies` (template + per-instance overrides), `bossPhases` (threshold announcements, party-wide pressure via `reducePartyToHpPct`/`executeBelowHpPct`, summons), `midBattleJoin` (character joins after N enemy actions).
- **Enemy AI:** `EnemyBrain` behavior trees (threat, health thresholds, status exploitation, roles) powered by per-template `movesetId`s from the generated `MovesetTable`.
- **Affinity matrix:** 7 spell schools × 7 enemy families drive weak/resist/absorb with an impairment-only **0.8 floor on base damage** — nothing is ever blocked, and the resonance multiplier is never discounted.
- **Statuses:** damage-over-time, crowd control, buffs/debuffs, shields; potency gated by tier (status resistance applies to potency/duration, never to the resonance multiplier).
- **Progression:** XP → level → per-class growth (`classes/*.json` + character `growth`), spell unlocks via `starterSpellIds`/`spellGrants`.

---

## 6. Persistence & Save

- `SaveManager` writes **multi-slot JSON** snapshots with `AtomicFile` semantics (crash-safe) and schema backward-compatibility.
- Persisted state: story flags, current scene/node, party stats/levels/XP, defeated/completed encounters, and user audio/options preferences (narration speed, voice pitch, pocket settings).
- Character ids are the durable keys — content edits that keep ids stable preserve saves.

---

## 7. Test Strategy (green against any content pack)

The suite (`./gradlew testDebugUnitTest`) is content-agnostic:
1. **Schema/invariant tests** iterate `GameContent` and validate the engine contract — every link resolves, no dead ends except `game_completed`, every choice has `voiceKeywords`, battle triggers resolve, flags are non-blank, hubs resolve, scene entries resolve, full node-reachability.
2. **Engine-mechanics tests** exercise behavior on the template data — resonance calibration, tutorial flow, combat economy, intent/voice resolution, save round-trips, story state-machine walks (choice elimination, scene change, battle-trigger resume, `game_completed` termination).

A valid new game = any content pack that satisfies the schema invariants; the engine's behavior guarantees are what the tests assert, never the fiction.

---

## 8. Technical Stack

| Subsystem | Technology | Specifications |
| :--- | :--- | :--- |
| Language & tooling | Kotlin, Gradle, JDK 17 | Min SDK 26, Compile/Target SDK 34 |
| UI | Jetpack Compose + Compose Canvas | 60 FPS hardware-accelerated retro rendering |
| Speech recognition | `android.speech.SpeechRecognizer` | on-device offline recognition |
| Text-to-speech | `android.speech.tts.TextToSpeech` | multi-voice via `tts.voices`, per-speaker binding |
| Persistence | `SaveManager` | multi-slot AtomicFile JSON, backward-compatible |
| Testing | JUnit 4 + kotlinx-coroutines-test | schema invariants + engine mechanics, no content coupling |