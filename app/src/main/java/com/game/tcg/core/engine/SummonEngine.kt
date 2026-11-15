package com.game.tcg.core.engine

import com.game.tcg.core.model.TcgCard

/** 현실 소환 결과 */
data class SummonResult(
    val isSuccess: Boolean,
    val cardId: Int = 0,
    val cardName: String = "",
    val bondBefore: Int = 0,
    val bondAfter: Int = 0,
    val energySpent: Int = 0,
    val buffPercent: Int = 0,
    val buffBattles: Int = 0,
    val unlockedDateLook: Boolean = false,
    val failureReason: String? = null,
    val message: String = ""
)

/**
 * 현실 소환 엔진 — 2026-11-16 사용자 요청 "현실 소환" 서브 플로우.
 *
 * - **차원 균열 에너지 [ENERGY_COST]** 를 소모해 보유 캐릭터를 현실로 부른다.
 * - 호감도 +[BOND_GAIN] → 50에서 **-2 일상 룩**, 100에서 **-3 데이트 룩·페어 얼티밋**이 열린다
 *   (기존 호감도 50 규칙을 그대로 살린다).
 * - 소환하면 [BUFF_BATTLES] 회 전투 동안 **아군 공격력 +[BUFF_PERCENT]%** 버프가 걸린다
 *   (덱 전체가 아니라 파티 전체에 적용되는 소환 보너스).
 *
 * 교감(행동 선택)은 [DualWorldEngine]이 맡고, 이 엔진은 "소환 그 자체"의 비용·효과만 다룬다.
 */
object SummonEngine {

    /** 소환 1회 차원 에너지 비용 */
    const val ENERGY_COST: Int = 3

    /** 소환 1회 호감도 상승 */
    const val BOND_GAIN: Int = 8

    /** 소환 버프 — 아군 공격력 증가율 (%) */
    const val BUFF_PERCENT: Int = 12

    /** 소환 버프가 유지되는 전투 수 */
    const val BUFF_BATTLES: Int = 2

    /** 일상 룩(-2) 해금 호감도 */
    const val CASUAL_UNLOCK_BOND: Int = 50

    /** 데이트 룩(-3) 해금 호감도 */
    const val DATE_UNLOCK_BOND: Int = 50

    /** 페어 얼티밋 해금 호감도 */
    const val ULTIMATE_UNLOCK_BOND: Int = 100

    fun canSummon(gateEnergy: Int): Boolean = gateEnergy >= ENERGY_COST

    /**
     * 소환 실행 — 카드의 호감도를 올리고(직접 변경) 결과를 돌려준다.
     * 차원 에너지 차감·버프 지속 턴 관리는 ViewModel이 결과를 받아 처리한다.
     */
    fun summon(card: TcgCard, gateEnergy: Int): SummonResult {
        if (gateEnergy < ENERGY_COST) {
            return SummonResult(
                isSuccess = false,
                cardId = card.id,
                cardName = card.name,
                bondBefore = card.bondLevel,
                failureReason = "차원 균열 에너지가 부족합니다. (필요 ${ENERGY_COST} · 보유 $gateEnergy)"
            )
        }
        val before = card.bondLevel
        card.bondLevel = (card.bondLevel + BOND_GAIN).coerceAtMost(100)
        val after = card.bondLevel
        val dateUnlocked = before < DATE_UNLOCK_BOND && after >= DATE_UNLOCK_BOND
        val ultimateUnlocked = before < ULTIMATE_UNLOCK_BOND && after >= ULTIMATE_UNLOCK_BOND
        return SummonResult(
            isSuccess = true,
            cardId = card.id,
            cardName = card.name,
            bondBefore = before,
            bondAfter = after,
            energySpent = ENERGY_COST,
            buffPercent = BUFF_PERCENT,
            buffBattles = BUFF_BATTLES,
            unlockedDateLook = dateUnlocked,
            message = "[현실 소환] ${card.name} 호감도 $before → $after" +
                (if (dateUnlocked) " · 일상·데이트 룩 개방!" else "") +
                (if (ultimateUnlocked) " · 페어 얼티밋 오의 개방!" else "") +
                " · 아군 공격력 +$BUFF_PERCENT% ($BUFF_BATTLES" + "회 전투)"
        )
    }
}
