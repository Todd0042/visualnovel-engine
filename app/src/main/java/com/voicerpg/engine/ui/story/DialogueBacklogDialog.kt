package com.voicerpg.engine.ui.story

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voicerpg.engine.model.DialogueLogEntry
import com.voicerpg.engine.ui.theme.LogosGold
import com.voicerpg.engine.ui.theme.RetroBlack
import com.voicerpg.engine.ui.theme.RetroBorder
import com.voicerpg.engine.ui.theme.RetroBorderGold
import com.voicerpg.engine.ui.theme.RetroPanel

/**
 * Fullscreen visual novel dialogue backlog dialog.
 * Allows reading prior dialogue lines from the current playthrough.
 */
@Composable
fun DialogueBacklogDialog(
    isOpen: Boolean,
    history: List<DialogueLogEntry>,
    onClose: () -> Unit
) {
    if (!isOpen) return

    val listState = rememberLazyListState()

    LaunchedEffect(isOpen, history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(14.dp))
                .background(RetroBlack.copy(alpha = 0.96f))
                .border(2.dp, RetroBorderGold, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 DIALOGUE LOG",
                        color = LogosGold,
                        fontSize = 16.sp,
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

                Spacer(modifier = Modifier.height(12.dp))

                // History List
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No dialogue recorded yet.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                val speakerName = if (entry.isNarrator) "NARRATOR" else entry.speakerName.uppercase()
                                Text(
                                    text = speakerName,
                                    color = entry.speakerThemeColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.text,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
