package com.game.tcg.core.engine

import com.game.tcg.core.model.Element
import com.game.tcg.core.model.EnemySquad
import com.game.tcg.core.model.EnemyUnit

/**
 * 다중 적 카드 전투 규칙 — 2026-11-18 개편.
 *
 * 화면([com.game.tcg.ui.BattleScreen])의 실시간 전투와 머리 없는 검증
 * ([BattleSimulator])이 **같은 계산**을 쓰도록 규칙을 이 한 곳에 모았다.
 *
 * 규칙
 *  - 아군 공격 1회 = 적 카드 **한 장**을 때린다 (전열 우선 → 전열이 비면 후열)
 *    · 피해 = 공격력 × 속성 상성(×오의 1.25) × 급소·소환 보정 × 홀림 감소
 *    · 그 카드 HP 가 0 이 되면 쓰러지고, **전부 쓰러지면 승리**
 *  - 적 반격 1턴 = 전열 살아 있는 카드 최대 [EnemySquad.MAX_ATTACKERS_PER_TURN]장이
 *    각자 아군 1장을 때린다 (적 카드가 줄면 받는 피해도 줄어든다)
 *    · 피해 = 그 카드 공격력 × 홀림 피해 증가 × (방어 보너스) × [BalanceRules.damageTakenModifier]
 *
 * Android 의존성이 없어 JVM 단위 테스트에서 그대로 검증한다.
 */
object SquadBattleEngine {

    /** 아군 공격 1회의 결과 */
    data class AttackOutcome(
        val targetIndex: Int,
        val targetName: String,
        val damage: Int,
        val killed: Boolean,
        val victory: Boolean,
        val multiplier: Double,
        val critical: Boolean,
        val ultimate: Boolean,
        val units: List<EnemyUnit>
    )

    /** 아군 공격 1회 → 대상 카드 1장 피해 */
    fun playerAttack(
        attackerAtk: Int,
        attackerElement: Element,
        ultimate: Boolean,
        units: List<EnemyUnit>,
        penaltyAttackReductionPercent: Int = 0,
        intelBonusPercent: Int = 0,
        summonBuffPercent: Int = 0
    ): AttackOutcome {
        val targetIndex = EnemySquad.focusIndex(units)
        if (targetIndex < 0) {
            return AttackOutcome(-1, "", 0, false, EnemySquad.isWipedOut(units), 1.0, false, ultimate, units)
        }
        val target = units[targetIndex]
        val penaltyMultiplier = (100 - penaltyAttackReductionPercent).coerceAtLeast(10) / 100.0
        val multiplier = attackerElement.getDamageMultiplier(target.element)
        val ultimateBonus = if (ultimate) 1.25 else 1.0
        val intel = 1.0 + intelBonusPercent / 100.0
        val summon = 1.0 + summonBuffPercent / 100.0
        val damage = (attackerAtk * penaltyMultiplier * multiplier * ultimateBonus * intel * summon)
            .toInt()
            .coerceAtLeast(1)
        val updated = units.mapIndexed { index, unit ->
            if (index == targetIndex) unit.damaged(damage) else unit
        }
        return AttackOutcome(
            targetIndex = targetIndex,
            targetName = target.name,
            damage = damage,
            killed = updated[targetIndex].hp <= 0,
            victory = EnemySquad.isWipedOut(updated),
            multiplier = multiplier,
            critical = multiplier >= 1.30,
            ultimate = ultimate,
            units = updated
        )
    }

    /**
     * 적 카드 1장의 반격 피해.
     *
     * @param frontElements 아군 전열의 살아 있는 카드 속성 (상성 방어 보정)
     */
    fun enemyHitDamage(
        unitAtk: Int,
        unitElement: Element,
        frontElements: List<Element>,
        damageTakenIncreasePercent: Int = 0,
        defenseBonusPercent: Int = 0,
        frontRowDecayPercent: Int = 0
    ): Int {
        val increased = (unitAtk * (100 + damageTakenIncreasePercent) / 100.0)
        val edge = BalanceRules.damageTakenModifier(frontElements, unitElement)
        val defense = (100 - defenseBonusPercent + frontRowDecayPercent).coerceAtLeast(10) / 100.0
        return (increased * defense * edge).toInt().coerceAtLeast(1)
    }

    /** 이번 턴 반격하는 적 카드 목록 (전열 우선) */
    fun attackers(units: List<EnemyUnit>): List<EnemyUnit> = EnemySquad.attackersThisTurn(units)

    /** 이번 턴 아군이 받는 예상 총 피해 (스테이지 화면·툴팁용) */
    fun expectedIncomingDamage(
        units: List<EnemyUnit>,
        frontElements: List<Element>,
        damageTakenIncreasePercent: Int = 0,
        defenseBonusPercent: Int = 0
    ): Int = attackers(units).sumOf {
        enemyHitDamage(
            unitAtk = it.atk,
            unitElement = it.element,
            frontElements = frontElements,
            damageTakenIncreasePercent = damageTakenIncreasePercent,
            defenseBonusPercent = defenseBonusPercent
        )
    }
}
