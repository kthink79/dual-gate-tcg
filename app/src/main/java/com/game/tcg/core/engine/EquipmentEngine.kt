package com.game.tcg.core.engine

import com.game.tcg.core.model.EquipmentCatalog
import com.game.tcg.core.model.EquipmentItem
import com.game.tcg.core.model.EquipmentSlot
import com.game.tcg.core.model.TcgCard

/** 장착/해제 결과 */
data class EquipResult(
    val isSuccess: Boolean,
    val cardId: Int = 0,
    val cardName: String = "",
    val item: EquipmentItem? = null,
    val atkDelta: Int = 0,
    val defDelta: Int = 0,
    val failureReason: String? = null,
    val message: String = ""
)

/**
 * 아이템 장착 엔진 — 2026-11-16 사용자 요청 "아이템 장착" 서브 플로우.
 *
 * 무기 · 방어구 · 장신구 **3슬롯**에 캐릭터별 최대 3개를 장착한다.
 * 장착하는 순간 카드의 ATK/DEF가 오르고(해제하면 정확히 되돌아간다), HP 보너스는
 * 전장 최대 HP 계산에 더해진다.
 *
 * 수치를 카드에 직접 가감하는 이유: 전투 데미지·덱 전투력·도감 표시가 모두 `TcgCard.atk/def`를
 * 읽으므로, 한 곳(장착)에서만 바꾸면 화면 전체가 자동으로 같은 값을 본다.
 * 대신 [EquipmentItem.equippedTo] 로 중복 장착을 막아 두 번 더해지는 일을 방지한다.
 */
object EquipmentEngine {

    /** 캐릭터 1명이 착용할 수 있는 최대 장비 수 */
    const val MAX_PER_CARD: Int = 3

    /** 이 카드가 해당 슬롯에 이미 착용 중인 장비 */
    fun equippedInSlot(cardId: Int, slot: EquipmentSlot, inventory: Collection<EquipmentItem>): EquipmentItem? =
        inventory.firstOrNull { it.equippedTo == cardId && it.slot == slot }

    /** 카드에 착용된 장비 목록 (슠롯 순) */
    fun equippedFor(cardId: Int, inventory: Collection<EquipmentItem>): List<EquipmentItem> =
        inventory.filter { it.equippedTo == cardId }.sortedBy { it.slot.ordinal }

    /** 카드에 착용된 장비의 HP 보너스 합계 */
    fun hpBonus(cardId: Int, inventory: Collection<EquipmentItem>): Int =
        inventory.filter { it.equippedTo == cardId }.sumOf { it.hp }

    /** 장착 가능 여부 — 불가하면 이유 문구 */
    fun blocker(cardId: Int, item: EquipmentItem, inventory: Collection<EquipmentItem>): String? {
        if (item.equippedTo != null) {
            return if (item.equippedTo == cardId) "이미 ${item.name}이(가) 장착되어 있습니다." else
                "${item.name}은(는) 다른 캐릭터가 착용 중입니다."
        }
        val worn = equippedFor(cardId, inventory)
        if (worn.size >= MAX_PER_CARD) return "장비 슬롯 3칸이 모두 찼습니다. 먼저 하나를 해제하세요."
        val slotItem = worn.firstOrNull { it.slot == item.slot }
        if (slotItem != null) return "${item.slot.displayName} 슬롯에는 이미 '${slotItem.name}'이(가) 있습니다."
        return null
    }

    /** 장착 — 카드 스탯에 보너스를 더한다 */
    fun equip(card: TcgCard, item: EquipmentItem, inventory: Collection<EquipmentItem>): EquipResult {
        val blocked = blocker(card.id, item, inventory)
        if (blocked != null) {
            return EquipResult(isSuccess = false, cardId = card.id, cardName = card.name, item = item, failureReason = blocked)
        }
        card.atk += item.atk
        card.def += item.def
        card.equippedRelicCount = (card.equippedRelicCount + 1).coerceAtMost(MAX_PER_CARD)
        item.equippedTo = card.id
        return EquipResult(
            isSuccess = true,
            cardId = card.id,
            cardName = card.name,
            item = item,
            atkDelta = item.atk,
            defDelta = item.def,
            message = "[장비] ${card.name} ← ${item.name} (${item.slot.displayName}) ${item.statSummary}"
        )
    }

    /** 해제 — 장착 시 더했던 값을 정확히 되돌린다 */
    fun unequip(card: TcgCard, item: EquipmentItem): EquipResult {
        if (item.equippedTo != card.id) {
            return EquipResult(
                isSuccess = false,
                cardId = card.id,
                cardName = card.name,
                item = item,
                failureReason = "${item.name}은(는) ${card.name}에게 장착되어 있지 않습니다."
            )
        }
        card.atk = (card.atk - item.atk).coerceAtLeast(1)
        card.def = (card.def - item.def).coerceAtLeast(1)
        card.equippedRelicCount = (card.equippedRelicCount - 1).coerceAtLeast(0)
        item.equippedTo = null
        return EquipResult(
            isSuccess = true,
            cardId = card.id,
            cardName = card.name,
            item = item,
            atkDelta = -item.atk,
            defDelta = -item.def,
            message = "[해제] ${card.name} → ${item.name} 소지품으로"
        )
    }

    /** 새 장비 소지품 1개 (상점 구매·드롭) */
    fun create(itemId: String): EquipmentItem? = EquipmentCatalog.instantiate(itemId)
}
