package com.game.tcg

import com.game.tcg.core.engine.IntroAdvancePolicy
import com.game.tcg.core.engine.IntroEffect
import com.game.tcg.core.engine.IntroSceneId
import com.game.tcg.core.engine.IntroScript
import com.game.tcg.core.engine.IntroStoryEngine
import com.game.tcg.ui.IntroArtRegistry
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도입부 스토리(4장면 컷신) — 2026-11-16 구현 검증
 *
 * 1) 대본: 우연한 발견 → 봉인 해제 → 이세계로의 연결(듀얼 게이트) → 소환과 시작
 * 2) 진행: 탭·자동 진행 4단계 → 마지막에서 종료, SKIP 즉시 종료, 로비 "다시보기"
 * 3) 앱 연결: 최초 실행 시 재생 · 완료/SKIP 후 메인 로비 진입 · 이미 본 인트로는 생략
 * 4) 리소스: 장면 4장 ↔ intro_scene_{1..4} 매핑
 */
class IntroStoryTest {

    private val engine = IntroStoryEngine()

    // ── 1. 대본 ───────────────────────────────────────────────────

    @Test
    fun `인트로는 4장면으로 구성되고 순서대로 이어진다`() {
        assertEquals(4, IntroScript.sceneCount)
        assertEquals(
            listOf(
                IntroSceneId.DISCOVERY,
                IntroSceneId.UNSEALING,
                IntroSceneId.GATE,
                IntroSceneId.SUMMON
            ),
            IntroScript.SCENES.map { it.id }
        )
        assertEquals(
            listOf("우연한 발견", "봉인 해제", "이세계로의 연결", "소환과 시작"),
            IntroScript.SCENES.map { it.title }
        )
    }

    @Test
    fun `각 장면은 제목 부제 본문을 비우지 않는다`() {
        IntroScript.SCENES.forEach { scene ->
            assertTrue("${scene.id} act 비어 있음", scene.act.isNotBlank())
            assertTrue("${scene.id} 제목 비어 있음", scene.title.isNotBlank())
            assertTrue("${scene.id} 부제 비어 있음", scene.subtitle.isNotBlank())
            assertTrue("${scene.id} 본문이 너무 짧음", scene.narration.length > 30)
        }
    }

    @Test
    fun `장면 연출은 4종이 각각 한 번씩 배치된다`() {
        assertEquals(
            IntroEffect.values().toList(),
            IntroScript.SCENES.map { it.effect }
        )
    }

    @Test
    fun `장면당 자동 진행 시간은 3_5초에서 4초 사이이고 전체는 20초를 넘지 않는다`() {
        IntroScript.SCENES.forEach { scene ->
            assertTrue(
                "${scene.title} 자동 진행 ${scene.autoAdvanceMillis}ms 는 너무 짧다",
                scene.autoAdvanceMillis >= IntroScript.MIN_AUTO_ADVANCE_MS
            )
            assertTrue(
                "${scene.title} 자동 진행 ${scene.autoAdvanceMillis}ms 는 너무 길다",
                scene.autoAdvanceMillis <= IntroScript.MAX_AUTO_ADVANCE_MS
            )
        }
        assertTrue(IntroScript.totalDurationMillis <= 20_000L)
    }

    // ── 2. 상태 전이 ───────────────────────────────────────────────

    @Test
    fun `최초 실행이면 1장면부터 자동 재생된다`() {
        val state = engine.initialState(alreadySeen = false)
        assertTrue(state.isPlaying)
        assertFalse(state.hasSeenIntro)
        assertEquals(0, state.sceneIndex)
        assertEquals(IntroSceneId.DISCOVERY, state.scene.id)
        assertEquals("1 / 4", state.stepLabel)
        assertFalse(state.isLastScene)
    }

    @Test
    fun `이미 본 인트로는 재생하지 않고 로비에서 시작한다`() {
        val state = engine.initialState(alreadySeen = true)
        assertFalse(state.isPlaying)
        assertTrue(state.hasSeenIntro)
    }

    @Test
    fun `탭으로 4장면을 차례로 넘기면 마지막 장면에서 인트로가 끝난다`() {
        var state = engine.initialState(alreadySeen = false)

        state = engine.advance(state)
        assertEquals(1, state.sceneIndex)
        assertEquals(IntroSceneId.UNSEALING, state.scene.id)
        assertTrue("장면 2에서 아직 재생 중이어야 한다", state.isPlaying)

        state = engine.advance(state)
        assertEquals(2, state.sceneIndex)
        assertEquals(IntroSceneId.GATE, state.scene.id)

        state = engine.advance(state)
        assertEquals(3, state.sceneIndex)
        assertEquals(IntroSceneId.SUMMON, state.scene.id)
        assertTrue("마지막 장면은 재생 중", state.isPlaying)
        assertTrue(state.isLastScene)
        assertEquals("4 / 4", state.stepLabel)

        // 마지막 장면에서 한 번 더 → 인트로 종료 (장면 위치는 유지)
        state = engine.advance(state)
        assertFalse(state.isPlaying)
        assertTrue(state.hasSeenIntro)
        assertFalse(state.skipped)
        assertEquals(3, state.sceneIndex)
    }

