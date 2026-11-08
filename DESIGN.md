# Visual Novel Engine: Technical Architecture & Design Specification

**Platform:** Android Native (Kotlin, Jetpack Compose, Android TTS & Audio APIs)  
**Target Architecture:** Offline-first, 100% on-device speech & audio processing, edge-to-edge Compose rendering, 3-slot atomic persistence, multi-language localization.  
**Content Model:** 100% data-driven. Every visual novel ships as a set of **JSON content packs + asset files** under `app/src/main/assets/game/`; the engine classes themselves contain **zero game content**.

---

## 1. Data-Driven Content Architecture

### 1.1 Layout
```
app/src/main/assets/game/
├── manifest.json              # Title, initial scene/node, title backdrop, phonetic TTS dictionary
├── characters/*.json          # Speakers: display name, title, portrait, theme color, TTS pitch & gender
└── story/
    ├── scenes.json            # Scenes: backdrop image, ambient music, entry node, chapter metadata
    └── nodes.json             # Modular dialogue nodes: speaker, text, nextNodeId, scene transitions, audio
```
Plus runtime media assets: `assets/portraits/*`, `assets/story/*`, `assets/audio/music/*`, `assets/audio/sfx/*`.

### 1.2 Resolution & Accessor Surface
`GameContent` (the `content` package) parses the JSON pack at boot into typed models and exposes immutable map accessors by id:
- `manifest`: Global game metadata & TTS pronunciation replacements.
- `scenes`: Map of scene ID to `StoryScene` (backdrop, music, initial node).
- `nodes`: Map of node ID to `DialogueNode` (speaker, subtitle text, branching/advancement, cues).
- `characters` / `speakers`: Map of character ID to `DialogueSpeaker` (portrait, color, vocal pitch, gender).
- `speakerById(id)`: Resolves speaker definitions with graceful narrator fallback.

---

## 2. Audio & Narration Architecture

### 2.1 Multi-Voice On-Device TTS Engine (`StoryNarrator.kt`)
- Enumerates installed `android.speech.tts.Voice` instances on the Android device.
- Filters by locale and categorizes voices by physical gender and language traits.
- Intelligently maps distinct device voices to characters declared in `characters/*.json`.
- Per-character pitch modulation (`ttsPitch`: 0.7f – 1.4f) ensures clear vocal differentiation even when sharing TTS engines.
- **Normalization Pipeline:**
  1. Removes Markdown formatting asterisks (`*whispers*` -> `whispers`).
  2. Cleans screenplay scene numbers (`(42)`).
  3. Applies word-boundary phonetic replacement dictionary from `manifest.ttsReplacements`.
  4. Expands common abbreviations (e.g. `St.` -> `Saint`).

### 2.2 Background Music & Audio Ducking (`MusicManager.kt`)
- Continuous ambient music playback via Android `MediaPlayer` using `AudioAttributes.USAGE_MEDIA`.
- Persistent audio focus management with `AudioManager.AUDIOFOCUS_GAIN`.
- **Dynamic Speech Ducking:** Smoothly attenuates BGM volume to ~22% while dialogue is being spoken, and ramps back up upon sentence completion.
- Seamless track continuation: scene transitions between matching music assets avoid track restarts.

### 2.3 Sound Effects Engine (`SfxManager.kt`)
- Low-latency one-shot sound effects via Android `SoundPool`.
- Triggered dynamically per dialogue node via `node.sfxAsset`.

---

## 3. Narrative State Machine & Playback Loop

### 3.1 Linear & Branching Dialogue Nodes
Each node represents an atomic line of story dialogue:
- `id`: Unique node key.
- `speaker`: Resolved `DialogueSpeaker` reference.
- `side`: Layout positioning (`LEFT`, `RIGHT`, `CENTER_NARRATOR`).
- `text`: Dialogue line displayed in subtitle container and narrated by TTS.
- `nextNodeId`: Next dialogue node in sequence.
- `changeSceneId`: Triggers transition to a new scene backdrop and music track.
- `backgroundAsset`: Overrides scene background for a specific dialogue beat.
- `musicAsset`: Overrides background music track.
- `sfxAsset`: Plays sound cue on line display.

### 3.2 Auto-Play & Manual Advance
- **Manual Tap:** Tapping the story viewport advances immediately to the next node, cleanly canceling any active speech utterance.
- **Auto-Play:** Automatically narrates the line, waits for TTS completion, and schedules dialogue advance after a natural reading pause (1200ms).
- **Previous Node Navigation:** History stack preserves the last 150 visited nodes, allowing players to step back and re-listen to previous dialogue lines.

### 3.3 Dialogue Backlog & History
- Captures full transcript history with speaker names, titles, theme colors, and timestamps.
- Displayed in a searchable, scrollable modal accessible from the top-right toolbar.

---

## 4. Persistence Architecture (`SaveManager.kt`)

- **Multi-Slot Atomic Storage:** Manages 3 independent save slots using `AtomicFile` semantics to prevent corruption during unexpected shutdowns.
- **Saved State Snapshot:**
  - `currentSceneId` & `currentNodeId`: Exact narrative progression point.
  - `recentDialogueLog`: Full dialogue transcript up to 150 entries.
  - `isAutoPlay`: Player playback preference.
  - `isNarrationEnabled`, `speechRate`, `musicVolume`: Audio preferences.
- **Save Slot Summaries:** Slot metadata (chapter title, scene name, save timestamp) displayed on the title screen with visual slot selection.

---

## 5. UI & Presentation (`StoryScreen.kt`, `TitleScreen.kt`)

- **Jetpack Compose Native Rendering:** 60 FPS hardware-accelerated declarative UI.
- **Edge-to-Edge Insets:** Safe drawing padding with `WindowInsets.safeDrawing` to prevent notches, camera cutouts, and navigation bars from obscuring text or controls.
- **Portrait & Landscape Orientation Support:** Responsive layout with high-contrast text rendering and retro border styling.
- **On-Device Localization:** Multi-language UI support with on-device MLKit neural translation and pre-compiled built-in fallback dictionaries (`es`, `de`, `fr`, `pt`, `it`).

---

## 6. Verification Suite (`app/src/test/`)

- `AssetWiringTest`: Verifies that every scene's backdrop and music, and every character's portrait, resolve to actual files on disk.
- `EngineDecoupledAssetsTest`: Verifies that manifest fields, initial scene/node pointers, and story graphs satisfy engine contracts.
- `SaveSystemTest`: Verifies multi-slot save creation, deletion, atomic isolation, and dialogue backlog capping.
- `TtsNormalizationTest`: Validates phonetic text normalization and regex replacements.
- `MusicManagerTest`: Validates volume clamping, ducking states, and track switching logic.