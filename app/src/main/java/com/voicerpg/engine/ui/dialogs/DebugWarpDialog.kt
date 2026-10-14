package com.voicerpg.engine.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.voicerpg.engine.viewmodel.StoryViewModel

@Composable
fun ChapterSelectDialog(
    isOpen: Boolean,
    targets: List<StoryViewModel.ChapterTarget>,
    onSelectChapter: (sceneId: String, introNodeId: String) -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(14.dp))
                .background(RetroBlack.copy(alpha = 0.98f))
                .border(2.dp, RetroBorderGold, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📖 CHAPTER SELECT",
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

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targets) { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectChapter(target.sceneId, target.introNodeId)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = target.label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Serif
                            )

                            Text(
                                text = "Start ➔",
                                color = LogosGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}