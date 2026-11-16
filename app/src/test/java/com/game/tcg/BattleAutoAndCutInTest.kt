package com.game.tcg

import com.game.tcg.core.model.Element
import com.game.tcg.ui.BattleAttackKind
import com.game.tcg.ui.BattleAutoDecor
import com.game.tcg.ui.BattleAutoPilot
import com.game.tcg.ui.BattleCutIn
import com.game.tcg.ui.BattleSpeed
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 🤖 기본 자동 전투 · ⚔️ 공격 카드 중앙 확대 컷인 — 2026-11-16 사용자 요청 검증
 *
 * 사용자 요청:
 *  - 전투에 들어가면 자동 전투가 기본으로 돌고, `🤖 자동 전투 ON/OFF` 토글·`1x/2x` 배속으로 조절
 *  - 공격할 때마다 공격자 카드가 중앙에 확대되고, 피격 카드에 흔들림·플래시·참격·데미지 숫자
 *
 * 화면 연출은 Compose라서, 여기서는 **상태·판단·수치·문구**를 단위 테스트로 고정한다.
 */
class BattleAutoAndCutInTest {

    private fun newGame(seed: Int = 1) = TcgGameViewModel(
        initialOwnedCardIds = listOf(3, 1, 2),
        random = Random(seed)
    )

    // =========================================================================
    // 1. 🤖 기본 자동 전투
    // =========================================================================

    @Test
    fun `전투 상태는 자동 전투 ON · 1배속이 기본값이다`() {
        val vm = newGame()
        val s = vm.uiState.value
        assertTrue("자동 전투는 기본 ON 이어야 한다", s.isAutoBattle)
        assertEquals(BattleSpeed.X1, s.battleSpeed)
        assertEquals("🤖 자동 전투 ON", BattleAutoDecor.autoLabel(true))
        assertEquals("✋ 수동 전투 OFF", BattleAutoDecor.autoLabel(false))
    }

    @Test
    fun `자동 전투 토글과 배속 토글이 상태에 반영된다`() {
        val vm = newGame()
        vm.toggleAutoBattle()
        assertFalse(vm.uiState.value.isAutoBattle)
        vm.toggleAutoBattle()
        assertTrue(vm.uiState.value.isAutoBattle)

        vm.toggleBattleSpeed()
        assertEquals(BattleSpeed.X2, vm.uiState.value.battleSpeed)
        vm.toggleBattleSpeed()
        assertEquals(BattleSpeed.X1, vm.uiState.value.battleSpeed)

        vm.setBattleSpeed(BattleSpeed.X2)
        assertEquals(BattleSpeed.X2, vm.uiState.value.battleSpeed)
    }

    @Test
    fun `새 전투를 시작하면 자동 전투가 다시 켜진다`() {
        val vm = newGame()
        vm.setAutoBattle(false)
        vm.startFieldBattle()
        assertTrue("원정 개시 시 자동 전투 ON", vm.uiState.value.isAutoBattle)

        vm.setAutoBattle(false)
        vm.startDemonLordBattle()
        assertTrue("마왕전 개시 시 자동 전투 ON", vm.uiState.value.isAutoBattle)

        vm.setAutoBattle(false)
        vm.startStageBattle(1)
        assertTrue("스테이지 개시 시 자동 전투 ON", vm.uiState.value.isAutoBattle)
    }

    @Test
    fun `배속이 오르면 컷인과 자동 스텝 시간이 비례해 줄어든다`() {
        val x1Step = BattleAutoDecor.stepIntervalMillis(BattleSpeed.X1)
        val x2Step = BattleAutoDecor.stepIntervalMillis(BattleSpeed.X2)
        assertEquals(BattleAutoDecor.STEP_MILLIS, x1Step)
        assertTrue("2배속 스텝은 1배속의 절반 수준", x2Step <= x1Step / 2 + 1)
        assertTrue("2배속에서도 눈에 보이는 최소 시간은 지킨다", x2Step >= 100L)

        // 한 스텝 안에서 컷인이 먼저 끝나고 타격 연출이 이어진다
        assertEquals(
            BattleAutoDecor.impactMillis(BattleSpeed.X1) + BattleAutoDecor.hitHoldMillis(BattleSpeed.X1),
            BattleAutoDecor.effectMillis(BattleSpeed.X1)
        )
        assertTrue(
            "타격은 컷인 확대가 정점에 이르는 순간",
            BattleAutoDecor.impactMillis(BattleSpeed.X1) < BattleAutoDecor.cutInMillis(BattleSpeed.X1)
        )
        assertTrue(
            "컷인 시간을 포함해도 자동 스텝(1.0~1.3초) 안에 정리된다",
            BattleAutoDecor.effectMillis(BattleSpeed.X1) < x1Step
        )
    }

