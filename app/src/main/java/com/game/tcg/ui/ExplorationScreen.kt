package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.model.TcgCard

/**
 * 좌측 서브메뉴 「탐색」 — 차원 틈새 탐색 · 기억 파편 수색 · 랜덤 조우
 * 결과는 이계 전투(전투 조우) · 기억의 잔향(기억 파편) · 유혹 페널티([홀림])와 직접 연결된다.
 */
@Composable
fun ExplorationScreen(
    state: TcgGameState,
    onExplore: (ExplorationMode) -> Unit,
    onOpenBattle: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenRealWorld: () -> Unit
) {
    ScreenFrame {
        // ── 차원 균열 에너지 ──
        SectionCard(
            title = "차원 균열 에너지",
            badge = "${state.gateEnergy}/${state.gateEnergyMax}",
            accent = TcgColors.Mana
        ) {
            StatBar(ratio = state.gateEnergy.toFloat() / state.gateEnergyMax, color = TcgColors.Mana, height = 10)
            Text(
                "탐색 1회당 에너지 ${ExplorationMode.RIFT.energyCost} 소모 · 이계 전투 승리 시 +${TcgGameViewModel.GATE_ENERGY_RECOVERY} 회복 " +
                        "(최대 ${state.gateEnergyMax})",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }

        // ── 탐색 3모드 ──
        SectionCard(title = "탐색 모드", badge = "누적 ${state.explorationCount}회", accent = TcgColors.Violet) {
            ExplorationMode.values().forEach { mode ->
                ActionTile(
                    label = "${mode.icon} ${mode.displayName}",
                    hint = "${mode.description} · 에너지 -${mode.energyCost}",
                    accent = when (mode) {
                        ExplorationMode.RIFT -> TcgColors.Violet
                        ExplorationMode.FRAGMENT -> TcgColors.Abyss
                        ExplorationMode.ENCOUNTER -> TcgColors.Gold
                    },
                    enabled = state.gateEnergy >= mode.energyCost,
                    onClick = { onExplore(mode) }
                )
            }
            if (state.gateEnergy < ExplorationMode.RIFT.energyCost) {
                Text(
                    "차원 균열 에너지가 모두 소진되었습니다. 이계 전투에서 승리하면 회복됩니다.",
                    color = TcgColors.Danger,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // ── 마지막 탐색 결과 ──
        val result = state.lastExploration
        if (result != null) {
            SectionCard(
                title = result.title,
                badge = result.mode.displayName,
                accent = if (result.startsBattle) TcgColors.Danger else TcgColors.Heal
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TcgColors.SurfaceAlt)
                        .padding(12.dp)
                ) {
                    Text(
                        result.narrative,
                        color = TcgColors.Text.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("획득", TcgColors.Heal)
                    Spacer(Modifier.width(6.dp))
                    Text(result.rewardSummary, color = TcgColors.Text, fontSize = 10.sp)
                }
                if (result.startsBattle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill("조우", TcgColors.Danger, filled = true)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${result.enemyName} · HP ${result.enemyMaxHp} · 공격력 ${result.enemyAtk} " +
                                    "(${result.enemyElement.displayName})",
                            color = TcgColors.Danger,
                            fontSize = 10.sp
                        )
                    }
                    ActionTile(
                        label = "⚔ 이계 전투에서 교전",
                        hint = "조우한 상대가 전장에 세팅되어 있습니다",
                        accent = TcgColors.Danger,
                        onClick = onOpenBattle
                    )
                }
            }
        }

        // ── 기억 파편 현황 ──
        SectionCard(title = "기억의 잔향 · 파편 수색", badge = "기억 파편", accent = TcgColors.Abyss) {
            val targets = state.memoryResetCards
            if (targets.isEmpty()) {
                Text(
                    "지금은 기억이 지워진 동료가 없어 파편이 맺히지 않습니다. " +
                            "이계 전투에서 동료가 쓰러졌다가 환생 재획득하면 '기억이 지워진 동료'가 되어 " +
                            "파편 수색이 의미를 갖습니다.",
                    color = TcgColors.Muted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            } else {
                targets.forEach { card ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(card.name.take(10), TcgColors.Abyss, filled = true)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "기억 파편 ${card.memoryFragments}/${TcgCard.MEMORY_FRAGMENT_TARGET} · 호감도 ${card.bondLevel}/100",
                            color = TcgColors.Text,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    StatBar(
                        ratio = card.memoryFragments.toFloat() / TcgCard.MEMORY_FRAGMENT_TARGET,
                        color = TcgColors.Abyss,
                        height = 7
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    "파편 수색은 기억이 지워진 동료 1명에게 기억 파편 +1을 지급합니다. " +
                            "3/3이 되면 캐릭터 관리에서 「기억의 조각 회상」 이벤트를 열 수 있습니다.",
                    color = TcgColors.Muted,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
                ActionTile(
                    label = "🃏 캐릭터 관리에서 회상 이벤트 열기",
                    hint = "파편 3/3 달성 시 기억 복원 · 스탯 보너스",
                    accent = TcgColors.Violet,
                    onClick = onOpenCollection
                )
            }
        }

        // ── 탐색 기록 ──
        SectionCard(title = "탐색 기록", accent = TcgColors.Muted) {
            LogBox(state.explorationLog, maxLines = 6)
            ActionTile(
                label = "🏙 현실 소환으로 이동",
                hint = "탐색으로 오른 호감도로 데이트 룩을 해금하세요",
                accent = TcgColors.Gold,
                onClick = onOpenRealWorld
            )
        }
    }
}
