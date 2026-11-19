package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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

/**
 * ⚒ 성장 화면 — 2026-11-16 사용자 요청(실기기 스크린샷 25050) 전면 개편.
 *
 * 이전 화면은 캐릭터 일러스트 없이 **텍스트·숫자 박스만** 늘어서 있어
 * 게임처럼 보이지 않았고, 9~10sp 본문이 읽히지 않았다. 이제
 *  ① 상단에 **선택 캐릭터의 2:3 풀 일러스트 카드** (별 등급·속성 문장·강화 게이지)
 *  ② 탭(카드 강화 / 연성·초월 / 장비)으로 메뉴를 갈라 한 화면에 다 담는다
 *  ③ 장비는 슬롯별 아이콘 그래픽 타일로 보여 준다
 *  ④ 하단에 보유 카드 2:3 미니 카드 스트립 (탭 = 성장 대상 변경)
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
    var tabIndex by remember { mutableIntStateOf(GROWTH_TAB_UPGRADE) }
    val card = state.growthCard

    ScreenFrame {
        // ── 헤더: 제목 + 재화 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("성장", color = TcgColors.Text, fontSize = TcgType.TITLE.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Pill("⚔ 덱 ${state.deckTotalPower}", TcgColors.Violet)
        }
        ResourceStrip(state)

        // ── 메뉴 탭 (한 화면에 다 들어가지 않으므로 탭으로 분리) ──
        TcgTabBar(
            tabs = listOf("카드 강화", "연성 · 초월", "장비"),
            selectedIndex = tabIndex,
            accent = TcgColors.Violet,
            onSelect = { tabIndex = it }
        )

        if (card == null) {
            SectionCard(title = "보유 카드 없음", accent = TcgColors.Danger) {
                Text(
                    "성장할 카드가 없습니다. 카드팩 상점에서 동료를 먼저 계약하세요.",
                    color = TcgColors.Muted,
                    fontSize = TcgType.BODY_SMALL.sp,
                    lineHeight = 19.sp
                )
                ActionTile("🛒 카드팩 상점", "신규 동료 계약", TcgColors.Gold, onClick = onOpenShop)
            }
            return@ScreenFrame
        }

        // ── ① 선택 캐릭터 2:3 풀 일러스트 프리뷰 ──
        GrowthHeroPreview(state, card)

        when (tabIndex) {
            GROWTH_TAB_UPGRADE -> UpgradeSection(state, card, onUpgrade, onOpenShop)
            GROWTH_TAB_SYNTHESIS -> SynthesisSection(state, card, onCraft, onTranscend)
            else -> EquipmentSection(state, card, onBuyEquipment, onEquip, onUnequip)
        }

        // ── 성장할 동료 — 2:3 썸네일 그리드 (2026-11-19 개편) ──
        // 예전에는 가로 스크롤 1줄이라 177명 중 뒤쪽 캐릭터를 찾기 어려웠다.
        // 이제 화면 폭에 맞춰 열 수가 늘어나는 그리드 + 이름순/전투력순 정렬을 쓴다.
        SectionCard(title = "성장할 동료", badge = "${state.ownedCards.size}명", accent = TcgColors.Abyss) {
            var sortByPower by remember { mutableStateOf(true) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipButton("전투력순", sortByPower, TcgColors.Violet) { sortByPower = true }
                ChipButton("번호순", !sortByPower, TcgColors.Muted) { sortByPower = false }
            }
            val roster = remember(state.ownedCards, sortByPower) {
                if (sortByPower) {
                    state.ownedCards.sortedWith(compareByDescending<TcgCard> { it.atk + it.def }.thenBy { it.id })
                } else {
                    state.ownedCards.sortedBy { it.id }
                }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = ResponsiveLayout.gridColumns(maxWidth.value)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (maxWidth.value >= 600f) 300.dp else 248.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(roster, key = { it.id }) { owned ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DuelCardTile(
                                cardId = owned.id,
                                visual = owned.currentVisual,
                                element = owned.element,
                                starLevel = owned.starLevel,
                                power = (owned.atk + owned.def) / 100,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(CardLayoutRules.TILE_ASPECT),
                                highlighted = owned.id == card.id,
                                targetWidthPx = 200,
                                onClick = { onSelectCard(owned.id) }
                            )
                            Text(
                                "Lv.${owned.upgradeLevel + 1}",
                                color = if (owned.id == card.id) TcgColors.Gold else TcgColors.Muted,
                                fontSize = TcgType.TILE_MARK.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (state.growthMessage != null) {
            SectionCard(title = "결과", accent = TcgColors.Heal) {
                Text(
                    state.growthMessage,
                    color = TcgColors.Text.copy(alpha = 0.92f),
                    fontSize = TcgType.BODY_SMALL.sp,
                    lineHeight = 19.sp
                )
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

private const val GROWTH_TAB_UPGRADE = 0
private const val GROWTH_TAB_SYNTHESIS = 1
private const val GROWTH_TAB_EQUIPMENT = 2

/** 상단 재화 4종 — 좁은 화면에서는 자동으로 두 줄이 된다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceStrip(state: TcgGameState) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Pill("🪙 ${state.gold}G", TcgColors.Gold)
        Pill("🧩 조각 ${state.shardTotal}", TcgColors.Abyss)
        Pill("🎟 ${state.packTickets}", TcgColors.Mana)
        Pill("Lv.${state.playerLevel}", TcgColors.Heal)
        Pill("👥 ${state.ownedCards.size}명", TcgColors.Violet)
    }
}

/**
 * 선택 캐릭터 프리뷰 — 왼쪽 2:3 일러스트, 오른쪽 등급/스탯/게이지.
 * 일러스트는 잘리지 않고 카드 안에 꼭 맞는다([CardLayoutRules.ART_FIT]).
 */
