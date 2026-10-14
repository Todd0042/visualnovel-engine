# VoiceRPG Engine: Game Creator & Starter Guide

This guide walks you through creating your own voice-commanded RPG using the **VoiceRPG Engine**. The engine is **100% data-driven**: the entire game — identity, characters, spells, enemies, encounters, story, voices, art, and music — lives as JSON + asset files under `app/src/main/assets/game/`. You build a new game by **editing and adding content files only**; you never touch engine Kotlin code.

The engine ships with a complete *template game* (the "Proving Grounds" / "Echo Hall" sandbox) that demonstrates every engine feature. The unit test suite is content-agnostic — it validates engine invariants against whatever content exists, so a correctly-formed content pack yields a green suite out of the box.

---

## Step 1: Change Your Game's Identity (REQUIRED)

> ⚠️ **Every game built from this engine must be its own unique Android app — this step is not optional.** Android treats `applicationId` as the install identity: two APKs that share an `applicationId` **silently replace each other** on the same device (the newest install overwrites the existing app and wipes its data). The engine's own reference app is `com.voicerpg.engine.reference`; shipping with it — or with any other game's id — will overwrite that app. See AGENTS.md §8.

1. **App name:** `app/src/main/res/values/strings.xml`
   ```xml
   <string name="app_name">Your Game Title</string>
   ```
2. **Unique application id (mandatory):** `app/build.gradle.kts` → `applicationId`. Use an identifier you own, e.g. `com.yourstudio.yourgame`. A unique `applicationId` is what lets your game coexist with every other VoiceRPG title on the same device instead of replacing it.
3. **App icon:** replace the launcher icons under `app/src/main/res/mipmap-*/` and `drawable-*/` with your game's own art — never ship the engine's default icon as a released game.

---

## Step 2: Set Your Game Manifest

Edit `app/src/main/assets/game/manifest.json`:

```json
{
  "gameTitle": "Your Game Title",
  "gameSubtitle": "A short tagline",
  "initialSceneId": "scene_your_start",
  "initialNodeId": "your_intro_node",
  "defaultEncounterId": "first_battle",
  "startingAchievementId": "QUEST_BEGUN",
  "victoryLine": "Victory! ...",
  "defeatLine": "Defeat. ..."
}
```

- `initialSceneId`/`initialNodeId` must resolve to entries in `story/scenes.json` / `story/nodes.json`.
- Victory/defeat lines are narrated when a battle ends.

---

## Step 3: Write Characters

Copy `characters/hero.json` and `characters/narrator.json` as your starting point. A character is both a dialogue speaker and a combatant:

```json
{
  "id": "mira",
  "role": "hero",
  "name": "Mira of the Deep",
  "title": "Tideborn",
  "portraitAsset": "portraits/mira.jpg",
  "spriteId": "template_hero",
  "themeColor": "#00BFFF",
  "ttsPitch": 1.0,
  "samplePhrase": "The sea answers when I speak.",
  "preferredVoiceId": null,
  "voiceAliases": ["mira", "hero", "protagonist"],
  "loreClass": "Spellblade",
  "combat": {
    "maxHp": 240, "maxMp": 140, "speed": 70, "defense": 6,
    "spellIds": ["power_strike", "flame_spark", "mending_touch", "attune"],
    "growth": { "maxHp": 14, "maxMp": 12, "speed": 1, "defense": 1, "potencyPct": 0.035 }
  }
}
```

- `id` is the durable key: story nodes reference `speakerId`, encounters reference party member ids, saves store stats by id.
- `voiceAliases` let players target/summon her by voice ("attack the caster", "Defend, Mira").
- `preferredVoiceId`: set a device TTS voice id to bind a distinct physical voice; leave `null` to use `ttsPitch` modulation. Characters with a `preferredVoiceId` are exported to `GameContent.preferredVoiceIds` automatically.
- `spellIds` reference `spells/spells.json`.

---

## Step 4: Define Hero Classes (Character Creation)

Copy `classes/*.json` (elementalist, battlemage, chanter, shadowweaver). Classes power the character-creation screen:

