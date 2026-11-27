package com.game.tcg

import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.DiamondShopEngine
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.DeckPactTab
import com.game.tcg.core.model.ScreenComfort
import com.game.tcg.core.model.VisualState
import com.game.tcg.persistence.InMemorySaveStore
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * TCG v0.6.0 — 2026-11-27 사용자 요청 검증.
 *
 * ① 💎 다이아 재화 사용처 4종(지정 소환 5 · 충전 3 · 교환 2 · 데이트 룩 4)
 * ② 💎로 연 데이트 룩이 친밀도 규칙을 이기고 로비·도감·덱에 함께 적용되는지
 * ③ DECK › 서약·정화의 아이콘 탭 3분할 + 전 화면 하단 클릭 여유 상수
 * ④ 💎 지출이 저장(재실행)을 넘어 유지되는지
 *
 * (Compose 화면은 JVM 테스트에서 그리지 않는다 — 화면이 쓰는 엔진·상태·상수를 검증한다.)
 */
class DiamondShopAndDeckTabTest {

    private fun game(
        gems: Int = 20,
        owned: List<Int> = listOf(3, 1, 2),
        bond: Int = 0,
        store: InMemorySaveStore? = null
    ) = TcgGameViewModel(
        random = Random(11),
        initialOwnedCardIds = owned,
        initialBondLevel = bond,
        initialGems = gems,
        todayEpochDay = { 20_000 },
        saveStore = store
    )

    // ── 1. ✨ 지정 소환 ─────────────────────────────────────────────

    @Test
    fun `지정 소환은 미보유 캐릭터를 다이아 5로 영입한다`() {
        val vm = game(gems = 20)
        val targetId = 5
        assertTrue(CardCatalog.find(targetId) != null)
        assertFalse("전제: 대상은 미보유", vm.uiState.value.ownedCards.any { it.id == targetId })

        vm.selectDiamondShopTarget(targetId)
        val purchase = vm.buyTargetSummon()

        assertNotNull(purchase)
        assertTrue(purchase!!.isSuccess)
        assertEquals(DiamondShopEngine.PRICE_TARGET_SUMMON, purchase.gemsSpent)
        val after = vm.uiState.value
        assertEquals("다이아 5 차감", 15, after.gems)
        assertTrue("보유 카드에 추가", after.ownedCards.any { it.id == targetId })
        assertEquals("영입 후 대상 선택은 비운다", 0, after.diamondShopTargetId)
        assertTrue(after.diamondShopMessage?.contains("지정 소환") == true)
    }

    @Test
    fun `지정 소환은 보유 캐릭터와 다이아 부족을 거절한다`() {
        val vm = game(gems = 4)
        vm.selectDiamondShopTarget(1)
        val owned = vm.buyTargetSummon()
        assertFalse(owned!!.isSuccess)
        assertTrue(owned.failureReason!!.contains("이미 보유"))
        assertEquals("실패에는 다이아를 쓰지 않는다", 4, vm.uiState.value.gems)

        vm.selectDiamondShopTarget(5)
        val poor = vm.buyTargetSummon()
        assertFalse(poor!!.isSuccess)
        assertTrue(poor.failureReason!!.contains("💎 5 필요"))
        assertFalse(vm.uiState.value.ownedCards.any { it.id == 5 })
    }

    @Test
    fun `중간 보스는 지정 소환 대상이 아니다`() {
        val midBoss = CardCatalog.midBosses().first()
        val purchase = DiamondShopEngine.summonTarget(
            record = midBoss,
            ownedIds = emptyList(),
            gems = 99
        )
        assertFalse(purchase.isSuccess)
        assertTrue(purchase.failureReason!!.contains("중간 보스"))
    }

    // ── 2. 🔋 충전 ─────────────────────────────────────────────────