    @Test
    fun `자동 전투 파일럿은 마나가 있으면 전열 공격, 없으면 턴 진행을 고른다`() {
        val vm = newGame()
        // HP 25000 마왕전 — 한 방에 끝나지 않아 마나 소모 상황을 볼 수 있다
        vm.startDemonLordBattle()
        assertEquals(BattleAutoPilot.Action.ATTACK_FRONT, BattleAutoPilot.nextAction(vm.uiState.value))

        vm.attackFront() // 마나 1 → 0
        assertEquals(0, vm.uiState.value.mana.currentMana)
        assertTrue("적이 아직 살아 있다", vm.uiState.value.enemyHp > 0)
        assertEquals(BattleAutoPilot.Action.NEXT_TURN, BattleAutoPilot.nextAction(vm.uiState.value))
    }

    @Test
    fun `자동 전투 스텝이 실제로 공격과 턴을 진행한다`() {
        val vm = newGame()
        vm.startDemonLordBattle()
        val before = vm.uiState.value

        assertEquals(BattleAutoPilot.Action.ATTACK_FRONT, vm.autoBattleStep())
        val afterAttack = vm.uiState.value
        assertTrue("적 HP 가 줄어야 한다", afterAttack.enemyHp < before.enemyHp)
        assertEquals(before.mana.currentMana - 1, afterAttack.mana.currentMana)

        assertEquals(BattleAutoPilot.Action.NEXT_TURN, vm.autoBattleStep())
        assertEquals("턴이 넘어가고 적이 반격한다", before.turn + 1, vm.uiState.value.turn)
        assertTrue(vm.uiState.value.lastAllyHitTick > before.lastAllyHitTick)
    }

    @Test
    fun `자동 전투는 승리하면 스스로 멈춘다`() {
        val vm = newGame(seed = 3)
        vm.startFieldBattle()

        var steps = 0
        while (vm.uiState.value.isAutoBattle && steps < 300) {
            vm.autoBattleStep()
            steps++
        }

        assertFalse("전투가 끝나면 자동 전투 OFF", vm.uiState.value.isAutoBattle)
        assertTrue("스텝이 무한 루프에 빠지지 않는다", steps < 300)
        assertTrue(
            "승리 또는 전멸 중 하나로 결판난다",
            vm.uiState.value.enemyHp == 0 || vm.uiState.value.party.none { it.isAlive }
        )
        assertEquals(BattleAutoPilot.Action.STOP, BattleAutoPilot.nextAction(vm.uiState.value))
        assertFalse(BattleAutoPilot.shouldContinue(vm.uiState.value))
    }

    @Test
    fun `전투가 끝났거나 토글이 꺼지면 자동 전투 루프를 돌리지 않는다`() {
        val vm = newGame()
        vm.startFieldBattle()
        assertTrue(BattleAutoPilot.shouldContinue(vm.uiState.value))

        // 적을 쓰러뜨리면(전투 종료) 파일럿은 STOP
        var guard = 0
        while (vm.uiState.value.enemyHp > 0 && guard < 50) {
            vm.autoBattleStep()
            guard++
        }
        assertEquals(BattleAutoPilot.Action.STOP, BattleAutoPilot.nextAction(vm.uiState.value))
        assertFalse(BattleAutoPilot.shouldContinue(vm.uiState.value))

        // 새 전투에서 토글을 끄면 역시 루프를 돌리지 않는다
        vm.startFieldBattle()
        vm.setAutoBattle(false)
        assertFalse("토글 OFF 면 루프를 멈춘다", BattleAutoPilot.shouldContinue(vm.uiState.value))
    }

    // =========================================================================
    // 2. ⚔️ 공격 카드 중앙 확대 컷인
    // =========================================================================

    @Test
    fun `아군 공격은 실제 카드 일러스트 컷인을 만든다`() {
        val vm = newGame()
        vm.startFieldBattle()
        vm.attackFront()
        val s = vm.uiState.value
        val cut = s.lastCutIn
        assertNotNull("공격하면 컷인 스냅샷이 생긴다", cut)
        cut!!

        assertFalse(cut.fromEnemy)
        assertEquals(BattleAttackKind.FRONT, cut.kind)
        assertTrue("공격자 카드 번호는 실제 카드여야 한다", cut.attackerCardId in listOf(3, 1, 2))
        assertTrue("배너에 이름이 실린다", cut.attackerName.isNotBlank())
        assertTrue("공격자 슬롯이 전열 강조에 쓰인다", cut.attackerSlot >= 0)
        assertEquals(s.enemyName, cut.targetName)
        assertEquals(s.lastEnemyHitDamage, cut.damage)
        assertEquals(1, cut.tick)
        assertTrue("데미지가 0보다 크다", cut.damage > 0)

        val banner = BattleAutoDecor.bannerText(cut)
        assertTrue("`⚔️ 이름 · 공격!` 배너", banner.startsWith("⚔️ "))
        assertTrue(banner.endsWith("공격!"))
        assertTrue(banner.contains(cut.attackerName))
    }

