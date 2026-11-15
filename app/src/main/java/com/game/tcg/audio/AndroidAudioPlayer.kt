package com.game.tcg.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes

/**
 * Android 구현체 — BGM 은 MediaPlayer(루프), SFX 는 SoundPool(저지연).
 *
 * 설계 메모
 * - BGM 트랙 전환 시 같은 트랙을 다시 play 하지 않아(중복 재생 방지) 화면 전환·리컴포지션에도 안전하다.
 * - SoundPool 로드는 비동기라 [SfxEffect] 를 처음에 모두 적재해 둔다. 로드 실패·재생 실패는
 *   조용히 무시한다(게임 자체는 음악 없이도 계속 돌아가야 하므로).
 * - 모든 메서드는 예외를 밖으로 던지지 않는다. 오디오는 부가 기능이다.
 */
class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private var bgmPlayer: MediaPlayer? = null
    private var current: BgmTrack? = null
    private var pausedTrack: BgmTrack? = null
    private var bgmEnabled: Boolean = true
    private var sfxEnabled: Boolean = true
    private var bgmVolume: Float = 0.65f
    private var sfxVolume: Float = 0.90f
    /** release() 이후에는 다시 재생하지 않는다 (해제된 SoundPool 접근 방지) */
    private var released: Boolean = false

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sfxIds: Map<SfxEffect, Int> = SfxEffect.entries.associateWith { effect ->
        runCatching { soundPool.load(context, effect.resId, 1) }.getOrDefault(0)
    }

    // ── BGM (MediaPlayer) ─────────────────────────────────────────

    override fun playBgm(track: BgmTrack) {
        if (released || !bgmEnabled || current == track) return
        releaseBgmPlayer()
        val player = runCatching {
            MediaPlayer.create(context, track.resId)?.apply {
                isLooping = true
                setVolume(bgmVolume, bgmVolume)
                // 재생 중 오류는 삼킨다 — 오디오는 부가 기능이므로 게임을 멈추지 않는다
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.reset() }
                    if (bgmPlayer === mp) {
                        bgmPlayer = null
                        current = null
                    }
                    true
                }
                start()
            }
        }.getOrNull()
        bgmPlayer = player
        current = if (player != null) track else null
        pausedTrack = null
    }

    override fun playScene(scene: AudioScene) {
        val track = scene.track ?: return stopBgm()
        playBgm(track)
    }

    override fun stopBgm() {
        releaseBgmPlayer()
        current = null
        pausedTrack = null
    }

    override fun pauseBgm() {
        val player = bgmPlayer ?: return
        runCatching { if (player.isPlaying) player.pause() }
        pausedTrack = current
    }

    override fun resumeBgm() {
        if (released) return
        val track = pausedTrack ?: current ?: return
        val player = bgmPlayer
        if (player != null && current == track) {
            val resumed = runCatching {
                if (!player.isPlaying) player.start()
                true
            }.getOrDefault(false)
            // 재생기 상태가 깨졌으면 새로 만든다
            if (!resumed) {
                bgmPlayer = null
                current = null
                playBgm(track)
            }
        } else {
            // 정지 상태에서 resume 요청 — 같은 트랙을 다시 튼다
            val wasEnabled = bgmEnabled
            bgmEnabled = true
            playBgm(track)
            bgmEnabled = wasEnabled
        }
        pausedTrack = null
    }

    override fun currentTrack(): BgmTrack? = current

    override fun setBgmVolume(volume: Float) {
        bgmVolume = volume.coerceIn(0f, 1f)
        val player = bgmPlayer
        if (player != null && bgmVolume > 0f) {
            runCatching { player.setVolume(bgmVolume, bgmVolume) }
        }
    }

    override fun setBgmEnabled(enabled: Boolean) {
        if (bgmEnabled == enabled) return
        bgmEnabled = enabled
        if (!enabled) {
            stopBgm()
        }
    }

    private fun releaseBgmPlayer() {
        bgmPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
            }
            runCatching { player.release() }
        }
        bgmPlayer = null
    }

    // ── SFX (SoundPool) ───────────────────────────────────────────

    override fun playSfx(effect: SfxEffect) {
        if (released || !sfxEnabled) return
        val id = sfxIds[effect] ?: return
        if (id == 0) return  // 아직 로드 전 또는 로드 실패
        runCatching {
            soundPool.play(id, sfxVolume, sfxVolume, 1, 0, 1.0f)
        }
    }

    override fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
    }

    override fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
    }

    override fun release() {
        if (released) return
        released = true
        releaseBgmPlayer()
        runCatching { soundPool.release() }
        current = null
        pausedTrack = null
    }
}
