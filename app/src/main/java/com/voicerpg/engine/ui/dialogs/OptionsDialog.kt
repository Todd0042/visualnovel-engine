package com.voicerpg.engine.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.voicerpg.engine.ui.theme.LogosGold
import com.voicerpg.engine.ui.theme.RetroBlack
import com.voicerpg.engine.ui.theme.RetroBorder
import com.voicerpg.engine.ui.theme.RetroBorderGold
import com.voicerpg.engine.ui.theme.RetroPanel

@Composable
fun OptionsDialog(
    isOpen: Boolean,
    isNarrationEnabled: Boolean,
    isAutoPlayEnabled: Boolean,
    speechRate: Float,
    isMusicEnabled: Boolean,
    musicVolume: Float,
    onToggleNarration: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onToggleMusic: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onOpenVoiceSettings: (() -> Unit)? = null,
    onReturnToTitle: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    if (!isOpen) return

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(RetroBlack.copy(alpha = 0.98f))
                .border(2.dp, RetroBorderGold, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙ SETTINGS",
                        color = LogosGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(RetroPanel)
                            .border(1.dp, RetroBorder, CircleShape)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Text-To-Speech Narration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Voice Narration (TTS)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Reads dialogue lines aloud with character voices", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isNarrationEnabled,
                        onCheckedChange = { onToggleNarration() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = LogosGold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Speech Rate Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Narration Speed", color = Color.White, fontSize = 14.sp)
                        Text(String.format("%.2fx", speechRate), color = LogosGold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = onSpeechRateChange,
                        valueRange = 0.75f..1.5f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = LogosGold,
                            activeTrackColor = LogosGold,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Auto-Play Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Play", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically progresses to next dialogue line", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isAutoPlayEnabled,
                        onCheckedChange = { onToggleAutoPlay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = LogosGold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Background Music Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Background Music", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Play soundtrack during story scenes", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isMusicEnabled,
                        onCheckedChange = { onToggleMusic() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = LogosGold
                        )
                    )
                }

                // 5. Music Volume Slider
                if (isMusicEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Music Volume", color = Color.White, fontSize = 14.sp)
                            Text("${(musicVolume * 100).toInt()}%", color = LogosGold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = musicVolume,
                            onValueChange = onMusicVolumeChange,
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = LogosGold,
                                activeTrackColor = LogosGold,
                                inactiveTrackColor = Color.DarkGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                if (onOpenVoiceSettings != null) {
                    Button(
                        onClick = onOpenVoiceSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Android TTS Engine Settings", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (onReturnToTitle != null) {
                    Button(
                        onClick = onReturnToTitle,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF450A0A),
                            contentColor = Color(0xFFFCA5A5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Return to Title Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
