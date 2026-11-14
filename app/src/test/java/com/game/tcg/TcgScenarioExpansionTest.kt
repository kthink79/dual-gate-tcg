package com.game.tcg

import com.game.tcg.core.engine.IMemoryRecallEngine
import com.game.tcg.core.engine.IPairUltimateEngine
import com.game.tcg.core.engine.ITrustRiftEngine
import com.game.tcg.core.engine.MemoryRecallEngine
import com.game.tcg.core.engine.PairUltimateEngine
import com.game.tcg.core.engine.TrustRiftEngine
import com.game.tcg.core.model.CombatPenalty
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.ManaState
import com.game.tcg.core.model.MemoryFragmentSource
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.TemptationState
import com.game.tcg.core.model.TrustRiftChoiceType
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 추가 시나리오 3종 & 관련 UI 단위 테스트
 * 1) 「기억의 잔향: 환생한 파트너와의 재회」 — 기억 초기화·파편 회상
 * 2) 「신뢰의 균열: 마왕의 홀림을 눈치챈 파트너」 — 현실·이세계 교차 이벤트
 * 3) 「페어 얼티밋 오의 컷인 & 제2장 프롤로그(그림자 기사)」
 */
class TcgScenarioExpansionTest {

    private fun memoryResetHero(
        id: Int = 7,
        name: String = "잊혀진 성기사 아우렐리아",
        baseAtk: Int = 5000,
        baseDef: Int = 4000
    ): TcgCard = TcgCard(
        id = id,
        name = name,
        element = Element.FIRE,
        baseCost = 5,
        atk = baseAtk,
        def = baseDef,
        skillText = "전열 방벽 형성 · 아군 피해 15% 분산",
        isMemoryReset = true,
        memoryFragments = 0
    )

    private fun noPenalty(): CombatPenalty = CombatPenalty(
        totalStacks = 0,
        attackReductionPercent = 0,
        damageTakenIncreasePercent = 0,
        criticalReductionPercent = 0,
        extraManaCost = 0,
        isCharmed = false,
        debuffDescription = "[정신 맑음]"
    )

    // =========================================================================
    // 1. 「기억의 잔향」 — 기억 초기화 · 기억 파편 · 회상 이벤트
    // =========================================================================

    @Test
    fun `기억 파편은 목표치 3까지만 쌓이고 회상 준비 상태가 된다`() {
        val engine: IMemoryRecallEngine = MemoryRecallEngine()
        val card = memoryResetHero()

        assertEquals("0/3", card.memoryFragmentGauge)
        assertFalse(card.isMemoryRecallReady)

        val first = engine.grantFragment(card, 1, MemoryFragmentSource.REAL_WORLD_BOND)
        assertTrue(first.isSuccess)
        assertEquals(1, first.progress.fragments)
        assertEquals("1/3", first.progress.gaugeText)
        assertFalse(first.isRecallUnlocked)

        engine.grantFragment(card, 1, MemoryFragmentSource.BATTLE)
        val third = engine.grantFragment(card, 1, MemoryFragmentSource.BATTLE)
        assertTrue("3개째에서 회상 이벤트가 열려야 한다", third.isRecallUnlocked)
        assertEquals(3, card.memoryFragments)
        assertTrue(engine.canOpenRecallEvent(card))

        // 목표치를 넘겨 쌓이지 않는다
        val overflow = engine.grantFragment(card, 5, MemoryFragmentSource.BATTLE)
        assertFalse(overflow.isSuccess)
        assertEquals(3, card.memoryFragments)
    }

    @Test
    fun `기억이 온전한 카드는 기억 파편을 얻지 못한다`() {
        val engine: IMemoryRecallEngine = MemoryRecallEngine()
        val intact = TcgCard(
            id = 3,
            name = "성기사 아우렐리아",
            element = Element.FIRE,
            baseCost = 5,
            atk = 5600,
            def = 6200,
            skillText = "전열 방벽"
        )

        val gain = engine.grantFragment(intact, 1, MemoryFragmentSource.BATTLE)

        assertFalse(gain.isSuccess)
        assertEquals(0, intact.memoryFragments)
        assertTrue(gain.message.contains("기억이 온전한 상태"))
    }

    @Test
    fun `기억의 조각 회상은 스탯 보너스와 기억 복원을 적용한다`() {
        val engine: IMemoryRecallEngine = MemoryRecallEngine()
        val card = memoryResetHero()

        // 파편이 부족하면 열리지 않는다
        val blocked = engine.openRecallEvent(card)
        assertFalse(blocked.isSuccess)
        assertTrue(blocked.failureReason!!.contains("부족"))

        engine.grantFragment(card, 3, MemoryFragmentSource.BATTLE)
        val result = engine.openRecallEvent(card)

        assertTrue(result.isSuccess)
        assertEquals((5000 * 0.15).toInt(), result.atkBonus)
        assertEquals((4000 * 0.15).toInt(), result.defBonus)
        assertEquals(5000 + 750, card.atk)
        assertEquals(4000 + 600, card.def)
        assertEquals(20, card.bondLevel)
        assertFalse("회상 후에는 기억 초기화 상태가 해제된다", card.isMemoryReset)
        assertEquals(0, card.memoryFragments)
        assertNotNull(result.dialogue)
        assertTrue(result.dialogue!!.lines.isNotEmpty())
    }

