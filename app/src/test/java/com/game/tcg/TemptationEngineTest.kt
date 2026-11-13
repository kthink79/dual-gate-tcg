package com.game.tcg

import com.game.tcg.core.engine.BattleEngine
import com.game.tcg.core.engine.TemptationEngine
import com.game.tcg.core.model.*
import com.game.tcg.ui.TcgViewModel
import org.junit.Assert.*
import org.junit.Test

class TemptationEngineTest {

    private val temptationEngine = TemptationEngine()
    private val battleEngine = BattleEngine()

    @Test
    fun testRecordGaze_accumulatesDurationAndTimeStacks() {
        var state = TemptationState()

        // 5초 응시: 스택 없음 (10초 미만)
        state = temptationEngine.recordGaze(112, 5, false, state)
        assertEquals(5L, state.gazeDurationSeconds)
        assertEquals(0, state.gazeTemptationStacks)

        // 10초 추가 (누적 15초): 1스택 달성
        state = temptationEngine.recordGaze(112, 10, false, state)
        assertEquals(15L, state.gazeDurationSeconds)
        assertEquals(1, state.gazeTemptationStacks)

        // 20초 추가 (누적 35초): 3스택 달성
        state = temptationEngine.recordGaze(112, 20, false, state)
        assertEquals(35L, state.gazeDurationSeconds)
        assertEquals(3, state.gazeTemptationStacks)

        // 50초 추가 (누적 85초): 최대 5스택 상한선 캡
        state = temptationEngine.recordGaze(112, 50, false, state)
        assertEquals(85L, state.gazeDurationSeconds)
        assertEquals(5, state.gazeTemptationStacks)
    }

    @Test
    fun testRecordGaze_pinchZoomImmediatelyAddsStack() {
        var state = TemptationState()

        // 핀치 줌 1회: 즉시 1스택 및 줌 카운트 증가
        state = temptationEngine.recordGaze(113, 2, true, state)
        assertEquals(1, state.gazeZoomCount)
        assertEquals(1, state.gazeTemptationStacks)

        // 핀치 줌 2회 추가
        state = temptationEngine.recordGaze(113, 1, true, state)
        state = temptationEngine.recordGaze(113, 1, true, state)
        assertEquals(3, state.gazeZoomCount)
        assertEquals(3, state.gazeTemptationStacks)
    }

    @Test
    fun testUndercoverEncounter_temptedChoiceAddsCurseAndStealsGold() {
        var state = TemptationState()

        val temptedChoice = UndercoverDialogueOption(
            optionId = "opt_flirt_1",
            text = "릴리의 칵테일을 받아들며 손을 마주 잡는다",
            isTemptedChoice = true,
            goldStolen = 800,
            curseAdded = 2
        )

        state = temptationEngine.processUndercoverChoice(temptedChoice, state)

        assertEquals(1, state.undercoverEncounterCount)
        assertEquals(1, state.undercoverFlirtCount)
        assertEquals(2, state.curseOfArchdemonStacks)
        assertEquals(800, state.stolenGold)

        // 유혹 거부 선택지
        val rejectChoice = UndercoverDialogueOption(
            optionId = "opt_reject_1",
            text = "정중히 거절하고 의심스러운 눈빛으로 관찰한다",
            isTemptedChoice = false,
            goldStolen = 0,
            curseAdded = 0
        )

        state = temptationEngine.processUndercoverChoice(rejectChoice, state)
        assertEquals(2, state.undercoverEncounterCount)
        assertEquals(1, state.undercoverFlirtCount) // 증가하지 않음
        assertEquals(2, state.curseOfArchdemonStacks) // 유지됨
    }

    @Test
    fun testCombatPenaltyCalculation_scalesWithTotalStacks() {
        // 0스택: 디버프 없음
        val zeroState = TemptationState()
        val zeroPenalty = temptationEngine.calculateCombatPenalty(zeroState)
        assertFalse(zeroPenalty.isCharmed)
        assertEquals(0, zeroPenalty.attackReductionPercent)
        assertEquals(0, zeroPenalty.damageTakenIncreasePercent)
        assertEquals(0, zeroPenalty.criticalReductionPercent)
        assertEquals(0, zeroPenalty.extraManaCost)

        // 시선 2스택 + 저주 2스택 = 총 4스택
        val fourState = TemptationState(gazeTemptationStacks = 2, curseOfArchdemonStacks = 2)
        val fourPenalty = temptationEngine.calculateCombatPenalty(fourState)
        assertTrue(fourPenalty.isCharmed)
        assertEquals(4, fourPenalty.totalStacks)
        assertEquals(20, fourPenalty.attackReductionPercent) // 4 * 5 = 20%
        assertEquals(20, fourPenalty.damageTakenIncreasePercent) // 4 * 5 = 20%
        assertEquals(16, fourPenalty.criticalReductionPercent) // 4 * 4 = 16%
        assertEquals(1, fourPenalty.extraManaCost) // 3스택 이상이므로 +1 마나

        // 8스택: 최대 캡 검증 (ATK/DMG 최대 35%, CRIT 최대 25%)
        val maxState = TemptationState(gazeTemptationStacks = 4, curseOfArchdemonStacks = 4)
        val maxPenalty = temptationEngine.calculateCombatPenalty(maxState)
        assertEquals(8, maxPenalty.totalStacks)
        assertEquals(35, maxPenalty.attackReductionPercent) // 40% -> 35% 캡
        assertEquals(35, maxPenalty.damageTakenIncreasePercent) // 40% -> 35% 캡
        assertEquals(25, maxPenalty.criticalReductionPercent) // 32% -> 25% 캡
        assertEquals(1, maxPenalty.extraManaCost)
    }

