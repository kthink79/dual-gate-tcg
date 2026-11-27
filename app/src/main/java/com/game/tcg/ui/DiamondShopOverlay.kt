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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.DiamondShopEngine
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 💎 다이아 교환소 — 2026-11-27 v0.6.0 사용자 요청.
 *
 * 💎 재화의 쓰임새 4종을 한 화면에 모은다(가격·잠금 판정은 [DiamondShopEngine]).
 *  - ✨ 지정 소환 : 원하는 미보유 캐릭터 1명을 💎 5 로 영입
 *  - 🔋 충전      : 차원 균열 에너지 또는 성수 즉시 충전 (💎 3)
 *  - 🎁 교환      : 강화석(조각) 또는 친밀도 선물 (💎 2)
 *  - 🌹 데이트    : 보유 캐릭터의 데이트 룩 즉시 해금 (💎 4)
 *
 * 상점(카드팩)과 현실 소환 두 화면에서 열 수 있지만 패널은 이 하나뿐이다
 * (같은 상태 [TcgGameState.isDiamondShopOpen] 를 본다).
 */
@Composable
fun DiamondShopOverlay(
    state: TcgGameState,
    onClose: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onSelectTarget: (Int) -> Unit,
    onBuyTargetSummon: () -> Unit,
    onRecharge: (DiamondShopEngine.RechargeKind) -> Unit,
    onExchange: (DiamondShopEngine.ExchangeKind) -> Unit,
    onUnlockDateLook: () -> Unit
) {
    val tab = DiamondShopEngine.Tab.entries.getOrNull(state.diamondShopTab) ?: DiamondShopEngine.Tab.TARGET_SUMMON
    val target = state.ownedCards.firstOrNull { it.id == state.diamondShopTargetId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00A0714))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── 머리: 제목 + 보유 다이아 + 닫기 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💎 다이아 교환소", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Pill("💎 ${state.gems}", TcgColors.Mana)
                Spacer(Modifier.weight(1f))
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

            // ── 탭 4종 (아이콘 + 라벨) ──
            TcgIconTabBar(
                tabs = DiamondShopEngine.Tab.entries.map { it.icon to it.label },
                selectedIndex = tab.ordinal,
                accent = TcgColors.Mana,
                onSelect = onSelectTab
            )

            // ── 결과·안내 한 줄 ──
            Text(
                state.diamondShopMessage ?: defaultHint(tab, state),
                color = if (state.diamondShopMessage == null) TcgColors.Muted else TcgColors.Gold,
                fontSize = TcgType.LABEL.sp,
                maxLines = 3
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (tab) {
                    DiamondShopEngine.Tab.TARGET_SUMMON -> TargetSummonTab(
                        state = state,
                        onSelectTarget = onSelectTarget,
                        onBuy = onBuyTargetSummon
                    )

                    DiamondShopEngine.Tab.RECHARGE -> RechargeTab(
                        state = state,
                        onRecharge = onRecharge
                    )

                    DiamondShopEngine.Tab.EXCHANGE -> ExchangeTab(
                        state = state,
                        target = target,
                        onSelectTarget = onSelectTarget,
                        onExchange = onExchange
                    )

                    DiamondShopEngine.Tab.DATE_LOOK -> DateLookTab(
                        state = state,
                        target = target,
                        onSelectTarget = onSelectTarget,
                        onUnlock = onUnlockDateLook
                    )
                }
            }
        }
    }
}

/** 탭별 한 줄 안내 — 결과 메시지가 없을 때만 보인다 */
private fun defaultHint(tab: DiamondShopEngine.Tab, state: TcgGameState): String = when (tab) {
    DiamondShopEngine.Tab.TARGET_SUMMON ->
        "원하는 캐릭터를 골라 💎 ${DiamondShopEngine.PRICE_TARGET_SUMMON} 로 바로 영입 — 보유 ${state.ownedCards.size}/${CardCatalog.ROSTER_SIZE}명"
    DiamondShopEngine.Tab.RECHARGE ->
        "💎 ${DiamondShopEngine.PRICE_RECHARGE} · 에너지 ${state.gateEnergy}/${state.gateEnergyMax} · 성수 ${state.holyWater}"
    DiamondShopEngine.Tab.EXCHANGE ->
        "💎 ${DiamondShopEngine.PRICE_EXCHANGE} · 강화석 +${DiamondShopEngine.SHARD_GAIN} 또는 친밀도 +${DiamondShopEngine.BOND_GIFT_GAIN}"
    DiamondShopEngine.Tab.DATE_LOOK ->
        "💎 ${DiamondShopEngine.PRICE_DATE_LOOK} · 친밀도 ${AffinityRules.DATE_UNLOCK_BOND} 을 기다리지 않고 데이트 룩 해금"
}

