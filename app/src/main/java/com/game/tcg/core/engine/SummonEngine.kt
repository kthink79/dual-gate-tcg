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
    val message: String = "",
    /** 잠금으로 실패했을 때 필요한 친밀도 (미해금 안내용) */
    val requiredBond: Int = 0
)

/**
 * 현실 소환 엔진 — 2026-11-16 사용자 요청 "현실 소환" 서브 플로우.
 *
 * - **차원 균열 에너지 [ENERGY_COST]** 를 소모해 보유 캐릭터를 현실로 부른다.
 * - **소환 조건**: 그 캐릭터의 **일상 사복(②)이 해금**되어 있어야 한다
 *   (= 친밀도 [AffinityRules.CASUAL_UNLOCK_BOND] 이상). 미해금 캐릭터는 소환 대신
 *   `🔒 친밀도 30 필요` 안내와 진행도 게이지만 보여 준다.
 * - 소환하면 친밀도 +[BOND_GAIN] (작은 폭 · 보조 경로) 과 [BUFF_BATTLES] 회 전투 동안
 *   **아군 공격력 +[BUFF_PERCENT]%** 버프가 걸린다.
 * - 친밀도 30 → 일상 사복, 70 → 데이트 룩, 100 → 페어 얼티밋(기준은 [AffinityRules] 한 곳).
 *
 * 교감(행동 선택)은 [DualWorldEngine]이 맡고, 이 엔진은 "소환 그 자체"의 비용·조건·효과만 다룬다.
 */
object SummonEngine {

    /** 소환 1회 차원 에너지 비용 */
    const val ENERGY_COST: Int = 3

    /** 소환 1회 친밀도 상승 — 직접 만나는 보조 경로이므로 작게 */
    const val BOND_GAIN: Int = AffinityRules.SUMMON_GAIN

    /** 소환 버프 — 아군 공격력 증가율 (%) */
    const val BUFF_PERCENT: Int = 12

    /** 소환 버프가 유지되는 전투 수 */
    const val BUFF_BATTLES: Int = 2

    /** 일상 사복(②) 해금 친밀도 — 현실 소환 조건과 같은 값 */
    const val CASUAL_UNLOCK_BOND: Int = AffinityRules.CASUAL_UNLOCK_BOND

    /** 데이트 룩(③) 해금 친밀도 */
    const val DATE_UNLOCK_BOND: Int = AffinityRules.DATE_UNLOCK_BOND

    /** 페어 얼티밋 해금 친밀도 */
    const val ULTIMATE_UNLOCK_BOND: Int = AffinityRules.ULTIMATE_UNLOCK_BOND

    /** 차원 에너지만 보고 판정 (친밀도까지 함께 보려면 [canSummon]) */
    fun hasEnergy(gateEnergy: Int): Boolean = gateEnergy >= ENERGY_COST

    /** 차원 에너지 + 일상 사복 해금을 모두 만족하는가 */
    fun canSummon(card: TcgCard, gateEnergy: Int): Boolean =
        hasEnergy(gateEnergy) && AffinityRules.canRealitySummon(card.bondLevel)

    /** 에너지만으로 판정 (기존 호출 호환 — 실제 소환은 [canSummon] 을 쓴다) */
    fun canSummon(gateEnergy: Int): Boolean = hasEnergy(gateEnergy)

    /** 소환 잠금 안내 문구 — 해금됐으면 null */
    fun lockReason(card: TcgCard): String? {
        if (AffinityRules.canRealitySummon(card.bondLevel)) return null
        val need = CASUAL_UNLOCK_BOND - card.bondLevel.coerceAtLeast(0)
        return "🔒 일상 사복 미해금 — 친밀도 $CASUAL_UNLOCK_BOND 필요 (현재 ${card.bondLevel}, $need 더 필요)"
    }

    /**
     * 소환 실행 — 조건을 확인하고 친밀도를 올린다(직접 변경) 뒤 결과를 돌려준다.
     * 차원 에너지 차감·버프 지속 턴 관리는 ViewModel이 결과를 받아 처리한다.
     */
    fun summon(card: TcgCard, gateEnergy: Int): SummonResult {
        val lock = lockReason(card)
        if (lock != null) {
            return SummonResult(
                isSuccess = false,
                cardId = card.id,
                cardName = card.name,
                bondBefore = card.bondLevel,
                bondAfter = card.bondLevel,
                requiredBond = CASUAL_UNLOCK_BOND,
                failureReason = "$lock — 전투 출전(1회 +${AffinityRules.BATTLE_WIN_GAIN})으로 먼저 친밀도를 올리세요."
            )
        }
        if (gateEnergy < ENERGY_COST) {
            return SummonResult(
                isSuccess = false,
                cardId = card.id,
                cardName = card.name,
                bondBefore = card.bondLevel,
                failureReason = "차원 균열 에너지가 부족합니다. (필요 ${ENERGY_COST} · 보유 $gateEnergy)"
            )
        }
        val change = AffinityRules.applyGain(card, BOND_GAIN)
        val dateUnlocked = VisualStateUnlock.date(change)
        val ultimateUnlocked = VisualStateUnlock.ultimate(change)
        return SummonResult(
            isSuccess = true,
            cardId = card.id,
            cardName = card.name,
            bondBefore = change.before,
            bondAfter = change.after,
            energySpent = ENERGY_COST,
            buffPercent = BUFF_PERCENT,
            buffBattles = BUFF_BATTLES,
            unlockedDateLook = dateUnlocked,
            message = "[현실 소환] ${card.name} 친밀도 ${change.before} → ${change.after}" +
                (if (dateUnlocked) " · 🎉 데이트 룩 개방!" else "") +
                (if (ultimateUnlocked) " · 페어 얼티밋 오의 개방!" else "") +
                " · 아군 공격력 +$BUFF_PERCENT% ($BUFF_BATTLES" + "회 전투)"
        )
    }

    /** 해금 판정 헬퍼 — 상승 결과에서 "이번에 열렸는지"만 뽑는다 */
    private object VisualStateUnlock {
        fun date(change: AffinityRules.BondChange): Boolean =
            change.newlyUnlocked.contains(com.game.tcg.core.model.VisualState.ROMANTIC_DATE)

        fun ultimate(change: AffinityRules.BondChange): Boolean =
            change.before < ULTIMATE_UNLOCK_BOND && change.after >= ULTIMATE_UNLOCK_BOND
    }
}