    @Test
    fun `환생 재획득은 기억이 지워진 동료 상태를 만들고 교감으로 파편을 얻는다`() {
        val vm = TcgGameViewModel()
        vm.startDemonLordBattle()

        var guard = 0
        while (vm.uiState.value.reincarnationPool.none { it.templateId == 3 } && guard < 20) {
            vm.nextTurn()
            guard++
        }
        val lost = vm.uiState.value.reincarnationPool.firstOrNull { it.templateId == 3 }
        assertNotNull("아우렐리아가 마왕전에서 상실되어야 한다", lost)

        vm.reacquire(3)

        val reborn = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertTrue("재획득 카드는 기억 초기화 상태로 돌아온다", reborn.isMemoryReset)
        assertEquals(0, reborn.memoryFragments)

        // 현실 교감 경로: 기억이 지워진 동료와 교감하면 파편 +1
        vm.bondMemoryResetPartner(3)
        val afterBond = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals(1, afterBond.memoryFragments)
        assertNotNull(vm.uiState.value.memoryRecallMessage)

        // 현실 교감 화면 경로도 파편을 준다
        vm.interactRealWorld(com.game.tcg.core.engine.RealWorldActivity.DAILY_GREETING)
        assertEquals(2, vm.uiState.value.ownedCards.first { it.id == 3 }.memoryFragments)
    }

    @Test
    fun `전투 승리는 기억이 지워진 동료에게 기억 파편을 준다`() {
        val vm = TcgGameViewModel()
        vm.startDemonLordBattle()
        var guard = 0
        while (vm.uiState.value.reincarnationPool.none { it.templateId == 3 } && guard < 20) {
            vm.nextTurn()
            guard++
        }
        vm.reacquire(3)
        assertEquals(0, vm.uiState.value.ownedCards.first { it.id == 3 }.memoryFragments)

        // 마수 원정을 승리로 끝낸다
        vm.startFieldBattle()
        var turns = 0
        while (vm.uiState.value.enemyHp > 0 && turns < 20) {
            vm.attackFront()
            if (vm.uiState.value.enemyHp > 0) vm.nextTurn()
            turns++
        }

        val after = vm.uiState.value
        assertTrue("원정 승리", after.enemyHp == 0)
        assertEquals(1, after.ownedCards.first { it.id == 3 }.memoryFragments)
    }

    @Test
    fun `파편 3개를 모으면 도감에서 회상 이벤트를 열어 기억을 복원한다`() {
        val vm = TcgGameViewModel()
        vm.startDemonLordBattle()
        var guard = 0
        while (vm.uiState.value.reincarnationPool.none { it.templateId == 3 } && guard < 20) {
            vm.nextTurn()
            guard++
        }
        vm.reacquire(3)

        repeat(3) { vm.bondMemoryResetPartner(3) }
        val ready = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals(3, ready.memoryFragments)

        val result = vm.openMemoryRecall(3)

        assertTrue(result!!.isSuccess)
        val recalled = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertFalse(recalled.isMemoryReset)
        assertEquals(0, recalled.memoryFragments)
        assertTrue(recalled.atk > 5600)
        assertNotNull(vm.uiState.value.memoryRecallDialogue)
    }

    // =========================================================================
    // 2. 「신뢰의 균열」 — 현실·이세계 교차 이벤트
    // =========================================================================

    @Test
    fun `홀림 2스택 미만이면 교차 이벤트가 열리지 않는다`() {
        val engine: ITrustRiftEngine = TrustRiftEngine()

        assertFalse(engine.isAvailable(temptationStacks = 1, alreadyResolved = false))
        assertTrue(engine.isAvailable(temptationStacks = 2, alreadyResolved = false))
        assertFalse(engine.isAvailable(temptationStacks = 4, alreadyResolved = true))
        assertEquals(2, engine.choices().size)
    }