```json
{
  "id": "stormcaller",
  "title": "Stormcaller",
  "subtitle": "Voice of the Tempest",
  "description": "Calls lightning and thunder with every breath.",
  "startingHp": 240, "startingMp": 160, "startingSpeed": 70, "startingBaseAttack": 22,
  "preferredSchool": "ELECTROMANCY",
  "starterSpellIds": ["chain_lightning", "thunder_cleave", "attune"],
  "voiceKeywords": ["stormcaller", "storm", "caller", "tempest"],
  "growth": { "maxHp": 12, "maxMp": 14, "speed": 1, "defense": 1, "potencyPct": 0.04 }
}
```

---

## Step 5: Build Your Spell Library

Edit `spells/spells.json`. Every voice-chantable ability lives here:

```json
{
  "id": "fireball",
  "name": "Fireball",
  "school": "PYROMANCY",
  "basePower": 65, "mpCost": 15,
  "status": "BURN",
  "description": "Roaring sphere of flame.",
  "exampleChant": "Fireball the brute!",
  "aliases": ["fire", "flame", "burn", "blaze", "inferno"]
}
```

- `school` must be one of `PYROMANCY / CRYOMANCY / ELECTROMANCY / NATURE / HOLY / SHADOW / PHYSICAL`; it drives the affinity matrix and the resonance thesaurus.
- `aliases` are fuzzy voice-match keywords for hands-free casting.
- Support flags (set opt-in): `isHeal`, `hitsAll`, `isGuard`, `lifesteal`, `manaRestorePct` (the free "Attune" breath discipline — `basePower: 0`, `mpCost: 0`).

**Resonance note:** the engine's thematic thesaurus (`SpellThesaurus` in engine code) has per-school root words, and the per-tier chant presets are calibrated for spell names matching **exactly one** root. When you add a spell, keep its `name` covered by a single school root word; the calibration test covers the whole catalog. Spell names not yet covered by the school's generic lexicon are an engine-lexicon gap you may fix in `SpellThesaurus` (generic words only — never game-specific terms bred in `app/src/main`).

---

## Step 6: Create Enemies & Encounters

### 6.1 Enemy templates (`enemies/enemies.json`)
Reusable definitions referenced by encounters via `templateId`:

```json
{
  "id": "mire_wyrm",
  "name": "Mire Wyrm",
  "subtitle": "Warden of the Fen",
  "family": "VERDANT",
  "selfElement": "NATURE",
  "hp": 380, "attack": 24, "defense": 8, "speed": 40,
  "xpReward": 150,
  "movesetId": "flesh_bruiser",
  "spriteId": "template_brute",
  "spriteTint": "#4CAF50",
  "isBoss": true
}
```

- `family` drives the affinity matrix: `FLESH / UNDEAD / CONSTRUCT / VERDANT / VOID / RADIANT / SILENCE`.
- `movesetId` selects AI behavior from the generated `MovesetTable`.

### 6.2 Encounters (`encounters/encounters.json`)
```json
{
  "id": "fen_ambush",
  "name": "Fen Ambush",
  "environment": "FOREST",
  "party": ["mira"],
  "enemies": [
    { "templateId": "mire_wyrm", "id": "wyrm_1", "isTargeted": true }
  ],
  "bossPhases": [
    { "enemyId": "wyrm_1", "hpBelow": 0.5,
      "bannerText": "WYRM SUMMONS HATCHLINGS",
      "narrationText": "The fen erupts with wyrmlings!",
      "summonEnemyId": "wyrmling", "summonCount": 2 }
  ]
}
```

- `party`: character ids; empty = the active campaign party.
- `bossPhases`: fire once when the boss drops below `hpBelow` — announce (`bannerText`/`narrationText`), crush the party (`reducePartyToHpPct` + `executeBelowHpPct`), and/or summon reinforcements.
- `midBattleJoin`: a character joins the party after N enemy actions.

---

## Step 7: Write Your Story

The story is a **graph**: `story/scenes.json` (locations) + `story/nodes.json` (narrative beats and choices).