@Composable
private fun GrowthHeroPreview(state: TcgGameState, card: TcgCard) {
    val level = card.upgradeLevel + 1
    val record = com.game.tcg.core.model.CardCatalog.find(card.id)
    val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, state.equipment)
    val myShards = state.shards.of(card.id)
    val inParty = state.party.any { it.card.id == card.id }
    val accent = elementColor(card.element)
    val equipped = EquipmentEngine.equippedFor(card.id, state.equipment)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.26f), TcgColors.Surface, TcgColors.SurfaceAlt)
                )
            )
            .border(1.5.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2:3 카드 일러스트
        DuelCardTile(
            cardId = card.id,
            visual = card.currentVisual,
            element = card.element,
            starLevel = card.starLevel,
            power = (card.atk + card.def) / 100,
            modifier = Modifier
                .width(126.dp)
                .aspectRatio(CardLayoutRules.TILE_ASPECT),
            targetWidthPx = 320
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                record?.fullName ?: card.name,
                color = TcgColors.Text,
                fontSize = TcgType.SECTION.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 23.sp
            )
            // 별 등급
            Text(
                "★".repeat(card.starLevel) + "☆".repeat((5 - card.starLevel).coerceAtLeast(0)),
                color = TcgColors.Gold,
                fontSize = TcgType.BODY.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ElementBadge(card.element, 22.dp)
                Pill(card.element.displayName, accent)
                Pill("코스트 ${card.baseCost}", TcgColors.Violet)
            }
            // 스탯 3종 그래픽 박스
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatBox("ATK", "${card.atk}", TcgColors.Hp, Modifier.weight(1f))
                StatBox("DEF", "${card.def}", TcgColors.Mana, Modifier.weight(1f))
                StatBox("HP", "$maxHp", TcgColors.Heal, Modifier.weight(1f))
            }
            // 강화 게이지
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "강화 Lv.$level / ${CardUpgradeEngine.MAX_LEVEL}",
                    color = TcgColors.Gold,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text("🧩 $myShards", color = TcgColors.Abyss, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
            }
            StatBar(
                ratio = level / CardUpgradeEngine.MAX_LEVEL.toFloat(),
                color = TcgColors.Gold,
                height = 10
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill(if (inParty) "출전 중" else "대기", if (inParty) TcgColors.Heal else TcgColors.Muted)
                if (record?.isDemonLord == true) Pill("마왕", TcgColors.Danger, filled = true)
                if (equipped.isNotEmpty()) Pill("장비 ${equipped.size}/3", TcgColors.Mana)
            }
        }
    }
}

