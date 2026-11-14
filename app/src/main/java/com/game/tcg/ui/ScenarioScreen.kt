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
import com.game.tcg.core.engine.ChapterTwoScript

/**
 * 탭 1 — 📖 시나리오 (홈)
 * 프롤로그 ~ 제1장 5화 진행 바, 스토리 카드, 인터랙티브 샘플 이벤트 2종
 * + 「신뢰의 균열」 긴급 교차 이벤트 + 제2장 프롤로그 「그림자 기사」 전초전 노드
 */
@Composable
fun ScenarioScreen(
    state: TcgGameState,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onChooseEventA: (Boolean) -> Unit,
    onChooseEventB: (Boolean) -> Unit,
    onResolveTrustRift: (String) -> Unit,
    onOpenChapter2: () -> Unit,
    onStartChapter2Boss: () -> Unit
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

        // ── 「신뢰의 균열」 긴급 교차 이벤트 ([홀림] 2스택 이상) ──
        TrustRiftEventCard(
            state = state,
            onResolve = onResolveTrustRift
        )

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

        // ── 제2장 프롤로그: 제1군단장 「그림자 기사」 전초전 노드 ──
        Chapter2PreludeCard(
            state = state,
            onOpenChapter2 = onOpenChapter2,
            onStartChapter2Boss = onStartChapter2Boss
        )
    }
}

/**
 * 「신뢰의 균열: 마왕의 홀림을 눈치챈 파트너」 — 현실·이세계 교차 긴급 이벤트
 */
@Composable
private fun TrustRiftEventCard(
    state: TcgGameState,
    onResolve: (String) -> Unit
) {
    val stacks = state.combatPenalty.totalStacks
    val alreadyResolved = state.trustRiftChoice != null
    if (!state.isTrustRiftAvailable && !alreadyResolved) {
        // [홀림] 스택이 2 미만이면 잠금 안내만 남긴다.
        SectionCard(
            title = "신뢰의 균열 — 마왕의 홀림을 눈치챈 파트너",
            badge = "잠금 · 홀림 ${stacks}/2",
            accent = TcgColors.Muted
        ) {
            Text(
                "[홀림] 스택이 2 이상 쌓이면 아우렐리아가 눈치채고 추궁하는 긴급 교차 이벤트가 열립니다. " +
                        "(도감에서 마왕 카드를 응시하거나 일상 잠입 이벤트를 겪으면 스택이 쌓입니다.)",
                color = TcgColors.Muted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }
        return
    }

    SectionCard(
        title = "신뢰의 균열 — 마왕의 홀림을 눈치챈 파트너",
        badge = if (alreadyResolved) "선택 완료" else "긴급 · 홀림 ${stacks}스택",
        accent = TcgColors.Danger
    ) {
        Text(
            "현실로 돌아온 밤, 아우렐리아가 카드에서 스스로 걸어 나왔다. 평상복 차림인데도 검을 쥔 손에 힘이 들어가 있다.\n" +
                    "「요즘 눈빛이 흔들리고 있어요… 그 카페의 릴리라는 사람 때문인가요?」\n" +
                    "「저는 계약자님의 방패입니다. 방패에게조차 말하지 못할 일이 생긴 거라면… 그게 더 아픕니다.」",
            color = TcgColors.Text.copy(alpha = 0.88f),
            fontSize = 12.sp,
            lineHeight = 19.sp
        )

        if (!alreadyResolved) {
            ActionTile(
                label = "A. 솔직히 털어놓고 함께 정화한다",
                hint = "성수 소모 없이 [홀림] 스택 -1 · 아우렐리아 호감도 +10",
                accent = TcgColors.Heal,
                onClick = { onResolve("RIFT_A_CONFESS") }
            )
            ActionTile(
                label = "B. 아무 일 아니라고 숨긴다",
                hint = "[홀림] 스택 +1 · 호감도 -15 · 다음 전투 전열 방어 -10%",
                accent = TcgColors.Danger,
                onClick = { onResolve("RIFT_B_HIDE") }
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("선택 완료", TcgColors.Heal)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (state.trustRiftChoice == "RIFT_A_CONFESS") "A · 솔직한 고백" else "B · 숨김",
                    color = TcgColors.Text,
                    fontSize = 11.sp
                )
            }
            if (state.trustRiftMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TcgColors.Danger.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(
                        state.trustRiftMessage,
                        color = TcgColors.Text.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            Text(
                "현재 [홀림] ${state.combatPenalty.totalStacks}스택 · 호감도 ${state.partnerBond}/100" +
                        (if (state.frontRowDefensePenaltyPercent > 0)
                            " · 다음 전투 전열 방어 -${state.frontRowDefensePenaltyPercent}% 예약됨" else ""),
                color = TcgColors.Muted,
                fontSize = 9.sp
            )
        }
    }
}

/**
 * 제2장 프롤로그 노드 — 5화 이후 「제1군단장 그림자 기사」 전초전
 */
@Composable
private fun Chapter2PreludeCard(
    state: TcgGameState,
    onOpenChapter2: () -> Unit,
    onStartChapter2Boss: () -> Unit
) {
    val node = ChapterTwoScript.SHADOW_KNIGHT_PRELUDE
    val unlocked = node.isUnlocked(state.storyStepIndex)

    SectionCard(
        title = "${node.chapter} · ${node.title}",
        badge = if (unlocked) "개방" else "5화 이후 개방",
        accent = if (unlocked) TcgColors.Abyss else TcgColors.Muted
    ) {
        Text(
            if (unlocked) {
                node.body
            } else {
                "제1장 5화 '가면이 벗겨진 밤'을 마치면 제2장 프롤로그 노드가 열립니다. " +
                        "(현재 ${state.storyStepIndex + 1}/5화)"
            },
            color = TcgColors.Text.copy(alpha = if (unlocked) 0.88f else 0.6f),
            fontSize = 12.sp,
            lineHeight = 19.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("목표", TcgColors.Violet)
            Spacer(Modifier.width(6.dp))
            Text(node.objective, color = TcgColors.Violet, fontSize = 10.sp)
        }
        if (unlocked) {
            ActionTile(
                label = if (state.chapter2PreludeOpened) "📜 프롤로그 열람 완료 · 다시 보기" else "📜 제2장 프롤로그 열기",
                hint = "다음 여정 노드를 진행 기록에 남깁니다",
                accent = TcgColors.Abyss,
                onClick = onOpenChapter2
            )
            ActionTile(
                label = "⚔ 제1군단장 그림자 기사 전초전 개시",
                hint = "보스 HP ${node.bossHp} · 공격력 ${node.bossAtk} · ${node.bossElement.displayName}",
                accent = TcgColors.Hp,
                onClick = onStartChapter2Boss
            )
        }
        if (state.chapter2Message != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TcgColors.Abyss.copy(alpha = 0.14f))
                    .padding(12.dp)
            ) {
                Text(
                    state.chapter2Message,
                    color = TcgColors.Text.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    lineHeight = 16.sp
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
