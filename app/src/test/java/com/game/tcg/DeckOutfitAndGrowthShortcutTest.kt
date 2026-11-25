package com.game.tcg

import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.model.CardCatalog
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
 * 덱 편성 화면 개선 — 2026-11-22 사용자 요청 검증.
 *
 * ① **덱에 편성된 캐릭터의 복장 변경**을 덱 편성 화면에서 바로 한다.
 *    - 해금 규칙은 그대로: 일상 30 · 데이트 70 · 미니어처는 항상
 *    - 고른 복장은 로비·도감과 **같은 캐릭터별 복장 기억**(`TcgGameState.lobbyOutfits`)을 쓴다
 *      → 덱 편성 · 메인 로비 · 도감이 한 곳에서 동기화된다
 * ② **성장·강화 화면 바로가기** — 편성 캐릭터가 선택된 채로 [LobbyDestination.GROWTH] 가 열린다
 *
 * (UI 컴포저블은 JVM 테스트에서 그리지 않는다. 화면이 쓰는 상태/로직을 여기서 검증한다.)
 */
class DeckOutfitAndGrowthShortcutTest {

    private fun game(
        bond: Int = 0,
        owned: List<Int> = listOf(3, 1, 2),
        store: InMemorySaveStore? = null
    ) = TcgGameViewModel(
        random = Random(11),
        initialOwnedCardIds = owned,
        initialBondLevel = bond,
        todayEpochDay = { 20_000 },
        saveStore = store
    )

    // ── 1. 캐릭터 시트 열기 / 닫기 ───────────────────────────────────

    @Test
    fun `편성 캐릭터를 탭하면 캐릭터 시트가 열리고 닫힌다`() {
        val vm = game()
        assertNull("처음에는 시트가 닫혀 있다", vm.uiState.value.deckSheetCardId)

        vm.openDeckCharacterSheet(1)

        val opened = vm.uiState.value
        assertEquals(1, opened.deckSheetCardId)
        assertEquals(1, opened.deckSheetCard?.id)
        assertNotNull("시트에 캐릭터 이름이 보여야 한다", opened.deckSheetCard?.name)
        assertTrue(opened.statusMessage.contains("덱 편성"))
        assertNull(opened.deckOutfitLockMessage)

        vm.closeDeckCharacterSheet()

        assertNull(vm.uiState.value.deckSheetCardId)
        assertNull(vm.uiState.value.deckSheetCard)
    }

    @Test
    fun `아직 계약하지 않은 캐릭터는 덱 시트로 열리지 않는다`() {
        val vm = game(owned = listOf(3, 1, 2))

        vm.openDeckCharacterSheet(112)

        val state = vm.uiState.value
        assertNull("미보유 캐릭터는 시트 대상이 아니다", state.deckSheetCardId)
        assertTrue(state.statusMessage.contains("계약하지 않은"))
    }

    // ── 2. 덱 편성 화면에서 복장 변경 (해금 규칙 유지) ────────────────

    @Test
    fun `덱 편성 화면에서 일상복으로 바꾸면 덱 카드가 그 복장으로 보인다`() {
        val vm = game(bond = AffinityRules.CASUAL_UNLOCK_BOND)
        vm.openDeckCharacterSheet(1)
        assertEquals("기본은 전투복", VisualState.BATTLE, vm.uiState.value.outfitVisual(1))

        vm.selectDeckOutfit(VisualState.CASUAL_CUTE)

        val state = vm.uiState.value
        assertEquals(VisualState.CASUAL_CUTE, state.outfitVisual(1))
        assertEquals(VisualState.CASUAL_CUTE, state.lobbyOutfits[1])
        assertNull(state.deckOutfitLockMessage)
        assertTrue(state.statusMessage.contains("복장"))
        assertEquals("시트는 열어 둔 채로 유지된다", 1, state.deckSheetCardId)
    }

