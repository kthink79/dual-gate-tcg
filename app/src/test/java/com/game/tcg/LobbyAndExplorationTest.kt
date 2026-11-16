package com.game.tcg

import com.game.tcg.core.engine.ExplorationContext
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.ExplorationEngine
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationRewardType
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.SideRail
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 메인 로비(캐릭터 중심 · 좌우 서브메뉴) + 탐색 모드 단위 테스트
 */
class LobbyAndExplorationTest {

    // ──────────────────────────────────────────────────────────────
    // 메인 로비 — 화면 구성 · 대표 캐릭터 · 3단 비주얼
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `메인 로비는 시작 캐릭터 3명과 좌우 측면 서브메뉴로 시작한다`() {
        val state = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2)).uiState.value

        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
        // 시작 3장 중 DEF가 가장 높은 카드가 전열 1번·대표로 선다
        val expectedHero = listOf(3, 1, 2).maxBy { TcgGameViewModel.characterCard(it).def }
        assertEquals("시작 카드 중 가장 튼튼한 카드가 대표로 선다", expectedHero, state.lobbyHeroId)
        assertEquals(VisualState.BATTLE, state.lobbyVisual)

        assertEquals(
            listOf(LobbyDestination.SCENARIO, LobbyDestination.STAGE, LobbyDestination.BATTLE, LobbyDestination.EXPLORATION),
            LobbyDestination.LEFT_ITEMS
        )
        assertEquals(
            listOf(LobbyDestination.REAL_WORLD, LobbyDestination.SHOP, LobbyDestination.GROWTH,
                LobbyDestination.COLLECTION, LobbyDestination.DECK),
            LobbyDestination.RIGHT_ITEMS
        )
        assertEquals(SideRail.LEFT, LobbyDestination.SCENARIO.rail)
        assertEquals(SideRail.LEFT, LobbyDestination.STAGE.rail)
        assertEquals(SideRail.RIGHT, LobbyDestination.DECK.rail)
        assertEquals(SideRail.RIGHT, LobbyDestination.GROWTH.rail)
        assertEquals("이세계 접속", LobbyDestination.SCENARIO.label)
        assertEquals("스테이지", LobbyDestination.STAGE.label)
        assertEquals("탐색", LobbyDestination.EXPLORATION.label)
        assertEquals("카드팩", LobbyDestination.SHOP.label)
        assertEquals("성장", LobbyDestination.GROWTH.label)
        assertEquals("캐릭터 관리", LobbyDestination.COLLECTION.label)
        assertEquals("서약·덱", LobbyDestination.DECK.label)
    }

    @Test
    fun `좌우 전환은 보유 캐릭터를 순환한다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))

        assertEquals("시작 3장만 보유한다", 3, vm.uiState.value.lobbyRoster.size)
        val roster = vm.uiState.value.lobbyRoster
        val startIndex = roster.indexOf(vm.uiState.value.lobbyHeroId)

        vm.cycleLobbyHero(1)
        assertEquals(roster[(startIndex + 1) % roster.size], vm.uiState.value.lobbyHeroId)

        vm.cycleLobbyHero(1)
        assertEquals(roster[(startIndex + 2) % roster.size], vm.uiState.value.lobbyHeroId)

        vm.cycleLobbyHero(1)
        assertEquals("마지막 다음은 처음으로 돌아온다", roster[startIndex], vm.uiState.value.lobbyHeroId)

        vm.cycleLobbyHero(-1)
        assertEquals("역방향도 순환한다", roster[(startIndex + 2) % roster.size], vm.uiState.value.lobbyHeroId)
    }

    @Test
    fun `대표 캐릭터를 직접 선택할 수 있고 미보유 캐릭터는 거부된다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))

        vm.selectLobbyHero(1)
        assertEquals(1, vm.uiState.value.lobbyHeroId)
        assertEquals("태양의 대천사", vm.uiState.value.lobbyHeroProfile?.title)

        // 시작 3장만 보유이므로 미보유 카드(도감 184번)와 미등록 번호 모두 거부된다
        vm.selectLobbyHero(184)
        assertEquals("미보유 카드는 대표로 세울 수 없다", 1, vm.uiState.value.lobbyHeroId)
        assertTrue(vm.uiState.value.statusMessage.contains("계약하지 않은"))

        vm.selectLobbyHero(999)
        assertEquals("미등록 번호도 대표로 세울 수 없다", 1, vm.uiState.value.lobbyHeroId)
    }

    @Test
    fun `3단 비주얼은 친밀도 단계에 따라 잠기고 열린다`() {
        val vm = TcgGameViewModel()

        // 2026-11-16 개편: 새 캐릭터는 친밀도 0으로 시작 — 일상(30)·데이트(70) 모두 잠김
        assertEquals(0, vm.uiState.value.lobbyHeroBond)
        assertFalse(vm.uiState.value.isLobbyCasualLookUnlocked)
        assertFalse(vm.uiState.value.isLobbyDateLookUnlocked)

        vm.selectLobbyVisual(VisualState.CASUAL_CUTE)
        assertEquals("일상 사복은 30 미만에서 잠긴다", VisualState.BATTLE, vm.uiState.value.lobbyVisual)
        assertNotNull(vm.uiState.value.visualLockMessage)

        // 친밀도 70이면 일상·데이트 모두 열린다
        val unlocked = TcgGameViewModel(
            initialOwnedCardIds = listOf(3, 1, 2),
            initialBondLevel = AffinityRules.DATE_UNLOCK_BOND
        )
        unlocked.selectLobbyVisual(VisualState.CASUAL_CUTE)
        assertEquals(VisualState.CASUAL_CUTE, unlocked.uiState.value.lobbyVisual)
        unlocked.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertEquals("데이트 룩이 잠기지 않는다", VisualState.ROMANTIC_DATE, unlocked.uiState.value.lobbyVisual)

        // 다른 캐릭터로 바꿔도 데이트 룩이 유지된다
        unlocked.selectLobbyHero(2)
        assertEquals(VisualState.ROMANTIC_DATE, unlocked.uiState.value.lobbyVisual)

        // 현실 교감으로 친밀도가 더 올라간다
        val before = unlocked.uiState.value.lobbyHeroBond
        unlocked.interactRealWorld(RealWorldActivity.ROMANTIC_EVENING_DATE)
        assertTrue(unlocked.uiState.value.lobbyHeroBond >= before)
    }

    @Test
    fun `대표 캐릭터의 호감도와 기억 파편 상태가 로비에 반영된다`() {
        val vm = TcgGameViewModel()
        val state = vm.uiState.value

        assertNotNull(state.lobbyHero)
        assertEquals("새 캐릭터의 친밀도는 0에서 시작한다", 0, state.lobbyHeroBond)
        assertFalse(state.lobbyHero.isMemoryReset)
    }

    // ──────────────────────────────────────────────────────────────
    // 좌우 서브메뉴 내비게이션 · 메인 로비 복귀
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `좌측 서브메뉴는 이세계 세 화면을 연다`() {
        val vm = TcgGameViewModel()

        LobbyDestination.LEFT_ITEMS.forEach { destination ->
            vm.openScreen(destination)
            assertEquals(destination, vm.uiState.value.currentScreen)
            assertTrue(vm.uiState.value.statusMessage.startsWith("[${destination.label}]"))
        }
    }

    @Test
    fun `우측 서브메뉴는 현실 관리 세 화면을 연다`() {
        val vm = TcgGameViewModel()

        LobbyDestination.RIGHT_ITEMS.forEach { destination ->
            vm.openScreen(destination)
            assertEquals(destination, vm.uiState.value.currentScreen)
        }
    }

    @Test
    fun `어느 화면에서든 메인 로비로 돌아온다`() {
        val vm = TcgGameViewModel()

        LobbyDestination.values().forEach { destination ->
            vm.openScreen(destination)
            vm.backToLobby()
            assertEquals(LobbyDestination.LOBBY, vm.uiState.value.currentScreen)
        }
    }

    @Test
    fun `홀림 스택이 쌓이면 로비 경보가 시나리오 화면으로 안내한다`() {
        val vm = TcgGameViewModel()
        assertEquals("처음에는 다음 스테이지 안내만 있다", 1, vm.uiState.value.lobbyAlerts.size)
        assertEquals(LobbyDestination.STAGE, vm.uiState.value.lobbyAlerts.single().destination)

        vm.gazeAtDemonLord()

        val alerts = vm.uiState.value.lobbyAlerts
        assertTrue(alerts.isNotEmpty())
        val riftAlert = alerts.firstOrNull { it.label == "신뢰의 균열" }
        assertNotNull("홀림 2스택이면 신뢰의 균열 경보가 뜬다", riftAlert)
        assertEquals(LobbyDestination.SCENARIO, riftAlert!!.destination)
        assertTrue(riftAlert.isUrgent)
    }

    // ──────────────────────────────────────────────────────────────
    // 탐색 — 차원 틈새 · 기억 파편 · 랜덤 조우
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `탐색은 차원 균열 에너지를 소모하고 결과와 기록을 남긴다`() {
        // 판정을 고정해 조우 분기(상태 메시지가 [조우]로 바뀌는 경우)를 배제한다
        val vm = TcgGameViewModel(explorationEngine = ExplorationEngine { 5 })
        val before = vm.uiState.value
        val logSize = before.explorationLog.size

        val result = vm.explore(ExplorationMode.RIFT)

        val after = vm.uiState.value
        assertNotNull(result)
        assertEquals(ExplorationMode.RIFT, result!!.mode)
        assertEquals(before.gateEnergy - ExplorationMode.RIFT.energyCost, after.gateEnergy)
        assertEquals(before.explorationCount + 1, after.explorationCount)
        assertEquals(result, after.lastExploration)
        assertEquals(logSize + 1, after.explorationLog.size)
        assertTrue(after.explorationLog.last().startsWith("[탐색 · 차원 틈새 탐색]"))
        assertTrue(after.statusMessage.contains("탐색"))
    }

    @Test
    fun `보상은 금화 보석 성수 자원에 실제로 반영된다`() {
        // 0~29 : 금화 판정
        val vm = TcgGameViewModel(explorationEngine = ExplorationEngine { 5 })
        val before = vm.uiState.value
        val result = vm.explore(ExplorationMode.RIFT)!!
        val after = vm.uiState.value

        val goldReward = result.rewards.first { it.type == ExplorationRewardType.GOLD }
        assertEquals(before.gold + goldReward.amount, after.gold)
        assertEquals(before.gems, after.gems)
        assertEquals(before.holyWater, after.holyWater)
    }

    @Test
    fun `전투 조우 판정은 이계 전투 무대를 세팅한다`() {
        // 72~87 : 조우 판정
        val vm = TcgGameViewModel(explorationEngine = ExplorationEngine { 75 })
        vm.explore(ExplorationMode.RIFT)

        val state = vm.uiState.value
        val result = state.lastExploration
        assertNotNull(result)
        assertTrue("조우 판정이어야 한다", result!!.startsBattle)
        assertEquals(result.enemyName, state.enemyName)
        assertEquals(result.enemyMaxHp, state.enemyMaxHp)
        assertEquals(result.enemyHp, state.enemyHp)
        assertFalse(state.isDemonLordBattle)
        assertEquals(3, state.party.size)
        assertTrue(state.statusMessage.contains("교전"))

        // 조우한 상대와 바로 전투할 수 있다
        val before = state.enemyHp
        vm.attackFront()
        assertTrue("조우 후 바로 공격이 가능하다", vm.uiState.value.enemyHp < before)
    }

    @Test
    fun `기억 파편 수색은 기억이 지워진 동료에게 파편을 준다`() {
        val vm = TcgGameViewModel(explorationEngine = ExplorationEngine { 10 })
        val memoryCard = dieAndReacquire(vm)
        assertTrue("환생 재획득 카드는 기억이 지워진 상태다", memoryCard.isMemoryReset)

        val result = vm.explore(ExplorationMode.FRAGMENT)!!
        val after = vm.uiState.value
        val updated = after.ownedCards.first { it.id == memoryCard.id }

        assertTrue(result.rewards.any { it.type == ExplorationRewardType.MEMORY_FRAGMENT })
        assertEquals(memoryCard.id, result.fragmentTargetCardId)
        assertEquals(1, updated.memoryFragments)
        assertTrue(after.explorationLog.any { it.contains("기억 파편") })
    }

    @Test
    fun `기억이 지워진 동료가 없으면 잔향은 대체 보상으로 바뀐다`() {
        val vm = TcgGameViewModel(explorationEngine = ExplorationEngine { 10 })
        assertTrue(vm.uiState.value.memoryResetCards.isEmpty())
        val before = vm.uiState.value

        val result = vm.explore(ExplorationMode.FRAGMENT)!!

        val after = vm.uiState.value
        assertFalse(result.rewards.any { it.type == ExplorationRewardType.MEMORY_FRAGMENT })
        assertTrue(after.gems > before.gems || after.gold > before.gold)
        assertNull(result.fragmentTargetCardId)
    }

    @Test
    fun `랜덤 조우는 호감도와 유혹 스택을 올릴 수 있다`() {
        // 40~59 : 아우렐리아 밤 산책 — 교감은 보조 폭(+1)만 오른다
        val walk = TcgGameViewModel(explorationEngine = ExplorationEngine { 45 })
        val before = walk.uiState.value
        walk.explore(ExplorationMode.ENCOUNTER)
        assertEquals(before.partnerBond + AffinityRules.WALK_GAIN, walk.uiState.value.partnerBond)
        assertTrue(walk.uiState.value.lobbyHeroBond >= 0)

        // 20~39 : 위장한 마왕 릴리 — 금화는 늘고 [홀림]이 쌓인다
        val lily = TcgGameViewModel(explorationEngine = ExplorationEngine { 25 })
        val lilyBefore = lily.uiState.value
        lily.explore(ExplorationMode.ENCOUNTER)
        val lilyAfter = lily.uiState.value
        assertTrue(lilyAfter.gold > lilyBefore.gold)
        assertTrue("유혹 스택이 쌓여야 한다", lilyAfter.combatPenalty.totalStacks >= 1)
        assertTrue(lilyAfter.combatPenalty.isCharmed)
    }

    @Test
    fun `에너지가 모두 소진되면 탐색이 거부된다`() {
        val vm = TcgGameViewModel()
        repeat(TcgGameViewModel.GATE_ENERGY_START) { vm.explore(ExplorationMode.RIFT) }

        assertEquals(0, vm.uiState.value.gateEnergy)
        val logSize = vm.uiState.value.explorationLog.size

        val denied = vm.explore(ExplorationMode.RIFT)

        assertNull("에너지가 없으면 탐색 결과가 없다", denied)
        assertEquals(0, vm.uiState.value.gateEnergy)
        assertEquals("기록도 늘지 않는다", logSize, vm.uiState.value.explorationLog.size)
        assertTrue(vm.uiState.value.statusMessage.contains("탐색 불가"))
    }

    @Test
    fun `이계 전투 승리는 차원 균열 에너지를 회복한다`() {
        val vm = TcgGameViewModel()
        repeat(3) { vm.explore(ExplorationMode.RIFT) }
        val spent = vm.uiState.value.gateEnergy
        assertEquals(TcgGameViewModel.GATE_ENERGY_START - 3, spent)

        vm.startFieldBattle()
        var guard = 0
        while (vm.uiState.value.enemyHp > 0 && guard < 30) {
            vm.attackFront()
            if (vm.uiState.value.enemyHp > 0) vm.nextTurn()
            guard++
        }

        val after = vm.uiState.value
        assertEquals(0, after.enemyHp)
        assertEquals(spent + TcgGameViewModel.GATE_ENERGY_RECOVERY, after.gateEnergy)
        assertTrue(after.gateEnergy <= after.gateEnergyMax)
    }

    @Test
    fun `탐색 모드는 셋이고 각각 에너지를 한 칸 쓴다`() {
        assertEquals(3, ExplorationMode.values().size)
        ExplorationMode.values().forEach { mode ->
            assertEquals(1, mode.energyCost)
            assertTrue(mode.displayName.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }
        assertEquals(
            listOf("차원 틈새 탐색", "기억 파편 수색", "랜덤 조우"),
            ExplorationMode.values().map { it.displayName }
        )
    }

    @Test
    fun `탐색 엔진은 같은 판정값에 같은 결과를 돌려준다`() {
        val context = ExplorationContext(
            gateEnergy = 10,
            temptationStacks = 0,
            memoryResetCardIds = listOf(3),
            explorationCount = 0,
            partnerBond = 20,
            gold = 0
        )
        val first = ExplorationEngine { 55 }.explore(ExplorationMode.RIFT, context)
        val second = ExplorationEngine { 55 }.explore(ExplorationMode.RIFT, context)

        assertEquals(first.title, second.title)
        assertEquals(first.narrative, second.narrative)
        assertEquals(first.rewards, second.rewards)
    }

    // ── 헬퍼: 마왕전에서 카드를 잃고 재획득해 '기억이 지워진 동료'를 만든다 ──
    private fun dieAndReacquire(vm: TcgGameViewModel): TcgCard {
        vm.startDemonLordBattle()
        var guard = 0
        while (vm.uiState.value.reincarnationPool.isEmpty() && guard < 30) {
            vm.nextTurn()
            guard++
        }
        val lost = vm.uiState.value.reincarnationPool.first()
        vm.reacquire(lost.templateId)
        return vm.uiState.value.ownedCards.first { it.id == lost.templateId }
    }
}
