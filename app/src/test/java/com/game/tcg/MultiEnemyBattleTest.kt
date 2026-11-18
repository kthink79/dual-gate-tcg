package com.game.tcg

import com.game.tcg.core.engine.BattleSimulator
import com.game.tcg.core.engine.SquadBattleEngine
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.EnemySquad
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.StartingRoster
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 다중 적 카드 전투 개편 검증 — 2026-11-18 사용자 요청.
 *
 * > "화면상 적 진영에는 9~12장이 표시되지만 실제 전투 엔진은 단일 enemyHp 풀만 상대한다.
 * >  적 진영 카드들이 개별 HP·공격력을 가진 다수 카드(전열·후열)로 순차/동시 교전하여
 * >  적 카드를 전부 쓰러뜨려야 승리하도록 개편하고, 1~4스테이지부터 난이도를 올려 달라"
 */
class MultiEnemyBattleTest {

    @Test
    fun `적 진영은 덱 크기만큼 개별 HP 카드로 편성된다`() {
        val stage = StageCatalog.require(1)
        val squad = EnemySquad.build(stage = stage, size = 9)

        assertEquals("적 카드 수 = 덱 크기", 9, squad.size)
        assertEquals("전열 3장", 3, squad.count { it.isFront })
        assertEquals("후열 6장", 6, squad.count { !it.isFront })
        assertEquals("총 HP = 스테이지 적 HP", stage.enemyHp, squad.sumOf { it.maxHp })
        assertEquals("전원 풀피로 시작", squad.size, squad.count { it.hp == it.maxHp && it.isAlive })
        assertTrue("전열이 후열보다 두껍다", squad.first().maxHp > squad.last().maxHp)
        assertTrue("전열이 후열보다 세다", squad.first().atk > squad.last().atk)
        assertTrue("보스 아트가 0번 슬롯", squad.first().isBoss)
        assertEquals("슬롯 번호는 0부터", listOf(0, 1, 2), squad.take(3).map { it.slot })
    }

    @Test
    fun `HP 분배는 총합이 정확히 스테이지 HP 와 같다`() {
        // 나머지가 생기는 값으로 나눠도 합계가 1 도 어긋나지 않아야 한다
        listOf(12100, 12101, 999, 12345, 9, 10).forEach { total ->
            val squad = EnemySquad.buildFromTotals(
                totalHp = total,
                perTurnAtk = 300,
                enemyName = "시험용 마수",
                enemyElement = Element.DARK,
                size = 9
            )
            assertEquals("총 HP $total", total, squad.sumOf { it.maxHp })
        }
    }

    @Test
    fun `아군 공격은 전열의 적 카드 한 장만 깎는다`() {
        val stage = StageCatalog.require(1)
        val squad = EnemySquad.build(stage = stage, size = 9)
        val outcome = SquadBattleEngine.playerAttack(
            attackerAtk = 300,
            attackerElement = Element.LIGHT,
            ultimate = false,
            units = squad
        )

        assertEquals("전열 첫 카드가 표적", 0, outcome.targetIndex)
        assertTrue("피해가 들어갔다", outcome.damage > 0)
        assertEquals("표적만 HP 가 줄어든다", (squad[0].hp - outcome.damage).coerceAtLeast(0), outcome.units[0].hp)
        (1 until squad.size).forEach { index ->
            assertEquals("#$index 는 그대로", squad[index].hp, outcome.units[index].hp)
        }
        assertFalse("한 장으로 끝나지 않는다", outcome.victory)
    }

    @Test
    fun `전열이 비면 후열을 때리고 전부 쓰러뜨려야 승리한다`() {
        val stage = StageCatalog.require(1)
        var squad = EnemySquad.build(stage = stage, size = 9)
        var attacks = 0
        var victory = false
        // 한 장씩 확실히 쓰러뜨리는 공격력으로 반복
        while (!victory && attacks < 40) {
            val outcome = SquadBattleEngine.playerAttack(
                attackerAtk = 99_999,
                attackerElement = Element.LIGHT,
                ultimate = false,
                units = squad
            )
            squad = outcome.units
            victory = outcome.victory
            attacks += 1
        }
        assertTrue("모든 적 카드를 쓰러뜨려야 승리", victory)
        assertEquals("9장을 9번에 걸쳐 격파", 9, attacks)
        assertEquals(0, EnemySquad.aliveCount(squad))
        assertEquals(0, EnemySquad.totalHp(squad))
    }

