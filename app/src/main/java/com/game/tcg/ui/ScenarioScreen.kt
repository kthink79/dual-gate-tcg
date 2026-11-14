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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 탭 1 — 📖 시나리오 (홈)
 * 프롤로그 ~ 제1장 5화 진행 바, 스토리 카드, 인터랙티브 샘플 이벤트 2종
 */
@Composable
fun ScenarioScreen(
    state: TcgGameState,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onChooseEventA: (Boolean) -> Unit,
    onChooseEventB: (Boolean) -> Unit
) {
    ScreenFrame {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ResourceBadge("🪙", "금화", "${state.gold}G", TcgColors.Gold)
            ResourceBadge("💎", "보석", "${state.gems}", TcgColors.Mana)
            ResourceBadge("💧", "성수", "${state.holyWater}", TcgColors.Heal)
            ResourceBadge(
                "🌫", "홀림",
                "${state.combatPenalty.totalStacks}",
                if (state.combatPenalty.totalStacks > 0) TcgColors.Danger else TcgColors.Muted
            )
        }

        // ── 챕터 진행 바 ──
        SectionCard(title = "제1장 · 황혼의 계약", badge = "진행 ${(state.storyStepIndex + 1) * 100 / StoryScript.STEPS.size}%") {
            StatBar(
                ratio = (state.storyStepIndex + 1).toFloat() / StoryScript.STEPS.size,
                color = TcgColors.Violet,
                height = 10
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StoryScript.STEPS.forEachIndexed { index, step ->
                    Text(
                        text = if (index <= state.storyStepIndex) "●" else "○",
                        color = if (index <= state.storyStepIndex) TcgColors.Violet else TcgColors.Line,
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                "프롤로그 → 제1장 5화 · 총 ${StoryScript.STEPS.size}단계 중 ${state.storyStepIndex + 1}단계",
                color = TcgColors.Muted,
                fontSize = 10.sp
            )
        }

        // ── 현재 스토리 카드 ──
        val step = StoryScript.STEPS[state.storyStepIndex]
        SectionCard(title = "스토리 카드", badge = step.chapter, accent = TcgColors.Gold) {
            Text(step.title, color = TcgColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TcgColors.SurfaceAlt)
                    .padding(12.dp)
            ) {
                Text(step.body, color = TcgColors.Text.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 19.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("목표", TcgColors.Gold)
                Spacer(Modifier.width(6.dp))
                Text(step.objective, color = TcgColors.Gold.copy(alpha = 0.9f), fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "◀ 이전 화",
                        accent = TcgColors.Muted,
                        enabled = state.storyStepIndex > 0,
                        onClick = onPrevStep
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "다음 화 ▶",
                        hint = if (state.storyStepIndex < StoryScript.STEPS.size - 1) "스토리 진행" else "제1장 완료",
                        accent = TcgColors.Violet,
                        onClick = onNextStep
                    )
                }
            }
        }

        // ── 인터랙티브 샘플 이벤트 A ──
        EventCard(
            event = StoryScript.eventById("EVENT_A"),
            chosenOptionId = state.eventAChoice,
            resultText = state.eventAResult,
            accent = TcgColors.Abyss,
            onChoose = onChooseEventA
        )

        // ── 인터랙티브 샘플 이벤트 B ──
        EventCard(
            event = StoryScript.eventById("EVENT_B"),
            chosenOptionId = state.eventBChoice,
            resultText = state.eventBResult,
            accent = TcgColors.Mana,
            onChoose = onChooseEventB
        )

        // ── 진행 로그 ──
        SectionCard(title = "진행 기록", accent = TcgColors.Muted) {
            LogBox(state.storyReadLog, maxLines = 8)
            if (state.lilyFamiliarity > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("릴리(가면) 친밀도", TcgColors.Abyss)
                    Spacer(Modifier.width(6.dp))
                    Text("${state.lilyFamiliarity}/100", color = TcgColors.Abyss, fontSize = 11.sp)
                }
            }
            if (state.defenseBonusPercent > 0 || state.intelBonusPercent > 0) {
                Text(
                    "전투 보정 — 방어 +${state.defenseBonusPercent}% · 급소 피해 +${state.intelBonusPercent}%",
                    color = TcgColors.Heal,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: StoryEvent,
    chosenOptionId: String?,
    resultText: String?,
    accent: Color,
    onChoose: (Boolean) -> Unit
) {
    SectionCard(title = event.title, badge = event.badge, accent = accent) {
        Text(event.scene, color = TcgColors.Text.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp)

        if (chosenOptionId == null) {
            event.options.forEach { option ->
                ActionTile(
                    label = option.label,
                    hint = option.hint,
                    accent = if (option.isTempted) TcgColors.Danger else accent,
                    onClick = { onChoose(option.isTempted) }
                )
            }
        } else {
            val chosen = event.options.firstOrNull { it.id == chosenOptionId }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("선택 완료", TcgColors.Heal)
                Spacer(Modifier.width(6.dp))
                Text(chosen?.label ?: "선택", color = TcgColors.Text, fontSize = 11.sp)
            }
            if (resultText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(resultText, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