### 7.1 Scenes
```json
{
  "id": "mire_fen",
  "name": "The Mire Fen",
  "chapterTitle": "Chapter 1: The Whispering Fen",
  "backgroundAsset": "story/fen.jpg",
  "initialNodeId": "fen_intro",
  "ambientDescription": "Mist coils between black cattails.",
  "musicAsset": "audio/music/bgm_fen.ogg",
  "chapterIndex": 1, "actNumber": 1, "actTitle": "Act I: The Lower Depths",
  "milestoneTitle": "Chapter 1: The Whispering Fen",
  "milestoneLocation": "The Mire Fen",
  "milestoneSummary": "You crossed the fen and learned the local wyrm's weakness.",
  "milestoneIcon": "fen",
  "defaultObjective": "Reach the Sunken Bell at the fen's heart."
}
```
`chapterIndex`/`actNumber`/milestone fields feed the spoken Quest Recap and the debug chapter-warp menu — nothing is hardcoded.

### 7.2 Nodes — the state machine

Basic beat with traits:
```json
{ "id": "fen_intro", "speakerId": "narrator", "side": "CENTER_NARRATOR",
  "text": "Mist coils between black cattails. The fen listens.",
  "nextNodeId": "fen_choice" }
```

A choice hub — each branch sets a `completionFlag`; **completed choices are eliminated on return**:
```json
{ "id": "fen_choice", "speakerId": "narrator",
  "text": "What do you do?",
  "choices": [
    { "id": "c_scout", "text": "Scout the ruins.", "voiceKeywords": ["scout", "ruins"],
      "nextNodeId": "fen_ruins", "completionFlag": "fen_scouted" },
    { "id": "c_combat", "text": "Test the wyrm.", "voiceKeywords": ["fight", "wyrm", "battle"],
      "nextNodeId": "fen_battle" }
  ] }
```

Battle trigger — runs the encounter, resumes at `nextNodeId` on victory:
```json
{ "id": "fen_battle", "speakerId": "narrator",
  "text": "The wyrm rears from the bog!",
  "triggerBattleEncounterId": "fen_ambush", "nextNodeId": "fen_victory" }
```

Scene change, party rest, flags:
```json
{ "id": "fen_warp", "speakerId": "narrator",
  "text": "You descend beneath the fen.", "changeSceneId": "sunken_bell", "nextNodeId": "bell_intro" }
{ "id": "fen_rest", "speakerId": "narrator",
  "text": "You rest by the last dry fire.", "healPartyOnEnter": "full",
  "setFlagOnEnter": "fen_rested", "nextNodeId": "fen_choice" }
```

Hub auto-redirect when everything is done:
```json
{ "id": "bell_hub", "speakerId": "narrator",
  "hubCompletion": { "requiredFlags": ["bell_rung"], "redirectToNodeId": "bell_conclusion" },
  "choices": [ /* remaining repeatable options */ ] }
```

**The ending contract:** the only terminal node (no `nextNodeId`, no `choices`) is one that sets `game_completed`. Everything else continues onward.
```json
{ "id": "bell_conclusion", "speakerId": "narrator",
  "text": "The bell rings out across the fen.", "setFlagOnEnter": "game_completed" }
```

**Every choice needs `voiceKeywords`** and **every branch needs a completion flag** — the schema tests enforce all of this.

---

## Step 8: Bind Voices & Pitch

