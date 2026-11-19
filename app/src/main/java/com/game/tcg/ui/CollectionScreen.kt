package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 탭 3 — 🃏 도감 · 성장 (도감 / 강화 / 합성)
 *
 * 2026-11-19 사용자 요청(스크린샷 피드백) 전면 개편:
 *  ① '신규 15인 / 기존 155인 / 보스 6' 구분 버튼 **삭제**
 *  ② 5줄 필터 버튼을 **콤보박스(보기/속성/성급) + '보유만' 1줄**로 축약
 *  ③ 도감에서 캐릭터를 고르면 **상단 서브 탭 [도감][강화][합성]** 으로 바로 전환 —
 *     아래로 길게 스크롤하지 않아도 되고, 선택한 캐릭터가 그대로 이어진다
 *  ④ 목록은 칩 나열 대신 **2:3 썸네일 그리드**(태블릿에서는 열 수 자동 증가)
 *  ⑤ 설명문은 짧은 라벨·아이콘 위주로 정리
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CollectionScreen(
    state: TcgGameState,
    onSelectCard: (Int) -> Unit,
    onSelectVisual: (VisualState) -> Unit,
    onGaze: () -> Unit,
    onPurify: () -> Unit,
    onLimitBreak: () -> Unit,
    onCraftNew: () -> Unit,
    onMemoryRecall: (Int) -> Unit,
    onSetLobbyHero: (Int) -> Unit,
    onDeployToParty: (Int, Int) -> Unit,
    onUndeployFromParty: (Int) -> Unit,
    onVideoStart: () -> Unit = {},
    onVideoEnd: () -> Unit = {},
    // 2026-11-19: 도감 화면에서 바로 강화·합성을 실행한다 (탭 연동)
    onUpgrade: (Int) -> Unit = {},
    onCraft: (Int) -> Unit = {},
    onTranscend: (Int) -> Unit = {},
    onOpenShop: () -> Unit = {}
) {
    val selected: TcgCard = state.ownedCards.firstOrNull { it.id == state.selectedCardId }
        ?: TcgGameViewModel.characterCard(state.selectedCardId)
    val isOwned = state.ownedCards.any { it.id == state.selectedCardId }
    val profile = TcgGameViewModel.profileOf(state.selectedCardId)
    val isDemonLordCard = state.selectedCardId == 112

    // 🎬 등장 영상 — assets/character_videos/{번호}.mp4 (또는 _{n}) 가 있는 캐릭터만 버튼을 보여 준다
    val context = LocalContext.current
    var videoCardId by remember { mutableStateOf<Int?>(null) }
    var videoTick by remember { mutableIntStateOf(0) }
    val selectedVideoCount = remember(state.selectedCardId) {
        CharacterVideoRegistry.videoCount(context, state.selectedCardId)
    }

    ScreenFrame {
        // ── 머리: 제목 + 보유 수 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("도감·성장", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Pill("보유 ${state.ownedCards.size}", TcgColors.Violet)
            Spacer(Modifier.weight(1f))
            Pill("#${selected.id}", elementColor(selected.element))
        }

        // ── ① 콤보박스 1줄 (보기 기준 / 속성 / 성급 / 보유만) ──
        val allProfiles = TcgGameViewModel.profiles()
        val ownedIds = state.ownedCardIds
        var viewModeIndex by remember { mutableIntStateOf(0) }
        var elementIndex by remember { mutableIntStateOf(0) }
        var starIndex by remember { mutableIntStateOf(0) }
        var ownedOnly by remember { mutableStateOf(false) }
        val viewMode = CatalogViewMode.entries[viewModeIndex.coerceIn(0, CatalogViewMode.entries.size - 1)]
        val elementFilter = CollectionCatalog.elementAt(elementIndex)
        val starFilter = CollectionCatalog.starAt(starIndex)
        val starOf: (Int) -> Int = { id -> state.ownedCards.firstOrNull { it.id == id }?.starLevel ?: 1 }

        val shownProfiles = CollectionCatalog.orderForManagement(
            profiles = CollectionCatalog.applyFilters(
                profiles = allProfiles,
                element = elementFilter,
                star = starFilter,
                ownedOnly = ownedOnly,
                ownedIds = ownedIds,
                starOf = starOf
            ),
            ownedIds = ownedIds,
            starOf = starOf
        )
        val groups = CollectionCatalog.group(shownProfiles, viewMode, starOf)

        // 좁은 화면에서는 자연스럽게 한 줄 더 접히도록 FlowRow (넓은 화면은 1줄)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TcgDropdown(
                label = "보기",
                value = viewMode.label,
                options = CollectionCatalog.VIEW_MODE_OPTIONS,
                modifier = Modifier.weight(1.05f),
                accent = TcgColors.Violet
            ) { viewModeIndex = it }
            TcgDropdown(
                label = "속성",
                value = CollectionCatalog.ELEMENT_OPTIONS[elementIndex.coerceIn(0, CollectionCatalog.ELEMENT_OPTIONS.size - 1)],
                options = CollectionCatalog.ELEMENT_OPTIONS,
                modifier = Modifier.weight(1f),
                accent = TcgColors.Mana
            ) { elementIndex = it }
            TcgDropdown(
                label = "성급",
                value = CollectionCatalog.STAR_OPTIONS[starIndex.coerceIn(0, CollectionCatalog.STAR_OPTIONS.size - 1)],
                options = CollectionCatalog.STAR_OPTIONS,
                modifier = Modifier.weight(0.85f),
                accent = TcgColors.Gold
            ) { starIndex = it }
            TcgToggleChip(label = "보유", checked = ownedOnly) { ownedOnly = it }
        }

        // ── ② 서브 탭 (도감 / 강화 / 합성) ──
        var hubTab by remember { mutableStateOf(CharacterHubTab.CATALOG) }
        TcgTabBar(
            tabs = CharacterHubTab.entries.map { it.label },
            selectedIndex = hubTab.ordinal,
            accent = TcgColors.Violet,
            onSelect = { hubTab = CharacterHubTab.entries[it] }
        )

        // ── ③ 캐릭터 썸네일 그리드 (선택하면 세 탭이 모두 이 캐릭터를 쓴다) ──
        SectionCard(
            title = "캐릭터",
            badge = CollectionCatalog.countLabel(shownProfiles.size, allProfiles.size, state.ownedCards.size),
            accent = TcgColors.Abyss
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = ResponsiveLayout.gridColumns(maxWidth.value)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (maxWidth.value >= 600f) 300.dp else 248.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TcgColors.Background)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groups.forEach { group ->
                        if (viewMode != CatalogViewMode.NUMBER) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(group.tint).copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        group.title,
                                        color = Color(group.tint),
                                        fontSize = TcgType.LABEL.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text("${group.entries.size}", color = TcgColors.Muted, fontSize = TcgType.LABEL.sp)
                                }
                            }
                        }
                        items(group.entries, key = { it.cardId }) { item ->
                            val owned = item.cardId in ownedIds
                            val star = starOf(item.cardId)
                            // 카드 모서리의 전투력 = 카탈로그 ATK+DEF (없으면 0)
                            val record = com.game.tcg.core.model.CardCatalog.find(item.cardId)
                            val power = ((record?.atk ?: 0) + (record?.def ?: 0)) / 100
                            DuelCardTile(
                                cardId = item.cardId,
                                visual = VisualState.BATTLE,
                                element = item.element,
                                starLevel = star,
                                power = power,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(CardLayoutRules.TILE_ASPECT),
                                highlighted = item.cardId == state.selectedCardId,
                                dimmed = !owned,
                                statusBadge = if (owned) null else "미보유",
                                statusBadgeColor = TcgColors.Line,
                                targetWidthPx = 200,
                                onClick = { onSelectCard(item.cardId) }
                            )
                        }
                    }
                }
            }
        }

        // ── ④ 탭 내용 ──
        when (hubTab) {
            CharacterHubTab.CATALOG -> CatalogTab(
                state = state,
                selected = selected,
                isOwned = isOwned,
                profileTitle = profile?.title,
                isDemonLordCard = isDemonLordCard,
                onSelectVisual = onSelectVisual,
                onGaze = onGaze,
                onPurify = onPurify,
                onMemoryRecall = onMemoryRecall,
                onSetLobbyHero = onSetLobbyHero,
                onDeployToParty = onDeployToParty,
                onUndeployFromParty = onUndeployFromParty,
                videoCount = selectedVideoCount,
                onReplayVideo = { videoTick += 1; videoCardId = state.selectedCardId },
                onCraftNew = onCraftNew,
                onLimitBreak = onLimitBreak
            )
            CharacterHubTab.UPGRADE -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectedHeadline(state, selected, isOwned)
                UpgradeSection(state, selected, onUpgrade, onOpenShop)
            }
            CharacterHubTab.SYNTHESIS -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectedHeadline(state, selected, isOwned)
                SynthesisSection(state, selected, onCraft, onTranscend)
            }
        }
    }

    // ── 🎬 등장 영상 전체 화면 ──
    // (2026-11-19: 1:N — 목록 중 1편을 랜덤 재생, 「다시 보기」를 누를 때마다 다시 고른다)
    val playingCardId = videoCardId
    val playingPath = playingCardId?.let {
        remember(playingCardId, videoTick) { CharacterVideoRegistry.randomVideoPath(context, it) }
    }
    if (playingCardId != null && playingPath != null) {
        DisposableEffect(playingCardId, playingPath) {
            onVideoStart()
            onDispose { onVideoEnd() }
        }
        CharacterVideoOverlay(
            cardId = playingCardId,
            assetPath = playingPath,
            onClose = { videoCardId = null }
        )
    }
}

