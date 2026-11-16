package com.game.tcg

import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.VisualState
import com.game.tcg.persistence.GameSave
import com.game.tcg.persistence.InMemorySaveStore
import com.game.tcg.persistence.SaveCodec
import com.game.tcg.persistence.SavedCard
import com.game.tcg.persistence.SavedPartyMember
import com.game.tcg.ui.CardLayoutRules
import com.game.tcg.ui.TcgGameViewModel
import com.game.tcg.ui.TcgType
import com.game.tcg.ui.toGameSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * UI 가독성 · 카드 규격 · 복장 유지 — 2026-11-16 사용자 요청 검증.
 *
 * 사용자 요청(원문)
 *  「글씨가 너무 작고 … 캐릭터가 너무 작고 일부분만 보이는 경우가 많아
 *   화면에는 최대 12장만 보이게 해주고 스크롤로 위 아래 움직일수 있게 …
 *   덱 구성이나 전투일때 케릭터를 크기에 맞춰서 보이게 해주고
 *   메인 화면에서 일상이나 데이트 복장을 선택한 캐릭터는 유지가 되게 해줘」
 *
 * 1) [TcgType] — 본문·배지 최소 12sp, 제목 18sp/22sp
 * 2) [CardLayoutRules] — 카드 2:3 · 한 화면 12장 · Fit(잘림 없음) · 3열
 * 3) 복장 유지 — 캐릭터별 기억 + 앱 재시작 복원 + 해금 잠금 규칙 유지
 */
class UiReadabilityAndCardRulesTest {

    private fun starterGame(
        bond: Int = 100,
        store: InMemorySaveStore? = null
    ) = TcgGameViewModel(
        random = Random(21),
        initialOwnedCardIds = listOf(3, 1, 2),
        initialGold = 5000,
        initialShards = ShardWallet(),
        initialPackTickets = 1,
        initialBondLevel = bond,
        todayEpochDay = { 20_000 },
        saveStore = store
    )

    // ──────────────────────────────────────────────────────────────
    // 1. 글자 크기
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `본문과 배지 글자는 12sp 아래로 내려가지 않는다`() {
        assertTrue("스케일 전체가 최소치 이상이어야 한다", TcgType.allAtLeastMinimum())
        assertTrue("본문은 14sp 이상", TcgType.BODY >= 14f)
        assertTrue("보조 본문은 13sp 이상", TcgType.BODY_SMALL >= 13f)
        assertTrue("배지·버튼은 12sp 이상", TcgType.LABEL >= 12f)
        assertTrue("카드 타일 표식도 11sp 이상", TcgType.TILE_MARK >= 11f)
    }

    @Test
    fun `화면 제목과 섹션 제목은 18sp 이상이다`() {
        assertTrue("섹션 제목 18sp 이상", TcgType.SECTION >= 18f)
        assertTrue("화면 제목 22sp 이상", TcgType.TITLE >= 22f)
        assertTrue(TcgType.TITLE > TcgType.SECTION)
        assertTrue(TcgType.SECTION > TcgType.BODY)
    }

