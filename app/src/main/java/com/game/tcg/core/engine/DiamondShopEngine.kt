package com.game.tcg.core.engine

import com.game.tcg.core.model.CharacterRecord
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 💎 다이아 재화 사용처 — 2026-11-27 사용자 요청(v0.6.0).
 *
 * 사용자 원문(대기열 `tcg_v060_bottom_padding_deck_tabs_and_diamond_shop_expansion`):
 *  - 💎 5개 · 캐릭터 지정 뽑기 — 원하는 캐릭터 1명을 직접 선택해 소환/영입
 *  - 💎 3개 · 에너지/성수 충전 — 차원 균열 에너지 또는 마왕 정화용 성수 즉시 충전
 *  - 💎 2개 · 강화석/친밀도 선물 교환 — 캐릭터 강화 재료 및 호감도 상승용 선물 교환
 *  - 💎 4개 · 데이트 룩 즉시 해금 — 보유 캐릭터의 데이트 룩을 다이아로 바로 해금
 *
 * 이 객체는 Android에 의존하지 않는 **순수 Kotlin** 이다. 화면은 결과만 그대로 보여 주고,
 * 가격·상한·잠금 판정은 전부 여기 한 곳에서 검증한다(JVM 단위 테스트 대상).
 */
object DiamondShopEngine {

    // ── 가격 (사용자 확정) ──────────────────────────────────────────
    /** 캐릭터 지정 뽑기 1명 */
    const val PRICE_TARGET_SUMMON: Int = 5

    /** 에너지 또는 성수 충전 1회 */
    const val PRICE_RECHARGE: Int = 3

    /** 강화석 또는 친밀도 선물 교환 1회 */
    const val PRICE_EXCHANGE: Int = 2

    /** 데이트 룩 즉시 해금 1명 */
    const val PRICE_DATE_LOOK: Int = 4

    // ── 지급량 ──────────────────────────────────────────────────────
    /** 성수 충전 시 지급량 (정화 1회가 성수 1개를 쓴다) */
    const val HOLY_WATER_GAIN: Int = 2

    /** 강화석 교환 시 그 캐릭터에게 들어가는 조각(강화 재료) 수 — 5레벨 강화 1회분(3 + 2×5 = 13)에 못 미치는 보조량 */
    const val SHARD_GAIN: Int = 10

    /** 친밀도 선물 1회 상승분 — 전투 1승(+6)에 맞먹지 않는 보조 경로 */
    const val BOND_GIFT_GAIN: Int = 5

    /** 💎 상점 탭 4종 — 화면 순서·아이콘·가격을 한 곳에서 정의한다 */
    enum class Tab(val icon: String, val label: String, val price: Int) {
        TARGET_SUMMON("✨", "지정 소환", PRICE_TARGET_SUMMON),
        RECHARGE("🔋", "충전", PRICE_RECHARGE),
        EXCHANGE("🎁", "교환", PRICE_EXCHANGE),
        DATE_LOOK("🌹", "데이트", PRICE_DATE_LOOK);
    }

    /** 💎 3 충전 대상 2종 */
    enum class RechargeKind(val icon: String, val label: String) {
        GATE_ENERGY("🌀", "차원 균열 에너지"),
        HOLY_WATER("💧", "성수")
    }

    /** 💎 2 교환 대상 2종 */
    enum class ExchangeKind(val icon: String, val label: String) {
        UPGRADE_SHARD("⚒", "강화석 ${SHARD_GAIN}"),
        AFFINITY_GIFT("🎁", "친밀도 선물 +${BOND_GIFT_GAIN}")
    }

    /**
     * 구매 결과 — 성공/실패와 지급 내역을 함께 돌려준다.
     * 화면은 [message] 한 줄을 그대로 보여 주고, 상태 갱신은 지급 필드를 쓴다.
     */
    data class Purchase(
        val isSuccess: Boolean,
        val gemsSpent: Int = 0,
        val message: String,
        val failureReason: String? = null,
        /** 지정 소환으로 영입한 캐릭터 (성공 시에만 non-null) */
        val summonedCardId: Int? = null,
        val energyGain: Int = 0,
        val holyWaterGain: Int = 0,
        val shardGain: Int = 0,
        val bondGain: Int = 0,
        /** 데이트 룩 해금 대상 (성공 시에만 non-null) */
        val dateLookCardId: Int? = null
    ) {
        companion object {
            /** 실패 — 다이아는 쓰지 않는다 */
            fun fail(reason: String) = Purchase(
                isSuccess = false,
                gemsSpent = 0,
                message = "[💎 구매 불가] $reason",
                failureReason = reason
            )
        }
    }

