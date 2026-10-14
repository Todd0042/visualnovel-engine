package com.voicerpg.engine

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.voicerpg.engine.audio.CombatNarrator
import com.voicerpg.engine.audio.MusicManager
import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.engine.SaveManager
import com.voicerpg.engine.model.GameScreen
import com.voicerpg.engine.ui.dialogs.ChapterSelectDialog
import com.voicerpg.engine.ui.dialogs.OptionsDialog
import com.voicerpg.engine.ui.story.DialogueBacklogDialog
import com.voicerpg.engine.ui.story.StoryScreen
import com.voicerpg.engine.ui.theme.VoiceRPGTheme
import com.voicerpg.engine.ui.title.TitleScreen
import com.voicerpg.engine.viewmodel.StoryViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var combatNarrator: CombatNarrator
    private lateinit var musicManager: MusicManager
    private lateinit var saveManager: SaveManager
    private lateinit var storyViewModel: StoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize drop-in visual novel game content pack
        GameContent.initialize(applicationContext)

        combatNarrator = CombatNarrator(this)
        combatNarrator.registerSpeakers(GameContent.allSpeakers)
        musicManager = MusicManager(this)
        saveManager = SaveManager(this)

        storyViewModel = StoryViewModel(
            combatNarrator = combatNarrator,
            saveManager = saveManager,
            musicManager = musicManager
        )

        // Automatic speech ducking: BGM ducks during voice narration
        lifecycleScope.launch {
            combatNarrator.isSpeaking.collect { speaking ->
                if (speaking) {
                    musicManager.duckForSpeech()
                } else {
                    musicManager.restoreFromSpeech()
                }
            }
        }

        setContent {
            val storyState by storyViewModel.state.collectAsState()
            val isNarrationEnabled by combatNarrator.isNarrationEnabled.collectAsState()
            val speechRate by combatNarrator.speechRate.collectAsState()
            val isMusicEnabled by musicManager.isMusicEnabled.collectAsState()
            val musicVolume by musicManager.musicVolume.collectAsState()

            // Keep screen on during story playback
            DisposableEffect(storyState.gameScreen) {
                if (storyState.gameScreen == GameScreen.STORY_PLAYBACK) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Track music playback according to current scene
            LaunchedEffect(storyState.currentScene.id, storyState.gameScreen) {
                if (storyState.gameScreen == GameScreen.STORY_PLAYBACK) {
                    musicManager.playTrack(storyState.currentScene.musicAsset)
                } else {
                    musicManager.playTrack(MusicManager.TRACK_TITLE)
                }
            }

            VoiceRPGTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (storyState.gameScreen) {
                        GameScreen.TITLE -> {
                            TitleScreen(
                                manifest = GameContent.manifest,
                                hasSave = storyState.hasExistingSave,
                                saveSummary = storyState.saveSummary,
                                saveSlots = storyState.saveSlots,
                                currentSlot = storyState.currentSlot,
                                onStartStory = { storyViewModel.startNewStory(storyState.currentSlot) },
                                onContinueStory = { storyViewModel.continueStory(storyState.currentSlot) },
                                onAutoPlay = { storyViewModel.startNewStory(storyState.currentSlot, startInAutoPlay = true) },
                                onSelectChapter = { storyViewModel.openChapterSelect() },
                                onOptions = { storyViewModel.openOptions() },
                                onSelectSlot = { slot -> storyViewModel.selectSlot(slot) }
                            )
                        }

                        GameScreen.STORY_PLAYBACK -> {
                            StoryScreen(
                                storyViewModel = storyViewModel,
                                onOpenOptions = { storyViewModel.openOptions() }
                            )
                        }
                    }

                    // Options Dialog
                    OptionsDialog(
                        isOpen = storyState.isOptionsOpen,
                        isNarrationEnabled = isNarrationEnabled,
                        isAutoPlayEnabled = storyState.isAutoPlay,
                        speechRate = speechRate,
                        isMusicEnabled = isMusicEnabled,
                        musicVolume = musicVolume,
                        onToggleNarration = {
                            combatNarrator.toggleNarration()
                            storyViewModel.persistCurrentState()
                        },
                        onToggleAutoPlay = {
                            storyViewModel.toggleAutoPlay()
                        },
                        onSpeechRateChange = { rate ->
                            combatNarrator.setSpeechRate(rate)
                            storyViewModel.persistCurrentState()
                        },
                        onToggleMusic = {
                            musicManager.toggleMusic()
                            storyViewModel.persistCurrentState()
                        },
                        onMusicVolumeChange = { vol ->
                            musicManager.setVolume(vol)
                            storyViewModel.persistCurrentState()
                        },
                        onOpenVoiceSettings = {
                            combatNarrator.openVoiceSettings(this@MainActivity)
                        },
                        onReturnToTitle = {
                            storyViewModel.closeOptions()
                            storyViewModel.returnToTitle()
                        },
                        onClose = { storyViewModel.closeOptions() }
                    )

                    // Chapter Select Dialog
                    ChapterSelectDialog(
                        isOpen = storyState.isChapterSelectOpen,
                        targets = StoryViewModel.CHAPTER_TARGETS,
                        onSelectChapter = { sceneId, introNodeId ->
                            storyViewModel.jumpToChapter(sceneId, introNodeId)
                        },
                        onClose = { storyViewModel.closeChapterSelect() }
                    )

                    // Dialogue Backlog Dialog
                    DialogueBacklogDialog(
                        isOpen = storyState.isBacklogOpen,
                        history = storyState.dialogueHistory,
                        onClose = { storyViewModel.closeBacklog() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        storyViewModel.persistCurrentState()
        combatNarrator.destroy()
        musicManager.destroy()
    }
}
