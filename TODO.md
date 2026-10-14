# VoiceRPG Engine: Roadmap & Engine TODOs

This document tracks upcoming architectural features, generic engine extensions, and content-neutral mechanics for the VoiceRPG native Android engine.

---

## High Priority / Architectural Roadmap

### 1. ⚔️ Data-Driven Story-Based Battle Circumstances & Environmental Mechanics
- **Context:** Previous builds had hardcoded boss phase thresholds (e.g. *Malakor's Cords of Severance*) and mid-battle companion recruitment (e.g. *Zephyr's Defection*) inside `CombatViewModel.kt`. These violated Rule 1 (Content Neutrality) and have been removed.
- **Objective:** Enable encounters to support unique narrative circumstances, phase transitions, and environmental effects purely via drop-in JSON data schema declarations in `encounters.json` (or `environments.json`), without touching generic engine code.
- **Key Capabilities to Design:**
  1. **Environmental Effects & Hazards:**
     - Turn-tick or round-based passive effects tied to the encounter's `environment` or `environmentId` (e.g. ongoing ambient damage, periodic healing, aura dampening, or mana drain).
     - Narrative announcement lines spoken aloud by `CombatNarrator` when environmental hazards trigger.
  2. **Narrative Mid-Battle Phase Triggers (`BossPhaseRule` / `NarrativeTrigger` schema):**
     - Condition checks evaluated at turn boundaries or action completion:
       - `triggerCondition`: e.g. `HP_BELOW_PERCENT`, `TURN_COUNT`, `SPECIFIC_ENEMY_DEFEATED`, `PARTY_SURVIVED_ROUNDS`.
       - `bannerText`: Floating combat banner announcing the phase or story event.
       - `narrationText`: Spoken narrative line by the narrator or character.
       - `screenShake`: Vibration / screen rumble intensity.
       - `sfxCue`: Impact or transformation sound effect.
  3. **Mid-Battle Narrative Actions:**
     - `joinPartyMemberId`: Seamlessly adds a declared companion (from `characters/*.json`) to the party mid-battle.
     - `summonEnemies`: Spawns wave reinforcements or transforms bosses into second forms.
     - `applyPartyStatus` / `applyEnemyStatus`: Inflicts mass status effects (e.g. overload, stagger, silence).
  4. **Custom Battle Resolution Rules:**
     - Support encounters where defeat does not trigger a game over, but instead sets narrative flags and advances the story (`advanceStoryOnDefeat: true`), as well as battles with non-standard victory conditions (e.g. `SURVIVE_TURNS`).

---

## Content-Neutrality Checklist for Engine Changes

- [x] Manifest fields for system text: `tutorialCompletionText`, `voicePermissionTitle`, `voicePermissionSubtitle`, `voicePermissionPrompt`.
- [x] Dynamic timeline icon fallback from `GameContent.allSpeakers` rather than hardcoded character IDs.
- [x] Dynamic companion speaker registration in `CombatNarrator` and `MainActivity`.
- [x] Automatic choice reading default in Screenless Pocket Mode (`isEyesFreeMode`) with dedicated toggle above Konami code.
- [x] Orientation-aware background loading for title and combat environments.
- [ ] Story-based battle environment circumstances and narrative phase triggers (item #1 above).