/** ✨ 지정 소환 — 미보유 캐릭터 그리드 + 영입 버튼 */
@Composable
private fun TargetSummonTab(
    state: TcgGameState,
    onSelectTarget: (Int) -> Unit,
    onBuy: () -> Unit
) {
    val ownedIds = remember(state.ownedCards) { state.ownedCards.map { it.id }.toSet() }
    val candidates = remember(ownedIds) {
        CardCatalog.ALL.filter { it.id !in ownedIds && !it.isMidBoss }
    }
    val selected = state.diamondShopTargetId

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (candidates.isEmpty()) {
            Text(
                "등록된 캐릭터를 모두 보유하고 있습니다.",
                color = TcgColors.Muted,
                fontSize = TcgType.BODY.sp
            )
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(TcgColors.Surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(candidates, key = { it.id }) { record ->
                PortraitIconTile(
                    cardId = record.id,
                    visual = VisualState.BATTLE,
                    modifier = Modifier.fillMaxWidth(),
                    label = record.name.take(4),
                    selected = record.id == selected,
                    accent = elementColor(record.element),
                    contentDescription = record.fullName,
                    onClick = { onSelectTarget(record.id) }
                )
            }
        }
        BuyRow(
            label = candidates.firstOrNull { it.id == selected }?.let { "${it.name} 영입" }
                ?: "캐릭터를 먼저 선택",
            price = DiamondShopEngine.PRICE_TARGET_SUMMON,
            enabled = selected > 0 && state.gems >= DiamondShopEngine.PRICE_TARGET_SUMMON,
            onClick = onBuy
        )
    }
}

/** 🔋 충전 — 에너지 / 성수 */
@Composable
private fun RechargeTab(
    state: TcgGameState,
    onRecharge: (DiamondShopEngine.RechargeKind) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("🌀 에너지 ${state.gateEnergy}/${state.gateEnergyMax}", TcgColors.Mana)
            Spacer(Modifier.width(6.dp))
            Pill("💧 성수 ${state.holyWater}", TcgColors.Heal)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                ActionTile(
                    label = "🌀 에너지 충전",
                    hint = "💎 ${DiamondShopEngine.PRICE_RECHARGE}",
                    accent = TcgColors.Mana,
                    enabled = state.gems >= DiamondShopEngine.PRICE_RECHARGE &&
                        state.gateEnergy < state.gateEnergyMax
                ) { onRecharge(DiamondShopEngine.RechargeKind.GATE_ENERGY) }
            }
            Box(Modifier.weight(1f)) {
                ActionTile(
                    label = "💧 성수 충전",
                    hint = "💎 ${DiamondShopEngine.PRICE_RECHARGE}",
                    accent = TcgColors.Heal,
                    enabled = state.gems >= DiamondShopEngine.PRICE_RECHARGE
                ) { onRecharge(DiamondShopEngine.RechargeKind.HOLY_WATER) }
            }
        }
        Text(
            "🌀 차원 균열 에너지 — 탐색 1회에 쓰고, 이계 전투 승리로 ${TcgGameViewModel.GATE_ENERGY_RECOVERY} 회복됩니다.\n" +
                "💧 성수 — 마왕 정화 1회에 1개, 매 정화마다 매혹·저주 2스택을 지웁니다.",
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            lineHeight = 16.sp
        )
    }
}