    @Test
    fun `친밀도가 모자란 복장은 덱 편성 화면에서도 잠긴다`() {
        val vm = game(bond = 0)
        vm.openDeckCharacterSheet(1)

        vm.selectDeckOutfit(VisualState.CASUAL_CUTE)

        val locked = vm.uiState.value
        assertNull("잠긴 복장은 기억되지 않는다", locked.lobbyOutfits[1])
        assertEquals(VisualState.BATTLE, locked.outfitVisual(1))
        assertNotNull(locked.deckOutfitLockMessage)
        assertTrue("친밀도 30 안내", locked.deckOutfitLockMessage!!.contains("30"))

        vm.selectDeckOutfit(VisualState.ROMANTIC_DATE)
        assertTrue("친밀도 70 안내", vm.uiState.value.deckOutfitLockMessage!!.contains("70"))
    }

    @Test
    fun `미니어처는 친밀도와 무관하게 덱 편성 화면에서 고를 수 있다`() {
        val vm = game(bond = 0)
        vm.openDeckCharacterSheet(2)

        vm.selectDeckOutfit(VisualState.MINIATURE)

        assertEquals(VisualState.MINIATURE, vm.uiState.value.outfitVisual(2))
        assertNull(vm.uiState.value.deckOutfitLockMessage)
    }

    @Test
    fun `복장 목록은 친밀도 단계만큼만 열린다`() {
        val start = game(bond = 0)
        assertEquals(listOf(VisualState.BATTLE, VisualState.MINIATURE), start.uiState.value.unlockedOutfitVisuals(1))

        val casual = game(bond = AffinityRules.CASUAL_UNLOCK_BOND)
        assertEquals(
            listOf(VisualState.BATTLE, VisualState.CASUAL_CUTE, VisualState.MINIATURE),
            casual.uiState.value.unlockedOutfitVisuals(1)
        )

        val date = game(bond = AffinityRules.DATE_UNLOCK_BOND)
        assertEquals(VisualState.entries.toList(), date.uiState.value.unlockedOutfitVisuals(1))
    }

    @Test
    fun `시트를 열지 않은 채로 복장을 바꾸면 안내만 남는다`() {
        val vm = game(bond = AffinityRules.CASUAL_UNLOCK_BOND)

        vm.selectDeckOutfit(VisualState.CASUAL_CUTE)

        val state = vm.uiState.value
        assertTrue(state.lobbyOutfits.isEmpty())
        assertTrue(state.statusMessage.contains("먼저"))
    }

    // ── 3. 로비 · 도감과의 동기화 ────────────────────────────────────

    @Test
    fun `덱 편성 화면의 복장 변경이 메인 로비 복장까지 함께 바꾼다`() {
        val vm = game(bond = AffinityRules.CASUAL_UNLOCK_BOND)
        vm.selectLobbyHero(3)
        assertEquals(VisualState.BATTLE, vm.uiState.value.lobbyVisual)

        vm.openDeckCharacterSheet(3)
        vm.selectDeckOutfit(VisualState.CASUAL_CUTE)

        assertEquals("대표 캐릭터는 로비에서도 그 복장", VisualState.CASUAL_CUTE, vm.uiState.value.lobbyVisual)
    }

