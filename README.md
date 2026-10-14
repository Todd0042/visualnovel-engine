# VoiceRPG Engine: Native Android Voice-Controlled JRPG Framework

A high-performance, native Android game engine designed for building audio-first, voice-commanded retro JRPGs. Every aspect of gameplay—incantations, tactical party combat, visual novel exploration, dialogue choices, and system toggles—can be operated hands-free via natural voice or via classic retro touch controls.

> [!NOTE]
> For a step-by-step tutorial on creating your own game with this engine, see [STARTER_GUIDE.md](STARTER_GUIDE.md).  
> For complete mathematical equations, combat design, and architectural specs, see [DESIGN.md](DESIGN.md).  
> For upcoming engine features and architectural roadmap items, see [TODO.md](TODO.md).

---

## Engine Features & Pillars

### 1. 🎤 Hands-Free Voice Control & Auto-Listen Loop
* **Continuous Microphone Loop:** Uses Android's `SpeechRecognizer` with automatic re-arming after TTS narration finishes.
* **Fuzzy & Phonetic Matching:** Utterance parsers match spoken player commands against spell names, targets, and dialogue choices even in noisy environments.
* **Global Voice Meta-Commands:** Players can query system state anytime by speaking:
  * *"Status"*, *"Enemies"*, *"Fellowship"*, *"Help"*, *"Options"*, *"Pocket mode"*, *"Auto listen"*, *"Toggle narration"*.

### 2. 🗣️ Multi-Voice Character Narration Engine
* **Physical Voice Binding:** Programmatically discovers installed Android TTS voices via `android.speech.tts.Voice` and binds distinct physical voices to different characters (e.g. Hero, Companion, Narrator).
* **Expressive Modulation:** Modulates pitch, speech rate, and intonation dynamically per character and dialogue line.
* **Queue & Interrupt Handling:** Synchronous or forced speech cues with visual typewriter synchronization.

### 3. ⚔️ Real-Time ATB (Active Time Battle) Combat System
* **Dynamic Initiative:** Agility-driven ATB gauge ticks with simultaneous hero/enemy actions and pause/resume capabilities.
* **Elemental Affinities & Reactions:** Physical, Fire, Ice, Lightning, Earth, Light, and Dark affinities with damage multipliers (Weakness, Resist, Absorb, Immune).
* **Comprehensive Status System:** Buffs, debuffs, damage-over-time (Bleed, Poison, Burn), crowd control (Stun, Freeze, Silence), and defensive shields.
* **Intelligent Enemy AI:** Behavior trees prioritizing threat, health thresholds, status exploit, and dynamic party targeting.

### 4. 🎵 Dynamic Resonance & Acoustic Scoring
* **Pitch & Vocal Analysis:** Real-time audio FFT / YIN pitch detection measuring pitch stability, volume intensity, and chant rhythm.
* **Resonance Tiers:** Grants tactical damage and effect bonuses (Basic +10%, Adept +40%, Master +80%, Mythic +125%, Transcendental +200%).
* **Novelty Cache:** Anti-spam system that discourages repeating identical phrases by applying diminishing resonance returns until novelty resets.

### 5. 🎧 Screenless Pocket Mode (Audio-First Playability)
* **100% Playable Without Looking:** Built from the ground up to be enjoyed while walking with your phone in your pocket, or by visually impaired players.
* **AMOLED Touch Guard:** True-black touch shield prevents accidental pocket presses while keeping audio and auto-listen active.
* **1-Tap Playstyle Presets:** Instant configuration cards:
  * **🎧 Pocket Walk:** Screen locked & dark, auto-listen mic, full narration.
  * **📖 Storybook:** Hands-free listening with screen awake, full audio drama.
  * **🎮 Classic Tactical:** Visual RPG with muted combat speech and manual mic taps.

### 6. 📜 Visual Novel Dialogue & Choice Elimination
* **Narrative State Machine:** Branching dialogue trees supporting character portraits, scene backgrounds, and audio triggers.
* **Automatic Choice Elimination:** Hub nodes filter out visited branches and automatically advance once all sub-stories are resolved.

---

## Directory & Package Architecture

```
com.voicerpg.engine/
├── audio/            # SpeechManager, CombatNarrator, PitchDetector, MusicManager, SfxManager
├── combat/           # DamageResolver, EnemyBrain AI, StatusSystem, AffinityTable, Movesets
├── content/          # GameContent, GameContentPack, dynamic schema models & asset loaders
├── engine/           # IntentParser, ResonanceEngine, NoveltyCache, StoryChoiceMatcher, SaveManager
├── model/            # CombatModels, StoryModels, SaveModels, BattleEnvironment, ResonanceResult
├── ui/               # Jetpack Compose UI
│   ├── combat/       # RetroBattleScreen, ResonanceConsole, InitiativeTrack, Flanks, OptionsDialog
│   ├── story/        # StoryScreen, DialogueBacklogDialog, AssetLoaders
│   ├── pocket/       # PocketModeTouchGuard (AMOLED protection)
│   ├── vfx/          # ParticleCanvas, SpellVfxEngine (Procedural retro spell VFX)
│   ├── sprites/      # PixelCharacterView, PixelSpriteData (16-bit procedural sprites)
│   └── setup/        # AudioSetupScreen, MicPermissionRationaleDialog
└── viewmodel/        # CombatViewModel, StoryViewModel
```

---

## Quickstart: Creating a New Game

1. **Clone as a template or branch:** this repo builds the engine's **reference app** (`applicationId` `com.voicerpg.engine.reference`) for testing the framework. To make a game, clone it and follow the starter guide:
   ```bash
   git clone https://github.com/Todd0042/voice-rpg-engine.git my-voice-game
   ```
2. **Build and test the reference project:**
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```
3. **Install to your phone via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. **Follow [STARTER_GUIDE.md](STARTER_GUIDE.md)** to swap in your own world: game content is drop-in **JSON + assets** under `app/src/main/assets/game/` (story graph, characters, spells, enemies, encounters, voices, art, music) — no engine code changes required.

> ⚠️ **Uniqueness rule (AGENTS.md §8):** every game built with this engine must be **its own unique app** — a unique `applicationId`, app name, icon, and game content. Never ship an app that keeps the reference `applicationId` or another game's identity; on Android, two APKs with the same `applicationId` overwrite each other on a device (which is why the reference app previously replaced *Commander America* when both used `com.voicerpg.engine`).
