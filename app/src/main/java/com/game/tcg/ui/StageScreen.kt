package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.StageEngine
import com.game.tcg.core.model.BossKind
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.StageDefinition

/**
 * 🗺 스테이지 캠페인 — 메인 진행 루프 (2026-11-16 사용자 요청).
 *
 * ```
 * 스테이지 클리어 → 골드·경험치·카드팩 티켓·조각·장비 → 카드팩/강화/연성 → 덱 전투력 상승 → 다음 스테이지
 * ```
 *
 * · 6챕터 × 5스테이지 = 30스테이지. 앞 스테이지를 깨야 다음이 열린다.
 * · 5·10·15·20·25·28 스테이지에 **중간 보스 6명(No.170~175)**,
 *   26·29·30 스테이지에 **3대 마왕(112 릴리스·113 카밀라·114 모르가나)** 이 선다.
 * · 선택한 스테이지는 권장 전투력·예상 승률·보상 미리보기를 보여주고, 「출전」을 누르면
 *   기존 VS 전투 화면([BattleScreen])이 그 스테이지의 적 편성으로 열린다.
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
    var chapter by remember(selected.chapter) { mutableStateOf(selected.chapter) }
    val stages = StageCatalog.chapter(chapter)
    val readiness = StageEngine.readiness(state.deckTotalPower, selected)
    val winChance = StageEngine.estimatedWinChance(state.deckTotalPower, selected)
    val unlocked = state.isStageUnlocked(selected.number)

    ScreenFrame {
        // ── 구제 패널: 카드가 바닥났을 때의 소프트락 방지 (2026-11-16 QA) ──
        RescuePanel(
            state = state,
            onReclaimLost = onReclaimLost,
            onClaimSupply = onClaimSupply,
            onOpenDeck = onOpenDeck
        )

        SectionCard(
            title = "스테이지 캠페인",
            badge = "${state.stageCleared.size}/${StageCatalog.TOTAL_STAGES} 클리어",
            accent = TcgColors.Violet
        ) {
            Text(
                "앞 스테이지를 깨면 다음 스테이지가 열립니다. 보스 관문을 넘으면 조각·티켓·장비가 쏟아집니다.",
                color = TcgColors.Muted,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("🪙 ${state.gold}G", TcgColors.Gold)
                Pill("🎟 ${state.packTickets}", TcgColors.Mana)
                Pill("🧩 조각 ${state.shardTotal}", TcgColors.Abyss)
                Pill("⚔ 덱 ${state.deckTotalPower}", TcgColors.Heal)
            }
            // 챕터 탭
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..StageCatalog.CHAPTER_COUNT).forEach { chapterNumber ->
                    val clearedInChapter = StageCatalog.chapter(chapterNumber).count { it.number in state.stageCleared }
                    ChipButton(
                        label = "${chapterNumber}장 ${StageCatalog.CHAPTER_NAMES[chapterNumber - 1]} $clearedInChapter/5",
                        selected = chapterNumber == chapter,
                        accent = chapterAccent(chapterNumber)
                    ) { chapter = chapterNumber }
                }
            }
            stages.forEach { stage ->
                StageRow(
                    stage = stage,
                    isCleared = stage.number in state.stageCleared,
                    isUnlocked = state.isStageUnlocked(stage.number),
                    isSelected = stage.number == selected.number,
                    onClick = { onSelectStage(stage.number) }
                )
            }
        }

        // ── 선택한 스테이지 상세 ──
        SectionCard(
            title = "${selected.number}. ${selected.name}",
            badge = when {
                selected.number in state.stageCleared -> "클리어"
                unlocked -> "도전 가능"
                else -> "잠김"
            },
            accent = if (selected.isBoss) TcgColors.Danger else TcgColors.Violet
        ) {
            if (selected.isBoss) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    CardArtImage(
                        cardId = selected.bossId ?: 112,
                        visual = com.game.tcg.core.model.VisualState.BATTLE,
                        contentDescription = selected.enemyName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(210.dp),
                        targetWidthPx = 720
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD0B0A12))))
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Pill(
                            if (selected.bossKind == BossKind.DEMON_LORD) "👑 대마왕" else "☠ 중간 보스",
                            if (selected.bossKind == BossKind.DEMON_LORD) TcgColors.Danger else TcgColors.Gold,
                            filled = true
                        )
                        Pill(selected.enemyElement.displayName, elementColor(selected.enemyElement))
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(selected.enemyName, color = TcgColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            selected.bossRecord?.skill ?: "",
                            color = TcgColors.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Text(selected.story, color = TcgColors.Muted, fontSize = 12.sp, lineHeight = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("${selected.chapter}장 ${selected.chapterName}", chapterAccent(selected.chapter))
                Pill("적 HP ${selected.enemyHp}", TcgColors.Hp)
                Pill("적 ATK ${selected.enemyAtk}", TcgColors.Danger)
            }

            // 권장 전투력 대비 내 덱
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("권장 ${selected.recommendedPower}", color = TcgColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("내 덱 ${state.deckTotalPower}", color = readinessColor(readiness), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Pill("${readiness.label} · 승률 ${winChance}%", readinessColor(readiness))
            }
            StatBar(
                ratio = (state.deckTotalPower.toFloat() / selected.recommendedPower).coerceIn(0f, 1.3f) / 1.3f,
                color = readinessColor(readiness),
                height = 8
            )
            Text(readiness.advice, color = TcgColors.Muted, fontSize = 12.sp, lineHeight = 14.sp)

            Text("클리어 보상 · ${selected.reward.preview}", color = TcgColors.Gold, fontSize = 12.sp, lineHeight = 15.sp)
            if (selected.number in state.stageCleared) {
                Text(
                    "재도전 보상은 ${StageEngine.REPEAT_REWARD_PERCENT}%만 지급됩니다. (파밍용)",
                    color = TcgColors.Muted,
                    fontSize = 12.sp
                )
            }

            ActionTile(
                label = if (unlocked) "⚔ ${selected.number}번 스테이지 출전" else "🔒 ${selected.number - 1}번 클리어 후 해금",
                hint = if (unlocked) {
                    "${selected.enemyName} · ${readiness.label} · 예상 승률 ${winChance}%"
                } else {
                    "앞 스테이지를 먼저 클리어하세요"
                },
                accent = if (selected.isBoss) TcgColors.Danger else TcgColors.Violet,
                enabled = unlocked,
                onClick = { onStartStage(selected.number) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "📜 덱 편성",
                        hint = "출전 덱 ${state.party.size}/${state.unlockedDeckSize}장",
                        accent = TcgColors.Mana,
                        onClick = onOpenDeck
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "⚒ 강화·장비",
                        hint = "전투력 보강",
                        accent = TcgColors.Violet,
                        onClick = onOpenGrowth
                    )
                }
            }
            ActionTile(
                label = "🛒 카드팩 상점",
                hint = "🎟 ${state.packTickets} · 조각 ${state.shardTotal}",
                accent = TcgColors.Gold,
                onClick = onOpenShop
            )
            if (state.stageMessage != null) {
                Text(state.stageMessage, color = TcgColors.Text.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 14.sp)
            }
        }
    }
}

/**
 * 구제 패널 — 로그라이크 상실로 보유 카드가 줄었을 때 진행이 막히지 않도록 둔 안전장치.
 *
 * ① 환생 풀에 잃은 카드가 있으면 **무료 재계약** (비용 없음)
 * ② 보유 3장 미만 + 환생 풀 0장 + 팩·티켓·연성 불가면 **무료 긴급 보급**
 * ③ 둘 다 아니면 표시하지 않는다(정상 진행 중).
 */
