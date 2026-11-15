package com.game.tcg.core.engine

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.CardRarity
import com.game.tcg.core.model.CharacterRecord
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.TcgCard

/** 연성 종류 */
enum class CraftKind(val displayName: String) {
    STANDARD("신규 연성"),
    MID_BOSS("심연 연성")
}

/** 연성 가능한 대상 1개 */
data class CraftTarget(
    val record: CharacterRecord,
    val kind: CraftKind,
    val shardCost: Int,
    val goldCost: Int,
    val rarity: CardRarity,
    val blockedReason: String? = null
) {
    val isAvailable: Boolean get() = blockedReason == null
    val label: String get() = "${record.name} (${rarity.displayName})"
    val costLabel: String get() = "조각 $shardCost · ${goldCost}G"
}

/** 연성 결과 */
data class CraftResult(
    val isSuccess: Boolean,
    val record: CharacterRecord? = null,
    val card: TcgCard? = null,
    val walletAfter: ShardWallet = ShardWallet(),
    val goldSpent: Int = 0,
    val shardsSpent: Int = 0,
    val failureReason: String? = null,
    val message: String = ""
)

/** 초월(성급 승급) 결과 */
data class TranscendResult(
    val isSuccess: Boolean,
    val cardId: Int = 0,
    val cardName: String = "",
    val fromStar: Int = 0,
    val toStar: Int = 0,
    val goldSpent: Int = 0,
    val shardsSpent: Int = 0,
    val walletAfter: ShardWallet = ShardWallet(),
    val failureReason: String? = null,
    val message: String = ""
)

/**
 * 연성(합성) · 초월 엔진 — 2026-11-16 사용자 요청 "합성" 서브 플로우.
 *
 * 기존 2웨이 합성(한계 돌파·잡카드 연성)을 **조각 기반 확정 연성**으로 확장한다.
 *
 * - **신규 연성**: 카드팩 중복으로 모은 조각 [CRAFT_SHARDS] + [CRAFT_GOLD]G → 미보유 캐릭터 1명 **확정 계약**.
 *   대상은 5코스트 이상(희귀 등급 이상)만 — "상위 등급/고코스트 미보유 캐릭터 확정 연성".
 * - **심연 연성**: 중간 보스(No.170~175)는 그 보스가 서는 스테이지를 깬 뒤에만
 *   조각 [MID_BOSS_SHARDS] + [MID_BOSS_GOLD]G로 계약할 수 있다.
 * - **초월**: 그 카드의 조각 [TRANSCEND_SHARDS] + [TRANSCEND_GOLD]G → 성급 +1 (최대 5★, ATK/DEF +20%).
 *   같은 카드가 또 나오지 않아도 조각으로 한계 돌파를 이어갈 수 있다.
 */
object AdvancedSynthesisEngine {

    /** 신규 연성 — 조각 */
    const val CRAFT_SHARDS: Int = 40

    /** 신규 연성 — 골드 */
    const val CRAFT_GOLD: Int = 500

    /** 심연 연성(중간 보스) — 조각 */
    const val MID_BOSS_SHARDS: Int = 60

    /** 심연 연성(중간 보스) — 골드 */
    const val MID_BOSS_GOLD: Int = 1200

    /** 초월 — 조각 */
    const val TRANSCEND_SHARDS: Int = 40

    /** 초월 — 골드 */
    const val TRANSCEND_GOLD: Int = 600

    /** 신규 연성 대상 최소 코스트 (희귀 이상) */
    const val CRAFT_MIN_COST: Int = 5

    /** 해당 중간 보스가 서는 스테이지 번호 */
    fun midBossStageOf(bossId: Int): Int? =
        StageCatalog.MID_BOSS_STAGES.entries.firstOrNull { it.value == bossId }?.key

