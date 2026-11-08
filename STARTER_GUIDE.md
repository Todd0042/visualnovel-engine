# Visual Novel Engine: Creator & Starter Guide

This guide walks you through creating your own voice-narrated, cinematic visual novel using the **Visual Novel Engine** for Android. The engine is **100% data-driven**: your entire narrative—characters, portraits, dialogue scripts, scenes, music, sound effects, and voice settings—lives as modular JSON + asset files under `app/src/main/assets/game/`. You build a new visual novel by **editing and adding content files only**; you never touch engine Kotlin code.

The engine ships with a complete reference *template story* (the "Proving Grounds" / "Echo Hall" sandbox) that demonstrates every visual novel capability:
* Spoken Android TTS dialogue with per-character vocal pitch, gender filtering, and regional accents.
* Fluid background music management with dynamic dialogue ducking.
* 3-slot persistent save/load system with auto-save and selectable save slots.
* Full dialogue transcript backlog dialog (with top-right history access).
* Multi-language on-device translation support (MLKit neural translations with built-in dictionary fallback).
* Edge-to-edge Compose rendering with safe insets and portrait/landscape adaptation.

---

## Step 1: Change Your Game's Identity (REQUIRED)

> ⚠️ **Every game built from this engine must be its own unique Android app — this step is not optional.** Android treats `applicationId` as the install identity: two APKs that share an `applicationId` **silently replace each other** on the same device. See AGENTS.md §8.

1. **App Name:** `app/src/main/res/values/strings.xml`
   ```xml
   <string name="app_name">Your Visual Novel Title</string>
   ```
2. **Unique Application ID (mandatory):** `app/build.gradle.kts` → `applicationId`. Use a reverse domain identifier you own, e.g. `com.yourstudio.yournovel`.
3. **App Launcher Icon:** Replace launcher icons under `app/src/main/res/mipmap-*/` and `drawable-*/` with your game's logo art.

---

## Step 2: Set Your Game Manifest

Edit `app/src/main/assets/game/manifest.json`:

```json
{
  "gameTitle": "Chronicles of the Horizon",
  "gameSubtitle": "A Voice-Narrated Sci-Fi Visual Novel",
  "initialSceneId": "scene_hangar_bay",
  "initialNodeId": "hangar_intro_01",
  "titleBackgroundAsset": "story/bg_title.jpg",
  "ttsReplacements": {
    "AI": "A.I.",
    "Dr.": "Doctor",
    "Capt.": "Captain",
    "St.": "Saint"
  }
}
```

- `initialSceneId`: Key of the scene loaded when starting a new game.
- `initialNodeId`: ID of the very first dialogue node in the script.
- `titleBackgroundAsset`: Title screen backdrop image under `assets/`.
- `ttsReplacements`: Pronunciation dictionary mapping abbreviations or proper names to phonetic equivalents for Android's on-device TextToSpeech.

---

## Step 3: Define Your Characters

Create JSON files under `app/src/main/assets/game/characters/` (e.g. `captain_reyes.json`, `narrator.json`):

```json
{
  "id": "reyes",
  "name": "Captain Reyes",
  "title": "Commander of the Vanguard",
  "portraitAsset": "portraits/reyes.jpg",
  "themeColor": "#4DA8DA",
  "ttsPitch": 0.95,
  "gender": "male",
  "allowedVoiceRegions": ["US", "CA", "GB"],
  "samplePhrase": "Engines are hot. Let us see what lies past the jump gate."
}
```

- `id`: Unique identifier referenced by dialogue nodes (`speakerId`).
- `name`: Display name shown in dialogue speaker boxes.
- `portraitAsset`: Path under `assets/` to the character's bust portrait (typically 1:1 or 4:5 aspect ratio).
- `themeColor`: Hex color accent for the character's nameplate and UI borders.
- `ttsPitch`: Float pitch modifier (`0.7` for deep bass, `1.3` for high treble).
- `gender`: `"male"`, `"female"`, or `"neutral"` used for intelligent Android TTS voice assignment.

---

## Step 4: Define Your Story Scenes

Edit `app/src/main/assets/game/story/scenes.json`:

