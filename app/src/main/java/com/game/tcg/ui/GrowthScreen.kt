package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.AdvancedSynthesisEngine
import com.game.tcg.core.engine.CardUpgradeEngine
import com.game.tcg.core.engine.CraftKind
import com.game.tcg.core.engine.CraftTarget
import com.game.tcg.core.engine.EquipmentEngine
import com.game.tcg.core.model.CardRarity
import com.game.tcg.core.model.EquipmentCatalog
import com.game.tcg.core.model.EquipmentItem
import com.game.tcg.core.model.EquipmentSlot
import com.game.tcg.core.model.TcgCard

/** 성장 화면 탭 */
private enum class GrowthTab(val label: String, val icon: String) {
    UPGRADE("카드 강화", "⚡"),
    SYNTHESIS("연성·초월", "🧪"),
    EQUIPMENT("장비", "🛡")
}

/**
 * ⚒ 강화 · 합성 · 장비 — 2026-11-16 사용자 요청 서브 플로우 3종을 한 화면에 담았다.
 *
 * ① **카드 강화**: 골드 + 그 카드의 조각 → Lv.1~10, ATK/DEF가 오르고 덱 전투력에 즉시 반영
 * ② **연성·초월**: 조각을 모아 미보유 캐릭터(5코스트 이상)를 확정 계약 /
 *    중간 보스(No.170~175)는 해당 스테이지 격파 후 「심연 연성」으로 계약 /
 *    보유 카드는 조각으로 성급 초월(최대 5★)
 * ③ **장비**: 무기·방어구·장신구 3슬롯 장착·해제, 상점 구매, 스테이지 드롭 소지품
 */
