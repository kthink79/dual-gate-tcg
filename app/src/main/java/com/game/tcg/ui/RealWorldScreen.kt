package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 * 2026-11-26 사용자 요청 「메뉴 아이콘 중심·설명문 축소·글자 확대·종스크롤 최소화」:
 *  - 소환 · 오늘의 교감 · 데이트 룩 해금을 **세로로 쌓지 않고** 상단 아이콘 탭
 *    (✨ 소환 / 🤝 교감 / 🎬 해금)으로 나눠 **스크롤 없이 한 화면**에 담는다.
 *  - 상·하단에 중복되던 긴 설명문("차원 균열 에너지 8로 소환 → …", "친밀도 30에서 …")을 걷어내고
 *    아이콘 + 짧은 1~2단어 라벨·수치 배지로 대체했다.
 *  - 글씨는 [TcgType] 기준(제목 24 · 섹션 18 · 본문 15 · 배지 13) 이상만 쓴다.
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
    val partnerId = summonTarget?.id ?: TcgGameViewModel.DEFAULT_LOBBY_HERO_ID
    val bond = summonTarget?.bondLevel ?: state.partnerBond
    val look = summonTarget?.currentVisual ?: state.partnerVisual
    val canSummonPartner = summonTarget != null && AffinityRules.canRealitySummon(summonTarget.bondLevel)
    val casualProgress = AffinityRules.casualProgress(bond)

    // 0 소환 · 1 교감 · 2 해금
    var tab by remember { mutableIntStateOf(0) }
    val memoryPartner = state.memoryResetCards.firstOrNull()

    CompactFrame {
        // ── 머리 1줄: 제목 + 차원 에너지 + 파트너 (설명문 없음) ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("현실 소환", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Pill("⚡ ${state.gateEnergy}/${state.gateEnergyMax}", TcgColors.Mana)
            Spacer(Modifier.weight(1f))
            Pill("#${partnerId} $partnerName", elementColor(summonTarget?.element ?: com.game.tcg.core.model.Element.LIGHT))
        }

        TcgIconTabBar(
            tabs = listOf("✨" to "소환", "🤝" to "교감", "🎬" to "해금"),
            selectedIndex = tab,
            accent = TcgColors.Violet
        ) { tab = it }

        when (tab) {
            0 -> SummonTab(
                state = state,
                summonTarget = summonTarget,
                partnerName = partnerName,
                partnerId = partnerId,
                bond = bond,
                look = look,
                canSummonPartner = canSummonPartner,
                casualProgress = casualProgress,
                memoryPartner = memoryPartner,
                onSelectSummonCard = onSelectSummonCard,
                onSummon = onSummon,
                onBondMemoryPartner = onBondMemoryPartner
            )

            1 -> BondTab(
                state = state,
                partnerName = partnerName,
                partnerId = partnerId,
                bond = bond,
                look = look,
                element = summonTarget?.element ?: com.game.tcg.core.model.Element.LIGHT,
                onInteract = onInteract
            )

            else -> UnlockTab(
                partnerName = partnerName,
                partnerId = partnerId,
                bond = bond
            )
        }
    }
}

// ============================================================================
// ✨ 소환 탭 — 파트너 선택 1줄 + 원화 + 소환 버튼
// ============================================================================

