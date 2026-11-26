package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.VisualState

/**
 * 우측 서브메뉴 「서약·덱」의 서약 탭 — 출전 덱 현황 + 마왕 응시·성수 정화 + 대표 캐릭터 설정.
 *
 * 2026-11-26 사용자 요청 「메뉴 아이콘 중심·설명문 축소·글자 확대·종스크롤 최소화」:
 *  - 장문 안내(장수 해금 규칙·1캐릭터 1장 원칙·로비 설명)를 걷어내고 배지·수치로 대체
 *  - 출전 덱은 **초상화 슬롯 그리드**(전열/후열 배지)로, 대표 캐릭터 설정은
 *    세로 리스트 → **4열 초상화 아이콘 그리드**로 압축
 *  - 글씨는 [TcgType] 기준(제목 24 · 섹션 18 · 본문 15 · 배지 13) 이상만 쓴다
 */
@Composable
fun DeckScreen(
    state: TcgGameState,
    onGaze: () -> Unit,
    onPurify: () -> Unit,
    onSetLobbyHero: (Int) -> Unit,
    onOpenCollection: () -> Unit
) {
    val deckSize = state.unlockedDeckSize
    val frontCount = state.party.count { it.isFrontRow }

    CompactFrame {
        // ── 머리 1줄: 제목 + 편성 수 + 전투력 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("서약 · 정화", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Pill("${state.party.size}/${deckSize}장", TcgColors.Gold)
            Spacer(Modifier.weight(1f))
            Pill("⚔ ${state.deckDisplayPower}", TcgColors.Violet)
        }

        // ── ① 출전 덱 — 초상화 슬롯 그리드 (전열 3 · 후열 나머지) ──
        CompactCard(
            title = "출전 덱",
            badge = "전열 $frontCount/3",
            accent = TcgColors.Gold
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until deckSize).chunked(6).forEach { rowSlots ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowSlots.forEach { slot ->
                            val member = state.party.firstOrNull { it.slotIndex == slot }
                            Box(modifier = Modifier.weight(1f)) {
                                DeckSlotTile(
                                    slot = slot,
                                    member = member,
                                    onClick = onOpenCollection
                                )
                            }
                        }
                        repeat(6 - rowSlots.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚒", fontSize = TcgType.ICON.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    "빈 칸을 누르면 캐릭터 관리",
                    color = TcgColors.Muted,
                    fontSize = TcgType.LABEL.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── ② 마왕 응시 · 성수 정화 — 배지 + 두 버튼 한 줄 ──
        CompactCard(
            title = "마왕 응시 · 정화",
            badge = "[홀림] ${state.combatPenalty.totalStacks}",
            accent = TcgColors.Danger
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("시선 ${state.temptationState.gazeTemptationStacks}/5", TcgColors.Danger)
                Spacer(Modifier.width(5.dp))
                Pill("저주 ${state.temptationState.curseOfArchdemonStacks}/5", TcgColors.Abyss)
                Spacer(Modifier.weight(1f))
                Text("💧 ${state.holyWater}", color = TcgColors.Heal, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
            }
            StatBar(ratio = state.temptationState.visualMistIntensity, color = TcgColors.Abyss, height = 7)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    ActionTile(label = "🔍 응시", hint = "+1", accent = TcgColors.Danger) { onGaze() }
                }
                Box(Modifier.weight(1f)) {
                    ActionTile(
                        label = "💧 정화",
                        hint = "-2",
                        accent = TcgColors.Heal,
                        enabled = state.holyWater > 0,
                        onClick = onPurify
                    )
                }
            }
            val message = state.gazeMessage ?: state.combatPenalty.debuffDescription
            Text(
                message,
                color = if (state.gazeMessage != null) TcgColors.Text.copy(alpha = 0.9f) else TcgColors.Danger,
                fontSize = TcgType.LABEL.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── ③ 대표 캐릭터 — 4열 초상화 아이콘 그리드 ──
        CompactCard(
            title = "대표 캐릭터",
            badge = "메인 로비",
            accent = TcgColors.Violet,
            modifier = Modifier.weight(1f),
            expand = true
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TcgColors.Background)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.lobbyRoster, key = { it }) { id ->
                    val card = TcgGameViewModel.characterCard(id)
                    val name = TcgGameViewModel.profileOf(id)?.displayName ?: card.name
                    PortraitIconTile(
                        cardId = id,
                        visual = VisualState.BATTLE,
                        modifier = Modifier.fillMaxWidth(),
                        label = name.take(4),
                        badge = if (id == state.lobbyHeroId) "★" else null,
                        selected = id == state.lobbyHeroId,
                        accent = elementColor(card.element),
                        contentDescription = name,
                        onClick = { onSetLobbyHero(id) }
                    )
                }
            }
        }
    }
}

/** 덱 슬롯 1칸 — 캐릭터가 있으면 초상화, 없으면 + (누르면 캐릭터 관리) */
@Composable
private fun DeckSlotTile(
    slot: Int,
    member: PartyMember?,
    onClick: () -> Unit
) {
    val card = member?.card
    val front = DeckRules.isFrontSlot(slot)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CardLayoutRules.TILE_ASPECT)
                .clip(RoundedCornerShape(8.dp))
                .background(TcgColors.SurfaceAlt)
                .border(
                    if (front) 1.5.dp else 1.dp,
                    if (front) TcgColors.Hp.copy(alpha = 0.8f) else TcgColors.Line,
                    RoundedCornerShape(8.dp)
                )
        ) {
            if (card != null) {
                CardArtImage(
                    cardId = card.id,
                    visual = card.currentVisual,
                    contentDescription = card.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 160
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (front) TcgColors.Hp else TcgColors.Mana)
                        .padding(horizontal = 3.dp)
                ) {
                    Text(
                        if (front) "전" else "후",
                        color = TcgColors.Background,
                        fontSize = TcgType.TILE_MARK.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋", color = TcgColors.Muted, fontSize = TcgType.NUMBER.sp)
                }
            }
        }
        Text(
            DeckRules.slotLabel(slot),
            color = TcgColors.Muted,
            fontSize = TcgType.TILE_MARK.sp,
            maxLines = 1
        )
    }
}

/**
 * 제목 + 배지 + 내용만 있는 콤팩트 카드 (장문 설명 없음).
 * [modifier] 로 `weight(1f)` 를 주면 남는 높이를 내용(그리드)이 채운다.
 */
@Composable
private fun CompactCard(
    title: String,
    badge: String? = null,
    accent: androidx.compose.ui.graphics.Color = TcgColors.Violet,
    modifier: Modifier = Modifier,
    expand: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = TcgColors.Surface)
    ) {
        Column(
            modifier = (if (expand) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(7.dp))
                Text(title, color = TcgColors.Text, fontSize = TcgType.SECTION.sp, fontWeight = FontWeight.Bold)
                if (badge != null) {
                    Spacer(Modifier.width(7.dp))
                    Pill(badge, accent)
                }
            }
            content()
        }
    }
}
