package com.game.tcg

import com.game.tcg.ui.InfoTone
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.LobbyInfoPanel
import com.game.tcg.ui.TcgGameState
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 메인 로비 정보 과다 정리 + 「📊 정보」 패널 — 2026-11-16 사용자 요청 검증
 *
 * 1) 로비 상단은 재화/차원 요약 1줄 + 📊 정보 버튼만 (정보 4줄 → 1줄)
 * 2) 스테이지·덱 전투력·보유 인원·정신 상태·BGM/SFX·자동 저장은 패널 안에
 * 3) 패널은 열기/닫기/토글이 되고, 다른 화면으로 이동하면 닫힌다
 * 4) 인트로 다시보기·새 게임은 패널의 동작 버튼으로 이동
 */
class LobbyInfoPanelTest {

    private fun newState(): TcgGameState = TcgGameViewModel(
        initialOwnedCardIds = listOf(3, 1, 2),
        initialGold = 1_200,
        initialGems = 30
    ).uiState.value

    // ── 1. 상단 1줄 요약 ──────────────────────────────────────────

    @Test
    fun `로비 최상단 요약은 차원과 재화 3종만 담는다`() {
        val state = newState()
        val summary = LobbyInfoPanel.topSummary(state)

        assertEquals(4, summary.size)
        assertEquals(listOf("⚡", "🪙", "💎", "🎟"), summary.map { it.icon })
        assertEquals("${state.gateEnergy}/${state.gateEnergyMax}", summary[0].value)
        assertEquals("1200G", summary[1].value)
        assertEquals("30", summary[2].value)
        assertEquals("${state.packTickets}", summary[3].value)
        assertEquals(
            "⚡ ${state.gateEnergy}/${state.gateEnergyMax} · 🪙 1200G · 💎 30 · 🎟 ${state.packTickets}",
            LobbyInfoPanel.topSummaryLine(state)
        )
    }

    @Test
    fun `요약 1줄에는 스테이지 덱 보유 인원 정신 상태가 들어가지 않는다`() {
        val line = LobbyInfoPanel.topSummaryLine(newState())
        listOf("스테이지", "덱", "보유", "홀림", "자동 저장", "인트로").forEach { banned ->
            assertFalse("상단 요약에 '$banned' 가 남아 있다", line.contains(banned))
        }
    }

    // ── 2. 패널 내용 ─────────────────────────────────────────────

    @Test
    fun `정보 패널은 진행도 정신 상태 설정 세 묶음을 담는다`() {
        val sections = LobbyInfoPanel.sections(newState())
        assertEquals(listOf("진행도", "정신 상태", "설정"), sections.map { it.title })

        val progressLabels = sections[0].entries.map { it.label }
        listOf("스토리", "스테이지", "덱 전투력", "보유 인원", "조각").forEach { label ->
            assertTrue("진행도에 '$label' 없음", progressLabels.contains(label))
        }
        assertTrue(
            "스테이지 진행도 표기",
            sections[0].entries.first { it.label == "스테이지" }.value.contains("/30")
        )
        assertEquals(
            "${newState().deckTotalPower}",
            sections[0].entries.first { it.label == "덱 전투력" }.value
        )
        assertEquals("3명", sections[0].entries.first { it.label == "보유 인원" }.value)

        val settingLabels = sections[2].entries.map { it.label }
        assertEquals(listOf("BGM", "SFX", LobbyInfoPanel.AUTOSAVE_LABEL), settingLabels)
        assertTrue(
            "자동 저장 상태 안내",
            sections[2].entries.last().value.contains("켜짐")
        )
    }

    @Test
    fun `정신이 맑으면 정신 상태가 맑음으로 표시된다`() {
        val state = newState()
        val entry = LobbyInfoPanel.sections(state)[1].entries.single()
        assertEquals("정신 맑음", entry.label)
        assertEquals(InfoTone.HEAL, entry.tone)
    }

