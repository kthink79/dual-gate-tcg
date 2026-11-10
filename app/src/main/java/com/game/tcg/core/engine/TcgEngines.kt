package com.game.tcg.core.engine

import com.game.tcg.core.model.*

/**
 * 턴제 TCG 룰 및 상성 데미지 계산 배틀 엔진
 */
class BattleEngine {

    /**
     * 카드 간 단일 공격 판정 (상성 배율 + 페어 얼티밋 오의 보너스 적용)
     */
    fun calculateDamage(attacker: TcgCard, defender: TcgCard): BattleDamageResult {
        val baseDamage = attacker.atk.toDouble()
        val multiplier = attacker.element.getDamageMultiplier(defender.element)

        // 호감도 MAX 달성으로 페어 오의가 각성된 경우 추가 데미지 +25%
        val ultimateBonus = if (attacker.isUltimateUnlocked) 1.25 else 1.00

        val finalDamage = (baseDamage * multiplier * ultimateBonus).toInt()
        val isCriticalElement = multiplier >= 1.30

        return BattleDamageResult(
            rawDamage = attacker.atk,
            multiplier = multiplier,
            finalDamage = finalDamage,
            isCriticalElement = isCriticalElement,
            isUltimateTriggered = attacker.isUltimateUnlocked,
            summaryMessage = buildString {
                append("${attacker.name} [${attacker.element.displayName}] → ")
                append("${defender.name} [${defender.element.displayName}] 타격: ")
                append("${finalDamage} 데미지! (상성 ${multiplier}x")
                if (attacker.isUltimateUnlocked) append(", 오의 발동 +25%")
                append(")")
            }
        )
    }
}

data class BattleDamageResult(
    val rawDamage: Int,
    val multiplier: Double,
    val finalDamage: Int,
    val isCriticalElement: Boolean,
    val isUltimateTriggered: Boolean,
    val summaryMessage: String
)

/**
 * 카드 합성 및 한계 돌파(5성) 엔진
 */
class SynthesisEngine {

    /**
     * 동일 카드 합성을 통한 성급 상승 (최대 5성 한계 돌파)
     * 성급당 공격력/방어력 +20% 영구 스케일링
     */
    fun synthesizeCard(baseCard: TcgCard, materialCard: TcgCard): Boolean {
        if (baseCard.id != materialCard.id) return false
        if (baseCard.starLevel >= 5) return false

        baseCard.starLevel += 1
        baseCard.atk = (baseCard.atk * 1.20).toInt()
        baseCard.def = (baseCard.def * 1.20).toInt()
        return true
    }
}

/**
 * 현실–이세계 크로스 듀얼 월드 순환 루프 엔진
 * - 현실 소환 모드: 일상 평상복/데이트 룩 교감, 호감도 상승 -> 페어 얼티밋 오의 개방
 * - 이세계 원정 모드: 턴제 배틀 클리어, EXP 획득 -> 레벨업 및 영구 스탯 성장
 */
class DualWorldEngine {

    /**
     * 현실 소환 모드: 일상 교감으로 호감도 육성
     */
    fun interactInRealWorld(card: TcgCard, activity: RealWorldActivity): Int {
        val bondIncrease = when (activity) {
            RealWorldActivity.DAILY_GREETING -> 5
            RealWorldActivity.CASUAL_CAFE_WALK -> 15
            RealWorldActivity.ROMANTIC_EVENING_DATE -> 30
        }
        card.bondLevel = (card.bondLevel + bondIncrease).coerceAtMost(100)

        // 비주얼 스킨 자동 전환
        card.currentVisual = when {
            activity == RealWorldActivity.ROMANTIC_EVENING_DATE -> VisualState.ROMANTIC_DATE
            card.bondLevel >= 50 -> VisualState.CASUAL_CUTE
            else -> VisualState.BATTLE
        }

        return card.bondLevel
    }

    /**
     * 이세계 원정 모드: 배틀 승리를 통해 EXP 획득 및 영구 능력치 성장
     */
    fun completeIsekaiBattle(card: TcgCard, earnedExp: Int): Boolean {
        card.currentExp += earnedExp
        val expThreshold = card.starLevel * 1000

        if (card.currentExp >= expThreshold) {
            card.currentExp -= expThreshold
            card.atk += 150
            card.def += 100
            return true // 레벨업 성공
        }
        return false
    }
}

enum class RealWorldActivity {
    DAILY_GREETING,
    CASUAL_CAFE_WALK,
    ROMANTIC_EVENING_DATE
}
