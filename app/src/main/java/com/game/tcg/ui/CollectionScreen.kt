package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 탭 3 — 🃏 도감 · 성장 (도감 / 강화 / 합성)
 *
 * 2026-11-26 사용자 요청 「메뉴 아이콘 중심·설명문 축소·글자 확대·종스크롤 최소화」:
 *  ① 상단 서브 탭을 **아이콘 중심 탭**(📖 도감 / ⚒ 강화 / 🧪 합성)으로 축소 — 긴 라벨·설명 없음
 *  ② 캐릭터를 고르면 아래로 길게 붙던 상세 카드를 **모달(전환 뷰)** 로 분리 —
 *     그리드가 화면 높이를 그대로 쓰고, 상세는 전체 화면 모달에서 스크롤한다
 *  ③ 그리드 아래에는 선택 캐릭터 **한 줄 스트립**(초상화·이름·상세 버튼)만 남긴다
 *  ④ 글씨는 [TcgType] 기준(제목 24 · 섹션 18 · 본문 15 · 배지 13) 이상만 쓴다
 *
 * (이전 2026-11-19 · 11-24 개편의 필터 콤보박스·큰 2:3 썸네일·원화 페이저·4종 뷰어는 유지)
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

    // 🔍 4종 전체화면 뷰어 (2026-11-24) — 원화를 누르면 열린다
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerBrowseIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 🗂 상세 모달 — 2026-11-26: 그리드 아래에 길게 붙던 상세 카드를 모달로 분리
    var detailOpen by remember { mutableStateOf(false) }

    // ── 필터 상태 (콤보박스 1줄) ──
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

    // ── 서브 탭 (📖 도감 / ⚒ 강화 / 🧪 합성) ──
    var hubTab by remember { mutableStateOf(CharacterHubTab.CATALOG) }

    CompactFrame {
        // ── 머리 1줄: 제목 + 보유 수 + 선택 번호 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("도감·성장", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Pill("보유 ${state.ownedCards.size}", TcgColors.Violet)
            Spacer(Modifier.weight(1f))
            Pill("#${selected.id}", elementColor(selected.element))
        }

        // ── ① 아이콘 중심 서브 탭 ──
        TcgIconTabBar(
            tabs = listOf("📖" to "도감", "⚒" to "강화", "🧪" to "합성"),
            selectedIndex = hubTab.ordinal,
            accent = TcgColors.Violet
        ) { hubTab = CharacterHubTab.entries[it] }

        // ── ② 필터 1줄 (보기 / 속성 / 성급 / 보유) ──
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

        when (hubTab) {
            // ── 📖 도감: 그리드가 남는 높이를 모두 쓴다 (상세는 모달) ──
            CharacterHubTab.CATALOG -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TcgColors.Background)
                        .border(1.dp, TcgColors.Line, RoundedCornerShape(14.dp))
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = ResponsiveLayout.gridColumns(maxWidth.value, minTileWidthDp = 128f)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        targetWidthPx = 360,
                                        onClick = {
                                            onSelectCard(item.cardId)
                                            detailOpen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 선택 캐릭터 한 줄 스트립 — 상세는 모달로
                SelectedStrip(
                    state = state,
                    card = selected,
                    isOwned = isOwned,
                    countLabel = CollectionCatalog.countLabel(shownProfiles.size, allProfiles.size, state.ownedCards.size),
                    onOpenDetail = { detailOpen = true }
                )
            }

            // ── ⚒ 강화 / 🧪 합성: 스크롤 본문 ──
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectedHeadline(state, selected, isOwned)
                if (hubTab == CharacterHubTab.UPGRADE) {
                    UpgradeSection(state, selected, onUpgrade, onOpenShop)
                } else {
                    SynthesisSection(state, selected, onCraft, onTranscend)
                }
            }
        }
    }

    // ── 🗂 캐릭터 상세 모달 (2026-11-26) ──
    if (detailOpen) {
        CatalogDetailDialog(
            onClose = { detailOpen = false },
            state = state,
            selected = selected,
            isOwned = isOwned,
            profileTitle = profile?.title,
            isDemonLordCard = isDemonLordCard,
            browseIds = shownProfiles.map { it.cardId },
            onSelectCard = onSelectCard,
            onOpenViewer = {
                viewerBrowseIds = shownProfiles.map { it.cardId }
                viewerOpen = true
            },
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

    // ── 🔍 4종 전체화면 뷰어 (2026-11-24) ──
    if (viewerOpen) {
        CharacterArtViewer(
            cardId = state.selectedCardId,
            cardName = selected.name,
            bondLevel = state.selectedCardBond,
            initialVisual = state.selectedVisual,
            browseIds = viewerBrowseIds.ifEmpty { state.ownedCardIds.toList() },
            onSelectCard = onSelectCard,
            onClose = { viewerOpen = false }
        )
    }
}

/**
 * 캐릭터 상세 모달 — 예전에는 그리드 아래로 길게 붙던 상세 카드를 전환 뷰로 옮겼다.
 * 그리드가 화면 전체 높이를 쓰고, 상세는 여기서만 스크롤한다.
 */
@Composable
private fun CatalogDetailDialog(
    onClose: () -> Unit,
    state: TcgGameState,
    selected: TcgCard,
    isOwned: Boolean,
    profileTitle: String?,
    isDemonLordCard: Boolean,
    browseIds: List<Int>,
    onSelectCard: (Int) -> Unit,
    onOpenViewer: () -> Unit,
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
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 2026-11-28 v0.6.1 — 가로 모드 보완
        //  · 다이얼로그는 프레임 밖(전체 화면)이라 태블릿 가로에서 원화가 `fillMaxWidth` 로
        //    폭을 다 먹어 높이 1900dp 짜리 거대한 카드가 됐다(스크롤을 한참 내려야 정보가 보인다).
        //  · 화면이 가로로 넓으면 원화를 **화면 높이에 맞춰** 줄여 한눈에 들어오게 한다.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(TcgColors.Background.copy(alpha = 0.98f))
        ) {
            val wide = ResponsiveLayout.isLandscape(maxWidth.value, maxHeight.value)
            val artWidth = if (wide) {
                minOf(
                    (maxHeight * 0.58f) * ResponsiveLayout.ART_ASPECT,
                    maxWidth * 0.46f
                )
            } else {
                null
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 모달 머리 — 이름·번호 + 닫기 (설명문 없음)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${selected.id} ${selected.name}",
                        color = TcgColors.Text,
                        fontSize = TcgType.TITLE.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Pill(if (isOwned) "${selected.starLevel}★" else "미보유", elementColor(selected.element))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(TcgColors.SurfaceAlt)
                            .border(1.dp, TcgColors.Line, RoundedCornerShape(50))
                            .clickable { onClose() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("✕ 닫기", color = TcgColors.Text, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CatalogTab(
                        state = state,
                        selected = selected,
                        isOwned = isOwned,
                        profileTitle = profileTitle,
                        isDemonLordCard = isDemonLordCard,
                        browseIds = browseIds,
                        onSelectCard = onSelectCard,
                        onOpenViewer = onOpenViewer,
                        onSelectVisual = onSelectVisual,
                        onGaze = onGaze,
                        onPurify = onPurify,
                        onMemoryRecall = onMemoryRecall,
                        onSetLobbyHero = onSetLobbyHero,
                        onDeployToParty = onDeployToParty,
                        onUndeployFromParty = onUndeployFromParty,
                        videoCount = videoCount,
                        onReplayVideo = onReplayVideo,
                        onCraftNew = onCraftNew,
                        onLimitBreak = onLimitBreak,
                        artWidth = artWidth
                    )
                }
            }
        }
    }
}

