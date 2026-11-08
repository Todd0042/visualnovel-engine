<p align="center">
  <a href="https://ko-fi.com/todd0042">
    <img src="https://raw.githubusercontent.com/Todd0042/creator-assets/main/banners/todd_creator_banner_v1.0.png" alt="Support Todd on Ko-fi" width="100%" />
  </a>
</p>

---

# 📖 Visual Novel Engine: Native Android Voice-Narrated Visual Novel Framework

[![Version](https://img.shields.io/badge/Version-1.5.0-ff2a85.svg)](https://github.com/Todd0042/visualnovel-engine/releases)
[![License: PolyForm Noncommercial](https://img.shields.io/badge/License-PolyForm%20Noncommercial%201.0.0-00f0ff.svg)](LICENSE)
[![Platform: Android 8.0+](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![16 KB Page Size](https://img.shields.io/badge/16%20KB-Compatible-success.svg)](https://developer.android.com/16kb-page-size)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%202024.10.01-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Unit Tests](https://img.shields.io/badge/Unit%20Tests-5%20Passing-39ff14.svg)](app/src/test/java/com/voicerpg/engine/)
[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Creator-F16061?style=flat&logo=ko-fi&logoColor=white)](https://ko-fi.com/todd0042)

> **A high-performance, modular native Android engine for building cinematic, voice-narrated visual novels. Featuring 100% data-driven JSON storylines, multi-voice Android Text-to-Speech character binding, ambient BGM management with dynamic dialogue ducking, 3-slot crash-safe atomic save states, top-right dialogue backlog logs, and edge-to-edge Compose rendering.**

```
       _________________________________________________________________________
      |  CAPTAIN REYES                           [ CHAPTER 1: THE DEPARTURE ]  |
      |  "The quiet here is absolute. Let us walk the halls and see what        |
      |   story this sanctuary has to tell."                                   |
      |_________________________________________________________________________|
      |  [▶ AUTO-PLAY]  [⏸ PAUSE]  [◀ PREV]  [▶ NEXT]  [📜 LOG]  [⚙ OPTIONS]     |
      |  VOICE: ON-DEVICE TTS (US-MALE) >>> BGM DUCKING: 22% AMBIENT LEVEL      |
      |_________________________________________________________________________|
```

---

## 📖 Table of Contents

* **Part 1 — Consumer Overview & Quick Start**
  * [🎭 What Is Visual Novel Engine?](#-what-is-visual-novel-engine)
  * [⚡ How a Visual Novel Works in 3 Steps](#-how-a-visual-novel-works-in-3-steps)
  * [✨ Core Engine Highlights at a Glance](#-core-engine-highlights-at-a-glance)
  * [🗣️ Multi-Voice Narration & Audio Drama Experience](#-multi-voice-narration--audio-drama-experience)
  * [🚀 Quick Start & Creating Your First Visual Novel](#-quick-start--creating-your-first-visual-novel)
* **Part 2 — Consolidated Technical Documentation & Architecture Reference**
  * [1. Multi-Voice Android Text-To-Speech Engine (`StoryNarrator.kt`)](#1-multi-voice-android-text-to-speech-engine)
  * [2. Ambient Background Music & Dynamic Audio Ducking (`MusicManager.kt`)](#2-ambient-background-music--dynamic-audio-ducking)
  * [3. Sound Effects Architecture (`SfxManager.kt`)](#3-sound-effects-architecture)
  * [4. Modular JSON Content System (`GameContent.kt`, `manifest.json`)](#4-modular-json-content-system)
  * [5. 3-Slot Atomic Persistence & Backlog History (`SaveManager.kt`)](#5-3-slot-atomic-persistence--backlog-history)
  * [6. Edge-to-Edge Compose Rendering & Safe Insets (`StoryScreen.kt`, `TitleScreen.kt`)](#6-edge-to-edge-compose-rendering--safe-insets)
  * [7. On-Device Translation & Built-In Multilingual Fallbacks (`TranslationManager.kt`)](#7-on-device-translation--built-in-multilingual-fallbacks)
  * [8. Automated Verification Suite (`app/src/test/`)](#8-automated-verification-suite)
  * [9. Building & Distributing Standalone Visual Novels](#9-building--distributing-standalone-visual-novels)
  * [10. License & Creator Support](#10-license--creator-support)

---

# Part 1 — Consumer Overview & Quick Start

### 🎭 What Is Visual Novel Engine?
**Visual Novel Engine** is a dedicated, production-grade native Android framework crafted specifically for narrative fiction, visual novels, and audio dramas. Born out of the need for an ultra-clean, content-agnostic storytelling engine, it decouples the game engine from narrative assets:
- **100% Data-Driven Narrative**: Story scripts, scenes, character definitions, dialogue nodes, and audio cues reside entirely in modular JSON files.
- **Pure Visual Novel Mechanics**: Zero combat remnants, zero RPG stat calculations, zero turn-based ATB noise—just immersive cinematic storytelling.
- **Audio Drama Immersion**: Spoken dialogue powered by Android's on-device Text-To-Speech engine gives every character a unique voice, intonation, and pitch.
- **Offline & Private**: Runs 100% locally on Android devices without mandatory logins, cloud servers, or external tracking.

---

### ⚡ How a Visual Novel Works in 3 Steps

1. **Title Menu & Save Slot Selection:** Players launch into a stylized title screen with persistent save slot management (3 independent slots with scene previews and chapter indicators) or dive straight into a New Journey.
2. **Cinematic Story Viewport:** Dialogue flows line by line with expressive character portraits, background art, ambient soundscapes, and synchronized subtitles.
3. **Hands-Free Auto-Play or Manual Control:** Players can relax and let the engine narrate continuously with intelligent pause cadences, tap to skip forward, step back to previous dialogue lines, or review the complete dialogue transcript in the top-right Backlog.

---

### ✨ Core Engine Highlights at a Glance

| Feature | Description | Engine Implementation |
| :--- | :--- | :--- |
| **🎙️ On-Device TTS Narration** | Reads dialogue aloud using native Android TTS voices with custom pitches and phonetics | `StoryNarrator.kt` |
| **🎵 Dynamic BGM Ducking** | Automatically attenuates background music during speech and smoothly restores volume | `MusicManager.kt` |
| **🔊 One-Shot SFX Cues** | Low-latency audio cues triggered dynamically per dialogue node via `SoundPool` | `SfxManager.kt` |
| **💾 3-Slot Atomic Saves** | Crash-safe `AtomicFile` persistence with auto-save and slot inspection | `SaveManager.kt` |
| **📜 150-Line Backlog Log** | Full scrollable dialogue history modal with timestamps and character color tags | `DialogueBacklogDialog.kt` |
| **📱 Edge-to-Edge Compose** | Hardware-accelerated UI with safe notch/navigation insets | `StoryScreen.kt`, `TitleScreen.kt` |
| **🌐 On-Device Translations** | Multilingual UI support with offline neural translation and dictionary fallbacks | `TranslationManager.kt` |
| **🧪 Automated Test Suite** | Validates asset wiring, graph reachability, and save integrity out of the box | `AssetWiringTest.kt` |

---

### 🗣️ Multi-Voice Narration & Audio Drama Experience

Visual novels built on this engine are designed to be both read and listened to:
- **Phonetic Normalization:** Cleans Markdown formatting (`*sighs*`), strips scene numbering tags (`(14)`), and resolves custom pronunciation dictionaries (e.g., proper nouns, sci-fi terms, character acronyms).
- **Physical Voice Assignment:** Detects available English and localized voices installed on the device and maps distinct physical voices to male, female, and narrator roles.
- **Vocal Pitch Modulation:** Characters can be assigned subtle pitch shifts (e.g. `0.85` for a gruff veteran, `1.20` for an energetic companion), providing acoustic differentiation even when sharing TTS engines.

---

### 🚀 Quick Start & Creating Your First Visual Novel

Creating a new visual novel takes only minutes:

1. **Clone the Engine:**
   ```bash
   git clone https://github.com/Todd0042/visualnovel-engine.git my-visual-novel
   cd my-visual-novel
   ```

2. **Configure App Identity:**
   - In `app/build.gradle.kts`: Update `applicationId = "com.yourstudio.yourgame"`.
   - In `app/src/main/res/values/strings.xml`: Set `<string name="app_name">Your Game Title</string>`.

3. **Drop In Your Content:**
   - Edit `app/src/main/assets/game/manifest.json`.
   - Add character definitions to `app/src/main/assets/game/characters/`.
   - Define scenes and backdrops in `app/src/main/assets/game/story/scenes.json`.
   - Write your dialogue script in `app/src/main/assets/game/story/nodes.json`.

4. **Verify & Run:**
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```

> 📖 **Complete Step-by-Step Creator Walkthrough:** See [STARTER_GUIDE.md](STARTER_GUIDE.md).  
> 📐 **Complete Technical Architecture & Engine Specs:** See [DESIGN.md](DESIGN.md).

---

# Part 2 — Consolidated Technical Documentation & Architecture Reference

### 1. Multi-Voice Android Text-To-Speech Engine (`StoryNarrator.kt`)
The narration subsystem wraps `android.speech.tts.TextToSpeech` with a stateful, coroutine-friendly lifecycle:
- **Voice Discovery (`assignCharacterVoices`):** Enumerates `engine.voices`, discards network-dependent voices, filters by language, and allocates distinct male and female models.
- **Speech Normalization (`normalizeTtsText`):**
  ```kotlin
  internal fun normalizeTtsText(input: String): String {
      var text = input.replace("*", "")
      text = text.replace(Regex("""\s*\(\d{1,3}\)"""), "")
      GameContent.manifest.ttsReplacements.forEach { (target, replacement) ->
          if (target.endsWith(".")) {
              text = text.replace(target, replacement, ignoreCase = true)
          } else {
              val wordRegex = Regex("""\b${Regex.escape(target)}\b""", RegexOption.IGNORE_CASE)
              text = wordRegex.replace(text, replacement)
          }
      }
      return text.trim()
  }
  ```
- **Utterance Progress Tracking:** Listens to `UtteranceProgressListener.onDone` to notify `StoryViewModel` when speech concludes, powering automated dialogue advance.

---

### 2. Ambient Background Music & Dynamic Audio Ducking (`MusicManager.kt`)
- **Audio Routing:** Utilizes `AudioAttributes.USAGE_MEDIA` and `AudioAttributes.CONTENT_TYPE_MUSIC` with `AudioManager.AUDIOFOCUS_GAIN`.
- **Dynamic Speech Ducking:** When `duckForSpeech()` is invoked, BGM volume smoothly interpolates down to `0.22x` base volume over 200ms. Upon `restoreFromSpeech()`, volume ramps back up to user-selected level.
- **Track Continuity:** If the next dialogue node or scene references the currently playing asset path, playback continues seamlessly without resetting the playback head.

---

### 3. Sound Effects Architecture (`SfxManager.kt`)
- Sound effects are managed via Android `SoundPool` configured with `AudioAttributes.USAGE_GAME` and `CONTENT_TYPE_SONIFICATION`.
- Supports up to 4 concurrent audio streams for interface feedback, environment ambience, and dramatic story stingers.

---

### 4. Modular JSON Content System (`GameContent.kt`, `manifest.json`)
The content pipeline reads files from `app/src/main/assets/game/` at startup:
- `manifest.json`: Stores `gameTitle`, `gameSubtitle`, `initialSceneId`, `initialNodeId`, `titleBackgroundAsset`, and `ttsReplacements`.
- `characters/*.json`: Serializes into `CharacterDto` defining `name`, `portraitAsset`, `themeColor`, `ttsPitch`, and `gender`.
- `story/scenes.json`: Serializes into `StorySceneDto` declaring `backgroundAsset`, `musicAsset`, and `chapterTitle`.
- `story/nodes*.json`: Supports split script files (`nodes.json`, `nodes_act1.json`, `nodes_act2.json`) loaded concurrently into an in-memory graph.

---

### 5. 3-Slot Atomic Persistence & Backlog History (`SaveManager.kt`)
- **Storage Strategy:** Uses Android's `AtomicFile` to perform atomic writes to `filesDir/saves/slot_X.json`.
- **Automatic Auto-Save:** Every dialogue line advance updates the active slot.
- **Backlog Retention:** Preserves the last 150 dialogue lines with speaker IDs, names, text, and scene context for in-game review.

---

### 6. Edge-to-Edge Compose Rendering & Safe Insets (`StoryScreen.kt`, `TitleScreen.kt`)
- Built with Jetpack Compose Material3.
- Utilizes `WindowInsets.safeDrawing` to ensure top status bars, camera cutouts, and bottom navigation bars never clip dialogue text or interactive controls.
- Dynamic responsive layout adapts automatically between portrait phones and landscape tablets.

---

### 7. On-Device Translation & Built-In Multilingual Fallbacks (`TranslationManager.kt`)
- Integrates Google MLKit On-Device Translation (`com.google.mlkit:translate`).
- Provides pre-compiled built-in fallback dictionaries (`BuiltInDictionaries.kt`) for Spanish (`es`), German (`de`), French (`fr`), Portuguese (`pt`), and Italian (`it`).
- Translates UI menus, options, and system strings instantly without network dependencies.

---

### 8. Automated Verification Suite (`app/src/test/`)
All engine invariants are validated in headless JVM unit tests:
```bash
./gradlew testDebugUnitTest
```
- `AssetWiringTest.kt`: Asserts that 100% of scene backgrounds, music tracks, and character portraits exist on disk.
- `EngineDecoupledAssetsTest.kt`: Asserts that manifest pointers and dialogue graphs resolve cleanly.
- `SaveSystemTest.kt`: Tests multi-slot independence, save deletion, and backlog bounds.
- `TtsNormalizationTest.kt`: Verifies phonetic transformations and regex replacements.
- `MusicManagerTest.kt`: Verifies volume bounds, ducking triggers, and track selection.

---

### 9. Building & Distributing Standalone Visual Novels
To build a release-ready APK:
```bash
./gradlew assembleRelease
```
Ensure you provide your own release keystore in `app/build.gradle.kts` or sign the generated APK with `apksigner`.

---

### 10. License & Creator Support
- **Non-Commercial License (Free & Open Source)**: Visual Novel Engine is licensed under the **[PolyForm Noncommercial License 1.0.0](LICENSE)**. Personal, educational, hobbyist, and non-commercial game development is 100% free.
- **Commercial Licensing Inquiries**: For commercial distribution inquiries, contact **[todd.0042.git@gmail.com](mailto:todd.0042.git@gmail.com)**.

---

<p align="center">
  If you enjoy building visual novels with this engine, consider supporting open-source development:<br/><br/>
  <a href="https://ko-fi.com/todd0042" target="_blank">
    <img src="https://img.shields.io/badge/☕_Support_on_Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white" alt="Support on Ko-fi">
  </a>
</p>

---

<p align="center">
  <a href="https://ko-fi.com/todd0042">
    <img src="https://raw.githubusercontent.com/Todd0042/creator-assets/main/banners/todd_creator_banner_v2.0.png" alt="Support Todd on Ko-fi" width="100%" />
  </a>
</p>
