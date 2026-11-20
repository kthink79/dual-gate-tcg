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

    // ── 2:3 원화 프레임 (2026-11-20 태블릿 가로 대응) ──────────────────────

    /** 캐릭터 원화(JPG 540×810)의 세로 비율 — 가로/세로 */
    const val ART_ASPECT: Float = 2f / 3f

    /**
     * 컨테이너가 원화보다 **가로로 넓은지** (가로형 태블릿·폴더블 펼침).
     *
     * 이 경우 [ContentScale.Crop] 으로 채우면 2:3 세로 원화의 위아래가 잘려
     * 머리가 날아간다 — 대신 앰비언트 배경 + 2:3 프레임([fittedArtWidthDp])으로 그린다.
     */
    fun isWideArtContainer(widthDp: Float, heightDp: Float): Boolean =
        heightDp > 0f && widthDp / heightDp > ART_ASPECT + 0.02f

    /**
     * 컨테이너 높이를 꽉 채우면서 2:3을 유지하는 원화 폭(dp).
     * 넓은 화면에서는 `높이 × 2/3`, 좁은 화면에서는 컨테이너 폭 그대로.
     */
    fun fittedArtWidthDp(widthDp: Float, heightDp: Float): Float {
        val byHeight = heightDp * ART_ASPECT
        return if (byHeight < widthDp) byHeight else widthDp
    }

    // ── HUD 크기 (태블릿 대화면에서 UI 가 구석에 작게 몰리지 않게) ──────────

    /** 휴대폰 기준 짧은 변(dp) — 이 크기면 배율 1.0 */
    const val PHONE_SHORT_SIDE_DP: Float = 420f

    /** 태블릿 배율 상한 — 너무 커지면 한 화면에 정보가 안 들어간다 */
    const val MAX_UI_SCALE: Float = 1.8f

    /**
     * 화면 짧은 변 기준 UI 배율. 휴대폰(≤420dp)은 1.0, 태블릿 가로(≈800dp)는 1.8 로 커진다.
     * 사이드 레일·상단 재화 바·하단 캐릭터 패널의 글자·여백·터치 영역에 곱해 쓴다.
     */
    fun uiScale(widthDp: Float, heightDp: Float): Float =
        (minOf(widthDp, heightDp) / PHONE_SHORT_SIDE_DP).coerceIn(1f, MAX_UI_SCALE)

    /** 태블릿·대화면 여부 — 배율이 1.0 보다 큰지로 판단 */
    fun isLargeScreen(widthDp: Float, heightDp: Float): Boolean = uiScale(widthDp, heightDp) > 1.02f

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
