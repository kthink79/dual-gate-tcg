package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.BalanceRules
import com.game.tcg.core.engine.BattleSimulator
import com.game.tcg.core.engine.StageEngine
import com.game.tcg.core.engine.StageReadiness
import com.game.tcg.core.model.BattleLineup
import com.game.tcg.core.model.BossKind
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.StageDefinition

/**
 * 🗺 스테이지 캠페인 — **그래픽 중심** 재구성 (2026-11-17 사용자 요청).
 *
 * 사용자 원문:
 * > "스테이지 메뉴의 과도한 텍스트 설명·목록을 대폭 줄이고 그래픽·아이콘·배지·진행도 바 중심으로 정돈.
 * >  중간 보스·보스 카드를 2:3 세로 비율(ContentScale.Fit)로 변경해 몸통만 잘리지 않고
 * >  얼굴·전신이 온전히 보이도록 수정."
 *
 * 화면 구성 (위 → 아래)
 *  1. **캠페인 진행 바** — 30스테이지 중 몇 개를 깼는지 한 줄로
 *  2. **챕터 타일 6개** — 아이콘 + `3/5` 도트만 (긴 챕터 이름 없음)
 *  3. **스테이지 타일 5개** — 번호 · 상태 아이콘(✓/🔒/▸) · 속성 점
 *  4. **선택 스테이지 카드** — 보스/적 일러스트를 **2:3 세로 + Fit** 으로 크게,
 *     그 옆에 속성·상성(▲▼)·적 HP/ATK 막대·승률 고리만 남긴다
 *  5. **출전 버튼 + 아이콘 3개** (덱 · 강화 · 상점)
 */
@Composable
fun StageScreen(
    state: TcgGameState,
    onSelectStage: (Int) -> Unit,
    onStartStage: (Int) -> Unit,
    onOpenDeck: () -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenShop: () -> Unit,
    onReclaimLost: () -> Unit = {},
    onClaimSupply: () -> Unit = {}
) {
    val selected = state.selectedStageDefinition
    val chapter = selected.chapter
    val stages = StageCatalog.chapter(chapter)
    val readiness = StageEngine.readiness(state.deckTotalPower, selected)
    val unlocked = state.isStageUnlocked(selected.number)
    val advantage = state.advantageShareAgainst(selected)
    // 실제 덱으로 머리 없는 전투를 돌려 승률을 구한다 (상성·강화·성급이 그대로 반영된다)
    val winChance = remember(state.party, selected.number) {
        BattleSimulator.winChance(state.equippedCards, selected)
    }

    ScreenFrame {
        // ── 소프트락 방지 구제 패널 (필요할 때만, 한 줄 요약) ──
        RescueStrip(
            state = state,
            onReclaimLost = onReclaimLost,
            onClaimSupply = onClaimSupply,
            onOpenDeck = onOpenDeck
        )

        // ── ① 캠페인 진행 바 ──
        CampaignProgress(
            cleared = state.stageCleared.size,
            total = StageCatalog.TOTAL_STAGES,
            gold = state.gold,
            tickets = state.packTickets,
            shards = state.shardTotal,
            deckPower = state.deckTotalPower
        )

        // ── ② 챕터 타일 ──
        ChapterTiles(
            current = chapter,
            clearedStages = state.stageCleared,
            onSelectChapter = { chapterNumber ->
                val first = StageCatalog.chapter(chapterNumber).first()
                onSelectStage(
                    StageCatalog.chapter(chapterNumber).firstOrNull { it.number !in state.stageCleared }?.number
                        ?: first.number
                )
            }
        )

        // ── ③ 스테이지 타일 5개 ──
        SectionCard(
            title = "${chapter}장 ${StageCatalog.CHAPTER_NAMES[chapter - 1]}",
            badge = "${stages.count { it.number in state.stageCleared }}/${stages.size}",
            accent = chapterAccent(chapter)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                stages.forEach { stage ->
                    Box(modifier = Modifier.weight(1f)) {
                        StageTile(
                            stage = stage,
                            isCleared = stage.number in state.stageCleared,
                            isUnlocked = state.isStageUnlocked(stage.number),
                            isSelected = stage.number == selected.number,
                            onClick = { onSelectStage(stage.number) }
                        )
                    }
                }
            }
        }

        // ── ④ 선택 스테이지 카드 ──
        StageDetailCard(
            state = state,
            stage = selected,
            unlocked = unlocked,
            readiness = readiness,
            advantage = advantage,
            winChance = winChance,
            onStart = { onStartStage(selected.number) }
        )

        // ── ⑤ 아이콘 버튼 3개 ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                IconActionTile("📜", "덱 편성", "${state.party.size}/${state.unlockedDeckSize}", TcgColors.Mana, onOpenDeck)
            }
            Box(modifier = Modifier.weight(1f)) {
                IconActionTile("⚒", "강화", "전투력 보강", TcgColors.Violet, onOpenGrowth)
            }
            Box(modifier = Modifier.weight(1f)) {
                IconActionTile("🛒", "카드팩", "🎟 ${state.packTickets}", TcgColors.Gold, onOpenShop)
            }
        }
    }
}