    /**다이아가 모자란지 — 모든 구매가 같은 문구를 쓴다 */
    private fun shortOfGems(price: Int, gems: Int): Purchase? =
        if (gems < price) {
            Purchase.fail("💎 $price 필요 (보유 💎 ${gems.coerceAtLeast(0)})")
        } else null

    /**
     * ✨ 캐릭터 지정 뽑기 — 미보유 캐릭터 1명을 💎 [PRICE_TARGET_SUMMON] 로 영입한다.
     *
     * 이미 보유한 캐릭터는 대상이 아니다(중복 영입으로 다이아를 낭비하지 않게).
     * 중간 보스(No.170~175)는 스테이지 격파 보상이므로 화면에서 후보에서 빼고,
     * 여기서도 한 번 더 막는다.
     */
    fun summonTarget(
        record: CharacterRecord,
        ownedIds: Collection<Int>,
        gems: Int
    ): Purchase {
        if (com.game.tcg.core.model.MidBossCatalog.isMidBoss(record.id)) {
            return Purchase.fail("중간 보스 ${record.name}은(는) 스테이지 격파로만 계약할 수 있습니다.")
        }
        if (record.id in ownedIds) {
            return Purchase.fail("${record.name}은(는) 이미 보유한 캐릭터입니다.")
        }
        shortOfGems(PRICE_TARGET_SUMMON, gems)?.let { return it }
        return Purchase(
            isSuccess = true,
            gemsSpent = PRICE_TARGET_SUMMON,
            message = "[💎 지정 소환] ${record.fullName} 영입! (💎 $PRICE_TARGET_SUMMON)",
            summonedCardId = record.id
        )
    }

    /**
     * 🔋 차원 균열 에너지 즉시 충전 — 💎 [PRICE_RECHARGE] 로 [energyMax] 까지 채운다.
     * 가득 찬 상태에서는 다이아를 쓰지 않는다.
     */
    fun rechargeEnergy(gems: Int, currentEnergy: Int, energyMax: Int): Purchase {
        val max = energyMax.coerceAtLeast(0)
        val current = currentEnergy.coerceIn(0, max)
        if (current >= max) {
            return Purchase.fail("차원 균열 에너지가 이미 가득 찼습니다. ($current/$max)")
        }
        shortOfGems(PRICE_RECHARGE, gems)?.let { return it }
        val gain = (max - current).coerceAtMost(ENERGY_RECHARGE_MAX_GAIN)
        return Purchase(
            isSuccess = true,
            gemsSpent = PRICE_RECHARGE,
            message = "[💎 충전] 차원 균열 에너지 +$gain ($current/$max → ${current + gain}/$max)",
            energyGain = gain
        )
    }

    /** 💧 성수 즉시 충전 — 💎 [PRICE_RECHARGE] 로 성수 +[HOLY_WATER_GAIN] */
    fun rechargeHolyWater(gems: Int): Purchase {
        shortOfGems(PRICE_RECHARGE, gems)?.let { return it }
        return Purchase(
            isSuccess = true,
            gemsSpent = PRICE_RECHARGE,
            message = "[💎 충전] 마왕 정화용 성수 +$HOLY_WATER_GAIN (💎 $PRICE_RECHARGE)",
            holyWaterGain = HOLY_WATER_GAIN
        )
    }

