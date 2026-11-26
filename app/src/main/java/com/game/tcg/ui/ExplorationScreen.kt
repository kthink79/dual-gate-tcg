package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationResult
import com.game.tcg.core.model.TcgCard

/**
 * 좌측 서브메뉴 「🧭 탐색」 — 차원 틈새 탐색 · 기억 파편 수색 · 랜덤 조우.
 *
 * 2026-11-26 사용자 요청 「메뉴 아이콘 중심·설명문 축소·글자 확대·종스크롤 최소화」:
 *  - 파견 파티를 **초상화 1줄**로 압축(에너지는 머리 배지로)
 *  - 탐색 구역 3곳의 2~3줄 설명문을 걷어내고 **구역 아이콘 · 보상 아이콘 · 탐색 버튼**만 배치해
 *    세 칸을 나란히 — 무스크롤 한 화면
 *  - 결과 서사는 화면 아래로 쌓지 않고 **결과 오버레이**로 띄운다(닫으면 사라진다)
 *  - 파편 현황·기록은 아이콘 탭(🧭 탐색 / 🌙 파편 / 📜 기록)으로 분리
 *  - 글씨는 [TcgType] 기준(제목 24 · 섹션 18 · 본문 15 · 배지 13) 이상만 쓴다
 */
@Composable
fun ExplorationScreen(
    state: TcgGameState,
    onExplore: (ExplorationMode) -> Unit,
    onOpenBattle: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenRealWorld: () -> Unit
) {
    // ── 탐색 진행 연출 상태 (2026-11-18) ──
    var stagingMode by remember { mutableStateOf<ExplorationMode?>(null) }
    val staging = stagingMode
    val companions = remember(state.party, state.lobbyHeroId, staging) {
        ExplorationStaging.companionIds(
            party = state.party.sortedBy { it.slotIndex }.map { it.card.id to it.isFrontRow },
            fallbackCardId = state.lobbyHeroId
        )
    }

    // 0 탐색 · 1 파편 · 2 기록
    var tab by remember { mutableIntStateOf(0) }

    // ── 결과 오버레이 — 새 결과가 도착했을 때만 연다 ──
    var shownResult by remember { mutableStateOf(state.lastExploration) }
    var resultOpen by remember { mutableStateOf(false) }
    val lastResult = state.lastExploration
    LaunchedEffect(lastResult) {
        if (lastResult != null && lastResult !== shownResult) {
            shownResult = lastResult
            resultOpen = true
        }
    }

    CompactFrame {
        // ── 머리 1줄: 제목 + 차원 에너지 + 누적 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("탐색", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Pill("⚡ ${state.gateEnergy}/${state.gateEnergyMax}", TcgColors.Mana)
            Spacer(Modifier.weight(1f))
            Pill("누적 ${state.explorationCount}회", TcgColors.Violet)
        }

        TcgIconTabBar(
            tabs = listOf("🧭" to "탐색", "🌙" to "파편", "📜" to "기록"),
            selectedIndex = tab,
            accent = TcgColors.Violet
        ) { tab = it }

        when (tab) {
            // ── 🧭 탐색: 파티 1줄 + 구역 3칸 (무스크롤) ──
            0 -> {
                PartyPortraitStrip(state = state)
                StatBar(
                    ratio = state.gateEnergy.toFloat() / state.gateEnergyMax,
                    color = TcgColors.Mana,
                    height = 6
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExplorationDecor.zones().forEach { zone ->
                        Box(modifier = Modifier.weight(1f)) {
                            ExplorationZoneTile(
                                zone = zone,
                                enabled = state.gateEnergy >= zone.mode.energyCost && staging == null,
                                onClick = { stagingMode = zone.mode }
                            )
                        }
                    }
                }
                if (state.gateEnergy < ExplorationMode.RIFT.energyCost) {
                    Text(
                        "⚡ 소진 — 이계 전투 승리로 회복",
                        color = TcgColors.Danger,
                        fontSize = TcgType.LABEL.sp,
                        maxLines = 1
                    )
                }
            }

            // ── 🌙 파편: 기억이 지워진 동료 (필요할 때만 스크롤) ──
            1 -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val targets = state.memoryResetCards
                if (targets.isEmpty()) {
                    Text(
                        "🌙 지금은 기억이 지워진 동료가 없습니다.",
                        color = TcgColors.Muted,
                        fontSize = TcgType.BODY_SMALL.sp
                    )
                } else {
                    targets.forEach { card ->
                        MemoryFragmentRow(card = card)
                    }
                    ActionTile(
                        label = "🌙 회상 이벤트 열기",
                        hint = "파편 3/3 달성 +",
                        accent = TcgColors.Abyss,
                        onClick = onOpenCollection
                    )
                }
            }

            // ── 📜 기록 ──
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LogBox(state.explorationLog, maxLines = 12)
                }
                ActionTile(
                    label = "🏙 현실 소환으로",
                    hint = "일상·데이트 룩 해금",
                    accent = TcgColors.Gold,
                    onClick = onOpenRealWorld
                )
            }
        }
    }

    // ── 새 결과 오버레이 (서사를 화면 아래로 쌓지 않는다) ──
    val result = lastResult
    if (resultOpen && result != null) {
        ExplorationResultOverlay(
            state = state,
            result = result,
            onOpenBattle = onOpenBattle,
            onClose = { resultOpen = false }
        )
    }

    // ── 탐색 진행 연출 오버레이 — 일상복 캐릭터가 화면에 서고 게이지가 차오른다 ──
    if (staging != null) {
        ExplorationProgressOverlay(
            mode = staging,
            companionIds = companions,
            backdropResId = ExplorationDecor.zoneOf(staging).backdropResId,
            tone = elementColor(ExploreStageTone(staging)),
            onFinished = {
                stagingMode = null
                onExplore(staging)
            }
        )
    }
}

