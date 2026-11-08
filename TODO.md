# Visual Novel Engine: Roadmap & Future Enhancements

This document tracks upcoming architectural features, UI refinements, and generic extensions for the native Android Visual Novel Engine.

---

## High Priority / Architectural Roadmap

### 1. 📖 Dialogue Display & Playback Enhancements
- **Typewriter Effect & Text Speed:** Configurable text typing animation speed (Instant, Fast, Normal, Slow) in the Options menu.
- **Customizable Auto-Play Delay:** Expose auto-advance delay (currently default 1200ms after TTS finish) via a slider in Settings (500ms – 3000ms).
- **Text Formatting & Highlighting:** Support rich-text styling (custom colored terms, italics, bold emphasis) within dialogue text boxes without breaking TTS normalization.

### 2. 🎨 Visuals & Presentation
- **Scene CG Gallery:** Unlockable gallery screen on the title menu for viewing discovered full-screen illustrations and background CGs.
- **Dynamic Camera & Pan/Zoom:** Mild ken-burns slow-pan and zoom effects on scene backdrops during dramatic narration.
- **Bust Portrait Transitions:** Smooth cross-fade and slide-in animations when characters enter, leave, or change emotional expressions.

### 3. 🔊 Audio & Voice Refinements
- **Independent Audio Sliders:** Tri-slider audio mixer (Master, BGM, Voice Narration, Sound Effects) in Options dialog.
- **Per-Speaker Voice Customization:** In-game voice picker allowing users to test and remap individual character TTS voices on the device.

---

## Architectural Principles
- **100% Content Neutral:** No game-specific characters, scripts, or assets hardcoded in engine classes.
- **Zero Combat Remnants:** Dedicated exclusively to visual novel storytelling, branching/linear narrative scripts, voice narration, and multimedia presentation.
- **Robust Offline Support:** Full functionality without network connectivity, utilizing on-device TTS and local JSON content.