    /** ⚒ 강화석(조각) 교환 — 💎 [PRICE_EXCHANGE] 로 그 캐릭터의 강화 재료 +[SHARD_GAIN] */
    fun exchangeShards(card: TcgCard?, gems: Int): Purchase {
        if (card == null) return Purchase.fail("강화석을 받을 캐릭터를 먼저 선택하세요.")
        shortOfGems(PRICE_EXCHANGE, gems)?.let { return it }
        return Purchase(
            isSuccess = true,
            gemsSpent = PRICE_EXCHANGE,
            message = "[💎 교환] ${card.name} 강화석 +$SHARD_GAIN (💎 $PRICE_EXCHANGE)",
            shardGain = SHARD_GAIN
        )
    }

    /** 🎁 친밀도 선물 교환 — 💎 [PRICE_EXCHANGE] 로 친밀도 +[BOND_GIFT_GAIN] (최대 100) */
    fun giftAffinity(card: TcgCard?, bond: Int, gems: Int): Purchase {
        if (card == null) return Purchase.fail("선물을 줄 캐릭터를 먼저 선택하세요.")
        val current = bond.coerceIn(AffinityRules.MIN_BOND, AffinityRules.MAX_BOND)
        if (current >= AffinityRules.MAX_BOND) {
            return Purchase.fail("${card.name}의 친밀도가 이미 최대(100)입니다.")
        }
        shortOfGems(PRICE_EXCHANGE, gems)?.let { return it }
        val gain = (AffinityRules.MAX_BOND - current).coerceAtMost(BOND_GIFT_GAIN)
        return Purchase(
            isSuccess = true,
            gemsSpent = PRICE_EXCHANGE,
            message = "[💎 선물] ${card.name} 친밀도 +$gain ($current → ${current + gain})",
            bondGain = gain
        )
    }

    /**
     * 🌹 데이트 룩 즉시 해금 — 친밀도 70을 기다리지 않고 💎 [PRICE_DATE_LOOK] 로 연다.
     * 이미 열린 캐릭터(친밀도 70 이상 또는 기존 해금)에게는 팔지 않는다.
     */
    fun unlockDateLook(card: TcgCard?, bond: Int, alreadyUnlocked: Boolean, gems: Int): Purchase {
        if (card == null) return Purchase.fail("데이트 룩을 열 캐릭터를 먼저 선택하세요.")
        if (alreadyUnlocked || AffinityRules.isDateUnlocked(bond)) {
            return Purchase.fail("${card.name}의 데이트 룩은 이미 열려 있습니다.")
        }
        shortOfGems(PRICE_DATE_LOOK, gems)?.let { return it }
        return Purchase(
            isSuccess = true,
            gemsSpent = PRICE_DATE_LOOK,
            message = "[💎 해금] ${card.name} 데이트 룩 개방! (💎 $PRICE_DATE_LOOK)",
            dateLookCardId = card.id
        )
    }

    /**
     * 복장 해금 판정 — 친밀도 규칙([AffinityRules])에 **다이아로 해금한 데이트 룩**을 더한다.
     *
     * 로비·도감·덱 편성이 모두 이 함수를 거치므로, 다이아로 연 데이트 룩은
     * 앱을 다시 켜도(저장된 [dateLookUnlockedIds]) 세 화면에서 똑같이 열려 있다.
     */
    fun isVisualUnlocked(
        cardId: Int,
        visual: VisualState,
        bond: Int,
        dateLookUnlockedIds: Set<Int>
    ): Boolean {
        if (visual == VisualState.ROMANTIC_DATE && cardId in dateLookUnlockedIds) return true
        return AffinityRules.isVisualUnlocked(visual, bond)
    }

    /** 친밀도보다 먼저 연 데이트 룩이므로 안내 문구도 다르게 — 잠금일 때만 non-null */
    fun lockLabel(
        cardId: Int,
        visual: VisualState,
        bond: Int,
        dateLookUnlockedIds: Set<Int>
    ): String? {
        if (isVisualUnlocked(cardId, visual, bond, dateLookUnlockedIds)) return null
        return AffinityRules.lockLabel(visual, bond)?.let { "$it · 💎 $PRICE_DATE_LOOK 로 즉시 해금 가능" }
    }

    /** 화면에서 한 번에 보여 줄 수 있는 최대 충전량 (에너지 최대치가 작은 기기·저장값 보호) */
    const val ENERGY_RECHARGE_MAX_GAIN: Int = 4
}