    /**
     * 연성 후보 목록 — 미보유 캐릭터(신규 연성) + 스테이지를 깬 중간 보스(심연 연성).
     * 마왕(No.112~114)은 토벌·서약으로만 합류하므로 연성 대상이 아니다.
     */
    fun targets(
        ownedCardIds: Collection<Int>,
        clearedStages: Set<Int>,
        wallet: ShardWallet,
        gold: Int
    ): List<CraftTarget> {
        val owned = ownedCardIds.toSet()

        val standard = CardCatalog.ALL
            .filter { it.id !in owned && !it.isDemonLord && it.baseCost >= CRAFT_MIN_COST }
            .sortedBy { it.id }
            .map { record ->
                CraftTarget(
                    record = record,
                    kind = CraftKind.STANDARD,
                    shardCost = CRAFT_SHARDS,
                    goldCost = CRAFT_GOLD,
                    rarity = CardRarity.of(record),
                    blockedReason = blockedReason(CRAFT_SHARDS, CRAFT_GOLD, wallet, gold)
                )
            }

        val midBosses = CardCatalog.midBosses()
            .filter { it.id !in owned }
            .map { record ->
                val stage = midBossStageOf(record.id)
                val cleared = stage != null && stage in clearedStages
                CraftTarget(
                    record = record,
                    kind = CraftKind.MID_BOSS,
                    shardCost = MID_BOSS_SHARDS,
                    goldCost = MID_BOSS_GOLD,
                    rarity = CardRarity.of(record),
                    blockedReason = when {
                        !cleared -> "스테이지 $stage 의 보스 격파 후 해금"
                        else -> blockedReason(MID_BOSS_SHARDS, MID_BOSS_GOLD, wallet, gold)
                    }
                )
            }

        return standard + midBosses
    }

    private fun blockedReason(shardCost: Int, goldCost: Int, wallet: ShardWallet, gold: Int): String? = when {
        wallet.total < shardCost -> "조각 부족 (${wallet.total}/$shardCost)"
        gold < goldCost -> "골드 부족 (${gold}G/$goldCost)"
        else -> null
    }

    /** 연성 실행 — 성공하면 새 카드([CraftResult.card])를 돌려준다 */
    fun craft(
        record: CharacterRecord,
        kind: CraftKind,
        wallet: ShardWallet,
        gold: Int
    ): CraftResult {
        val shardCost = if (kind == CraftKind.MID_BOSS) MID_BOSS_SHARDS else CRAFT_SHARDS
        val goldCost = if (kind == CraftKind.MID_BOSS) MID_BOSS_GOLD else CRAFT_GOLD
        if (gold < goldCost) {
            return CraftResult(isSuccess = false, failureReason = "골드가 부족합니다. (필요 ${goldCost}G · 보유 ${gold}G)")
        }
        val nextWallet = wallet.spendAny(shardCost)
            ?: return CraftResult(isSuccess = false, failureReason = "조각이 부족합니다. (필요 $shardCost · 보유 ${wallet.total})")
        val card = record.toCard()
        val tag = if (kind == CraftKind.MID_BOSS) "심연 연성" else "신규 연성"
        return CraftResult(
            isSuccess = true,
            record = record,
            card = card,
            walletAfter = nextWallet,
            goldSpent = goldCost,
            shardsSpent = shardCost,
            message = "[$tag] ${record.fullName} 계약 완료! (조각 -$shardCost · ${goldCost}G)"
        )
    }

    /**
     * 초월(성급 승급) — 그 카드의 조각을 써서 성급을 올린다.
     * 이미 5성이면 실패. ATK/DEF +20% (한계 돌파와 같은 계수).
     */
    fun transcend(card: TcgCard, wallet: ShardWallet, gold: Int): TranscendResult {
        if (card.starLevel >= 5) {
            return TranscendResult(
                isSuccess = false,
                cardId = card.id,
                cardName = card.name,
                fromStar = card.starLevel,
                toStar = card.starLevel,
                failureReason = "이미 5성 최대치입니다."
            )
        }
        if (gold < TRANSCEND_GOLD) {
            return TranscendResult(
                isSuccess = false,
                cardId = card.id,
                cardName = card.name,
                fromStar = card.starLevel,
                failureReason = "골드가 부족합니다. (필요 ${TRANSCEND_GOLD}G · 보유 ${gold}G)"
            )
        }
        val nextWallet = wallet.spend(card.id, TRANSCEND_SHARDS)
            ?: return TranscendResult(
                isSuccess = false,
                cardId = card.id,
                cardName = card.name,
                fromStar = card.starLevel,
                failureReason = "${card.name} 조각이 부족합니다. (필요 ${TRANSCEND_SHARDS} · 보유 ${wallet.of(card.id)})"
            )
        val fromStar = card.starLevel
        card.starLevel += 1
        card.atk = (card.atk * 1.20).toInt()
        card.def = (card.def * 1.20).toInt()
        return TranscendResult(
            isSuccess = true,
            cardId = card.id,
            cardName = card.name,
            fromStar = fromStar,
            toStar = card.starLevel,
            goldSpent = TRANSCEND_GOLD,
            shardsSpent = TRANSCEND_SHARDS,
            walletAfter = nextWallet,
            message = "[초월] ${card.name} $fromStar★ → ${card.starLevel}★ (ATK/DEF +20%) · 조각 -$TRANSCEND_SHARDS"
        )
    }
}
