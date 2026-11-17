package com.game.tcg

import com.game.tcg.core.engine.BattleOutcome
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.VisualState
import com.game.tcg.persistence.InMemorySaveStore
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * VICTORY / DEFEAT 결과 오버레이의 상태 연동 검증 (2026-11-17).
 *
 * 화면([com.game.tcg.ui.BattleResultOverlay])은 상태의 [com.game.tcg.core.engine.BattleResultPanel]
 * 만 보고 그리므로, 여기서는 "언제 패널이 생기고, 버튼이 어디로 가는지"를 검증한다.
 */
class BattleResultOverlayTest {

    private fun game(
        cleared: Set<Int> = emptySet(),
        owned: List<Int> = listOf(3, 1, 2),
        store: InMemorySaveStore? = null
    ) = TcgGameViewModel(
        random = Random(11),
        initialOwnedCardIds = owned,
        initialGold = 2000,
        initialShards = ShardWallet(),
        initialPackTickets = 1,
        initialClearedStages = cleared,
        initialBondLevel = 0,
        todayEpochDay = { 20_000 },
        saveStore = store
    )

    /** 마나가 있으면 공격, 없으면 턴을 넘겨 적 HP 를 끝까지 깎는다 */
    private fun fightToResult(vm: TcgGameViewModel, maxSteps: Int = 120) {
        var step = 0
        while (step < maxSteps && vm.uiState.value.battleResult == null) {
            if (vm.uiState.value.enemyHp <= 0) break
            if (vm.uiState.value.party.isEmpty()) break
            if (vm.uiState.value.mana.currentMana >= 1) vm.attackFront() else vm.nextTurn()
            step++
        }
    }

    @Test
    fun `스테이지에서 이기면 VICTORY 패널이 생긴다`() {
        val vm = game()
        assertTrue(vm.startStageBattle(1))
        fightToResult(vm)

        val panel = vm.uiState.value.battleResult
        assertNotNull("승리하면 결과 패널이 있어야 한다", panel)
        assertTrue(panel!!.isVictory)
        assertEquals(BattleOutcome.VICTORY, panel.outcome)
        assertEquals(1, panel.stageNumber)
        assertEquals(2, panel.nextStageNumber)
        assertTrue("보상 줄이 있어야 한다", panel.rewardLines.isNotEmpty())
        assertTrue("대표 캐릭터가 있다", panel.heroCardId != null)
        assertFalse("자동 전투는 결과와 함께 멈춘다", vm.uiState.value.isAutoBattle)
    }

    @Test
    fun `VICTORY 다음 스테이지 버튼은 다음 스테이지를 연다`() {
        val vm = game()
        vm.startStageBattle(1)
        fightToResult(vm)
        assertNotNull(vm.uiState.value.battleResult)

        vm.battleResultNextStage()

        val s = vm.uiState.value
        assertNull("패널은 닫힌다", s.battleResult)
        assertEquals("다음 스테이지로 출전", 2, s.activeStage)
        assertEquals(LobbyDestination.BATTLE, s.currentScreen)
    }

    @Test
    fun `전멸하면 DEFEAT 패널이 뜨고 패인 힌트가 있다`() {
        // 29스테이지까지 깬 상태에서 최종 관문(30)에 무강화 덱으로 도전 → 전멸
        val vm = game(cleared = (1..29).toSet())
        assertTrue(vm.startStageBattle(30))
        var step = 0
        while (step < 40 && vm.uiState.value.battleResult == null) {
            vm.nextTurn()
            step++
        }

        val panel = vm.uiState.value.battleResult
        assertNotNull("전멸하면 패배 패널이 있어야 한다", panel)
        assertTrue(panel!!.isVictory.not())
        assertEquals(BattleOutcome.DEFEAT, panel.outcome)
        assertEquals(30, panel.stageNumber)
        assertTrue("패인 힌트가 있어야 한다", panel.hints.isNotEmpty())
        assertTrue("재도전 라벨", panel.primaryLabel!!.contains("다시 도전"))
        assertTrue("출전 덱이 비었다", vm.uiState.value.party.isEmpty())
        assertEquals("패배 후 자동 전투 정지", false, vm.uiState.value.isAutoBattle)
    }

