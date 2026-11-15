package com.game.tcg

import com.game.tcg.core.engine.DeckAutoBuilder
import com.game.tcg.core.model.BattleLineup
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.PlayerProgress
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.game.tcg.core.model.CardCatalog
import org.junit.Test

/**
 * 덱 편성(DECK) 비주얼 UI 로직 — 2026-11-16 사용자 확정 규칙 검증
 *
 * 1) 기본 9장 편성 · 레벨당 +1 해금(Lv.5/10/15) · **최대 12장 상한**
 * 2) 자동 편성(전투력 상위 → 전열은 DEF 상위 3장) · 전체 해제 · 슬롯 장착/해제
 * 3) 잠긴 슬롯 배치 거부 · 가득 찬 덱 추가 장착 거부
 * 4) 전투 화면 적 진영 진형(보스 + 수하 9~12장)
 */
class DeckBuilderUiTest {

    /** 덱 편성 검증용 — 도감 170명을 모두 보유한 상태에서 시작한다 */
    private fun allCardViewModel() =
        TcgGameViewModel(initialOwnedCardIds = CardCatalog.ALL.map { it.id })


    // ── 1. 덱 크기 규칙 ─────────────────────────────────────────────

    @Test
    fun `기본 덱 크기는 9장이고 최대치는 12장이다`() {
        assertEquals(9, DeckRules.BASE_DECK_SIZE)
        assertEquals(9, DeckRules.MIN_DECK_SIZE)
        assertEquals(12, DeckRules.MAX_DECK_SIZE)
        assertEquals(9, DeckRules.deckSizeForLevel(1))
    }

    @Test
    fun `레벨 5에서 10장 레벨 10에서 11장 레벨 15에서 12장이 열린다`() {
        assertEquals(9, DeckRules.deckSizeForLevel(4))
        assertEquals(10, DeckRules.deckSizeForLevel(5))
        assertEquals(10, DeckRules.deckSizeForLevel(9))
        assertEquals(11, DeckRules.deckSizeForLevel(10))
        assertEquals(11, DeckRules.deckSizeForLevel(14))
        assertEquals(12, DeckRules.deckSizeForLevel(15))
        assertEquals(12, DeckRules.deckSizeForLevel(99))
    }

    @Test
    fun `최대 덱 크기는 어떤 레벨에서도 12장을 넘지 않는다`() {
        listOf(1, 5, 10, 15, 30, 200).forEach { level ->
            assertTrue(
                "Lv.$level 덱 크기가 12장을 넘었다",
                DeckRules.deckSizeForLevel(level) <= DeckRules.MAX_DECK_SIZE
            )
        }
        assertEquals(12, DeckAutoBuilder.autoEquip(List(12) { TcgGameViewModel.characterCard(it + 1) }, 99).size)
    }

    @Test
    fun `잠긴 슬롯은 해금 레벨을 알려준다`() {
        // 0~8번(9장)은 Lv.1, 9번은 Lv.5, 10번은 Lv.10, 11번은 Lv.15
        assertEquals(1, DeckRules.requiredLevelForSlot(8))
        assertEquals(5, DeckRules.requiredLevelForSlot(9))
        assertEquals(10, DeckRules.requiredLevelForSlot(10))
        assertEquals(15, DeckRules.requiredLevelForSlot(11))
        assertTrue(DeckRules.isSlotUnlocked(8, 1))
        assertTrue(!DeckRules.isSlotUnlocked(9, 4))
        assertTrue(DeckRules.isSlotUnlocked(9, 5))
        assertEquals(3, DeckRules.lockedSlotCount(1))
        assertEquals(0, DeckRules.lockedSlotCount(15))
    }

    @Test
    fun `경험치 100당 한 레벨 오르고 전투 승리는 120 경험치다`() {
        assertEquals(1, PlayerProgress.levelForExp(0))
        assertEquals(1, PlayerProgress.levelForExp(99))
        assertEquals(2, PlayerProgress.levelForExp(100))
        assertEquals(5, PlayerProgress.levelForExp(400))
        assertEquals(120, PlayerProgress.EXP_BATTLE_VICTORY)
        assertEquals(100, PlayerProgress.expToNextLevel(0))
    }

    // ── 2. 자동 편성 · 전체 해제 · 장착 ─────────────────────────────

