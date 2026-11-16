package com.game.tcg

import com.game.tcg.core.engine.PackOpeningEngine
import com.game.tcg.core.engine.PackOpeningPhase
import com.game.tcg.core.model.CardRarity
import com.game.tcg.core.model.PackDraw
import com.game.tcg.core.model.PackOpenResult
import com.game.tcg.ui.CharacterArtRegistry
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 카드팩 개봉 비주얼 연출 — 2026-11-16 사용자 요청 검증
 *
 * 1) 단계: 봉인 카드팩(SEALED) → 터치 개봉 → 카드 1장씩(REVEAL) → 전체 그리드(SUMMARY) → 닫기
 * 2) 개봉 판정은 그대로: 카드·조각·골드·티켓이 실제로 적용되고 연출만 얹힌다
 * 3) 연출에 쓰는 카드 일러스트(CardArtLoader·CharacterArtRegistry)가 전부 존재한다
 * 4) 「개봉 연출 다시 보기」가 마지막 결과로 연출을 다시 연다
 */
class PackOpeningEventTest {

    private val engine = PackOpeningEngine()

    private fun resultOf(ids: List<Int>, news: Set<Int> = ids.toSet()): PackOpenResult {
        val draws = ids.map { id ->
            val isNew = news.contains(id)
            PackDraw(
                cardId = id,
                name = "카드$id",
                rarity = CardRarity.COMMON,
                isNew = isNew,
                shards = if (isNew) 0 else 10,
                gold = if (isNew) 0 else 50
            )
        }
        return PackOpenResult(
            packId = "pack_standard",
            packName = "일반 카드팩",
            draws = draws,
            goldSpent = 200,
            ticketsSpent = 0,
            newCardIds = draws.filter { it.isNew }.map { it.cardId },
            totalShards = draws.sumOf { it.shards },
            totalGold = draws.sumOf { it.gold },
            message = "카드팩 3장 개봉"
        )
    }

    // ── 1. 단계 전이 ───────────────────────────────────────────────

    @Test
    fun `개봉 연출은 봉인 카드팩에서 시작해 카드 1장씩 전체 그리드로 끝난다`() {
        val start = engine.start(resultOf(listOf(1, 2, 3)))
        assertEquals(PackOpeningPhase.SEALED, start.phase)
        assertTrue(start.isActive)
        assertEquals(0, start.revealedCount)
        assertEquals("0 / 3", start.stepLabel)
        assertEquals("일반 카드팩", start.packName)

        val reveal0 = engine.tearSeal(start)
        assertEquals(PackOpeningPhase.REVEAL, reveal0.phase)
        assertEquals(0, reveal0.revealIndex)
        assertEquals(1, reveal0.revealedCount)
        assertEquals(1, reveal0.currentDraw?.cardId)
        assertTrue(reveal0.hasNextCard)

        val reveal1 = engine.revealNext(reveal0)
        assertEquals(1, reveal1.revealIndex)
        assertEquals("2 / 3", reveal1.stepLabel)

        val reveal2 = engine.revealNext(reveal1)
        assertEquals(2, reveal2.revealIndex)
        assertFalse("마지막 카드", reveal2.hasNextCard)
        assertEquals(3, reveal2.revealedCount)

        // 마지막 카드에서 한 번 더 → 전체 그리드
        val summary = engine.revealNext(reveal2)
        assertEquals(PackOpeningPhase.SUMMARY, summary.phase)
        assertEquals(3, summary.revealedCount)

        val closed = engine.close()
        assertEquals(PackOpeningPhase.IDLE, closed.phase)
        assertFalse(closed.isActive)
    }

    @Test
    fun `전체 보기는 남은 카드를 한 번에 펼친다`() {
        val reveal = engine.tearSeal(engine.start(resultOf(listOf(4, 5, 6, 7, 8))))
        val all = engine.revealAll(reveal)
        assertEquals(PackOpeningPhase.SUMMARY, all.phase)
        assertEquals(5, all.revealedCount)
        assertEquals("5 / 5", all.stepLabel)
    }

    @Test
    fun `봉인 전에는 카드가 없고 봉인 해제 전에는 다음 카드로 넘어가지 않는다`() {
        val sealed = engine.start(resultOf(listOf(1, 2, 3)))
        assertNull("봉인 상태에서는 세워진 카드가 없다", sealed.currentDraw)
        // SEALED 에서 revealNext 를 불러도 단계가 바뀌지 않는다
        assertEquals(PackOpeningPhase.SEALED, engine.revealNext(sealed).phase)
        // 두 번 봉인 해제해도 첫 카드 그대로
        val once = engine.tearSeal(sealed)
        assertEquals(0, engine.tearSeal(once).revealIndex)
    }

