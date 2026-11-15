package com.game.tcg.core.engine

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.CardRarity
import com.game.tcg.core.model.PackCatalog
import com.game.tcg.core.model.PackDefinition
import com.game.tcg.core.model.PackDraw
import com.game.tcg.core.model.PackOpenResult
import kotlin.random.Random

/**
 * 카드팩 구매·개봉 엔진.
 *
 * - 등급(CardRarity)별 가중치로 카드 1장을 뽑는다.
 * - 이미 보유한 카드가 나오면 **중복** → 그 카드의 조각 [SHARDS_PER_DUPLICATE] + 소량 골드 [GOLD_PER_DUPLICATE]
 *   로 자동 변환되어 강화·연성 재료가 된다.
 * - 마왕(No.112~114)과 중간 보스(No.170~175)는 카드팩 풀에서 제외한다
 *   (마왕은 토벌 후 서약, 중간 보스는 「심연 연성」으로만 계약).
 */
interface IPackShopEngine {
    /** 이 팩을 지금 열 수 있는지 (재화 검사) */
    fun canAfford(pack: PackDefinition, gold: Int, tickets: Int): Boolean

    /** 팩 1회 개봉 */
    fun open(
        pack: PackDefinition,
        ownedCardIds: Collection<Int>,
        random: Random = Random.Default
    ): PackOpenResult
}

class PackShopEngine : IPackShopEngine {

    /** 티켓이 있으면 티켓, 없으면 골드로 살 수 있다 (둘 중 하나) */
    override fun canAfford(pack: PackDefinition, gold: Int, tickets: Int): Boolean {
        if (pack.ticketCost > 0 && tickets >= pack.ticketCost) return true
        return gold >= pack.goldCost
    }

    override fun open(
        pack: PackDefinition,
        ownedCardIds: Collection<Int>,
        random: Random
    ): PackOpenResult {
        val pool = poolFor(pack)
        val owned = ownedCardIds.toSet()
        val draws = mutableListOf<PackDraw>()

        for (index in 0 until pack.cardCount) {
            // 마지막 1장이 확정 등급 조건을 채워야 하면 그 등급 이상에서 뽑는다
            val needGuarantee = pack.guaranteedRarity != null &&
                index == pack.cardCount - 1 &&
                draws.none { it.rarity.ordinal >= pack.guaranteedRarity.ordinal }
            val picked = if (needGuarantee) {
                pickFromRarity(pool, pack.guaranteedRarity!!, random)
            } else {
                pickWeighted(pool, random, pack)
            }
            draws += toDraw(picked, owned)
        }

        val newIds = draws.filter { it.isNew }.map { it.cardId }
        val shards = draws.sumOf { it.shards }
        val gold = draws.sumOf { it.gold }
        val summary = if (newIds.isEmpty()) {
            "${pack.name} 개봉 — 신규 없음 · 조각 +$shards · ${gold}G"
        } else {
            "${pack.name} 개봉 — 신규 ${newIds.size}명 계약! (" +
                draws.filter { it.isNew }.joinToString(", ") { it.name } + ")"
        }

        return PackOpenResult(
            packId = pack.id,
            packName = pack.name,
            draws = draws,
            goldSpent = pack.goldCost,
            ticketsSpent = pack.ticketCost,
            newCardIds = newIds,
            totalShards = shards,
            totalGold = gold,
            message = summary
        )
    }

    /** 팩 종류별 카드 풀 (플레이 가능 로스터 · 마왕/중간보스 제외) */
    fun poolFor(pack: PackDefinition): List<com.game.tcg.core.model.CharacterRecord> {
        val base = CardCatalog.ALL.filter { !it.isDemonLord && !it.isMidBoss }
        return if (pack.element == null) base else base.filter { it.element == pack.element }
    }

    /** 등급 가중치 — 흔한 등급이 더 자주 나온다 (고급팩은 상위 등급 비중↑) */
    private fun weightOf(pack: PackDefinition, rarity: CardRarity): Int {
        val premium = pack.kind == com.game.tcg.core.model.PackKind.PREMIUM
        val elemental = pack.kind == com.game.tcg.core.model.PackKind.ELEMENTAL
        return when (rarity) {
            CardRarity.COMMON -> if (premium) 12 else if (elemental) 40 else 52
            CardRarity.RARE -> if (premium) 40 else if (elemental) 36 else 32
            CardRarity.EPIC -> if (premium) 38 else if (elemental) 18 else 13
            CardRarity.LEGENDARY -> if (premium) 10 else if (elemental) 6 else 3
        }
    }

    /** 가중치 랜덤으로 카드 1장 (팩 종류에 따라 상위 등급 비중이 달라진다) */
    fun pickWeighted(
        pool: List<com.game.tcg.core.model.CharacterRecord>,
        random: Random,
        pack: PackDefinition = PackCatalog.STANDARD
    ): com.game.tcg.core.model.CharacterRecord {
        if (pool.isEmpty()) return CardCatalog.require(1)
        val weighted = pool.map { it to weightOf(pack, CardRarity.of(it)) }
        val total = weighted.sumOf { it.second }
        var roll = random.nextInt(total)
        for ((record, weight) in weighted) {
            roll -= weight
            if (roll < 0) return record
        }
        return pool.last()
    }

    /** 지정 등급 이상에서 균등 추첨 (확정 보장용) */
    fun pickFromRarity(
        pool: List<com.game.tcg.core.model.CharacterRecord>,
        minRarity: CardRarity,
        random: Random
    ): com.game.tcg.core.model.CharacterRecord {
        val candidates = pool.filter { CardRarity.of(it).ordinal >= minRarity.ordinal }
        if (candidates.isEmpty()) return pickWeighted(pool, random)
        return candidates[random.nextInt(candidates.size)]
    }

    private fun toDraw(
        record: com.game.tcg.core.model.CharacterRecord,
        owned: Set<Int>
    ): PackDraw {
        val isNew = record.id !in owned
        return PackDraw(
            cardId = record.id,
            name = record.name,
            rarity = CardRarity.of(record),
            isNew = isNew,
            shards = if (isNew) 0 else SHARDS_PER_DUPLICATE,
            gold = if (isNew) 0 else GOLD_PER_DUPLICATE
        )
    }

    companion object {
        /** 중복 카드 1장 → 조각 */
        const val SHARDS_PER_DUPLICATE: Int = 10

        /** 중복 카드 1장 → 골드 */
        const val GOLD_PER_DUPLICATE: Int = 50
    }
}
