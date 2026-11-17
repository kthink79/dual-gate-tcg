package com.game.tcg

import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.ExplorationEngine
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationRewardType
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.engine.SummonEngine
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.VisualState
import com.game.tcg.persistence.GameSave
import com.game.tcg.persistence.InMemorySaveStore
import com.game.tcg.persistence.SavedCard
import com.game.tcg.persistence.SavedPartyMember
import com.game.tcg.persistence.SaveCodec
import com.game.tcg.ui.TcgGameViewModel
import com.game.tcg.ui.toGameSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 친밀도(호감도) 개편 — 2026-11-16 사용자 요청 검증.
 *
 * 1) 새 캐릭터는 **친밀도 0**에서 시작한다 (170명 전원 · 예전의 일괄 50 제거)
 * 2) **일상 사복 30 · 데이트 룩 70 · 페어 오의 100** 해금 단계
 * 3) **현실(일상) 소환은 일상 사복이 열린 캐릭터만** — 미해금은 잠금 표시·진행도
 * 4) 친밀도 상승은 **전투 출전 중심**(출전 +4 · 승리 +6)이고,
 *    선물·대화·보유 기간은 +1~+2 의 작은 보조 경로
 * 5) 출전 횟수·친밀도·보유 기간 정산일이 **저장·복원**된다
 */
class AffinityProgressionTest {

    private fun starterGame(bond: Int = 0, store: InMemorySaveStore? = null, today: Int = 20_000) =
        TcgGameViewModel(
            random = Random(11),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 5000,
            initialShards = ShardWallet(),
            initialPackTickets = 1,
            initialBondLevel = bond,
            todayEpochDay = { today },
            saveStore = store
        )

    // ──────────────────────────────────────────────────────────────
    // 1. 시작 친밀도 0
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `170명 전원의 합류 친밀도는 0이다`() {
        assertEquals(0, CardCatalog.ROSTER_BOND_SEED)
        assertTrue("모든 기록이 0에서 시작", CardCatalog.ALL.all { it.bondSeed == 0 })

        val vm = starterGame()
        assertTrue("시작 3장 모두 0", vm.uiState.value.ownedCards.all { it.bondLevel == 0 })
        assertEquals(0, vm.uiState.value.lobbyHeroBond)
        assertEquals(0, TcgGameViewModel.characterCard(112).bondLevel)
    }

    // ──────────────────────────────────────────────────────────────
    // 2. 해금 단계 (30 / 70 / 100)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `일상 30 데이트 70 오의 100 단계가 정확히 동작한다`() {
        assertEquals(0, AffinityRules.CASUAL_UNLOCK_BOND - 30)
        assertEquals(0, AffinityRules.DATE_UNLOCK_BOND - 70)
        assertEquals(0, AffinityRules.ULTIMATE_UNLOCK_BOND - 100)

        assertFalse(AffinityRules.isCasualUnlocked(29))
        assertTrue(AffinityRules.isCasualUnlocked(30))
        assertFalse(AffinityRules.isDateUnlocked(69))
        assertTrue(AffinityRules.isDateUnlocked(70))
        assertFalse(AffinityRules.isUltimateUnlocked(99))
        assertTrue(AffinityRules.isUltimateUnlocked(100))

        assertEquals(VisualState.BATTLE, AffinityRules.visualFor(0))
        assertEquals(VisualState.BATTLE, AffinityRules.visualFor(29))
        assertEquals(VisualState.CASUAL_CUTE, AffinityRules.visualFor(30))
        assertEquals(VisualState.CASUAL_CUTE, AffinityRules.visualFor(69))
        assertEquals(VisualState.ROMANTIC_DATE, AffinityRules.visualFor(70))
        assertEquals(VisualState.ROMANTIC_DATE, AffinityRules.visualFor(100))

        // 미니어처(SD)는 친밀도와 무관하게 처음부터 열려 있다 (2026-11-17)
        assertEquals(listOf(VisualState.BATTLE, VisualState.MINIATURE), AffinityRules.unlockedVisuals(0))
        assertEquals(
            listOf(VisualState.BATTLE, VisualState.CASUAL_CUTE, VisualState.MINIATURE),
            AffinityRules.unlockedVisuals(30)
        )
        assertEquals(VisualState.entries.toList(), AffinityRules.unlockedVisuals(100))

        // 해금까지 남은 양 · 진행도
        assertEquals(30, AffinityRules.nextUnlockNeed(0))
        assertEquals(40, AffinityRules.nextUnlockNeed(30))
        assertEquals(30, AffinityRules.nextUnlockNeed(70))
        assertNull(AffinityRules.nextUnlockNeed(100))
        assertEquals(0.5f, AffinityRules.casualProgress(15), 0.001f)
        assertEquals(1f, AffinityRules.casualProgress(80), 0.001f)

