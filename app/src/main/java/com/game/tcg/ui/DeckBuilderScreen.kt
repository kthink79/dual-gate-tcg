package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.DeckAutoBuilder
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 🃏 덱 편성 (DECK) — 2026-11-16 사용자 요청(실기기 스크린샷 25048) 개편.
 *
 * 이전 화면은
 *  · 덱 슬롯이 납작한 가로 직사각형(높이 78dp)에 `ContentScale.Crop` 으로 채워져
 *    캐릭터 얼굴·몸통 일부만 잘려 보였고,
 *  · 덱 12칸 + 컬렉션 63명이 한 화면에 이어져 스크롤이 길고 카드가 작았다.
 *
 * 이제
 *  - **카드 타일은 2:3 세로 비율**에 아트를 Fit 으로 꼭 맞춘다([CardLayoutRules])
 *  - 탭(덱 편성 / 컬렉션 / 서약·정화)으로 화면을 가른다
 *  - 덱은 3열 × 최대 4행 = **한 화면 최대 12칸**, 컬렉션은 3열 2:3 카드로 세로 스크롤
 */
@Composable
fun DeckBuilderScreen(
    state: TcgGameState,
    onBack: () -> Unit,
    onAutoEquip: () -> Unit,
    onUnloadAll: () -> Unit,
    onToggleCard: (Int) -> Unit,
    onUnloadSlot: (Int) -> Unit,
    onTrain: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenBattle: () -> Unit,
    onGaze: () -> Unit,
    onPurify: () -> Unit,
    onSetLobbyHero: (Int) -> Unit,
    onOpenDeckCard: (Int) -> Unit = {},
    onCloseDeckSheet: () -> Unit = {},
    onSelectDeckOutfit: (VisualState) -> Unit = {},
    onOpenGrowthFromDeck: (Int) -> Unit = {}
) {
    var tabIndex by remember { mutableIntStateOf(DECK_TAB_BUILD) }
    var elementFilter by remember { mutableStateOf<Element?>(null) }
    var onlyNewWave by remember { mutableStateOf(false) }
    var sortByPower by remember { mutableStateOf(true) }
    var filterOpen by remember { mutableStateOf(false) }

    val collection = remember(state.ownedCards, elementFilter, onlyNewWave, sortByPower) {
        state.ownedCards
            .filter { card -> elementFilter == null || card.element == elementFilter }
            .filter { card -> !onlyNewWave || CardCatalog.isNewWave(card.id) }
            .let { list ->
                if (sortByPower) {
                    list.sortedWith(
                        compareByDescending<TcgCard> { DeckAutoBuilder.powerOf(it) }
                            .thenByDescending { it.atk }
                            .thenBy { it.id }
                    )
                } else {
                    list.sortedBy { it.id }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 보라 · 다크 바이올렛 배경
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF5B2C9E), Color(0xFF4A2387), Color(0xFF321A63))
                    )
                )
                .dotPattern(Color.White.copy(alpha = 0.06f), step = 28f)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── 상단 타이틀 · 총 전투력 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x55FFFFFF))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↩", color = Color.White, fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "DECK",
                        color = Color.White,
                        fontSize = TcgType.TITLE.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "편성 ${state.party.size}/${state.unlockedDeckSize}장 · 최대 ${DeckRules.MAX_DECK_SIZE}장",
                        color = Color(0xE6FFFFFF),
                        fontSize = TcgType.LABEL.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xB3A03FB8))
                        .border(1.5.dp, Color(0xFFFFEB3B), RoundedCornerShape(50))
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔", color = Color.White, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${state.deckDisplayPower}",
                            color = Color.White,
                            fontSize = TcgType.NUMBER.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // ── 탭 ──
            TcgTabBar(
                tabs = listOf(
                    "덱 편성 ${state.party.size}/${state.unlockedDeckSize}",
                    "컬렉션 ${collection.size}",
                    "서약 · 정화"
                ),
                selectedIndex = tabIndex,
                accent = Color(0xFFEDE7F6),
                modifier = Modifier.padding(horizontal = 12.dp),
                onSelect = { tabIndex = it }
            )

            when (tabIndex) {
                DECK_TAB_BUILD -> DeckBuildTab(
                    state = state,
                    onUnloadSlot = onUnloadSlot,
                    onOpenCollection = onOpenCollection,
                    onUnloadAll = onUnloadAll,
                    onAutoEquip = onAutoEquip,
                    onOpenBattle = onOpenBattle,
                    onTrain = onTrain,
                    onOpenDeckCard = onOpenDeckCard,
                    onOpenGrowthFromDeck = onOpenGrowthFromDeck
                )

                DECK_TAB_COLLECTION -> CollectionTab(
                    state = state,
                    collection = collection,
                    sortByPower = sortByPower,
                    onToggleCard = onToggleCard
                )

                else -> Box(modifier = Modifier.weight(1f)) {
                    DeckScreen(
                        state = state,
                        onGaze = onGaze,
                        onPurify = onPurify,
                        onSetLobbyHero = onSetLobbyHero,
                        onOpenCollection = onOpenCollection
                    )
                }
            }
        }

        // ── 컬렉션 탭의 플로팅 필터 ──
        if (tabIndex == DECK_TAB_COLLECTION) {
            if (filterOpen) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xF21B1230))
                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("속성", color = Color(0xCCFFFFFF), fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip("전체", elementFilter == null) { elementFilter = null }
                        Element.entries.forEach { element ->
                            FilterChip(
                                element.displayName,
                                elementFilter == element,
                                elementColor(element)
                            ) { elementFilter = element }
                        }
                    }
                    Text("분류 · 정렬", color = Color(0xCCFFFFFF), fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip("신규 15", onlyNewWave, Color(0xFF00E5FF)) { onlyNewWave = !onlyNewWave }
                        FilterChip("전투력순", sortByPower, Color(0xFFFFB74D)) { sortByPower = true }
                        FilterChip("도감순", !sortByPower, Color(0xFFB388FF)) { sortByPower = false }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (filterOpen) Color(0xFF00E5FF) else Color(0xFF9C6BFF))
                    .border(2.dp, Color(0x66FFFFFF), CircleShape)
                    .clickable { filterOpen = !filterOpen },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (filterOpen) "✕" else "▽",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // ── 덱 편성 탭의 편성 캐릭터 시트 (복장 변경 · 성장 바로가기) ──
        if (tabIndex == DECK_TAB_BUILD) {
            DeckCharacterSheet(
                state = state,
                onClose = onCloseDeckSheet,
                onSelectOutfit = onSelectDeckOutfit,
                onOpenGrowth = onOpenGrowthFromDeck,
                onUnloadFromDeck = onUnloadSlot
            )
        }
    }
}

