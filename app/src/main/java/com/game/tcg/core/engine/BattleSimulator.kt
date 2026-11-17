package com.game.tcg.core.engine

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.StageDefinition
import com.game.tcg.core.model.TcgCard

/**
 * 전투 시뮬레이터 — 스테이지 난이도·강화·합성·상성 밸런스를 **숫자로 검증**하기 위한 머리 없는 전투.
 *
 * 게임 화면의 VS 전투([com.game.tcg.ui.BattleScreen])와 같은 규칙을 단순화해 따른다.
 *  - 턴마다 **전열(앞 3장) 중 살아 있는 카드가 번갈아 1회 공격**한다
 *    (전열이 모두 쓰러지면 후열이 공격).
 *  - 공격 피해 = ATK × 속성 상성 배율(× 페어 오의 1.25)
 *  - 적은 턴마다 **살아 있는 카드 1장에게 반격**하고, 그 피해는
 *    [BalanceRules.damageTakenModifier] (전열 상성 우열) 만큼 보정된다.
 *  - 적 HP 가 0 이 되면 승리, 아군이 모두 쓰러지면 패배.
 *
 * Android 에 의존하지 않으므로 JVM 단위 테스트에서 그대로 돌린다.
 * (밸런스 목표: 무강화 1성 덱은 5스테이지 이후를 뚫지 못하고,
 *  Lv.5·3성 + 상성 우위 덱은 15~25스테이지, Lv.10·5성 덱은 30스테이지까지 간다)
 */
object BattleSimulator {

    /** 시뮬레이션에 쓰는 카드 상태 (전장 HP 포함) */
    data class Combatant(
        val cardId: Int,
        val atk: Int,
        val element: Element,
        val hp: Int,
        val ultimate: Boolean = false
    )

    /** 시뮬레이션 결과 */
    data class Result(
        val victory: Boolean,
        val turns: Int,
        val lostCards: Int,
        val remainingHpRatio: Double,
        val enemyHpLeft: Int
    ) {
        /** 결과를 한 줄로 (테스트 실패 메시지·로그용) */
        val summary: String
            get() = (if (victory) "승리" else "패배") +
                " · ${turns}턴 · 손실 ${lostCards}장 · 적 잔여 HP $enemyHpLeft"
    }

    /** 최대 시뮬레이션 턴 (무한 루프 방지) */
    const val MAX_TURNS: Int = 80

    /** 카드 → 전장 HP 포함 전투 단위 (카드 기록의 maxHp 를 현재 DEF 비율로 스케일) */
    fun combatant(card: TcgCard): Combatant {
        val record = CardCatalog.find(card.id)
        val baseDef = record?.def?.coerceAtLeast(1) ?: 1
        val hp = if (record == null) 4000 else (record.maxHp.toLong() * card.def / baseDef).toInt().coerceAtLeast(1)
        return Combatant(
            cardId = card.id,
            atk = card.atk.coerceAtLeast(1),
            element = card.element,
            hp = hp,
            ultimate = card.isUltimateUnlocked
        )
    }

    /**
     * 덱 vs 스테이지 1판 시뮬레이션 (결정적 — 같은 입력이면 같은 결과).
     * @param cards 출전 덱 (앞 3장이 전열)
     */
    fun simulate(
        cards: List<TcgCard>,
        stage: StageDefinition,
        frontSlots: Int = 3,
        maxTurns: Int = MAX_TURNS
    ): Result = simulateCombatants(cards.map { combatant(it) }, stage, frontSlots, maxTurns)

    fun simulateCombatants(
        units: List<Combatant>,
        stage: StageDefinition,
        frontSlots: Int = 3,
        maxTurns: Int = MAX_TURNS
    ): Result {
        if (units.isEmpty()) return Result(false, 0, 0, 0.0, stage.enemyHp)
        val hp = units.map { it.hp }.toMutableList()
        val alive = units.indices.toMutableList()
        val frontCount = frontSlots.coerceAtLeast(1)
        var enemyHp = stage.enemyHp
        var turns = 0

        while (turns < maxTurns && enemyHp > 0 && alive.isNotEmpty()) {
            turns += 1

            // ── 아군 공격 (전열 우선, 번갈아 1회) ──
            val front = alive.filter { it < frontCount }.ifEmpty { alive }
            val attackerIndex = front[(turns - 1) % front.size]
            val attacker = units[attackerIndex]
            val damage = (attacker.atk * attacker.element.getDamageMultiplier(stage.enemyElement) *
                (if (attacker.ultimate) 1.25 else 1.0)).toInt()
            enemyHp = (enemyHp - damage).coerceAtLeast(0)
            if (enemyHp <= 0) break

            // ── 적 반격 ──
            val frontElements = front.map { units[it].element }
            val modifier = BalanceRules.damageTakenModifier(frontElements, stage.enemyElement)
            val victim = alive[(turns - 1) % alive.size]
            hp[victim] -= (stage.enemyAtk * modifier).toInt().coerceAtLeast(1)
            if (hp[victim] <= 0) alive.remove(victim)
        }

        val lost = units.size - alive.size
        val maxHpTotal = units.sumOf { it.hp }.coerceAtLeast(1)
        val remaining = alive.sumOf { hp[it].coerceAtLeast(0) }.toDouble() / maxHpTotal
        return Result(
            victory = enemyHp <= 0,
            turns = turns,
            lostCards = lost,
            remainingHpRatio = remaining,
            enemyHpLeft = enemyHp
        )
    }

    /** 여러 판(같은 덱)을 돌려 승률(%)을 구한다 — 덱 순서만 회전시켜 편향을 없앤다 */
    fun winChance(cards: List<TcgCard>, stage: StageDefinition, rounds: Int = 6): Int {
        if (cards.isEmpty()) return 0
        var wins = 0
        repeat(rounds) { offset ->
            val rotated = cards.drop(offset % cards.size) + cards.take(offset % cards.size)
            if (simulate(rotated, stage).victory) wins += 1
        }
        return (wins * 100) / rounds
    }
}
