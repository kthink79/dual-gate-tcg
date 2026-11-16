package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 메인 로비 「📊 정보」 패널 (모달 시트).
 *
 * 로비 상단에 4줄로 겹쳐 캐릭터를 가리던 정보를 이 패널 하나로 모았다:
 * 진행도(스토리·스테이지·덱 전투력·보유 인원·조각) · 정신 상태(홀림/맑음) ·
 * 설정(BGM·SFX·자동 저장) · 로비 경보 · 「🎬 인트로 다시보기」 · 「🆕 새 게임」.
 *
 * 패널 밖(어두운 스크림)을 누르면 닫힌다.
 */
@Composable
fun LobbyInfoOverlay(
    state: TcgGameState,
    onClose: () -> Unit,
    onOpenDestination: (LobbyDestination) -> Unit,
    onReplayIntro: () -> Unit,
    onToggleBgm: () -> Unit,
    onToggleSfx: () -> Unit,
    onResetGame: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC05040A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(TcgColors.Surface)
                .border(1.dp, TcgColors.Line, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                // 패널 안쪽 터치가 스크림 닫기로 새지 않게 소비한다
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── 헤더 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    LobbyInfoPanel.TITLE,
                    color = TcgColors.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(TcgColors.SurfaceAlt)
                        .border(1.dp, TcgColors.Line, RoundedCornerShape(50))
                        .clickable { onClose() }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("닫기 ✕", color = TcgColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── 진행도 · 정신 상태 · 설정 ──
            LobbyInfoPanel.sections(state).forEach { section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF100D18))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        section.title,
                        color = TcgColors.Violet,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    section.entries.forEach { entry ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(entry.icon, fontSize = 12.sp)
                            Spacer(Modifier.width(7.dp))
                            Text(
                                entry.label,
                                color = TcgColors.Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.width(78.dp)
                            )
                            Text(
                                entry.value,
                                color = entry.tone.color(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // 설정 묶음에는 실제 토글을 붙인다
                    if (section.title == "설정") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LobbyToggleChip(
                                label = if (state.audioSettings.bgmEnabled) "🔊 BGM 켜짐" else "🔇 BGM 꺼짐",
                                active = state.audioSettings.bgmEnabled,
                                onClick = onToggleBgm
                            )
                            LobbyToggleChip(
                                label = if (state.audioSettings.sfxEnabled) "🔊 SFX 켜짐" else "🔇 SFX 꺼짐",
                                active = state.audioSettings.sfxEnabled,
                                onClick = onToggleSfx
                            )
                        }
                    }
                }
            }

            // ── 로비 경보 ──
            val alerts = LobbyInfoPanel.alerts(state)
            if (alerts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF100D18))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("경보", color = TcgColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    alerts.forEach { alert ->
                        val color = if (alert.isUrgent) TcgColors.Danger else TcgColors.Gold
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(color.copy(alpha = 0.12f))
                                .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable { onOpenDestination(alert.destination) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠", fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                alert.label,
                                color = color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${alert.destination.icon} ${alert.destination.label} ›", color = TcgColors.Muted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── 동작: 인트로 다시보기 · 새 게임 ──
            ActionTile(
                label = "🎬 인트로 다시보기",
                hint = "프롤로그 4장면을 처음부터 — 장면마다 터치로 넘깁니다",
                accent = TcgColors.Violet,
                onClick = onReplayIntro
            )
            ActionTile(
                label = "🆕 새 게임",
                hint = "보유 카드 ${state.ownedCards.size}명 · 스테이지 ${state.stageCleared.size}/30 · ${state.gold}G 초기화",
                accent = TcgColors.Danger,
                onClick = { showResetDialog = true }
            )
            Text(
                "💾 진행 자동 저장 — 상태가 바뀔 때마다 저장되어 앱을 닫아도 이어집니다.",
                color = TcgColors.Muted,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("새 게임으로 시작할까요?", color = TcgColors.Text) },
            text = {
                Text(
                    "보유 카드 ${state.ownedCards.size}명 · 스테이지 ${state.stageCleared.size}/30 · " +
                        "${state.gold}G 가 모두 사라지고 처음 상태로 돌아갑니다.\n" +
                        "이 작업은 되돌릴 수 없습니다.",
                    color = TcgColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetGame()
                }) { Text("진행 초기화", color = TcgColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("취소", color = TcgColors.Violet)
                }
            }
        )
    }
}

/** 정보 패널 · 로비 상단의 작은 설정 칩 */
@Composable
fun LobbyToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) TcgColors.Violet.copy(alpha = 0.18f) else TcgColors.SurfaceAlt)
            .border(
                1.dp,
                if (active) TcgColors.Violet.copy(alpha = 0.6f) else TcgColors.Line,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            color = if (active) TcgColors.Violet else TcgColors.Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** [InfoTone] → 실제 색 */
fun InfoTone.color(): Color = when (this) {
    InfoTone.NEUTRAL -> TcgColors.Muted
    InfoTone.GOLD -> TcgColors.Gold
    InfoTone.VIOLET -> TcgColors.Violet
    InfoTone.HEAL -> TcgColors.Heal
    InfoTone.DANGER -> TcgColors.Danger
    InfoTone.MANA -> TcgColors.Mana
    InfoTone.ABYSS -> TcgColors.Abyss
}
