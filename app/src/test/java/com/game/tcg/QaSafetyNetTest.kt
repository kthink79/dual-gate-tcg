package com.game.tcg

import com.game.tcg.audio.AudioManager
import com.game.tcg.audio.AudioScene
import com.game.tcg.audio.BgmTrack
import com.game.tcg.audio.InMemoryAudioPrefs
import com.game.tcg.core.engine.EmergencySupplyEngine
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.PackCatalog
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 2026-11-16 QA 보완 검증.
 *
 * 1) 로그라이크 「영구 상실」 규칙이 실제로 지켜지는지 — 사망한 카드는 편성에서도 빠지고
 *    다음 전투에서 되살아나지 않는다.
 * 2) 무료 재계약(환생 풀) · 무료 긴급 보급으로 **소프트락**(보유 0장 + 팩/티켓/연성 불가)이
 *    발생하지 않는지.
 * 3) 오디오 — BGM 을 껐다 켜면 마지막 장면이 즉시 되살아나는지.
 * 4) 아트 레지스트리 — 176명 × 3비주얼 528장이 실제 리소스와 1:1 대응하는지(등록 누락 0).
 */
class QaSafetyNetTest {

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private fun emptyViewModel(): TcgGameViewModel = TcgGameViewModel(
        random = Random(11),
        initialOwnedCardIds = emptyList(),
        initialGold = 0,
        initialGems = 0,
        initialPackTickets = 0
    )

    private fun starterViewModel(): TcgGameViewModel = TcgGameViewModel(
        random = Random(42),
        initialOwnedCardIds = listOf(3, 1, 2),
        initialGold = 0,
        initialPackTickets = 0
    )

    // ======================================================================
    // 1. 영구 상실 — 편성에서도 제거되고 다음 전투에서 되살아나지 않는다
    // ======================================================================

    @Test
    fun `사망한 카드는 보유 목록과 편성에서 동시에 빠진다`() {
        val vm = starterViewModel()
        vm.startDemonLordBattle()

        var guard = 0
        while (vm.uiState.value.reincarnationPool.isEmpty() && guard < 30) {
            vm.nextTurn()
            guard++
        }

        val state = vm.uiState.value
        assertEquals("한 장은 상실된다", 1, state.reincarnationPool.size)
        val lostId = state.reincarnationPool.first().templateId

        assertTrue("보유 목록에서 제거", state.ownedCards.none { it.id == lostId })
        assertTrue("편성에서도 제거(영구 상실)", state.party.none { it.card.id == lostId })
        assertTrue("대기열에도 남지 않는다", state.battleReserve.none { it.id == lostId })
        assertEquals("보유 2장", 2, state.ownedCards.size)
    }

    @Test
    fun `상실된 카드는 다음 전투에서 되살아나지 않는다`() {
        val vm = starterViewModel()
        vm.startDemonLordBattle()

        var guard = 0
        while (vm.uiState.value.reincarnationPool.isEmpty() && guard < 30) {
            vm.nextTurn()
            guard++
        }
        val lostId = vm.uiState.value.reincarnationPool.first().templateId

        // 새 원정을 시작해도 편성에 남아 있지 않다
        vm.startFieldBattle()
        val party = vm.uiState.value.party
        assertTrue("상실 카드 미복귀", party.none { it.card.id == lostId })
        assertTrue("생존 카드 HP 는 만땅으로 회복", party.all { it.hp == it.maxHp && it.hp > 0 })
        assertEquals("편성 수 = 보유 수(2장)", 2, party.size)
    }

    @Test
    fun `무료 재계약으로 잃은 카드를 전부 되찾는다`() {
        val vm = starterViewModel()
        vm.startDemonLordBattle()

        var guard = 0
        while (vm.uiState.value.reincarnationPool.isEmpty() && guard < 30) {
            vm.nextTurn()
            guard++
        }
        val lostId = vm.uiState.value.reincarnationPool.first().templateId
        val goldBefore = vm.uiState.value.gold

        val reclaimed = vm.reclaimAllLostCards()

        assertEquals(1, reclaimed)
        assertTrue("보유 목록 복귀", vm.uiState.value.ownedCards.any { it.id == lostId })
        assertTrue("환생 풀 비었음", vm.uiState.value.reincarnationPool.isEmpty())
        assertEquals("재계약은 무료", goldBefore, vm.uiState.value.gold)
        assertTrue("편성에도 복귀", vm.uiState.value.party.any { it.card.id == lostId })
    }

