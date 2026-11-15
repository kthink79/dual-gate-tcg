package com.game.tcg.core.model

import kotlin.random.Random

/**
 * 게임 시작 상태 — 2026-11-16 사용자 확정 규칙.
 *
 * > "처음 시작은 케릭터 3장(낮은 레벨 케릭터 중에서 랜덤 취득)"
 *
 * - 보유 카드: **코스트 3~4 저코스트 풀에서 무작위 3장** ([draw])
 * - 나머지 167명은 잠금(미보유) → 도감에서 실루엣·자물쇠로 보이고, 카드팩·연성으로 획득한다
 * - 출전 덱: 뽑힌 3장을 9칸 덱의 **전열 3칸(0·1·2)** 에 자동 배치하고 시작 (나머지 6칸은 빈 슬롯)
 *
 * 3장은 서로 다른 속성이 나오도록 우선 배분한다(같은 속성만 뽑히면 상성 놀이가 사라지므로).
 * 마왕(No.112~114)과 중간 보스(No.170~175)는 시작 풀에서 제외한다.
 */
object StartingRoster {

    /** 시작 시 보유하는 카드 수 */
    const val STARTER_COUNT: Int = 3

    /** 시작 카드의 코스트 구간 (저코스트·저등급) */
    val STARTER_COST_RANGE: IntRange = 3..4

    /** 시작 후보 풀 — 코스트 3~4의 플레이 가능 캐릭터 */
    val POOL_IDS: List<Int> = CardCatalog.ALL
        .filter { it.baseCost in STARTER_COST_RANGE && !it.isDemonLord && !it.isMidBoss }
        .map { it.id }

    /** 시작 카드가 놓이는 슬롯 — 9칸 덱의 전열 3칸 */
    val STARTER_SLOTS: List<Int> = DeckRules.FRONT_SLOTS.let { (0 until it).toList() }

    /**
     * 시작 카드 3장을 뽑는다.
     *
     * 속성이 서로 다른 카드를 먼저 고르고, 풀에 남은 속성이 모자라면 남은 풀에서 채운다.
     * 같은 시드([random])에는 항상 같은 결과가 나온다(테스트·재현용).
     */
    fun draw(random: Random = Random.Default): List<Int> {
        val pool = POOL_IDS.shuffled(random)
        val picked = mutableListOf<Int>()
        val usedElements = mutableSetOf<Element>()
        pool.forEach { id ->
            if (picked.size >= STARTER_COUNT) return@forEach
            val element = CardCatalog.require(id).element
            if (element !in usedElements) {
                picked += id
                usedElements += element
            }
        }
        if (picked.size < STARTER_COUNT) {
            pool.forEach { id ->
                if (picked.size < STARTER_COUNT && id !in picked) picked += id
            }
        }
        return picked.take(STARTER_COUNT)
    }

    /** 뽑힌 3장을 실제 카드로 만든다 (호감도 시드 50 유지) */
    fun starterCards(ids: List<Int> = draw()): List<TcgCard> =
        ids.map { CardCatalog.require(it).toCard() }

    /**
     * 시작 덱 배치 — 3장을 전열 0·1·2에 넣는다.
     * 방어력이 높은 카드를 앞(전1)에 세워 적의 1차 공격을 받게 한다.
     */
    fun deploySlots(cards: List<TcgCard>): List<Pair<TcgCard, Int>> =
        cards.sortedByDescending { it.def }
            .take(STARTER_COUNT)
            .mapIndexed { index, card -> card to (STARTER_SLOTS.getOrNull(index) ?: index) }
}