/** 강화 섹션 — 도감 화면의 [강화] 서브 탭에서도 그대로 재사용한다 (2026-11-19) */
@Composable
internal fun UpgradeSection(
    state: TcgGameState,
    card: TcgCard,
    onUpgrade: (Int) -> Unit,
    onOpenShop: () -> Unit
) {
    val cost = CardUpgradeEngine().cost(card)
    val level = card.upgradeLevel + 1
    val myShards = state.shards.of(card.id)

    SectionCard(
        title = "카드 강화",
        badge = "${card.starLevel}★ · Lv.$level",
        accent = TcgColors.Gold
    ) {
        Text(
            "스킬 · ${card.skillText}",
            color = TcgColors.Text.copy(alpha = 0.9f),
            fontSize = TcgType.BODY_SMALL.sp,
            lineHeight = 19.sp
        )
        if (cost == null) {
            Text(
                "최대 레벨(Lv.${CardUpgradeEngine.MAX_LEVEL})입니다.",
                color = TcgColors.Gold,
                fontSize = TcgType.BODY.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            val goldOk = state.gold >= cost.gold
            val shardOk = myShards >= cost.shards
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("필요 ${cost.gold}G", TcgColors.Gold)
                Pill("필요 조각 ${cost.shards}", TcgColors.Abyss)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("보유 ${state.gold}G", if (goldOk) TcgColors.Heal else TcgColors.Danger)
                Pill("보유 조각 $myShards", if (shardOk) TcgColors.Heal else TcgColors.Danger)
            }
            StatBar(
                ratio = level / CardUpgradeEngine.MAX_LEVEL.toFloat(),
                color = TcgColors.Gold,
                height = 10
            )
            Text(
                "Lv.$level → Lv.${level + 1} · 레벨업마다 원본 ATK/DEF 의 " +
                    "${CardUpgradeEngine.GROWTH_PERCENT}% 상승 — 덱 전투력·전장 HP 에 즉시 반영",
                color = TcgColors.Muted,
                fontSize = TcgType.BODY_SMALL.sp,
                lineHeight = 19.sp
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
            fontSize = TcgType.LABEL.sp,
            lineHeight = 17.sp
        )
    }
}

/** 합성·초월 섹션 — 도감 화면의 [합성] 서브 탭에서도 그대로 재사용한다 (2026-11-19) */
@Composable
internal fun SynthesisSection(
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
        title = "초월 · ${card.name}",
        badge = "${card.starLevel}★",
        accent = TcgColors.Gold
    ) {
        val myShards = state.shards.of(card.id)
        val canTranscend = card.starLevel < 5 &&
            myShards >= AdvancedSynthesisEngine.TRANSCEND_SHARDS &&
            state.gold >= AdvancedSynthesisEngine.TRANSCEND_GOLD
        Text(
            "같은 카드의 조각 ${AdvancedSynthesisEngine.TRANSCEND_SHARDS}개 + " +
                "${AdvancedSynthesisEngine.TRANSCEND_GOLD}G → 성급 +1 (ATK/DEF +20%, 최대 5★)",
            color = TcgColors.Muted,
            fontSize = TcgType.BODY_SMALL.sp,
            lineHeight = 19.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("보유 조각 $myShards", TcgColors.Abyss)
            Pill("보유 ${state.gold}G", TcgColors.Gold)
            Pill("${card.starLevel}★ → ${(card.starLevel + 1).coerceAtMost(5)}★", TcgColors.Violet)
        }
        ActionTile(
            label = if (card.starLevel >= 5) "5★ 최대치" else "🧪 초월 실행",
            hint = if (card.starLevel >= 5) "더 올릴 수 없습니다" else {
                "${AdvancedSynthesisEngine.TRANSCEND_GOLD}G · 조각 -${AdvancedSynthesisEngine.TRANSCEND_SHARDS}"
            },
            accent = TcgColors.Gold,
            enabled = canTranscend,
            onClick = { onTranscend(card.id) }
        )
    }

    SectionCard(
        title = "연성 — 조각으로 확정 계약",
        badge = "가능 ${available.size}",
        accent = TcgColors.Abyss
    ) {
        Text(
            "조각 ${AdvancedSynthesisEngine.CRAFT_SHARDS}개 + ${AdvancedSynthesisEngine.CRAFT_GOLD}G → " +
                "미보유 캐릭터 1명 확정 계약 (5코스트 이상 · 마왕 제외)",
            color = TcgColors.Muted,
            fontSize = TcgType.BODY_SMALL.sp,
            lineHeight = 19.sp
        )
        if (available.isEmpty() && locked.isEmpty()) {
            Text(
                "연성할 수 있는 미보유 캐릭터가 없습니다.",
                color = TcgColors.Muted,
                fontSize = TcgType.BODY_SMALL.sp
            )
        }
        available.take(6).forEach { target -> CraftRow(target, unlocked = true, onCraft = onCraft) }
        if (available.size > 6) {
            Text(
                "… 그 외 ${available.size - 6}명 (조각을 더 모으면 목록이 늘어납니다)",
                color = TcgColors.Muted,
                fontSize = TcgType.LABEL.sp
            )
        }
        val nextLocked = locked.firstOrNull()
        if (available.isEmpty() && nextLocked != null) {
            Text(
                "다음 목표 · ${nextLocked.label} — ${nextLocked.blockedReason}",
                color = TcgColors.Gold,
                fontSize = TcgType.LABEL.sp,
                lineHeight = 17.sp
            )
        }
    }

    val midBossTargets = targets.filter { it.kind == CraftKind.MID_BOSS }
    SectionCard(
        title = "심연 연성 — 중간 보스",
        badge = "No.170~175",
        accent = TcgColors.Danger
    ) {
        Text(
            "챕터 마일스톤(5·10·15·20·25·28)의 중간 보스를 격파하면 조각 " +
                "${AdvancedSynthesisEngine.MID_BOSS_SHARDS}개 + ${AdvancedSynthesisEngine.MID_BOSS_GOLD}G 로 계약할 수 있습니다.",
            color = TcgColors.Muted,
            fontSize = TcgType.BODY_SMALL.sp,
            lineHeight = 19.sp
        )
        midBossTargets.forEach { target -> CraftRow(target, unlocked = target.isAvailable, onCraft = onCraft) }
    }
}

