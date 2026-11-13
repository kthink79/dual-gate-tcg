package com.game.tcg

import com.game.tcg.core.engine.IRoguelikeLifecycleEngine
import com.game.tcg.core.engine.ITacticalRetreatEngine
import com.game.tcg.core.engine.RoguelikeLifecycleEngine
import com.game.tcg.core.engine.TacticalRetreatEngine
import com.game.tcg.core.model.*
import com.game.tcg.ui.TcgViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * v0.3 로그라이크 영구 상실·기억 초기화 환생 & 전투 중 전략적 후퇴 단위 테스트
 */
class RoguelikeLifecycleAndRetreatTest {

    private fun createHero(
        id: Int = 3,
        name: String = "천상의 성기사 아우렐리아",
        element: Element = Element.LIGHT
    ): TcgCard = TcgCard(
        id = id,
        name = name,
        element = element,
        baseCost = 5,
        atk = 5000,
        def = 4000,
        skillText = "아군 보호 및 방벽 전개"
    )

    private fun createRetreatBattleState(): BattleState {
        val attacker = createHero()
        val healer = createHero(id = 4, name = "오로라의 무녀 히스이", element = Element.WIND)
        val slots = FieldSlotFactory.createStandardSlots().toMutableList()
        slots[0].placedCard = attacker
        slots[3].placedCard = healer

        return BattleState(
            turn = 4,
            mana = ManaState(currentMana = 5, maxMana = 5),
            playerSlots = slots,
            playerHp = mutableListOf(
                BattleCardHp(slotIndex = 0, cardId = attacker.id, currentHp = 3000, maxHp = 4000),
                BattleCardHp(slotIndex = 3, cardId = healer.id, currentHp = 2500, maxHp = 4000)
            ),
            retreatInfo = BattleRetreatInfo(turn = 4)
        )
    }

    // =========================================================================
    // 1. 사망 상실 · 기억/강화 초기화 · 환생 풀 이동
    // =========================================================================

    @Test
    fun testCardDeath_removesFromCollectionAndResetsGrowthIntoReincarnationPool() {
        val engine: IRoguelikeLifecycleEngine = RoguelikeLifecycleEngine(listOf(createHero()))
        engine.acquireCard(3)

        val owned = engine.collection.cards[3]!!
        owned.upgradeLevel = 7
        owned.memoryAffinity = 88
        owned.bondLevel = 92
        owned.starLevel = 4
        owned.equippedRelicCount = 3
        owned.atk = 9000
        owned.def = 7800

        val result = engine.onCardDied(3, causeOfDeath = "마왕 릴리스의 일격", diedAtTurn = 6)

        assertTrue(result.isSuccess)
        assertEquals(7, result.lostUpgradeLevel)
        assertEquals(88, result.lostMemoryAffinity)
        assertNotNull(result.reincarnation)

        // 보유 컬렉션에서 즉시 제거 (영구 상실)
        assertFalse(engine.collection.isOwned(3))
        assertEquals(0, engine.collection.size)

        // 환생 풀 등록 및 상실 수치 기록
        assertTrue(engine.collection.isAwaitingReincarnation(3))
        val seed = engine.collection.reincarnationPool[3]!!
        assertEquals(1, seed.reincarnationCount)
        assertEquals(6, seed.diedAtTurn)
        assertEquals(4, seed.lostStarLevel)
        assertEquals(3, seed.lostRelicCount)
        assertEquals(5000, seed.baseAtk) // 원본 스탯 시드는 보존
        assertTrue(result.message.contains("[영구 상실]"))
    }

    @Test
    fun testCardDeath_failsForUnownedCard() {
        val engine: IRoguelikeLifecycleEngine = RoguelikeLifecycleEngine(listOf(createHero()))
        val result = engine.onCardDied(99)

        assertFalse(result.isSuccess)
        assertNull(result.lostCard)
        assertTrue(result.message.contains("컬렉션에 보유 중이지 않은"))
    }

    // =========================================================================
    // 2. 중복 보유 불가 (1캐릭터 = 유일한 1장) 및 환생 후 재획득
    // =========================================================================

    @Test
    fun testAcquireCard_blocksDuplicateCharacter_singleCardPerCharacter() {
        val engine: IRoguelikeLifecycleEngine = RoguelikeLifecycleEngine(listOf(createHero()))

        val first = engine.acquireCard(3)
        assertEquals(AcquisitionStatus.ACQUIRED_NEW, first.status)
        assertTrue(first.isSuccess)
        assertEquals(1, engine.collection.size)

        val duplicate = engine.acquireCard(3)
        assertEquals(AcquisitionStatus.REJECTED_DUPLICATE, duplicate.status)
        assertFalse(duplicate.isSuccess)
        assertEquals(1, engine.collection.size)
        assertTrue(duplicate.message.contains("1캐릭터 = 1장"))
    }