    @Test
    fun `카드가 없는 결과로는 연출을 열지 않는다`() {
        val empty = PackOpenResult(
            packId = "pack_standard",
            packName = "일반 카드팩",
            draws = emptyList(),
            goldSpent = 0,
            ticketsSpent = 0
        )
        val state = engine.start(empty)
        assertEquals(PackOpeningPhase.IDLE, state.phase)
        assertFalse(state.isActive)
    }

    @Test
    fun `신규와 중복 수를 연출 배지용으로 나눠 센다`() {
        val mixed = engine.start(resultOf(listOf(1, 2, 3), news = setOf(1)))
        assertEquals(1, mixed.newCount)
        assertEquals(2, mixed.duplicateCount)
        assertEquals(PackOpeningPhase.SEALED, mixed.phase)
    }

    // ── 2. 게임 상태 연동 ─────────────────────────────────────────

    @Test
    fun `카드팩을 사면 연출이 봉인 카드팩에서 열리고 닫으면 상점으로 돌아간다`() {
        val vm = TcgGameViewModel(
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 5_000,
            initialPackTickets = 1
        )
        assertFalse("처음에는 연출 없음", vm.uiState.value.isPackOpeningActive)

        val result = vm.openPack("pack_standard")
        assertNotNull("개봉 결과", result)
        val afterOpen = vm.uiState.value
        assertEquals(PackOpeningPhase.SEALED, afterOpen.packOpening.phase)
        assertEquals(result!!.draws.size, afterOpen.packOpening.total)
        assertTrue("개봉 결과는 기존대로 보관", afterOpen.lastPackOpen != null)

        vm.tearPackSeal()
        assertEquals(PackOpeningPhase.REVEAL, vm.uiState.value.packOpening.phase)
        assertEquals(0, vm.uiState.value.packOpening.revealIndex)

        repeat(result.draws.size - 1) { vm.revealNextPackDraw() }
        assertEquals(result.draws.size - 1, vm.uiState.value.packOpening.revealIndex)
        assertEquals(PackOpeningPhase.REVEAL, vm.uiState.value.packOpening.phase)

        vm.revealNextPackDraw()
        assertEquals(PackOpeningPhase.SUMMARY, vm.uiState.value.packOpening.phase)

        vm.closePackOpening()
        assertFalse(vm.uiState.value.isPackOpeningActive)
    }

    @Test
    fun `개봉 연출은 보유 카드와 재화 계산을 바꾸지 않는다`() {
        val vm = TcgGameViewModel(
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 5_000,
            initialPackTickets = 1
        )
        val before = vm.uiState.value
        val result = vm.openPack("pack_standard")!!
        val afterOpen = vm.uiState.value

        vm.tearPackSeal()
        repeat(result.draws.size + 1) { vm.revealNextPackDraw() }
        vm.revealAllPackDraws()
        val afterEffects = vm.uiState.value

        assertEquals("연출 단계는 카드 수를 바꾸지 않는다", afterOpen.ownedCards.size, afterEffects.ownedCards.size)
        assertEquals(afterOpen.gold, afterEffects.gold)
        assertEquals(afterOpen.shardTotal, afterEffects.shardTotal)
        assertEquals("티켓 1장 사용", before.packTickets - 1, afterEffects.packTickets)
    }

    @Test
    fun `개봉 연출 다시 보기는 마지막 결과로 봉인 카드팩을 다시 연다`() {
        val vm = TcgGameViewModel(
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 5_000,
            initialPackTickets = 1
        )
        vm.openPack("pack_standard")
        vm.tearPackSeal()
        vm.closePackOpening()
        assertFalse(vm.uiState.value.isPackOpeningActive)

        vm.replayPackOpening()
        assertEquals(PackOpeningPhase.SEALED, vm.uiState.value.packOpening.phase)
        assertEquals(vm.uiState.value.lastPackOpen?.draws?.size, vm.uiState.value.packOpening.total)
    }

    @Test
    fun `개봉 결과 카드에는 모두 실제 일러스트가 매핑되어 있다`() {
        val vm = TcgGameViewModel(
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 20_000,
            initialPackTickets = 3
        )
        val drawn = buildList {
            vm.openPack("pack_standard")?.draws?.forEach { add(it.cardId) }
            vm.closePackOpening()
            vm.openPack("pack_premium")?.draws?.forEach { add(it.cardId) }
            vm.closePackOpening()
            vm.openPack("pack_element_fire")?.draws?.forEach { add(it.cardId) }
        }
        assertTrue("카드가 나왔다", drawn.isNotEmpty())
        drawn.forEach { id ->
            assertTrue("#$id 일러스트 없음", CharacterArtRegistry.hasArt(id))
        }
    }
}