    // ======================================================================
    // 2. 소프트락 안전장치 — 무료 긴급 보급
    // ======================================================================

    @Test
    fun `보유 0장 골드 0 티켓 0이면 긴급 보급이 열린다`() {
        val vm = emptyViewModel()
        val state = vm.uiState.value

        assertEquals(0, state.ownedCards.size)
        assertTrue("보급 대상", state.supplyOffer.isOffered)
        assertEquals("3장 지급", 3, state.supplyOffer.cardIds.size)
        assertEquals(0, state.supplyOffer.ownedBefore)
        assertEquals(3, state.supplyOffer.ownedAfter)
        assertTrue("로비 경보에 노출", state.lobbyAlerts.any { it.label.contains("긴급 보급") })
    }

    @Test
    fun `보급을 받으면 3장을 확보해 다시 출전할 수 있다`() {
        val vm = emptyViewModel()

        val offer = vm.claimEmergencySupply()
        assertNotNull(offer)
        assertTrue(offer!!.isOffered)

        val state = vm.uiState.value
        assertEquals(3, state.ownedCards.size)
        assertEquals("3장 모두 서로 다른 카드", 3, state.ownedCardIds.size)
        assertEquals("전열 3칸에 자동 배치", 3, state.party.size)
        assertTrue("보급 사용 1회 기록", state.supplyClaims == 1)
        assertTrue("보급 카드는 모두 미보유 상태에서 지급", offer.cardIds.none { it in setOf(3, 1, 2) })
        assertTrue("스테이지 출전 가능", vm.startStageBattle(1))
    }

    @Test
    fun `골드가 있으면 보급하지 않고 카드팩을 먼저 쓰게 한다`() {
        val vm = TcgGameViewModel(
            random = Random(3),
            initialOwnedCardIds = emptyList(),
            initialGold = PackCatalog.STANDARD.goldCost,
            initialPackTickets = 0
        )
        val offer = vm.uiState.value.supplyOffer
        assertFalse("정상 구매 경로가 있으면 지급 안 함", offer.isOffered)
        assertTrue(offer.reason.contains("카드팩"))
    }

    @Test
    fun `티켓만 있어도 보급하지 않는다`() {
        val vm = TcgGameViewModel(
            random = Random(3),
            initialOwnedCardIds = emptyList(),
            initialGold = 0,
            initialPackTickets = 1
        )
        assertFalse(vm.uiState.value.supplyOffer.isOffered)
    }

    @Test
    fun `환생 풀이 남아 있으면 보급 대신 무료 재계약을 안내한다`() {
        val vm = starterViewModel()
        vm.startDemonLordBattle()
        var guard = 0
        while (vm.uiState.value.reincarnationPool.isEmpty() && guard < 30) {
            vm.nextTurn()
            guard++
        }

        val state = vm.uiState.value
        assertFalse("재계약 경로가 있으면 보급 안 함", state.supplyOffer.isOffered)
        assertTrue(state.supplyOffer.reason.contains("재계약"))
        // 실제로 지급 요청을 해도 아무 일도 일어나지 않는다
        vm.claimEmergencySupply()
        assertEquals("카드는 늘지 않는다", 2, vm.uiState.value.ownedCards.size)
    }

    @Test
    fun `보급은 한 판 최대 횟수를 넘지 않는다`() {
        val owned = emptySet<Int>()
        val offer = EmergencySupplyEngine.evaluate(
            ownedCardIds = owned,
            lostCardIds = emptySet(),
            gold = 0,
            packTickets = 0,
            shardTotal = 0,
            claimsUsed = EmergencySupplyEngine.MAX_CLAIMS
        )
        assertFalse("한도 초과 시 지급 없음", offer.isOffered)
        assertTrue(offer.reason.contains("한도"))
        assertEquals(0, offer.claimsLeft)
    }