/** 그리드 아래 한 줄 — 선택 캐릭터 초상화 · 이름 · 상세 버튼 */
@Composable
private fun SelectedStrip(
    state: TcgGameState,
    card: TcgCard,
    isOwned: Boolean,
    countLabel: String,
    onOpenDetail: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TcgColors.SurfaceAlt)
            .clickable { onOpenDetail() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .aspectRatio(CardLayoutRules.TILE_ASPECT)
                .clip(RoundedCornerShape(7.dp))
                .border(1.dp, elementColor(card.element).copy(alpha = 0.7f), RoundedCornerShape(7.dp))
        ) {
            CardArtImage(
                cardId = card.id,
                visual = card.currentVisual,
                contentDescription = card.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = 160
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "#${card.id} ${card.name}",
                color = TcgColors.Text,
                fontSize = TcgType.BODY_SMALL.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$countLabel · ${if (isOwned) "❤ ${card.bondLevel}" else "미보유"}",
                color = TcgColors.Muted,
                fontSize = TcgType.LABEL.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Pill("상세 ▸", TcgColors.Gold, filled = true)
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
    browseIds: List<Int>,
    onSelectCard: (Int) -> Unit,
    onOpenViewer: () -> Unit,
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
    onLimitBreak: () -> Unit,
    /** 가로 모드에서 원화가 화면을 넘지 않는 폭 — null 이면 예전처럼 폭을 가득 채운다 */
    artWidth: androidx.compose.ui.unit.Dp? = null
) {
    // ── 카드 상세 (2:3 원본 비율 유지) ──
    SectionCard(
        title = selected.name,
        badge = if (isOwned) "${selected.starLevel}★" else "미계약",
        accent = elementColor(selected.element)
    ) {
        Box(
            modifier = (if (artWidth != null) {
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(artWidth)
                    .aspectRatio(2f / 3f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            })
                .clip(RoundedCornerShape(14.dp))
        ) {
            // 2026-11-24 — 좌우 스와이프로 보유 캐릭터를 연속 탐색, 누르면 4종 전체화면 뷰어
            CharacterCardPager(
                browseIds = browseIds,
                selectedId = selected.id,
                visual = state.selectedVisual,
                onSelectCard = onSelectCard,
                onOpenViewer = { onOpenViewer() },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD0B0A12))))
            )

            // ‹ › 빠른 넘기기 (좌우 스와이프와 같은 동작)
            val browseIndex = browseIds.indexOf(selected.id)
            if (browseIndex > 0) {
                CircleNavButton(
                    label = "‹",
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)
                ) { onSelectCard(browseIds[browseIndex - 1]) }
            }
            if (browseIndex >= 0 && browseIndex < browseIds.lastIndex) {
                CircleNavButton(
                    label = "›",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
                ) { onSelectCard(browseIds[browseIndex + 1]) }
            }

            // 🔍 4종 전체화면 감상
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC120E1C))
                    .border(1.dp, TcgColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(50))
                    .clickable { onOpenViewer() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("🔍 크게 보기", color = TcgColors.Gold, fontSize = TcgType.TILE_MARK.sp, fontWeight = FontWeight.Bold)
            }

            // 현재 위치 표시 (몇 번째 / 전체)
            if (browseIds.isNotEmpty() && browseIndex >= 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC120E1C))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${browseIndex + 1} / ${browseIds.size}",
                        color = TcgColors.Muted,
                        fontSize = TcgType.TILE_MARK.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
        // 2026-11-24: 4종을 각각 크게 — 원화 탭과 같은 동작의 안내 버튼
        ActionTile(
            label = "🔍 ${visualShortLabel(state.selectedVisual)} 원화 크게 보기 · 4종 감상",
            hint = "핀치 확대 · 더블 탭 · 드래그 이동",
            accent = TcgColors.Gold,
            onClick = onOpenViewer
        )

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