@Composable
private fun RescuePanel(
    state: TcgGameState,
    onReclaimLost: () -> Unit,
    onClaimSupply: () -> Unit,
    onOpenDeck: () -> Unit
) {
    val offer = state.supplyOffer
    val lostCount = state.reincarnationPool.size
    if (lostCount == 0 && !offer.isOffered) {
        if (state.ownedCards.size >= com.game.tcg.core.engine.EmergencySupplyEngine.MIN_OWNED_CARDS) return
        // 카드가 적지만 아직 정상 획득 경로가 있는 경우 — 안내만
        SectionCard(title = "동료가 부족합니다", badge = "보유 ${state.ownedCards.size}장", accent = TcgColors.Gold) {
            Text(
                offer.reason.ifBlank { "보유 카드가 3장 미만입니다. 카드팩·연성으로 덱을 채우세요." },
                color = TcgColors.Muted,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
        return
    }

    SectionCard(
        title = "구제 · 잃은 동료 되찾기",
        badge = if (offer.isOffered) "긴급 보급 가능" else "무료 재계약",
        accent = TcgColors.Danger
    ) {
        Text(
            "쓰러진 동료는 영구 상실되지만, 환생 풀에서 **비용 없이** 되찾을 수 있습니다. " +
                "보유 카드가 3장 미만이고 팩·티켓·조각 연성까지 모두 불가능해지면 무료 지원 카드를 지급합니다.",
            color = TcgColors.Muted,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Pill("보유 ${state.ownedCards.size}장", TcgColors.Violet)
            Pill("환생 풀 ${lostCount}장", TcgColors.Abyss)
            Pill("골드 ${state.gold}G · 🎟 ${state.packTickets} · 조각 ${state.shardTotal}", TcgColors.Gold)
        }
        if (lostCount > 0) {
            state.reincarnationPool.take(4).forEach { lost ->
                Text(
                    "· ${lost.name} (${lost.reincarnationCount}회차 · 잃은 강화 Lv.${lost.lostUpgradeLevel})",
                    color = TcgColors.Text.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
            ActionTile(
                label = "✨ 무료 재계약 · 잃은 ${lostCount}장 전부 되찾기",
                hint = "비용 없음 · 초기 상태로 복귀 (기억·강화 초기화)",
                accent = TcgColors.Heal,
                onClick = onReclaimLost
            )
        }
        if (offer.isOffered) {
            ActionTile(
                label = "🎁 긴급 보급 · 동료 ${offer.cardIds.size}명 무료 지급",
                hint = "${offer.ownedBefore}장 → ${offer.ownedAfter}장 · 남은 보급 ${offer.claimsLeft}회",
                accent = TcgColors.Danger,
                onClick = onClaimSupply
            )
        }
        if (state.ownedCards.isNotEmpty()) {
            ActionTile(
                label = "📜 덱 편성으로 이동",
                hint = "되찾은 동료를 전열에 배치하세요",
                accent = TcgColors.Mana,
                onClick = onOpenDeck
            )
        }
        if (state.supplyMessage != null) {
            Text(
                state.supplyMessage!!,
                color = TcgColors.Text.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/** 스테이지 목록 한 줄 — 잠김 · 클리어 · 보스 표시 */
@Composable
private fun StageRow(
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accent.copy(alpha = 0.18f) else TcgColors.SurfaceAlt)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (!isUnlocked) "🔒" else if (isCleared) "✓" else "▸",
            color = if (!isUnlocked) TcgColors.Muted else accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stage.number}. ${stage.name}",
                    color = if (isUnlocked) TcgColors.Text else TcgColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (stage.isBoss) {
                    Spacer(Modifier.width(5.dp))
                    Pill(
                        if (stage.bossKind == BossKind.DEMON_LORD) "대마왕" else "중간 보스",
                        if (stage.bossKind == BossKind.DEMON_LORD) TcgColors.Danger else TcgColors.Gold
                    )
                }
            }
            Text(
                "${stage.enemyName} · 권장 ${stage.recommendedPower}",
                color = TcgColors.Muted,
                fontSize = 12.sp
            )
        }
        Pill("${stage.enemyElement.displayName}", elementColor(stage.enemyElement))
    }
}

private fun chapterAccent(chapter: Int): Color = when (chapter) {
    1 -> TcgColors.Violet
    2 -> TcgColors.Muted
    3 -> TcgColors.Mana
    4 -> TcgColors.Heal
    5 -> TcgColors.Gold
    else -> TcgColors.Danger
}

private fun readinessColor(readiness: com.game.tcg.core.engine.StageReadiness): Color = when (readiness) {
    com.game.tcg.core.engine.StageReadiness.EASY -> TcgColors.Heal
    com.game.tcg.core.engine.StageReadiness.EVEN -> TcgColors.Mana
    com.game.tcg.core.engine.StageReadiness.HARD -> TcgColors.Gold
    com.game.tcg.core.engine.StageReadiness.LETHAL -> TcgColors.Danger
}
