package com.game.tcg.audio

import com.game.tcg.ui.LobbyDestination

/**
 * 오디오 지휘자(퍼사드) — 화면 상태와 행동을 받아 BGM 장면·SFX 를 결정한다.
 *
 * 순수 로직만 담당(실제 재생은 [AudioPlayer] 에 위임)하므로 JVM 단위 테스트 대상이다.
 * Compose 쪽에서는 `AudioManager.attach(player)` 로 붙여 쓴다.
 */
class AudioManager(
    private val player: AudioPlayer = NoopAudioPlayer(),
    private val prefs: AudioPrefs = InMemoryAudioPrefs()
) {

    private var settings: AudioSettings = prefs.load().sanitized()
    private var lastScene: AudioScene = AudioScene.NONE
    private var bgmPausedByLifecycle: Boolean = false

    init {
        applySettingsToPlayer()
    }

    // ──────────────────────────────────────────────────────────────
    // 화면 상태 → BGM
    // ──────────────────────────────────────────────────────────────

    /** 화면 상태가 바뀔 때마다 호출 — 필요할 때만 트랙을 바꾼다 */
    fun updateScene(scene: AudioScene) {
        if (scene == lastScene) return
        lastScene = scene
        if (bgmPausedByLifecycle) return  // 백그라운드에서는 복귀 시 반영된다
        if (!settings.bgmEnabled) return
        player.playScene(scene)
    }

    /** 마지막 장면을 다시 적용 (음소거 해제 · 볼륨 변경 후 재시작) */
    fun refresh() {
        applySettingsToPlayer()
        if (!settings.bgmEnabled || bgmPausedByLifecycle) return
        player.playScene(lastScene)
    }

    // ──────────────────────────────────────────────────────────────
    // 액션 → SFX
    // ──────────────────────────────────────────────────────────────

    fun playSfx(effect: SfxEffect) {
        if (!settings.sfxEnabled) return
        player.playSfx(effect)
    }

    /** UI 공통 — 버튼 · 메뉴 탭 */
    fun click() = playSfx(SfxEffect.CLICK)

    /** 전투 액션 */
    fun attack() = playSfx(SfxEffect.ATTACK)
    fun skill() = playSfx(SfxEffect.SKILL)
    fun hit() = playSfx(SfxEffect.HIT)

    /** 성장·수집 액션 */
    fun summon() = playSfx(SfxEffect.SUMMON)
    fun packOpen() = playSfx(SfxEffect.PACK_OPEN)
    fun upgrade() = playSfx(SfxEffect.UPGRADE)

    /** 전투 결과 */
    fun victory() = playSfx(SfxEffect.VICTORY)
    fun defeat() = playSfx(SfxEffect.DEFEAT)

    // ──────────────────────────────────────────────────────────────
    // 설정 (변경 → 저장 → 플레이어 반영)
    // ──────────────────────────────────────────────────────────────

    fun settings(): AudioSettings = settings

    fun toggleBgm(): AudioSettings = updateSettings(settings.copy(bgmEnabled = !settings.bgmEnabled))

    fun toggleSfx(): AudioSettings = updateSettings(settings.copy(sfxEnabled = !settings.sfxEnabled))

    fun setBgmVolume(volume: Float): AudioSettings =
        updateSettings(settings.copy(bgmVolume = volume.coerceIn(0f, 1f)))

    fun setSfxVolume(volume: Float): AudioSettings =
        updateSettings(settings.copy(sfxVolume = volume.coerceIn(0f, 1f)))

    private fun updateSettings(next: AudioSettings): AudioSettings {
        settings = next.sanitized()
        prefs.save(settings)
        applySettingsToPlayer()
        return settings
    }

    private fun applySettingsToPlayer() {
        player.setBgmEnabled(settings.bgmEnabled)
        player.setBgmVolume(settings.bgmVolume)
        player.setSfxEnabled(settings.sfxEnabled)
        player.setSfxVolume(settings.sfxVolume)
    }

    // ──────────────────────────────────────────────────────────────
    // 생명주기 (백그라운드 일시정지 · 복귀 재개)
    // ──────────────────────────────────────────────────────────────

    /** ON_PAUSE / ON_STOP — BGM 을 멈춘다 */
    fun onAppPause() {
        bgmPausedByLifecycle = true
        player.pauseBgm()
    }

    /** ON_RESUME — 멈췄던 BGM 을 이어 재생한다 */
    fun onAppResume() {
        bgmPausedByLifecycle = false
        if (settings.bgmEnabled) player.resumeBgm()
    }

    /** Activity 종료 — 리소스 해제 */
    fun release() {
        player.release()
    }
}

/**
 * 게임 화면 · 보스 여부 → [AudioScene] 결정 (퍼사드 위의 얇은 헬퍼).
 * ViewModel 의 상태에서 필요한 3개 불리언만 뽑아 넘긴다.
 */
object AudioSceneMapper {
    fun of(isIntroPlaying: Boolean, screen: LobbyDestination, isBossBattle: Boolean): AudioScene =
        AudioSceneResolver.resolve(
            isIntroPlaying = isIntroPlaying,
            isBattleScreen = screen == LobbyDestination.BATTLE,
            isBossBattle = isBossBattle
        )
}