    @Test
    fun `적은 전열 최대 3장이 한 턴에 각자 반격한다`() {
        val stage = StageCatalog.require(3)
        val squad = EnemySquad.build(stage = stage, size = 9)
        val attackers = EnemySquad.attackersThisTurn(squad)

        assertEquals("한 턴 반격은 최대 3장", 3, attackers.size)
        assertTrue("전열 카드가 반격한다", attackers.all { it.isFront })
        assertEquals("반격 위협 = 3장 공격력 합", attackers.sumOf { it.atk }, EnemySquad.threatPerTurn(squad))
        // 전열을 모두 격파하면 후열이 3장까지 반격에 나선다
        val thinned = squad.mapIndexed { index, unit -> if (index < 3) unit.damaged(unit.hp) else unit }
        assertEquals(3, EnemySquad.attackersThisTurn(thinned).size)
        assertTrue(EnemySquad.attackersThisTurn(thinned).all { !it.isFront })
    }

    @Test
    fun `적 카드가 적을수록 한 턴 받는 피해가 줄어든다`() {
        val stage = StageCatalog.require(5)
        val squad = EnemySquad.build(stage = stage, size = 9)
        val front = listOf(Element.LIGHT)
        val full = SquadBattleEngine.expectedIncomingDamage(squad, front)
        val thinned = squad.mapIndexed { index, unit -> if (index < 2) unit.damaged(unit.hp) else unit }
        val after = SquadBattleEngine.expectedIncomingDamage(thinned, front)
        assertTrue("남은 적이 줄면 반격도 약해진다", after < full)
    }

    @Test
    fun `상성 – 유리 속성으로 때리면 적 카드 피해가 커진다`() {
        val squad = EnemySquad.buildFromTotals(
            totalHp = 30000, perTurnAtk = 300, enemyName = "바람 마수",
            enemyElement = Element.WIND, size = 9
        )
        val fire = SquadBattleEngine.playerAttack(3000, Element.FIRE, false, squad)
        val ice = SquadBattleEngine.playerAttack(3000, Element.ICE, false, squad)
        assertTrue("화염 > 빙결 (바람 적)", fire.damage > ice.damage)
        assertEquals("유리 상성 배율", Element.ADVANTAGE_MULTIPLIER, fire.multiplier, 0.001)
    }

    @Test
    fun `전투 화면 상태의 적 HP 는 개별 카드 HP 의 합이다`() {
        val vm = TcgGameViewModel(random = Random(3))
        vm.startFieldBattle()

        val state = vm.uiState.value
        assertEquals("적 카드 9장", 9, state.enemyUnitCount)
        assertEquals("합 HP", state.enemyUnits.sumOf { it.hp }, state.enemyHp)
        assertNotNull("표적 카드가 있다", state.enemyTarget)
        assertTrue("전열이 표적", state.enemyTarget!!.isFront)

        val targetCard = state.enemyTarget!!
        assertTrue("시작 적 HP 는 카드 합", state.enemyHp > 0)
        vm.attackFront()
        val after = vm.uiState.value
        // 2026-11-18 다중 적 — 한 번의 공격은 전열 첫 카드만 무너뜨린다
        assertEquals("전열 첫 카드가 쓰러진다", 0, after.enemyUnits[0].hp)
        assertEquals("다른 카드는 그대로", state.enemyUnits.drop(1).sumOf { it.hp }, after.enemyUnits.drop(1).sumOf { it.hp })
        assertEquals("줄어든 HP = 그 카드 HP", state.enemyHp - after.enemyHp, targetCard.maxHp)
        assertTrue("표적이 다음 카드로 넘어간다", after.enemyTarget!!.hp > 0)
        assertTrue("전체 HP 도 줄어든다", after.enemyHp < state.enemyHp)
    }

