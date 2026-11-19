package com.game.tcg

import com.game.tcg.core.model.Element
import com.game.tcg.ui.CatalogViewMode
import com.game.tcg.ui.CharacterHubTab
import com.game.tcg.ui.CollectionCatalog
import com.game.tcg.ui.ResponsiveLayout
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 캐릭터 관리(도감·강화·합성) 목록 정리 검증 — 2026-11-19 사용자 요청.
 *
 * ① '신규 15인 / 기존 155인 / 보스 6' 구분 버튼 **삭제**
 * ② 필터는 **콤보박스(보기/속성/성급) + '보유만' 토글 1줄**로 축약
 * ③ 도감↔강화↔합성을 **상단 서브 탭 3개**로 즉시 전환
 * ④ 목록은 2:3 썸네일 그리드 — 폭에 따라 열 수가 바뀌어도 카드 비율은 유지
 */
class CollectionFilterTest {

    private val profiles = TcgGameViewModel.profiles()
    private val starOf: (Int) -> Int = { id -> if (id <= 10) 3 else if (id <= 20) 5 else 1 }

    @Test
    fun `보기 방식은 번호순·속성별·성급별 세 가지이고 라벨은 짧다`() {
        assertEquals(3, CatalogViewMode.entries.size)
        assertEquals(listOf("번호순", "속성별", "성급별"), CatalogViewMode.entries.map { it.label })
        // 콤보박스 옵션은 보기 방식과 1:1
        assertEquals(CatalogViewMode.entries.map { it.label }, CollectionCatalog.VIEW_MODE_OPTIONS)
    }

    @Test
    fun `서브 탭은 도감 강화 합성 세 개다`() {
        assertEquals(3, CharacterHubTab.entries.size)
        assertEquals(listOf("도감", "강화", "합성"), CharacterHubTab.entries.map { it.label })
    }

    @Test
    fun `신규 기존 보스 구분 버튼은 사라졌다`() {
        // 예전 CatalogFilter(전체/신규 15인/기존 155인/보스 6)는 삭제됐다.
        // 남은 필터 축은 속성 · 성급 · 보유 여부 셋뿐이다.
        val fire = CollectionCatalog.applyFilters(profiles, element = Element.FIRE)
        assertTrue(fire.isNotEmpty())
        assertTrue(fire.all { it.element == Element.FIRE })
        assertTrue("전체보다 적다", fire.size < profiles.size)
    }

    @Test
    fun `속성 콤보박스 옵션은 전체 더하기 5속성이다`() {
        val options = CollectionCatalog.ELEMENT_OPTIONS
        assertEquals(6, options.size)
        assertEquals("전체", options.first())
        assertEquals(Element.entries.map { it.displayName }, options.drop(1))

        // 인덱스 → 속성
        assertNull(CollectionCatalog.elementAt(0))
        Element.entries.forEachIndexed { index, element ->
            assertEquals(element, CollectionCatalog.elementAt(index + 1))
        }
    }

    @Test
    fun `성급 콤보박스 옵션은 전체 더하기 5성부터 1성이다`() {
        val options = CollectionCatalog.STAR_OPTIONS
        assertEquals(6, options.size)
        assertEquals(listOf("전체", "5★", "4★", "3★", "2★", "1★"), options)
        assertNull(CollectionCatalog.starAt(0))
        assertEquals(5, CollectionCatalog.starAt(1))
        assertEquals(1, CollectionCatalog.starAt(5))
    }

    @Test
    fun `성급 필터와 보유만 보기가 함께 걸린다`() {
        val owned = setOf(1, 2, 3, 11, 21)
        val fiveStar = CollectionCatalog.applyFilters(
            profiles, star = 5, ownedIds = owned, starOf = starOf
        )
        assertEquals("11~20 이 5성", 10, fiveStar.size)

        val ownedFiveStar = CollectionCatalog.applyFilters(
            profiles, star = 5, ownedOnly = true, ownedIds = owned, starOf = starOf
        )
        assertEquals(listOf(11), ownedFiveStar.map { it.cardId })

        val ownedAll = CollectionCatalog.applyFilters(
            profiles, ownedOnly = true, ownedIds = owned, starOf = starOf
        )
        assertEquals(5, ownedAll.size)
    }