@Composable
private fun ColumnScope.SummonTab(
    state: TcgGameState,
    summonTarget: TcgCard?,
    partnerName: String,
    partnerId: Int,
    bond: Int,
    look: VisualState,
    canSummonPartner: Boolean,
    casualProgress: Float,
    memoryPartner: TcgCard?,
    onSelectSummonCard: (Int) -> Unit,
    onSummon: (Int) -> Unit,
    onBondMemoryPartner: (Int) -> Unit
) {
    // 파트너 선택 — 초상화 아이콘 1줄(가로 스크롤). 이름·친밀도만 짧게.
    val owned = state.ownedCards.sortedByDescending { it.bondLevel }
    if (owned.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(owned, key = { it.id }) { ownedCard ->
                val locked = !AffinityRules.canRealitySummon(ownedCard.bondLevel)
                PortraitIconTile(
                    cardId = ownedCard.id,
                    visual = ownedCard.currentVisual,
                    modifier = Modifier.width(52.dp),
                    label = "${ownedCard.name.take(4)} ${ownedCard.bondLevel}",
                    selected = ownedCard.id == summonTarget?.id,
                    locked = locked,
                    accent = elementColor(ownedCard.element),
                    contentDescription = ownedCard.name,
                    onClick = { onSelectSummonCard(ownedCard.id) }
                )
            }
        }
    }

    // 원화 — 남는 높이를 모두 쓴다(스크롤 없음)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.5.dp,
                if (canSummonPartner) TcgColors.Violet.copy(alpha = 0.7f) else TcgColors.Line,
                RoundedCornerShape(14.dp)
            )
    ) {
        CardArtImage(
            cardId = partnerId,
            visual = look,
            contentDescription = partnerName,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 720
        )
        if (!canSummonPartner) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0B0A12)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔒 일상 사복 미해금", color = TcgColors.Danger, fontSize = TcgType.BODY.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "친밀도 ${AffinityRules.CASUAL_UNLOCK_BOND} · 지금 $bond/100",
                        color = TcgColors.Text,
                        fontSize = TcgType.LABEL.sp
                    )
                    Box(modifier = Modifier.width(160.dp)) {
                        StatBar(ratio = casualProgress, color = TcgColors.Danger, height = 7)
                    }
                }
            }
        } else if (state.hasSummonBuff) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Pill("+${state.summonBuffPercent}% · ${state.summonBuffBattles}회", TcgColors.Heal, filled = true)
            }
        }
    }

    // 소환 버튼 — 라벨·힌트 모두 짧게
    ActionTile(
        label = if (canSummonPartner) {
            "🌙 $partnerName 소환 (⚡ ${SummonEngine.ENERGY_COST})"
        } else {
            "🔒 소환 잠금 — 친밀도 ${AffinityRules.CASUAL_UNLOCK_BOND}"
        },
        hint = when {
            !canSummonPartner -> "출전으로 친밀도 $bond/100"
            state.gateEnergy >= SummonEngine.ENERGY_COST ->
                "❤ +${SummonEngine.BOND_GAIN} · 공격 +${SummonEngine.BUFF_PERCENT}% (${SummonEngine.BUFF_BATTLES}회)"
            else -> "⚡ 부족 — 승리 시 +${TcgGameViewModel.GATE_ENERGY_RECOVERY}"
        },
        accent = if (canSummonPartner) TcgColors.Violet else TcgColors.Muted,
        enabled = canSummonPartner && state.gateEnergy >= SummonEngine.ENERGY_COST
    ) { if (summonTarget != null) onSummon(summonTarget.id) }

    // 결과·기억의 잔향 — 한 줄 안내만 남긴다
    val line = state.summonMessage
        ?: if (state.summonCount > 0) "소환 ${state.summonCount}회" else null
    if (line != null) {
        Text(
            line,
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (memoryPartner != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(TcgColors.Abyss.copy(alpha = 0.14f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌙", fontSize = TcgType.ICON.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "${memoryPartner.name} · 파편 ${memoryPartner.memoryFragments}/${TcgCard.MEMORY_FRAGMENT_TARGET}",
                color = TcgColors.Text,
                fontSize = TcgType.LABEL.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ChipButton(
                label = "회상",
                selected = memoryPartner.memoryFragments >= TcgCard.MEMORY_FRAGMENT_TARGET,
                accent = TcgColors.Abyss
            ) { onBondMemoryPartner(memoryPartner.id) }
        }
    }
}

// ============================================================================
// 🤝 교감 탭 — 일상/데이트 원화 + 하트 게이지 + 액션 3버튼
// ============================================================================

@Composable
private fun ColumnScope.BondTab(
    state: TcgGameState,
    partnerName: String,
    partnerId: Int,
    bond: Int,
    look: VisualState,
    element: com.game.tcg.core.model.Element,
    onInteract: (RealWorldActivity) -> Unit
) {
    // 교감 직후 하트가 떠오르는 반응 연출
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
            .weight(1f)
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
            cardId = partnerId,
            visual = look,
            contentDescription = partnerName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 900
        )
        // 하단 가독성 스크림
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0x33000000), Color(0xF20B0A12)))
                )
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
                repeat(3) { Text("❤", fontSize = TcgType.TITLE.sp) }
            }
        }

        // 상단 배지 줄 — 모습·속성 (문장 없음)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Pill(look.displayName, TcgColors.Gold, filled = true)
            Pill(element.displayName, elementColor(element))
        }

        // 하단 — 이름 · 하트 게이지 · 파트너 한마디
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(partnerName, color = TcgColors.Text, fontSize = TcgType.SECTION.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RealWorldDecor.hearts(bond).forEach { filled ->
                        Text(
                            if (filled) "❤" else "♡",
                            color = if (filled) TcgColors.Danger else TcgColors.Muted.copy(alpha = 0.7f),
                            fontSize = TcgType.BODY_SMALL.sp
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    RealWorldDecor.heartLabel(bond),
                    color = TcgColors.Gold,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            StatBar(ratio = bond / 100f, color = TcgColors.Danger, height = 7)
            Text(
                RealWorldDecor.reactionLine(bond),
                color = TcgColors.Text.copy(alpha = 0.92f),
                fontSize = TcgType.LABEL.sp,
                lineHeight = TcgType.BODY_SMALL.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    // 오늘의 교감 — 액션 카드 3종 (아이콘 · 짧은 제목 · ❤ 수치)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RealWorldDecor.actionCards().forEach { card ->
            Box(modifier = Modifier.weight(1f)) {
                BondActionCard(
                    icon = card.icon,
                    title = card.title,
                    bondGain = card.bondGain,
                    accent = card.tone.color()
                ) { onInteract(card.activity) }
            }
        }
    }

    // 마지막 교감 결과 — 한 줄만
    val lastLog = RealWorldDecor.lastReaction(state.realWorldLog)
    if (lastLog != null) {
        Text(
            lastLog,
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 교감 액션 카드 — 아이콘 · 제목 · 호감도 배지 (설명문 없이 콤팩트) */
@Composable
private fun BondActionCard(
    icon: String,
    title: String,
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = TcgType.ICON.sp)
        }
        Text(
            title,
            color = TcgColors.Text,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Pill("❤ +$bondGain", accent)
    }
}

// ============================================================================
// 🎬 해금 탭 — 데이트 룩 해금 연출 + 단계 배지
// ============================================================================

@Composable
private fun ColumnScope.UnlockTab(
    partnerName: String,
    partnerId: Int,
    bond: Int
) {
    val dateOpen = bond >= SummonEngine.DATE_UNLOCK_BOND
    val dreadOpen = bond >= AffinityRules.CASUAL_UNLOCK_BOND
    val ultimateOpen = bond >= SummonEngine.ULTIMATE_UNLOCK_BOND

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.5.dp,
                if (dateOpen) TcgColors.Heal else TcgColors.Line,
                RoundedCornerShape(14.dp)
            )
    ) {
        CardArtImage(
            cardId = partnerId,
            visual = if (dateOpen) VisualState.ROMANTIC_DATE else VisualState.CASUAL_CUTE,
            contentDescription = "데이트 룩",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 720
        )
        if (!dateOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0B0A12)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎬 데이트 룩 잠금", color = TcgColors.Text, fontSize = TcgType.BODY.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "친밀도 ${SummonEngine.DATE_UNLOCK_BOND} · 지금 $bond/100",
                        color = TcgColors.Muted,
                        fontSize = TcgType.LABEL.sp
                    )
                    Box(modifier = Modifier.width(170.dp)) {
                        StatBar(
                            ratio = bond / SummonEngine.DATE_UNLOCK_BOND.toFloat(),
                            color = TcgColors.Muted,
                            height = 7
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Pill("🎬 $partnerName 데이트 룩", TcgColors.Heal, filled = true)
            }
        }
    }

    // 해금 단계 — 아이콘 배지 3개 (문장 없음)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        UnlockBadge("🤝 교감", AffinityRules.CASUAL_UNLOCK_BOND, bond, dreadOpen, Modifier.weight(1f))
        UnlockBadge("🎬 데이트", SummonEngine.DATE_UNLOCK_BOND, bond, dateOpen, Modifier.weight(1f))
        UnlockBadge("💞 서약", SummonEngine.ULTIMATE_UNLOCK_BOND, bond, ultimateOpen, Modifier.weight(1f))
    }
    Text(
        if (ultimateOpen) {
            "💞 서약 완성 — 페어 얼티밋 오의 (전투 데미지 +25%)"
        } else {
            "❤ $bond/100 · 다음 해금까지 ${(nextUnlockAt(bond) - bond).coerceAtLeast(0)}"
        },
        color = if (ultimateOpen) TcgColors.Gold else TcgColors.Muted,
        fontSize = TcgType.LABEL.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** 해금 단계 배지 1개 — 잠금이면 자물쇠, 열리면 체크 */
@Composable
private fun UnlockBadge(
    label: String,
    need: Int,
    bond: Int,
    open: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (open) TcgColors.Heal.copy(alpha = 0.16f) else TcgColors.SurfaceAlt)
            .border(1.dp, if (open) TcgColors.Heal.copy(alpha = 0.6f) else TcgColors.Line, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            if (open) "✓" else "🔒",
            color = if (open) TcgColors.Heal else TcgColors.Muted,
            fontSize = TcgType.BODY_SMALL.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = if (open) TcgColors.Text else TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            if (open) "해금" else "❤ $need",
            color = if (open) TcgColors.Heal else TcgColors.Muted,
            fontSize = TcgType.TILE_MARK.sp,
            maxLines = 1
        )
    }
}

/** 다음 해금 기준값 (지금 단계 다음 문턱) */
private fun nextUnlockAt(bond: Int): Int = when {
    bond < AffinityRules.CASUAL_UNLOCK_BOND -> AffinityRules.CASUAL_UNLOCK_BOND
    bond < SummonEngine.DATE_UNLOCK_BOND -> SummonEngine.DATE_UNLOCK_BOND
    bond < SummonEngine.ULTIMATE_UNLOCK_BOND -> SummonEngine.ULTIMATE_UNLOCK_BOND
    else -> SummonEngine.ULTIMATE_UNLOCK_BOND
}
