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

    // ── 가로 모드 높이 맞춤 프레임 (2026-11-21 태블릿 가로 대응) ──────────────

    /**
     * 가로 모드에서 게임을 담는 세로 프레임의 가로/세로 비율 — 카드 원화와 같은 2:3.
     *
     * 2026-11-21 사용자 피드백:
     * > "tcg 게임 테블릿 에서 실행하면 여전히 가로 모드일때 풀 화면에 맞춰 지면서 영상이 안보여
     * >  가로 모드일때는 높이에 맞춰 화면 사이즈가 조절 되도록 해줘"
     *
     * 태블릿을 가로로 들면 화면이 `width >> height` 가 되는데, 앱이 폭을 기준으로 배치되면
     * 세로 카드·영상(16:9 무대 포함)이 화면 높이를 넘어 아래로 밀려나 보이지 않는다.
     * 그래서 가로 모드에서는 **높이를 기준으로** 2:3 세로 프레임을 만들고 양옆은 배경으로 채운다.
     */
    const val LANDSCAPE_FRAME_ASPECT: Float = 2f / 3f

    /** 가로 모드(폭 > 높이)인지 — 휴대폰 가로 회전도 포함 */
    fun isLandscape(widthDp: Float, heightDp: Float): Boolean =
        widthDp > 0f && heightDp > 0f && widthDp > heightDp

    /**
     * 가로 모드에서 게임 프레임의 폭(dp) — **높이를 꽉 채우고** 2:3을 유지한다.
     * 화면이 프레임보다 좁으면(비정상적으로 좁은 창) 화면 폭을 그대로 쓴다.
     */
    fun portraitFrameWidthDp(widthDp: Float, heightDp: Float): Float {
        val byHeight = heightDp * LANDSCAPE_FRAME_ASPECT
        return if (byHeight < widthDp) byHeight else widthDp
    }

    /** 가로 모드에서 프레임 양옆에 남는 여백 폭(dp) — 배경(앰비언트)을 그리는 영역 */
    fun landscapeSideBarWidthDp(widthDp: Float, heightDp: Float): Float =
        ((widthDp - portraitFrameWidthDp(widthDp, heightDp)) / 2f).coerceAtLeast(0f)

    /**
     * 16:9 영상 무대의 크기 — **높이를 넘지 않게** 폭을 줄인다.
     *
     * 가로 모드에서 `fillMaxWidth()` 만 주면 폭 × 9/16 높이가 화면을 넘어 영상이 밑으로 밀린다.
     * 컨테이너 폭·높이 중 먼저 닿는 쪽에 맞춘다(레터박스).
     */
    fun fitVideoWidthDp(containerWidthDp: Float, containerHeightDp: Float, aspect: Float = 16f / 9f): Float {
        if (containerWidthDp <= 0f || containerHeightDp <= 0f) return 0f
        val byHeight = containerHeightDp * aspect
        return if (byHeight < containerWidthDp) byHeight else containerWidthDp
    }

    // ── 등장 영상 TextureView 변환 (2026-11-28 v0.6.1 태블릿 비율 버그) ─────────

    /**
     * [android.view.TextureView.setTransform] 에 넣을 (scaleX, scaleY).
     *
     * TextureView 는 `setTransform` 을 주지 않으면(항등원) 영상을 **뷰 크기 전체로 늘려**
     * 그린다. 그래서 변환은 '뷰 대비 배율'이어야 한다 — 예전 코드처럼
     * `viewWidth / videoWidth`(픽셀 비율)를 곱하면 태블릿 고해상도 뷰에서 영상이
     * 1.5~2배로 확대돼 잘리거나 찌그러졌다(사용자 제보).
     *
     * 반환값은 가운데를 기준으로 줄이는 배율이다(한 축은 항상 1 — 레터박스).
     *  - 영상이 뷰보다 **가로로 길면** 가로를 꽉 채우고 세로를 줄인다
     *  - 세로로 길면 세로를 꽉 채우고 가로를 줄인다
     */
    fun videoFitScale(
        viewWidthDp: Float,
        viewHeightDp: Float,
        videoWidthPx: Float,
        videoHeightPx: Float
    ): Pair<Float, Float> {
        if (viewWidthDp <= 0f || viewHeightDp <= 0f || videoWidthPx <= 0f || videoHeightPx <= 0f) {
            return 1f to 1f
        }
        val viewAspect = viewWidthDp / viewHeightDp
        val videoAspect = videoWidthPx / videoHeightPx
        return if (videoAspect > viewAspect) {
            1f to (viewAspect / videoAspect)
        } else {
            (videoAspect / viewAspect) to 1f
        }
    }

    /**
     * 컨테이너 안에 들어가면서 영상 비율을 유지하는 (폭, 높이) — dp 단위 값.
     *
     * 16:9 만 오던 시절의 [fitVideoWidthDp] 를 비율 일반형으로 넓힌 것.
     * 2:3 · 9:16 같은 세로 영상이 와도 무대가 그 비율대로 커져 레터박스 여백이 사라진다.
     */
    fun fitVideoSizeDp(
        containerWidthDp: Float,
        containerHeightDp: Float,
        aspect: Float
    ): Pair<Float, Float> {
        if (containerWidthDp <= 0f || containerHeightDp <= 0f || aspect <= 0f) return 0f to 0f
        val width = fitVideoWidthDp(containerWidthDp, containerHeightDp, aspect)
        return width to (width / aspect)
    }

    // ── 짧은 화면(가로 모드) 판정 ──────────────────────────────────────────

    /** 높이가 이보다 낮으면 '한 화면에 다 넣기'보다 스크롤·축소가 유리한 가로 모드로 본다 */
    const val SHORT_VIEWPORT_DP: Float = 560f

    /** 높이가 좁은 화면(폰 가로 480dp·태블릿 가로 프레임 등)인지 */
    fun isShortViewport(heightDp: Float): Boolean = heightDp > 0f && heightDp < SHORT_VIEWPORT_DP

    /** 가로로 넓고 낮은 화면(폰 가로·태블릿 가로 다이얼로그)인지 */
    fun isWideAndShort(widthDp: Float, heightDp: Float): Boolean =
        isLandscape(widthDp, heightDp) && isShortViewport(heightDp)
}
