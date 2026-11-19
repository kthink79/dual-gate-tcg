package com.game.tcg.ui

import com.game.tcg.core.model.Element

/**
 * 캐릭터 관리(도감·강화·합성) 목록의 보기 방식 — 2026-11-18 요청, 2026-11-19 슬림화.
 *
 * 2026-11-19 사용자 요청(스크린샷 피드백):
 *  - '신규 15인 / 기존 155인 / 보스 6' 구분 버튼 **완전 삭제**
 *  - 상단 5줄 필터 버튼 → **콤보박스(보기 기준/속성/성급) + '보유만' 1줄**로 압축
 *  - 도감↔강화↔합성을 **상단 서브 탭**으로 바로 전환(선택 캐릭터 유지)
 *
 * 화면(Compose)과 분리해 순수 함수로 두었기 때문에 JVM 단위 테스트에서 그대로 검증한다.
 */
enum class CatalogViewMode(val label: String) {
    NUMBER("번호순"),
    ELEMENT("속성별"),
    STAR("성급별")
}

/** 도감·성장 허브의 상단 서브 탭 — 선택한 캐릭터를 그대로 들고 이동한다 */
enum class CharacterHubTab(val label: String) {
    CATALOG("도감"),
    UPGRADE("강화"),
    SYNTHESIS("합성")
}

/** 묶음 머리글 1개 */
data class CatalogGroup(
    val key: String,
    val title: String,
    /** 묶음 강조색 (ARGB Long) — 속성 묶음은 속성색, 성급 묶음은 금색 계열 */
    val tint: Long,
    val entries: List<CharacterProfile>
)

/**
 * 도감 목록 계산 — 필터 → 보유 우선 정렬 → 묶음.
 *
 * @param ownedIds 보유 카드 번호
 * @param starOf 카드 번호 → 성급 (미보유는 1)
 */
object CollectionCatalog {

    /** 콤보박스 '속성' 옵션 — 0번이 '전체' */
    val ELEMENT_OPTIONS: List<String> = listOf("전체") + Element.entries.map { it.displayName }

    /** 콤보박스 '성급' 옵션 — 0번이 '전체' (5★ → 1★) */
    val STAR_OPTIONS: List<String> = listOf("전체") + (5 downTo 1).map { "$it★" }

    /** 콤보박스 '보기 기준' 옵션 — [CatalogViewMode] 순서와 1:1 */
    val VIEW_MODE_OPTIONS: List<String> = CatalogViewMode.entries.map { it.label }

    /** 옵션 인덱스 → 속성 (0 = 전체 = null) */
    fun elementAt(index: Int): Element? =
        Element.entries.getOrNull(index - 1)

    /** 옵션 인덱스 → 성급 (0 = 전체 = null) */
    fun starAt(index: Int): Int? =
        if (index in 1..5) 6 - index else null

    /** 속성·성급·보유 필터를 한 번에 적용한다 */
    fun applyFilters(
        profiles: List<CharacterProfile>,
        element: Element? = null,
        star: Int? = null,
        ownedOnly: Boolean = false,
        ownedIds: Set<Int> = emptySet(),
        starOf: (Int) -> Int = { 1 }
    ): List<CharacterProfile> = profiles.filter { profile ->
        (element == null || profile.element == element) &&
            (star == null || starOf(profile.cardId) == star) &&
            (!ownedOnly || profile.cardId in ownedIds)
    }

    /**
     * 보유 우선 정렬 — 보유 카드를 앞에, 그 안에서는 성급 높은 순 → 번호순.
     * (미보유는 뒤에서 번호순)
     */
    fun orderForManagement(
        profiles: List<CharacterProfile>,
        ownedIds: Set<Int>,
        starOf: (Int) -> Int = { 1 },
        ownedFirst: Boolean = true
    ): List<CharacterProfile> {
        if (!ownedFirst) return profiles.sortedBy { it.cardId }
        return profiles.sortedWith(
            compareByDescending<CharacterProfile> { it.cardId in ownedIds }
                .thenByDescending { starOf(it.cardId) }
                .thenBy { it.cardId }
        )
    }

    /** 보기 방식에 따른 묶음 — 번호순은 묶지 않고 한 덩어리로 돌려준다 */
    fun group(
        profiles: List<CharacterProfile>,
        mode: CatalogViewMode,
        starOf: (Int) -> Int = { 1 }
    ): List<CatalogGroup> = when (mode) {
        CatalogViewMode.NUMBER -> listOf(
            CatalogGroup(key = "number", title = "번호순", tint = 0xFFB388FF, entries = profiles)
        )
        CatalogViewMode.ELEMENT -> Element.entries.mapNotNull { element ->
            val entries = profiles.filter { it.element == element }
            if (entries.isEmpty()) null else CatalogGroup(
                key = element.name,
                title = element.displayName,
                tint = elementTint(element),
                entries = entries
            )
        }
        CatalogViewMode.STAR -> (5 downTo 1).mapNotNull { star ->
            val entries = profiles.filter { starOf(it.cardId) == star }
            if (entries.isEmpty()) null else CatalogGroup(
                key = "star_$star",
                title = "${star}성 " + starText(star),
                tint = 0xFFFFD54F,
                entries = entries
            )
        }
    }

    /** 카드 한 칸의 표시 문구 — 번호 + 이름 + 보유 표시 */
    fun entryLabel(profile: CharacterProfile, owned: Boolean): String =
        "#${profile.cardId} ${profile.displayName}" + if (owned) "" else " 🔒"

    /** 카드의 성급 표기 (`★★★`) */
    fun starText(star: Int): String = "★".repeat(star.coerceIn(1, 5))

    /** 카드 모서리에 붙는 속성 한 글자 배지 */
    fun elementBadge(element: Element): String = when (element) {
        Element.FIRE -> "🔥"
        Element.WIND -> "🌪"
        Element.ICE -> "❄"
        Element.LIGHT -> "✨"
        Element.DARK -> "🌑"
    }

    /** 속성 강조색 (ARGB) */
    fun elementTint(element: Element): Long = when (element) {
        Element.FIRE -> 0xFFFF7043
        Element.WIND -> 0xFF4DB6AC
        Element.ICE -> 0xFF64B5F6
        Element.LIGHT -> 0xFFFFD54F
        Element.DARK -> 0xFF7C4DFF
    }

    /** 한 줄 요약 — 긴 설명문 대신 목록 머리에 붙는 짧은 라벨 */
    fun countLabel(shown: Int, total: Int, owned: Int): String =
        "$shown/$total · 보유 $owned"
}