```json
{
  "scenes": [
    {
      "id": "scene_hangar_bay",
      "name": "Hangar Bay 4",
      "chapterTitle": "Chapter 1: The Departure",
      "backgroundAsset": "story/bg_hangar.jpg",
      "initialNodeId": "hangar_intro_01",
      "musicAsset": "audio/music/bgm_exploration.ogg",
      "chapterIndex": 1,
      "actNumber": 1,
      "actTitle": "Act I: Into the Deep",
      "milestoneTitle": "Chapter 1: The Departure",
      "milestoneLocation": "Hangar Bay 4",
      "milestoneSummary": "You boarded the vessel and met with Captain Reyes.",
      "milestoneIcon": "🚀",
      "defaultObjective": "Converse with Captain Reyes and initiate the jump sequence."
    }
  ]
}
```

- `backgroundAsset`: High-resolution backdrop image under `assets/` (e.g. 1920x1080 or 1280x720).
- `musicAsset`: Ambient looping background music under `assets/audio/music/`.

---

## Step 5: Write Your Dialogue Script

Write your dialogue nodes in `app/src/main/assets/game/story/nodes.json`. For large stories, you can modularize by splitting into multiple files (e.g. `nodes_act1.json`, `nodes_act2.json`):

```json
{
  "nodes": [
    {
      "id": "hangar_intro_01",
      "speakerId": "narrator",
      "side": "CENTER_NARRATOR",
      "text": "The hum of sub-light thrusters vibrates through the deck plates of Hangar 4.",
      "nextNodeId": "reyes_greeting"
    },
    {
      "id": "reyes_greeting",
      "speakerId": "reyes",
      "side": "LEFT",
      "text": "Glad to see you made it aboard in time. We leave orbit in three minutes.",
      "nextNodeId": "hangar_bridge_transition"
    },
    {
      "id": "hangar_bridge_transition",
      "speakerId": "narrator",
      "side": "CENTER_NARRATOR",
      "text": "Together you walk through the airlock toward the command deck.",
      "changeSceneId": "scene_command_bridge",
      "nextNodeId": "bridge_arrival"
    }
  ]
}
```

### Dialogue Node Attributes:
- `id`: Unique node key.
- `speakerId`: Character ID matching a definition in `characters/` (or `"narrator"`).
- `side`: `"LEFT"`, `"RIGHT"`, or `"CENTER_NARRATOR"` for character portrait positioning.
- `text`: Spoken dialogue and displayed subtitle text.
- `nextNodeId`: The next dialogue node to advance to on tap or auto-play.
- `changeSceneId` (optional): Seamlessly transitions the background and music to a new scene.
- `backgroundAsset` (optional): Swaps the visual backdrop for this specific line without changing scenes.
- `musicAsset` (optional): Changes ambient BGM track.
- `sfxAsset` (optional): Plays a one-shot sound effect (e.g. `audio/sfx/door_chime.ogg`).

---

## Step 6: Audio & Graphic Assets Structure

Organize your files under `app/src/main/assets/`:

```
app/src/main/assets/
├── audio/
│   ├── music/
│   │   ├── bgm_title.ogg
│   │   └── bgm_exploration.ogg
│   └── sfx/
│       └── door_chime.ogg
├── portraits/
│   ├── reyes.jpg
│   └── template_narrator.jpg
├── story/
│   ├── bg_hangar.jpg
│   └── bg_title.jpg
└── game/
    ├── manifest.json
    ├── characters/
    │   ├── narrator.json
    │   └── reyes.json
    └── story/
        ├── scenes.json
        └── nodes.json
```

---

## Step 7: Verification & Automated Tests

Run the engine's built-in unit tests to verify that all assets, character portraits, scene backdrops, and dialogue linkages are valid and uncorrupted:

```bash
./gradlew testDebugUnitTest
```

The test suite validates:
1. Every scene's `backgroundAsset` and `musicAsset` exist on disk.
2. Every character's `portraitAsset` exists on disk.
3. Every dialogue node has valid speaker bindings and non-empty text.
4. The 3-slot save system functions with dialogue backlog preservation.
5. TTS normalization and phonetic replacements operate accurately.

---

## Step 8: Build & Run

To assemble a debug APK and install on a connected Android phone or emulator:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```