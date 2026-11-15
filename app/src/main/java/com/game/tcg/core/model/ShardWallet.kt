package com.game.tcg.core.model

/**
 * 카드별 조각 지갑 (불변).
 *
 * 중복 카드·스테이지 보상으로 쌓이며 **강화(업그레이드)** 와 **연성(합성)** 의 공통 재료다.
 * - 강화: 그 카드의 조각을 쓴다.
 * - 연성: 어느 카드의 조각이든 합산해 쓴다 ([spendAny]).
 */
data class ShardWallet(val byCard: Map<Int, Int> = emptyMap()) {

    /** 보유 조각 총합 */
    val total: Int get() = byCard.values.sum()

    fun of(cardId: Int): Int = byCard[cardId] ?: 0

    fun add(cardId: Int, amount: Int): ShardWallet {
        if (amount <= 0) return this
        return copy(byCard = byCard + (cardId to of(cardId) + amount))
    }

    /** 특정 카드의 조각이 충분한지 */
    fun canSpend(cardId: Int, amount: Int): Boolean = of(cardId) >= amount

    /** 특정 카드 조각을 쓴다 (부족하면 null) */
    fun spend(cardId: Int, amount: Int): ShardWallet? {
        if (amount <= 0) return this
        if (!canSpend(cardId, amount)) return null
        val left = of(cardId) - amount
        return copy(byCard = if (left == 0) byCard - cardId else byCard + (cardId to left))
    }

    /**
     * 어느 카드의 조각이든 합산해 쓴다 — 연성 재료.
     * 조각이 많은 카드부터 차감해 강화 재료를 최대한 남긴다.
     * 부족하면 null.
     */
    fun spendAny(amount: Int): ShardWallet? {
        if (amount <= 0) return this
        if (total < amount) return null
        var remain = amount
        val sorted = byCard.entries.sortedByDescending { it.value }
        val next = byCard.toMutableMap()
        for ((cardId, have) in sorted) {
            if (remain <= 0) break
            val take = minOf(have, remain)
            val left = have - take
            if (left == 0) next.remove(cardId) else next[cardId] = left
            remain -= take
        }
        return copy(byCard = next)
    }
}
