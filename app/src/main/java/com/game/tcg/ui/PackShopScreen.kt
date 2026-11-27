package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.audio.SfxEffect
import com.game.tcg.core.model.PackCatalog
import com.game.tcg.core.model.PackDefinition
import com.game.tcg.core.model.PackKind
import com.game.tcg.core.model.PackOpenResult

/**
 * 🛒 카드팩 상점 — 2026-11-16 사용자 요청(실기기 스크린샷 25052) 개편.
 *
 * 이전 화면은 카드팩 4종이 **텍스트 박스 4개**로만 나열돼 게임처럼 보이지 않았다.
 * 이제
 *  - 카드팩마다 **봉인 카드팩 그래픽**(Canvas) — 속성 오라·금속 테두리·인장·구성 장수 배지
 *  - 탭(일반 / 고급 / 속성팩)으로 진열을 갈라 한 화면에 다 담는다
 *  - 본문 12sp·제목 18sp 로 키워 읽기 쉽게
 */
@Composable
fun PackShopScreen(
    state: TcgGameState,
    onOpenPack: (String) -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenDiamondShop: () -> Unit = {},
    onTearPack: () -> Unit = {},
    onRevealNextDraw: () -> Unit = {},
    onRevealAllDraws: () -> Unit = {},
    onCloseOpening: () -> Unit = {},
    onReplayOpening: () -> Unit = {},
    onSfx: (SfxEffect) -> Unit = {},
    onVideoStart: () -> Unit = {},
    onVideoEnd: () -> Unit = {}
) {
    var tabIndex by remember { mutableIntStateOf(PACK_TAB_STANDARD) }

    val packs = remember(tabIndex) {
        when (tabIndex) {
            PACK_TAB_PREMIUM -> listOf(PackCatalog.PREMIUM)
            PACK_TAB_ELEMENT -> PackCatalog.ALL.filter { it.kind == PackKind.ELEMENTAL }
            else -> listOf(PackCatalog.STANDARD)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenFrame {
            // ── 헤더 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("카드팩", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Pill("🪙 ${state.gold}G", TcgColors.Gold)
                Spacer(Modifier.width(5.dp))
                Pill("🎟 ${state.packTickets}", TcgColors.Mana)
            }
            Text(
                "카드팩에서 나온 동료로 덱을 채우고, 중복은 조각이 되어 강화·연성 재료가 됩니다. " +
                    "티켓이 있으면 티켓을 먼저 사용합니다.",
                color = TcgColors.Muted,
                fontSize = TcgType.BODY_SMALL.sp,
                lineHeight = 19.sp
            )
            PackResourceRow(state)

            // 2026-11-27 v0.6.0 — 💎 다이아 사용처 4종 진입 (지정 소환·충전·교환·데이트 룩)
            SectionCard(title = "💎 다이아 교환소", badge = "보유 ${state.gems}", accent = TcgColors.Mana) {
                ActionTile(
                    label = "💎 다이아로 교환 · 소환",
                    hint = "지정 소환 5 · 충전 3 · 교환 2 · 데이트 룩 4",
                    accent = TcgColors.Mana,
                    onClick = onOpenDiamondShop
                )
            }

            // ── 진열 탭 ──
            TcgTabBar(
                tabs = listOf("일반팩", "고급팩", "속성팩"),
                selectedIndex = tabIndex,
                accent = TcgColors.Gold,
                onSelect = { tabIndex = it }
            )

            packs.forEach { pack ->
                PackTile(
                    pack = pack,
                    gold = state.gold,
                    tickets = state.packTickets,
                    onBuy = { onOpenPack(pack.id) }
                )
            }

            val opened: PackOpenResult? = state.lastPackOpen
            if (opened != null) {
                SectionCard(
                    title = "개봉 결과 · ${opened.packName}",
                    badge = if (opened.hasNewCard) "신규 ${opened.newCardCount}명" else "중복 ${opened.draws.size}장",
                    accent = if (opened.hasNewCard) TcgColors.Heal else TcgColors.Muted
                ) {
                    PackOpenGrid(opened)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Pill("획득 조각 +${opened.totalShards}", TcgColors.Abyss)
                        if (opened.totalGold > 0) Pill("중복 환급 ${opened.totalGold}G", TcgColors.Gold)
                        Pill(
                            if (opened.ticketsSpent > 0) "🎟 ${opened.ticketsSpent} 사용" else "${opened.goldSpent}G 사용",
                            TcgColors.Muted
                        )
                    }
                    ActionTile(
                        label = "🎬 개봉 연출 다시 보기",
                        hint = "방금 나온 카드를 다시 한 장씩",
                        accent = TcgColors.Gold,
                        onClick = onReplayOpening
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            ActionTile(
                                label = "⚒ 강화·합성으로",
                                hint = "조각 ${state.shardTotal} 보유",
                                accent = TcgColors.Violet,
                                onClick = onOpenGrowth
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ActionTile(
                                label = "🃏 도감에서 확인",
                                hint = "새 동료 ${state.ownedCards.size}명",
                                accent = TcgColors.Gold,
                                onClick = onOpenCollection
                            )
                        }
                    }
                }
            }

            SectionCard(title = "상점 안내", badge = "확률", accent = TcgColors.Muted) {
                Text(
                    "· 일반팩: 흔한 등급 위주 3장\n" +
                        "· 고급팩: 영웅(7코스트) 이상 1장 확정 5장\n" +
                        "· 속성팩: 지정 속성만 3장\n" +
                        "· 중복 카드는 조각 +10 · 50G로 자동 변환\n" +
                        "· 마왕(112~114)·중간 보스(170~175)는 카드팩에 등장하지 않습니다.",
                    color = TcgColors.Muted,
                    fontSize = TcgType.BODY_SMALL.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // ── 단계별 개봉 연출 (봉인 카드팩 → 카드 1장씩 → 전체 그리드) ──
        if (state.packOpening.isActive) {
            PackOpeningOverlay(
                opening = state.packOpening,
                onTear = onTearPack,
                onRevealNext = onRevealNextDraw,
                onRevealAll = onRevealAllDraws,
                onClose = onCloseOpening,
                onOpenGrowth = onOpenGrowth,
                onOpenCollection = onOpenCollection,
                onSfx = onSfx,
                onVideoStart = onVideoStart,
                onVideoEnd = onVideoEnd
            )
        }
    }
}

private const val PACK_TAB_STANDARD = 0
private const val PACK_TAB_PREMIUM = 1
private const val PACK_TAB_ELEMENT = 2

/** 보유 현황 배지 — 좁은 화면에서는 두 줄로 흐른다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackResourceRow(state: TcgGameState) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Pill("보유 동료 ${state.ownedCards.size}명", TcgColors.Violet)
        Pill("🧩 조각 ${state.shardTotal}", TcgColors.Abyss)
        Pill("🎟 티켓 ${state.packTickets}", TcgColors.Mana)
    }
}

/** 개봉 결과 카드 — 2:3 비율 유지 + 이름 라벨을 카드 아래로 */
@Composable
private fun PackOpenGrid(opened: PackOpenResult) {
    opened.draws.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row.forEach { draw ->
                val card = TcgGameViewModel.characterCard(draw.cardId)
                Column(modifier = Modifier.weight(1f)) {
                    DuelCardTile(
                        cardId = draw.cardId,
                        visual = com.game.tcg.core.model.VisualState.BATTLE,
                        element = card.element,
                        starLevel = card.starLevel,
                        power = (card.atk + card.def) / 100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(CardLayoutRules.TILE_ASPECT),
                        statusBadge = if (draw.isNew) "NEW" else "🧩+${draw.shards}",
                        statusBadgeColor = if (draw.isNew) TcgColors.Heal else TcgColors.Gold,
                        targetWidthPx = 300
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        draw.name,
                        color = TcgColors.Text,
                        fontSize = TcgType.LABEL.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** 카드팩 1종 진열 — 봉인 카드팩 그래픽 + 이름·구성·가격 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackTile(
    pack: PackDefinition,
    gold: Int,
    tickets: Int,
    onBuy: () -> Unit
) {
    val useTicket = pack.ticketCost > 0 && tickets >= pack.ticketCost
    val affordable = useTicket || gold >= pack.goldCost
    val accent = packAccent(pack)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.22f), TcgColors.Surface, TcgColors.SurfaceAlt)
                )
            )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PackArtwork(
                    pack = pack,
                    modifier = Modifier
                        .width(92.dp)
                        .aspectRatio(0.74f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        pack.name,
                        color = TcgColors.Text,
                        fontSize = TcgType.SECTION.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Pill("${pack.kind.icon} ${pack.kind.displayName}", accent)
                        Pill("${pack.cardCount}장", TcgColors.Mana)
                        pack.guaranteedRarity?.let { rarity ->
                            Pill("${rarity.displayName} 이상 확정", Color(rarity.color))
                        }
                    }
                    Text(
                        pack.description,
                        color = TcgColors.Muted,
                        fontSize = TcgType.BODY_SMALL.sp,
                        lineHeight = 19.sp
                    )
                    Text(
                        pack.priceLabel,
                        color = TcgColors.Gold,
                        fontSize = TcgType.NUMBER.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            ActionTile(
                label = when {
                    !affordable -> "재화 부족"
                    useTicket -> "🎟 ${pack.ticketCost} 로 개봉"
                    else -> "${pack.goldCost}G 로 개봉"
                },
                hint = if (!affordable) {
                    "🪙 ${gold}G · 🎟 ${tickets} — 조금 더 모아 오세요"
                } else {
                    "개봉 연출과 함께 카드가 등장합니다"
                },
                accent = accent,
                enabled = affordable,
                onClick = onBuy
            )
        }
    }
}