    @Test
    fun `에너지 충전은 가득 차면 거절하고 최대치를 넘기지 않는다`() {
        val full = DiamondShopEngine.rechargeEnergy(
            gems = 30,
            currentEnergy = TcgGameViewModel.GATE_ENERGY_MAX,
            energyMax = TcgGameViewModel.GATE_ENERGY_MAX
        )
        assertFalse(full.isSuccess)
        assertTrue(full.failureReason!!.contains("가득"))

        val almost = DiamondShopEngine.rechargeEnergy(
            gems = 30,
            currentEnergy = TcgGameViewModel.GATE_ENERGY_MAX - 1,
            energyMax = TcgGameViewModel.GATE_ENERGY_MAX
        )
        assertTrue(almost.isSuccess)
        assertEquals("최대치를 넘지 않는다", 1, almost.energyGain)
        assertEquals(DiamondShopEngine.PRICE_RECHARGE, almost.gemsSpent)
    }

    @Test
    fun `성수 충전은 다이아 3으로 성수 2를 준다`() {
        val vm = game(gems = 10)
        val before = vm.uiState.value.holyWater
        vm.openDiamondShop(DiamondShopEngine.Tab.RECHARGE.ordinal)
        val purchase = vm.rechargeWithGems(DiamondShopEngine.RechargeKind.HOLY_WATER)

        assertTrue(purchase!!.isSuccess)
        val after = vm.uiState.value
        assertEquals(7, after.gems)
        assertEquals(before + DiamondShopEngine.HOLY_WATER_GAIN, after.holyWater)
    }

    // ── 3. 🎁 교환 ─────────────────────────────────────────────────

    @Test
    fun `강화석 교환은 그 캐릭터의 조각으로 들어간다`() {
        val vm = game(gems = 6)
        vm.selectDiamondShopTarget(1)
        val purchase = vm.exchangeWithGems(DiamondShopEngine.ExchangeKind.UPGRADE_SHARD)

        assertTrue(purchase!!.isSuccess)
        val after = vm.uiState.value
        assertEquals(4, after.gems)
        assertEquals(DiamondShopEngine.SHARD_GAIN, after.shards.of(1))
    }

    @Test
    fun `친밀도 선물은 최대 100을 넘지 않는다`() {
        val open = DiamondShopEngine.giftAffinity(
            card = CardCatalog.require(1).toCard(bondLevel = 97),
            bond = 97,
            gems = 10
        )
        assertTrue(open.isSuccess)
        assertEquals("100 에서 멈춘다", 3, open.bondGain)

        val max = DiamondShopEngine.giftAffinity(
            card = CardCatalog.require(1).toCard(bondLevel = 100),
            bond = 100,
            gems = 10
        )
        assertFalse(max.isSuccess)
    }

    // ── 4. 🌹 데이트 룩 즉시 해금 ───────────────────────────────────