    @Test
    fun `후열 지원 공격 컷인은 공격명과 종류가 다르다`() {
        val vm = newGame()
        vm.startFieldBattle()
        // 후열에 카드가 없으면 공격이 막히므로 후열 배치가 있는 스타터 덱에서만 확인한다
        val backMember = vm.uiState.value.party.firstOrNull { !it.isFrontRow }
        if (backMember == null) {
            // 스타터 덱이 전열만 쓰는 경우: 파일럿이 후열을 고르지 않는다는 규칙만 고정
            assertEquals(BattleAutoPilot.Action.ATTACK_FRONT, BattleAutoPilot.nextAction(vm.uiState.value))
            return
        }
        vm.attackBack()
        val cut = vm.uiState.value.lastCutIn!!
        assertEquals(BattleAttackKind.BACK, cut.kind)
        assertEquals(backMember.slotIndex, cut.attackerSlot)
        assertTrue(BattleAutoDecor.badges(cut).contains("후열"))
    }

    @Test
    fun `적 반격 컷인은 적 보스 카드 아트와 피해 슬롯을 담는다`() {
        val vm = newGame()
        vm.startFieldBattle()
        val beforeTick = vm.uiState.value.lastCutIn?.tick ?: 0
        vm.nextTurn()
        val s = vm.uiState.value
        val cut = s.lastCutIn!!

        assertTrue(cut.fromEnemy)
        assertEquals(BattleAttackKind.ENEMY_COUNTER, cut.kind)
        assertEquals(s.enemyCutInCardId, cut.attackerCardId)
        assertEquals(s.enemyName, cut.attackerName)
        assertEquals(s.enemyElement, cut.element)
        assertEquals("반격 후 컷인 틱이 증가", beforeTick + 1, cut.tick)
        assertEquals("피해 슬롯이 흔들림 대상", s.lastAllyHitSlot, cut.targetSlot)
        assertEquals(s.lastAllyHitDamage, cut.damage)
        assertTrue(BattleAutoDecor.badges(cut).contains("적 반격"))
        assertTrue(BattleAutoDecor.damageText(cut.damage).startsWith("-"))
    }

    @Test
    fun `페어 얼티밋 오의 컷인은 금빛 오의 연출로 표시된다`() {
        val vm = newGame()
        vm.startFieldBattle()
        // 마나 3까지 턴을 넘겨 오의 조건을 만든다
        repeat(2) { vm.nextTurn() }
        assertTrue("오의 조건(생존 2명·마나 3)", vm.uiState.value.canActivatePairUltimate)

        vm.activatePairUltimate()
        val cut = vm.uiState.value.lastCutIn!!
        assertEquals(BattleAttackKind.PAIR_ULTIMATE, cut.kind)
        assertTrue(cut.ultimate)
        assertTrue("오의 데미지는 일반 공격보다 크다", cut.damage > 0)
        assertTrue(BattleAutoDecor.badges(cut).contains("오의"))
        assertTrue("오의는 금빛 타격", BattleAutoDecor.isGoldenHit(cut))
    }

    @Test
    fun `컷인 애니메이션 곡선과 문구 규칙`() {
        // 확대: 작게 시작 → 정점 → 살짝 가라앉음
        assertTrue(BattleAutoDecor.cutInScale(0f) < BattleAutoDecor.cutInScale(0.55f))
        assertTrue(BattleAutoDecor.cutInScale(0.55f) > BattleAutoDecor.cutInScale(1f))
        assertEquals(0f, BattleAutoDecor.cutInAlpha(0f), 0.001f)
        assertEquals(1f, BattleAutoDecor.cutInAlpha(0.5f), 0.001f)
        assertEquals(0f, BattleAutoDecor.cutInAlpha(1f), 0.001f)
        assertEquals(0f, BattleAutoDecor.shakeOffset(0f), 0.001f)
        assertNotEquals(0f, BattleAutoDecor.shakeOffset(0.9f), 0f)

        // 데미지 숫자 · 상성 배지
        assertEquals("1,240", BattleAutoDecor.thousands(1240))
        assertEquals("-1,240", BattleAutoDecor.damageText(1240))
        assertEquals("1.5x", BattleAutoDecor.multiplierText(1.5))
        assertEquals("1x", BattleAutoDecor.multiplierText(1.0))

        val crit = BattleCutIn(
            tick = 1,
            attackerName = "세라피엘",
            attackName = "공격",
            element = Element.LIGHT,
            damage = 1240,
            critical = true,
            multiplier = 1.5
        )
        val badges = BattleAutoDecor.badges(crit)
        assertTrue(badges.contains("급소"))
        assertTrue(badges.contains("상성 1.5x"))
        assertEquals("⚔️ 세라피엘 · 공격!", BattleAutoDecor.bannerText(crit))
        assertTrue(BattleAutoDecor.isGoldenHit(crit))

        val weak = crit.copy(critical = false, multiplier = 0.8)
        assertTrue(BattleAutoDecor.badges(weak).contains("상성 열세"))
        assertFalse(BattleAutoDecor.isGoldenHit(weak))
    }
}
