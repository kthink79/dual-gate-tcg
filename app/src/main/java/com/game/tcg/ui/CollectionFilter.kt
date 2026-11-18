package com.game.tcg.ui

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element

/**
 * 캐릭터 관리(도감·합성) 목록의 보기 방식 — 2026-11-18 사용자 요청.
 *
 * > "캐릭터 관리 화면은 번호순 2열 알약 버튼만 나열되어 보유 캐릭터를 찾고 강화·합성을 진행하기
 * >  매우 불편함. 속성별 묶음/필터, 성급별 묶음/필터, 보유만 보기(또는 보유 우선 정렬) 기능을
 * >  추가해 강화·합성 대상 캐릭터를 빠르게 찾고 선택할 수 있도록 UI/UX를 개편한다."
 *
 * 화면(Compose)과 분리해 순수 함수로 두었기 때문에 JVM 단위 테스트에서 그대로 검증한다.
 */
enum class CatalogViewMode(val label: String) {
    NUMBER("번호순"),
    ELEMENT("속성별 묶음"),
    STAR("성급별 묶음")
}

/** 도감 목록의 1차 필터 (기존 '전체 / 신규 / 기존 / 보스' 유지) */
enum class CatalogFilter(val label: String) {
    ALL("전체"),
    NEW_WAVE("신규 15인"),
    ORIGINAL("기존 155인"),
    BOSS("보스 6")
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

    /** 속성 필터 선택값 (null = 전체) */
    fun filterByPrimary(
        profiles: List<CharacterProfile>,
        filter: CatalogFilter,
        midBossProfiles: List<CharacterProfile>
    ): List<CharacterProfile> = when (filter) {
        CatalogFilter.ALL -> profiles
        CatalogFilter.NEW_WAVE -> profiles.filter { CardCatalog.isNewWave(it.cardId) }
        CatalogFilter.ORIGINAL -> profiles.filter { !CardCatalog.isNewWave(it.cardId) }
        CatalogFilter.BOSS -> midBossProfiles
    }

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
}
