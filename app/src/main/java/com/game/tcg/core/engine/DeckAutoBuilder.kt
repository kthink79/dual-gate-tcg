package com.game.tcg.core.engine

import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.TcgCard

/**
 * 출전 덱(기본 9장 ~ 최대 12장) 자동 편성 · 슬롯 배치 순수 로직.
 *
 * UI(DeckBuilderScreen)와 ViewModel이 **같은 계산**을 쓰도록 여기에 모아 두고, 단위 테스트로 검증한다.
 * - 전투력 = ATK + DEF (카드 한 장의 종합 전투력)
 * - 표시 전투력 = 전투력 / 100 (카드 모서리·덱 합계에 쓰는 짧은 숫자, 예: 15400 → 154)
 * - 자동 편성: 보유 카드 중 전투력 상위 N장(현재 해금된 슬롯 수) →
 *   그중 DEF 상위 3장을 전열(0~2), 나머지를 후열·대기(3~11)에 배치
 *
 * 슬롯 번호는 [DeckRules]와 `BattleState`가 동일하게 0~2 = 전열, 3 이상 = 후열을 쓴다.
 * 해금되지 않은 슬롯([DeckRules.MAX_DECK_SIZE] 초과)은 절대 채우지 않는다.
 */
object DeckAutoBuilder {

    /** 전열 슬롯 수 (0 ~ 2) */
    const val FRONT_SLOTS: Int = DeckRules.FRONT_SLOTS

    /** 출전 슬롯 총수 — 기본 9장, 최대 12장 규칙 참조 */
    const val TOTAL_SLOTS: Int = DeckRules.MAX_DECK_SIZE

    /** 카드 종합 전투력 (ATK + DEF) */
    fun powerOf(card: TcgCard): Int = card.atk + card.def

    /** 카드 모서리에 찍는 짧은 전투력 숫자 (전투력 / 100) */
    fun displayPower(card: TcgCard): Int = powerOf(card) / 100

    /** 여러 장의 짧은 전투력 합계 — 덱 총 전투력 표시용 */
    fun displayTotalPower(cards: List<TcgCard>): Int = cards.sumOf { displayPower(it) }

    /** 원시 전투력 합계 */
    fun totalPower(cards: List<TcgCard>): Int = cards.sumOf { powerOf(it) }

    /** 슬롯 배치 순서 — 전투력 → ATK → 도감 번호 (동점이어도 항상 같은 결과) */
    private val POWER_ORDER: Comparator<TcgCard> =
        compareByDescending<TcgCard> { powerOf(it) }
            .thenByDescending { it.atk }
            .thenBy { it.id }

    /** 전열 배치 순서 — DEF → ATK → 도감 번호 */
    private val FRONT_ORDER: Comparator<TcgCard> =
        compareByDescending<TcgCard> { it.def }
            .thenByDescending { it.atk }
            .thenBy { it.id }

    /** 후열 배치 순서 — ATK → DEF → 도감 번호 */
    private val BACK_ORDER: Comparator<TcgCard> =
        compareByDescending<TcgCard> { it.atk }
            .thenByDescending { it.def }
            .thenBy { it.id }

    /** 레벨로 해금된 슬롯 수 (9 ~ 12) */
    fun slotsForLevel(level: Int): Int = DeckRules.deckSizeForLevel(level)

    /**
     * 자동 편성 결과를 슬롯별 카드 id로 돌려준다. (null = 빈 슬롯)
     * @param slotCount 현재 해금된 슬롯 수 (9~12). 12를 넘기면 12로 잘린다.
     */
    fun autoEquip(owned: List<TcgCard>, slotCount: Int = DeckRules.BASE_DECK_SIZE): List<Int?> {
        val available = slotCount.coerceIn(0, DeckRules.MAX_DECK_SIZE)
        if (available <= 0) return emptyList()
        val picked = autoEquipCards(owned, available)
        if (picked.isEmpty()) return List(available) { null }

        val frontCount = FRONT_SLOTS.coerceAtMost(available)
        val front = picked.sortedWith(FRONT_ORDER).take(frontCount)
        val frontIds = front.map { it.id }.toSet()
        val back = picked.filterNot { it.id in frontIds }.sortedWith(BACK_ORDER)

        val slots = MutableList<Int?>(available) { null }
        front.forEachIndexed { index, card -> slots[index] = card.id }
        back.forEachIndexed { index, card ->
            val slot = frontCount + index
            if (slot < available) slots[slot] = card.id
        }
        return slots
    }

    /** 자동 편성 대상 카드 — 보유 카드 중 전투력 상위 N장 (최대 12) */
    fun autoEquipCards(owned: List<TcgCard>, slotCount: Int = DeckRules.BASE_DECK_SIZE): List<TcgCard> =
        owned.distinctBy { it.id }
            .sortedWith(POWER_ORDER)
            .take(slotCount.coerceIn(0, DeckRules.MAX_DECK_SIZE))

    /** 비어 있는 첫 슬롯 (전열 → 후열 순, 해금된 범위 안에서만). 자리가 없으면 null */
    fun firstEmptySlot(occupiedSlots: Collection<Int>, slotCount: Int = DeckRules.BASE_DECK_SIZE): Int? =
        (0 until slotCount.coerceIn(0, DeckRules.MAX_DECK_SIZE)).firstOrNull { it !in occupiedSlots }

    /** 슬롯이 전열인지 */
    fun isFrontSlot(slotIndex: Int): Boolean = DeckRules.isFrontSlot(slotIndex)

    /** 슬롯 이름 (전1 ~ 전3 · 후1 ~ 후9) */
    fun slotLabel(slotIndex: Int): String = DeckRules.slotLabel(slotIndex)
}
