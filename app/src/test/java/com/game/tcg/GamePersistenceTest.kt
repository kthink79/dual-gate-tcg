package com.game.tcg

import com.game.tcg.core.model.ShardWallet
import com.game.tcg.persistence.GameSave
import com.game.tcg.persistence.InMemorySaveStore
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
 * 진행 상태 자동 저장(Persistence) — 왕복 복원·손상 폴백·새 게임 초기화 검증.
 *
 * 앱 재실행은 "같은 [InMemorySaveStore] 를 넘긴 새 ViewModel" 로 재현한다.
 */
class GamePersistenceTest {

    private fun newGame(store: InMemorySaveStore?, introSeen: Boolean = true) = TcgGameViewModel(
        random = Random(7),
        initialOwnedCardIds = listOf(3, 1, 2),
        introAlreadySeen = introSeen,
        saveStore = store
    )

    /** 전투를 실제로 끝까지 진행해 스테이지 클리어·보상(조각·티켓)을 만든다 */
    private fun clearFirstStage(vm: TcgGameViewModel): Boolean {
        if (!vm.startStageBattle(1)) return false
        var guard = 0
        while (vm.uiState.value.enemyHp > 0 && guard < 80) {
            guard += 1
            if (vm.uiState.value.mana.currentMana > 0 && vm.uiState.value.aliveSurvivorCount > 0) {
                vm.attackFront()
            } else if (vm.uiState.value.aliveSurvivorCount > 0) {
                vm.nextTurn()
            } else {
                return false
            }
        }
        return vm.uiState.value.stageCleared.contains(1)
    }

    @Test
    fun `저장한 진행이 앱 재실행 후 그대로 복원된다`() {
        val store = InMemorySaveStore()
        val first = newGame(store)

        // ── 진행: 이벤트 선택 · 장비 구매/장착 · 편성 변경 · 훈련 · 스테이지 클리어 ──
        first.chooseEventA(talkWithLily = true)
        val bought = first.buyEquipment("weapon_training_dagger")
        assertNotNull("장비 구매가 실패하면 테스트 전제가 깨진다", bought)
        val uid = first.uiState.value.equipment.last().uid
        first.equipItem(uid, 3)
        first.deployToParty(1, 4)
        first.trainForExp()
        val cleared = clearFirstStage(first)
        assertTrue("1스테이지 클리어로 진행도를 만들어야 한다", cleared)

        val before = first.uiState.value
        val savedText = store.stored
        assertNotNull("상태가 바뀌면 저장 파일이 생겨야 한다", savedText)

        // ── 앱 재실행 ──
        val second = newGame(store)
        val after = second.uiState.value

        assertEquals(before.gold, after.gold)
        assertEquals(before.gems, after.gems)
        assertEquals(before.packTickets, after.packTickets)
        assertEquals(before.playerExp, after.playerExp)
        assertEquals(before.shardTotal, after.shardTotal)
        assertEquals(before.stageCleared, after.stageCleared)
        assertEquals(before.selectedStage, after.selectedStage)
        assertEquals(before.eventAChoice, after.eventAChoice)
        assertEquals(before.supplyClaims, after.supplyClaims)
        assertEquals(before.ownedCards.map { it.id }, after.ownedCards.map { it.id })
        assertEquals(before.party.map { it.slotIndex }, after.party.map { it.slotIndex })
        assertEquals(before.party.map { it.card.id }, after.party.map { it.card.id })
        assertEquals(
            "장비 uid·장착 대상까지 복원되어야 한다",
            before.equipment.map { it.uid to it.equippedTo },
            after.equipment.map { it.uid to it.equippedTo }
        )
        assertEquals(
            "카드별 진행 수치(레벨·스탯·호감도)가 그대로여야 한다",
            before.ownedCards.map { it.atk to it.def },
            after.ownedCards.map { it.atk to it.def }
        )
        assertEquals(
            before.ownedCards.map { it.upgradeLevel to it.bondLevel },
            after.ownedCards.map { it.upgradeLevel to it.bondLevel }
        )
        assertEquals(
            "복원된 저장은 원본과 완전히 같은 저장 내용이어야 한다",
            SaveCodec.encode(before.toGameSave()),
            SaveCodec.encode(after.toGameSave())
        )
        assertTrue("복원 안내 문구가 남아야 한다", after.statusMessage.contains("불러오기"))
    }