/* ─────────────────────────── ① 캠페인 진행 ─────────────────────────── */

@Composable
private fun CampaignProgress(cleared: Int, total: Int, gold: Int, tickets: Int, shards: Int, deckPower: Int) {
    val ratio = if (total <= 0) 0f else cleared.toFloat() / total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF241C33), Color(0xFF141020)))
            )
            .border(1.dp, TcgColors.Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🗺", fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "캠페인 $cleared/$total",
                color = TcgColors.Text,
                fontSize = TcgType.SECTION.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Pill("⚔ ${deckPower}", TcgColors.Heal)
            Spacer(Modifier.width(4.dp))
            Pill("🪙 ${gold}", TcgColors.Gold)
        }
        StatBar(ratio = ratio, color = TcgColors.Gold, height = 9)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Pill("🎟 $tickets", TcgColors.Mana)
            Pill("🧩 $shards", TcgColors.Abyss)
            Spacer(Modifier.weight(1f))
            Text(
                "${(ratio * 100).toInt()}%",
                color = TcgColors.Gold,
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ─────────────────────────── ② 챕터 타일 ─────────────────────────── */

private val CHAPTER_ICONS = listOf("🌫", "🏰", "❄", "🌪", "🌇", "👑")

@Composable
private fun ChapterTiles(
    current: Int,
    clearedStages: Set<Int>,
    onSelectChapter: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StageCatalog.CHAPTER_NAMES.forEachIndexed { index, name ->
            val number = index + 1
            val cleared = StageCatalog.chapter(number).count { it.number in clearedStages }
            val total = StageCatalog.chapter(number).size
            val acc = chapterAccent(number)
            val selected = number == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) acc.copy(alpha = 0.20f) else TcgColors.SurfaceAlt)
                    .border(1.dp, if (selected) acc else TcgColors.Line, RoundedCornerShape(12.dp))
                    .clickable { onSelectChapter(number) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(CHAPTER_ICONS[index], fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${number}장 $name",
                    color = if (selected) TcgColors.Text else TcgColors.Muted,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // 5칸 도트 — 어디까지 깼는지 한눈에
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(total) { i ->
                        val done = i < cleared
                        Box(
                            modifier = Modifier
                                .size(if (done) 8.dp else 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (done) acc else TcgColors.Line)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "$cleared/$total",
                    color = if (cleared == total) TcgColors.Heal else TcgColors.Muted,
                    fontSize = TcgType.TILE_MARK.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ─────────────────────────── ③ 스테이지 타일 ─────────────────────────── */

@Composable
private fun StageTile(
    stage: StageDefinition,
    isCleared: Boolean,
    isUnlocked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = when {
        !isUnlocked -> TcgColors.Line
        stage.bossKind == BossKind.DEMON_LORD -> TcgColors.Danger
        stage.bossKind == BossKind.MID_BOSS -> TcgColors.Gold
        isCleared -> TcgColors.Heal
        else -> TcgColors.Violet
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accent.copy(alpha = 0.22f) else TcgColors.SurfaceAlt)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else TcgColors.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = if (isUnlocked) 0.22f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when {
                    !isUnlocked -> "🔒"
                    isCleared -> "✓"
                    stage.bossKind == BossKind.DEMON_LORD -> "👑"
                    stage.bossKind == BossKind.MID_BOSS -> "☠"
                    else -> "▸"
                },
                fontSize = 13.sp,
                color = if (isUnlocked) accent else TcgColors.Muted
            )
        }
        Text(
            "${stage.number}",
            color = if (isUnlocked) TcgColors.Text else TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(elementColor(stage.enemyElement))
        )
    }
}

/* ─────────────────────────── ④ 선택 스테이지 상세 ─────────────────────────── */

@Composable
private fun StageDetailCard(
    state: TcgGameState,
    stage: StageDefinition,
    unlocked: Boolean,
    readiness: StageReadiness,
    advantage: Double,
    winChance: Int,
    onStart: () -> Unit
) {
    val accent = if (stage.isBoss) TcgColors.Danger else TcgColors.Violet
    val enemyCardId = remember(stage.number) {
        stage.bossId ?: BattleLineup.enemyLineup(
            enemyElement = stage.enemyElement,
            enemyName = stage.enemyName,
            size = 1
        ).first().cardId
    }
    val edge = BalanceRules.elementEdge(state.frontRowElements, stage.enemyElement)
    val edgeBadge = when {
        edge > 0 -> "▲ 상성 우세" to TcgColors.Heal
        edge < 0 -> "▼ 상성 열세" to TcgColors.Danger
        else -> "= 상성 동등" to TcgColors.Muted
    }

    SectionCard(
        title = "${stage.number}. ${stage.name}",
        badge = when {
            stage.number in state.stageCleared -> "클리어"
            unlocked -> "도전 가능"
            else -> "잠김"
        },
        accent = accent
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // ── 보스/적 일러스트 — 2:3 세로, Fit(얼굴·전신 보존) ──
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .aspectRatio(CardLayoutRules.TILE_ASPECT)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF120E1C))
                    .border(
                        width = if (stage.isBoss) 2.dp else 1.dp,
                        color = if (stage.isBoss) accent else TcgColors.Line,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                CardArtImage(
                    cardId = enemyCardId,
                    visual = com.game.tcg.core.model.VisualState.BATTLE,
                    contentDescription = stage.enemyName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(198.dp),
                    targetWidthPx = 540
                )
                // 상단 배지 — 보스 종류 / 원소
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (stage.isBoss) {
                        Pill(
                            if (stage.bossKind == BossKind.DEMON_LORD) "👑 마왕" else "☠ 보스",
                            if (stage.bossKind == BossKind.DEMON_LORD) TcgColors.Danger else TcgColors.Gold,
                            filled = true
                        )
                    }
                    Pill(stage.enemyElement.displayName, elementColor(stage.enemyElement), filled = true)
                }
            }
            Spacer(Modifier.width(10.dp))

            // ── 수치만 남긴 정보판 ──
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stage.enemyName,
                    color = TcgColors.Text,
                    fontSize = TcgType.BODY_SMALL.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Pill(edgeBadge.first, edgeBadge.second)
                    if (advantage >= 0.5) Pill("유리 속성 ${(advantage * 100).toInt()}%", TcgColors.Heal)
                }
                // 적 HP / ATK — 숫자 대신 막대로 비교
                MiniStatBar("적 HP", stage.enemyHp, stage.recommendedPower * 2.0, TcgColors.Hp)
                MiniStatBar("적 ATK", stage.enemyAtk, stage.recommendedPower / 30.0, TcgColors.Danger)
                // 내 덱 대 권장 전투력
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔ ${state.deckTotalPower}", color = readinessColor(readiness), fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
                    Text(" / ${stage.recommendedPower}", color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp)
                    Spacer(Modifier.weight(1f))
                    Pill(readiness.label, readinessColor(readiness), filled = true)
                }
                StatBar(
                    ratio = (state.deckTotalPower.toFloat() / stage.recommendedPower).coerceIn(0f, 1.4f) / 1.4f,
                    color = readinessColor(readiness),
                    height = 7
                )
                // 예상 승률 — 숫자 하나로 강조
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🎯 승률", color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp)
                    Text(
                        "$winChance%",
                        color = if (winChance >= 60) TcgColors.Heal else if (winChance >= 30) TcgColors.Gold else TcgColors.Danger,
                        fontSize = TcgType.NUMBER.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 보상 아이콘 줄 (텍스트 설명 대신 아이콘)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            val reward = stage.reward
            Pill("🪙 ${reward.gold}", TcgColors.Gold)
            Pill("✨ ${reward.exp}", TcgColors.Mana)
            if (reward.packTickets > 0) Pill("🎟 ${reward.packTickets}", TcgColors.Mana)
            if (reward.gems > 0) Pill("💎 ${reward.gems}", TcgColors.Violet)
            if (reward.shardCardId != null && reward.shardCount > 0) Pill("🧩 ${reward.shardCount}", TcgColors.Abyss)
            if (reward.equipmentId != null) Pill("🗡", TcgColors.Heal)
            if (stage.number in state.stageCleared) {
                Pill("재도전 ${StageEngine.REPEAT_REWARD_PERCENT}%", TcgColors.Muted)
            }
        }

        ActionTile(
            label = if (unlocked) "⚔ ${stage.number}번 스테이지 출전" else "🔒 ${stage.number - 1}번 클리어 후 해금",
            hint = if (unlocked) "${readiness.label} · 승률 $winChance% · ${stage.enemyElement.displayName} 상대"
            else "앞 스테이지를 먼저 클리어하세요",
            accent = if (stage.isBoss) TcgColors.Danger else TcgColors.Violet,
            enabled = unlocked,
            onClick = onStart
        )
        if (state.stageMessage != null) {
            Text(
                state.stageMessage!!,
                color = TcgColors.Text.copy(alpha = 0.85f),
                fontSize = TcgType.LABEL.sp,
                lineHeight = 16.sp,
                maxLines = 3
            )
        }
    }
}

