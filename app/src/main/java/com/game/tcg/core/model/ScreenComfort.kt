package com.game.tcg.core.model

/**
 * 화면 여백·탭 구성 규칙 — 2026-11-27 v0.6.0 사용자 요청.
 *
 * 사용자 원문(대기열 `tcg_v060_bottom_padding_deck_tabs_and_diamond_shop_expansion`):
 *  - "전 화면 하단 패딩 및 `navigationBarsPadding()` 여유 공간을 충분히 확보해
 *     최하단 버튼/타일 클릭이 편하도록 조정"
 *  - "`DECK › 서약·정화`를 ⚔️ 출전 덱 / 😈 마왕 정화 / ⭐ 대표 아이콘 탭으로 분리해
 *     대표 캐릭터 영역이 하단으로 밀려 잘리지 않게 개선"
 *
 * 값은 화면 코드가 아니라 여기 한 곳에 둈다 — 여백이 화면마다 달라지면
 * 어떤 화면은 다시 잘리고 어떤 화면은 쓸데없이 비기 때문이다(JVM 단위 테스트 대상).
 */
object ScreenComfort {

    /** 스크롤 없는 화면([com.game.tcg.ui.CompactFrame]) 맨 아래에 두는 클릭 여유 */
    const val COMPACT_FRAME_BOTTOM_MARGIN_DP: Int = 18

    /** 스크롤 화면([com.game.tcg.ui.ScreenFrame]) 마지막 내용 뒤에 두는 클릭 여유 */
    const val SCROLL_FRAME_BOTTOM_MARGIN_DP: Int = 28

    /** 내비게이션 바 인셋 위에 더 얹는 최소 여유 (제스처 영역과 겹치지 않게) */
    const val NAV_BAR_EXTRA_MARGIN_DP: Int = 10

    /** 하단 클릭 여유의 최소 허용치 — 테스트가 이 값을 지킨다 */
    const val MIN_BOTTOM_MARGIN_DP: Int = 12
}

/**
 * `DECK › 서약·정화` 화면의 아이콘 탭 3종 — 2026-11-27 v0.6.0.
 *
 * 이전에는 출전 덱 · 마왕 응시/정화 · 대표 캐릭터 3장을 세로로 쌓아
 * 대표 캐릭터 그리드가 화면 아래로 밀려 잘렸다. 이제 한 번에 한 영역만 보여 주고,
 * 각 영역이 남는 높이를 그대로 쓴다.
 */
enum class DeckPactTab(val icon: String, val label: String) {
    /** ⚔️ 출전 덱 — 덱 슬롯·전열/후열 배지·편성 수 */
    LINEUP("⚔️", "출전 덱"),

    /** 😈 마왕 정화 — 시선/저주 스택 · 응시 · 성수 정화 */
    PURIFY("😈", "마왕 정화"),

    /** ⭐ 대표 — 메인 로비 대표 캐릭터 그리드 */
    HERO("⭐", "대표");

    companion object {
        /** 화면 탭 바에 그대로 넘기는 (아이콘, 라벨) 목록 */
        val ICON_TABS: List<Pair<String, String>> = entries.map { it.icon to it.label }

        /** 화면을 처음 열 때 보이는 탭 — 덱 현황이 먼저다 */
        val DEFAULT: DeckPactTab = LINEUP
    }
}
