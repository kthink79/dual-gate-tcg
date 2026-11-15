package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.CardRarity
import com.game.tcg.core.model.PackCatalog
import com.game.tcg.core.model.PackDefinition
import com.game.tcg.core.model.PackOpenResult

/**
 * 🛒 카드팩 상점 — 메인 루프의 "카드팩 확보 → 캐릭터 추가 확보" 단계.
 *
 * · 스테이지 보상으로 받은 **🎟 카드팩 티켓**이 있으면 티켓을 먼저 쓰고, 없으면 골드로 산다.
 * · 개봉 결과는 카드별로 **신규 계약(★)** / **중복 → 조각 +10 · 50G** 로 나뉘어 표시된다.
 * · 모인 조각은 「강화·합성·장비」 화면에서 카드 강화와 연성 재료로 쓰인다.
 */
@Composable
fun PackShopScreen(
    state: TcgGameState,
    onOpenPack: (String) -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenCollection: () -> Unit
) {
    ScreenFrame {
        SectionCard(
            title = "카드팩 상점",
            badge = "🪙 ${state.gold}G · 🎟 ${state.packTickets}",
            accent = TcgColors.Gold
        ) {
            Text(
                "카드팩에서 나온 동료로 덱을 채우고, 중복은 조각으로 바뀌어 강화·연성 재료가 됩니다. " +
                    "티켓이 있으면 티켓을 먼저 사용합니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("보유 동료 ${state.ownedCards.size}명", TcgColors.Violet)
                Pill("🧩 조각 ${state.shardTotal}", TcgColors.Abyss)
                Pill("🎟 ${state.packTickets}", TcgColors.Mana)
            }
            PackCatalog.ALL.forEach { pack ->
                PackTile(
                    pack = pack,
                    gold = state.gold,
                    tickets = state.packTickets,
                    onBuy = { onOpenPack(pack.id) }
                )
            }
        }

        val opened: PackOpenResult? = state.lastPackOpen
        if (opened != null) {
            SectionCard(
                title = "개봉 결과 · ${opened.packName}",
                badge = if (opened.hasNewCard) "신규 ${opened.newCardCount}명" else "중복 ${opened.draws.size}장",
                accent = if (opened.hasNewCard) TcgColors.Heal else TcgColors.Muted
            ) {
                opened.draws.forEach { draw ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (draw.isNew) TcgColors.Heal.copy(alpha = 0.12f) else TcgColors.SurfaceAlt)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pill(
                            if (draw.isNew) "NEW" else "중복",
                            if (draw.isNew) TcgColors.Heal else TcgColors.Muted
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(draw.name, color = TcgColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Pill(draw.rarity.displayName, Color(draw.rarity.color))
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (draw.isNew) "계약" else "조각 +${draw.shards} · ${draw.gold}G",
                            color = if (draw.isNew) TcgColors.Heal else TcgColors.Gold,
                            fontSize = 10.sp
                        )
                    }
                }
                Text(
                    "획득 조각 +${opened.totalShards} · 중복 환급 ${opened.totalGold}G" +
                        if (opened.ticketsSpent > 0) " · 🎟 ${opened.ticketsSpent} 사용" else " · ${opened.goldSpent}G 사용",
                    color = TcgColors.Muted,
                    fontSize = 9.sp
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
                    "· 마왕(112~114)·중간 보스(170~175)는 카드팩에 등장하지 않습니다. " +
                    "마왕은 토벌 후 서약, 중간 보스는 「심연 연성」으로 합류합니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/** 카드팩 1종 진열 */
@Composable
private fun PackTile(
    pack: PackDefinition,
    gold: Int,
    tickets: Int,
    onBuy: () -> Unit
) {
    val useTicket = pack.ticketCost > 0 && tickets >= pack.ticketCost
    val affordable = useTicket || gold >= pack.goldCost
    val accent = when {
        pack.element != null -> elementColor(pack.element)
        pack.kind == com.game.tcg.core.model.PackKind.PREMIUM -> TcgColors.Gold
        else -> TcgColors.Violet
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pack.kind.icon, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(pack.name, color = TcgColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Pill("${pack.cardCount}장", accent)
            Spacer(Modifier.weight(1f))
            Pill(pack.priceLabel, TcgColors.Gold)
        }
        Text(pack.description, color = TcgColors.Muted, fontSize = 9.sp, lineHeight = 14.sp)
        ActionTile(
            label = if (!affordable) "재화 부족" else if (useTicket) "🎟 ${pack.ticketCost} 로 개봉" else "${pack.goldCost}G 로 개봉",
            hint = if (!affordable) "🪙 ${gold}G · 🎟 ${tickets}" else "눌러서 즉시 개봉",
            accent = accent,
            enabled = affordable,
            onClick = onBuy
        )
    }
}