private const val DECK_TAB_BUILD = 0
private const val DECK_TAB_COLLECTION = 1
private const val DECK_TAB_PACT = 2

/**
 * 덱 편성 탭 — 3열 × 최대 4행(12칸).
 * 카드는 2:3 세로 카드로 크게 보여 주고, 12칸을 넘지 않으므로 이 탭은 스크롤도 짧다.
 */
@Composable
private fun DeckBuildTab(
    state: TcgGameState,
    onUnloadSlot: (Int) -> Unit,
    onOpenCollection: () -> Unit,
    onUnloadAll: () -> Unit,
    onAutoEquip: () -> Unit,
    onOpenBattle: () -> Unit,
    onTrain: () -> Unit,
    onOpenDeckCard: (Int) -> Unit,
    onOpenGrowthFromDeck: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 레벨 · 해금 현황
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Lv.${state.playerLevel}",
                color = Color(0xFFFFEB3B),
                fontSize = TcgType.BODY.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                StatBar(ratio = state.levelProgress, color = Color(0xFF00E5FF), height = 8)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x66FFFFFF))
                    .clickable { onTrain() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("훈련 +120", color = Color.White, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            state.nextDeckUnlock?.let { "다음 해금 Lv.${it.level} → ${it.size}장" } ?: "최대 12장 해금 완료 — 카드 탭 = 복장·성장",
            color = Color(0xCCFFFFFF),
            fontSize = TcgType.LABEL.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // 액션 바 (화면 위쪽 고정 — 카드에 가리지 않는다)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x66B388FF))
                    .border(1.5.dp, Color(0xCCD1C4E9), RoundedCornerShape(50))
                    .clickable { onUnloadAll() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "전체 해제",
                    color = Color.White,
                    fontSize = TcgType.BODY.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF8A3D))))
                    .border(1.5.dp, Color(0x66FFFFFF), RoundedCornerShape(50))
                    .clickable { onAutoEquip() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "AUTO · 자동 편성",
                    color = Color(0xFF2B1600),
                    fontSize = TcgType.BODY.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xCCFF7043))
                    .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
                    .clickable { onOpenBattle() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚔", fontSize = 18.sp)
            }
        }

        // 12칸 그리드 — 3열 × 4행, 카드 2:3
        LazyVerticalGrid(
            columns = GridCells.Fixed(CardLayoutRules.GRID_COLUMNS),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = (0 until DeckRules.MAX_DECK_SIZE).toList(),
                key = { _, slot -> "slot_$slot" }
            ) { _, slot ->
                val member = state.memberAt(slot)
                val unlocked = slot < state.unlockedDeckSize
                Box(modifier = Modifier.aspectRatio(CardLayoutRules.TILE_ASPECT)) {
                    when {
                        // 2026-11-22: 편성 카드는 **그 캐릭터가 고른 복장**으로 보여 준다 (로비·도감과 같은 기억)
                        member != null -> DuelCardTile(
                            cardId = member.card.id,
                            visual = state.outfitVisual(member.card.id),
                            element = member.card.element,
                            starLevel = member.card.starLevel,
                            power = DeckAutoBuilder.displayPower(member.card),
                            modifier = Modifier.fillMaxSize(),
                            targetWidthPx = 320,
                            onClick = { onOpenDeckCard(member.card.id) }
                        )
                        !unlocked -> LockedSlotTile(
                            unlockLevel = DeckRules.requiredLevelForSlot(slot),
                            label = DeckRules.slotLabel(slot),
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> EmptySlotTile(
                            label = DeckRules.slotLabel(slot),
                            hint = "빈 칸",
                            modifier = Modifier.fillMaxSize(),
                            onClick = onOpenCollection
                        )
                    }
                    if (member != null) {
                        // ✕ 해제 — 배지 자체가 버튼 (카드 탭 = 캐릭터 시트 열기)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC000000))
                                .clickable { onUnloadSlot(slot) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "✕ 해제",
                                color = Color.White,
                                fontSize = TcgType.TILE_MARK.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // ⚒ 성장 — 그 캐릭터가 선택된 성장·강화 화면으로 바로 이동
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xE6FF8A3D))
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(50))
                                .clickable { onOpenGrowthFromDeck(member.card.id) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "⚒ 성장",
                                color = Color(0xFF2B1600),
                                fontSize = TcgType.TILE_MARK.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "카드를 탭하면 복장 변경·성장 바로가기 / ✕ = 해제 / 빈 칸 = 컬렉션에서 고르기",
                    color = Color(0xCCFFFFFF),
                    fontSize = TcgType.LABEL.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** 컬렉션 탭 — 보유 카드 전체를 2:3 카드로 세로 스크롤 */
@Composable
private fun CollectionTab(
    state: TcgGameState,
    collection: List<TcgCard>,
    sortByPower: Boolean,
    onToggleCard: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(Color(0xE6EDE7F6))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    "My Collection",
                    color = Color(0xFF2C1C4A),
                    fontSize = TcgType.BODY.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${collection.size}명 · ${if (sortByPower) "전투력순" else "도감순"}",
                color = Color(0xE6FFFFFF),
                fontSize = TcgType.LABEL.sp
            )
            Spacer(Modifier.weight(1f))
            Text("탭 = 장착/해제", color = Color(0xCCFFFFFF), fontSize = TcgType.LABEL.sp)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(CardLayoutRules.COLLECTION_COLUMNS),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = collection, key = { it.id }) { card ->
                val equipped = card.id in state.equippedCardIds
                DuelCardTile(
                    cardId = card.id,
                    visual = card.currentVisual,
                    element = card.element,
                    starLevel = card.starLevel,
                    power = DeckAutoBuilder.displayPower(card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CardLayoutRules.TILE_ASPECT),
                    equipped = equipped,
                    statusBadge = if (CardCatalog.isNewWave(card.id)) "NEW" else null,
                    statusBadgeColor = Color(0xFF00E5FF),
                    targetWidthPx = 320,
                    onClick = { onToggleCard(card.id) }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, accent: Color = Color(0xFFB388FF), onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else Color(0x33FFFFFF))
            .border(1.dp, if (selected) accent else Color(0x55FFFFFF), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF140F22) else Color(0xE6FFFFFF),
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 덱 편성 탭의 **편성 캐릭터 시트** — 2026-11-22 사용자 요청 두 가지를 한곳에 담았다.
 *
 *  ① 덱에 편성된 캐릭터의 **복장 변경**: 전투·일상·데이트·미니어처 중 해금된 것만 고를 수 있고,
 *     고른 복장은 로비·도감과 같은 **캐릭터별 복장 기억**에 저장되어 세 화면이 함께 바뀝니다.
 *  ② **성장·강화 화면 바로가기**: 그 캐릭터가 성장 대상으로 선택된 채 [GrowthScreen] 이 열린다.
 *     (전투 결과 화면처럼 카드를 다시 찾지 않고 바로 강화하러 가지 않게)
 *
 * 카드 탭 = 이 시트 열기 / 배지 ✕ = 덱에서 빼기 / 배지 ⚒ 성장 = 바로 성장 화면.
 */
@Composable
private fun DeckCharacterSheet(
    state: TcgGameState,
    onClose: () -> Unit,
    onSelectOutfit: (VisualState) -> Unit,
    onOpenGrowth: (Int) -> Unit,
    onUnloadFromDeck: (Int) -> Unit
) {
    val card = state.deckSheetCard ?: return
    val bond = state.deckSheetCardBond
    val visual = state.outfitVisual(card.id)
    val slot = state.party.firstOrNull { it.card.id == card.id }?.slotIndex ?: -1
    val each = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = each, indication = null) { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(Color(0xF21B1230))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                // 시트 안의 탭이 바깥(닫기)으로 새지 않게 입력을 받아 둔다
                .clickable(interactionSource = each, indication = null) { }
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        CardCatalog.require(card.id).name,
                        color = Color.White,
                        fontSize = TcgType.TITLE.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "${CardCatalog.require(card.id).title} · 친밀도 $bond/100" +
                            if (slot >= 0) " · 덱 ${DeckRules.slotLabel(slot)}" else "",
                        color = Color(0xCCFFFFFF),
                        fontSize = TcgType.LABEL.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row {
                DuelCardTile(
                    cardId = card.id,
                    visual = visual,
                    element = card.element,
                    starLevel = card.starLevel,
                    power = DeckAutoBuilder.displayPower(card),
                    modifier = Modifier
                        .width(104.dp)
                        .aspectRatio(CardLayoutRules.TILE_ASPECT),
                    targetWidthPx = 260
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "👗 복장 — 탭하면 바로 바뀝니다",
                        color = Color(0xFFFFEB3B),
                        fontSize = TcgType.LABEL.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutfitChip("① 전투", VisualState.BATTLE, visual, bond, onSelectOutfit)
                        OutfitChip("② 일상", VisualState.CASUAL_CUTE, visual, bond, onSelectOutfit)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutfitChip("③ 데이트", VisualState.ROMANTIC_DATE, visual, bond, onSelectOutfit)
                        OutfitChip("④ 미니어처", VisualState.MINIATURE, visual, bond, onSelectOutfit)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.deckOutfitLockMessage
                            ?: (AffinityRules.nextUnlockNeed(bond)?.let { "일상 30 · 데이트 70 — 다음 해금까지 친밀도 ${it} (전투 출전 +${AffinityRules.BATTLE_WIN_GAIN}/승리)" }
                                ?: "복장 모두 해금 — 페어 오의도 열려 있습니다"),
                        color = if (state.deckOutfitLockMessage != null) Color(0xFFFF8A80) else Color(0x99FFFFFF),
                        fontSize = TcgType.LABEL.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF8A3D))))
                        .border(1.5.dp, Color(0x66FFFFFF), RoundedCornerShape(50))
                        .clickable { onOpenGrowth(card.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚒ 성장·강화 바로가기",
                        color = Color(0xFF2B1600),
                        fontSize = TcgType.BODY.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                if (slot >= 0) {
                    Box(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(50))
                            .clickable { onUnloadFromDeck(slot) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "덱에서 빼기",
                            color = Color(0xE6FFFFFF),
                            fontSize = TcgType.BODY.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** 시트의 복장 칩 — 잠긴 복장은 🔒 를 붙이고 누르면 잠금 안내가 뜨다 ([TcgGameViewModel.selectDeckOutfit]) */
@Composable
private fun OutfitChip(
    label: String,
    visual: VisualState,
    current: VisualState,
    bond: Int,
    onSelect: (VisualState) -> Unit
) {
    val unlocked = AffinityRules.isVisualUnlocked(visual, bond)
    FilterChip(
        label = if (unlocked) label else "$label 🔒",
        selected = visual == current,
        accent = if (unlocked) Color(0xFFB388FF) else Color(0x66FFFFFF),
        onClick = { onSelect(visual) }
    )
}
