package com.game.tcg

import com.game.tcg.core.engine.ExplorationContext
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
    fun `메인 로비는 대표 캐릭터와 좌우 측면 서브메뉴로 시작한다`() {
        val state = TcgGameViewModel().uiState.value

        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
        assertEquals(TcgGameViewModel.DEFAULT_LOBBY_HERO_ID, state.lobbyHeroId)
        assertEquals("아우렐리아", state.lobbyHeroProfile?.displayName)
        assertEquals(VisualState.BATTLE, state.lobbyVisual)

        assertEquals(
            listOf(LobbyDestination.SCENARIO, LobbyDestination.BATTLE, LobbyDestination.EXPLORATION),
            LobbyDestination.LEFT_ITEMS
        )
        assertEquals(
            listOf(LobbyDestination.REAL_WORLD, LobbyDestination.COLLECTION, LobbyDestination.DECK),
            LobbyDestination.RIGHT_ITEMS
        )
        assertEquals(SideRail.LEFT, LobbyDestination.SCENARIO.rail)
        assertEquals(SideRail.RIGHT, LobbyDestination.DECK.rail)
        assertEquals("이세계 접속", LobbyDestination.SCENARIO.label)
        assertEquals("탐색", LobbyDestination.EXPLORATION.label)
        assertEquals("캐릭터 관리", LobbyDestination.COLLECTION.label)
        assertEquals("서약·덱", LobbyDestination.DECK.label)
    }

    @Test
    fun `좌우 전환은 보유 캐릭터를 순환한다`() {
        val vm = TcgGameViewModel()
        // 스타팅 3인방 + 개성파 30인(155~184) 순서
        assertEquals(listOf(3, 1, 2), vm.uiState.value.lobbyRoster.take(3))
        assertEquals(33, vm.uiState.value.lobbyRoster.size)

        vm.cycleLobbyHero(1)
        assertEquals(1, vm.uiState.value.lobbyHeroId)
        assertEquals("세라피엘", vm.uiState.value.lobbyHeroProfile?.displayName)

        vm.cycleLobbyHero(1)
        assertEquals(2, vm.uiState.value.lobbyHeroId)

        vm.cycleLobbyHero(1)
        assertEquals("세 번째 다음은 첫 개성파", 155, vm.uiState.value.lobbyHeroId)

        // 로스터 끝까지 돌면 처음으로 돌아온다
        repeat(29) { vm.cycleLobbyHero(1) }
        assertEquals(184, vm.uiState.value.lobbyHeroId)
        vm.cycleLobbyHero(1)
        assertEquals("마지막 다음은 처음으로 돌아온다", 3, vm.uiState.value.lobbyHeroId)

        vm.cycleLobbyHero(-1)
        assertEquals("역방향도 순환한다", 184, vm.uiState.value.lobbyHeroId)
    }

    @Test
    fun `대표 캐릭터를 직접 선택할 수 있고 미보유 캐릭터는 거부된다`() {
        val vm = TcgGameViewModel()

        vm.selectLobbyHero(1)
        assertEquals(1, vm.uiState.value.lobbyHeroId)
        assertEquals("태양의 대천사", vm.uiState.value.lobbyHeroProfile?.title)

        // 릴리스(112)는 아직 계약하지 않은 마왕 — 로비 로스터에 없다
        vm.selectLobbyHero(112)
        assertEquals("미보유 캐릭터는 대표로 세울 수 없다", 1, vm.uiState.value.lobbyHeroId)
        assertTrue(vm.uiState.value.statusMessage.contains("계약하지 않은"))
    }

    @Test
    fun `3단 비주얼은 전투 일상은 바로 데이트는 호감도 50부터 열린다`() {
        val vm = TcgGameViewModel()

        vm.selectLobbyVisual(VisualState.CASUAL_CUTE)
        assertEquals(VisualState.CASUAL_CUTE, vm.uiState.value.lobbyVisual)

        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertEquals("호감도 50 전에는 데이트 룩이 잠긴다", VisualState.CASUAL_CUTE, vm.uiState.value.lobbyVisual)
        assertTrue(vm.uiState.value.statusMessage.contains("잠금"))

        // 현실 교감으로 호감도 50 달성 → 데이트 룩 해금
        vm.interactRealWorld(RealWorldActivity.ROMANTIC_EVENING_DATE)
        assertTrue(vm.uiState.value.isLobbyDateLookUnlocked)

        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.lobbyVisual)

        // 호감도가 낮은 캐릭터로 바꾸면 데이트 룩은 전투 룩으로 되돌아간다
        vm.selectLobbyHero(2)
        assertEquals(VisualState.BATTLE, vm.uiState.value.lobbyVisual)
    }

    @Test
    fun `대표 캐릭터의 호감도와 기억 파편 상태가 로비에 반영된다`() {
        val vm = TcgGameViewModel()
        val state = vm.uiState.value

        assertNotNull(state.lobbyHero)
        assertEquals(20, state.lobbyHeroBond)
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
        assertTrue("초기에는 경보가 없다", vm.uiState.value.lobbyAlerts.isEmpty())

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
        // 40~59 : 아우렐리아 밤 산책
        val walk = TcgGameViewModel(explorationEngine = ExplorationEngine { 45 })
        val before = walk.uiState.value
        walk.explore(ExplorationMode.ENCOUNTER)
        assertEquals(before.partnerBond + 10, walk.uiState.value.partnerBond)
        assertTrue(walk.uiState.value.lobbyHeroBond >= 30)

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
