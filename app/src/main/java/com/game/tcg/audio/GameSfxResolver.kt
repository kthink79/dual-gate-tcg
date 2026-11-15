package com.game.tcg.audio

/**
 * 게임 이벤트 → 효과음 매핑 (순수 로직 · 단위 테스트 대상).
 *
 * ViewModel 의 상태 전후를 받아 어떤 SFX 를 튼지 정한다.
 * 실제 재생은 [AudioManager] 가 담당한다.
 */
object GameSfxResolver {

    /** 성장 계열 액션 결과 성공 여부 → 강화 성공음 */
    fun forUpgrade(success: Boolean): SfxEffect? =
        if (success) SfxEffect.UPGRADE else SfxEffect.CLICK

    /** 전투 정산 — 승리/패배 */
    fun forBattleOutcome(enemyHp: Int, aliveAllies: Int): SfxEffect? = when {
        enemyHp <= 0 -> SfxEffect.VICTORY
        aliveAllies <= 0 -> SfxEffect.DEFEAT
        else -> null
    }
}