    @Test
    fun testReacquireAfterDeath_restoresInitialStateButKeepsReincarnationCount() {
        val engine: IRoguelikeLifecycleEngine = RoguelikeLifecycleEngine(listOf(createHero()))
        engine.acquireCard(3)

        val hero = engine.collection.cards[3]!!
        hero.upgradeLevel = 5
        hero.memoryAffinity = 60
        hero.bondLevel = 100
        hero.starLevel = 3
        hero.equippedRelicCount = 2
        hero.atk = 12000
        hero.def = 9000

        engine.onCardDied(3)

        val reacquired = engine.acquireCard(3)
        assertEquals(AcquisitionStatus.REACQUIRED_FROM_REINCARNATION, reacquired.status)
        assertTrue(reacquired.isSuccess)

        val reborn = reacquired.card!!
        assertEquals(0, reborn.upgradeLevel)
        assertEquals(0, reborn.memoryAffinity)
        assertEquals(0, reborn.bondLevel)
        assertEquals(1, reborn.starLevel)
        assertEquals(0, reborn.equippedRelicCount)
        assertEquals(5000, reborn.atk) // 원본 공격력 복원
        assertEquals(4000, reborn.def)
        assertEquals(1, reborn.reincarnationCount) // 환생 횟수만 계승

        assertTrue(engine.collection.isOwned(3))
        assertFalse(engine.collection.isAwaitingReincarnation(3))
        assertTrue(reacquired.message.contains("환생 카드 재획득"))
    }

    @Test
    fun testRepeatedDeath_accumulatesReincarnationCount() {
        val engine: IRoguelikeLifecycleEngine = RoguelikeLifecycleEngine(listOf(createHero()))
        engine.acquireCard(3)

        engine.onCardDied(3)
        val firstRebirth = engine.acquireCard(3).card!!
        assertEquals(1, firstRebirth.reincarnationCount)

        engine.onCardDied(3)
        val secondRebirth = engine.acquireCard(3).card!!
        assertEquals(2, secondRebirth.reincarnationCount)
        assertEquals(2, secondRebirth.rebirthMarkTier)
    }

    // =========================================================================
    // 3. 개별 전술 후퇴 (마나 소모 · 턴당 1회 · 빈사 생존 보너스)
    // =========================================================================

    @Test
    fun testTacticalRetreat_spendsManaMovesCardAndLimitsOncePerTurn() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val battle = createRetreatBattleState()

        val first = engine.retreatCard(battle, 0)
        assertTrue(first.isSuccess)
        assertEquals(1, first.manaSpent)
        assertEquals(4, first.remainingMana)
        assertEquals(4, battle.mana.currentMana)
        assertEquals(RetreatZone.HAND, first.toZone)
        assertNull(battle.playerSlots[0].placedCard)
        assertEquals(1, battle.hand.size)
        assertEquals(1, battle.retreatInfo.totalRetreatCount)
        assertTrue(battle.retreatInfo.retreatUsedThisTurn)
        assertFalse(first.nearDeathSurvivalBonus) // HP 75% — 빈사 아님

        // 턴당 1회 제한
        val second = engine.retreatCard(battle, 3)
        assertFalse(second.isSuccess)
        assertTrue(second.failureReason!!.contains("턴당 1회"))
        assertNotNull(battle.playerSlots[3].placedCard)
        assertEquals(4, battle.mana.currentMana) // 실패 시 마나 미소모