/** 구역별 진행 연출 색조 */
private fun ExploreStageTone(mode: ExplorationMode) = when (mode) {
    ExplorationMode.RIFT -> com.game.tcg.core.model.Element.DARK
    ExplorationMode.FRAGMENT -> com.game.tcg.core.model.Element.ICE
    ExplorationMode.ENCOUNTER -> com.game.tcg.core.model.Element.LIGHT
}

// ============================================================================
// 🧭 탐색 구역 타일 — 아이콘 · 보상 아이콘 · 탐색 버튼만 (설명문 없음)
// ============================================================================

@Composable
private fun ExplorationZoneTile(
    zone: ExplorationDecor.Zone,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tone = zone.tone.color()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, tone.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
    ) {
        // 배경 일러스트 위에 어두운 스크림 — 글씨 대비 확보
        ResourceArtImage(
            resId = zone.backdropResId,
            contentDescription = zone.mode.displayName,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 480
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xD90B0A12), Color(0x990B0A12), Color(0xF20B0A12)))
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(zone.mode.icon, fontSize = TcgType.TITLE.sp)
            Text(
                zone.mode.displayName,
                color = TcgColors.Text,
                fontSize = TcgType.BODY_SMALL.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Pill("⚡ -${zone.mode.energyCost}", TcgColors.Mana)
            Spacer(Modifier.weight(1f))
            // 보상은 아이콘만 (설명 없음)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                zone.expectedRewards.forEach { reward ->
                    Text(
                        reward.substringBefore(" "),
                        fontSize = TcgType.BODY_SMALL.sp,
                        maxLines = 1
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (enabled) tone.copy(alpha = 0.92f) else TcgColors.SurfaceAlt)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (enabled) "탐색 ▶" else "⚡ 부족",
                    color = if (enabled) Color(0xFF120E1C) else TcgColors.Muted,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================================================
// 🎁 탐색 결과 오버레이
// ============================================================================

@Composable
private fun ExplorationResultOverlay(
    state: TcgGameState,
    result: ExplorationResult,
    onOpenBattle: () -> Unit,
    onClose: () -> Unit
) {
    val artId = ExplorationDecor.resultArtCardId(result = result, fallbackCardId = state.lobbyHeroId)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background.copy(alpha = 0.98f))
            // 아래 화면으로 탭이 새지 않게
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${ExplorationDecor.resultIcon(result)} ${result.title}",
                    color = TcgColors.Text,
                    fontSize = TcgType.TITLE.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Pill(result.mode.displayName, if (result.startsBattle) TcgColors.Danger else TcgColors.Heal)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(TcgColors.SurfaceAlt)
                        .border(1.dp, TcgColors.Line, RoundedCornerShape(50))
                        .clickable { onClose() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("✕", color = TcgColors.Text, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (artId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.5.dp,
                            if (result.startsBattle) TcgColors.Danger else TcgColors.Heal,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    CardArtImage(
                        cardId = artId,
                        visual = ExplorationDecor.resultVisual(result),
                        contentDescription = ExplorationDecor.resultCompanionLabel(result) ?: "탐색 결과",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        targetWidthPx = 900
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color(0x33000000), Color(0xF20B0A12)))
                            )
                    )
                    val label = ExplorationDecor.resultCompanionLabel(result)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (label != null) {
                            Pill("👤 $label", TcgColors.Gold, filled = true)
                        }
                        Text(
                            result.narrative,
                            color = TcgColors.Text.copy(alpha = 0.94f),
                            fontSize = TcgType.BODY_SMALL.sp,
                            lineHeight = TcgType.BODY.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    result.narrative,
                    color = TcgColors.Text.copy(alpha = 0.94f),
                    fontSize = TcgType.BODY_SMALL.sp,
                    lineHeight = TcgType.BODY.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                )
            }

            // 보상 배지 한 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ExplorationDecor.rewardBadges(result).take(4).forEach { badge ->
                    Pill(badge, TcgColors.Heal)
                }
                if (result.bondGained > 0) Pill("❤ +${result.bondGained}", TcgColors.Gold)
                if (result.temptationGained > 0) Pill("🌫 +${result.temptationGained}", TcgColors.Danger)
            }

            if (result.startsBattle) {
                ActionTile(
                    label = "⚔ 교전 — ${result.enemyName}",
                    hint = "HP ${result.enemyMaxHp} · ATK ${result.enemyAtk} · ${result.enemyElement.displayName}",
                    accent = TcgColors.Danger,
                    onClick = {
                        onClose()
                        onOpenBattle()
                    }
                )
            } else {
                ActionTile(
                    label = "✓ 확인",
                    hint = null,
                    accent = TcgColors.Heal,
                    onClick = onClose
                )
            }
        }
    }
}

