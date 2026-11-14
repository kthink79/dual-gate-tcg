package com.game.tcg.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.VisualState

/**
 * 탭 2 — ⚔️ 이계 전투
 * 전열/후열 슬롯 카드, HP·마나 게이지, [홀림] 디버프 배지, 전술 후퇴, 로그라이크 사망·환생 표시
 */
@Composable
fun BattleScreen(
    state: TcgGameState,
    onAttackFront: () -> Unit,
    onAttackBack: () -> Unit,
    onNextTurn: () -> Unit,
    onRetreat: (Int) -> Unit,
    onEmergencyRetreat: () -> Unit,
    onReacquire: (Int) -> Unit,
    onRedeploy: (Int) -> Unit,
    onStartDemonLordBattle: () -> Unit,
    onStartFieldBattle: () -> Unit
) {
    ScreenFrame {
        // ── 적 정보 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (state.isDemonLordBattle) Color(0xFF2A1040) else Color(0xFF141C2A),
                            TcgColors.Surface
                        )
                    )
                )
                .border(1.dp, TcgColors.Line, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isDemonLordBattle) {
                        Image(
                            painter = painterResource(characterImage(112, VisualState.BATTLE)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(48.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.enemyName,
                                color = TcgColors.Text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Pill(
                                if (state.isDemonLordBattle) "마왕전" else "원정",
                                if (state.isDemonLordBattle) TcgColors.Abyss else TcgColors.Mana
                            )
                        }
                        Text(
                            "속성 ${state.enemyElement.displayName} · 공격력 ${state.enemyAtk}",
                            color = TcgColors.Muted,
                            fontSize = 10.sp
                        )
                    }
                }
                StatBar(
                    ratio = if (state.enemyMaxHp == 0) 0f else state.enemyHp.toFloat() / state.enemyMaxHp,
                    color = if (state.isDemonLordBattle) TcgColors.Abyss else TcgColors.Hp,
                    height = 10
                )
                Text(
                    "적 HP ${state.enemyHp} / ${state.enemyMaxHp}",
                    color = TcgColors.Hp,
                    fontSize = 10.sp
                )
            }
        }

        // ── 턴 / 마나 / 홀림 ──
        SectionCard(title = "전황", badge = "턴 ${state.turn}", accent = TcgColors.Mana) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("마나", color = TcgColors.Muted, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    StatBar(
                        ratio = if (state.mana.maxMana == 0) 0f else state.mana.currentMana.toFloat() / state.mana.maxMana,
                        color = TcgColors.Mana,
                        height = 8
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("${state.mana.currentMana}/${state.mana.maxMana}", color = TcgColors.Mana, fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    text = if (state.combatPenalty.totalStacks > 0)
                        "[홀림] ${state.combatPenalty.totalStacks}스택" else "[정신 맑음]",
                    color = if (state.combatPenalty.totalStacks > 0) TcgColors.Danger else TcgColors.Heal,
                    filled = state.combatPenalty.totalStacks > 0
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (state.combatPenalty.isCharmed)
                        "공격력 -${state.combatPenalty.attackReductionPercent}% · 받는 피해 +${state.combatPenalty.damageTakenIncreasePercent}%" +
                                if (state.combatPenalty.extraManaCost > 0) " · 스킬 마나 +${state.combatPenalty.extraManaCost}" else ""
                    else "유혹 페널티 없음",
                    color = if (state.combatPenalty.isCharmed) TcgColors.Danger else TcgColors.Muted,
                    fontSize = 9.sp
                )
            }
            if (state.defenseBonusPercent > 0 || state.intelBonusPercent > 0) {
                Text(
                    "아군 보정 — 방어 +${state.defenseBonusPercent}% · 급소 피해 +${state.intelBonusPercent}%",
                    color = TcgColors.Heal,
                    fontSize = 9.sp
                )
            }
        }

        // ── 전열 3 ──
        SectionCard(title = "전열 3슬롯", badge = "적 공격 1차 방어선", accent = TcgColors.Hp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0..2).forEach { slot ->
                    val member = state.party.firstOrNull { it.slotIndex == slot }
                    Box(modifier = Modifier.weight(1f)) {
                        FieldSlotCard(member, slot, state.combatPenalty.totalStacks > 0) { onRetreat(slot) }
                    }
                }
            }
        }

        // ── 후열 2 ──
        SectionCard(title = "후열 2슬롯", badge = "지원·원거리", accent = TcgColors.Mana) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (3..4).forEach { slot ->
                    val member = state.party.firstOrNull { it.slotIndex == slot }
                    Box(modifier = Modifier.weight(1f)) {
                        FieldSlotCard(member, slot, state.combatPenalty.totalStacks > 0) { onRetreat(slot) }
                    }
                }
            }
            if (state.battleReserve.isNotEmpty()) {
                Text("후퇴 대기열: ${state.battleReserve.joinToString(", ") { it.name }}", color = TcgColors.Muted, fontSize = 9.sp)
                state.battleReserve.forEach { card ->
                    ActionTile(
                        label = "재투입 · ${card.name}",
                        hint = "빈 슬롯에 재배치",
                        accent = TcgColors.Heal,
                        onClick = { onRedeploy(card.id) }
                    )
                }
            }
        }

        // ── 조작 ──
        SectionCard(title = "전술 지시", accent = TcgColors.Gold) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "⚔ 전열 공격",
                        hint = "마나 1",
                        accent = TcgColors.Hp,
                        onClick = onAttackFront
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "🏹 후열 지원",
                        hint = "마나 1",
                        accent = TcgColors.Mana,
                        onClick = onAttackBack
                    )
                }
            }
            ActionTile(
                label = "⏭ 턴 진행 (적 반격 · 마나 회복)",
                hint = "턴마다 전술 후퇴 1회 제한이 해제됩니다",
                accent = TcgColors.Gold,
                onClick = onNextTurn
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "🏃 파티 긴급 철수",
                        hint = "전투 포기 · 전리품 상실 · 카드 상실 없음",
                        accent = TcgColors.Danger,
                        onClick = onEmergencyRetreat
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = if (state.isDemonLordBattle) "마수 원정" else "마왕전 개시",
                        hint = if (state.isDemonLordBattle) "일반 원정으로 전환" else "릴리스 HP 25000",
                        accent = TcgColors.Abyss,
                        onClick = if (state.isDemonLordBattle) onStartFieldBattle else onStartDemonLordBattle
                    )
                }
            }
            if (state.lastRetreatMessage != null) {
                Text(state.lastRetreatMessage, color = TcgColors.Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Text(
                "전술 후퇴: 슬롯 카드를 눌러 손패로 철수 (턴당 1회, 마나 1 / 마왕전 2). [홀림] 3스택 이상이면 명령 불복.",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }

        // ── 로그라이크 사망·환생 ──
        SectionCard(
            title = "로그라이크 사망 · 환생",
            badge = "기억 초기화",
            accent = TcgColors.Danger
        ) {
            Text(
                "전장에서 HP 0이 된 카드는 덱과 보유 컬렉션에서 영구 상실되고, 성급·강화·교감·기억이 모두 0으로 초기화됩니다. " +
                        "환생 풀에서 재획득해야 다시 계약할 수 있습니다.",
                color = TcgColors.Muted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            if (state.reincarnationPool.isEmpty()) {
                Text("현재 상실된 카드 없음 — 파티 전원 생존", color = TcgColors.Heal, fontSize = 10.sp)
            } else {
                state.reincarnationPool.forEach { lost ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TcgColors.Danger.copy(alpha = 0.12f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "${lost.displayName} · ${lost.reincarnationCount}회차 환생 대기",
                            color = TcgColors.Danger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "상실: ${lost.lostStarLevel}★ · 강화 Lv.${lost.lostUpgradeLevel} · 기억 ${lost.lostMemoryAffinity} · 교감 ${lost.lostBondLevel} / 사인: ${lost.causeOfDeath}",
                            color = TcgColors.Muted,
                            fontSize = 9.sp
                        )
                        ActionTile(
                            label = "재획득 · ${lost.name} (초기 상태 복귀)",
                            accent = TcgColors.Heal,
                            onClick = { onReacquire(lost.templateId) }
                        )
                    }
                }
            }
        }

        // ── 전투 로그 ──
        SectionCard(title = "전투 로그", accent = TcgColors.Muted) {
            LogBox(state.battleLog, maxLines = 10)
            Text("승리 ${state.battleVictories}회 · 누적 전리품 ${state.lootGold}G", color = TcgColors.Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun FieldSlotCard(
    member: PartyMember?,
    slotIndex: Int,
    charmed: Boolean,
    onRetreat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TcgColors.SurfaceAlt)
            .border(
                1.dp,
                if (member == null) TcgColors.Line else if (member.isNearDeath) TcgColors.Danger else TcgColors.Line,
                RoundedCornerShape(12.dp)
            )
            .then(if (member != null && member.isAlive) Modifier.clickable { onRetreat() } else Modifier)
    ) {
        if (member == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("빈 슬롯\n$slotIndex", color = TcgColors.Muted, fontSize = 9.sp)
            }
        } else {
            Box {
                Image(
                    painter = painterResource(characterImage(member.card.id, member.card.currentVisual)),
                    contentDescription = member.card.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC0B0A12))
                            )
                        )
                )
                if (charmed) {
                    Box(modifier = Modifier.padding(3.dp)) {
                        Pill("[홀림]", TcgColors.Danger, filled = true)
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        member.card.name.take(7),
                        color = TcgColors.Text,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatBar(ratio = member.hpRatio, color = if (member.isNearDeath) TcgColors.Danger else TcgColors.Hp, height = 5)
                    Text("${member.hp}/${member.maxHp}", color = TcgColors.Muted, fontSize = 7.sp)
                }
            }
            Text(
                if (member.isAlive) "탭하여 후퇴" else "사망 · 상실 처리",
                color = if (member.isAlive) TcgColors.Muted else TcgColors.Danger,
                fontSize = 8.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
            )
        }
    }
}
