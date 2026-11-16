package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.engine.SummonEngine
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 우측 서브메뉴 「🏙 현실 소환」 — 파트너를 현실로 부르고 교감한다.
 *
 * 2026-11-16 사용자 요청("텍스트 위주라 게임성이 부족하다") 반영:
 *  - 소환한 캐릭터의 **일상(casual)·데이트(date) 실제 일러스트**를 크게 세운 교감 카드
 *  - 하트 호감도 게이지(❤ 5칸 + 수치 + 단계 라벨)
 *  - 🌹 데이트 / ☕ 산책 / 💬 대화 **액션 카드 3종**과 교감 직후 **하트 팝 + 반응 말풍선**
 */
@Composable
fun RealWorldScreen(
    state: TcgGameState,
    onInteract: (RealWorldActivity) -> Unit,
    onBondMemoryPartner: (Int) -> Unit,
    onSummon: (Int) -> Unit = {},
    onSelectSummonCard: (Int) -> Unit = {}
) {
    val summonTarget = state.summonCard
    val partnerName = summonTarget?.name ?: "파트너"
    val bond = summonTarget?.bondLevel ?: state.partnerBond
    val look = summonTarget?.currentVisual ?: state.partnerVisual
    // 현실(일상) 소환 조건 — 일상 사복(CASUAL_CUTE) 해금 = 친밀도 30
    val canSummonPartner = summonTarget != null && AffinityRules.canRealitySummon(summonTarget.bondLevel)
    val casualProgress = AffinityRules.casualProgress(bond)

    ScreenFrame {
        // ── ① 현실 소환 — 차원 에너지로 보유 캐릭터를 부른다 ──
        SectionCard(
            title = "현실 소환 · $partnerName",
            badge = "⚡ ${state.gateEnergy}/${state.gateEnergyMax}",
            accent = TcgColors.Violet
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 대상 초상화 — 2:3 원본 비율을 유지해 머리·발끝이 잘리지 않게 그린다
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TcgColors.SurfaceAlt)
                        .border(1.dp, TcgColors.Violet.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                ) {
                    CardArtImage(
                        cardId = summonTarget?.id ?: TcgGameViewModel.DEFAULT_LOBBY_HERO_ID,
                        visual = look,
                        contentDescription = partnerName,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                        targetWidthPx = 200
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "차원 균열 에너지 ${SummonEngine.ENERGY_COST}로 소환 → 친밀도 +${SummonEngine.BOND_GAIN}",
                        color = TcgColors.Text.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Pill(
                            "일상 ${AffinityRules.CASUAL_UNLOCK_BOND} · 데이트 ${AffinityRules.DATE_UNLOCK_BOND}",
                            TcgColors.Gold
                        )
                        Pill("+${SummonEngine.BUFF_PERCENT}% ${SummonEngine.BUFF_BATTLES}회", TcgColors.Heal)
                    }
                    if (state.hasSummonBuff) {
                        Pill(
                            "소환 버프 유지 · +${state.summonBuffPercent}% (${state.summonBuffBattles}회)",
                            TcgColors.Heal,
                            filled = true
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.ownedCards.sortedByDescending { it.bondLevel }.forEach { owned ->
                    // 일상 사복 미해금(친밀도 30 미만) 캐릭터는 목록에서 잠금으로 보인다
                    val locked = !AffinityRules.canRealitySummon(owned.bondLevel)
                    ChipButton(
                        label = "${if (locked) "🔒 " else ""}${owned.name.take(6)} ${owned.bondLevel}",
                        selected = owned.id == summonTarget?.id,
                        accent = if (locked) TcgColors.Line else elementColor(owned.element)
                    ) { onSelectSummonCard(owned.id) }
                }
            }
            if (summonTarget != null && !canSummonPartner) {
                // 미해금 — 잠금 안내 + 해금까지의 진행도 게이지
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "🔒 $partnerName — 일상 사복 미해금",
                        color = TcgColors.Danger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "친밀도 ${AffinityRules.CASUAL_UNLOCK_BOND} 이상에서 현실 소환이 열립니다. " +
                            "(현재 $bond/100 · ${(AffinityRules.CASUAL_UNLOCK_BOND - bond).coerceAtLeast(0)} 더 필요)",
                        color = TcgColors.Text.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        lineHeight = 14.sp
                    )
                    StatBar(ratio = casualProgress, color = TcgColors.Danger, height = 8)
                    Text(
                        "가장 빠른 길은 이계 전투 출전 — 출전 1회 +${AffinityRules.BATTLE_DEPLOY_GAIN}, " +
                            "승리하면 +${AffinityRules.BATTLE_WIN_GAIN} (출전 $bond/100)",
                        color = TcgColors.Muted,
                        fontSize = 9.sp,
                        lineHeight = 14.sp
                    )
                }
            }
            if (summonTarget != null) {
                ActionTile(
                    label = if (canSummonPartner) {
                        "🌙 $partnerName 현실 소환 (⚡ ${SummonEngine.ENERGY_COST})"
                    } else {
                        "🔒 $partnerName 현실 소환 — 친밀도 ${AffinityRules.CASUAL_UNLOCK_BOND} 필요"
                    },
                    hint = when {
                        !canSummonPartner -> "이계 전투 출전으로 친밀도를 올리면 열립니다 (현재 $bond/100)"
                        state.gateEnergy >= SummonEngine.ENERGY_COST ->
                            "친밀도 +${SummonEngine.BOND_GAIN} · 아군 공격력 +${SummonEngine.BUFF_PERCENT}% (${SummonEngine.BUFF_BATTLES}회)"
                        else -> "차원 에너지 부족 — 이계 전투 승리로 +2 회복"
                    },
                    accent = if (canSummonPartner) TcgColors.Violet else TcgColors.Muted,
                    enabled = canSummonPartner && state.gateEnergy >= SummonEngine.ENERGY_COST,
                    onClick = { onSummon(summonTarget.id) }
                )
            }
            if (state.summonMessage != null) {
                Text(state.summonMessage, color = TcgColors.Text.copy(alpha = 0.85f), fontSize = 10.sp, lineHeight = 15.sp)
            }
            Text(
                "소환 ${state.summonCount}회 · 교감은 아래 「오늘의 교감」에서 이어집니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp
            )
        }

        // ── ② 교감 카드 — 일상/데이트 일러스트 + 하트 게이지 + 반응 ──
        BondPortraitCard(
            cardId = summonTarget?.id ?: TcgGameViewModel.DEFAULT_LOBBY_HERO_ID,
            name = partnerName,
            bond = bond,
            visual = look,
            element = summonTarget?.element ?: com.game.tcg.core.model.Element.LIGHT,
            lastLog = RealWorldDecor.lastReaction(state.realWorldLog)
        )

        // ── ③ 오늘의 교감 — 액션 카드 3종 ──
        SectionCard(title = "오늘의 교감", badge = "호감도 상승", accent = TcgColors.Gold) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RealWorldDecor.actionCards().forEach { card ->
                    Box(modifier = Modifier.weight(1f)) {
                        BondActionCard(
                            icon = card.icon,
                            title = card.title,
                            hint = card.hint,
                            bondGain = card.bondGain,
                            accent = card.tone.color(),
                            onClick = { onInteract(card.activity) }
                        )
                    }
                }
            }
            Text(
                "친밀도 ${AffinityRules.CASUAL_UNLOCK_BOND}에서 일상 사복·현실 소환, " +
                    "${AffinityRules.DATE_UNLOCK_BOND}에서 데이트 룩, 100에서 페어 얼티밋 오의(+25%)가 열립니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }

        // ── ④ 「기억의 잔향」: 기억이 지워진 동료와의 재회 ──
        val memoryPartner = state.memoryResetCards.firstOrNull()
        if (memoryPartner != null) {
            MemoryRecallSection(state = state, partner = memoryPartner, onBond = onBondMemoryPartner)
        }

        // ── ⑤ 데이트 룩 해금 연출 ──
        SectionCard(
            title = "데이트 룩 해금 연출 · $partnerName",
            badge = if (bond >= SummonEngine.DATE_UNLOCK_BOND) "해금" else "잠금",
            accent = if (bond >= SummonEngine.DATE_UNLOCK_BOND) TcgColors.Heal else TcgColors.Muted
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                CardArtImage(
                    cardId = summonTarget?.id ?: TcgGameViewModel.DEFAULT_LOBBY_HERO_ID,
                    visual = if (bond >= SummonEngine.DATE_UNLOCK_BOND) {
                        VisualState.ROMANTIC_DATE
                    } else {
                        VisualState.CASUAL_CUTE
                    },
                    contentDescription = "데이트 룩",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 540
                )
                if (bond < SummonEngine.DATE_UNLOCK_BOND) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC0B0A12)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🔒 친밀도 ${SummonEngine.DATE_UNLOCK_BOND} 달성 시 해금\n(현재 $bond/100)",
                            color = TcgColors.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (bond >= SummonEngine.DATE_UNLOCK_BOND) {
                Text(
                    "「이 옷, 당신이 좋아할까 싶어서…」 — 데이트 룩이 개방되었습니다. 도감 ②③ 탭에서도 확인할 수 있습니다.",
                    color = TcgColors.Heal,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
            if (bond >= SummonEngine.ULTIMATE_UNLOCK_BOND) {
                Text(
                    "페어 얼티밋 오의 개방 — $partnerName 와(과)의 서약이 완성되었습니다. (전투 데미지 +25%)",
                    color = TcgColors.Gold,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // ── ⑥ 교감 기록 ──
        SectionCard(title = "교감 기록", accent = TcgColors.Muted) {
            LogBox(state.realWorldLog, maxLines = 6)
        }
    }
}

/** 교감 카드 — 일상/데이트 일러스트 위에 하트 게이지와 반응 말풍선을 얹는다 */
@Composable
private fun BondPortraitCard(
    cardId: Int,
    name: String,
    bond: Int,
    visual: VisualState,
    element: com.game.tcg.core.model.Element,
    lastLog: String?
) {
    // 교감 직후 하트가 떠오르는 반응 연출 (호감도가 바뀔 때마다)
    val pop = remember { Animatable(0f) }
    LaunchedEffect(bond) {
        if (bond > 0) {
            pop.snapTo(1f)
            pop.animateTo(0f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 540×810(2:3) 원본 비율을 그대로 유지 — 머리·발끝이 잘리지 않는다
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                when {
                    bond >= SummonEngine.ULTIMATE_UNLOCK_BOND -> TcgColors.Gold
                    bond >= SummonEngine.DATE_UNLOCK_BOND -> TcgColors.Heal
                    else -> TcgColors.Line
                },
                RoundedCornerShape(16.dp)
            )
    ) {
        CardArtImage(
            cardId = cardId,
            visual = visual,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 900
        )
        // 가독성 스크림
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x22000000), Color(0x50000000), Color(0xEE0B0A12))))
        )

        // 하트 팝 연출
        if (pop.value > 0.01f) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = pop.value
                        translationY = -(1f - pop.value) * 90f
                        scaleX = 0.8f + 0.5f * pop.value
                        scaleY = 0.8f + 0.5f * pop.value
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { Text("❤", fontSize = 22.sp) }
            }
        }

        // 상단: 모습 라벨 + 속성
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Pill(visual.displayName, TcgColors.Gold, filled = true)
            Pill(element.displayName, elementColor(element))
            if (bond >= SummonEngine.DATE_UNLOCK_BOND) Pill("데이트 룩 해금", TcgColors.Heal)
        }

        // 하단: 이름 + 하트 게이지 + 반응
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(name, color = TcgColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ❤ 5칸 하트 게이지
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RealWorldDecor.hearts(bond).forEach { filled ->
                        Text(
                            if (filled) "❤" else "♡",
                            color = if (filled) TcgColors.Danger else TcgColors.Muted.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    RealWorldDecor.heartLabel(bond),
                    color = TcgColors.Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Pill(RealWorldDecor.bondStageLabel(bond), TcgColors.Muted)
            }
            StatBar(ratio = bond / 100f, color = TcgColors.Danger, height = 7)

            // 파트너 반응 말풍선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xB3100D18))
                    .border(1.dp, TcgColors.Violet.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        RealWorldDecor.reactionLine(bond),
                        color = TcgColors.Text.copy(alpha = 0.92f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    if (lastLog != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(lastLog, color = TcgColors.Muted, fontSize = 8.sp, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}

/** 교감 액션 카드 — 아이콘 · 제목 · 힌트 · 호감도 상승 배지 */
@Composable
private fun BondActionCard(
    icon: String,
    title: String,
    hint: String,
    bondGain: Int,
    accent: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Text(title, color = TcgColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            hint,
            color = TcgColors.Muted,
            fontSize = 8.sp,
            lineHeight = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Pill("❤ +$bondGain", accent)
    }
}

/** 「기억의 잔향」 — 기억이 지워진 동료와의 재회 교감 */
@Composable
private fun MemoryRecallSection(
    state: TcgGameState,
    partner: TcgCard,
    onBond: (Int) -> Unit
) {
    val target = TcgCard.MEMORY_FRAGMENT_TARGET
    SectionCard(
        title = "기억의 잔향 — 기억이 지워진 동료",
        badge = "기억 파편 ${partner.memoryFragments}/$target",
        accent = TcgColors.Abyss
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, TcgColors.Abyss.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                CardArtImage(
                    cardId = partner.id,
                    visual = VisualState.CASUAL_CUTE,
                    contentDescription = partner.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 240
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TcgColors.Abyss.copy(alpha = 0.12f))
                    .padding(10.dp)
            ) {
                Text(
                    "「저… 계약자님이 맞으시죠? 이상하게, 이 문양을 보면 가슴이 아파요.」\n" +
                        "${partner.name}은(는) 죽음을 넘어 돌아왔지만, 당신과의 기억을 전부 잃었습니다.",
                    color = TcgColors.Text.copy(alpha = 0.88f),
                    fontSize = 10.sp,
                    lineHeight = 16.sp
                )
            }
        }
        StatBar(
            ratio = partner.memoryFragments.toFloat() / target,
            color = TcgColors.Abyss,
            height = 9
        )
        Text(
            "기억 파편 ${partner.memoryFragments}/$target · 친밀도 ${partner.bondLevel}/100",
            color = TcgColors.Muted,
            fontSize = 9.sp
        )
        ActionTile(
            label = "기억의 잔향 교감 — ${partner.name}",
            hint = "기억 파편 +1 · 호감도 +5 (전장 승리로도 파편 획득)",
            accent = TcgColors.Abyss,
            enabled = partner.memoryFragments < target,
            onClick = { onBond(partner.id) }
        )
        if (partner.memoryFragments >= target) {
            Text(
                "기억 파편이 모두 모였습니다 — 도감·서약 탭에서 「기억의 조각 회상」을 열어 기억을 복원하세요.",
                color = TcgColors.Gold,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }
        if (state.memoryRecallMessage != null && partner.memoryFragments < target) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TcgColors.Abyss.copy(alpha = 0.14f))
                    .padding(10.dp)
            ) {
                Text(
                    state.memoryRecallMessage,
                    color = TcgColors.Text.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