    @Test
    fun `솔직한 고백은 성수 없이 홀림 스택을 낮추고 호감도를 올린다`() {
        val engine: ITrustRiftEngine = TrustRiftEngine()
        val state = TemptationState(gazeDurationSeconds = 20, gazeZoomCount = 1, gazeTemptationStacks = 2)

        val honest = engine.choices().first { it.type == TrustRiftChoiceType.HONEST_CONFESSION }
        val result = engine.resolve(honest.id, state, bondLevel = 20)

        assertTrue(result.isSuccess)
        assertEquals(1, result.resolvedState.gazeTemptationStacks)
        assertEquals(1, result.resolvedState.totalTemptationStacks)
        assertEquals(10, result.bondDelta)
        assertEquals(30, result.newBondLevel)
        assertEquals(0, result.frontRowDefensePenaltyPercent)
        // 성수 자원은 건드리지 않는다 (엔진은 성수를 받지 않는다)
        assertEquals(0, state.stolenGold)
    }

    @Test
    fun `숨긴 선택은 홀림을 키우고 호감도를 떨어뜨리며 전열 방어 페널티를 남긴다`() {
        val engine: ITrustRiftEngine = TrustRiftEngine()
        val state = TemptationState(gazeTemptationStacks = 2)

        val hide = engine.choices().first { it.type == TrustRiftChoiceType.HIDE_IT }
        val result = engine.resolve(hide.id, state, bondLevel = 40)

        assertTrue(result.isSuccess)
        assertEquals(1, result.resolvedState.curseOfArchdemonStacks)
        assertEquals(3, result.resolvedState.totalTemptationStacks)
        assertEquals(-15, result.bondDelta)
        assertEquals(25, result.newBondLevel)
        assertEquals(10, result.frontRowDefensePenaltyPercent)
    }

    @Test
    fun `시나리오 탭 교차 이벤트는 홀림 2스택에서 활성되고 선택 후 닫힌다`() {
        val vm = TcgGameViewModel()
        assertFalse("초기에는 이벤트가 잠겨 있다", vm.uiState.value.isTrustRiftAvailable)

        vm.gazeAtDemonLord() // 핀치 줌 + 10초 응시 → 2스택
        val ready = vm.uiState.value
        assertEquals(2, ready.combatPenalty.totalStacks)
        assertTrue(ready.isTrustRiftAvailable)

        val resolution = vm.resolveTrustRift("RIFT_A_CONFESS")

        assertTrue(resolution!!.isSuccess)
        val after = vm.uiState.value
        assertEquals("RIFT_A_CONFESS", after.trustRiftChoice)
        assertEquals(1, after.combatPenalty.totalStacks)
        assertEquals("호감도 +10", 30, after.partnerBond)
        assertFalse("이미 선택한 이벤트는 다시 열리지 않는다", after.isTrustRiftAvailable)
        assertNotNull(after.trustRiftMessage)

        // 두 번째 선택은 무시된다
        val second = vm.resolveTrustRift("RIFT_B_HIDE")
        assertNull(second)
        assertEquals(1, vm.uiState.value.combatPenalty.totalStacks)
    }

    @Test
    fun `숨긴 선택의 전열 방어 페널티는 다음 전투에서 실제 피해를 키운다`() {
        fun totalHp(vm: TcgGameViewModel): Int = vm.uiState.value.party.sumOf { it.hp }

        val baseline = TcgGameViewModel()
        baseline.gazeAtDemonLord()
        baseline.startFieldBattle()
        val baseBefore = totalHp(baseline)
        baseline.nextTurn()
        val baseLoss = baseBefore - totalHp(baseline)

        val hidden = TcgGameViewModel()
        hidden.gazeAtDemonLord()
        hidden.resolveTrustRift("RIFT_B_HIDE")
        assertEquals(10, hidden.uiState.value.frontRowDefensePenaltyPercent)
        hidden.startFieldBattle()
        val hideBefore = totalHp(hidden)
        hidden.nextTurn()
        val hideLoss = hideBefore - totalHp(hidden)

        assertTrue("기준 피해가 기록되어야 한다", baseLoss > 0)
        assertTrue(
            "전열 방어 -10% + 저주 스택으로 피해가 더 커져야 한다 ($hideLoss > $baseLoss)",
            hideLoss > baseLoss
        )
        assertTrue(
            "전열 결손 방어 페널티가 로그에 남는다",
            hidden.uiState.value.battleLog.any { it.contains("전열 결손 방어 -10%") }
        )
    }

    // =========================================================================
    // 3. 「페어 얼티밋 오의」 컷인 & 제2장 프롤로그
    // =========================================================================