    @Test
    fun testBattleEngineWithTemptation_reducesDamage() {
        val attacker = TcgCard(1, "성기사", Element.WIND, 5, 2000, 2000, skillText = "")
        val defender = TcgCard(2, "오크", Element.WIND, 5, 2000, 2000, skillText = "") // 동속성 상성 1.0

        // 정상 공격: 2000 데미지
        val normalResult = battleEngine.calculateDamage(attacker, defender)
        assertEquals(2000, normalResult.finalDamage)

        // 20% 공격력 저감 페널티 적용 (4스택)
        val penalty = CombatPenalty(
            totalStacks = 4,
            attackReductionPercent = 20,
            damageTakenIncreasePercent = 20,
            criticalReductionPercent = 16,
            extraManaCost = 1,
            isCharmed = true,
            debuffDescription = "매혹 디버프"
        )

        val charmedResult = battleEngine.calculateDamageWithTemptation(attacker, defender, penalty)
        // 유효 공격력 = 2000 * 0.8 = 1600
        assertEquals(1600, charmedResult.finalDamage)
        assertTrue(charmedResult.summaryMessage.contains("[마왕의 매혹] 공격력 -20%"))
    }

    @Test
    fun testDamageTakenPenalty_increasesIncomingDamage() {
        val baseIncoming = 1000
        val penalty = CombatPenalty(
            totalStacks = 4,
            attackReductionPercent = 20,
            damageTakenIncreasePercent = 20,
            criticalReductionPercent = 16,
            extraManaCost = 1,
            isCharmed = true,
            debuffDescription = "매혹 디버프"
        )

        val finalIncoming = temptationEngine.applyDamageTakenPenalty(baseIncoming, penalty)
        // 1000 * 1.2 = 1200
        assertEquals(1200, finalIncoming)
    }

    @Test
    fun testDemonLordDefeat_unmasksLordUnlocksCompanionAndReducesCurse() {
        val lilith = DemonLordRegistry.LILITH.copy()
        assertFalse(lilith.isDefeated)
        assertFalse(lilith.isVowedCompanion)

        val state = TemptationState(curseOfArchdemonStacks = 4)
        val updatedState = temptationEngine.onDemonLordDefeated(lilith, state)

        assertTrue(lilith.isDefeated)
        assertTrue(lilith.isVowedCompanion)
        assertEquals(1, updatedState.curseOfArchdemonStacks) // 4 - 3 = 1
    }

    @Test
    fun testPurifyTemptationWithHolyWater_cleansesStacks() {
        val state = TemptationState(gazeTemptationStacks = 3, curseOfArchdemonStacks = 3)
        val purified = temptationEngine.purifyTemptationWithHolyWater(state, 2)

        assertEquals(1, purified.gazeTemptationStacks)
        assertEquals(1, purified.curseOfArchdemonStacks)
    }

    @Test
    fun testViewModelTemptationIntegration() {
        val vm = TcgViewModel()
        assertEquals(0, vm.uiState.value.temptationState.totalTemptationStacks)
        assertFalse(vm.uiState.value.combatPenalty.isCharmed)

        // 마왕 릴리스 사진 확대 및 응시 발생
        vm.recordDemonLordGaze(112, 12L, true)

        val stateAfterGaze = vm.uiState.value
        assertTrue(stateAfterGaze.temptationState.totalTemptationStacks >= 2)
        assertTrue(stateAfterGaze.combatPenalty.isCharmed)
        assertTrue(stateAfterGaze.statusMessage.contains("[마왕의 시선 유혹]"))

        // 일상 잠입 선택지 선택
        val flirtOpt = UndercoverDialogueOption("opt1", "밀라의 드레스 품평 제안을 승낙한다", true, 500, 1)
        vm.chooseUndercoverOption(flirtOpt)
        assertTrue(vm.uiState.value.temptationState.undercoverFlirtCount == 1)

        // 마왕 토벌
        vm.defeatDemonLord(112)
        val defeatedLord = vm.uiState.value.demonLords.find { it.id == 112 }!!
        assertTrue(defeatedLord.isDefeated)
        assertTrue(defeatedLord.isVowedCompanion)
        assertTrue(vm.uiState.value.statusMessage.contains("[마왕 토벌 완료]"))
    }
}
