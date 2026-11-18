package com.game.tcg

import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.model.VisualState
import com.game.tcg.ui.ExplorationStaging
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 탐색 진행 연출 검증 — 2026-11-18 사용자 요청.
 *
 * > "탐색 진행 화면에서 텍스트만 표시되지 않도록, 일상복 차림(평상복/캐주얼 페어) 캐릭터
 * >  일러스트가 화면에 함께 나타나면서 탐색이 진행되도록 UI와 연출을 구현한다."
 */
class ExplorationStagingTest {

    @Test
    fun `연출은 일상복 비주얼을 쓴다`() {
        assertEquals(VisualState.CASUAL_CUTE, ExplorationStaging.stagingVisual)
        assertEquals("일상 사복 큐트", ExplorationStaging.stagingVisual.displayName)
    }

    @Test
    fun `세 구역 모두 진행 단계가 순서대로 차오른다`() {
        ExplorationMode.entries.forEach { mode ->
            val steps = ExplorationStaging.steps(mode)
            assertTrue("${mode.displayName} 단계 3개 이상", steps.size >= 3)
            assertEquals("첫 단계는 0보다 크다", true, steps.first().progress > 0f)
            assertEquals("마지막 단계는 100%", 1.0f, steps.last().progress, 0.0001f)
            // 진행률이 단조 증가
            steps.zipWithNext().forEach { (a, b) ->
                assertTrue("${mode.displayName} 진행률 증가", b.progress > a.progress)
            }
            assertTrue("${mode.displayName} 첫 단계에서 동료 등장", steps.first().revealCompanion)
            assertTrue("${mode.displayName} 문구가 비어 있지 않다", steps.all { it.message.isNotBlank() })
        }
    }

    @Test
    fun `진행률에 맞는 단계 문구가 나온다`() {
        val mode = ExplorationMode.RIFT
        val steps = ExplorationStaging.steps(mode)
        assertEquals(steps.first().message, ExplorationStaging.messageAt(mode, 0.05f))
        assertEquals(steps[1].message, ExplorationStaging.messageAt(mode, 0.5f))
        assertEquals(steps.last().message, ExplorationStaging.messageAt(mode, 1.0f))
        // 범위 밖 값도 안전
        assertEquals(steps.first().message, ExplorationStaging.messageAt(mode, -1f))
        assertEquals(steps.last().message, ExplorationStaging.messageAt(mode, 9f))
    }

    @Test
    fun `연출에는 전열 캐릭터부터 최대 3명이 선다`() {
        val party = listOf(5 to false, 2 to true, 9 to true, 4 to false)
        val ids = ExplorationStaging.companionIds(party, fallbackCardId = 1)
        assertEquals(3, ids.size)
        assertEquals("전열이 먼저", listOf(2, 9), ids.take(2))
        assertTrue("중복 없음", ids.distinct().size == ids.size)
    }

    @Test
    fun `파티가 비어 있으면 대표 캐릭터가 선다`() {
        val ids = ExplorationStaging.companionIds(emptyList(), fallbackCardId = 3)
        assertEquals(listOf(3), ids)
    }

    @Test
    fun `실제 게임 파티로도 일상복 연출 인원이 만들어진다`() {
        val vm = TcgGameViewModel(random = Random(21))
        val state = vm.uiState.value
        val ids = ExplorationStaging.companionIds(
            party = state.party.map { it.card.id to it.isFrontRow },
            fallbackCardId = state.lobbyHeroId
        )
        assertTrue("최소 1명", ids.isNotEmpty())
        assertTrue("최대 3명", ids.size <= ExplorationStaging.MAX_COMPANIONS)
        ids.forEach { id ->
            assertTrue("도감에 있는 캐릭터", TcgGameViewModel.profileOf(id) != null)
        }
    }

    @Test
    fun `연출 길이는 2초 안팎이다`() {
        assertTrue("연출 1.5초 이상", ExplorationStaging.DURATION_MILLIS >= 1500)
        assertTrue("연출 4초 미만", ExplorationStaging.DURATION_MILLIS < 4000)
    }

    @Test
    fun `탐색은 연출이 끝난 뒤 결과가 반영된다`() {
        // 엔진은 그대로 — 연출은 UI 계층에서만 시간을 쓴다 (결과·에너지 규칙 불변)
        val vm = TcgGameViewModel(random = Random(2))
        val before = vm.uiState.value
        val result = vm.explore(ExplorationMode.RIFT)
        val after = vm.uiState.value
        assertTrue("탐색 결과가 생성된다", result != null)
        assertEquals("에너지 1 소모", before.gateEnergy - 1, after.gateEnergy)
        assertTrue("탐색 기록", after.explorationLog.size >= before.explorationLog.size)
        assertFalse("연출용 상태는 게임 상태에 없다", after.statusMessage.isBlank())
    }
}
