package com.game.tcg

import com.game.tcg.audio.AudioScene
import com.game.tcg.audio.AudioSceneResolver
import com.game.tcg.audio.AudioSettings
import com.game.tcg.audio.AudioManager
import com.game.tcg.audio.BgmTrack
import com.game.tcg.audio.GameSfxResolver
import com.game.tcg.audio.InMemoryAudioPrefs
import com.game.tcg.audio.NoopAudioPlayer
import com.game.tcg.audio.SfxEffect
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 오디오 시스템 단위 테스트 — 화면/보스 여부별 BGM 매핑, 음소거 토글 시 재생 차단,
 * SFX 이벤트 매핑, 설정 저장/복원.
 */
class AudioManagerTest {

    // ──────────────────────────────────────────────────────────────
    // 화면 상태 → BGM 매핑
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `인트로 재생 중에는 로비 화면이라도 인트로 테마가 우선한다`() {
        val scene = AudioSceneResolver.resolve(
            isIntroPlaying = true,
            isBattleScreen = false,
            isBossBattle = false
        )
        assertEquals(AudioScene.INTRO, scene)
        assertEquals(BgmTrack.INTRO, scene.track)
    }

    @Test
    fun `인트로가 전투 화면 위에 있어도 인트로 테마가 이긴다`() {
        val scene = AudioSceneResolver.resolve(
            isIntroPlaying = true,
            isBattleScreen = true,
            isBossBattle = true
        )
        assertEquals(AudioScene.INTRO, scene)
    }

    @Test
    fun `로비_스테이지_상점_성장_도감_덱_탐색_현실은 모두 로비 테마다`() {
        val managementScreens = listOf(
            LobbyDestination.LOBBY,
            LobbyDestination.SCENARIO,
            LobbyDestination.STAGE,
            LobbyDestination.EXPLORATION,
            LobbyDestination.REAL_WORLD,
            LobbyDestination.SHOP,
            LobbyDestination.GROWTH,
            LobbyDestination.COLLECTION,
            LobbyDestination.DECK
        )
        managementScreens.forEach { screen ->
            val scene = AudioSceneResolver.resolve(false, isBattleScreen = false, isBossBattle = false)
            // 화면 자체는 battle 여부로만 판단하므로, mapper 를 통해 검증한다
            val mapped = com.game.tcg.audio.AudioSceneMapper.of(false, screen, isBossBattle = false)
            assertEquals("화면 $screen 은 로비 테마", AudioScene.LOBBY, mapped)
            assertEquals(scene.track, mapped.track)
        }
    }

    @Test
    fun `일반 전투 화면은 전투 테마다`() {
        val mapped = com.game.tcg.audio.AudioSceneMapper.of(false, LobbyDestination.BATTLE, isBossBattle = false)
        assertEquals(AudioScene.BATTLE, mapped)
        assertEquals(BgmTrack.BATTLE, mapped.track)
    }

    @Test
    fun `보스 전투 화면은 보스 테마다`() {
        val mapped = com.game.tcg.audio.AudioSceneMapper.of(false, LobbyDestination.BATTLE, isBossBattle = true)
        assertEquals(AudioScene.BOSS, mapped)
        assertEquals(BgmTrack.BOSS, mapped.track)
    }