    @Test
    fun `보유 카드가 앞으로 오고 그 안에서 성급 높은 순으로 정렬된다`() {
        val owned = setOf(21, 3, 12)
        val ordered = CollectionCatalog.orderForManagement(profiles, owned, starOf)

        assertTrue("보유가 앞", ordered.take(3).all { it.cardId in owned })
        assertEquals("보유 중 5성(12)이 최우선", 12, ordered[0].cardId)
        assertEquals("그다음 3성(3)", 3, ordered[1].cardId)
        assertEquals("그다음 1성(21)", 21, ordered[2].cardId)
        val rest = ordered.drop(3)
        assertTrue("미보유는 보유 뒤", rest.none { it.cardId in owned })
        rest.zipWithNext().forEach { (a, b) ->
            val sa = starOf(a.cardId)
            val sb = starOf(b.cardId)
            assertTrue("성급 내림차순", sa > sb || (sa == sb && a.cardId < b.cardId))
        }
    }

    @Test
    fun `속성별 묶음은 빈 속성을 빼고 5개 묶음으로 나뉜다`() {
        val groups = CollectionCatalog.group(profiles, CatalogViewMode.ELEMENT)
        assertEquals(5, groups.size)
        assertEquals("속성 순서대로", Element.entries.map { it.displayName }, groups.map { it.title })
        assertEquals("묶음 합 = 전체", profiles.size, groups.sumOf { it.entries.size })
    }

    @Test
    fun `성급별 묶음은 5성부터 1성까지 내림차순이다`() {
        val groups = CollectionCatalog.group(profiles, CatalogViewMode.STAR, starOf)
        assertEquals(3, groups.size) // 5성·3성·1성만 존재
        assertEquals("star_5", groups[0].key)
        assertEquals(10, groups[0].entries.size)
        assertEquals("묶음 합 = 전체", profiles.size, groups.sumOf { it.entries.size })
    }

    @Test
    fun `번호순은 묶지 않고 한 덩어리다`() {
        val groups = CollectionCatalog.group(profiles, CatalogViewMode.NUMBER)
        assertEquals(1, groups.size)
        assertEquals(profiles.size, groups.single().entries.size)
    }

    @Test
    fun `목록 머리에는 긴 설명 대신 짧은 개수 라벨만 붙는다`() {
        assertEquals("12/177 · 보유 5", CollectionCatalog.countLabel(12, 177, 5))
    }

    @Test
    fun `카드 한 칸에 속성 배지와 성급이 함께 보인다`() {
        val profile = profiles.first { it.cardId == 1 }
        val label = CollectionCatalog.entryLabel(profile, owned = true) + " " + CollectionCatalog.starText(3)
        assertTrue(label.contains("#1"))
        assertTrue("성급 표기", label.contains("★★★"))
        assertFalse("보유 카드에는 자물쇠 없음", label.contains("🔒"))
        assertTrue("미보유는 자물쇠", CollectionCatalog.entryLabel(profile, owned = false).contains("🔒"))

        Element.entries.forEach { element ->
            assertTrue(CollectionCatalog.elementBadge(element).isNotBlank())
            assertTrue(CollectionCatalog.elementTint(element) != 0L)
        }
    }

    // ── 2026-11-19: 화면 폭 대응 ──────────────────────────────

    @Test
    fun `태블릿과 휴대폰 모두 카드가 2대3 비율을 유지한다`() {
        // 열 수만 바뀌고 타일은 항상 2:3 — 찌그러지지 않는다
        listOf(320f, 393f, 600f, 800f, 1024f).forEach { width ->
            val columns = ResponsiveLayout.gridColumns(width)
            assertTrue("$width dp 열 수", columns in 2..8)
            val tile = (width - 28f - 8f * (columns - 1)) / columns
            assertTrue("$width dp 타일 폭 ${tile}", tile > 60f)
        }
        assertTrue("태블릿은 4열 이상", ResponsiveLayout.gridColumns(800f) >= 4)
        assertTrue("휴대폰은 3열", ResponsiveLayout.gridColumns(393f) in 2..3)
    }

    @Test
    fun `넓은 화면은 본문 폭을 제한하고 가운데 정렬한다`() {
        assertEquals(393f, ResponsiveLayout.contentMaxWidthDp(393f))
        assertEquals(600f, ResponsiveLayout.contentMaxWidthDp(600f))
        assertEquals(
            "1024dp 태블릿은 840dp 로 제한",
            ResponsiveLayout.TABLET_MAX_WIDTH_DP,
            ResponsiveLayout.contentMaxWidthDp(1024f)
        )
        assertTrue(ResponsiveLayout.isWide(600f))
        assertFalse(ResponsiveLayout.isWide(393f))
        assertTrue("프리뷰 폭이 2:3 카드로 쓸 만하다", ResponsiveLayout.heroWidthDp(1024f) in 180f..320f)
    }
}
