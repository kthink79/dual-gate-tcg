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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.game.tcg.core.model.VisualState

/**
 * 🃏 덱 편성 (DECK) — 타 TCG `DECK` 화면 레퍼런스의 비주얼 편성 UI (2026-11-16 개편)
 *
 * · 상단: 보라/다크 바이올렛 배경의 `DECK` 타이틀 + **덱 총 전투력** + 플레이어 레벨·해금 현황
 * · 덱 그리드: 4열 × 3행 = 12칸 (기본 9칸 + 잠금 3칸) — 장착 카드 탭 = 즉시 해제, 잠긴 칸은 🔒 Lv.5/10/15
 * · 액션 바: **전체 해제(Unload all)** · **자동 편성(AUTO EQUIP)** — 해금된 9~12칸까지만 채운다
 * · 하단 `My Collection` 3열 그리드: 보유 카드 전체를 스크롤 탐색, 탭하면 빈 슬롯에 즉시 장착
 * · 우하단 플로팅 필터: 속성별 · 신규 15인 · 전투력순/도감순
 *
 * 170명(510장 JPG)은 [CardArtImage]의 축소 디코딩 + LazyVerticalGrid로 화면에 보이는 만큼만 그린다.
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
    var tab by remember { mutableStateOf(DECK_TAB_BUILD) }
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x55FFFFFF))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↩", color = Color.White, fontSize = 15.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "DECK",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "편성 ${state.party.size}/${state.unlockedDeckSize}장 · 최대 ${DeckRules.MAX_DECK_SIZE}장",
                        color = Color(0xCCFFFFFF),
                        fontSize = 8.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xB3A03FB8))
                        .border(1.5.dp, Color(0xFFFFEB3B), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔", color = Color.White, fontSize = 10.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${state.deckDisplayPower}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // ── 탭 전환 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabChip("덱 편성", tab == DECK_TAB_BUILD) { tab = DECK_TAB_BUILD }
                TabChip("서약 · 정화", tab == DECK_TAB_PACT) { tab = DECK_TAB_PACT }
            }

            if (tab == DECK_TAB_PACT) {
                Box(modifier = Modifier.weight(1f)) {
                    DeckScreen(
                        state = state,
                        onGaze = onGaze,
                        onPurify = onPurify,
                        onSetLobbyHero = onSetLobbyHero,
                        onOpenCollection = onOpenCollection
                    )
                }
                return@Column
            }

            // ── 레벨 · 해금 현황 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lv.${state.playerLevel}",
                    color = Color(0xFFFFEB3B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    StatBar(ratio = state.levelProgress, color = Color(0xFF00E5FF), height = 6)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    state.nextDeckUnlock?.let { "다음 해금 Lv.${it.level} → ${it.size}장" } ?: "최대 12장 해금 완료",
                    color = Color(0xCCFFFFFF),
                    fontSize = 8.sp
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x66FFFFFF))
                        .clickable { onTrain() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("훈련 +120", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── 덱 그리드 (4열 × 3행 = 12칸) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                (0 until DeckRules.MAX_DECK_SIZE).chunked(4).forEach { slotRow ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        slotRow.forEach { slot ->
                            val member = state.memberAt(slot)
                            val unlocked = slot < state.unlockedDeckSize
                            Box(modifier = Modifier.weight(1f)) {
                                when {
                                    member != null -> DuelCardTileFor(
                                        card = member.card,
                                        modifier = Modifier.fillMaxSize(),
                                        equipped = false,
                                        hpRatio = null,
                                        targetWidthPx = 200,
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
                                    Text(
                                        "✕",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 1.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xCC000000))
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 액션 바 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x66B388FF))
                        .border(1.5.dp, Color(0xCCD1C4E9), RoundedCornerShape(50))
                        .clickable { onUnloadAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Unload all · 전체 해제", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF8A3D)))
                        )
                        .border(1.5.dp, Color(0x66FFFFFF), RoundedCornerShape(50))
                        .clickable { onAutoEquip() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "AUTO EQUIP · 자동 편성",
                        color = Color(0xFF2B1600),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCCFF7043))
                        .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
                        .clickable { onOpenBattle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚔", fontSize = 16.sp)
                }
            }

            // ── My Collection 헤더 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(Color(0xE6EDE7F6))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        "My Collection",
                        color = Color(0xFF2C1C4A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${collection.size}명 · ${if (sortByPower) "전투력순" else "도감순"}",
                    color = Color(0xCCFFFFFF),
                    fontSize = 8.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "탭 = 장착/해제",
                    color = Color(0x99FFFFFF),
                    fontSize = 8.sp
                )
            }

            // ── 컬렉션 3열 그리드 ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(items = collection, key = { it.id }) { card ->
                    val equipped = card.id in state.equippedCardIds
                    Box(modifier = Modifier.height(148.dp)) {
                        DuelCardTile(
                            cardId = card.id,
                            visual = card.currentVisual,
                            element = card.element,
                            starLevel = card.starLevel,
                            power = DeckAutoBuilder.displayPower(card),
                            modifier = Modifier.fillMaxSize(),
                            equipped = equipped,
                            statusBadge = if (CardCatalog.isNewWave(card.id)) "NEW" else null,
                            statusBadgeColor = Color(0xFF00E5FF),
                            artAlignment = Alignment.TopCenter,
                            targetWidthPx = 240,
                            onClick = { onToggleCard(card.id) }
                        )
                    }
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(76.dp))
                }
            }
        }

        // ── 플로팅 필터 ──
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
                Text("속성", color = Color(0xCCFFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                Text("분류 · 정렬", color = Color(0xCCFFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                .background(
                    if (filterOpen) Color(0xFF00E5FF) else Color(0xFF9C6BFF)
                )
                .border(2.dp, Color(0x66FFFFFF), CircleShape)
                .clickable { filterOpen = !filterOpen },
            contentAlignment = Alignment.Center
        ) {
            Text(if (filterOpen) "✕" else "▽", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

private const val DECK_TAB_BUILD = 0
private const val DECK_TAB_PACT = 1

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0xFFEDE7F6) else Color(0x44FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF2C1C4A) else Color(0xCCFFFFFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
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
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF140F22) else Color(0xCCFFFFFF),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