    @Test
    fun `Lv1 자동 편성은 9칸만 채운다`() {
        val vm = allCardViewModel()
        assertEquals(1, vm.uiState.value.playerLevel)
        assertEquals(9, vm.uiState.value.unlockedDeckSize)

        vm.autoEquipParty()

        val state = vm.uiState.value
        assertEquals(9, state.party.size)
        assertTrue("슬롯 번호는 0~8 안이어야 한다", state.party.all { it.slotIndex in 0..8 })
        assertEquals("같은 캐릭터가 중복 편성되면 안 된다", 9, state.party.map { it.card.id }.distinct().size)
        assertEquals(3, state.party.count { it.isFrontRow })
    }

    @Test
    fun `자동 편성은 전열에 방어력 상위 3장을 세운다`() {
        val vm = allCardViewModel()
        vm.autoEquipParty()
        val state = vm.uiState.value
        val front = state.party.filter { it.isFrontRow }
        val back = state.party.filterNot { it.isFrontRow }
        assertEquals(3, front.size)
        assertEquals(6, back.size)
        assertTrue(
            "전열 최소 DEF가 후열 최대 DEF보다 낮으면 안 된다",
            front.minOf { it.card.def } >= back.maxOf { it.card.def }
        )
    }

    @Test
    fun `레벨 5가 되면 자동 편성이 10장을 채운다`() {
        val vm = allCardViewModel()
        vm.grantPlayerExp(400)
        assertEquals(5, vm.uiState.value.playerLevel)
        assertEquals(10, vm.uiState.value.unlockedDeckSize)

        vm.autoEquipParty()

        assertEquals(10, vm.uiState.value.party.size)
        assertTrue(vm.uiState.value.party.any { it.slotIndex == 9 })
    }

    @Test
    fun `레벨 15의 자동 편성은 12장에서 멈춘다`() {
        val vm = allCardViewModel()
        vm.grantPlayerExp(1400)
        assertEquals(15, vm.uiState.value.playerLevel)
        assertEquals(12, vm.uiState.value.unlockedDeckSize)

        vm.autoEquipParty()

        val state = vm.uiState.value
        assertEquals(12, state.party.size)
        assertEquals((0..11).toSet(), state.party.map { it.slotIndex }.toSet())
        assertEquals(12, state.party.map { it.card.id }.distinct().size)
    }

    @Test
    fun `전체 해제는 덱을 비우고 카드를 대기열로 돌린다`() {
        val vm = allCardViewModel()
        vm.autoEquipParty()
        val equippedBefore = vm.uiState.value.party.size
        assertEquals(9, equippedBefore)

        vm.unloadAllParty()

        val state = vm.uiState.value
        assertTrue("덱이 비어 있어야 한다", state.party.isEmpty())
        assertEquals(0, state.deckDisplayPower)
        assertTrue("해제 카드는 대기열에 남는다", state.battleReserve.size >= equippedBefore)
        assertTrue(state.statusMessage.contains("해제"))
    }

    @Test
    fun `카드를 탭하면 첫 빈 슬롯에 장착되고 다시 탭하면 해제된다`() {
        val vm = allCardViewModel()
        vm.unloadAllParty()
        val target = vm.uiState.value.ownedCards.first { it.id !in vm.uiState.value.equippedCardIds }

        vm.toggleEquipCard(target.id)
        assertEquals("첫 빈 슬롯(0)에 들어가야 한다", 0, vm.uiState.value.party.single().slotIndex)
        assertTrue(vm.uiState.value.equippedCardIds.contains(target.id))

        vm.toggleEquipCard(target.id)
        assertTrue("다시 탭하면 해제된다", vm.uiState.value.party.isEmpty())
    }

    @Test
    fun `빈 슬롯 장착은 앞자리부터 차례로 채운다`() {
        val vm = allCardViewModel()
        vm.unloadAllParty()
        val cards = vm.uiState.value.ownedCards

        vm.equipToFirstEmptySlot(cards[10].id)
        vm.equipToFirstEmptySlot(cards[20].id)

        val slots = vm.uiState.value.party.sortedBy { it.slotIndex }
        assertEquals(listOf(0, 1), slots.map { it.slotIndex })
        assertEquals("0·1번 모두 전열이다", 2, slots.count { it.isFrontRow })
    }

    @Test
    fun `가득 찬 덱에는 더 장착되지 않는다`() {
        val vm = allCardViewModel()
        vm.autoEquipParty()
        val full = vm.uiState.value
        assertEquals(9, full.party.size)
        val outsider = full.ownedCards.first { it.id !in full.equippedCardIds }

        vm.equipToFirstEmptySlot(outsider.id)

        val state = vm.uiState.value
        assertEquals("9칸이 가득이면 장착되지 않는다", 9, state.party.size)
        assertTrue(state.statusMessage.contains("모두 찼습니다"))
    }