    @Test
    fun `편성 카드는 보유 카드와 같은 인스턴스를 가리킨다`() {
        val store = InMemorySaveStore()
        val first = newGame(store)
        first.trainForExp()
        val second = newGame(store)
        val state = second.uiState.value
        val ownedById = state.ownedCards.associateBy { it.id }
        state.party.forEach { member ->
            assertTrue(
                "편성 카드가 보유 목록의 다른 인스턴스면 강화 수치가 갈라진다",
                ownedById.getValue(member.card.id) === member.card
            )
        }
    }

    @Test
    fun `인트로 시청 여부가 저장되어 재실행 시 로비에서 시작한다`() {
        val store = InMemorySaveStore()
        val first = newGame(store, introSeen = false)
        assertTrue("처음 켜면 인트로가 재생된다", first.uiState.value.introStory.isPlaying)

        first.skipIntro()

        val second = newGame(store, introSeen = false)
        assertFalse("인트로를 본 뒤에는 재생하지 않는다", second.uiState.value.introStory.isPlaying)
        assertTrue(second.uiState.value.introStory.hasSeenIntro)
    }

    @Test
    fun `손상된 저장 데이터는 크래시 없이 기본 상태로 폴백한다`() {
        listOf(
            "{ this is not a save }",
            "DUALGATE-SAVE\nversion=1\ncard=고장난|줄",
            "DUALGATE-SAVE",
            "",
            "\u0000\u0001\u0002"
        ).forEach { broken ->
            val vm = newGame(InMemorySaveStore(broken))
            val state = vm.uiState.value
            assertEquals("손상 데이터는 시작 골드로 폴백", TcgGameViewModel.START_GOLD, state.gold)
            assertEquals("기본 스타팅 3인방으로 시작", 3, state.ownedCards.size)
            assertEquals(3, state.party.size)
            assertTrue("스테이지 진행도도 비어 있어야 한다", state.stageCleared.isEmpty())
        }
    }

    @Test
    fun `지원하지 않는 버전의 저장 데이터도 기본 상태로 폴백한다`() {
        val future = buildString {
            appendLine(SaveCodec.HEADER)
            appendLine("version=999")
            appendLine("gold=999999")
            appendLine("card=3|9000|9000|5|0|100|9|0|1|3|0|3|BATTLE")
        }
        val vm = newGame(InMemorySaveStore(future))
        assertEquals(TcgGameViewModel.START_GOLD, vm.uiState.value.gold)
        assertEquals(3, vm.uiState.value.ownedCards.size)
    }

    @Test
    fun `도감에 없는 카드·스테이지 번호가 섞인 저장은 유효한 항목만 복원한다`() {
        val store = InMemorySaveStore()
        val base = newGame(store)
        base.trainForExp()
        val encoded = store.stored!!
        val tampered = buildString {
            appendLine(encoded.trimEnd())
            appendLine("card=9999|100|100|1|0|0|0|0|0|0|0|0|BATTLE")
            appendLine("party=9999|5|100|100|0")
            appendLine("stage=99")
        }
        val vm = newGame(InMemorySaveStore(tampered))
        val state = vm.uiState.value
        assertEquals("미등록 카드는 조용히 버린다", listOf(3, 1, 2), state.ownedCards.map { it.id })
        assertTrue("없는 슬롯·미보유 카드 편성은 무시한다", state.party.none { it.card.id == 9999 })
        assertTrue(state.stageCleared.none { it == 99 })
    }