/** 🎁 교환 — 대상 캐릭터 1줄 + 강화석 / 친밀도 선물 */
@Composable
private fun ExchangeTab(
    state: TcgGameState,
    target: TcgCard?,
    onSelectTarget: (Int) -> Unit,
    onExchange: (DiamondShopEngine.ExchangeKind) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OwnedPickerRow(
            state = state,
            selectedId = state.diamondShopTargetId,
            onSelectTarget = onSelectTarget
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                target?.let { "#${it.id} ${it.name} · 친밀도 ${it.bondLevel}" } ?: "대상을 고르세요",
                TcgColors.Violet
            )
            Spacer(Modifier.weight(1f))
            Pill("⚒ 조각 ${state.shards.of(state.diamondShopTargetId)}", TcgColors.Gold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                ActionTile(
                    label = "⚒ 강화석 +${DiamondShopEngine.SHARD_GAIN}",
                    hint = "💎 ${DiamondShopEngine.PRICE_EXCHANGE}",
                    accent = TcgColors.Gold,
                    enabled = target != null && state.gems >= DiamondShopEngine.PRICE_EXCHANGE
                ) { onExchange(DiamondShopEngine.ExchangeKind.UPGRADE_SHARD) }
            }
            Box(Modifier.weight(1f)) {
                ActionTile(
                    label = "🎁 친밀도 +${DiamondShopEngine.BOND_GIFT_GAIN}",
                    hint = "💎 ${DiamondShopEngine.PRICE_EXCHANGE}",
                    accent = TcgColors.Hp,
                    enabled = target != null && state.gems >= DiamondShopEngine.PRICE_EXCHANGE
                ) { onExchange(DiamondShopEngine.ExchangeKind.AFFINITY_GIFT) }
            }
        }
        Text(
            "강화석은 그 캐릭터의 강화 재료(조각)로 들어가고, 친밀도 선물은 전투 출전만큼은 아니지만 바로 호감도를 올립니다.",
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            lineHeight = 16.sp
        )
    }
}

/** 🌹 데이트 룩 — 아직 잠긴 보유 캐릭터 1줄 + 해금 버튼 */
@Composable
private fun DateLookTab(
    state: TcgGameState,
    target: TcgCard?,
    onSelectTarget: (Int) -> Unit,
    onUnlock: () -> Unit
) {
    val eligible = remember(state.ownedCards, state.dateLookUnlockedIds) {
        state.ownedCards
            .filter { card ->
                !state.isDateLookUnlockedByGem(card.id) && !AffinityRules.isDateUnlocked(card.bondLevel)
            }
            .sortedBy { it.id }
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (eligible.isEmpty()) {
            Text(
                "데이트 룩이 잠긴 캐릭터가 없습니다. (전원 해금 또는 친밀도 ${AffinityRules.DATE_UNLOCK_BOND} 이상)",
                color = TcgColors.Muted,
                fontSize = TcgType.BODY.sp
            )
            return@Column
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(eligible, key = { it.id }) { card ->
                PortraitIconTile(
                    cardId = card.id,
                    visual = VisualState.ROMANTIC_DATE,
                    modifier = Modifier.width(74.dp),
                    label = card.name.take(4),
                    badge = "💎",
                    selected = card.id == state.diamondShopTargetId,
                    accent = elementColor(card.element),
                    contentDescription = card.name,
                    onClick = { onSelectTarget(card.id) }
                )
            }
        }
        Text(
            target?.let { "#${it.id} ${it.name} — 친밀도 ${it.bondLevel}/100" } ?: "해금할 캐릭터를 고르세요",
            color = TcgColors.Text,
            fontSize = TcgType.BODY.sp,
            fontWeight = FontWeight.Bold
        )
        ActionTile(
            label = "🌹 데이트 룩 즉시 해금",
            hint = "💎 ${DiamondShopEngine.PRICE_DATE_LOOK}",
            accent = TcgColors.Hp,
            enabled = target != null && state.gems >= DiamondShopEngine.PRICE_DATE_LOOK
        ) { onUnlock() }
        Text(
            "친밀도 ${AffinityRules.DATE_UNLOCK_BOND} 없이도 열리며, 한 번 열면 저장됩니다(로비·도감·덱 모두 적용).",
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            lineHeight = 16.sp
        )
    }
}

/** 보유 캐릭터 선택 1줄 (가로 스크롤) */
@Composable
private fun OwnedPickerRow(
    state: TcgGameState,
    selectedId: Int,
    onSelectTarget: (Int) -> Unit
) {
    val owned = remember(state.ownedCards) { state.ownedCards.sortedBy { it.id } }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
    ) {
        items(owned, key = { it.id }) { card ->
            PortraitIconTile(
                cardId = card.id,
                visual = VisualState.BATTLE,
                modifier = Modifier.width(70.dp),
                label = card.name.take(4),
                selected = card.id == selectedId,
                accent = elementColor(card.element),
                contentDescription = card.name,
                onClick = { onSelectTarget(card.id) }
            )
        }
    }
}

/** 하단 구매 버튼 줄 — 가격을 항상 함께 보여 준다 */
@Composable
private fun BuyRow(
    label: String,
    price: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ActionTile(
        label = label,
        hint = "💎 $price",
        accent = TcgColors.Mana,
        enabled = enabled,
        onClick = onClick
    )
}
