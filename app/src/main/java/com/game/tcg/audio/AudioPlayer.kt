package com.game.tcg.audio

/**
 * 오디오 재생 추상화 — JVM 단위 테스트(로보렉틱 없음)에서도 크래시하지 않도록
 * Android API(MediaPlayer · SoundPool)를 이 인터페이스 뒤로 완전히 숨긴다.
 *
 * - [AndroidAudioPlayer] : 실기기용 구현체
 * - [NoopAudioPlayer]   : 유닛 테스트·프리뷰용 no-op
 */
interface AudioPlayer {
    /** 지정 트랙을 (이미 그 트랙이 아니라면) 처음부터 루프 재생한다 */
    fun playBgm(track: BgmTrack)

    /** 지정 장면에 맞는 트랙으로 전환 (같은 트랙이면 무시) */
    fun playScene(scene: AudioScene)

    /** BGM 정지 (현재 트랙 기억 없음 — resume 은 재개 시 다시 지정) */
    fun stopBgm()

    /** BGM 일시정지 (같은 트랙에서 resume) */
    fun pauseBgm()

    /** 일시정지했던 트랙 재개 */
    fun resumeBgm()

    /** 지금 흐르고 있는 트랙 (없으면 null) */
    fun currentTrack(): BgmTrack?

    /** BGM 볼륨 즉시 적용 (0f~1f) */
    fun setBgmVolume(volume: Float)

    /** BGM 켜기/끄기 — 끄면 정지, 켜면 마지막 장면부터 재시작 */
    fun setBgmEnabled(enabled: Boolean)

    /** 효과음 1회 재생 (저지연 — 로드 실패 시 조용히 무시) */
    fun playSfx(effect: SfxEffect)

    /** SFX 볼륨 즉시 적용 (0f~1f) */
    fun setSfxVolume(volume: Float)

    /** SFX 켜기/끄기 */
    fun setSfxEnabled(enabled: Boolean)

    /** 리소스 해제 (Activity 파괴 시) */
    fun release()
}

/** 재생하지 않는 no-op 구현 — 유닛 테스트(JVM)와 Compose 프리뷰용 */
class NoopAudioPlayer : AudioPlayer {
    var lastScene: AudioScene = AudioScene.NONE
        private set

    override fun playBgm(track: BgmTrack) {
        lastScene = AudioScene.LOBBY
    }

    override fun playScene(scene: AudioScene) {
        lastScene = scene
    }

    override fun stopBgm() {
        lastScene = AudioScene.NONE
    }

    override fun pauseBgm() = Unit

    override fun resumeBgm() = Unit

    override fun currentTrack(): BgmTrack? = null

    override fun setBgmVolume(volume: Float) = Unit

    override fun setBgmEnabled(enabled: Boolean) = Unit

    override fun playSfx(effect: SfxEffect) = Unit

    override fun setSfxVolume(volume: Float) = Unit

    override fun setSfxEnabled(enabled: Boolean) = Unit

    override fun release() = Unit
}