@Composable
fun GrowthScreen(
    state: TcgGameState,
    onSelectCard: (Int) -> Unit,
    onUpgrade: (Int) -> Unit,
    onCraft: (Int) -> Unit,
    onTranscend: (Int) -> Unit,
    onBuyEquipment: (String) -> Unit,
    onEquip: (String, Int) -> Unit,
    onUnequip: (String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenStage: () -> Unit
) {
    var tab by remember { mutableStateOf(GrowthTab.UPGRADE) }
    val card = state.growthCard

    ScreenFrame {
        SectionCard(
            title = "성장",
            badge = "⚔ 덱 ${state.deckTotalPower}",
            accent = TcgColors.Violet
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("🪙 ${state.gold}G", TcgColors.Gold)
                Pill("🧩 조각 ${state.shardTotal}", TcgColors.Abyss)
                Pill("🎟 ${state.packTickets}", TcgColors.Mana)
                Pill("Lv.${state.playerLevel}", TcgColors.Heal)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GrowthTab.entries.forEach { entry ->
                    ChipButton("${entry.icon} ${entry.label}", tab == entry) { tab = entry }
                }
            }
            // 카드 선택 (보유 카드 가로 스크롤)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.ownedCards.sortedByDescending { it.atk + it.def }.forEach { owned ->
                    ChipButton(
                        label = "${owned.name.take(6)} ${owned.upgradeLevel + 1}Lv",
                        selected = owned.id == card?.id,
                        accent = elementColor(owned.element)
                    ) { onSelectCard(owned.id) }
                }
            }
        }

        if (card == null) {
            SectionCard(title = "보유 카드 없음", accent = TcgColors.Danger) {
                Text("성장할 카드가 없습니다. 카드팩 상점에서 동료를 먼저 계약하세요.", color = TcgColors.Muted, fontSize = 10.sp)
                ActionTile("🛒 카드팩 상점", "신규 동료 계약", TcgColors.Gold, onClick = onOpenShop)
            }
            return@ScreenFrame
        }

        when (tab) {
            GrowthTab.UPGRADE -> UpgradeSection(state, card, onUpgrade, onOpenShop)
            GrowthTab.SYNTHESIS -> SynthesisSection(state, card, onCraft, onTranscend)
            GrowthTab.EQUIPMENT -> EquipmentSection(state, card, onBuyEquipment, onEquip, onUnequip)
        }

        if (state.growthMessage != null) {
            SectionCard(title = "결과", accent = TcgColors.Heal) {
                Text(state.growthMessage, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        ActionTile(
            label = "🗺 스테이지로 돌아가기",
            hint = "다음 스테이지에서 보상과 조각을 더 모으세요",
            accent = TcgColors.Violet,
            onClick = onOpenStage
        )
    }
}

@Composable
private fun UpgradeSection(
    state: TcgGameState,
    card: TcgCard,
    onUpgrade: (Int) -> Unit,
    onOpenShop: () -> Unit
) {
    val cost = CardUpgradeEngine().cost(card)
    val level = card.upgradeLevel + 1
    val record = com.game.tcg.core.model.CardCatalog.find(card.id)
    val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, state.equipment)
    val myShards = state.shards.of(card.id)
    val inParty = state.party.any { it.card.id == card.id }

    SectionCard(
        title = "강화 · ${card.name}",
        badge = "${card.starLevel}★ · Lv.$level",
        accent = elementColor(card.element)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("ATK", "${card.atk}", TcgColors.Hp)
            StatBox("DEF", "${card.def}", TcgColors.Mana)
            StatBox("HP", "$maxHp", TcgColors.Heal)
            StatBox("조각", "$myShards", TcgColors.Abyss)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Pill("코스트 ${card.baseCost}", TcgColors.Violet)
            Pill(card.element.displayName, elementColor(card.element))
            Pill(
                if (inParty) "출전 중" else "대기",
                if (inParty) TcgColors.Heal else TcgColors.Muted
            )
            if (record?.isDemonLord == true) Pill("마왕", TcgColors.Danger, filled = true)
        }
        StatBar(ratio = level / CardUpgradeEngine.MAX_LEVEL.toFloat(), color = TcgColors.Gold, height = 8)
        Text("스킬 · ${card.skillText}", color = TcgColors.Muted, fontSize = 9.sp, lineHeight = 14.sp)

        if (cost == null) {
            Text("최대 레벨(Lv.${CardUpgradeEngine.MAX_LEVEL})입니다.", color = TcgColors.Gold, fontSize = 10.sp)
        } else {
            val goldOk = state.gold >= cost.gold
            val shardOk = myShards >= cost.shards
            Text(
                "Lv.$level → Lv.${level + 1} · ${cost.gold}G + 조각 ${cost.shards}개 " +
                    "(보유 ${state.gold}G · 조각 $myShards)",
                color = if (goldOk && shardOk) TcgColors.Heal else TcgColors.Danger,
                fontSize = 10.sp
            )
            Text(
                "레벨업마다 원본 ATK/DEF의 ${CardUpgradeEngine.GROWTH_PERCENT}% 상승 — 덱 전투력·전장 HP에 즉시 반영",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
            ActionTile(
                label = "⚡ 강화 실행",
                hint = if (goldOk && shardOk) "${cost.gold}G · 조각 -${cost.shards}" else "골드 또는 조각 부족",
                accent = TcgColors.Gold,
                enabled = goldOk && shardOk,
                onClick = { onUpgrade(card.id) }
            )
            if (!shardOk) {
                ActionTile(
                    label = "🛒 조각 모으기 — 카드팩 개봉",
                    hint = "중복 카드가 조각으로 변환됩니다",
                    accent = TcgColors.Violet,
                    onClick = onOpenShop
                )
            }
        }
        Text(
            "강화 레벨은 v0.3 로그라이크 규칙에 따라 전투 중 사망하면 초기화됩니다.",
            color = TcgColors.Muted,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun SynthesisSection(
    state: TcgGameState,
    card: TcgCard,
    onCraft: (Int) -> Unit,
    onTranscend: (Int) -> Unit
) {
    // 연성 후보 계산은 카드 170명을 훑으므로 상태가 바뀔 때만 다시 만든다
    val targets = remember(state.ownedCardIds, state.stageCleared, state.shards, state.gold) {
        state.craftTargets
    }
    val available = targets.filter { it.isAvailable }
    val locked = targets.filterNot { it.isAvailable }

    SectionCard(
        title = "연성 — 조각으로 확정 계약",
        badge = "가능 ${available.size}",
        accent = TcgColors.Abyss
    ) {
        Text(
            "카드팩 중복으로 모은 조각 ${AdvancedSynthesisEngine.CRAFT_SHARDS}개 + " +
                "${AdvancedSynthesisEngine.CRAFT_GOLD}G → 미보유 캐릭터 1명 확정 계약. " +
                "(5코스트 이상 · 마왕 제외)",
            color = TcgColors.Muted,
            fontSize = 9.sp,
            lineHeight = 14.sp
        )
        if (available.isEmpty() && locked.isEmpty()) {
            Text("연성할 수 있는 미보유 캐릭터가 없습니다.", color = TcgColors.Muted, fontSize = 10.sp)
        }
        available.take(6).forEach { target ->
            CraftRow(target, unlocked = true, onCraft = onCraft)
        }
        if (available.size > 6) {
            Text("… 그 외 ${available.size - 6}명 (조각을 더 모으면 목록이 늘어납니다)", color = TcgColors.Muted, fontSize = 9.sp)
        }
        val nextLocked = locked.firstOrNull()
        if (available.isEmpty() && nextLocked != null) {
            Text("다음 목표 · ${nextLocked.label} — ${nextLocked.blockedReason}", color = TcgColors.Gold, fontSize = 9.sp)
        }
    }

    // 중간 보스 심연 연성
    val midBossTargets = targets.filter { it.kind == CraftKind.MID_BOSS }
    SectionCard(
        title = "심연 연성 — 중간 보스",
        badge = "No.170~175",
        accent = TcgColors.Danger
    ) {
        Text(
            "챕터 마일스톤(5·10·15·20·25·28)의 중간 보스를 격파하면 " +
                "조각 ${AdvancedSynthesisEngine.MID_BOSS_SHARDS}개 + ${AdvancedSynthesisEngine.MID_BOSS_GOLD}G로 계약할 수 있습니다.",
            color = TcgColors.Muted,
            fontSize = 9.sp,
            lineHeight = 14.sp
        )
        midBossTargets.forEach { target ->
            CraftRow(target, unlocked = target.isAvailable, onCraft = onCraft)
        }
    }

    // 초월
    val myShards = state.shards.of(card.id)
    val canTranscend = card.starLevel < 5 &&
        myShards >= AdvancedSynthesisEngine.TRANSCEND_SHARDS &&
        state.gold >= AdvancedSynthesisEngine.TRANSCEND_GOLD
    SectionCard(
        title = "초월 · ${card.name}",
        badge = "${card.starLevel}★",
        accent = TcgColors.Gold
    ) {
        Text(
            "같은 카드의 조각 ${AdvancedSynthesisEngine.TRANSCEND_SHARDS}개 + " +
                "${AdvancedSynthesisEngine.TRANSCEND_GOLD}G → 성급 +1 (ATK/DEF +20%, 최대 5★). " +
                "보유 조각 " + myShards + "개.",
            color = TcgColors.Muted,
            fontSize = 9.sp,
            lineHeight = 14.sp
        )
        ActionTile(
            label = if (card.starLevel >= 5) "5★ 최대치" else "🧪 초월 실행 (${card.starLevel}★ → ${card.starLevel + 1}★)",
            hint = if (card.starLevel >= 5) "더 올릴 수 없습니다" else {
                "${AdvancedSynthesisEngine.TRANSCEND_GOLD}G · 조각 -${AdvancedSynthesisEngine.TRANSCEND_SHARDS}"
            },
            accent = TcgColors.Gold,
            enabled = canTranscend,
            onClick = { onTranscend(card.id) }
        )
    }
}

@Composable
private fun CraftRow(target: CraftTarget, unlocked: Boolean, onCraft: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (target.isAvailable) TcgColors.Abyss.copy(alpha = 0.12f) else TcgColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(if (target.kind == CraftKind.MID_BOSS) "보스" else "신규", if (target.kind == CraftKind.MID_BOSS) TcgColors.Danger else TcgColors.Abyss)
            Spacer(Modifier.width(6.dp))
            Text(target.record.name, color = TcgColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Pill(target.rarity.displayName, Color(target.rarity.color))
            Spacer(Modifier.weight(1f))
            Text(target.costLabel, color = TcgColors.Muted, fontSize = 9.sp)
        }
        Text(
            if (target.isAvailable) target.record.fullName else "${target.blockedReason}",
            color = if (target.isAvailable) TcgColors.Muted else TcgColors.Gold,
            fontSize = 9.sp
        )
        ActionTile(
            label = if (target.isAvailable) "🧪 ${target.record.name} 연성" else "잠김",
            hint = if (target.isAvailable) target.costLabel else target.blockedReason,
            accent = if (target.kind == CraftKind.MID_BOSS) TcgColors.Danger else TcgColors.Abyss,
            enabled = unlocked && target.isAvailable,
            onClick = { onCraft(target.record.id) }
        )
    }
}

@Composable
private fun EquipmentSection(
    state: TcgGameState,
    card: TcgCard,
    onBuyEquipment: (String) -> Unit,
    onEquip: (String, Int) -> Unit,
    onUnequip: (String) -> Unit
) {
    val equipped = EquipmentEngine.equippedFor(card.id, state.equipment)
    val loose = state.equipment.filter { it.equippedTo == null }

    SectionCard(
        title = "장비 슬롯 · ${card.name}",
        badge = "${equipped.size}/${EquipmentEngine.MAX_PER_CARD}",
        accent = TcgColors.Mana
    ) {
        EquipmentSlot.entries.forEach { slot ->
            val item = equipped.firstOrNull { it.slot == slot }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TcgColors.SurfaceAlt)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${slot.icon} ${slot.displayName}", color = TcgColors.Muted, fontSize = 11.sp, modifier = Modifier.width(74.dp))
                if (item == null) {
                    Text("비어 있음", color = TcgColors.Muted, fontSize = 10.sp)
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.name} ${item.rarityStars}", color = TcgColors.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.statSummary, color = TcgColors.Heal, fontSize = 9.sp)
                    }
                    ChipButton("해제", false, TcgColors.Danger) { onUnequip(item.uid) }
                }
            }
        }
        Text(
            "장착 즉시 ATK/DEF가 오르고, 해제하면 정확히 되돌아갑니다. HP 보너스는 전장 최대 HP에 더해집니다.",
            color = TcgColors.Muted,
            fontSize = 9.sp,
            lineHeight = 14.sp
        )
    }

    SectionCard(title = "소지 장비", badge = "${loose.size}개", accent = TcgColors.Violet) {
        if (loose.isEmpty()) {
            Text("미장착 장비가 없습니다. 스테이지 드롭이나 상점 구매로 모으세요.", color = TcgColors.Muted, fontSize = 10.sp)
        }
        loose.forEach { item ->
            ItemRow(item, actionLabel = "장착", accent = TcgColors.Heal) { onEquip(item.uid, card.id) }
        }
    }

    SectionCard(title = "장비 상점", badge = "골드 구매", accent = TcgColors.Gold) {
        EquipmentCatalog.SHOP_ITEMS.forEach { shopItem ->
            val affordable = state.gold >= shopItem.goldPrice
            ItemRow(
                item = shopItem,
                actionLabel = "${shopItem.goldPrice}G",
                accent = if (affordable) TcgColors.Gold else TcgColors.Muted,
                enabled = affordable,
                trailing = "구매"
            ) { onBuyEquipment(shopItem.id) }
        }
        Text(
            "전설 장비(심연의 대검·황혼의 성갑·마왕의 인장)는 스테이지 보스 드롭 전용입니다.",
            color = TcgColors.Muted,
            fontSize = 9.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun ItemRow(
    item: EquipmentItem,
    actionLabel: String,
    accent: Color,
    enabled: Boolean = true,
    trailing: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${item.slot.icon} ${item.name} ${item.rarityStars}",
                color = TcgColors.Text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(item.statSummary, color = TcgColors.Muted, fontSize = 9.sp)
        }
        ChipButton(actionLabel, false, accent) { if (enabled) onClick() }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            Pill(trailing, accent)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = TcgColors.Muted, fontSize = 8.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
