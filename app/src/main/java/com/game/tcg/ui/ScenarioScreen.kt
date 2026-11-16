package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.ChapterTwoScript

/**
 * 좌측 서브메뉴 「📖 이세계 접속」 — 프롤로그 ~ 제1장 스토리 · 인터랙티브 이벤트.
 *
 * 2026-11-16 사용자 요청("텍스트 위주라 게임성이 부족하다") 반영:
 *  - 상단에 **차원문 배너**(인트로 3장면 일러스트) + 챕터/스테이지 진행
 *  - 동행 캐릭터(아우렐리아)의 **일러스트 초상화** + 비주얼 노벨풍 대사창
 *  - 선택지를 **아이콘 · 보상/리스크 미리보기**가 담긴 카드로
 *  - 「신뢰의 균열」 긴급 이벤트도 초상화 + 말풍선으로
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
    val step = StoryScript.STEPS[state.storyStepIndex]

    ScreenFrame {
        // ── ① 차원문 배너 + 현재 화 ──
        ScenarioBanner(state = state, step = step)

        // ── ② 동행 캐릭터 초상화 + 비주얼 노벨 대사창 ──
        CompanionDialogue(
            cardId = ScenarioDecor.COMPANION_CARD_ID,
            speaker = ScenarioDecor.COMPANION_NAME,
            visual = ScenarioDecor.COMPANION_VISUAL,
            body = step.body,
            objective = step.objective
        )

        // ── ③ 화 이동 ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // ── ④ 「신뢰의 균열」 긴급 교차 이벤트 ([홀림] 2스택 이상) ──
        TrustRiftEventCard(state = state, onResolve = onResolveTrustRift)

        // ── ⑤ 인터랙티브 이벤트 A · B ──
        EventCard(
            event = StoryScript.eventById("EVENT_A"),
            chosenOptionId = state.eventAChoice,
            resultText = state.eventAResult,
            accent = TcgColors.Abyss,
            onChoose = onChooseEventA
        )
        EventCard(
            event = StoryScript.eventById("EVENT_B"),
            chosenOptionId = state.eventBChoice,
            resultText = state.eventBResult,
            accent = TcgColors.Mana,
            onChoose = onChooseEventB
        )

        // ── ⑥ 제2장 프롤로그 노드 ──
        Chapter2PreludeCard(
            state = state,
            onOpenChapter2 = onOpenChapter2,
            onStartChapter2Boss = onStartChapter2Boss
        )

        // ── ⑦ 진행 기록 ──
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

/** 차원문 분위기의 상단 배너 — 현재 화 제목과 챕터 진행을 겹쳐 그린다 */
@Composable
private fun ScenarioBanner(state: TcgGameState, step: StoryStep) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        ResourceArtImage(
            resId = ScenarioDecor.bannerResId,
            contentDescription = "차원의 문 배너",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 1080
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x990A0714), Color(0x330A0714), Color(0xF20A0714))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("📖 이세계 접속", TcgColors.Violet, filled = true)
                Spacer(Modifier.width(6.dp))
                Pill(step.chapter, TcgColors.Muted)
                Spacer(Modifier.weight(1f))
                Pill("${state.storyStepIndex + 1}/${StoryScript.STEPS.size}", TcgColors.Gold)
            }
            Column {
                Text(
                    step.title,
                    color = TcgColors.Text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("목표", TcgColors.Gold)
                    Spacer(Modifier.width(6.dp))
                    Text(step.objective, color = TcgColors.Gold.copy(alpha = 0.9f), fontSize = 10.sp)
                }
                Spacer(Modifier.height(7.dp))
                StatBar(
                    ratio = (state.storyStepIndex + 1).toFloat() / StoryScript.STEPS.size,
                    color = TcgColors.Violet,
                    height = 6
                )
            }
        }
    }
}