    @Test
    fun `페어 오의는 생존 2명과 마나 3 조건에서만 발동한다`() {
        val engine: IPairUltimateEngine = PairUltimateEngine()
        val attacker = TcgCard(
            id = 3,
            name = "성기사 아우렐리아",
            element = Element.FIRE,
            baseCost = 5,
            atk = 5600,
            def = 6200,
            skillText = "전열 방벽"
        )
        val defender = TcgCard(
            id = 900,
            name = "차원 틈새의 마수",
            element = Element.DARK,
            baseCost = 10,
            atk = 900,
            def = 12000,
            skillText = "심연의 압박"
        )

        // 생존 1명 → 불가
        val lonely = engine.activate(attacker, defender, noPenalty(), 1, ManaState(5, 5), false)
        assertFalse(lonely.isSuccess)
        assertTrue(lonely.failureReason!!.contains("2명 이상"))

        // 마나 부족 → 불가
        val noMana = engine.activate(attacker, defender, noPenalty(), 3, ManaState(2, 5), false)
        assertFalse(noMana.isSuccess)
        assertTrue(noMana.failureReason!!.contains("마나"))

        // 이미 사용 → 불가
        val used = engine.activate(attacker, defender, noPenalty(), 3, ManaState(5, 5), true)
        assertFalse(used.isSuccess)

        // 조건 충족 → 발동, 3인 합격이 2인 합격보다 강하다
        val duo = engine.activate(attacker, defender, noPenalty(), 2, ManaState(3, 3), false)
        val trio = engine.activate(attacker, defender, noPenalty(), 3, ManaState(3, 3), false)
        assertTrue(duo.isSuccess)
        assertTrue(trio.isSuccess)
        assertTrue(duo.damage > 0)
        assertTrue("3인 합격 배율이 더 높아야 한다", trio.damage > duo.damage)
        assertEquals(3, duo.manaSpent)
        assertTrue(duo.bannerTitle.contains("페어 얼티밋 오의"))
        assertEquals(2, duo.logLines.size)
    }

    @Test
    fun `전투 화면 페어 오의 버튼은 마나 3에서 발동하고 전투당 1회로 제한된다`() {
        val vm = TcgGameViewModel()
        vm.startDemonLordBattle()
        // 마나 1 → 3까지 턴 진행
        val early = vm.activatePairUltimate()
        assertNotNull(early)
        assertFalse("마나가 1이면 발동할 수 없다", early!!.isSuccess)
        vm.nextTurn()
        vm.nextTurn()

        val before = vm.uiState.value
        assertEquals(3, before.mana.currentMana)
        assertTrue(before.canActivatePairUltimate)

        val result = vm.activatePairUltimate()

        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        val after = vm.uiState.value
        assertTrue("적 HP가 크게 줄어야 한다", after.enemyHp < before.enemyHp - 5000)
        assertEquals(0, after.mana.currentMana)
        assertTrue(after.pairUltimateUsedThisBattle)
        assertFalse(after.canActivatePairUltimate)
        assertNotNull(after.pairUltimateBanner)
        assertEquals(1, after.pairUltimateCount)
        assertTrue(after.battleLog.any { it.contains("페어 얼티밋 오의") })

        // 전투당 1회 제한
        vm.nextTurn()
        vm.nextTurn()
        vm.nextTurn()
        val again = vm.activatePairUltimate()
        assertNotNull(again)
        assertFalse(again!!.isSuccess)
        assertEquals(1, vm.uiState.value.pairUltimateCount)
    }

    @Test
    fun `제2장 프롤로그 노드는 5화 이후 열리고 그림자 기사 전초전을 시작한다`() {
        val locked = TcgGameViewModel()
        locked.startChapter2BossBattle()
        assertEquals("차원 틈새의 마수", locked.uiState.value.enemyName)
        assertTrue(locked.uiState.value.statusMessage.contains("5화"))

        val vm = TcgGameViewModel()
        repeat(8) { vm.advanceStory() }
        assertEquals(5, vm.uiState.value.storyStepIndex)
        assertTrue(vm.uiState.value.isChapter2Unlocked)

        vm.openChapter2Prelude()
        assertTrue(vm.uiState.value.chapter2PreludeOpened)
        assertNotNull(vm.uiState.value.chapter2Message)

        vm.startChapter2BossBattle()
        val battle = vm.uiState.value
        assertEquals("제1군단장 그림자 기사", battle.enemyName)
        assertEquals(32000, battle.enemyHp)
        assertEquals(3400, battle.enemyAtk)
        assertEquals(Element.DARK, battle.enemyElement)
        assertTrue(battle.isChapter2BossBattle)
        assertTrue(battle.battleLog.any { it.contains("제1군단장 그림자 기사") })
    }

    @Test
    fun `새 원정을 시작하면 페어 오의 사용 제한이 초기화된다`() {
        val vm = TcgGameViewModel()
        vm.startDemonLordBattle()
        vm.nextTurn()
        vm.nextTurn()
        assertTrue(vm.activatePairUltimate()!!.isSuccess)
        assertTrue(vm.uiState.value.pairUltimateUsedThisBattle)

        vm.startFieldBattle()

        assertFalse(vm.uiState.value.pairUltimateUsedThisBattle)
        assertNull(vm.uiState.value.pairUltimateBanner)
        assertTrue(vm.uiState.value.canActivatePairUltimate.not())
    }
}
