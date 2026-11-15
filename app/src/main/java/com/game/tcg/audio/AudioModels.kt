package com.game.tcg.audio

import androidx.annotation.RawRes
import com.game.tcg.R

/**
 * BGM 트랙 4종 — res/raw 의 ogg 파일과 1:1 대응.
 *
 * 파일은 `scripts/generate_audio.py` 가 numpy 로 합성해 만든다.
 * 모든 트랙은 정수 마디 길이 · 꼬리 되접기(circular fold) 방식으로 만들어져
 * `MediaPlayer.isLooping = true` 로 돌려도 이음매가 끊기지 않는다.
 */
enum class BgmTrack(@RawRes val resId: Int, val label: String) {
    /** 몽환·신비 — 카드팩 발견 / 게이트 개방 오프닝 (80 BPM, 24초) */
    INTRO(R.raw.bgm_intro, "도입부 · 게이트"),
    /** 밝고 세련된 판타지+현대 — 로비 · 스테이지 · 상점 · 성장 · 도감 (100 BPM, 28.8초) */
    LOBBY(R.raw.bgm_lobby, "로비 · 상점 · 도감"),
    /** 박진감 있는 일반 전투 (140 BPM, 27.4초) */
    BATTLE(R.raw.bgm_battle, "일반 전투"),
    /** 웅장하고 긴박한 중간 보스(170~175) · 3대 마왕(112~114) 전투 (130 BPM, 29.5초) */
    BOSS(R.raw.bgm_boss, "보스 전투"),
}

/**
 * 효과음 9종 — 저지연 재생을 위해 SoundPool 로 미리 적재한다.
 */
enum class SfxEffect(@RawRes val resId: Int, val label: String) {
    /** UI 버튼 · 탭 */
    CLICK(R.raw.sfx_click, "버튼"),
    /** 일반 공격 타격 */
    ATTACK(R.raw.sfx_attack, "공격"),
    /** 스킬 · 페어 얼티밋 오의 발동 */
    SKILL(R.raw.sfx_skill, "스킬"),
    /** 피격 */
    HIT(R.raw.sfx_hit, "피격"),
    /** 현실 소환 · 영입 */
    SUMMON(R.raw.sfx_summon, "소환"),
    /** 카드팩 개봉 팡파르 */
    PACK_OPEN(R.raw.sfx_pack_open, "카드팩 개봉"),
    /** 강화 · 연성 · 초월 · 장비 장착 성공 */
    UPGRADE(R.raw.sfx_upgrade, "강화 성공"),
    /** 전투 승리 */
    VICTORY(R.raw.sfx_victory, "승리"),
    /** 전투 패배 */
    DEFEAT(R.raw.sfx_defeat, "패배"),
}

/**
 * 화면 상태에서 유도되는 음악 장면. 어떤 BGM 을 트는지의 기준이다.
 */
enum class AudioScene(val track: BgmTrack?) {
    /** 아직 아무것도 정해지지 않음 (무음) */
    NONE(null),
    INTRO(BgmTrack.INTRO),
    LOBBY(BgmTrack.LOBBY),
    BATTLE(BgmTrack.BATTLE),
    BOSS(BgmTrack.BOSS),
}

/**
 * 오디오 설정 — 화면 상태(TcgGameState.audioSettings)로 관리하고 SharedPreferences 에 남긴다.
 *
 * @param bgmEnabled 배경음악 켜기/끄기 (🔊/🔇)
 * @param sfxEnabled 효과음 켜기/끄기 (🔊/🔇)
 * @param bgmVolume  0f~1f
 * @param sfxVolume  0f~1f
 */
data class AudioSettings(
    val bgmEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val bgmVolume: Float = 0.65f,
    val sfxVolume: Float = 0.90f
) {
    /** 범위를 벗어난 값이 들어와도 안전하게 보정한 복사본 */
    fun sanitized(): AudioSettings = copy(
        bgmVolume = bgmVolume.coerceIn(0f, 1f),
        sfxVolume = sfxVolume.coerceIn(0f, 1f)
    )

    companion object {
        /** 볼륨 슬라이더가 움직이는 기본 단계 (0 · 25 · 50 · 75 · 100%) */
        val VOLUME_STEPS: List<Float> = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
    }
}

/**
 * 화면 상태 → 음악 장면 매핑 (순수 함수 · JVM 단위 테스트 대상).
 *
 * 우선순위
 *  1) 도입부 컷신이 떠 있으면 인트로 테마 (로비 위에 겹쳐 재생되는 연출)
 *  2) 전투 화면 + 보스(중간 보스 · 대마왕 · 제2장 전초전) → 보스 테마
 *  3) 전투 화면 → 일반 전투 테마
 *  4) 그 외 모든 관리 화면(로비 · 스테이지 · 상점 · 성장 · 도감 · 덱 · 탐색 · 현실) → 로비 테마
 */
object AudioSceneResolver {
    fun resolve(
        isIntroPlaying: Boolean,
        isBattleScreen: Boolean,
        isBossBattle: Boolean = false
    ): AudioScene = when {
        isIntroPlaying -> AudioScene.INTRO
        isBattleScreen && isBossBattle -> AudioScene.BOSS
        isBattleScreen -> AudioScene.BATTLE
        else -> AudioScene.LOBBY
    }
}