    @Test
    fun `적 카드를 모두 쓰러뜨려야만 승리한다`() {
        val vm = TcgGameViewModel(random = Random(5))
        vm.startFieldBattle()
        var guard = 0
        while (vm.uiState.value.enemyHp > 0 && guard < 200) {
            vm.attackFront()
            if (vm.uiState.value.enemyHp > 0) vm.nextTurn()
            guard += 1
        }
        val state = vm.uiState.value
        assertEquals("적 전멸", 0, state.enemyAliveCount)
        assertEquals("HP 합 0", 0, state.enemyHp)
        assertTrue("승리 처리", state.battleVictories > 0)
    }

    @Test
    fun `턴 진행에서 적 카드 여러 장이 각각 아군을 때린다`() {
        val vm = TcgGameViewModel(random = Random(9))
        vm.startFieldBattle()
        val before = vm.uiState.value
        val frontTotal = before.enemyAttackers.sumOf { it.atk }
        vm.nextTurn()
        val after = vm.uiState.value

        assertTrue("적 반격 로그가 카드별로 남는다", after.battleLog.count { it.contains("의 반격") } >= 3)
        assertTrue("반격 1회 피해가 카드 1장 공격력 이하", after.lastAllyHitDamage <= frontTotal)
        assertTrue("턴이 넘어갔다", after.turn == before.turn + 1)
    }

    @Test
    fun `1~4스테이지 권장 전투력이 초반 덱보다 높다`() {
        // 2026-11-18 개편 전: 18,000~22,000 → 지금: 34,000 이상
        (1..4).forEach { number ->
            val stage = StageCatalog.require(number)
            assertTrue(
                "${number}스테이지 권장 전투력 ${stage.recommendedPower} 가 30,000 이상",
                stage.recommendedPower >= 30000
            )
        }
        // 시작 덱(코스트 3~4, 3장)으로 4스테이지부터는 대부분 밀린다
        val starterIds = StartingRoster.draw(Random(7))
        val starter = starterIds.map { CardCatalog.require(it).toCard() }
        val stage4 = BattleSimulator.simulate(starter, StageCatalog.require(4))
        val stage1 = BattleSimulator.simulate(starter, StageCatalog.require(1))
        assertTrue("1스테이지는 이긴다 (${stage1.summary})", stage1.victory)
        assertTrue("4스테이지는 1스테이지보다 훨씬 어렵다", stage4.turns >= stage1.turns || !stage4.victory)
    }

    @Test
    fun `던전 시뮬레이터도 다중 적 규칙으로 돈다`() {
        val starter = StartingRoster.draw(Random(11)).map { CardCatalog.require(it).toCard() }
        val result = BattleSimulator.simulate(starter, StageCatalog.require(1))

        assertEquals("적 진영 9장", 9, result.enemyUnitsTotal)
        assertTrue("결과 요약에 적 잔존이 표시된다", result.summary.contains("적 "))
        assertNotEquals("적 카드는 남거나 전멸한다", -1, result.enemyUnitsLeft)
        if (!result.victory) assertTrue("패배면 적이 남아 있다", result.enemyUnitsLeft > 0)
    }

    @Test
    fun `오의는 초과 피해가 다음 적 카드로 이어진다`() {
        val squad = EnemySquad.buildFromTotals(
            totalHp = 9000, perTurnAtk = 200, enemyName = "시험용",
            enemyElement = Element.DARK, size = 9
        )
        val frontHp = squad.first().maxHp
        val hit = EnemySquad.applyDamageSequential(squad, frontHp + frontHp / 2)
        assertEquals("첫 카드 격파", 0, hit[0].hp)
        assertTrue("두 번째 카드도 피해", hit[1].hp < squad[1].hp)
        assertEquals("총 HP 감소 = 준 피해", 9000 - (frontHp + frontHp / 2), EnemySquad.totalHp(hit))
    }
}