/** 숫자 대신 비율 막대로 보여 주는 적 스탯 */
@Composable
private fun MiniStatBar(label: String, value: Int, max: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp, modifier = Modifier.width(38.dp))
        Box(modifier = Modifier.weight(1f)) {
            StatBar(
                ratio = (value / max.coerceAtLeast(1.0)).toFloat().coerceIn(0.03f, 1f),
                color = color,
                height = 6
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            if (value >= 10000) "${value / 1000}k" else "$value",
            color = color,
            fontSize = TcgType.TILE_MARK.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp)
        )
    }
}

/* ─────────────────────────── ⑤ 아이콘 버튼 ─────────────────────────── */

@Composable
private fun IconActionTile(icon: String, label: String, hint: String, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Text(label, color = TcgColors.Text, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = accent, fontSize = TcgType.TILE_MARK.sp, maxLines = 1)
    }
}

/* ─────────────────────────── 구제 스트립 ─────────────────────────── */

/**
 * 소프트락 방지 — 내용은 유지하되 **한 줄 스트립**으로 줄였다.
 * (잃은 카드가 없고 긴급 보급도 필요 없으면 아무것도 그리지 않는다)
 */
@Composable
private fun RescueStrip(
    state: TcgGameState,
    onReclaimLost: () -> Unit,
    onClaimSupply: () -> Unit,
    onOpenDeck: () -> Unit
) {
    val lost = state.reincarnationPool.size
    val offer = state.supplyOffer
    if (lost == 0 && !offer.isOffered) {
        if (state.ownedCards.size >= com.game.tcg.core.engine.EmergencySupplyEngine.MIN_OWNED_CARDS) return
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TcgColors.SurfaceAlt)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👥 보유 ${state.ownedCards.size}장", color = TcgColors.Gold, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("카드팩·연성으로 덱을 채우세요", color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp, modifier = Modifier.weight(1f))
            Pill("📜 덱", TcgColors.Mana)
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TcgColors.Danger.copy(alpha = 0.14f))
            .border(1.dp, TcgColors.Danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("💔 잃은 동료 $lost", color = TcgColors.Danger, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (lost > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(TcgColors.Heal.copy(alpha = 0.9f))
                    .clickable { onReclaimLost() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("✨ 무료 재계약", color = Color(0xFF120E1C), fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (offer.isOffered) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(TcgColors.Danger.copy(alpha = 0.9f))
                    .clickable { onClaimSupply() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🎁 긴급 보급", color = Color.White, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(TcgColors.SurfaceAlt)
                .clickable { onOpenDeck() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("📜 덱", color = TcgColors.Muted, fontSize = TcgType.LABEL.sp)
        }
    }
}

/* ─────────────────────────── 색 ─────────────────────────── */

private fun chapterAccent(chapter: Int): Color = when (chapter) {
    1 -> TcgColors.Violet
    2 -> TcgColors.Muted
    3 -> TcgColors.Mana
    4 -> TcgColors.Heal
    5 -> TcgColors.Gold
    else -> TcgColors.Danger
}

private fun readinessColor(readiness: StageReadiness): Color = when (readiness) {
    StageReadiness.EASY -> TcgColors.Heal
    StageReadiness.EVEN -> TcgColors.Mana
    StageReadiness.HARD -> TcgColors.Gold
    StageReadiness.LETHAL -> TcgColors.Danger
}