    @Test
    fun `홀림 스택이 쌓이면 정신 상태가 홀림 스택으로 표시된다`() {
        val base = newState()
        val tempted = base.copy(
            combatPenalty = base.combatPenalty.copy(totalStacks = 3),
            lobbyHeroId = base.lobbyHeroId
        )
        val entry = LobbyInfoPanel.sections(tempted)[1].entries.single()
        assertEquals("[홀림]", entry.label)
        assertEquals(InfoTone.DANGER, entry.tone)
        assertTrue(entry.value.startsWith("3스택"))
    }

    @Test
    fun `오디오 설정은 실제 상태를 따라 켜짐 꺼짐으로 표시된다`() {
        val base = newState()
        val muted = base.copy(audioSettings = base.audioSettings.copy(bgmEnabled = false, sfxEnabled = false))
        val entries = LobbyInfoPanel.sections(muted)[2].entries
        assertEquals("꺼짐", entries[0].value)
        assertEquals("꺼짐", entries[1].value)
    }

    // ── 3. 패널 상태 전이 ────────────────────────────────────────

    @Test
    fun `정보 패널은 열고 닫고 토글할 수 있다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))
        assertFalse(vm.uiState.value.isInfoPanelOpen)

        vm.openInfoPanel()
        assertTrue(vm.uiState.value.isInfoPanelOpen)
        assertTrue(vm.uiState.value.statusMessage.contains("정보"))

        vm.closeInfoPanel()
        assertFalse(vm.uiState.value.isInfoPanelOpen)

        vm.toggleInfoPanel()
        assertTrue(vm.uiState.value.isInfoPanelOpen)
        vm.toggleInfoPanel()
        assertFalse(vm.uiState.value.isInfoPanelOpen)
    }

    @Test
    fun `다른 화면으로 이동하거나 로비로 돌아오면 패널이 닫힌다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))
        vm.openInfoPanel()
        vm.openScreen(LobbyDestination.STAGE)
        assertFalse("화면 이동 시 닫힘", vm.uiState.value.isInfoPanelOpen)
        assertEquals(LobbyDestination.STAGE, vm.uiState.value.currentScreen)

        vm.backToLobby()
        assertFalse(vm.uiState.value.isInfoPanelOpen)
        assertEquals(LobbyDestination.LOBBY, vm.uiState.value.currentScreen)
    }

    @Test
    fun `패널의 경보를 누르면 해당 화면으로 이동하며 패널이 닫힌다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))
        vm.openInfoPanel()
        val alert = vm.uiState.value.lobbyAlerts.first()
        assertNotNull(alert)

        vm.openAlertDestination(alert.destination)
        assertFalse(vm.uiState.value.isInfoPanelOpen)
        assertEquals(alert.destination, vm.uiState.value.currentScreen)
    }

    @Test
    fun `인트로 다시보기는 패널을 닫고 1장면부터 재생한다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2), introAlreadySeen = true)
        vm.openInfoPanel()
        vm.replayIntro()

        val state = vm.uiState.value
        assertFalse(state.isInfoPanelOpen)
        assertTrue(state.isIntroPlaying)
        assertEquals(0, state.introStory.sceneIndex)
        assertEquals(LobbyDestination.LOBBY, state.currentScreen)
    }

    // ── 4. 경보 배지 ─────────────────────────────────────────────

    @Test
    fun `정보 버튼 배지는 로비 경보 개수와 긴급 여부를 따른다`() {
        val state = newState()
        assertEquals(state.lobbyAlerts.size, LobbyInfoPanel.alertCount(state))
        assertEquals(
            state.lobbyAlerts.any { it.isUrgent },
            LobbyInfoPanel.hasUrgentAlert(state)
        )
        assertEquals(
            "처음에는 다음 스테이지 안내만 있어 긴급 경보가 없다",
            false,
            LobbyInfoPanel.hasUrgentAlert(state)
        )
    }

    @Test
    fun `정보 패널 제목과 버튼 문구는 사용자가 누를 수 있는 형태다`() {
        assertEquals("📊 정보", LobbyInfoPanel.BUTTON_LABEL)
        assertEquals("정보", LobbyInfoPanel.TITLE)
        assertEquals("진행 자동 저장", LobbyInfoPanel.AUTOSAVE_LABEL)
    }
}
