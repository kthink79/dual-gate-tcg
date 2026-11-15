package com.game.tcg.core.engine

import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.TcgCard

/** 강화 1회 비용 */
data class UpgradeCost(val gold: Int, val shards: Int)

/**
 * 강화 결과 (카드 스탯은 엔진이 직접 올린다)
 */
data class UpgradeResult(
    val isSuccess: Boolean,
    val cardName: String = "",
    val cardId: Int = 0,
    val fromLevel: Int = 0,
    val toLevel: Int = 0,
    val atkGain: Int = 0,
    val defGain: Int = 0,
    val goldSpent: Int = 0,
    val shardsSpent: Int = 0,
    val walletAfter: ShardWallet = ShardWallet(),
    val failureReason: String? = null,
    val message: String = ""
)

/**
 * 카드 강화(업그레이드) 엔진 — 2026-11-16 사용자 요청 "카드 업그레이드" 서브 플로우.
 *
 * - **골드 + 그 카드의 조각**을 소모해 Lv.1 → Lv.10까지 올린다 (최대 [MAX_LEVEL]).
 * - 1레벨마다 **원본(기본) ATK·DEF의 6%** 씩 영구 상승 → 덱 총 전투력·전장 HP에 즉시 반영.
 * - 레벨이 오를수록 비용이 비싸진다 (골드 150 + 120×Lv · 조각 3 + 2×Lv).
 *
 * 카드의 [TcgCard.upgradeLevel] 은 v0.3 로그라이크 규칙에 따라 사망 시 0으로 초기화된다.
 */
interface ICardUpgradeEngine {
    /** 다음 강화 비용 (최대 레벨이면 null) */
    fun cost(card: TcgCard): UpgradeCost?

    /** 강화 실행 */
    fun upgrade(card: TcgCard, gold: Int, wallet: ShardWallet): UpgradeResult
}

class CardUpgradeEngine : ICardUpgradeEngine {

    override fun cost(card: TcgCard): UpgradeCost? {
        if (card.upgradeLevel >= MAX_LEVEL - 1) return null
        val level = card.upgradeLevel
        return UpgradeCost(
            gold = GOLD_BASE + GOLD_PER_LEVEL * level,
            shards = SHARD_BASE + SHARD_PER_LEVEL * level
        )
    }

    override fun upgrade(card: TcgCard, gold: Int, wallet: ShardWallet): UpgradeResult {
        val cost = cost(card)
            ?: return UpgradeResult(
                isSuccess = false,
                cardName = card.name,
                cardId = card.id,
                fromLevel = card.upgradeLevel + 1,
                toLevel = card.upgradeLevel + 1,
                failureReason = "이미 최대 레벨(Lv.$MAX_LEVEL)입니다."
            )
        if (gold < cost.gold) {
            return UpgradeResult(
                isSuccess = false,
                cardName = card.name,
                cardId = card.id,
                failureReason = "골드가 부족합니다. (필요 ${cost.gold}G · 보유 ${gold}G)"
            )
        }
        val nextWallet = wallet.spend(card.id, cost.shards)
            ?: return UpgradeResult(
                isSuccess = false,
                cardName = card.name,
                cardId = card.id,
                failureReason = "조각이 부족합니다. (필요 ${cost.shards}개 · 보유 ${wallet.of(card.id)}개)"
            )

        val fromLevel = card.upgradeLevel + 1
        val atkGain = (card.baseAtk * GROWTH_PERCENT / 100.0).toInt().coerceAtLeast(1)
        val defGain = (card.baseDef * GROWTH_PERCENT / 100.0).toInt().coerceAtLeast(1)
        card.atk += atkGain
        card.def += defGain
        card.upgradeLevel += 1
        val toLevel = card.upgradeLevel + 1

        return UpgradeResult(
            isSuccess = true,
            cardName = card.name,
            cardId = card.id,
            fromLevel = fromLevel,
            toLevel = toLevel,
            atkGain = atkGain,
            defGain = defGain,
            goldSpent = cost.gold,
            shardsSpent = cost.shards,
            walletAfter = nextWallet,
            message = "[강화] ${card.name} Lv.$fromLevel → Lv.$toLevel " +
                "(ATK +$atkGain · DEF +$defGain) · ${cost.gold}G · 조각 -${cost.shards}"
        )
    }

    companion object {
        /** 최대 강화 레벨 (Lv.1 ~ Lv.10) */
        const val MAX_LEVEL: Int = 10

        /** 레벨업 1회당 기본 스탯 상승률 (%) */
        const val GROWTH_PERCENT: Int = 6

        const val GOLD_BASE: Int = 150
        const val GOLD_PER_LEVEL: Int = 120
        const val SHARD_BASE: Int = 3
        const val SHARD_PER_LEVEL: Int = 2
    }
}