    @Test
    fun `다이아로 데이트 룩을 열면 친밀도 0에서도 로비 복장으로 쓸 수 있다`() {
        val vm = game(gems = 10, bond = 0)
        vm.selectLobbyHero(1)
        // 열기 전에는 잠겨 있다
        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertTrue(vm.uiState.value.visualLockMessage != null)

        vm.selectDiamondShopTarget(1)
        val purchase = vm.unlockDateLookWithGems()
        assertTrue(purchase!!.isSuccess)
        assertEquals(DiamondShopEngine.PRICE_DATE_LOOK, purchase.gemsSpent)

        val after = vm.uiState.value
        assertEquals(6, after.gems)
        assertTrue("해금 목록에 남는다", after.isDateLookUnlockedByGem(1))
        assertTrue("복장 목록에 데이트 룩이 열린다", after.unlockedOutfitVisuals(1).contains(VisualState.ROMANTIC_DATE))
        assertEquals("해금 직후 그 캐릭터는 데이트 룩", VisualState.ROMANTIC_DATE, after.outfitVisual(1))

        // 이제 잠금 없이 고를 수 있다
        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertNull(vm.uiState.value.visualLockMessage)
        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.lobbyVisual)
    }

    @Test
    fun `친밀도 70 이상이거나 이미 열린 캐릭터에게는 데이트 룩을 팔지 않는다`() {
        val bond70 = DiamondShopEngine.unlockDateLook(
            card = CardCatalog.require(1).toCard(bondLevel = 70),
            bond = 70,
            alreadyUnlocked = false,
            gems = 20
        )
        assertFalse(bond70.isSuccess)

        val already = DiamondShopEngine.unlockDateLook(
            card = CardCatalog.require(1).toCard(bondLevel = 0),
            bond = 0,
            alreadyUnlocked = true,
            gems = 20
        )
        assertFalse(already.isSuccess)
    }

    @Test
    fun `해금 판정은 그 캐릭터에만 적용된다`() {
        val unlocked = setOf(1)
        assertTrue(
            DiamondShopEngine.isVisualUnlocked(1, VisualState.ROMANTIC_DATE, 0, unlocked)
        )
        assertFalse(
            "번호가 다른 캐릭터에는 열리지 않는다",
            DiamondShopEngine.isVisualUnlocked(2, VisualState.ROMANTIC_DATE, 0, unlocked)
        )
        assertFalse(
            "다이아 해금 목록이 없으면 기존 친밀도 규칙 그대로",
            DiamondShopEngine.isVisualUnlocked(1, VisualState.ROMANTIC_DATE, 0, emptySet())
        )
        assertTrue(
            "친밀도 70 은 다이아 없이도 열린다",
            DiamondShopEngine.isVisualUnlocked(9, VisualState.ROMANTIC_DATE, AffinityRules.DATE_UNLOCK_BOND, emptySet())
        )
    }

    // ── 5. 저장(재실행) 유지 ────────────────────────────────────────

    @Test
    fun `다이아 구매 결과는 앱을 다시 켜도 유지된다`() {
        val store = InMemorySaveStore()
        val first = game(gems = 20, store = store)
        first.selectDiamondShopTarget(5)
        assertTrue(first.buyTargetSummon()!!.isSuccess)
        first.selectDiamondShopTarget(1)
        assertTrue(first.unlockDateLookWithGems()!!.isSuccess)

        // 같은 저장소로 새 게임을 열면 이전 진행을 복원한다
        val second = TcgGameViewModel(
            random = Random(11),
            initialOwnedCardIds = listOf(3, 1, 2),
            introAlreadySeen = true,
            todayEpochDay = { 20_000 },
            saveStore = store
        )
        val restored = second.uiState.value
        assertEquals("다이아도 그대로", 11, restored.gems)
        assertTrue("영입한 캐릭터 유지", restored.ownedCards.any { it.id == 5 })
        assertTrue("💎 해금 데이트 룩 유지", restored.isDateLookUnlockedByGem(1))
        assertEquals(VisualState.ROMANTIC_DATE, restored.outfitVisual(1))
    }

    // ── 6. 화면 구성 규칙 ──────────────────────────────────────────

    @Test
    fun `다이아 상점 탭은 가격 순서대로 4종이다`() {
        val tabs = DiamondShopEngine.Tab.entries
        assertEquals(4, tabs.size)
        assertEquals(
            listOf(
                DiamondShopEngine.PRICE_TARGET_SUMMON,
                DiamondShopEngine.PRICE_RECHARGE,
                DiamondShopEngine.PRICE_EXCHANGE,
                DiamondShopEngine.PRICE_DATE_LOOK
            ),
            tabs.map { it.price }
        )
        assertEquals(listOf("✨", "🔋", "🎁", "🌹"), tabs.map { it.icon })
    }

    @Test
    fun `DECK 서약 정화는 아이콘 탭 3분할이다`() {
        val tabs = DeckPactTab.entries
        assertEquals(3, tabs.size)
        assertEquals(listOf("⚔️", "😈", "⭐"), tabs.map { it.icon })
        assertEquals(listOf("출전 덱", "마왕 정화", "대표"), tabs.map { it.label })
        assertEquals("처음엔 출전 덱", DeckPactTab.LINEUP, DeckPactTab.DEFAULT)
        assertEquals(tabs.size, DeckPactTab.ICON_TABS.size)
    }

    @Test
    fun `전 화면 하단 클릭 여유는 최소값 이상이다`() {
        assertTrue(ScreenComfort.COMPACT_FRAME_BOTTOM_MARGIN_DP >= ScreenComfort.MIN_BOTTOM_MARGIN_DP)
        assertTrue(ScreenComfort.SCROLL_FRAME_BOTTOM_MARGIN_DP >= ScreenComfort.MIN_BOTTOM_MARGIN_DP)
        assertTrue("내비게이션 바 인셋 위 여유도 양수", ScreenComfort.NAV_BAR_EXTRA_MARGIN_DP > 0)
    }
}
