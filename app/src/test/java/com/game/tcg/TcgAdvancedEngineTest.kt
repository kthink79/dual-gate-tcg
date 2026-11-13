package com.game.tcg

import com.game.tcg.core.engine.BattleAiEngine
import com.game.tcg.core.engine.DeckEngine
import com.game.tcg.core.engine.ManaEngine
import com.game.tcg.core.model.*
import com.game.tcg.ui.TcgViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TcgAdvancedEngineTest {

    private lateinit var deckEngine: DeckEngine
    private lateinit var manaEngine: ManaEngine
    private lateinit var battleAiEngine: BattleAiEngine

    @Before
    fun setUp() {
        deckEngine = DeckEngine()
        manaEngine = ManaEngine()
        battleAiEngine = BattleAiEngine()
    }

    private fun createValid30CardDeck(): MutableList<TcgCard> {
        val list = mutableListOf<TcgCard>()
        // 10 distinct card IDs, 3 copies each = 30 cards total
        for (id in 1..10) {
            val element = when (id % 5) {
                0 -> Element.FIRE
                1 -> Element.WIND
                2 -> Element.ICE
                3 -> Element.LIGHT
                else -> Element.DARK
            }
            val cost = (id % 7) + 1
            repeat(3) { copyIndex ->
                list.add(
                    TcgCard(
                        id = id,
                        name = "Card-$id#$copyIndex",
                        element = element,
                        baseCost = cost,
                        atk = 1000 * cost,
                        def = 800 * cost,
                        skillText = "Skill-$id"
                    )
                )
            }
        }
        return list
    }

    // =========================================================================
    // 1. DeckEngine Tests: Deck Building Constraints & Cost Curve
    // =========================================================================

    @Test
    fun testDeckValidationSuccessWithValid30Cards() {
        val deck = createValid30CardDeck()
        val result = deckEngine.validateDeck(deck)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(30, deck.size)
        // Check cost curve map is properly populated
        assertTrue(result.costCurve.isNotEmpty())
        assertTrue(result.averageCost > 0.0)
    }

    @Test
    fun testDeckValidationFailsWhenSizeNot30() {
        val undersizedDeck = createValid30CardDeck().take(29)
        val underResult = deckEngine.validateDeck(undersizedDeck)
        assertFalse(underResult.isValid)
        assertTrue(underResult.errors.any { it.contains("정확히 30장") })

        val oversizedDeck = createValid30CardDeck().toMutableList().apply {
            add(TcgCard(99, "ExtraCard", Element.FIRE, 1, 100, 100, skillText = ""))
        }
        val overResult = deckEngine.validateDeck(oversizedDeck)
        assertFalse(overResult.isValid)
        assertTrue(overResult.errors.any { it.contains("정확히 30장") })
    }

    @Test
    fun testDeckValidationFailsWhenCardExceedsMax3Copies() {
        val deck = createValid30CardDeck().toMutableList()
        // Replace 1 card of id=10 with card of id=1 -> id=1 now has 4 copies, total size remains 30
        val indexOfTen = deck.indexOfFirst { it.id == 10 }
        deck[indexOfTen] = deck.first { it.id == 1 }.copy()

        assertEquals(30, deck.size)
        val result = deckEngine.validateDeck(deck)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("매수가 초과") })
    }

    @Test
    fun testDeckCostCurveAnalysis() {
        val deck = mutableListOf<TcgCard>()
        // 10 cards of cost 1, 10 cards of cost 2, 10 cards of cost 3 (with IDs within 3 copies limit)
        for (i in 1..10) {
            val cost = 1
            val id = (i % 4) + 1 // ids 1,2,3,4 max copies <= 3
            deck.add(TcgCard(i, "Card-$i", Element.FIRE, cost, 100, 100, skillText = ""))
        }
        for (i in 11..20) {
            val cost = 2
            deck.add(TcgCard(i, "Card-$i", Element.WIND, cost, 200, 200, skillText = ""))
        }
        for (i in 21..30) {
            val cost = 3
            deck.add(TcgCard(i, "Card-$i", Element.ICE, cost, 300, 300, skillText = ""))
        }

        val result = deckEngine.validateDeck(deck)
        assertTrue(result.isValid)
        assertEquals(10, result.costCurve[1])
        assertEquals(10, result.costCurve[2])
        assertEquals(10, result.costCurve[3])
        assertEquals(2.0, result.averageCost, 0.01)
    }

    // =========================================================================
    // 2. DeckEngine Tests: Opening Hand, Mulligan, Turn Draw & Fatigue
    // =========================================================================

    @Test
    fun testInitialHandDrawFiveCards() {
        val deck = createValid30CardDeck()
        val hand = deckEngine.drawInitialHand(deck)

        assertEquals(5, hand.size)
        assertEquals(25, deck.size)
    }

    @Test
    fun testSingleUseMulliganMechanism() {
        val deck = createValid30CardDeck()
        val initialHand = deckEngine.drawInitialHand(deck).toMutableList()

        assertEquals(5, initialHand.size)
        val cardsToReplace = listOf(initialHand[0], initialHand[1])

        val newHand = deckEngine.performMulligan(deck, initialHand, cardsToReplace)

        assertEquals(5, newHand.size)
        // Cards to replace were put back, so deck size remains 25
        assertEquals(25, deck.size)
        // Original replaced cards should no longer be in the new hand in those positions
        assertFalse(newHand.take(3).contains(cardsToReplace[0]) && newHand.take(3).contains(cardsToReplace[1]))
    }

    @Test
    fun testTurnDrawNormalAndMaxHandSizeCap() {
        val deck = createValid30CardDeck()
        val hand = deckEngine.drawInitialHand(deck).toMutableList() // 5 cards

        // Draw 5 more cards: hand reaches 10 cards
        repeat(5) {
            val drawResult = deckEngine.drawTurnCard(deck, hand, fatigueCount = 0)
            assertNotNull(drawResult.drawnCard)
            assertFalse(drawResult.isBurned)
            assertEquals(0, drawResult.fatigueDamage)
        }
        assertEquals(10, hand.size)
        assertEquals(20, deck.size)

        // 11th card draw attempt: Hand is capped at 10, drawn card is burned
        val burnedResult = deckEngine.drawTurnCard(deck, hand, fatigueCount = 0)
        assertNotNull(burnedResult.drawnCard)
        assertTrue(burnedResult.isBurned)
        assertEquals(10, hand.size) // Hand size remains 10
        assertEquals(19, deck.size)
    }

    @Test
    fun testDeckDepletionAndScalingFatigueDamage() {
        val deck = mutableListOf<TcgCard>() // Empty deck
        val hand = mutableListOf<TcgCard>()
        var fatigueCount = 0

        // 1st draw on empty deck: 1 fatigue damage
        val draw1 = deckEngine.drawTurnCard(deck, hand, fatigueCount)
        assertNull(draw1.drawnCard)
        assertFalse(draw1.isBurned)
        assertEquals(1, draw1.fatigueDamage)
        assertEquals(1, draw1.newFatigueCount)
        fatigueCount = draw1.newFatigueCount

        // 2nd draw: 2 fatigue damage
        val draw2 = deckEngine.drawTurnCard(deck, hand, fatigueCount)
        assertEquals(2, draw2.fatigueDamage)
        assertEquals(2, draw2.newFatigueCount)
        fatigueCount = draw2.newFatigueCount

        // 3rd draw: 3 fatigue damage
        val draw3 = deckEngine.drawTurnCard(deck, hand, fatigueCount)
        assertEquals(3, draw3.fatigueDamage)
        assertEquals(3, draw3.newFatigueCount)
    }

    // =========================================================================
    // 3. ManaEngine Tests: Progressive Scaling, Replenishment & Cost Deduction
    // =========================================================================

    @Test
    fun testProgressiveManaScalingUpToTen() {
        assertEquals(1, manaEngine.calculateMaxManaForTurn(1))
        assertEquals(2, manaEngine.calculateMaxManaForTurn(2))
        assertEquals(5, manaEngine.calculateMaxManaForTurn(5))
        assertEquals(9, manaEngine.calculateMaxManaForTurn(9))
        assertEquals(10, manaEngine.calculateMaxManaForTurn(10))
        // Capped at 10 mana
        assertEquals(10, manaEngine.calculateMaxManaForTurn(11))
        assertEquals(10, manaEngine.calculateMaxManaForTurn(25))
    }

    @Test
    fun testFullManaReplenishmentAtTurnStart() {
        val turn1Mana = manaEngine.replenishTurnMana(1)
        assertEquals(1, turn1Mana.currentMana)
        assertEquals(1, turn1Mana.maxMana)

        val turn7Mana = manaEngine.replenishTurnMana(7)
        assertEquals(7, turn7Mana.currentMana)
        assertEquals(7, turn7Mana.maxMana)

        val turn10Mana = manaEngine.replenishTurnMana(10)
        assertEquals(10, turn10Mana.currentMana)
        assertEquals(10, turn10Mana.maxMana)
    }

    @Test
    fun testManaCostDeductionAndStrictBoundaries() {
        val initialMana = ManaState(currentMana = 5, maxMana = 5)

        // Can afford valid cost
        assertTrue(manaEngine.canAfford(initialMana, 3))
        val after3 = manaEngine.deductCost(initialMana, 3)
        assertEquals(2, after3.currentMana)
        assertEquals(5, after3.maxMana)

        // Exact afford (currentMana == cost)
        assertTrue(manaEngine.canAfford(after3, 2))
        val after2 = manaEngine.deductCost(after3, 2)
        assertEquals(0, after2.currentMana)

        // Cannot afford cost exceeding current mana
        assertFalse(manaEngine.canAfford(after2, 1))

        // Deduction beyond current mana throws IllegalArgumentException
        try {
            manaEngine.deductCost(after2, 1)
            fail("Expected IllegalArgumentException when deducting more mana than available")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("마나가 부족합니다") == true)
        }

        // Negative cost validation
        assertFalse(manaEngine.canAfford(initialMana, -1))
        try {
            manaEngine.deductCost(initialMana, -1)
            fail("Expected IllegalArgumentException when cost is negative")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("음수일 수 없습니다") == true)
        }
    }

    // =========================================================================
    // 4. BattleAiEngine Tests: 3 Frontline / 2 Backline Slots & AI Targeting
    // =========================================================================

    @Test
    fun testFrontlineProtectionRule() {
        // Setup 3 Frontline (indices 0, 1, 2) and 2 Backline (indices 3, 4)
        val slots = FieldSlotFactory.createStandardSlots()
        val frontUnit = TcgCard(1, "FrontGuardian", Element.FIRE, 3, 2000, 2000, skillText = "")
        val backUnit = TcgCard(2, "BackSniper", Element.WIND, 5, 4000, 1000, skillText = "")

        slots[0].placedCard = frontUnit
        slots[3].placedCard = backUnit

        // When frontline unit survives, backline unit CANNOT be targeted
        val validTargetsWithFrontline = battleAiEngine.getValidTargetSlots(slots)
        assertEquals(1, validTargetsWithFrontline.size)
        assertEquals(0, validTargetsWithFrontline[0].slotIndex)
        assertTrue(validTargetsWithFrontline[0].isFrontRow)

        // Kill frontline unit
        slots[0].placedCard = null

        // Now that all frontline units are gone, backline unit CAN be targeted
        val validTargetsAfterFrontlineWiped = battleAiEngine.getValidTargetSlots(slots)
        assertEquals(1, validTargetsAfterFrontlineWiped.size)
        assertEquals(3, validTargetsAfterFrontlineWiped[0].slotIndex)
        assertFalse(validTargetsAfterFrontlineWiped[0].isFrontRow)
    }

    @Test
    fun testAiElementalCounterTargetingPriority() {
        val attackerFire = TcgCard(10, "FireMage", Element.FIRE, 4, 3000, 2000, skillText = "")

        val slots = FieldSlotFactory.createStandardSlots()
        // Slot 0: ICE unit (FIRE -> ICE is 0.8x disadvantage)
        slots[0].placedCard = TcgCard(1, "IceGolem", Element.ICE, 4, 2500, 2500, skillText = "")
        // Slot 1: WIND unit (FIRE -> WIND is 1.3x advantage!)
        slots[1].placedCard = TcgCard(2, "WindRanger", Element.WIND, 4, 2500, 2500, skillText = "")
        // Slot 2: FIRE unit (FIRE -> FIRE is 1.0x neutral)
        slots[2].placedCard = TcgCard(3, "FireWarrior", Element.FIRE, 4, 2500, 2500, skillText = "")

        val optimalTarget = battleAiEngine.selectOptimalTarget(attackerFire, slots)
        assertNotNull(optimalTarget)
        // AI must prioritize Slot 1 (WIND unit) due to 1.30x advantage
        assertEquals(1, optimalTarget!!.slotIndex)
        assertEquals(Element.WIND, optimalTarget.placedCard!!.element)
    }

    @Test
    fun testAiLightDarkMutualCounterTargetingPriority() {
        val attackerLight = TcgCard(20, "Angel", Element.LIGHT, 6, 4000, 3000, skillText = "")

        val slots = FieldSlotFactory.createStandardSlots()
        // Slot 0: FIRE unit (LIGHT -> FIRE is 1.0x)
        slots[0].placedCard = TcgCard(1, "FireWarrior", Element.FIRE, 4, 2500, 2500, skillText = "")
        // Slot 1: DARK unit (LIGHT -> DARK is 1.5x mutual counter!)
        slots[1].placedCard = TcgCard(2, "DemonLord", Element.DARK, 6, 3500, 3500, skillText = "")

        val optimalTarget = battleAiEngine.selectOptimalTarget(attackerLight, slots)
        assertNotNull(optimalTarget)
        // AI must select DARK unit with highest priority (1.5x)
        assertEquals(1, optimalTarget!!.slotIndex)
        assertEquals(Element.DARK, optimalTarget.placedCard!!.element)
    }

    // =========================================================================
    // 5. TcgViewModel Full Turn Cycle & Interaction Integration Tests
    // =========================================================================

    @Test
    fun testViewModelTurnCycleAndPlayCardFlow() {
        val viewModel = TcgViewModel(
            deckEngine = deckEngine,
            manaEngine = manaEngine,
            battleAiEngine = battleAiEngine
        )

        // 1. Start game: Turn 1, Mana 1/1, Hand 5 cards
        viewModel.startNewGame(createValid30CardDeck())
        var state = viewModel.uiState.value
        assertEquals(1, state.currentTurn)
        assertEquals(1, state.manaState.currentMana)
        assertEquals(1, state.manaState.maxMana)
        assertEquals(5, state.playerHand.size)
        assertEquals(25, state.playerDeck.size)
        assertFalse(state.mulliganUsed)

        // 2. Perform Mulligan on first 2 cards
        viewModel.executeMulligan(listOf(0, 1))
        state = viewModel.uiState.value
        assertTrue(state.mulliganUsed)
        assertEquals(5, state.playerHand.size)

        // Second mulligan attempt should be rejected
        viewModel.executeMulligan(listOf(0))
        assertTrue(viewModel.uiState.value.statusMessage.contains("1회만 가능"))

        // 3. Advance turn: Turn 2 -> Mana 2/2, Hand 6 cards
        viewModel.executeTurnDraw()
        state = viewModel.uiState.value
        assertEquals(2, state.currentTurn)
        assertEquals(2, state.manaState.currentMana)
        assertEquals(2, state.manaState.maxMana)
        assertEquals(6, state.playerHand.size)

        // 4. Play a card from hand to frontline slot 0
        // Find a card in hand with cost <= 2
        val playableIndex = state.playerHand.indexOfFirst { it.baseCost <= 2 }
        if (playableIndex != -1) {
            val cardCost = state.playerHand[playableIndex].baseCost
            val playSuccess = viewModel.executePlayCard(playableIndex, slotIndex = 0)
            assertTrue(playSuccess)
            state = viewModel.uiState.value
            assertEquals(2 - cardCost, state.manaState.currentMana)
            assertEquals(5, state.playerHand.size)
            assertNotNull(state.playerFieldSlots[0].placedCard)
        }
    }
}
