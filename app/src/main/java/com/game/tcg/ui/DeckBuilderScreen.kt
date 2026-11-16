package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.game.tcg.core.engine.DeckAutoBuilder
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.TcgCard

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
    onSetLobbyHero: (Int) -> Unit
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
                    onTrain = onTrain
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
    onTrain: () -> Unit
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
            state.nextDeckUnlock?.let { "다음 해금 Lv.${it.level} → ${it.size}장" } ?: "최대 12장 해금 완료 — 슬롯 탭 = 해제",
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
                        member != null -> DuelCardTileFor(
                            card = member.card,
                            modifier = Modifier.fillMaxSize(),
                            equipped = false,
                            hpRatio = null,
                            targetWidthPx = 320,
                            onClick = { onUnloadSlot(slot) }
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
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC000000))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "✕ 해제",
                                color = Color.White,
                                fontSize = TcgType.TILE_MARK.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "덱은 ${DeckRules.MAX_DECK_SIZE}칸까지 — 빈 칸을 누르면 컬렉션에서 고를 수 있습니다.",
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
