package com.voicerpg.engine.model

/**
 * Save data for the linear visual novel player.
 */
data class GameSaveData(
    val saveVersion: Int = 1,
    val saveTimestamp: Long = System.currentTimeMillis(),
    val totalPlaytimeSeconds: Long = 0L,

    // Story progress markers
    val currentSceneId: String = "",
    val currentNodeId: String = "",
    val visitedNodeIds: List<String> = emptyList(),

    // Rolling transcript backlog (capped to 150 entries)
    val recentDialogueLog: List<DialogueLogEntry> = emptyList(),

    // Playback settings
    val isAutoPlay: Boolean = false,
    val isNarrationEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val musicVolume: Float = 0.55f
)