        // 새 턴 시작 시 제한 해제
        engine.beginTurn(battle, 5, ManaState(currentMana = 6, maxMana = 6))
        assertFalse(battle.retreatInfo.retreatUsedThisTurn)
        assertTrue(engine.retreatCard(battle, 3).isSuccess)
    }

    @Test
    fun testNearDeathRetreat_grantsSurvivalMemoryBonus() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val hero = createHero()
        val slots = FieldSlotFactory.createStandardSlots().toMutableList()
        slots[1].placedCard = hero

        val battle = BattleState(
            turn = 3,
            mana = ManaState(currentMana = 4, maxMana = 4),
            playerSlots = slots,
            playerHp = mutableListOf(BattleCardHp(slotIndex = 1, cardId = hero.id, currentHp = 500, maxHp = 4000))
        )

        val result = engine.retreatCard(battle, 1)
        assertTrue(result.isSuccess)
        assertTrue(result.nearDeathSurvivalBonus)
        assertEquals(10, result.memoryAffinityGained)
        assertEquals(10, hero.memoryAffinity)
        assertEquals(1, battle.retreatInfo.nearDeathSurvivalCount)
        assertTrue(result.message.contains("생사 고비"))
    }

    @Test
    fun testRetreatFailsWhenCardAlreadyDead() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val hero = createHero()
        val slots = FieldSlotFactory.createStandardSlots().toMutableList()
        slots[2].placedCard = hero

        val battle = BattleState(
            turn = 2,
            mana = ManaState(currentMana = 3, maxMana = 3),
            playerSlots = slots,
            playerHp = mutableListOf(BattleCardHp(slotIndex = 2, cardId = hero.id, currentHp = 0, maxHp = 4000))
        )

        val result = engine.retreatCard(battle, 2)
        assertFalse(result.isSuccess)
        assertTrue(result.failureReason!!.contains("HP 0"))
        assertNotNull(battle.playerSlots[2].placedCard) // 상실 처리 대상으로 남는다
    }

    @Test
    fun testRetreatMovesCardToReserveWhenHandIsFull() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val battle = createRetreatBattleState()
        repeat(10) { index ->
            battle.hand.add(createHero(id = 100 + index, name = "예비 카드 $index"))
        }

        val result = engine.retreatCard(battle, 0)
        assertTrue(result.isSuccess)
        assertEquals(RetreatZone.RESERVE, result.toZone)
        assertEquals(10, battle.hand.size)
        assertEquals(1, battle.reserve.size)
    }

    @Test
    fun testRetreatBlockedByCharmStacks() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val battle = createRetreatBattleState()

        val blocked = engine.retreatCard(battle, 0, temptationStacks = 3)
        assertFalse(blocked.isSuccess)
        assertTrue(blocked.failureReason!!.contains("홀림"))
        assertNotNull(battle.playerSlots[0].placedCard)
        assertEquals(5, battle.mana.currentMana) // 마나 미소모
        assertFalse(battle.retreatInfo.retreatUsedThisTurn)
    }

    @Test
    fun testDemonLordBattleRetreatCostsTwoMana() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val battle = createRetreatBattleState()

        val result = engine.retreatCard(battle, 0, isDemonLordBattle = true)
        assertTrue(result.isSuccess)
        assertEquals(2, result.manaSpent)
        assertEquals(3, battle.mana.currentMana)
    }

    // =========================================================================
    // 4. 파티 긴급 철수 (전리품 손실 · 파티 전원 생존 귀환)
    // =========================================================================

    @Test
    fun testEmergencyPartyRetreat_forfeitsLootAndRescuesAllSurvivors() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val battle = createRetreatBattleState()
        battle.lootGold = 1200
        battle.lootGems = 30
        battle.lootItems.add("심연 파편")
        battle.hand.add(createHero(id = 9, name = "대기 카드"))

        val result = engine.emergencyPartyRetreat(battle)

        assertTrue(result.isSuccess)
        assertEquals(3, result.rescuedCardCount) // 전장 2장 + 손패 1장
        assertEquals(1200, result.lostLootGold)
        assertEquals(30, result.lostLootGems)
        assertEquals(listOf("심연 파편"), result.abandonedLootNames)

        // 전리품 포기 및 전장 정리
        assertEquals(0, battle.lootGold)
        assertEquals(0, battle.lootGems)
        assertTrue(battle.lootItems.isEmpty())
        assertTrue(battle.playerSlots.all { it.placedCard == null })
        assertEquals(3, battle.hand.size)
        assertTrue(battle.retreatInfo.emergencyRetreatUsed)
        assertTrue(result.message.contains("[파티 긴급 철수]"))
    }

    @Test
    fun testEmergencyPartyRetreat_limitedOncePerBattle() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val battle = createRetreatBattleState()

        assertTrue(engine.emergencyPartyRetreat(battle).isSuccess)

        val second = engine.emergencyPartyRetreat(battle)
        assertFalse(second.isSuccess)
        assertTrue(second.failureReason!!.contains("이미 긴급 철수"))
    }

    @Test
    fun testEmergencyPartyRetreat_failsWhenPartyWiped() {
        val engine: ITacticalRetreatEngine = TacticalRetreatEngine()
        val battle = BattleState(
            turn = 8,
            mana = ManaState(currentMana = 5, maxMana = 5),
            playerSlots = FieldSlotFactory.createStandardSlots().toMutableList()
        )

        val result = engine.emergencyPartyRetreat(battle)
        assertFalse(result.isSuccess)
        assertTrue(result.failureReason!!.contains("전멸"))
    }

    // =========================================================================
    // 5. ViewModel 바인딩 (환생 대기 목록 · 후퇴 사용 여부 · 사망 알림)
    // =========================================================================

    @Test
    fun testViewModelCardDeathAndReacquireIntegration() {
        val viewModel = TcgViewModel()
        viewModel.startNewGame()

        val handCard = viewModel.uiState.value.playerHand.first()
        assertTrue(viewModel.uiState.value.ownedCards.any { it.id == handCard.id })

        val death = viewModel.handleCardDeath(handCard.id)
        assertTrue(death.isSuccess)
        assertEquals(death, viewModel.uiState.value.lastCardDeathResult)
        assertTrue(viewModel.uiState.value.playerHand.none { it.id == handCard.id })
        assertTrue(viewModel.uiState.value.playerDeck.none { it.id == handCard.id })
        assertTrue(viewModel.uiState.value.ownedCards.none { it.id == handCard.id })
        assertTrue(viewModel.uiState.value.reincarnationPool.any { it.templateId == handCard.id })
        assertTrue(viewModel.uiState.value.statusMessage.contains("[영구 상실]"))

        val reacquired = viewModel.reacquireCard(handCard.id)
        assertTrue(reacquired.isSuccess)
        assertEquals(AcquisitionStatus.REACQUIRED_FROM_REINCARNATION, reacquired.status)
        assertEquals(0, reacquired.card!!.memoryAffinity)
        assertTrue(viewModel.uiState.value.reincarnationPool.none { it.templateId == handCard.id })
        assertTrue(viewModel.uiState.value.ownedCards.any { it.id == handCard.id })
    }

    @Test
    fun testViewModelRetreatCycleBindsTurnUsageFlag() {
        val viewModel = TcgViewModel()
        viewModel.startNewGame()

        // 턴 3까지 진행해 마나 3 확보 후 필드에 카드 소환 (소환 후에도 후퇴 마나 1이 남도록)
        viewModel.executeTurnDraw()
        viewModel.executeTurnDraw()
        val state = viewModel.uiState.value
        val playableIndex = state.playerHand.indexOfFirst { it.baseCost <= 2 }
        assertTrue(playableIndex >= 0)
        assertTrue(viewModel.executePlayCard(playableIndex, slotIndex = 0))
        assertTrue(viewModel.uiState.value.manaState.currentMana >= 1) // 후퇴 마나 1 확보
        assertFalse(viewModel.uiState.value.retreatUsedThisTurn)

        val retreat = viewModel.retreatCard(slotIndex = 0)
        assertTrue(retreat.isSuccess)
        assertTrue(viewModel.uiState.value.retreatUsedThisTurn)
        assertTrue(viewModel.uiState.value.retreatInfo.retreatUsedThisTurn)
        assertNull(viewModel.uiState.value.playerFieldSlots[0].placedCard)
        assertEquals(retreat, viewModel.uiState.value.lastRetreatResult)

        // 같은 턴 두 번째 후퇴는 거부
        val blocked = viewModel.retreatCard(slotIndex = 0)
        assertFalse(blocked.isSuccess)

        // 다음 턴 시작 시 후퇴 사용 여부 초기화
        viewModel.executeTurnDraw()
        assertFalse(viewModel.uiState.value.retreatUsedThisTurn)
    }

    @Test
    fun testViewModelEmergencyRetreatForfeitsLootButKeepsCards() {
        val viewModel = TcgViewModel()
        viewModel.startNewGame()
        viewModel.executeTurnDraw()

        val state = viewModel.uiState.value
        val playableIndex = state.playerHand.indexOfFirst { it.baseCost <= state.manaState.currentMana }
        assertTrue(playableIndex >= 0)
        assertTrue(viewModel.executePlayCard(playableIndex, slotIndex = 1))
        viewModel.setBattleLoot(gold = 900, gems = 12, items = listOf("심연 파편", "고대 유물"))

        val result = viewModel.emergencyRetreat()
        assertTrue(result.isSuccess)
        assertEquals(900, result.lostLootGold)
        assertEquals(12, result.lostLootGems)
        assertEquals(2, result.abandonedLootNames.size)

        val after = viewModel.uiState.value
        assertEquals(0, after.battleLootGold)
        assertEquals(0, after.battleLootGems)
        assertTrue(after.battleLootItems.isEmpty())
        assertTrue(after.playerFieldSlots.all { it.placedCard == null })
        assertEquals(6, after.lastEmergencyRetreatResult!!.rescuedCardCount)
        assertTrue(after.statusMessage.contains("[파티 긴급 철수]"))
    }

}