        // 잠금 문구에 필요한 친밀도가 들어간다
        val lock = AffinityRules.lockLabel(VisualState.CASUAL_CUTE, 12)
        assertNotNull(lock)
        assertTrue(lock!!.contains("30"))
        assertTrue(lock.contains("12"))
        assertNull(AffinityRules.lockLabel(VisualState.CASUAL_CUTE, 30))
    }

    @Test
    fun `친밀도 상승은 100에서 멈추고 비주얼은 내려가지 않는다`() {
        val card = TcgGameViewModel.characterCard(3).copy(bondLevel = 95)
        card.currentVisual = VisualState.ROMANTIC_DATE

        val change = AffinityRules.applyGain(card, 20)

        assertEquals(95, change.before)
        assertEquals(100, change.after)
        assertEquals(5, change.gained)
        assertTrue(change.isMax)
        assertEquals(VisualState.ROMANTIC_DATE, card.currentVisual)

        val again = AffinityRules.applyGain(card, 10)
        assertEquals(0, again.gained)
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 전투 출전 중심 상승 (+4 출전 / +6 승리)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `전투 출전 1회에 출전 카드 전원이 4 오르고 승리하면 총 6이 된다`() {
        val vm = starterGame()
        val before = vm.uiState.value.ownedCards.associate { it.id to it.bondLevel }
        val partyIds = vm.uiState.value.party.map { it.card.id }

        assertTrue(vm.startStageBattle(1))

        val afterDeploy = vm.uiState.value
        partyIds.forEach { id ->
            val card = afterDeploy.ownedCards.first { it.id == id }
            assertEquals("출전 $id", (before[id] ?: 0) + AffinityRules.BATTLE_DEPLOY_GAIN, card.bondLevel)
            assertEquals("출전 횟수 $id", 1, card.battleCount)
        }
        assertTrue(afterDeploy.battleLog.any { it.contains("[출전]") })

        // 승리 정산 — 승리 보너스 +2 가 더해져 한 전투 +6
        fightToVictory(vm)

        val afterWin = vm.uiState.value
        partyIds.forEach { id ->
            val card = afterWin.ownedCards.first { it.id == id }
            assertEquals(
                "출전 4 + 승리 2 = 6 ($id)",
                (before[id] ?: 0) + AffinityRules.BATTLE_WIN_GAIN,
                card.bondLevel
            )
            if (card.id in afterWin.party.map { it.card.id }) {
                assertEquals("한 전투에서 출전 횟수는 1회만", 1, card.battleCount)
            }
        }
        assertTrue(afterWin.battleLog.any { it.contains("[출전 승리]") })
    }

    @Test
    fun `후퇴나 패배로 끝난 전투도 출전 4만 오른다`() {
        val vm = starterGame()
        val target = vm.uiState.value.party.first()
        val before = target.card.bondLevel

        vm.startFieldBattle()
        vm.emergencyRetreat()

        val card = vm.uiState.value.ownedCards.first { it.id == target.card.id }
        assertEquals(before + AffinityRules.BATTLE_DEPLOY_GAIN, card.bondLevel)
        assertEquals(1, card.battleCount)
    }

    @Test
    fun `전투 6회를 이기면 일상 사복과 데이트 룩이 순서대로 열린다`() {
        val vm = starterGame()
        val heroId = vm.uiState.value.party.first().card.id

        // 출전 1회 = +6 → 5승이면 30 도달 (일상 사복 · 현실 소환 개방)
        repeat(5) { battle(vm) }
        val atCasual = vm.uiState.value.ownedCards.first { it.id == heroId }
        assertEquals(30, atCasual.bondLevel)
        assertEquals(VisualState.CASUAL_CUTE, atCasual.currentVisual)
        assertTrue(AffinityRules.canRealitySummon(atCasual.bondLevel))

        // 12승 = 72 → 데이트 룩
        repeat(7) { battle(vm) }
        val atDate = vm.uiState.value.ownedCards.first { it.id == heroId }
        assertEquals(72, atDate.bondLevel)
        assertEquals(VisualState.ROMANTIC_DATE, atDate.currentVisual)
    }

    // ──────────────────────────────────────────────────────────────
    // 4. 보조 경로는 작은 폭 (+1 ~ +2)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `대화 산책 데이트는 1 1 2 로만 오르고 전투보다 작다`() {
        val vm = starterGame()
        vm.selectSummonCard(3)

        vm.interactRealWorld(RealWorldActivity.DAILY_GREETING)
        assertEquals(AffinityRules.DAILY_TALK_GAIN, vm.uiState.value.partnerBond)
        vm.interactRealWorld(RealWorldActivity.CASUAL_CAFE_WALK)
        assertEquals(
            AffinityRules.DAILY_TALK_GAIN + AffinityRules.WALK_GAIN,
            vm.uiState.value.partnerBond
        )
        vm.interactRealWorld(RealWorldActivity.ROMANTIC_EVENING_DATE)
        assertEquals(
            AffinityRules.DAILY_TALK_GAIN + AffinityRules.WALK_GAIN + AffinityRules.DATE_GAIN,
            vm.uiState.value.partnerBond
        )

        assertTrue(
            "교감 한 번은 전투 출전보다 작아야 한다",
            AffinityRules.DATE_GAIN < AffinityRules.BATTLE_DEPLOY_GAIN
        )
        assertTrue(AffinityRules.COMPANION_ENCOUNTER_GAIN <= AffinityRules.DATE_GAIN)
        assertTrue(AffinityRules.HOLDING_GAIN_PER_DAY < AffinityRules.BATTLE_DEPLOY_GAIN)
    }

    @Test
    fun `탐색 동료 조우는 선물 수준으로만 오른다`() {
        val vm = TcgGameViewModel(
            random = Random(5),
            explorationEngine = ExplorationEngine { 64 },   // 64 → 인연 조우 분기
            initialOwnedCardIds = CardCatalog.ALL.map { it.id }
        )
        val targetId = CardCatalog.ALL[64].id
        val before = vm.uiState.value.ownedCards.first { it.id == targetId }.bondLevel

        val result = vm.explore(ExplorationMode.ENCOUNTER)

        assertNotNull(result)
        val gain = result!!.rewards.first { it.type == ExplorationRewardType.COMPANION_BOND }.amount
        assertTrue("조우 상승은 1~2", gain in 1..2)
        assertEquals(before + gain, vm.uiState.value.ownedCards.first { it.id == targetId }.bondLevel)
    }

    @Test
    fun `보유 기간은 하루 1씩만 오르고 한 번에 최대 10까지다`() {
        val card = TcgGameViewModel.characterCard(3).copy(bondLevel = 0)

        assertEquals(0, AffinityRules.creditHolding(card, 0).gained)
        assertEquals(3, AffinityRules.creditHolding(card, 3).gained)
        assertEquals(3, card.bondLevel)

        val capped = AffinityRules.creditHolding(card, 40)
        assertEquals(10, capped.gained)
        assertEquals(13, card.bondLevel)
    }

    @Test
    fun `앱을 다시 켜면 지난 날짜만큼 보유 기간 친밀도가 정산된다`() {
        val today = 20_000
        val store = InMemorySaveStore()
        val save = GameSave(
            gold = 100,
            cards = listOf(
                SavedCard(
                    cardId = 3, atk = 7600, def = 6800,
                    bondLevel = 10, lastBondDay = today - 4
                )
            ),
            party = listOf(SavedPartyMember(cardId = 3, slotIndex = 0, hp = 5200, maxHp = 5200))
        )
        store.write(SaveCodec.encode(save))

        val vm = TcgGameViewModel(
            random = Random(3),
            initialOwnedCardIds = listOf(3, 1, 2),
            todayEpochDay = { today },
            saveStore = store
        )

        val card = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals("4일 보유 → +4", 14, card.bondLevel)
        assertEquals(today, card.lastBondDay)
        assertTrue(vm.uiState.value.realWorldLog.any { it.contains("보유 4일") })
    }

    // ──────────────────────────────────────────────────────────────
    // 5. 현실(일상) 소환 잠금
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `친밀도 30 미만은 현실 소환이 잠기고 안내와 진행도가 남는다`() {
        val vm = starterGame(bond = 12)
        vm.selectSummonCard(3)

        val result = vm.summonToReality(3)

        assertNotNull(result)
        assertFalse("친밀도 30 미만은 소환 불가", result!!.isSuccess)
        assertTrue(result.failureReason!!.contains("일상 사복"))
        val state = vm.uiState.value
        assertTrue(state.summonMessage!!.contains("일상 사복"))
        assertEquals("에너지는 쓰지 않는다", 10, state.gateEnergy)
        assertEquals(12, state.ownedCards.first { it.id == 3 }.bondLevel)
        assertFalse(AffinityRules.canRealitySummon(12))
        assertTrue(AffinityRules.casualProgress(12) in 0.3f..0.45f)
    }

    @Test
    fun `친밀도 30 이상이면 현실 소환이 되고 보조 폭으로 오른다`() {
        val vm = starterGame(bond = AffinityRules.CASUAL_UNLOCK_BOND)
        vm.selectSummonCard(3)

        val result = vm.summonToReality(3)

        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        val state = vm.uiState.value
        assertEquals(10 - SummonEngine.ENERGY_COST, state.gateEnergy)
        assertEquals(
            AffinityRules.CASUAL_UNLOCK_BOND + SummonEngine.BOND_GAIN,
            state.ownedCards.first { it.id == 3 }.bondLevel
        )
        assertEquals(1, state.summonCount)
    }

    @Test
    fun `도감에서도 잠긴 일상 데이트 비주얼은 잠금 안내가 보인다`() {
        val vm = starterGame()
        vm.selectCard(3)

        vm.selectVisual(VisualState.CASUAL_CUTE)
        assertEquals(VisualState.BATTLE, vm.uiState.value.selectedVisual)
        assertTrue(vm.uiState.value.visualLockMessage!!.contains("30"))

        val opened = starterGame(bond = AffinityRules.DATE_UNLOCK_BOND)
        opened.selectCard(3)
        opened.selectVisual(VisualState.CASUAL_CUTE)
        assertEquals(VisualState.CASUAL_CUTE, opened.uiState.value.selectedVisual)
        opened.selectVisual(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.ROMANTIC_DATE, opened.uiState.value.selectedVisual)
        assertNull(opened.uiState.value.visualLockMessage)
    }

    // ──────────────────────────────────────────────────────────────
    // 6. 저장 · 복원
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `친밀도와 출전 횟수가 저장되고 다시 켜도 그대로다`() {
        val store = InMemorySaveStore()
        val first = starterGame(store = store)
        val heroId = first.uiState.value.party.first().card.id

        assertTrue(first.startStageBattle(1))
        fightToVictory(first)
        val saved = first.uiState.value.ownedCards.first { it.id == heroId }
        assertTrue(saved.bondLevel > 0)
        assertEquals(1, saved.battleCount)

        // 앱 재실행 = 같은 저장소를 넘긴 새 ViewModel
        val second = starterGame(store = store)
        val restored = second.uiState.value.ownedCards.first { it.id == heroId }

        assertEquals("친밀도 복원", saved.bondLevel, restored.bondLevel)
        assertEquals("출전 횟수 복원", saved.battleCount, restored.battleCount)
        assertEquals(
            "복원한 저장도 원본과 같다",
            SaveCodec.encode(first.uiState.value.toGameSave()),
            SaveCodec.encode(second.uiState.value.toGameSave())
        )
    }

    @Test
    fun `출전 횟수와 정산일은 카드 행 끝에 붙어 구버전 저장도 읽힌다`() {
        val save = GameSave(
            cards = listOf(
                SavedCard(
                    cardId = 7, atk = 6400, def = 3900,
                    bondLevel = 42, battleCount = 9, lastBondDay = 19_999
                )
            )
        )
        val decoded = SaveCodec.decode(SaveCodec.encode(save))
        assertNotNull(decoded)
        assertEquals(9, decoded!!.cards.first().battleCount)
        assertEquals(19_999, decoded.cards.first().lastBondDay)

        // 예전 13필드 형식(추가 필드 없음)도 그대로 읽힌다
        val legacy = """
            DUALGATE-SAVE
            version=1
            gold=100
            card=7|6400|3900|1|0|42|0|0|0|0|0|0|BATTLE
        """.trimIndent()
        val old = SaveCodec.decode(legacy)
        assertNotNull(old)
        assertEquals(42, old!!.cards.first().bondLevel)
        assertEquals("구버전은 출전 0회", 0, old.cards.first().battleCount)
        assertEquals(0, old.cards.first().lastBondDay)
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────

    /** 스테이지 1을 승리까지 진행한다 */
    private fun battle(vm: TcgGameViewModel) {
        assertTrue(vm.startStageBattle(1))
        fightToVictory(vm)
    }

    private fun fightToVictory(vm: TcgGameViewModel) {
        var guard = 0
        while (vm.uiState.value.enemyHp > 0 && guard < 200) {
            guard += 1
            if (vm.uiState.value.aliveSurvivorCount <= 0) {
                vm.reclaimAllLostCards()
                vm.startStageBattle(1)
            }
            if (vm.uiState.value.mana.currentMana > 0 && vm.uiState.value.aliveSurvivorCount > 0) {
                vm.attackFront()
            } else {
                vm.nextTurn()
            }
        }
        assertTrue("전투가 끝나지 않았다 (guard $guard)", vm.uiState.value.enemyHp <= 0)
    }
}
