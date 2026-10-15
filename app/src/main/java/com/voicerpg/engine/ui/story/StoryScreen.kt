package com.voicerpg.engine.ui.story

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.voicerpg.engine.model.DialogueSpeaker
import com.voicerpg.engine.model.SpeakerSide
import com.voicerpg.engine.ui.theme.LogosGold
import com.voicerpg.engine.ui.theme.LogosGlow
import com.voicerpg.engine.ui.theme.RetroBlack
import com.voicerpg.engine.ui.theme.RetroBorder
import com.voicerpg.engine.ui.theme.RetroBorderGold
import com.voicerpg.engine.ui.theme.RetroPanel
import com.voicerpg.engine.viewmodel.StoryViewModel

/**
 * Pure Visual Novel playback viewport.
 * Features:
 * - Orientation-aware background plate with LRU texture caching
 * - Full-sized character sprite staging with breathing animation
 * - Lower-third dialogue box with speaker nameplate
 * - Pure touch/button playback controls (Play, Pause, Next, Prev, Auto, Backlog, Options)
 */
@Composable
fun StoryScreen(
    storyViewModel: StoryViewModel,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val storyState by storyViewModel.state.collectAsState()
    val isSpeaking by storyViewModel.storyNarrator.isSpeaking.collectAsState()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentScene = storyState.currentScene
    val currentNode = storyState.currentNode
    val speaker = currentNode.speaker

    val backgroundAssetPath = currentNode.backgroundAsset ?: currentScene.backgroundAsset
    val backgroundBitmap = remember(backgroundAssetPath, isLandscape) {
        val dotIdx = backgroundAssetPath.lastIndexOf('.')
        val portraitPath = if (dotIdx != -1) {
            backgroundAssetPath.substring(0, dotIdx) + "_p" + backgroundAssetPath.substring(dotIdx)
        } else {
            backgroundAssetPath
        }

        if (!isLandscape) {
            StoryAssetLoader.loadBitmap(context, portraitPath)
                ?: StoryAssetLoader.loadBitmap(context, backgroundAssetPath)
        } else {
            StoryAssetLoader.loadBitmap(context, backgroundAssetPath)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                // Tapping anywhere advances dialogue if not in options/backlog
                storyViewModel.advanceDialogue()
            }
    ) {
        // 1. Full-screen background plate
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap,
                contentDescription = currentScene.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF020617))
                        )
                    )
            )
        }

        // 2. Subtle ambient vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 3. Visual Novel Full-Sized Character Sprite Staging
        if (!speaker.isNarrator) {
            val spritePath = speaker.effectiveSpriteAsset()
            val spriteBitmap = remember(spritePath) {
                if (spritePath != null) StoryAssetLoader.loadBitmap(context, spritePath) else null
            }

            if (spriteBitmap != null) {
                val infiniteTransition = rememberInfiniteTransition(label = "SpriteBreathing")
                val breatheOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "BreatheY"
                )
                val breatheScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.015f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "BreatheScale"
                )

                val spriteAlignment = when (currentNode.side) {
                    SpeakerSide.LEFT -> Alignment.BottomStart
                    SpeakerSide.RIGHT -> Alignment.BottomEnd
                    SpeakerSide.CENTER_NARRATOR -> Alignment.BottomCenter
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = if (isLandscape) 140.dp else 220.dp,
                            start = if (currentNode.side == SpeakerSide.LEFT) 32.dp else 0.dp,
                            end = if (currentNode.side == SpeakerSide.RIGHT) 32.dp else 0.dp
                        ),
                    contentAlignment = spriteAlignment
                ) {
                    Image(
                        bitmap = spriteBitmap,
                        contentDescription = speaker.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight(if (isLandscape) 0.85f else 0.65f)
                            .offset(y = breatheOffset.dp)
                            .scale(breatheScale)
                            .shadow(24.dp, RoundedCornerShape(12.dp))
                    )
                }
            }
        }

        // 4. Top Header Bar (Chapter & Scene Info + Quick Buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter and Scene indicator
            Surface(
                color = RetroBlack.copy(alpha = 0.80f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetroBorderGold.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "${currentScene.chapterTitle} • ${currentScene.name}",
                    color = LogosGold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Chapters
                IconButton(
                    onClick = { storyViewModel.openChapterSelect() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(RetroBlack.copy(alpha = 0.8f), CircleShape)
                        .border(1.dp, RetroBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Chapters",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = { onOpenOptions() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(RetroBlack.copy(alpha = 0.8f), CircleShape)
                        .border(1.dp, RetroBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 5. Lower-Third Dialogue Box & Control Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .zIndex(10f)
                .clickable { storyViewModel.advanceDialogue() }
        ) {
            // Dialogue Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 32.dp else 12.dp)
                    .background(
                        color = RetroPanel.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSpeaking) LogosGold else RetroBorder,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    // Speaker Nameplate
                    val speakerName = if (speaker.isNarrator) "NARRATOR" else speaker.name.uppercase()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = speaker.themeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, speaker.themeColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = speakerName,
                                color = speaker.themeColor,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (!speaker.title.isNullOrBlank()) {
                            Text(
                                text = speaker.title,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialogue Text
                    Text(
                        text = currentNode.text,
                        color = Color.White,
                        fontSize = if (isLandscape) 17.sp else 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                    )
                }
            }

            // 6. Visual Novel Playback Control Dock
            Surface(
                color = RetroBlack.copy(alpha = 0.98f),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetroBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left buttons: Back / Prev, Next
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { storyViewModel.previousDialogue() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Prev", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { storyViewModel.advanceDialogue() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Next", fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Center buttons: Play/Pause, Auto
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { storyViewModel.togglePlayPause() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (storyState.isPlaying) LogosGold else Color(0xFF334155),
                                contentColor = if (storyState.isPlaying) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = if (storyState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (storyState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (storyState.isPlaying) "Pause" else "Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { storyViewModel.toggleAutoPlay() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (storyState.isAutoPlay) LogosGold.copy(alpha = 0.85f) else Color(0xFF1E293B),
                                contentColor = if (storyState.isAutoPlay) Color.Black else Color.LightGray
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Auto", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auto", fontSize = 12.sp)
                        }
                    }

                    // Right button: Backlog Transcript
                    Button(
                        onClick = { storyViewModel.openBacklog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Log", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
