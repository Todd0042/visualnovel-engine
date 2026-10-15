package com.voicerpg.engine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicerpg.engine.audio.StoryNarrator
import com.voicerpg.engine.audio.MusicManager
import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.engine.SaveManager
import com.voicerpg.engine.model.DialogueLogEntry
import com.voicerpg.engine.model.DialogueNode
import com.voicerpg.engine.model.GameSaveData
import com.voicerpg.engine.model.GameScreen
import com.voicerpg.engine.model.SaveSlotInfo
import com.voicerpg.engine.model.SaveSummary
import com.voicerpg.engine.model.StoryScene
import com.voicerpg.engine.ui.story.StoryAssetLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoryState(
    val currentScene: StoryScene = GameContent.initialScene,
    val currentNode: DialogueNode = GameContent.initialNode,
    val gameScreen: GameScreen = GameScreen.TITLE,
    val isAutoPlay: Boolean = false,
    val isPlaying: Boolean = false,
    val hasExistingSave: Boolean = false,
    val saveSummary: SaveSummary? = null,
    val currentSlot: Int = 1,
    val saveSlots: List<SaveSlotInfo> = emptyList(),
    val dialogueHistory: List<DialogueLogEntry> = emptyList(),
    val isBacklogOpen: Boolean = false,
    val isOptionsOpen: Boolean = false,
    val isChapterSelectOpen: Boolean = false
)