- Set `preferredVoiceId` on a character to bind an exact installed TTS voice (enumerated via Android's `TextToSpeech.voices`). If that voice isn't installed, the engine falls back to `ttsPitch`/`ttsRate` modulation.
- `ttsPitch`: 0.5–2.0 per speaker for a quick distinct-character feel without custom voices.
- All dialogue, choices ("Read choices"), and meta-commands are hands-free by default — pocket/screenless play is built in.

---

## Step 9: Add Art & Music

Every asset path referenced in JSON must exist under `app/src/main/assets/`:
- `portraits/<id>.jpg` — character dialogue portraits
- `story/<name>.jpg` — scene backgrounds
- `audio/music/<name>.ogg` — scene/combat music
- `sprites/<id>` — combat pixel sprites (or reuse the provided `template_*`/`practice_dummy` sprites)

The five template placeholder assets (`template_hero.jpg`, `template_narrator.jpg`, `template_scene.jpg`, `template_scene_alt.jpg`, `bgm_exploration.ogg`) are the reference slots used by the sandbox.

---

## Step 10: Build, Verify & Play

The unit suite is content-agnostic — it validates engine invariants against **your** content pack:
```bash
./gradlew testDebugUnitTest      # schema invariants + engine mechanics, must stay green
./gradlew compileDebugKotlin     # engine compile gate
./gradlew assembleDebug          # build your game APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**If a test fails after you swap content, it is telling you your JSON broke an engine contract** (dangling node id, missing `voiceKeywords`, dead-end node, unresolvable encounter, unreachable node, spell name outside the resonance lexicon) — fix the content, not the test.

---

---

## Step 11: Decoupled Engine Systems (Modular Drop-In Assets)

The engine decouples gameplay systems and presentation assets into dedicated JSON files under `app/src/main/assets/game/`:

### 11.1 Battle Environments (`environments/environments.json`)
Defines combat backdrops, theme colors, and visual overlays:
```json
{
  "id": "castle",
  "displayName": "The Training Grounds",
  "icon": "🏰",
  "loreLocation": "Grand Courtyard",
  "subtitle": "Stone battlements, banners in the wind",
  "ambientThemeColor": "#2196F3",
  "backgroundAsset": "environments/castle.jpg",
  "aliases": ["courtyard", "CASTLE", "training_grounds"],
  "overlayType": "CASTLE"
}
```
> **Orientation Awareness:** Every environment supports landscape and portrait. Supply both `environments/<name>.jpg` and `environments/<name>_p.jpg`.

### 11.2 Status Effects & Affinities (`combat/status_catalog.json` & `combat/affinity_matrix.json`)
- `status_catalog.json`: Declares all status effect rules (DOT, CC, Buffs, Debuffs) and their turn durations/tickers.
- `affinity_matrix.json`: Configures the element-to-family multiplier table and narrated affinity lines when hitting vulnerabilities or resistances.

### 11.3 Spell Resonance Lexicon (`spells/thesaurus.json`)
Declares the root vocabulary for each magic school used by the acoustic matcher to award Transcendental and Resonant chant bonuses.

### 11.4 Enemy Sprites & Movesets (`enemies/enemy_sprites.json` & `enemies/enemy_spells.json`)
- `enemy_sprites.json`: Stores 16-bit multi-frame pixel art frames (`idle_upright`, `idle_crouch`, `action_cast`, `damaged`) and hex color palettes for enemies.
- `enemy_spells.json`: Stores full movesets, weights, targeting rules, cooldowns, and visual VFX graphics for enemy AI.

### 11.5 Embedded Character Sprites & Spell Graphics
Characters in `characters/*.json` can embed their own 16-bit sprite animation frames and spell visual graphics (`effectType`, `primaryColor`, `secondaryColor`, `particleCount`, `screenShake`) directly within their character definition.

---

## Content Feature Cheat Sheet

| Feature | Where |
|---|---|
| Game title / entry point / victory text / creation prompt | `manifest.json` |
| Characters, voices, party stats, embedded sprites/spells | `characters/*.json` |
| Creation classes, growth | `classes/*.json` |
| Spells, aliases, chant examples | `spells/spells.json` |
| Spell resonance root lexicon | `spells/thesaurus.json` |
| Enemy templates, stats, families | `enemies/enemies.json` |
| Enemy movesets, AI rules & VFX graphics | `enemies/enemy_spells.json` |
| Enemy 16-bit pixel sprites & palettes | `enemies/enemy_sprites.json` |
| Encounters, boss phases, summons | `encounters/encounters.json` |
| Combat environments (landscape & portrait) | `environments/environments.json` |
| Combat status effects (DOT, CC, buffs) | `combat/status_catalog.json` |
| Damage affinity matrix & narration | `combat/affinity_matrix.json` |
| Story locations + recap metadata | `story/scenes.json` |
| Dialogue, choices, flags, battles, scene changes, rest, ending | `story/nodes.json` |
| Art / music referenced by the JSON | `assets/{portraits,story,audio,environments,sprites}` |