/** 기억 파편 1명 — 초상화 + 파편 게이지 (설명문 없음) */
@Composable
private fun MemoryFragmentRow(card: TcgCard) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .aspectRatio(CardLayoutRules.TILE_ASPECT)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TcgColors.Abyss.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            ) {
                CardArtImage(
                    cardId = card.id,
                    visual = com.game.tcg.core.model.VisualState.CASUAL_CUTE,
                    contentDescription = card.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 160
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(card.name, color = TcgColors.Text, fontSize = TcgType.BODY_SMALL.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "🌙 ${card.memoryFragments}/${TcgCard.MEMORY_FRAGMENT_TARGET} · ❤ ${card.bondLevel}",
                    color = TcgColors.Muted,
                    fontSize = TcgType.LABEL.sp,
                    maxLines = 1
                )
            }
        }
        StatBar(
            ratio = card.memoryFragments.toFloat() / TcgCard.MEMORY_FRAGMENT_TARGET,
            color = TcgColors.Abyss,
            height = 6
        )
    }
}

// ============================================================================
// 연출 오버레이 · 파티 스트립
// ============================================================================

/**
 * 탐색 진행 연출 — 배경 장면 위로 일상복 동료가 차례로 들어서고,
 * 진행 게이지와 단계 문구가 함께 흘러간다.
 */
