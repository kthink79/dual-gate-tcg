package com.game.tcg.core.engine

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.EnemySquad
import com.game.tcg.core.model.EnemyUnit
import com.game.tcg.core.model.StageDefinition
import com.game.tcg.core.model.TcgCard

/**
 * 전투 시뮬레이터 — 스테이지 난이도·강화·합성·상성 밸런스를 **숫자로 검증**하기 위한 머리 없는 전투.
 *
 * 2026-11-18 다중 적 개편으로 게임 화면([com.game.tcg.ui.BattleScreen])과 **같은 규칙**을 쓴다.
 *  - 적 진영은 [EnemySquad.build] 로 만든 개별 HP·공격력을 가진 카드 9~12장이다
 *  - 턴마다 전열(앞 3장) 중 살아 있는 카드가 번갈아 **적 카드 1장**을 공격한다
 *    (전열이 모두 쓰러지면 후열이 공격). 대상은 전열의 살아 있는 적 → 후열 순
 *  - 적은 같은 턴에 전열 살아 있는 카드 최대 3장이 각자 아군 1장을 반격한다
 *    (적 카드를 쓰러뜨릴수록 들어오는 피해가 줄어든다)
 *  - **적 카드를 전부 쓰러뜨리면 승리**, 아군이 모두 쓰러지면 패배
 *
 * Android 에 의존하지 않으므로 JVM 단위 테스트에서 그대로 돌린다.
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
        val enemyHpLeft: Int,
        /** 살아남은 적 카드 수 */
        val enemyUnitsLeft: Int = 0,
        /** 적 진영 카드 수 */
        val enemyUnitsTotal: Int = 0
    ) {
        /** 결과를 한 줄로 (테스트 실패 메시지·로그용) */
        val summary: String
            get() = (if (victory) "승리" else "패배") +
                " · ${turns}턴 · 손실 ${lostCards}장 · 적 ${enemyUnitsLeft}/${enemyUnitsTotal}장 잔존" +
                " (적 잔여 HP $enemyHpLeft)"
    }

    /** 최대 시뮬레이션 턴 (무한 루프 방지) */
    const val MAX_TURNS: Int = 80

    /** 시뮬레이션 적 진영 카드 수 (사용자 기본 덱과 같은 9장) */
    const val ENEMY_SQUAD_SIZE: Int = 9

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

    /** 스테이지의 적 진영 (시뮬레이터·화면 공통 규칙) */
    fun enemySquad(stage: StageDefinition, size: Int = ENEMY_SQUAD_SIZE): List<EnemyUnit> =
        EnemySquad.build(stage = stage, size = size)

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
        maxTurns: Int = MAX_TURNS,
        squadSize: Int = ENEMY_SQUAD_SIZE
    ): Result {
        val enemies0 = enemySquad(stage, squadSize)
        if (units.isEmpty()) {
            return Result(false, 0, 0, 0.0, EnemySquad.totalHp(enemies0), enemies0.size, enemies0.size)
        }
        val hp = units.map { it.hp }.toMutableList()
        val alive = units.indices.toMutableList()
        val frontCount = frontSlots.coerceAtLeast(1)
        var enemies = enemies0
        var turns = 0

        while (turns < maxTurns && !EnemySquad.isWipedOut(enemies) && alive.isNotEmpty()) {
            turns += 1

            // ── 아군 공격 (전열 우선, 번갈아 1회) — 적 카드 1장을 쓰러뜨린다 ──
            val front = alive.filter { it < frontCount }.ifEmpty { alive }
            val attacker = units[front[(turns - 1) % front.size]]
            enemies = SquadBattleEngine.playerAttack(
                attackerAtk = attacker.atk,
                attackerElement = attacker.element,
                ultimate = attacker.ultimate,
                units = enemies
            ).units
            if (EnemySquad.isWipedOut(enemies)) break

            // ── 적 반격 (전열 최대 3장, 각자 아군 1장) ──
            val attackers = EnemySquad.attackersThisTurn(enemies)
            attackers.forEachIndexed { index, enemyUnit ->
                if (alive.isEmpty()) return@forEachIndexed
                val frontElements = alive.filter { it < frontCount }.ifEmpty { alive }.map { units[it].element }
                val victim = alive[(turns + index - 1).mod(alive.size)]
                val damage = SquadBattleEngine.enemyHitDamage(
                    unitAtk = enemyUnit.atk,
                    unitElement = enemyUnit.element,
                    frontElements = frontElements
                )
                hp[victim] -= damage
                if (hp[victim] <= 0) alive.remove(victim)
            }
        }

        val lost = units.size - alive.size
        val maxHpTotal = units.sumOf { it.hp }.coerceAtLeast(1)
        val remaining = alive.sumOf { hp[it].coerceAtLeast(0) }.toDouble() / maxHpTotal
        return Result(
            victory = EnemySquad.isWipedOut(enemies),
            turns = turns,
            lostCards = lost,
            remainingHpRatio = remaining,
            enemyHpLeft = EnemySquad.totalHp(enemies),
            enemyUnitsLeft = EnemySquad.aliveCount(enemies),
            enemyUnitsTotal = enemies.size
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