    @Test
    fun `로비에서 고른 복장은 덱 편성 화면에도 그대로 보인다`() {
        val vm = game(bond = AffinityRules.DATE_UNLOCK_BOND)
        vm.selectLobbyHero(3)

        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)

        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.outfitVisual(3))
        vm.openDeckCharacterSheet(3)
        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.outfitVisual(3))
    }

    @Test
    fun `도감과 덱 편성 화면의 복장이 같은 기억을 공유한다`() {
        val vm = game(bond = AffinityRules.DATE_UNLOCK_BOND)

        // 도감에서 고른 복장 → 덱 편성 화면
        vm.selectCard(1)
        assertEquals("고른 적 없으면 전투복", VisualState.BATTLE, vm.uiState.value.selectedVisual)
        vm.selectVisual(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.outfitVisual(1))

        // 덱 편성 화면에서 바꾼 복장 → 도감(그 캐릭터를 다시 열면) 그 복장
        vm.selectCard(2)
        vm.openDeckCharacterSheet(1)
        vm.selectDeckOutfit(VisualState.CASUAL_CUTE)
        vm.selectCard(1)
        assertEquals(VisualState.CASUAL_CUTE, vm.uiState.value.selectedVisual)
    }

    @Test
    fun `덱 편성 화면에서 바꾼 복장은 저장되고 다시 켜도 유지된다`() {
        val store = InMemorySaveStore()
        val first = game(bond = AffinityRules.DATE_UNLOCK_BOND, store = store)
        first.openDeckCharacterSheet(1)
        first.selectDeckOutfit(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.ROMANTIC_DATE, first.uiState.value.outfitVisual(1))

        val second = game(bond = AffinityRules.DATE_UNLOCK_BOND, store = store)

        assertEquals(VisualState.ROMANTIC_DATE, second.uiState.value.outfitVisual(1))
        assertEquals(VisualState.ROMANTIC_DATE, second.uiState.value.lobbyOutfits[1])
    }

    // ── 4. 성장·강화 화면 바로가기 ──────────────────────────────────

    @Test
    fun `덱 편성 화면에서 성장 바로가기를 누르면 그 캐릭터가 선택된 성장 화면이 열린다`() {
        val vm = game()
        vm.selectGrowthCard(3)
        vm.openDeckCharacterSheet(1)

        vm.openGrowthFromDeck(1)

        val state = vm.uiState.value
        assertEquals(LobbyDestination.GROWTH, state.currentScreen)
        assertEquals("성장 대상이 그 캐릭터", 1, state.growthCardId)
        assertEquals(1, state.growthCard?.id)
        assertEquals("도감 선택도 함께 맞춘다", 1, state.selectedCardId)
        assertNull("시트는 닫힌다", state.deckSheetCardId)
        assertTrue(state.statusMessage.contains("성장"))
    }

    @Test
    fun `덱에 넣지 않은 보유 캐릭터도 성장 바로가기로 갈 수 있다`() {
        val vm = game(owned = listOf(3, 1, 2))
        vm.unloadAllParty()

        vm.openGrowthFromDeck(2)

        val state = vm.uiState.value
        assertEquals(LobbyDestination.GROWTH, state.currentScreen)
        assertEquals(2, state.growthCardId)
        assertTrue("덱에 없어도 성장은 열린다", state.party.none { it.card.id == 2 })
    }

    @Test
    fun `미보유 캐릭터로는 성장 바로가기가 열리지 않는다`() {
        val vm = game(owned = listOf(3, 1, 2))
        vm.openScreen(LobbyDestination.DECK)

        vm.openGrowthFromDeck(112)

        val state = vm.uiState.value
        assertEquals("덱 화면에 그대로 머문다", LobbyDestination.DECK, state.currentScreen)
        assertFalse("성장 대상이 되지 않는다", state.growthCardId == 112)
        assertTrue(state.statusMessage.contains("보유하지 않은"))
    }

    // ── 5. 편성 자체는 그대로 동작한다 (회귀 방지) ──────────────────

    @Test
    fun `복장을 바꿔도 덱 편성과 전투력은 그대로다`() {
        val vm = game(bond = AffinityRules.CASUAL_UNLOCK_BOND)
        vm.autoEquipParty()
        val before = vm.uiState.value
        val power = before.deckDisplayPower
        val slots = before.party.map { it.card.id to it.slotIndex }.toSet()
        val target = before.party.first().card.id

        vm.openDeckCharacterSheet(target)
        vm.selectDeckOutfit(VisualState.CASUAL_CUTE)

        val after = vm.uiState.value
        assertEquals("편성 슬롯은 그대로", slots, after.party.map { it.card.id to it.slotIndex }.toSet())
        assertEquals("전투력은 복장과 무관", power, after.deckDisplayPower)
        assertEquals(VisualState.CASUAL_CUTE, after.outfitVisual(target))
    }

    @Test
    fun `덱 시트에서 빼기를 누르면 그 슬롯이 비워진다`() {
        val vm = game()
        vm.autoEquipParty()
        val target = vm.uiState.value.party.first()
        vm.openDeckCharacterSheet(target.card.id)

        vm.unloadSlot(target.slotIndex)

        val state = vm.uiState.value
        assertNull("슬롯이 비워진다", state.memberAt(target.slotIndex))
        assertTrue(state.battleReserve.any { it.id == target.card.id })
        assertTrue("카드는 193명 로스터 안에 그대로 있다", CardCatalog.ALL.any { it.id == target.card.id })
    }
}