@Composable
private fun ExplorationProgressOverlay(
    mode: ExplorationMode,
    companionIds: List<Int>,
    backdropResId: Int,
    tone: Color,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    var revealed by remember { mutableIntStateOf(0) }
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(mode) {
        revealed = 1
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ExplorationStaging.DURATION_MILLIS, easing = LinearEasing)
        )
        finished()
    }
    // 단계별로 동료를 한 명씩 세운다 (일상복 차림)
    LaunchedEffect(progress.value) {
        val want = (1 + (progress.value * companionIds.size).toInt()).coerceAtMost(companionIds.size)
        if (want > revealed) revealed = want
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background.copy(alpha = 0.96f))
            // 연출 중 아래 화면(스크롤·버튼)으로 탭이 새지 않도록 입력을 소비한다
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        AmbientResourceBackdrop(
            resId = backdropResId,
            contentDescription = mode.displayName,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 720
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC0B0A12), Color(0x550B0A12), Color(0xF20B0A12))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Pill("${mode.icon} ${mode.displayName} 탐색 중", tone, filled = true)
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                companionIds.forEachIndexed { index, id ->
                    val show = index < revealed
                    val offset by animateDpAsState(
                        targetValue = if (show) 0.dp else 28.dp,
                        animationSpec = tween(220),
                        label = "explore_slide_$id"
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (show) 1f else 0f,
                        animationSpec = tween(220),
                        label = "explore_alpha_$id"
                    )
                    Box(
                        modifier = Modifier
                            .width(if (index == 0) 116.dp else 92.dp)
                            .aspectRatio(2f / 3f)
                            .offset(x = offset, y = 0.dp)
                            .alpha(alpha)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, tone.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    ) {
                        CardArtImage(
                            cardId = id,
                            visual = ExplorationStaging.stagingVisual,
                            contentDescription = TcgGameViewModel.profileOf(id)?.displayName ?: "동료",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            targetWidthPx = 320
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                ExplorationStaging.messageAt(mode, progress.value),
                color = TcgColors.Text,
                fontSize = TcgType.BODY_SMALL.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = TcgType.BODY.sp
            )
            Spacer(Modifier.height(8.dp))
            StatBar(ratio = progress.value, color = tone, height = 9)
            Spacer(Modifier.height(6.dp))
            Text(
                "${(progress.value * 100).toInt()}%",
                color = TcgColors.Muted,
                fontSize = TcgType.LABEL.sp
            )
        }
    }
}

/** 파견 파티 초상화 1줄 — 전열 3명이 앞에 온다 */
@Composable
private fun PartyPortraitStrip(state: TcgGameState) {
    val ids = ExplorationDecor.partyStrip(state)
    val frontIds = state.party.filter { it.isFrontRow }.sortedBy { it.slotIndex }.map { it.card.id }
    if (ids.isEmpty()) {
        Text(
            "출전 덱이 비어 있습니다 — 「서약·덱」에서 편성하세요.",
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp
        )
        return
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(ids, key = { it }) { id ->
            val card = state.ownedCards.firstOrNull { it.id == id } ?: TcgGameViewModel.characterCard(id)
            val isFront = frontIds.contains(id)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight(0.82f)
                        .clip(RoundedCornerShape(9.dp))
                        .border(
                            if (isFront) 1.5.dp else 1.dp,
                            if (isFront) TcgColors.Gold else TcgColors.Line,
                            RoundedCornerShape(9.dp)
                        )
                ) {
                    CardArtImage(
                        cardId = id,
                        visual = card.currentVisual,
                        contentDescription = card.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        targetWidthPx = 180
                    )
                    if (isFront) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(TcgColors.Gold)
                                .padding(horizontal = 3.dp)
                        ) {
                            Text("전", color = Color(0xFF120E1C), fontSize = TcgType.TILE_MARK.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    card.name.take(4),
                    color = TcgColors.Muted,
                    fontSize = TcgType.TILE_MARK.sp,
                    maxLines = 1
                )
            }
        }
    }
}
