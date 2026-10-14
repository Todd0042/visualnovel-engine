package com.voicerpg.engine.model

import androidx.compose.ui.graphics.Color
import com.voicerpg.engine.content.GameContent

enum class SpeakerSide {
    LEFT,
    RIGHT,
    CENTER_NARRATOR
}

/**
 * A visual novel character or narrator speaker.
 * Speaker data is loaded from JSON definitions in assets/game/characters directory.
 */
data class DialogueSpeaker(
    val id: String,
    val name: String,
    val title: String = "",
    val portraitAsset: String? = null,
    val spriteAsset: String? = null,
    val themeColor: Color = Color.White,
    val ttsPitch: Float = 1.0f,
    val samplePhrase: String? = null,
    val gender: String? = null,
    val allowedVoiceRegions: List<String> = emptyList()
) {
    companion object {
        val NARRATOR = DialogueSpeaker(
            id = "narrator",
            name = "Narrator",
            title = "",
            portraitAsset = null,
            spriteAsset = null,
            themeColor = Color(0xFFFFD700),
            ttsPitch = 1.0f
        )
    }

    val isNarrator: Boolean get() = id == NARRATOR.id || id == "narrator"

    fun effectivePortraitAsset(): String? = portraitAsset
    fun effectiveSpriteAsset(): String? = spriteAsset ?: portraitAsset
}

/**
 * A single atomic dialogue line in the linear visual novel story script.
 */
data class DialogueNode(
    val id: String,
    val speaker: DialogueSpeaker,
    val side: SpeakerSide = SpeakerSide.LEFT,
    val text: String,
    val nextNodeId: String? = null,
    val changeSceneId: String? = null,
    val backgroundAsset: String? = null,
    val musicAsset: String? = null,
    val sfxAsset: String? = null
)

/**
 * A scene or act backdrop grouping linear dialogue nodes.
 */
data class StoryScene(
    val id: String,
    val name: String,
    val chapterTitle: String,
    val backgroundAsset: String,
    val initialNodeId: String,
    val ambientDescription: String = "",
    val musicAsset: String = "audio/music/bgm_exploration.ogg",
    val chapterIndex: Int = 0,
    val actNumber: Int = 1,
    val actTitle: String = "Act I"
)

enum class GameScreen {
    TITLE,
    STORY_PLAYBACK
}

data class SaveSummary(
    val slotIndex: Int = 1,
    val chapterTitle: String,
    val sceneName: String,
    val timestamp: Long
)

data class SaveSlotInfo(
    val slotIndex: Int,
    val summary: SaveSummary?
) {
    val isEmpty: Boolean get() = summary == null
}

data class DialogueLogEntry(
    val speakerId: String,
    val speakerName: String,
    val speakerTitle: String = "",
    val text: String,
    val sceneName: String = "",
    val isNarrator: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun speakerThemeColor(): Color {
        val speaker = GameContent.speakerById(speakerId)
        return speaker?.themeColor ?: if (isNarrator) Color(0xFFFFD700) else Color.White
    }

    fun speakerPortraitAsset(): String? {
        return GameContent.speakerById(speakerId)?.portraitAsset
    }
}