/** 초상화 + 비주얼 노벨 대사창 (화자 이름표 · 본문 · 목표) */
@Composable
private fun CompanionDialogue(
    cardId: Int,
    speaker: String,
    visual: com.game.tcg.core.model.VisualState,
    body: String,
    objective: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        // 초상화
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(126.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, TcgColors.Violet.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
        ) {
            CardArtImage(
                cardId = cardId,
                visual = visual,
                contentDescription = "$speaker 초상화",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = 300
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60A0714))))
            )
        }
        Spacer(Modifier.width(8.dp))
        // 대사창
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF100D18))
                .border(1.dp, TcgColors.Line, RoundedCornerShape(14.dp))
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Pill("🎙 $speaker", TcgColors.Violet, filled = true)
            Text(
                body,
                color = TcgColors.Text.copy(alpha = 0.9f),
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Text(
                "▸ $objective",
                color = TcgColors.Gold.copy(alpha = 0.85f),
                fontSize = 9.sp
            )
        }
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
        // 초상화 + 말풍선
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, TcgColors.Danger.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            ) {
                CardArtImage(
                    cardId = ScenarioDecor.COMPANION_CARD_ID,
                    visual = com.game.tcg.core.model.VisualState.BATTLE,
                    contentDescription = "아우렐리아",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 260
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TcgColors.Danger.copy(alpha = 0.10f))
                    .border(1.dp, TcgColors.Danger.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Pill("🎙 아우렐리아", TcgColors.Danger, filled = true)
                Text(
                    "현실로 돌아온 밤, 아우렐리아가 카드에서 스스로 걸어 나왔다. 평상복 차림인데도 검을 쥔 손에 힘이 들어가 있다.\n" +
                        "「요즘 눈빛이 흔들리고 있어요… 그 카페의 릴리라는 사람 때문인가요?」\n" +
                        "「저는 계약자님의 방패입니다. 방패에게조차 말하지 못할 일이 생긴 거라면… 그게 더 아픕니다.」",
                    color = TcgColors.Text.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }

        if (!alreadyResolved) {
            ChoiceCard(
                icon = "🤝",
                label = "A. 솔직히 털어놓고 함께 정화한다",
                reward = "성수 소모 없이 [홀림] 스택 -1 · 호감도 +10",
                risk = null,
                accent = TcgColors.Heal,
                onClick = { onResolve("RIFT_A_CONFESS") }
            )
            ChoiceCard(
                icon = "🤐",
                label = "B. 아무 일 아니라고 숨긴다",
                reward = null,
                risk = "[홀림] 스택 +1 · 호감도 -15 · 다음 전투 전열 방어 -10%",
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

/** 나레이션 + 선택지 카드 묶음 (이벤트 1건) */
@Composable
private fun EventCard(
    event: StoryEvent,
    chosenOptionId: String?,
    resultText: String?,
    accent: Color,
    onChoose: (Boolean) -> Unit
) {
    SectionCard(title = event.title, badge = event.badge, accent = accent) {
        // 나레이션 박스 (비주얼 노벨 하단 대사창 스타일)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF100D18))
                .padding(12.dp)
        ) {
            Text(
                event.scene,
                color = TcgColors.Text.copy(alpha = 0.88f),
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }

        if (chosenOptionId == null) {
            event.options.forEach { option ->
                val preview = ScenarioDecor.choicePreview(option.label, option.hint, option.isTempted)
                ChoiceCard(
                    icon = preview.icon,
                    label = option.label,
                    reward = preview.reward,
                    risk = preview.risk,
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

/**
 * 아이콘 · 보상/리스크 미리보기가 담긴 선택지 카드.
 * 보상은 초록 배지, 리스크는 빨강 배지로 나뉘어 한눈에 대가를 알 수 있다.
 */
@Composable
fun ChoiceCard(
    icon: String,
    label: String,
    reward: String?,
    risk: String?,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent.copy(alpha = 0.10f) else TcgColors.SurfaceAlt)
            .border(
                1.dp,
                if (enabled) accent.copy(alpha = 0.5f) else TcgColors.Line,
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 14.sp)
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                label,
                color = if (enabled) TcgColors.Text else TcgColors.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (reward != null) {
                    Pill("보상 · $reward", TcgColors.Heal)
                }
                if (risk != null) {
                    Pill("리스크 · $risk", TcgColors.Danger)
                }
            }
        }
        Text("›", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