    @Test
    fun `조각 연성이 가능하면 보급하지 않는다`() {
        val offer = EmergencySupplyEngine.evaluate(
            ownedCardIds = setOf(3),
            lostCardIds = emptySet(),
            gold = 500,
            packTickets = 0,
            shardTotal = com.game.tcg.core.engine.AdvancedSynthesisEngine.CRAFT_SHARDS
        )
        assertFalse(offer.isOffered)
        assertTrue(offer.reason.contains("연성"))
    }

    @Test
    fun `지원 카드는 미보유 카드 중에서만 고른다`() {
        val owned = (3..40).toSet()
        val cards = EmergencySupplyEngine.pickSupportCards(owned, emptySet(), 3)
        assertEquals(3, cards.size)
        assertTrue("이미 보유한 카드는 지급하지 않는다", cards.none { it in owned })
        assertEquals("같은 상태엔 같은 결과", cards, EmergencySupplyEngine.pickSupportCards(owned, emptySet(), 3))
    }

    // ======================================================================
    // 3. 출전 가드 — 보유 0장이면 전투·탐색을 시작하지 않는다
    // ======================================================================

    @Test
    fun `보유 0장이면 원정과 마왕전이 시작되지 않는다`() {
        val vm = emptyViewModel()

        vm.startFieldBattle()
        assertEquals("적 HP 는 그대로", 12000, vm.uiState.value.enemyHp)
        assertTrue(vm.uiState.value.statusMessage.contains("출전 불가"))

        vm.startDemonLordBattle()
        assertEquals("마왕전 미시작", 12000, vm.uiState.value.enemyHp)
        assertTrue(vm.uiState.value.statusMessage.contains("출전 불가"))

        assertNull("탐색도 막힌다", vm.explore(com.game.tcg.core.engine.ExplorationMode.RIFT))
        assertTrue(vm.uiState.value.statusMessage.contains("탐색 불가"))

        assertFalse("스테이지 출전도 실패", vm.startStageBattle(1))
    }

    // ======================================================================
    // 4. 아트 레지스트리 — 199명 × 4비주얼, 누락 0
    // ======================================================================

    @Test
    fun `아트 레지스트리는 플레이 193명과 중간 보스 6명을 모두 덮는다`() {
        val registry = com.game.tcg.ui.CharacterArtRegistry
        val roster = CardCatalog.ALL.map { it.id }.toSet()
        val bosses = com.game.tcg.core.model.MidBossCatalog.ALL.map { it.id }.toSet()

        assertEquals("199명", 199, registry.registeredCardCount)
        assertEquals(roster + bosses, registry.registeredCardIds)
        (roster + bosses).forEach { id ->
            assertTrue("#$id 아트 등록", registry.hasArt(id))
            assertTrue(
                "#$id 3비주얼 모두 존재",
                com.game.tcg.core.model.VisualState.entries.all { registry.resId(id, it) != null }
            )
        }
        assertEquals("등록 199명 × 4 = 796장", 796, registry.registeredCardCount * 4)
    }

    /** 편성된 모든 카드는 반드시 보유 목록에 있어야 한다 (상태 일관성 불변식) */
    private fun assertPartyOwned(vm: TcgGameViewModel, step: String) {
        val state = vm.uiState.value
        val owned = state.ownedCardIds
        val outsiders = state.party.filter { it.card.id !in owned }.map { it.card.id }
        assertTrue("$step — 편성에 미보유 카드가 없다: $outsiders", outsiders.isEmpty())
        assertEquals("$step — 같은 카드가 두 슬롯에 없다", state.party.size, state.party.map { it.card.id }.toSet().size)
    }

