package com.voicerpg.engine.ui.title

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicerpg.engine.content.GameManifest
import com.voicerpg.engine.model.SaveSlotInfo
import com.voicerpg.engine.model.SaveSummary
import com.voicerpg.engine.ui.story.StoryAssetLoader
import com.voicerpg.engine.ui.theme.LogosGold
import com.voicerpg.engine.ui.theme.RetroBlack
import com.voicerpg.engine.ui.theme.RetroBorder
import com.voicerpg.engine.ui.theme.RetroBorderGold

@Composable
fun TitleScreen(
    manifest: GameManifest,
    hasSave: Boolean,
    saveSummary: SaveSummary?,
    saveSlots: List<SaveSlotInfo> = emptyList(),
    currentSlot: Int = 1,
    onStartStory: () -> Unit,
    onContinueStory: () -> Unit,
    onAutoPlay: () -> Unit,
    onSelectChapter: () -> Unit,
    onOptions: () -> Unit,
    onSelectSlot: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgAsset = manifest.titleBackgroundAsset ?: "backgrounds/plate_forest.png"
    val backgroundBitmap = remember(bgAsset) {
        StoryAssetLoader.loadBitmap(context, bgAsset)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background plate
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark gradient vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Title and Interactive Menu
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = manifest.gameTitle.uppercase(),
                    color = LogosGold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                if (manifest.gameSubtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = manifest.gameSubtitle,
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Menu Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save info badge if existing save
                if (hasSave && saveSummary != null) {
                    Surface(
                        color = RetroBlack.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RetroBorderGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CURRENT PROGRESS",
                                color = LogosGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${saveSummary.chapterTitle} • ${saveSummary.sceneName}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }

                    // Continue button
                    Button(
                        onClick = onContinueStory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LogosGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("CONTINUE STORY", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Start / Restart button
                Button(
                    onClick = onStartStory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasSave) Color(0xFF1E293B) else LogosGold,
                        contentColor = if (hasSave) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(if (hasSave) "START NEW STORY" else "BEGIN STORY", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Auto Play button
                Button(
                    onClick = onAutoPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("AUTO-PLAY STORY", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // Chapter Select button
                Button(
                    onClick = onSelectChapter,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("CHAPTER SELECT", fontSize = 14.sp)
                }

                // Settings button
                Button(
                    onClick = onOptions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("SETTINGS", fontSize = 14.sp)
                }

                // Save slot selector pills
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..3).forEach { slot ->
                        val isSelected = slot == currentSlot
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isSelected) LogosGold else RetroBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .background(
                                    if (isSelected) LogosGold.copy(alpha = 0.2f) else RetroBlack,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Slot $slot",
                                color = if (isSelected) LogosGold else Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
