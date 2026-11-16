package com.game.tcg.core.video

import com.game.tcg.core.engine.PackOpeningPhase

/**
 * 등장 영상 재생 상태.
 *
 *  - [NONE]    : 이번 카드에는 등장 영상이 없거나, 개봉 연출이 영상 단계가 아니다
 *  - [PLAYING] : 지금 영상을 재생 중 — 카드 일러스트보다 먼저 보여 준다
 *  - [DONE]    : 재생이 끝났거나 건너뛰었다 — 이제 카드 스탯/등급 화면으로
 */
enum class CharacterVideoStatus {
    NONE,
    PLAYING,
    DONE
}

/**
 * 카드팩 개봉 1장분의 등장 영상 큐 — 순수 데이터(Compose·Android 무관).
 *
 * 화면(`ui/PackOpeningOverlay.kt`)은 이 큐만 보고
 * "영상 재생 → 카드 등장" 순서를 정한다. 그래서 "영상이 있는 카드/없는 카드",
 * "건너뛰기", "다시 보기"가 모두 JVM 단위 테스트로 검증된다.
 */
data class CharacterVideoCue(
    val status: CharacterVideoStatus = CharacterVideoStatus.NONE,
    /** 이 큐가 가리키는 카드 번호 (영상이 없으면 0) */
    val cardId: Int = 0,
    /** assets 상대 경로 (없으면 null) */
    val assetPath: String? = null
) {

    val isPlaying: Boolean get() = status == CharacterVideoStatus.PLAYING

    /** 영상이 이 카드에 등록되어 있는지 (재생 여부와 무관) */
    val hasVideo: Boolean get() = assetPath.isNullOrBlank().not() && cardId > 0

    /** 재생이 끝났거나 건너뜀 — DONE 이 아니면 그대로 */
    fun finished(): CharacterVideoCue =
        if (status == CharacterVideoStatus.PLAYING) copy(status = CharacterVideoStatus.DONE) else this

    /** 화면 탭 · SKIP ▸ 버튼 — [finished] 와 같은 결과(카드로 넘어간다) */
    fun skipped(): CharacterVideoCue = finished()

    companion object {

        val NONE: CharacterVideoCue = CharacterVideoCue()

        /**
         * 개봉 연출의 지금 위치로 큐를 만든다.
         *
         * 영상은 **[PackOpeningPhase.REVEAL] 단계에서만** 재생한다 —
         * 봉인 카드팩([PackOpeningPhase.SEALED])·전체 그리드([PackOpeningPhase.SUMMARY])에서는
         * 카드가 아직/이미 정리된 상태라 영상이 끼어들면 안 된다.
         *
         * @param assetPath [CharacterVideoIndex.videoFor] 결과 (없으면 null)
         */
        fun forReveal(
            phase: PackOpeningPhase,
            cardId: Int,
            assetPath: String?
        ): CharacterVideoCue {
            val hasVideo = phase == PackOpeningPhase.REVEAL &&
                cardId > 0 &&
                !assetPath.isNullOrBlank()
            return if (hasVideo) {
                CharacterVideoCue(
                    status = CharacterVideoStatus.PLAYING,
                    cardId = cardId,
                    assetPath = assetPath
                )
            } else {
                NONE
            }
        }

        /** 영상 1회 재생 단위 키 — 같은 카드라도 다음 장으로 넘어가면 새로 재생한다 */
        fun revealKey(phase: PackOpeningPhase, revealIndex: Int): String = "$phase#$revealIndex"
    }
}