/** 강화·합성 탭 위에 붙는 선택 캐릭터 요약 — 어느 캐릭터를 키우는지 항상 보이게 */
@Composable
private fun SelectedHeadline(state: TcgGameState, card: TcgCard, isOwned: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DuelCardTile(
            cardId = card.id,
            visual = card.currentVisual,
            element = card.element,
            starLevel = card.starLevel,
            power = (card.atk + card.def) / 100,
            modifier = Modifier
                .width(58.dp)
                .aspectRatio(CardLayoutRules.TILE_ASPECT),
            targetWidthPx = 160
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "#${card.id} ${card.name}",
                color = TcgColors.Text,
                fontSize = TcgType.BODY.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "${"★".repeat(card.starLevel)} · Lv.${card.upgradeLevel + 1} · ATK ${card.atk} / DEF ${card.def}",
                color = TcgColors.Gold,
                fontSize = TcgType.LABEL.sp,
                maxLines = 1
            )
            if (!isOwned) {
                Text("미보유 — 도감 열람만 가능", color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp)
            } else if (state.party.any { it.card.id == card.id }) {
                Pill("출전 중", TcgColors.Heal)
            }
        }
    }
}

@Composable
private fun CatalogTab(
    state: TcgGameState,
    selected: TcgCard,
    isOwned: Boolean,
    profileTitle: String?,
    isDemonLordCard: Boolean,
    onSelectVisual: (VisualState) -> Unit,
    onGaze: () -> Unit,
    onPurify: () -> Unit,
    onMemoryRecall: (Int) -> Unit,
    onSetLobbyHero: (Int) -> Unit,
    onDeployToParty: (Int, Int) -> Unit,
    onUndeployFromParty: (Int) -> Unit,
    videoCount: Int,
    onReplayVideo: () -> Unit,
    onCraftNew: () -> Unit,
    onLimitBreak: () -> Unit
) {
    // ── 카드 상세 (2:3 원본 비율 유지) ──
    SectionCard(
        title = selected.name,
        badge = if (isOwned) "${selected.starLevel}★" else "미계약",
        accent = elementColor(selected.element)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
        ) {
            CardArtImage(
                cardId = selected.id,
                visual = state.selectedVisual,
                contentDescription = selected.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = 720
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD0B0A12))))
            )
            if (!isOwned) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xD90B0A12)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒 미보유", color = TcgColors.Text.copy(alpha = 0.8f), fontSize = TcgType.BODY_SMALL.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(state.selectedVisual.displayName, TcgColors.Violet, filled = true)
                Pill(selected.element.displayName, elementColor(selected.element))
                if (isDemonLordCard) Pill("마왕", TcgColors.Danger, filled = true)
            }
        }
        if (profileTitle != null) {
            Text(profileTitle, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = TcgType.BODY_SMALL.sp, fontWeight = FontWeight.SemiBold)
        }

        // 4종 비주얼
        val bond = state.selectedCardBond
        val casualOpen = AffinityRules.isCasualUnlocked(bond)
        val dateOpen = AffinityRules.isDateUnlocked(bond)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChipButton("① 전투", state.selectedVisual == VisualState.BATTLE) { onSelectVisual(VisualState.BATTLE) }
            ChipButton(
                if (casualOpen) "② 일상" else "② 일상 🔒",
                state.selectedVisual == VisualState.CASUAL_CUTE,
                if (casualOpen) TcgColors.Heal else TcgColors.Muted
            ) { onSelectVisual(VisualState.CASUAL_CUTE) }
            ChipButton(
                if (dateOpen) "③ 데이트" else "③ 데이트 🔒",
                state.selectedVisual == VisualState.ROMANTIC_DATE,
                if (dateOpen) TcgColors.Gold else TcgColors.Muted
            ) { onSelectVisual(VisualState.ROMANTIC_DATE) }
            ChipButton("④ 미니어처", state.selectedVisual == VisualState.MINIATURE, TcgColors.Abyss) {
                onSelectVisual(VisualState.MINIATURE)
            }
        }
        if (!casualOpen) {
            Text(
                "일상 ${AffinityRules.CASUAL_UNLOCK_BOND} · 데이트 ${AffinityRules.DATE_UNLOCK_BOND} (지금 $bond/100)",
                color = TcgColors.Muted,
                fontSize = TcgType.TILE_MARK.sp
            )
        }
        if (state.visualLockMessage != null) {
            Text(state.visualLockMessage, color = TcgColors.Danger, fontSize = TcgType.LABEL.sp)
        }

        // 스탯
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip("ATK", "${selected.atk}", TcgColors.Hp, Modifier.weight(1f))
            StatChip("DEF", "${selected.def}", TcgColors.Mana, Modifier.weight(1f))
            StatChip("성급", "${selected.starLevel}★", TcgColors.Gold, Modifier.weight(1f))
            StatChip("친밀도", "${selected.bondLevel}", TcgColors.Heal, Modifier.weight(1f))
        }
        Text("스킬 · ${selected.skillText}", color = TcgColors.Text.copy(alpha = 0.85f), fontSize = TcgType.LABEL.sp, lineHeight = 17.sp)

        // 영상 다시 보기 (1:N 이면 편수 표시)
        if (videoCount > 0) {
            ActionTile(
                label = if (videoCount > 1) "🎬 등장 영상 다시 보기 · ${videoCount}편 랜덤" else "🎬 등장 영상 다시 보기",
                hint = "카드팩 개봉 연출로 재생",
                accent = TcgColors.Gold,
                onClick = onReplayVideo
            )
        }

        // 전투 편성
        val currentSlot = state.party.firstOrNull { it.card.id == selected.id }
        Text(
            if (currentSlot != null) "편성 · ${DeckRules.slotLabel(currentSlot.slotIndex)} (HP ${currentSlot.hp}/${currentSlot.maxHp})"
            else "편성 · 미배치 (${state.party.size}/${state.unlockedDeckSize})",
            color = if (currentSlot != null) TcgColors.Heal else TcgColors.Muted,
            fontSize = TcgType.TILE_MARK.sp
        )
        (0 until state.unlockedDeckSize).chunked(4).forEach { rowSlots ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowSlots.forEach { slot ->
                    Box(modifier = Modifier.weight(1f)) {
                        ChipButton(
                            label = DeckRules.slotLabel(slot),
                            selected = currentSlot?.slotIndex == slot,
                            accent = if (DeckRules.isFrontSlot(slot)) TcgColors.Hp else TcgColors.Mana
                        ) { if (isOwned) onDeployToParty(selected.id, slot) }
                    }
                }
                repeat(4 - rowSlots.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (currentSlot != null) {
            ActionTile("↩ 편성에서 빼기", "HP는 전투 시작 시 최대치로", TcgColors.Muted) { onUndeployFromParty(selected.id) }
        }
        ActionTile(
            label = if (state.lobbyHeroId == selected.id) "★ 로비 대표 캐릭터" else "★ 로비 대표로 설정",
            hint = null,
            accent = TcgColors.Gold,
            enabled = isOwned && state.lobbyHeroId != selected.id,
            onClick = { onSetLobbyHero(selected.id) }
        )

        // 빠른 합성 (선택 캐릭터 기준)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                ActionTile(
                    label = "🧪 한계 돌파",
                    hint = if (isOwned && selected.starLevel < 5) "${selected.starLevel}★ → ${selected.starLevel + 1}★" else "5★ 최대",
                    accent = TcgColors.Violet,
                    enabled = isOwned && selected.starLevel < 5,
                    onClick = onLimitBreak
                )
            }
            Box(Modifier.weight(1f)) {
                ActionTile(
                    label = "✨ 신규 연성",
                    hint = "잡카드 ${state.junkCards} · ${state.gold}G",
                    accent = TcgColors.Gold,
                    enabled = state.junkCards >= 3 && state.gold >= 300,
                    onClick = onCraftNew
                )
            }
        }
        if (state.synthesisMessage != null) {
            Text(state.synthesisMessage, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = TcgType.LABEL.sp, lineHeight = 16.sp)
        }
        if (!isOwned) {
            Text("계약은 '2웨이 합성 → 신규 연성'에서", color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp)
        }
    }

    // ── 마왕 응시 기믹 ──
    if (isDemonLordCard) {
        SectionCard(title = "마왕 · 응시", badge = "유혹", accent = TcgColors.Danger) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("[홀림] ${state.temptationState.gazeTemptationStacks}/5 · 저주 ${state.temptationState.curseOfArchdemonStacks}/5", TcgColors.Danger)
                Spacer(Modifier.width(6.dp))
                Text("누적 ${state.temptationState.totalTemptationStacks}/10", color = TcgColors.Danger, fontSize = TcgType.LABEL.sp)
            }
            StatBar(ratio = state.temptationState.visualMistIntensity, color = TcgColors.Abyss, height = 8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    ActionTile("🔍 응시 (+1)", "줌 체험", TcgColors.Danger) { onGaze() }
                }
                Box(Modifier.weight(1f)) {
                    ActionTile("💧 정화 (${state.holyWater})", "매혹·저주 -2", TcgColors.Heal, enabled = state.holyWater > 0) { onPurify() }
                }
            }
            if (state.gazeMessage != null) {
                Text(state.gazeMessage, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = TcgType.LABEL.sp, lineHeight = 16.sp)
            }
            Text(state.combatPenalty.debuffDescription, color = TcgColors.Danger, fontSize = TcgType.TILE_MARK.sp)
        }
    }

    // ── 기억의 잔향 ──
    if (selected.isMemoryReset) {
        val progress = state.memoryResetCards.firstOrNull { it.id == selected.id }
        val fragments = progress?.memoryFragments ?: selected.memoryFragments
        val target = TcgCard.MEMORY_FRAGMENT_TARGET
        SectionCard(title = "기억이 지워진 동료", badge = "파편 $fragments/$target", accent = TcgColors.Abyss) {
            StatBar(ratio = fragments.toFloat() / target, color = TcgColors.Abyss, height = 10)
            ActionTile(
                label = "🌙 기억 회상",
                hint = if (fragments >= target) "스탯 보너스 + 기억 복원" else "파편 ${target - fragments}개 부족",
                accent = TcgColors.Gold,
                enabled = fragments >= target,
                onClick = { onMemoryRecall(selected.id) }
            )
            if (state.memoryRecallMessage != null) {
                Text(state.memoryRecallMessage, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = TcgType.LABEL.sp, lineHeight = 16.sp)
            }
        }
    }

    // ── 기억 회상 연출 ──
    val recallDialogue = state.memoryRecallDialogue
    if (recallDialogue != null) {
        SectionCard(title = "「${recallDialogue.eventTitle}」", badge = recallDialogue.subtitle, accent = TcgColors.Gold) {
            recallDialogue.lines.forEach { line ->
                Text(line, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = TcgType.LABEL.sp, lineHeight = 17.sp)
            }
            Text(recallDialogue.closingLine, color = TcgColors.Gold, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(label, color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp, maxLines = 1)
        Text(value, color = color, fontSize = TcgType.BODY_SMALL.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