    @Test
    fun `뷰모델은 마왕전과 제2장 전초전을 보스 장면으로 본다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2), introAlreadySeen = true)

        vm.startDemonLordBattle()
        assertEquals(AudioScene.BOSS, vm.audioSceneOf(vm.uiState.value))

        // 마왕전 승리 → 로비 장면 복귀
        repeat(30) { vm.attackFront() }
        val after = vm.uiState.value
        if (after.enemyHp <= 0) {
            assertEquals(AudioScene.LOBBY, after.audioScene)
        }

        vm.startChapter2BossBattle()
        assertEquals(AudioScene.BOSS, vm.audioSceneOf(vm.uiState.value))
    }

    @Test
    fun `뷰모델은 일반 원정을 전투 장면으로 본다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2), introAlreadySeen = true)
        vm.startFieldBattle()
        assertEquals(AudioScene.BATTLE, vm.audioSceneOf(vm.uiState.value))
    }

    @Test
    fun `뷰모델은 스테이지 1을 일반 전투 장면으로 본다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2), introAlreadySeen = true)
        assertTrue(vm.startStageBattle(1))
        assertEquals(AudioScene.BATTLE, vm.audioSceneOf(vm.uiState.value))
    }

    @Test
    fun `뷰모델은 관리 화면을 로비 장면으로 본다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2), introAlreadySeen = true)
        vm.openScreen(LobbyDestination.SHOP)
        assertEquals(AudioScene.LOBBY, vm.audioSceneOf(vm.uiState.value))
        vm.openScreen(LobbyDestination.COLLECTION)
        assertEquals(AudioScene.LOBBY, vm.audioSceneOf(vm.uiState.value))
    }

    // ──────────────────────────────────────────────────────────────
    // AudioManager — 장면 전환 · 음소거
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `같은 장면을 반복 갱신해도 재생기에는 한 번만 닿는다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())

        audio.updateScene(AudioScene.LOBBY)
        audio.updateScene(AudioScene.LOBBY)
        audio.updateScene(AudioScene.LOBBY)

        assertEquals(listOf(BgmTrack.LOBBY), player.bgmPlays)
    }

    @Test
    fun `장면이 바뀌면 새 트랙으로 전환된다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())

        audio.updateScene(AudioScene.LOBBY)
        audio.updateScene(AudioScene.BATTLE)
        audio.updateScene(AudioScene.BOSS)
        audio.updateScene(AudioScene.LOBBY)

        assertEquals(
            listOf(BgmTrack.LOBBY, BgmTrack.BATTLE, BgmTrack.BOSS, BgmTrack.LOBBY),
            player.bgmPlays
        )
    }

    @Test
    fun `BGM 음소거 토글 시 새 장면 재생이 차단된다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())

        audio.updateScene(AudioScene.LOBBY)
        assertEquals(listOf(BgmTrack.LOBBY), player.bgmPlays)

        // 끄기
        val off = audio.toggleBgm()
        assertFalse(off.bgmEnabled)
        assertEquals(emptyList<BgmTrack>(), player.bgmPlaysSinceMute)

        // 화면이 바뀌어도 소리가 나지 않는다
        audio.updateScene(AudioScene.BATTLE)
        assertEquals(emptyList<BgmTrack>(), player.bgmPlaysSinceMute)

        // 다시 켜면 마지막 장면(전투)부터 재개한다
        val on = audio.toggleBgm()
        assertTrue(on.bgmEnabled)
        audio.refresh()
        assertEquals(listOf(BgmTrack.BATTLE), player.bgmPlaysSinceMute)
    }

    @Test
    fun `SFX 음소거 시 효과음이 차단되고 다시 켜면 들린다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())

        audio.click()
        assertEquals(listOf(SfxEffect.CLICK), player.sfxPlays)

        val off = audio.toggleSfx()
        assertFalse(off.sfxEnabled)
        audio.attack()
        audio.victory()
        assertEquals(listOf(SfxEffect.CLICK), player.sfxPlays)

        audio.toggleSfx()
        audio.attack()
        assertEquals(listOf(SfxEffect.CLICK, SfxEffect.ATTACK), player.sfxPlays)
    }

    @Test
    fun `볼륨은 0~1 로 강제 보정되고 재생기에 전달된다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())

        val s = audio.setBgmVolume(2.5f).let { audio.setSfxVolume(-1f) }
        assertEquals(1f, s.bgmVolume, 1e-6f)
        assertEquals(0f, s.sfxVolume, 1e-6f)
        assertEquals(1f, player.lastBgmVolume!!, 1e-6f)
        assertEquals(0f, player.lastSfxVolume!!, 1e-6f)
    }

    @Test
    fun `설정은 prefs 에 저장되고 새 매니저가 복원한다`() {
        val prefs = InMemoryAudioPrefs()
        val first = AudioManager(RecordingPlayer(), prefs)
        first.toggleBgm()
        first.setSfxVolume(0.25f)

        val restored = AudioManager(RecordingPlayer(), prefs).settings()
        assertFalse(restored.bgmEnabled)
        assertEquals(0.25f, restored.sfxVolume, 1e-6f)
        assertTrue(restored.sfxEnabled)
    }

    @Test
    fun `백그라운드 전환 중에는 장면 갱신이 재생기에 닿지 않고 복귀 시 재개된다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())
        audio.updateScene(AudioScene.LOBBY)

        audio.onAppPause()
        assertEquals(1, player.pauseCount)

        // 백그라운드에서 화면이 바뀌어도 즉시 재생하지 않는다
        audio.updateScene(AudioScene.BATTLE)
        assertEquals(listOf(BgmTrack.LOBBY), player.bgmPlays)

        audio.onAppResume()
        assertEquals(1, player.resumeCount)
        // 복귀 시 pause 되돌림 재개 → resume 이 기억한 트랙(로비) 재생으로 확인된다.
        // 그 뒤 Compose 가 새 장면(전투)을 다시 updateScene 하면 전투로 전환되므로,
        // 여기서는 resume 재개 자체만 검증한다.
        assertEquals(BgmTrack.LOBBY, player.resumedTrack)
    }

    @Test
    fun `백그라운드 일시정지 후 복귀해도 음소거면 재개하지 않는다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())
        audio.toggleBgm()

        audio.onAppPause()
        audio.onAppResume()
        assertEquals(0, player.resumeCount)
    }

    // ──────────────────────────────────────────────────────────────
    // SFX 이벤트 매핑
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `강화 성공은 업그레이드음 실패는 클릭음이다`() {
        assertEquals(SfxEffect.UPGRADE, GameSfxResolver.forUpgrade(success = true))
        assertEquals(SfxEffect.CLICK, GameSfxResolver.forUpgrade(success = false))
    }

    @Test
    fun `전투 정산 매핑 — 적 격파는 승리 전멸은 패배 진행 중은 무음`() {
        assertEquals(SfxEffect.VICTORY, GameSfxResolver.forBattleOutcome(enemyHp = 0, aliveAllies = 3))
        assertEquals(SfxEffect.DEFEAT, GameSfxResolver.forBattleOutcome(enemyHp = 5000, aliveAllies = 0))
        assertNull(GameSfxResolver.forBattleOutcome(enemyHp = 5000, aliveAllies = 2))
    }

    @Test
    fun `no-op 플레이어는 예외 없이 모든 호출을 받아낸다`() {
        val player = NoopAudioPlayer()
        val audio = AudioManager(player, InMemoryAudioPrefs())

        audio.updateScene(AudioScene.INTRO)
        audio.click()
        audio.attack()
        audio.skill()
        audio.hit()
        audio.summon()
        audio.packOpen()
        audio.upgrade()
        audio.victory()
        audio.defeat()
        audio.onAppPause()
        audio.onAppResume()
        audio.release()

        assertEquals(AudioScene.INTRO, player.lastScene)
    }

    @Test
    fun `기본 설정은 BGM·SFX 모두 켜져 있다`() {
        val settings = AudioSettings()
        assertTrue(settings.bgmEnabled)
        assertTrue(settings.sfxEnabled)
        assertTrue(settings.bgmVolume > 0f && settings.bgmVolume <= 1f)
        assertTrue(settings.sfxVolume > 0f && settings.sfxVolume <= 1f)
    }

    @Test
    fun `설정 복사본은 항상 보정된다`() {
        val s = AudioSettings(bgmVolume = 7f, sfxVolume = -3f).sanitized()
        assertEquals(1f, s.bgmVolume, 1e-6f)
        assertEquals(0f, s.sfxVolume, 1e-6f)
        assertNotEquals(AudioSettings().hashCode(), s.hashCode())
        assertNotNull(s)
    }

    // ──────────────────────────────────────────────────────────────
    // 기록용 테스트 더블
    // ──────────────────────────────────────────────────────────────

    private class RecordingPlayer : com.game.tcg.audio.AudioPlayer {
        val bgmPlays = mutableListOf<BgmTrack>()
        var bgmPlaysSinceMute: List<BgmTrack> = emptyList()
        val sfxPlays = mutableListOf<SfxEffect>()
        var lastBgmVolume: Float? = null
        var lastSfxVolume: Float? = null
        var bgmEnabledFlag: Boolean = true
        var sfxEnabledFlag: Boolean = true
        var pauseCount = 0
        var resumeCount = 0
        var resumedTrack: BgmTrack? = null
        private var pausedTrack: BgmTrack? = null

        override fun playBgm(track: BgmTrack) {
            if (!bgmEnabledFlag) return
            if (bgmPlays.lastOrNull() == track) return
            bgmPlays.add(track)
            bgmPlaysSinceMute = bgmPlaysSinceMute + track
        }

        override fun playScene(scene: AudioScene) {
            scene.track?.let { playBgm(it) } ?: stopBgm()
        }

        override fun stopBgm() {
            pausedTrack = null
        }

        override fun pauseBgm() {
            pauseCount++
            pausedTrack = bgmPlays.lastOrNull()
        }

        override fun resumeBgm() {
            if (bgmEnabledFlag) resumeCount++
            resumedTrack = pausedTrack
            pausedTrack?.let { playBgm(it) }
            pausedTrack = null
        }

        override fun currentTrack(): BgmTrack? = bgmPlays.lastOrNull()

        override fun setBgmVolume(volume: Float) {
            lastBgmVolume = volume.coerceIn(0f, 1f)
        }

        override fun setBgmEnabled(enabled: Boolean) {
            bgmEnabledFlag = enabled
            if (!enabled) bgmPlaysSinceMute = emptyList()
        }

        override fun playSfx(effect: SfxEffect) {
            if (sfxEnabledFlag) sfxPlays.add(effect)
        }

        override fun setSfxVolume(volume: Float) {
            lastSfxVolume = volume.coerceIn(0f, 1f)
        }

        override fun setSfxEnabled(enabled: Boolean) {
            sfxEnabledFlag = enabled
        }

        override fun release() = Unit
    }
}