    @Test
    fun `4장면을 모두 넘기는 데 필요한 진행 횟수는 장면 수와 같다`() {
        var state = engine.initialState(alreadySeen = false)
        var advances = 0
        while (state.isPlaying && advances < 20) {
            state = engine.advance(state)
            advances++
        }
        assertEquals(IntroScript.sceneCount, advances)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `SKIP은 어느 장면에서든 즉시 종료하고 건너뜀으로 기록한다`() {
        val started = engine.initialState(alreadySeen = false)
        val afterSecondScene = engine.advance(started)

        val skipped = engine.skip(afterSecondScene)
        assertFalse(skipped.isPlaying)
        assertTrue(skipped.hasSeenIntro)
        assertTrue(skipped.skipped)
        assertEquals(1, skipped.sceneIndex)

        // 처음 장면에서 바로 SKIP
        val immediate = engine.skip(started)
        assertFalse(immediate.isPlaying)
        assertTrue(immediate.skipped)
        assertTrue(immediate.hasSeenIntro)
    }

    @Test
    fun `다시보기는 1장면부터 재생하고 다시보기 횟수를 센다`() {
        val finished = engine.skip(engine.initialState(alreadySeen = true))
        assertFalse(finished.isPlaying)

        val firstReplay = engine.replay(finished)
        assertTrue(firstReplay.isPlaying)
        assertEquals(0, firstReplay.sceneIndex)
        assertFalse(firstReplay.skipped)
        assertEquals(1, firstReplay.replayCount)
        assertTrue("이미 본 인트로라는 사실은 유지", firstReplay.hasSeenIntro)

        val secondReplay = engine.replay(engine.skip(firstReplay))
        assertEquals(2, secondReplay.replayCount)
        assertEquals(0, secondReplay.sceneIndex)
    }

    @Test
    fun `재생 중이 아닐 때 탭과 SKIP은 상태를 바꾸지 않는다`() {
        val idle = engine.initialState(alreadySeen = true)
        assertEquals(idle, engine.advance(idle))
        assertEquals(idle, engine.skip(idle))
    }

    @Test
    fun `진행 표기와 진행도가 단계에 맞게 계산된다`() {
        var state = engine.initialState(alreadySeen = false)
        assertEquals(0.25f, state.progress, 0.0001f)
        assertTrue(state.isStepPassed(0))
        assertFalse(state.isStepPassed(1))

        state = engine.advance(engine.advance(engine.advance(state)))
        assertEquals(1f, state.progress, 0.0001f)
        assertTrue((0..3).all { state.isStepPassed(it) })
    }

    // ── 3. 앱 연결 (ViewModel) ──────────────────────────────────────

    @Test
    fun `앱 최초 실행 시 인트로가 로비 위에서 재생된다`() {
        val vm = TcgGameViewModel()
        val state = vm.uiState.value
        assertTrue(state.isIntroPlaying)
        assertTrue(state.introStory.isPlaying)
        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
        assertEquals(0, state.introStory.sceneIndex)
    }

    @Test
    fun `인트로를 끝까지 넘기면 메인 로비로 진입한다`() {
        val vm = TcgGameViewModel()
        repeat(IntroScript.sceneCount) { vm.advanceIntro() }

        val state = vm.uiState.value
        assertFalse(state.isIntroPlaying)
        assertTrue(state.introStory.hasSeenIntro)
        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
        assertTrue(
            "완료 안내문: ${state.statusMessage}",
            state.statusMessage.contains("메인 로비")
        )
    }

    @Test
    fun `SKIP하면 즉시 메인 로비로 진입하고 건너뜀을 기록한다`() {
        val vm = TcgGameViewModel()
        vm.advanceIntro()
        vm.skipIntro()

        val state = vm.uiState.value
        assertFalse(state.isIntroPlaying)
        assertTrue(state.introStory.skipped)
        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
        assertTrue(
            "건너뜀 안내문: ${state.statusMessage}",
            state.statusMessage.contains("건너뛰")
        )
    }

    @Test
    fun `인트로 중 장면을 넘기면 상태 표시줄에 장면 이름이 나온다`() {
        val vm = TcgGameViewModel()
        vm.advanceIntro()
        val state = vm.uiState.value
        assertTrue(state.isIntroPlaying)
        assertTrue(
            "장면 안내문: ${state.statusMessage}",
            state.statusMessage.contains(IntroScript.SCENES[1].title)
        )
    }

    @Test
    fun `로비의 다시보기 버튼은 다른 화면에서도 인트로를 처음부터 재생한다`() {
        val vm = TcgGameViewModel()
        repeat(IntroScript.sceneCount) { vm.advanceIntro() }
        vm.openScreen(LobbyDestination.SCENARIO)

        vm.replayIntro()

        val state = vm.uiState.value
        assertTrue(state.isIntroPlaying)
        assertEquals(0, state.introStory.sceneIndex)
        assertEquals(1, state.introStory.replayCount)
        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
    }

    @Test
    fun `이미 본 인트로면 ViewModel이 로비에서 바로 시작한다`() {
        val vm = TcgGameViewModel(introAlreadySeen = true)
        val state = vm.uiState.value
        assertFalse(state.isIntroPlaying)
        assertTrue(state.introStory.hasSeenIntro)
        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
    }

    @Test
    fun `인트로는 로비의 게임 상태를 건드리지 않는다`() {
        val vm = TcgGameViewModel()
        val before = vm.uiState.value
        val goldBefore = before.gold
        val cardsBefore = before.ownedCards.map { it.id }
        val deckBefore = before.party.map { it.card.id }

        repeat(IntroScript.sceneCount + 1) { vm.advanceIntro() }

        val after = vm.uiState.value
        assertEquals(goldBefore, after.gold)
        assertEquals(cardsBefore, after.ownedCards.map { it.id })
        assertEquals(deckBefore, after.party.map { it.card.id })
        assertEquals(before.lobbyHeroId, after.lobbyHeroId)
    }

    @Test
    fun `인트로를 다시 봐도 재생 횟수 외에는 본 적 있음 상태가 유지된다`() {
        val vm = TcgGameViewModel()
        vm.skipIntro()
        assertTrue(vm.uiState.value.introStory.hasSeenIntro)

        vm.replayIntro()
        assertTrue(vm.uiState.value.introStory.hasSeenIntro)

        vm.skipIntro()
        assertTrue(vm.uiState.value.introStory.hasSeenIntro)
        assertEquals(1, vm.uiState.value.introStory.replayCount)
    }

    // ── 4. 일러스트 리소스 ─────────────────────────────────────────

    @Test
    fun `장면 4장은 각각 다른 intro_scene 리소스에 연결된다`() {
        assertEquals(IntroScript.sceneCount, IntroArtRegistry.size)
        val ids = (0 until IntroArtRegistry.size).map { IntroArtRegistry.resId(it) }
        assertEquals("리소스 id가 중복되었다", ids.size, ids.toSet().size)
        assertTrue(ids.all { it != 0 })

        IntroScript.SCENES.forEachIndexed { index, scene ->
            assertEquals(IntroArtRegistry.resId(index), IntroArtRegistry.resIdOf(scene.id))
            assertEquals("intro_scene_${index + 1}", scene.id.artName)
        }
    }

    // ── 5. 터치 진행 (2026-11-16 사용자 요청: 자동 재생 제거) ─────────

    @Test
    fun `인트로는 자동으로 넘어가지 않고 터치로만 진행된다`() {
        assertFalse("자동 넘김 금지", IntroAdvancePolicy.AUTO_ADVANCE_ENABLED)
        assertEquals("화면을 터치해 계속 ▶", IntroAdvancePolicy.TAP_PROMPT)
        assertEquals("⚔ 이계로 들어가기", IntroAdvancePolicy.START_PROMPT)

        var state = engine.initialState(alreadySeen = false)
        assertTrue(IntroAdvancePolicy.isWaitingForTap(state))
        assertEquals(IntroAdvancePolicy.TAP_PROMPT, IntroAdvancePolicy.prompt(state))

        // 한 번에 한 장면씩만 — 탭 1회 = 장면 +1
        state = engine.advance(state)
        assertEquals(1, state.sceneIndex)
        assertTrue(state.isPlaying)
        state = engine.advance(state)
        assertEquals(2, state.sceneIndex)
        state = engine.advance(state)
        assertEquals(3, state.sceneIndex)
        assertEquals(IntroAdvancePolicy.START_PROMPT, IntroAdvancePolicy.prompt(state))

        // 마지막 장면에서 탭하면 인트로 종료
        state = engine.advance(state)
        assertFalse(state.isPlaying)
        assertFalse(IntroAdvancePolicy.isWaitingForTap(state))
    }

    @Test
    fun `하단 안내는 장면 번호를 1부터 4까지 보여 준다`() {
        var state = engine.initialState(alreadySeen = false)
        (1..4).forEach { expected ->
            assertEquals("$expected/4", IntroAdvancePolicy.sceneCounter(state))
            state = engine.advance(state)
        }
    }

    @Test
    fun `ViewModel도 탭 호출 없이는 장면을 넘기지 않는다`() {
        val vm = TcgGameViewModel()
        val before = vm.uiState.value.introStory.sceneIndex
        assertEquals(before, vm.uiState.value.introStory.sceneIndex)
        // 탭(=advanceIntro) 전에는 그대로 0장면
        assertEquals(0, vm.uiState.value.introStory.sceneIndex)
        vm.advanceIntro()
        assertEquals(1, vm.uiState.value.introStory.sceneIndex)
    }

    @Test
    fun `범위를 벗어난 장면 인덱스는 마지막 장면으로 보정된다`() {
        assertEquals(IntroScript.SCENES.last(), IntroScript.sceneAt(9))
        assertEquals(IntroScript.SCENES.first(), IntroScript.sceneAt(-3))
        assertEquals(
            IntroArtRegistry.resId(IntroScript.lastIndex),
            IntroArtRegistry.resId(42)
        )
        assertNotEquals(IntroArtRegistry.resId(0), IntroArtRegistry.resId(IntroScript.lastIndex))
    }
}
