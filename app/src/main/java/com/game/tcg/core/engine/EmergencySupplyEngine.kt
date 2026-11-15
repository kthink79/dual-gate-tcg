package com.game.tcg.core.engine

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.PackCatalog
import com.game.tcg.core.model.StartingRoster

/**
 * 무료 긴급 보급 판정 결과.
 *
 * @param isOffered 지금 무료 지원 카드를 받을 수 있는 상태인지
 * @param cardIds 지급할 카드 번호 (지급 불가·불필요하면 빈 목록)
 * @param ownedBefore 지급 전 보유 카드 수
 * @param ownedAfter 지급 후 예상 보유 카드 수
 * @param claimsUsed 이번 판에서 이미 받은 보급 횟수
 * @param reason 판정 사유 (불필요·차단·지급 가능 사유를 그대로 화면에 보여 준다)
 */
data class SupplyOffer(
    val isOffered: Boolean,
    val cardIds: List<Int> = emptyList(),
    val ownedBefore: Int = 0,
    val ownedAfter: Int = 0,
    val claimsUsed: Int = 0,
    val claimsLeft: Int = 0,
    val reason: String = "",
    val message: String = ""
)

/**
 * 긴급 보급 · 소프트락 안전장치 (2026-11-16 QA 보완).
 *
 * 로그라이크 상실 규칙 때문에 보유 카드가 0~2장으로 줄었는데
 *   - 환생 풀에 재계약할 카드도 없고,
 *   - 카드팩을 살 골드(최저 [cheapestPackGold]G)도 티켓도 없고,
 *   - 조각 연성(조각 40 + 500G)도 불가능하면
 * 보유 카드를 늘릴 방법이 사라져 **진행이 막힌다(소프트락)**.
 *
 * 그 상태에서만 **무료 지원 카드**를 [MIN_OWNED_CARDS]장까지 지급한다.
 * 무료 재계약(환생 풀)이나 유료 획득 경로가 하나라도 열려 있으면 지급하지 않으므로,
 * 이 보급을 반복해서 카드를 불리는 파밍 경로로 쓸 수 없다.
 * ([MAX_CLAIMS]는 한 판에서의 최후 안전판 횟수 제한)
 */
object EmergencySupplyEngine {

    /** 소프트락 판정 기준 보유 카드 수 — 이보다 적으면 보급 대상 */
    const val MIN_OWNED_CARDS: Int = 3

    /** 한 판에서 무료 보급을 받을 수 있는 최대 횟수 */
    const val MAX_CLAIMS: Int = 3

    /** 신규 연성 최소 코스트 (이보다 낮은 카드는 연성으로 얻을 수 없다) */
    private const val CRAFT_MIN_COST: Int = 5

    /** 카드팩 최저 골드가 — 이 골드가 있으면 상점이라는 정상 경로가 있다 */
    val cheapestPackGold: Int get() = PackCatalog.ALL.minOfOrNull { it.goldCost } ?: 200