@Composable
private fun CraftRow(target: CraftTarget, unlocked: Boolean, onCraft: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (target.isAvailable) TcgColors.Abyss.copy(alpha = 0.14f) else TcgColors.SurfaceAlt)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 후보 캐릭터도 2:3 미니 카드로 — 텍스트 목록 대신 그래픽
        DuelCardTile(
            cardId = target.record.id,
            visual = com.game.tcg.core.model.VisualState.BATTLE,
            element = target.record.element,
            starLevel = 2,
            power = (target.record.atk + target.record.def) / 100,
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(CardLayoutRules.TILE_ASPECT),
            targetWidthPx = 180
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    if (target.kind == CraftKind.MID_BOSS) "보스" else "신규",
                    if (target.kind == CraftKind.MID_BOSS) TcgColors.Danger else TcgColors.Abyss
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    target.record.name,
                    color = TcgColors.Text,
                    fontSize = TcgType.BODY_SMALL.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                if (target.isAvailable) target.record.fullName else (target.blockedReason ?: "잠김"),
                color = if (target.isAvailable) TcgColors.Muted else TcgColors.Gold,
                fontSize = TcgType.LABEL.sp,
                lineHeight = 16.sp
            )
            ActionTile(
                label = if (target.isAvailable) "🧪 ${target.record.name} 연성" else "잠김",
                hint = if (target.isAvailable) target.costLabel else (target.blockedReason ?: "잠김"),
                accent = if (target.kind == CraftKind.MID_BOSS) TcgColors.Danger else TcgColors.Abyss,
                enabled = unlocked && target.isAvailable,
                onClick = { onCraft(target.record.id) }
            )
        }
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
        // 슬롯 3개를 그래픽 타일로 나란히
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EquipmentSlot.entries.forEach { slot ->
                val item = equipped.firstOrNull { it.slot == slot }
                EquipmentSlotTile(
                    slot = slot,
                    item = item,
                    modifier = Modifier.weight(1f),
                    onUnequip = { item?.let { onUnequip(it.uid) } }
                )
            }
        }
        Text(
            "장착 즉시 ATK/DEF 가 오르고, 해제하면 정확히 되돌아갑니다. HP 보너스는 전장 최대 HP 에 더해집니다.",
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            lineHeight = 17.sp
        )
    }

    SectionCard(title = "소지 장비", badge = "${loose.size}개", accent = TcgColors.Violet) {
        if (loose.isEmpty()) {
            Text(
                "미장착 장비가 없습니다. 스테이지 드롭이나 상점 구매로 모으세요.",
                color = TcgColors.Muted,
                fontSize = TcgType.BODY_SMALL.sp
            )
        }
        loose.forEach { item ->
            ItemRow(item, actionLabel = "${item.slot.icon} 장착", accent = TcgColors.Heal) {
                onEquip(item.uid, card.id)
            }
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
            fontSize = TcgType.LABEL.sp,
            lineHeight = 17.sp
        )
    }
}