class StoryViewModel(
    val storyNarrator: StoryNarrator,
    val saveManager: SaveManager = SaveManager(),
    val musicManager: MusicManager? = null,
    private val scopeOverride: CoroutineScope? = null
) : ViewModel() {

    data class ChapterTarget(
        val label: String,
        val sceneId: String,
        val introNodeId: String,
        val chapterIndex: Int
    )

    companion object {
        val CHAPTER_TARGETS: List<ChapterTarget>
            get() = GameContent.scenes.values
                .distinctBy { it.chapterIndex }
                .sortedBy { it.chapterIndex }
                .map { ChapterTarget(it.chapterTitle, it.id, it.initialNodeId, it.chapterIndex) }
    }

    private val activeScope: CoroutineScope
        get() = scopeOverride ?: viewModelScope

    private val _state = MutableStateFlow(StoryState())
    val state: StateFlow<StoryState> = _state.asStateFlow()

    private var autoAdvanceJob: Job? = null
    private val nodeHistory = mutableListOf<Pair<String, String>>() // (sceneId, nodeId)

    init {
        refreshSaveStatus()
    }

    fun refreshSaveStatus() {
        val hasSave = saveManager.hasSave(_state.value.currentSlot)
        val summary = saveManager.getSlotSummary(_state.value.currentSlot)
        val allSlots = saveManager.getAllSlotInfos()
        _state.value = _state.value.copy(
            hasExistingSave = hasSave,
            saveSummary = summary,
            saveSlots = allSlots
        )
    }

    fun selectSlot(slot: Int) {
        saveManager.currentSlot = slot
        val hasSave = saveManager.hasSave(slot)
        val summary = saveManager.getSlotSummary(slot)
        _state.value = _state.value.copy(
            currentSlot = slot,
            hasExistingSave = hasSave,
            saveSummary = summary
        )
    }

    fun deleteSlot(slot: Int) {
        saveManager.deleteSave(slot)
        refreshSaveStatus()
    }

    fun startNewStory(slot: Int = _state.value.currentSlot, startInAutoPlay: Boolean = false) {
        val initialScene = GameContent.initialScene
        val initialNode = GameContent.initialNode
        nodeHistory.clear()

        _state.value = _state.value.copy(
            currentScene = initialScene,
            currentNode = initialNode,
            gameScreen = GameScreen.STORY_PLAYBACK,
            isAutoPlay = startInAutoPlay,
            isPlaying = startInAutoPlay,
            currentSlot = slot,
            dialogueHistory = emptyList()
        )
        musicManager?.playTrack(initialScene.musicAsset)
        applyNode(initialNode, initialScene, isForward = true)
    }

    fun continueStory(slot: Int = _state.value.currentSlot, startInAutoPlay: Boolean = false) {
        val saved = saveManager.load(slot)
        if (saved == null) {
            startNewStory(slot, startInAutoPlay)
            return
        }

        val scene = GameContent.scenes[saved.currentSceneId] ?: GameContent.initialScene
        val node = GameContent.nodes[saved.currentNodeId] ?: GameContent.initialNode
        nodeHistory.clear()

        _state.value = _state.value.copy(
            currentScene = scene,
            currentNode = node,
            gameScreen = GameScreen.STORY_PLAYBACK,
            isAutoPlay = startInAutoPlay || saved.isAutoPlay,
            isPlaying = startInAutoPlay,
            currentSlot = slot,
            dialogueHistory = saved.recentDialogueLog
        )
        musicManager?.playTrack(scene.musicAsset)
        applyNode(node, scene, isForward = true)
    }

    fun togglePlayPause() {
        val nextPlaying = !_state.value.isPlaying
        _state.value = _state.value.copy(isPlaying = nextPlaying, isAutoPlay = nextPlaying)
        if (nextPlaying) {
            // If narrator is currently not speaking, advance to start playback
            if (!storyNarrator.isSpeaking.value) {
                advanceDialogue()
            }
        } else {
            cancelAutoAdvance()
            storyNarrator.stop()
        }
    }

    fun toggleAutoPlay() {
        val next = !_state.value.isAutoPlay
        _state.value = _state.value.copy(isAutoPlay = next, isPlaying = next)
        if (next && !storyNarrator.isSpeaking.value) {
            advanceDialogue()
        } else if (!next) {
            cancelAutoAdvance()
        }
    }

    fun advanceDialogue() {
        cancelAutoAdvance()
        storyNarrator.stop()

        val currentNode = _state.value.currentNode
        val nextNodeId = currentNode.nextNodeId

        if (nextNodeId.isNullOrBlank()) {
            // Check if scene transition
            if (!currentNode.changeSceneId.isNullOrBlank()) {
                val nextScene = GameContent.scenes[currentNode.changeSceneId]
                if (nextScene != null) {
                    val entryNode = GameContent.nodes[nextScene.initialNodeId]
                    if (entryNode != null) {
                        applyNode(entryNode, nextScene, isForward = true)
                        return
                    }
                }
            }
            // End of story or chapter
            _state.value = _state.value.copy(isPlaying = false, isAutoPlay = false)
            return
        }

        val nextNode = GameContent.nodes[nextNodeId]
        if (nextNode != null) {
            val nextScene = if (!nextNode.changeSceneId.isNullOrBlank()) {
                GameContent.scenes[nextNode.changeSceneId] ?: _state.value.currentScene
            } else {
                _state.value.currentScene
            }
            applyNode(nextNode, nextScene, isForward = true)
        } else {
            _state.value = _state.value.copy(isPlaying = false, isAutoPlay = false)
        }
    }

    fun previousDialogue() {
        cancelAutoAdvance()
        storyNarrator.stop()

        if (nodeHistory.isEmpty()) return
        val (prevSceneId, prevNodeId) = nodeHistory.removeAt(nodeHistory.lastIndex)
        val prevScene = GameContent.scenes[prevSceneId] ?: _state.value.currentScene
        val prevNode = GameContent.nodes[prevNodeId] ?: return

        applyNode(prevNode, prevScene, isForward = false)
    }

    fun jumpToChapter(sceneId: String, introNodeId: String) {
        cancelAutoAdvance()
        storyNarrator.stop()

        val scene = GameContent.scenes[sceneId] ?: _state.value.currentScene
        val node = GameContent.nodes[introNodeId] ?: GameContent.initialNode

        closeChapterSelect()
        closeOptions()

        _state.value = _state.value.copy(
            currentScene = scene,
            currentNode = node,
            gameScreen = GameScreen.STORY_PLAYBACK
        )
        musicManager?.playTrack(scene.musicAsset)
        applyNode(node, scene, isForward = true)
    }

    private fun applyNode(node: DialogueNode, scene: StoryScene, isForward: Boolean) {
        if (isForward && _state.value.currentNode.id != node.id) {
            nodeHistory.add(_state.value.currentScene.id to _state.value.currentNode.id)
            if (nodeHistory.size > 100) {
                nodeHistory.removeAt(0)
            }
        }

        // Scene transition check
        if (scene.id != _state.value.currentScene.id) {
            StoryAssetLoader.onSceneTransition()
            musicManager?.playTrack(scene.musicAsset)
        } else if (!node.musicAsset.isNullOrBlank()) {
            musicManager?.playTrack(node.musicAsset)
        }

        // Add to dialogue transcript backlog (max 150)
        val newEntry = DialogueLogEntry(
            speakerId = node.speaker.id,
            speakerName = node.speaker.name,
            speakerTitle = node.speaker.title,
            text = node.text,
            sceneName = scene.name,
            isNarrator = node.speaker.isNarrator
        )
        val updatedLog = (_state.value.dialogueHistory + newEntry).takeLast(150)

        _state.value = _state.value.copy(
            currentScene = scene,
            currentNode = node,
            dialogueHistory = updatedLog
        )

        persistCurrentState()

        // Narrate line via TTS
        storyNarrator.narrateDialogue(node.speaker, node.text) {
            onLineNarrated(node)
        }
    }

    private fun onLineNarrated(node: DialogueNode) {
        if (_state.value.currentNode.id == node.id && _state.value.isAutoPlay && _state.value.isPlaying) {
            scheduleAutoAdvance(delayMs = 1200L)
        }
    }

    private fun scheduleAutoAdvance(delayMs: Long) {
        cancelAutoAdvance()
        autoAdvanceJob = activeScope.launch {
            delay(delayMs)
            if (_state.value.isAutoPlay && _state.value.isPlaying && _state.value.gameScreen == GameScreen.STORY_PLAYBACK) {
                advanceDialogue()
            }
        }
    }

    private fun cancelAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
    }

    fun openOptions() {
        cancelAutoAdvance()
        _state.value = _state.value.copy(isOptionsOpen = true)
    }

    fun closeOptions() {
        _state.value = _state.value.copy(isOptionsOpen = false)
        if (_state.value.isAutoPlay && _state.value.isPlaying && !storyNarrator.isSpeaking.value) {
            scheduleAutoAdvance(delayMs = 800L)
        }
    }

    fun openBacklog() {
        cancelAutoAdvance()
        _state.value = _state.value.copy(isBacklogOpen = true)
    }

    fun closeBacklog() {
        _state.value = _state.value.copy(isBacklogOpen = false)
        if (_state.value.isAutoPlay && _state.value.isPlaying && !storyNarrator.isSpeaking.value) {
            scheduleAutoAdvance(delayMs = 800L)
        }
    }

    fun openChapterSelect() {
        _state.value = _state.value.copy(isChapterSelectOpen = true)
    }

    fun closeChapterSelect() {
        _state.value = _state.value.copy(isChapterSelectOpen = false)
    }

    fun returnToTitle() {
        cancelAutoAdvance()
        storyNarrator.stop()
        persistCurrentState()
        refreshSaveStatus()
        _state.value = _state.value.copy(
            gameScreen = GameScreen.TITLE,
            isPlaying = false,
            isOptionsOpen = false,
            isBacklogOpen = false,
            isChapterSelectOpen = false
        )
        musicManager?.playTrack(MusicManager.TRACK_TITLE)
    }

    fun persistCurrentState() {
        val s = _state.value
        val save = GameSaveData(
            currentSceneId = s.currentScene.id,
            currentNodeId = s.currentNode.id,
            recentDialogueLog = s.dialogueHistory,
            isAutoPlay = s.isAutoPlay,
            isNarrationEnabled = storyNarrator.isNarrationEnabled.value,
            speechRate = storyNarrator.speechRate.value,
            musicVolume = musicManager?.musicVolume?.value ?: 0.55f
        )
        saveManager.save(save, s.currentSlot)
    }

    override fun onCleared() {
        super.onCleared()
        cancelAutoAdvance()
        storyNarrator.stop()
    }
}
