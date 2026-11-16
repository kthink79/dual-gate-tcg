package com.game.tcg.ui

/**
 * 카드 타일 배치 규칙 — 2026-11-16 사용자 요청 반영.
 *
 * 사용자 원문: 「덱이나 전투 진행시 캐릭터가 너무 작고, 일부분만 보이는 경우가 많아
 * 화면에는 최대 12장만 보이게 해주고 스크롤로 위 아래 움직일수 있게 하고
 * 덱 구성이나 전투일때 캐릭터를 크기에 맞춰서 보이게 해」
 *
 * 그래서 카드 규격을 코드가 아니라 **규칙 객체**로 고정하고, 화면은 이 값을 그대로 쓴다.
 *  - 원본 일러스트는 540×810 (2:3 세로) → 타일도 [TILE_ASPECT] 2:3 으로 그린다
 *  - 아트는 잘라내지 않고 타일 안에 꼭 맞춘다([ART_FIT]) — 얼굴/몸통이 잘리지 않는다
 *  - 한 화면에 보이는 카드는 최대 [MAX_VISIBLE_CARDS]장, 나머지는 세로 스크롤
 *
 * Compose 를 쓰지 않으므로 JVM 단위 테스트에서 그대로 검증한다.
 */
object CardLayoutRules {

    /** 카드 원본 세로 비율의 분자 (가로) */
    const val ASPECT_WIDTH: Float = 2f

    /** 카드 원본 세로 비율의 분모 (세로) */
    const val ASPECT_HEIGHT: Float = 3f

    /** 카드 타일 가로/세로 비율 = 2:3 */
    const val TILE_ASPECT: Float = ASPECT_WIDTH / ASPECT_HEIGHT

    /** 한 화면에 배치하는 카드 최대 수 */
    const val MAX_VISIBLE_CARDS: Int = 12

    /** 덱·전투 그리드 열 수 (3열 → 타일이 충분히 커진다) */
    const val GRID_COLUMNS: Int = 3

    /** 컬렉션 그리드 열 수 (3열) */
    const val COLLECTION_COLUMNS: Int = 3

    /** 카드 아트는 잘라내지 않고 전체가 보이도록 맞춘다 (ContentScale.Fit) */
    const val ART_FIT: Boolean = true

    /** 카드 수 → 필요한 줄 수 (12장이면 4줄) */
    fun rowsFor(cardCount: Int): Int {
        val safe = cardCount.coerceAtLeast(0)
        return (safe + GRID_COLUMNS - 1) / GRID_COLUMNS
    }

    /** 12장을 넘으면 한 화면에 다 담을 수 없으므로 스크롤이 필요하다 */
    fun requiresScroll(cardCount: Int): Boolean = cardCount > MAX_VISIBLE_CARDS

    /**
     * 화면 폭에서 카드 1장의 폭(dp)을 계산한다.
     * @param screenWidthDp 화면 폭
     * @param columns 열 수
     * @param spacingDp 타일 사이 간격
     * @param horizontalPaddingDp 좌우 여백 합
     */
    fun tileWidthDp(
        screenWidthDp: Float,
        columns: Int = GRID_COLUMNS,
        spacingDp: Float = 6f,
        horizontalPaddingDp: Float = 20f
    ): Float {
        val cols = columns.coerceAtLeast(1)
        val usable = (screenWidthDp - horizontalPaddingDp - spacingDp * (cols - 1)).coerceAtLeast(1f)
        return usable / cols
    }

    /** 카드 1장의 높이(dp) = 폭 × 3/2 */
    fun tileHeightDp(tileWidthDp: Float): Float = tileWidthDp / TILE_ASPECT
}