    // ──────────────────────────────────────────────────────────────
    // 2. 카드 규격 (2:3 · 12장 · Fit)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `카드 타일은 원본과 같은 2대3 세로 비율이다`() {
        assertEquals(2f, CardLayoutRules.ASPECT_WIDTH)
        assertEquals(3f, CardLayoutRules.ASPECT_HEIGHT)
        assertEquals(2f / 3f, CardLayoutRules.TILE_ASPECT, 0.0001f)
        assertEquals(2f / 3f, CardLayoutRules.ASPECT_WIDTH / CardLayoutRules.ASPECT_HEIGHT, 0.0001f)
    }

    @Test
    fun `한 화면 카드 상한은 12장이고 3열 4행으로 배치된다`() {
        assertEquals(12, CardLayoutRules.MAX_VISIBLE_CARDS)
        assertEquals(3, CardLayoutRules.GRID_COLUMNS)
        assertEquals(4, CardLayoutRules.rowsFor(12))
        assertEquals(3, CardLayoutRules.rowsFor(9))
        assertEquals(1, CardLayoutRules.rowsFor(1))
        assertEquals(0, CardLayoutRules.rowsFor(0))
        // 12장을 넘는 덱/진영은 한 화면에 담지 않고 스크롤한다
        assertFalse(CardLayoutRules.requiresScroll(12))
        assertTrue(CardLayoutRules.requiresScroll(13))
        assertTrue(CardLayoutRules.requiresScroll(CardCatalog.ALL.size))
    }

    @Test
    fun `카드 아트는 잘라내지 않고 타일 안에 맞춘다`() {
        assertTrue("아트는 Fit 으로 전체가 보여야 한다", CardLayoutRules.ART_FIT)
    }

    @Test
    fun `카드 한 장은 이전 덱 그리드보다 훨씬 크게 계산된다`() {
        // 실기기(393dp 폭) 기준 — 3열이면 폭 118dp 내외, 높이는 2:3 이라 177dp 내외
        val width = CardLayoutRules.tileWidthDp(screenWidthDp = 393f)
        val height = CardLayoutRules.tileHeightDp(width)
        assertTrue("카드 폭이 100dp 이상이어야 한다 (계산값 ${width})", width >= 100f)
        assertTrue("카드 높이가 160dp 이상이어야 한다 (계산값 ${height})", height >= 160f)
        assertEquals(width * 1.5f, height, 0.01f)
        // 예전 덱 슬롯은 높이 78dp 납작한 타일이었다 — 세로로 2배 이상 커져야 한다
        assertTrue("이전 78dp 슬롯보다 2배 이상 커야 한다", height > 78f * 2f)
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 복장 유지 (세션 · 캐릭터별 기억)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `캐릭터별로 고른 복장을 기억해 돌아오면 그대로 복원된다`() {
        val vm = starterGame(bond = 100)
        val heroA = vm.uiState.value.lobbyHeroId
        val heroB = vm.uiState.value.ownedCards.first { it.id != heroA }.id

        // 대표 캐릭터(A)는 데이트 룩
        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.lobbyVisual)

        // 다른 캐릭터(B)로 전환 → 전투복 선택
        vm.selectLobbyHero(heroB)
        assertEquals(heroB, vm.uiState.value.lobbyHeroId)
        vm.selectLobbyVisual(VisualState.BATTLE)
        assertEquals(VisualState.BATTLE, vm.uiState.value.lobbyVisual)

        // 다시 A 로 돌아오면 데이트 룩이 그대로
        vm.selectLobbyHero(heroA)
        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.lobbyVisual)

        // B 로 돌아가면 전투복 그대로
        vm.selectLobbyHero(heroB)
        assertEquals(VisualState.BATTLE, vm.uiState.value.lobbyVisual)

        // 캐릭터별 기억이 상태에 남아 있다
        val outfits = vm.uiState.value.lobbyOutfits
        assertEquals(VisualState.ROMANTIC_DATE, outfits[heroA])
        assertEquals(VisualState.BATTLE, outfits[heroB])
    }

    @Test
    fun `다른 화면을 다녀와도 고른 복장이 유지된다`() {
        val vm = starterGame(bond = 100)
        vm.selectLobbyVisual(VisualState.CASUAL_CUTE)
        val before = vm.uiState.value.lobbyVisual

        // 덱 → 성장 → 카드팩 → 전투 → 로비
        vm.openScreen(com.game.tcg.ui.LobbyDestination.DECK)
        vm.openScreen(com.game.tcg.ui.LobbyDestination.GROWTH)
        vm.openScreen(com.game.tcg.ui.LobbyDestination.SHOP)
        vm.openScreen(com.game.tcg.ui.LobbyDestination.BATTLE)
        vm.backToLobby()

        assertEquals(VisualState.CASUAL_CUTE, before)
        assertEquals(VisualState.CASUAL_CUTE, vm.uiState.value.lobbyVisual)
    }

    @Test
    fun `앱을 껐다 켜도 고른 복장이 그대로 복원된다`() {
        val store = InMemorySaveStore()
        val first = starterGame(store = store)
        val hero = first.uiState.value.lobbyHeroId
        first.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.ROMANTIC_DATE, first.uiState.value.lobbyVisual)
        assertNotNull("복장을 바꾸면 저장 파일이 갱신되어야 한다", store.stored)

        // 앱 재실행 — 같은 저장소를 넘긴 새 ViewModel
        val second = starterGame(store = store)
        assertEquals(VisualState.ROMANTIC_DATE, second.uiState.value.lobbyVisual)
        assertEquals(VisualState.ROMANTIC_DATE, second.uiState.value.lobbyOutfits[hero])
    }

    @Test
    fun `저장 텍스트로 왕복해도 복장 정보가 유지된다`() {
        val save = GameSave(
            gold = 100,
            cards = listOf(
                SavedCard(cardId = 3, atk = 2100, def = 1800, bondLevel = 80),
                SavedCard(cardId = 1, atk = 2200, def = 1900, bondLevel = 40)
            ),
            party = listOf(
                SavedPartyMember(cardId = 3, slotIndex = 0, hp = 5000, maxHp = 5000)
            ),
            lobbyHeroId = 3,
            lobbyVisual = VisualState.ROMANTIC_DATE.name,
            lobbyOutfits = mapOf(3 to VisualState.ROMANTIC_DATE.name, 1 to VisualState.CASUAL_CUTE.name)
        )
        val text = SaveCodec.encode(save)
        assertTrue("복장 행이 저장 텍스트에 있어야 한다", text.contains("outfit=3|ROMANTIC_DATE"))

        val decoded = SaveCodec.decode(text)
        assertNotNull(decoded)
        assertEquals(VisualState.ROMANTIC_DATE.name, decoded!!.lobbyOutfits[3])
        assertEquals(VisualState.CASUAL_CUTE.name, decoded.lobbyOutfits[1])
        assertEquals(VisualState.ROMANTIC_DATE.name, decoded.lobbyVisual)
    }

    @Test
    fun `해금되지 않은 복장은 기존 규칙대로 잠긴다`() {
        // 친밀도 0 — 일상(30)·데이트(70) 모두 잠금
        val vm = starterGame(bond = 0)
        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.BATTLE, vm.uiState.value.lobbyVisual)
        assertTrue(vm.uiState.value.lobbyOutfits.isEmpty())

        vm.selectLobbyVisual(VisualState.CASUAL_CUTE)
        assertEquals(VisualState.BATTLE, vm.uiState.value.lobbyVisual)
    }

    @Test
    fun `저장된 복장도 친밀도보다 높으면 복원 시 내려간다`() {
        val save = GameSave(
            gold = 100,
            cards = listOf(SavedCard(cardId = 3, atk = 2100, def = 1800, bondLevel = 10)),
            party = listOf(SavedPartyMember(cardId = 3, slotIndex = 0, hp = 5000, maxHp = 5000)),
            lobbyHeroId = 3,
            lobbyVisual = VisualState.ROMANTIC_DATE.name,
            lobbyOutfits = mapOf(3 to VisualState.ROMANTIC_DATE.name)
        )
        val vm = TcgGameViewModel(
            random = Random(21),
            initialOwnedCardIds = listOf(3),
            saveStore = InMemorySaveStore(SaveCodec.encode(save))
        )
        val state = vm.uiState.value
        // 친밀도 10 → 전투복까지만 허용
        assertEquals(VisualState.BATTLE, state.lobbyVisual)
        assertEquals(VisualState.BATTLE, state.lobbyOutfits[3])
        assertEquals(AffinityRules.visualFor(10), VisualState.BATTLE)
    }

    @Test
    fun `카드 단위 복장 기억은 보유하지 않은 캐릭터를 복원하지 않는다`() {
        val save = GameSave(
            gold = 100,
            cards = listOf(SavedCard(cardId = 3, atk = 2100, def = 1800, bondLevel = 100)),
            party = listOf(SavedPartyMember(cardId = 3, slotIndex = 0, hp = 5000, maxHp = 5000)),
            lobbyHeroId = 3,
            lobbyVisual = VisualState.CASUAL_CUTE.name,
            lobbyOutfits = mapOf(
                3 to VisualState.CASUAL_CUTE.name,
                99 to VisualState.ROMANTIC_DATE.name
            )
        )
        val vm = TcgGameViewModel(
            random = Random(21),
            initialOwnedCardIds = listOf(3),
            saveStore = InMemorySaveStore(SaveCodec.encode(save))
        )
        val outfits = vm.uiState.value.lobbyOutfits
        assertEquals(VisualState.CASUAL_CUTE, outfits[3])
        assertFalse("미보유 캐릭터의 복장은 버린다", outfits.containsKey(99))
    }

    @Test
    fun `상태에서 저장 모델로 바꿀 때 복장 맵이 이름으로 직렬화된다`() {
        val vm = starterGame(bond = 100)
        val hero = vm.uiState.value.lobbyHeroId
        vm.selectLobbyVisual(VisualState.CASUAL_CUTE)
        val save = vm.uiState.value.toGameSave()
        assertEquals(VisualState.CASUAL_CUTE.name, save.lobbyOutfits[hero])
        assertEquals(VisualState.CASUAL_CUTE.name, save.lobbyVisual)
    }
}