    /**
     * 무료 보급 필요 여부를 판정한다.
     *
     * @param ownedCardIds 현재 보유 카드 번호
     * @param lostCardIds 환생 풀(영구 상실 후 무료 재계약 가능) 카드 번호
     * @param gold 보유 골드
     * @param packTickets 보유 카드팩 티켓
     * @param shardTotal 보유 조각 합계
     * @param claimsUsed 이번 판에서 이미 받은 보급 횟수
     */
    fun evaluate(
        ownedCardIds: Set<Int>,
        lostCardIds: Set<Int>,
        gold: Int,
        packTickets: Int,
        shardTotal: Int,
        claimsUsed: Int = 0
    ): SupplyOffer {
        val owned = ownedCardIds.size
        val claimsLeft = (MAX_CLAIMS - claimsUsed).coerceAtLeast(0)
        val base = SupplyOffer(
            isOffered = false,
            ownedBefore = owned,
            ownedAfter = owned,
            claimsUsed = claimsUsed,
            claimsLeft = claimsLeft
        )

        if (owned >= MIN_OWNED_CARDS) {
            return base.copy(
                reason = "보유 ${owned}장 — 보급이 필요하지 않습니다.",
                message = ""
            )
        }
        if (lostCardIds.isNotEmpty()) {
            return base.copy(
                reason = "환생 풀에 ${lostCardIds.size}장이 있어 무료 재계약으로 되찾을 수 있습니다.",
                message = "[보급 불필요] 잃은 카드는 무료로 재계약할 수 있습니다. (${lostCardIds.size}장 대기)"
            )
        }
        if (packTickets >= 1) {
            return base.copy(
                reason = "카드팩 티켓이 있어 동료를 얻을 수 있습니다.",
                message = "[보급 불필요] 카드팩 티켓이 남아 있습니다."
            )
        }
        if (shardTotal >= AdvancedSynthesisEngine.CRAFT_SHARDS && gold >= AdvancedSynthesisEngine.CRAFT_GOLD) {
            return base.copy(
                reason = "조각 ${shardTotal} + 골드 ${gold}G — 조각 연성이 가능합니다.",
                message = "[보급 불필요] 조각 연성으로 동료를 계약할 수 있습니다."
            )
        }
        if (gold >= cheapestPackGold) {
            return base.copy(
                reason = "골드 ${gold}G — 최저가 카드팩(${cheapestPackGold}G)을 살 수 있습니다.",
                message = "[보급 불필요] 골드로 카드팩을 구매할 수 있습니다."
            )
        }
        if (claimsLeft <= 0) {
            return base.copy(
                reason = "이번 판 무료 보급 한도(${MAX_CLAIMS}회)를 모두 사용했습니다.",
                message = "[보급 한도] 이번 판의 무료 보급을 모두 받았습니다. 남은 카드는 환생 풀 재계약으로 되찾으세요."
            )
        }

        val cards = pickSupportCards(ownedCardIds, lostCardIds, MIN_OWNED_CARDS - owned)
        if (cards.isEmpty()) {
            return base.copy(
                reason = "지급할 수 있는 미보유 카드가 없습니다.",
                message = "[보급 불가] 새로 지급할 수 있는 동료 카드가 남아 있지 않습니다."
            )
        }
        val after = owned + cards.size
        return base.copy(
            isOffered = true,
            cardIds = cards,
            ownedAfter = after,
            reason = "보유 ${owned}장 · 골드 ${gold}G · 티켓 ${packTickets} · 환생 풀 0장 — 무료 보급이 필요합니다.",
            message = "[긴급 보급] 동료 ${cards.size}명이 무료로 합류합니다. (${owned}장 → ${after}장)"
        )
    }

    /**
     * 지급할 지원 카드 번호를 고른다.
     * 스타팅 풀(코스트 3~4 · 마왕·중간 보스 제외)에서 아직 보유·대기하지 않은 카드를
     * 번호가 낮은 순으로 고른다(같은 상태에는 항상 같은 결과 — 테스트·재현용).
     */
    fun pickSupportCards(
        ownedCardIds: Set<Int>,
        lostCardIds: Set<Int>,
        count: Int
    ): List<Int> {
        if (count <= 0) return emptyList()
        val excluded = ownedCardIds + lostCardIds
        val fromStarterPool = StartingRoster.POOL_IDS
            .asSequence()
            .filter { it !in excluded }
            .sorted()
            .take(count)
            .toList()
        if (fromStarterPool.size >= count) return fromStarterPool

        // 스타팅 풀이 모자라면 나머지 저코스트 미보유 카드로 채운다
        val extra = CardCatalog.ALL
            .asSequence()
            .filter { it.id !in excluded && it.id !in fromStarterPool }
            .filter { !it.isDemonLord && !it.isMidBoss && it.baseCost >= CRAFT_MIN_COST - 2 }
            .sortedBy { it.baseCost * 1000 + it.id }
            .map { it.id }
            .take(count - fromStarterPool.size)
            .toList()
        return fromStarterPool + extra
    }
}