/**
 * 상세 원화 페이저 — 2026-11-24 사용자 요청 "보유 캐릭터들을 다 둘러 볼때 편하게".
 *
 * 좌우 스와이프로 다음/이전 캐릭터의 같은 복장을 바로 보여 주고, 페이지가 바뀌면
 * 선택 캐릭터를 갱신한다(도감·강화·합성 탭이 모두 같은 캐릭터를 쓴다).
 * 원화를 한 번 누르면 4종 전체화면 뷰어가 열린다.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CharacterCardPager(
    browseIds: List<Int>,
    selectedId: Int,
    visual: VisualState,
    onSelectCard: (Int) -> Unit,
    onOpenViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (browseIds.isEmpty()) {
        CardArtImage(
            cardId = selectedId,
            visual = visual,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
            targetWidthPx = 720
        )
        return
    }
    val startIndex = browseIds.indexOf(selectedId).coerceAtLeast(0)
    // 필터가 바뀌어도 페이지 수가 최신 목록을 따르도록 최신 값을 참조한다
    val latestIds by rememberUpdatedState(browseIds)
    val pagerState = rememberPagerState(initialPage = startIndex) { latestIds.size }

    LaunchedEffect(selectedId, browseIds.size) {
        val target = browseIds.indexOf(selectedId)
        if (target >= 0 && pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    LaunchedEffect(pagerState.currentPage, browseIds.size) {
        browseIds.getOrNull(pagerState.currentPage)?.let { id ->
            if (id != selectedId) onSelectCard(id)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageSpacing = 10.dp
    ) { page ->
        val id = browseIds.getOrNull(page) ?: selectedId
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onOpenViewer() }
        ) {
            CardArtImage(
                cardId = id,
                visual = visual,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = 720
            )
        }
    }
}

/** 원화 위에 얹는 ‹ › 원형 넘기기 버튼 */
@Composable
private fun CircleNavButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .width(38.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC120E1C))
            .border(1.dp, TcgColors.Violet.copy(alpha = 0.7f), RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TcgColors.Text, fontSize = TcgType.SECTION.sp, fontWeight = FontWeight.Bold)
    }
}