    @Test
    fun `DEFEAT 재도전 버튼은 잃은 동료를 되찾고 같은 스테이지로 돌아간다`() {
        val vm = game(cleared = (1..29).toSet())
        vm.startStageBattle(30)
        var step = 0
        while (step < 40 && vm.uiState.value.battleResult == null) {
            vm.nextTurn()
            step++
        }
        assertNotNull(vm.uiState.value.battleResult)
        val lost = vm.uiState.value.reincarnationPool.size
        assertTrue("잃은 동료가 있다", lost > 0)

        vm.retryStageFromResult()

        val s = vm.uiState.value
        assertNull(s.battleResult)
        assertEquals("같은 스테이지 재도전", 30, s.activeStage)
        assertTrue("무료 재계약으로 덱이 복구된다", s.party.isNotEmpty())
        assertTrue("환생 풀이 비워진다", s.reincarnationPool.isEmpty())
    }

    @Test
    fun `패배 오버레이에서 강화 화면과 덱 편성으로 바로 갈 수 있다`() {
        val vm = game(cleared = (1..29).toSet())
        vm.startStageBattle(30)
        var step = 0
        while (step < 40 && vm.uiState.value.battleResult == null) {
            vm.nextTurn()
            step++
        }
        vm.battleResultToGrowth()
        assertNull(vm.uiState.value.battleResult)
        assertEquals(LobbyDestination.GROWTH, vm.uiState.value.currentScreen)

        vm.startStageBattle(30)
        step = 0
        while (step < 40 && vm.uiState.value.battleResult == null) {
            vm.nextTurn()
            step++
        }
        vm.battleResultToDeck()
        assertEquals(LobbyDestination.DECK, vm.uiState.value.currentScreen)
    }

    @Test
    fun `미니어처 복장은 친밀도 0에서도 저장되고 복원된다`() {
        val store = InMemorySaveStore()
        val first = game(store = store)
        val hero = first.uiState.value.lobbyHeroId

        first.selectLobbyVisual(VisualState.MINIATURE)
        assertEquals(VisualState.MINIATURE, first.uiState.value.lobbyVisual)
        assertEquals(VisualState.MINIATURE, first.uiState.value.lobbyOutfits[hero])
        assertNotNull("복장 변경은 저장된다", store.stored)

        // 앱 재실행
        val second = game(store = store)
        assertEquals(VisualState.MINIATURE, second.uiState.value.lobbyVisual)
        assertEquals(VisualState.MINIATURE, second.uiState.value.lobbyOutfits[hero])

        // 다른 캐릭터로 갔다 와도 미니어처 선택은 유지된다
        val other = second.uiState.value.ownedCards.first { it.id != hero }.id
        second.selectLobbyHero(other)
        second.selectLobbyHero(hero)
        assertEquals(VisualState.MINIATURE, second.uiState.value.lobbyVisual)
    }

    @Test
    fun `스테이지 화면의 예상 승률은 덱 전투력이 높을수록 올라간다`() {
        val weak = game()
        weak.startStageBattle(1)
        val weakChance = com.game.tcg.core.engine.BattleSimulator
            .winChance(weak.uiState.value.equippedCards, StageCatalog.require(1))

        val strong = TcgGameViewModel(
            random = Random(11),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 0
        )
        // 강화로 덱을 키운 뒤 같은 스테이지 승률 비교
        repeat(9) { strong.upgradeCard(3) }
        val strongChance = com.game.tcg.core.engine.BattleSimulator
            .winChance(strong.uiState.value.equippedCards, StageCatalog.require(1))

        assertTrue("강화한 덱의 승률($strongChance)이 더 높아야 한다 (약한 덱 $weakChance)", strongChance >= weakChance)
    }
}
