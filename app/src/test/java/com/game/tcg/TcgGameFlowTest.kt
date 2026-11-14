package com.game.tcg

import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.model.VisualState
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI 초안(시나리오·이벤트·전투·도감·현실 교감) 흐름 단위 테스트
 */
class TcgGameFlowTest {

    @Test
    fun `이벤트A 유혹 선택은 홀림 스택과 금화 유출을 만든다`() {
        val vm = TcgGameViewModel()
        val before = vm.uiState.value

        vm.chooseEventA(talkWithLily = true)

        val after = vm.uiState.value
        assertEquals("A_TALK", after.eventAChoice)
        assertNotNull(after.eventAResult)
        assertTrue("홀림 스택이 쌓여야 한다", after.combatPenalty.totalStacks >= 1)
        assertEquals(before.gold - 150, after.gold)
        assertEquals(before.lilyFamiliarity + 12, after.lilyFamiliarity)
        assertTrue(after.combatPenalty.isCharmed)
    }

    @Test
    fun `이벤트A 성수 정화는 방어 보너스를 주고 성수를 소모한다`() {
        val vm = TcgGameViewModel()
        val before = vm.uiState.value

        vm.chooseEventA(talkWithLily = false)

        val after = vm.uiState.value
        assertEquals("A_PURGE", after.eventAChoice)
        assertEquals(before.holyWater - 1, after.holyWater)
        assertEquals(10, after.defenseBonusPercent)
        assertEquals(0, after.combatPenalty.totalStacks)
    }

    @Test
    fun `이벤트B 즉시 전투는 마수를 세팅하고 정찰은 급소 정보를 남긴다`() {
        val fight = TcgGameViewModel()
        fight.chooseEventB(engageBattle = true)
        assertEquals("차원 틈새의 마수", fight.uiState.value.enemyName)
        assertEquals(12000, fight.uiState.value.enemyHp)

        val scout = TcgGameViewModel()
        val beforeGold = scout.uiState.value.gold
        scout.chooseEventB(engageBattle = false)
        assertEquals("B_SCOUT", scout.uiState.value.eventBChoice)
        assertEquals(15, scout.uiState.value.intelBonusPercent)
        assertEquals(beforeGold - 200, scout.uiState.value.gold)
    }

    @Test
    fun `전열 공격은 마나를 소모하고 적 HP를 깎는다`() {
        val vm = TcgGameViewModel()
        vm.startFieldBattle()
        val before = vm.uiState.value

        vm.attackFront()

        val after = vm.uiState.value
        assertTrue("적 HP가 줄어야 한다", after.enemyHp < before.enemyHp)
        assertEquals(before.mana.currentMana - 1, after.mana.currentMana)
        assertTrue(after.battleLog.last().contains("데미지"))
    }

    @Test
    fun `턴 진행은 마나를 회복하고 적이 반격한다`() {
        val vm = TcgGameViewModel()
        vm.startFieldBattle()
        vm.attackFront()

        vm.nextTurn()

        val after = vm.uiState.value
        assertEquals(2, after.turn)
        assertEquals(2, after.mana.currentMana)
        assertTrue("아군 피해가 기록되어야 한다", after.battleLog.last().contains("반격"))
    }

    @Test
    fun `마왕전에서 카드가 쓰러지면 영구 상실 후 재획득으로 복귀한다`() {
        val vm = TcgGameViewModel()
        vm.startDemonLordBattle()

        var guard = 0
        while (vm.uiState.value.reincarnationPool.isEmpty() && guard < 20) {
            vm.nextTurn()
            guard++
        }

        val lost = vm.uiState.value
        assertTrue("마왕의 반격으로 최소 1장이 상실되어야 한다", lost.reincarnationPool.isNotEmpty())
        val lostCard = lost.reincarnationPool.first()

        vm.reacquire(lostCard.templateId)

        val recovered = vm.uiState.value
        assertTrue(
            "재획득한 카드가 전장에 복귀해야 한다",
            recovered.party.any { it.card.id == lostCard.templateId }
        )
        assertTrue(recovered.reincarnationPool.none { it.templateId == lostCard.templateId })
    }

    @Test
    fun `마왕 카드 응시는 스택을 올리고 성수 정화는 스택을 내린다`() {
        val vm = TcgGameViewModel()

        vm.gazeAtDemonLord()
        val gazed = vm.uiState.value
        assertEquals(2, gazed.temptationState.gazeTemptationStacks)
        assertTrue(gazed.gazeMessage != null)

        vm.purify()
        val purified = vm.uiState.value
        assertEquals(0, purified.temptationState.gazeTemptationStacks)
        assertEquals(gazed.holyWater - 1, purified.holyWater)
    }

    @Test
    fun `현실 교감은 호감도를 올리고 데이트 룩을 해금한다`() {
        val vm = TcgGameViewModel()

        vm.interactRealWorld(RealWorldActivity.ROMANTIC_EVENING_DATE)

        val after = vm.uiState.value
        assertEquals(50, after.partnerBond)
        assertTrue("호감도 50에서 데이트 룩이 해금된다", after.dateLookUnlocked)
        assertEquals(VisualState.ROMANTIC_DATE, after.partnerVisual)
    }

    @Test
    fun `한계 돌파는 성급과 스탯을 올린다`() {
        val vm = TcgGameViewModel()
        vm.selectCard(3)
        val before = vm.uiState.value.ownedCards.first { it.id == 3 }

        vm.limitBreakSelected()

        val after = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals(before.starLevel + 1, after.starLevel)
        assertTrue(after.atk > before.atk)
        assertNotNull(vm.uiState.value.synthesisMessage)
    }

    @Test
    fun `신규 연성은 잡카드와 자금을 소모하고 새 캐릭터를 획득한다`() {
        val vm = TcgGameViewModel()
        val before = vm.uiState.value

        vm.craftNewCharacter()

        val after = vm.uiState.value
        assertEquals(before.junkCards - 3, after.junkCards)
        assertEquals(before.gold - 300, after.gold)
        assertEquals(before.ownedCards.size + 1, after.ownedCards.size)
    }

    @Test
    fun `스토리는 5화까지 진행되고 그 이후에는 완료 안내를 남긴다`() {
        val vm = TcgGameViewModel()
        repeat(8) { vm.advanceStory() }

        val after = vm.uiState.value
        assertEquals(5, after.storyStepIndex)
        assertTrue(after.statusMessage.contains("다음 챕터"))
    }

    @Test
    fun `파티 긴급 철수는 전리품을 버리고 생존 카드를 귀환시킨다`() {
        val vm = TcgGameViewModel()
        vm.startFieldBattle()

        val result = vm.emergencyRetreat()

        assertNotNull(result)
        assertTrue("긴급 철수는 성공해야 한다", result!!.isSuccess)
        val after = vm.uiState.value
        assertTrue(after.battleReserve.isNotEmpty())
        assertTrue(after.party.isEmpty())
    }
}
