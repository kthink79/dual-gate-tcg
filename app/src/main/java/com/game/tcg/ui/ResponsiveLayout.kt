package com.game.tcg.ui

/**
 * 화면 크기 대응 규칙 — 2026-11-19 사용자 요청.
 *
 * > "설치 디바이스 해상도·화면 크기에 맞게 자동 조정하고, 태블릿·폴더블처럼 넓은 화면에서도
 * >  캐릭터 카드(2:3)·보스 일러스트·전투/탐색 화면의 세로 비율이 깨지거나 가로로 찌그러지지 않게."
 *
 * Compose 를 쓰지 않는 순수 계산만 담아 JVM 단위 테스트에서 그대로 검증한다.
 *  - 본문 최대 폭: 넓은 화면에서 한 줄이 지나치게 길어지지 않게 중앙 정렬 + 폭 제한
 *  - 열 수: 화면 폭과 타일 최소 폭으로 2:3 카드가 찌그러지지 않는 열 수를 고른다
 */
object ResponsiveLayout {

    /** 휴대폰 기준 본문 최대 폭(dp) — 이보다 넓은 화면은 가운데 정렬한다 */
    const val PHONE_MAX_WIDTH_DP: Float = 600f

    /** 태블릿·폴더블에서 허용하는 본문 최대 폭(dp) */
    const val TABLET_MAX_WIDTH_DP: Float = 840f

    /** 카드 타일이 이보다 작아지면 글자가 읽히지 않는다 */
    const val MIN_TILE_WIDTH_DP: Float = 96f

    /** 태블릿으로 보는 기준 폭 (WindowSizeClass 의 Medium 구간과 같은 600dp) */
    const val TABLET_BREAKPOINT_DP: Float = 600f

    /** 이 폭이면 태블릿·폴더블 펼침으로 본다 */
    fun isWide(screenWidthDp: Float): Boolean = screenWidthDp >= TABLET_BREAKPOINT_DP

    /**
     * 본문 컨텐츠 최대 폭 — 넓은 화면에서 늘어지지 않게 제한한다.
     * (휴대폰은 화면 폭 그대로, 태블릿은 [TABLET_MAX_WIDTH_DP] 까지)
     */
    fun contentMaxWidthDp(screenWidthDp: Float): Float = when {
        screenWidthDp <= PHONE_MAX_WIDTH_DP -> screenWidthDp
        screenWidthDp <= TABLET_MAX_WIDTH_DP -> screenWidthDp
        else -> TABLET_MAX_WIDTH_DP
    }

    /**
     * 2:3 카드 그리드의 열 수 — 타일이 [MIN_TILE_WIDTH_DP] 보다 작아지지 않는 선에서 최대한 채운다.
     * 휴대폰 3열, 태블릿 5~6열이 자연스럽다.
     */
    fun gridColumns(
        screenWidthDp: Float,
        minTileWidthDp: Float = MIN_TILE_WIDTH_DP,
        spacingDp: Float = 8f,
        horizontalPaddingDp: Float = 28f
    ): Int {
        val usable = (screenWidthDp - horizontalPaddingDp).coerceAtLeast(minTileWidthDp)
        val fit = ((usable + spacingDp) / (minTileWidthDp + spacingDp)).toInt()
        return fit.coerceIn(2, 8)
    }

    /** 한 줄에 놓는 보스·프리뷰 일러스트 최대 폭 — 세로 화면에서 2:3 비율을 유지한 채 키만 정한다 */
    fun heroWidthDp(screenWidthDp: Float): Float {
        val max = contentMaxWidthDp(screenWidthDp)
        return (max * 0.62f).coerceIn(180f, 320f)
    }
}
