package com.game.tcg.ui

/**
 * 글자 크기 규칙 — 2026-11-16 사용자 요청 「글씨가 너무 작다」 대응,
 * 2026-11-26 사용자 요청 「메뉴 화면 글자 확대·설명문 축소」로 한 단계 더 상향.
 *
 * 실기기 스크린샷(1080×2400 · 393dp 폭)에서 본문·배지가 6~11sp 로 찍혀
 * 읽기 어려웠다. 이제 화면 코드는 sp 를 직접 적지 않고 **이 표의 값**을 쓴다.
 *
 * - 화면 제목([TITLE]) 20~24sp → 24sp
 * - 섹션 제목([SECTION]) 18sp
 * - 본문([BODY]) 14~16sp → 15sp, 보조 본문([BODY_SMALL]) 14sp
 * - 배지·버튼·표 캡션([LABEL]) 13~14sp → 13sp — **이보다 작은 글씨는 쓰지 않는다**
 * - 카드 타일 위 아주 작은 표식([TILE_MARK]) 12sp
 *
 * 값은 Float 로 두어 Compose 없이 JVM 단위 테스트에서 최소 크기를 검증한다
 * (`UiReadabilityAndCardRulesTest`).
 */
object TcgType {

    /** 화면 제목 (FIGHT · DECK · 성장 등) — 상한 24sp */
    const val TITLE: Float = 24f

    /** 섹션 카드 제목 */
    const val SECTION: Float = 18f

    /** 본문 */
    const val BODY: Float = 15f

    /** 보조 본문 (설명·힌트) */
    const val BODY_SMALL: Float = 14f

    /** 배지 · 버튼 · 표 캡션 */
    const val LABEL: Float = 13f

    /** 강조 수치 (전투력·골드 등 큰 숫자) */
    const val NUMBER: Float = 18f

    /** 카드 타일 위 아주 작은 표식(NO.·등급)도 이 값 아래로 내려가지 않는다 */
    const val TILE_MARK: Float = 12f

    /** 아이콘 탭·초상화 그리드 안의 큰 심볼 크기 */
    const val ICON: Float = 18f

    /** 가독성 하한선 — 이보다 작은 크기는 화면에서 쓰지 않는다 */
    const val MIN_READABLE: Float = TILE_MARK

    /** 본문 최소값 검증용 목록 (테스트가 순회한다) */
    val BODY_LEVEL_SIZES: List<Float> = listOf(SECTION, BODY, BODY_SMALL, LABEL)

    /** 2026-11-26 상향 목표 구간 — 제목 20~24 · 본문 14~16 · 배지 13~14 */
    val TITLE_RANGE: ClosedFloatingPointRange<Float> = 20f..24f
    val BODY_RANGE: ClosedFloatingPointRange<Float> = 14f..16f
    val LABEL_RANGE: ClosedFloatingPointRange<Float> = 13f..14f

    /** 스케일 전 구간에서 최소 가독 크기를 지키는지 */
    fun allAtLeastMinimum(): Boolean = BODY_LEVEL_SIZES.all { it >= MIN_READABLE }

    /** 2026-11-26 상향 목표 구간 안에 들어왔는지 */
    fun withinExpandedScale(): Boolean =
        TITLE in TITLE_RANGE && BODY in BODY_RANGE && LABEL in LABEL_RANGE &&
            SECTION >= 18f && NUMBER >= TITLE * 0.7f
}
