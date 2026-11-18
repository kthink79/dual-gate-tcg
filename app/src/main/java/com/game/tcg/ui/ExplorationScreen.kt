package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.model.TcgCard

/**
 * 좌측 서브메뉴 「🧭 탐색」 — 차원 틈새 탐색 · 기억 파편 수색 · 랜덤 조우.
 *
 * 2026-11-16 사용자 요청("텍스트 위주라 게임성이 부족하다") 반영:
 *  - **파견 파티 초상화 스트립**(전열 우선)
 *  - 탐색 구역을 **배경 일러스트 카드**로 — 소모 차원력 · 예상 보상 배지
 *  - 조우/전투/보물 발견 결과 카드에 **실제 캐릭터 일러스트와 보상 아이콘**
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

    ScreenFrame {
        // ── ① 파견 파티 + 차원 균열 에너지 ──
        SectionCard(
            title = "파견 파티",
            badge = "⚡ ${state.gateEnergy}/${state.gateEnergyMax}",
            accent = TcgColors.Mana
        ) {
            PartyPortraitStrip(state = state)
            StatBar(
                ratio = state.gateEnergy.toFloat() / state.gateEnergyMax,
                color = TcgColors.Mana,
                height = 9
            )
            Text(
                "탐색 1회당 에너지 ${ExplorationMode.RIFT.energyCost} 소모 · 이계 전투 승리 시 +${TcgGameViewModel.GATE_ENERGY_RECOVERY} 회복 " +
                    "(최대 ${state.gateEnergyMax}) · 누적 탐색 ${state.explorationCount}회",
                color = TcgColors.Muted,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }

        // ── ② 탐색 구역 3곳 ──
        SectionCard(title = "탐색 구역", badge = "3곳", accent = TcgColors.Violet) {
            ExplorationDecor.zones().forEach { zone ->
                ExplorationZoneCard(
                    zone = zone,
                    enabled = state.gateEnergy >= zone.mode.energyCost && staging == null,
                    onClick = { stagingMode = zone.mode }
                )
            }
            if (state.gateEnergy < ExplorationMode.RIFT.energyCost) {
                Text(
                    "차원 균열 에너지가 모두 소진되었습니다. 이계 전투에서 승리하면 회복됩니다.",
                    color = TcgColors.Danger,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // ── ③ 마지막 탐색 결과 ──
        val result = state.lastExploration
        if (result != null) {
            val artId = ExplorationDecor.resultArtCardId(
                result = result,
                fallbackCardId = state.lobbyHeroId
            )
            SectionCard(
                title = "${ExplorationDecor.resultIcon(result)} ${result.title}",
                badge = result.mode.displayName,
                accent = if (result.startsBattle) TcgColors.Danger else TcgColors.Heal
            ) {
                if (artId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        CardArtImage(
                            cardId = artId,
                            visual = ExplorationDecor.resultVisual(result),
                            contentDescription = ExplorationDecor.resultCompanionLabel(result) ?: "탐색 결과",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            targetWidthPx = 720
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0x1A000000), Color(0xDD0B0A12))))
                        )
                        val label = ExplorationDecor.resultCompanionLabel(result)
                        if (label != null) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Pill("👤 조우", TcgColors.Gold, filled = true)
                                Spacer(Modifier.width(6.dp))
                                Text(label, color = TcgColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
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
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                // 보상 배지
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ExplorationDecor.rewardBadges(result).forEach { badge ->
                        Pill(badge, TcgColors.Heal)
                    }
                    if (result.bondGained > 0) Pill("❤ +${result.bondGained}", TcgColors.Gold)
                    if (result.temptationGained > 0) Pill("🌫 +${result.temptationGained}", TcgColors.Danger)
                }
                if (result.startsBattle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill("조우", TcgColors.Danger, filled = true)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${result.enemyName} · HP ${result.enemyMaxHp} · 공격력 ${result.enemyAtk} " +
                                "(${result.enemyElement.displayName})",
                            color = TcgColors.Danger,
                            fontSize = 12.sp
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

        // ── ④ 기억 파편 현황 ──
        SectionCard(title = "기억의 잔향 · 파편 수색", badge = "기억 파편", accent = TcgColors.Abyss) {
            val targets = state.memoryResetCards
            if (targets.isEmpty()) {
                Text(
                    "지금은 기억이 지워진 동료가 없어 파편이 맺히지 않습니다. " +
                        "이계 전투에서 동료가 쓰러졌다가 환생 재획득하면 '기억이 지워진 동료'가 되어 " +
                        "파편 수색이 의미를 갖습니다.",
                    color = TcgColors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            } else {
                targets.forEach { card ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, TcgColors.Abyss.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        ) {
                            CardArtImage(
                                cardId = card.id,
                                visual = com.game.tcg.core.model.VisualState.CASUAL_CUTE,
                                contentDescription = card.name,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier.fillMaxSize(),
                                targetWidthPx = 160
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.name, color = TcgColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "기억 파편 ${card.memoryFragments}/${TcgCard.MEMORY_FRAGMENT_TARGET} · 친밀도 ${card.bondLevel}/100",
                                color = TcgColors.Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    StatBar(
                        ratio = card.memoryFragments.toFloat() / TcgCard.MEMORY_FRAGMENT_TARGET,
                        color = TcgColors.Abyss,
                        height = 6
                    )
                }
                ActionTile(
                    label = "🃏 캐릭터 관리에서 회상 이벤트 열기",
                    hint = "파편 3/3 달성 시 기억 복원 · 스탯 보너스",
                    accent = TcgColors.Violet,
                    onClick = onOpenCollection
                )
            }
        }

        // ── ⑤ 탐색 기록 ──
        SectionCard(title = "탐색 기록", accent = TcgColors.Muted) {
            LogBox(state.explorationLog, maxLines = 6)
            ActionTile(
                label = "🏙 현실 소환으로 이동",
                hint = "전투 출전으로 오른 친밀도로 일상·데이트 룩을 해금하세요",
                accent = TcgColors.Gold,
                onClick = onOpenRealWorld
            )
        }
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
    val steps = remember(mode) { ExplorationStaging.steps(mode) }
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
        ResourceArtImage(
            resId = backdropResId,
            contentDescription = mode.displayName,
            contentScale = ContentScale.Crop,
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
            // 일상복 캐릭터 스트립 — 차례로 슬라이드 인
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
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
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
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            StatBar(ratio = progress.value, color = tone, height = 9)
            Spacer(Modifier.height(6.dp))
            Text(
                "${(progress.value * 100).toInt()}%",
                color = TcgColors.Muted,
                fontSize = 11.sp
            )
        }
    }
}

/** 파견 파티 초상화 스트립 — 전열 3명이 앞에 온다 */
@Composable
private fun PartyPortraitStrip(state: TcgGameState) {
    val ids = ExplorationDecor.partyStrip(state)
    val frontIds = state.party.filter { it.isFrontRow }.sortedBy { it.slotIndex }.map { it.card.id }
    if (ids.isEmpty()) {
        Text("출전 덱이 비어 있습니다 — 「서약·덱」에서 편성하세요.", color = TcgColors.Muted, fontSize = 12.sp)
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(ids, key = { it }) { id ->
            val card = state.ownedCards.firstOrNull { it.id == id } ?: TcgGameViewModel.characterCard(id)
            val isFront = frontIds.contains(id)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(66.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            if (isFront) 1.5.dp else 1.dp,
                            if (isFront) TcgColors.Gold else TcgColors.Line,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    CardArtImage(
                        cardId = id,
                        visual = card.currentVisual,
                        contentDescription = card.name,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                        targetWidthPx = 200
                    )
                    if (isFront) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(TcgColors.Gold)
                                .padding(horizontal = 3.dp)
                        ) {
                            Text("전", color = Color(0xFF120E1C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    card.name.take(4),
                    color = TcgColors.Muted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** 탐색 구역 카드 — 배경 일러스트 + 소모 차원력 + 예상 보상 배지 */
@Composable
private fun ExplorationZoneCard(
    zone: ExplorationDecor.Zone,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, zone.tone.color().copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
    ) {
        ResourceArtImage(
            resId = zone.backdropResId,
            contentDescription = zone.mode.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 720
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xE60B0A12), Color(0x990B0A12), Color(0x330B0A12))
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(zone.mode.icon, fontSize = 15.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        zone.mode.displayName,
                        color = TcgColors.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(5.dp))
                    Pill("⚡ -${zone.mode.energyCost}", TcgColors.Mana)
                }
                Text(
                    zone.mode.description,
                    color = TcgColors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    zone.expectedRewards.forEach { reward ->
                        Pill(reward, zone.tone.color())
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(zone.tone.color().copy(alpha = if (enabled) 0.9f else 0.3f))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    if (enabled) "탐색 ▶" else "⚡ 부족",
                    color = Color(0xFF120E1C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
