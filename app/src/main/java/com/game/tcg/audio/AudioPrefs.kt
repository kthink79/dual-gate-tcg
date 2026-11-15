package com.game.tcg.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * 오디오 설정 저장 — SharedPreferences 로 남겨 다음 실행에도 유지한다.
 * (JVM 테스트에서는 [InMemoryAudioPrefs] 를 쓴다)
 */
interface AudioPrefs {
    fun load(): AudioSettings
    fun save(settings: AudioSettings)
}

class SharedPrefsAudioPrefs(context: Context) : AudioPrefs {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): AudioSettings = AudioSettings(
        bgmEnabled = prefs.getBoolean(KEY_BGM_ON, true),
        sfxEnabled = prefs.getBoolean(KEY_SFX_ON, true),
        bgmVolume = prefs.getFloat(KEY_BGM_VOL, 0.65f),
        sfxVolume = prefs.getFloat(KEY_SFX_VOL, 0.90f)
    ).sanitized()

    override fun save(settings: AudioSettings) {
        val s = settings.sanitized()
        prefs.edit()
            .putBoolean(KEY_BGM_ON, s.bgmEnabled)
            .putBoolean(KEY_SFX_ON, s.sfxEnabled)
            .putFloat(KEY_BGM_VOL, s.bgmVolume)
            .putFloat(KEY_SFX_VOL, s.sfxVolume)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "dual_gate_audio"
        const val KEY_BGM_ON = "bgm_enabled"
        const val KEY_SFX_ON = "sfx_enabled"
        const val KEY_BGM_VOL = "bgm_volume"
        const val KEY_SFX_VOL = "sfx_volume"
    }
}

/** 테스트용 메모리 저장소 */
class InMemoryAudioPrefs(initial: AudioSettings = AudioSettings()) : AudioPrefs {
    private var value: AudioSettings = initial.sanitized()

    override fun load(): AudioSettings = value
    override fun save(settings: AudioSettings) {
        value = settings.sanitized()
    }
}