/** 장비 슬롯 1칸 — 아이콘 그래픽 + 등급 별 + 장착 아이템 */
@Composable
private fun EquipmentSlotTile(
    slot: EquipmentSlot,
    item: EquipmentItem?,
    modifier: Modifier = Modifier,
    onUnequip: () -> Unit
) {
    val accent = when (slot) {
        EquipmentSlot.WEAPON -> TcgColors.Hp
        EquipmentSlot.ARMOR -> TcgColors.Mana
        EquipmentSlot.ACCESSORY -> TcgColors.Gold
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (item != null) {
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.32f), TcgColors.SurfaceAlt))
                } else {
                    Brush.verticalGradient(listOf(TcgColors.SurfaceAlt, TcgColors.Surface))
                }
            )
            .border(1.5.dp, if (item != null) accent else TcgColors.Line, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .then(if (item != null) Modifier.clickable { onUnequip() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = if (item != null) 0.85f else 0.18f))
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(slot.icon, fontSize = 19.sp)
        }
        Text(
            slot.displayName,
            color = TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            item?.name ?: "비어 있음",
            color = if (item != null) TcgColors.Text else TcgColors.Muted.copy(alpha = 0.7f),
            fontSize = TcgType.LABEL.sp,
            fontWeight = if (item != null) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            lineHeight = 15.sp
        )
        if (item != null) {
            Text(item.rarityStars, color = accent, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
            Text("탭 = 해제", color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp)
        }
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
            .clip(RoundedCornerShape(12.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${item.slot.icon} ${item.name} ${item.rarityStars}",
                color = TcgColors.Text,
                fontSize = TcgType.BODY_SMALL.sp,
                fontWeight = FontWeight.Bold
            )
            Text(item.statSummary, color = TcgColors.Heal, fontSize = TcgType.LABEL.sp)
        }
        ChipButton(actionLabel, false, accent) { if (enabled) onClick() }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            Pill(trailing, accent)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp)
        Text(
            value,
            color = color,
            fontSize = TcgType.BODY.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