    @Test
    fun `시작 3장부터 스테이지 카드팩 성장 소환 인트로 다시보기까지 끊기지 않는다`() {
        val vm = TcgGameViewModel(
            random = Random(9),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 3000,
            initialPackTickets = 1
        )
        assertPartyOwned(vm, "시작")
        assertEquals(3, vm.uiState.value.ownedCards.size)

        // 1) 덱 자동 편성 -> 2) 1스테이지 출전 -> 승리
        vm.autoEquipParty()
        assertPartyOwned(vm, "자동 편성")
        assertTrue(vm.startStageBattle(1))
        var guard = 0
        while (vm.uiState.value.enemyHp > 0 && guard < 200) {
            vm.attackFront()
            vm.nextTurn()
            guard++
        }
        assertTrue("1스테이지 클리어", 1 in vm.uiState.value.stageCleared)
        assertPartyOwned(vm, "1스테이지 클리어")

        // 3) 카드팩 개봉으로 동료 확보
        vm.openPack("pack_standard")
        assertPartyOwned(vm, "카드팩 개봉")

        // 4) 잃은 카드가 있으면 무료 재계약으로 전부 복구한다
        if (vm.uiState.value.reincarnationPool.isNotEmpty()) {
            vm.reclaimAllLostCards()
            assertPartyOwned(vm, "무료 재계약")
            assertTrue(vm.uiState.value.reincarnationPool.isEmpty())
            vm.autoEquipParty()
        }

        // 5) 현실 소환 · 6) 인트로 다시보기는 로비 상태를 깨지 않는다
        val summonTarget = vm.uiState.value.summonCard
        assertNotNull("소환 대상이 있다", summonTarget)
        vm.summonToReality(summonTarget!!.id)
        assertPartyOwned(vm, "현실 소환")

        val before = vm.uiState.value
        vm.replayIntro()
        assertTrue("인트로 다시보기 시작", vm.uiState.value.isIntroPlaying)
        assertEquals("보유 카드는 그대로", before.ownedCards.size, vm.uiState.value.ownedCards.size)
        assertEquals("골드는 그대로", before.gold, vm.uiState.value.gold)
        assertPartyOwned(vm, "인트로 다시보기")
    }

    // ======================================================================
    // 5. 오디오 — BGM 토글 복구
    // ======================================================================

    @Test
    fun `BGM 을 껐다 켜면 마지막 장면이 즉시 되살아난다`() {
        val player = RecordingAudioPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())

        audio.updateScene(AudioScene.LOBBY)
        assertEquals(listOf(BgmTrack.LOBBY), player.plays)

        audio.toggleBgm()               // 끄기 — 재생 중단
        audio.updateScene(AudioScene.BATTLE)  // 음소거 중 장면 변경은 소리 없음
        assertEquals(1, player.plays.size)

        audio.toggleBgm()               // 켜기 — refresh() 를 부르지 않아도 복구되어야 한다
        assertEquals(listOf(BgmTrack.LOBBY, BgmTrack.BATTLE), player.plays)
    }

    /** BGM/SFX 재생을 기록만 하는 테스트 더블 */
    private class RecordingAudioPlayer : com.game.tcg.audio.AudioPlayer {
        val plays = mutableListOf<BgmTrack>()
        private var enabled = true
        private var current: BgmTrack? = null

        override fun playBgm(track: BgmTrack) {
            if (!enabled) return
            if (current == track) return
            current = track
            plays += track
        }

        override fun playScene(scene: AudioScene) {
            scene.track?.let { playBgm(it) } ?: stopBgm()
        }

        override fun stopBgm() { current = null }
        override fun pauseBgm() = Unit
        override fun resumeBgm() { current?.let { playBgm(it) } }
        override fun currentTrack(): BgmTrack? = current
        override fun setBgmVolume(volume: Float) = Unit
        override fun setBgmEnabled(enabled: Boolean) { this.enabled = enabled }
        override fun playSfx(effect: com.game.tcg.audio.SfxEffect) = Unit
        override fun setSfxVolume(volume: Float) = Unit
        override fun setSfxEnabled(enabled: Boolean) = Unit
        override fun release() = Unit
    }
}