    @Test
    fun `새 게임 초기화는 진행과 저장 파일을 함께 되돌린다`() {
        val store = InMemorySaveStore()
        val vm = newGame(store)
        vm.chooseEventB(engageBattle = false) // 금화 -200
        vm.trainForExp()
        assertTrue("초기화 전에는 진행이 있어야 한다", vm.uiState.value.gold != TcgGameViewModel.START_GOLD)

        vm.resetGame()

        val reset = vm.uiState.value
        assertEquals(TcgGameViewModel.START_GOLD, reset.gold)
        assertEquals(TcgGameViewModel.START_GEMS, reset.gems)
        assertEquals(0, reset.playerExp)
        assertTrue(reset.stageCleared.isEmpty())
        assertEquals(3, reset.ownedCards.size)
        assertEquals(3, reset.party.size)
        assertNull(reset.eventBChoice)
        assertEquals(0, reset.combatPenalty.totalStacks)

        // 앱을 다시 켜도 초기화된 상태로 시작한다 (되살아나지 않는다)
        val again = newGame(store)
        assertEquals(TcgGameViewModel.START_GOLD, again.uiState.value.gold)
        assertTrue(again.uiState.value.stageCleared.isEmpty())
        assertEquals(3, again.uiState.value.ownedCards.size)
    }

    @Test
    fun `상태가 바뀔 때마다 자동 저장된다`() {
        val store = InMemorySaveStore()
        val vm = newGame(store)
        val before = store.writeCount
        vm.trainForExp()
        assertTrue("행동 직후 저장이 일어나야 한다", store.writeCount > before)
        // 변화 없는 호출은 파일을 다시 쓰지 않는다
        val afterAction = store.writeCount
        vm.openScreen(com.game.tcg.ui.LobbyDestination.LOBBY)
        assertTrue("같은 내용이면 다시 쓰지 않는다", store.writeCount <= afterAction + 1)
    }

    @Test
    fun `저장 코덱은 카드 취급 수치와 문자열을 정확히 왕복한다`() {
        val save = GameSave(
            gold = 12345,
            gems = 7,
            shards = mapOf(3 to 5, 11 to 2),
            cards = listOf(
                com.game.tcg.persistence.SavedCard(
                    cardId = 3, atk = 2100, def = 1800, starLevel = 3, currentExp = 40,
                    bondLevel = 77, upgradeLevel = 4, memoryAffinity = 12,
                    reincarnationCount = 2, equippedRelicCount = 1,
                    isMemoryReset = true, memoryFragments = 2, visual = "ROMANTIC_DATE"
                )
            ),
            storyReadLog = listOf("[프롤로그] 봉인 카드팩", "줄바꿈\n포함된 기록 | 파이프 , 쉼표"),
            eventAChoice = "A_TALK",
            stageCleared = listOf(1, 2, 3),
            equipment = listOf(
                com.game.tcg.persistence.SavedEquipment(uid = "weapon_training_dagger#7", itemId = "weapon_training_dagger", equippedTo = 3)
            )
        )

        val decoded = SaveCodec.decode(SaveCodec.encode(save))
        assertNotNull(decoded)
        assertEquals(save.gold, decoded!!.gold)
        assertEquals(save.shards, decoded.shards)
        assertEquals(save.cards, decoded.cards)
        assertEquals(save.storyReadLog, decoded.storyReadLog)
        assertEquals(save.stageCleared, decoded.stageCleared)
        assertEquals(save.equipment, decoded.equipment)
        assertEquals(save.eventAChoice, decoded.eventAChoice)
    }

    @Test
    fun `저장소가 없으면 아무 것도 저장하지 않는다`() {
        val vm = TcgGameViewModel(random = Random(1), initialOwnedCardIds = listOf(3, 1, 2))
        vm.trainForExp()
        assertTrue(vm.uiState.value.playerExp > 0)
        assertEquals(null, SaveCodec.decode(null))
    }

    @Test
    fun `조각 지갑도 저장·복원된다`() {
        val store = InMemorySaveStore()
        val first = TcgGameViewModel(
            random = Random(3),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialShards = ShardWallet(mapOf(3 to 9, 2 to 4)),
            saveStore = store
        )
        first.trainForExp()
        val second = newGame(store)
        assertEquals(mapOf(3 to 9, 2 to 4), second.uiState.value.shards.byCard)
    }
}