    @Test
    fun `덱 총 전투력은 장착 카드의 표시 전투력 합계다`() {
        val vm = allCardViewModel()
        vm.autoEquipParty()
        val state = vm.uiState.value
        val expected = state.party.sumOf { (it.card.atk + it.card.def) / 100 }
        assertEquals(expected, state.deckDisplayPower)
        assertTrue("총 전투력은 0보다 커야 한다", state.deckDisplayPower > 0)
    }

    // ── 3. 잠긴 슬롯 ────────────────────────────────────────────────

    @Test
    fun `Lv1에서는 10번째 슬롯에 배치할 수 없다`() {
        val vm = allCardViewModel()
        val before = vm.uiState.value
        val card = before.ownedCards.first()

        vm.deployToParty(card.id, 9)

        val state = vm.uiState.value
        assertTrue("잠긴 슬롯에는 배치되지 않는다", state.party.none { it.slotIndex == 9 })
        assertTrue(state.statusMessage.contains("Lv.5"))
    }

    @Test
    fun `Lv5가 되면 10번째 슬롯 12번째 슬롯이 열린다`() {
        val vm = allCardViewModel()
        vm.grantPlayerExp(400)
        val card = vm.uiState.value.ownedCards.first { it.id !in vm.uiState.value.equippedCardIds }

        vm.deployToParty(card.id, 9)

        assertEquals(9, vm.uiState.value.party.first { it.card.id == card.id }.slotIndex)

        // Lv.5에서는 12번째 슬롯(11)은 아직 잠겨 있다
        val other = vm.uiState.value.ownedCards.first { it.id !in vm.uiState.value.equippedCardIds }
        vm.deployToParty(other.id, 11)
        assertTrue(vm.uiState.value.party.none { it.slotIndex == 11 })

        vm.grantPlayerExp(1400)
        val third = vm.uiState.value.ownedCards.first { it.id !in vm.uiState.value.equippedCardIds }
        vm.deployToParty(third.id, 11)
        assertEquals(11, vm.uiState.value.party.first { it.card.id == third.id }.slotIndex)
    }

    @Test
    fun `레벨 업 상태 메시지는 새로 열린 덱 장수를 알린다`() {
        val vm = allCardViewModel()
        vm.grantPlayerExp(400)
        assertTrue(
            "Lv.5 해금 안내가 있어야 한다: ${vm.uiState.value.statusMessage}",
            vm.uiState.value.statusMessage.contains("10장")
        )
    }

    // ── 4. 전투 화면 적 진영 ────────────────────────────────────────

    @Test
    fun `적 진영은 내 덱과 같은 9장으로 보스와 수하를 만든다`() {
        val units = BattleLineup.enemyLineup(
            enemyElement = Element.DARK,
            enemyName = "차원 틈새의 마수",
            size = 9,
            isDemonLordBattle = false
        )
        assertEquals(9, units.size)
        assertTrue(units.first().isBoss)
        assertEquals("차원 틈새의 마수", units.first().name)
        assertTrue("수하 8장이 따라붙는다", units.drop(1).none { it.isBoss })
    }

    @Test
    fun `마왕전은 릴리스 카드를 보스로 세운다`() {
        val units = BattleLineup.enemyLineup(
            enemyElement = Element.DARK,
            enemyName = "마왕 릴리스",
            size = 12,
            isDemonLordBattle = true
        )
        assertEquals(12, units.size)
        assertEquals(BattleLineup.DEMON_LORD_CARD_ID, units.first().cardId)
        assertEquals("마왕 릴리스", units.first().name)
        assertTrue(BattleLineup.totalDisplayPower(units) > 0)
    }

    @Test
    fun `적 진영 크기는 12장을 넘지 않는다`() {
        val units = BattleLineup.enemyLineup(
            enemyElement = Element.DARK,
            enemyName = "심연의 군주",
            size = 40
        )
        assertEquals(DeckRules.MAX_DECK_SIZE, units.size)
    }

    @Test
    fun `빈 덱이면 전투력은 0이고 빈 슬롯 탐색은 null이다`() {
        assertEquals(0, DeckAutoBuilder.displayTotalPower(emptyList()))
        assertNull(DeckAutoBuilder.firstEmptySlot((0..8).toList(), 9))
        assertNotNull(DeckAutoBuilder.firstEmptySlot(emptyList(), 9))
        assertEquals(0, DeckAutoBuilder.firstEmptySlot(emptyList(), 9))
    }
}
