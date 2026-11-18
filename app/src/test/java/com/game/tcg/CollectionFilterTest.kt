package com.game.tcg

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import com.game.tcg.ui.CatalogFilter
import com.game.tcg.ui.CatalogViewMode
import com.game.tcg.ui.CollectionCatalog
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 캐릭터 관리(도감·합성) 목록 개편 검증 — 2026-11-18 사용자 요청.
 *
 * 번호순 나열만 있던 목록에 ① 번호순/속성별 묶음/성급별 묶음 ② 속성·성급 필터
 * ③ 보유만 보기·보유 우선 정렬 ④ 카드별 속성 배지·성급 표시를 더했다.
 */
class CollectionFilterTest {

    private val profiles = TcgGameViewModel.profiles()
    private val midBoss = TcgGameViewModel.midBossProfiles()
    private val starOf: (Int) -> Int = { id -> if (id <= 10) 3 else if (id <= 20) 5 else 1 }

    @Test
    fun `보기 방식은 번호순·속성별·성급별 세 가지다`() {
        assertEquals(3, CatalogViewMode.entries.size)
        assertTrue(CatalogViewMode.NUMBER.label.contains("번호"))
        assertTrue(CatalogViewMode.ELEMENT.label.contains("속성"))
        assertTrue(CatalogViewMode.STAR.label.contains("성급"))
    }

    @Test
    fun `기존 전체 신규 기존 보스 필터가 그대로 동작한다`() {
        val all = CollectionCatalog.filterByPrimary(profiles, CatalogFilter.ALL, midBoss)
        val newWave = CollectionCatalog.filterByPrimary(profiles, CatalogFilter.NEW_WAVE, midBoss)
        val original = CollectionCatalog.filterByPrimary(profiles, CatalogFilter.ORIGINAL, midBoss)
        val boss = CollectionCatalog.filterByPrimary(profiles, CatalogFilter.BOSS, midBoss)

        assertEquals(170, all.size)
        assertEquals(15, newWave.size)
        assertEquals(155, original.size)
        assertEquals(6, boss.size)
        assertTrue("신규는 155~169", newWave.all { it.cardId in 155..169 })
        assertTrue("보스는 170~175", boss.all { it.cardId in 170..175 })
    }

    @Test
    fun `속성 필터는 그 속성만 남는다`() {
        val fire = CollectionCatalog.applyFilters(profiles, element = Element.FIRE)
        assertTrue(fire.isNotEmpty())
        assertTrue(fire.all { it.element == Element.FIRE })
        assertTrue("전체보다 적다", fire.size < profiles.size)

        // 기존 필터와 조합 — 신규 15인 + 빛 속성
        val newWave = CollectionCatalog.filterByPrimary(profiles, CatalogFilter.NEW_WAVE, midBoss)
        val newLight = CollectionCatalog.applyFilters(newWave, element = Element.LIGHT)
        assertTrue(newLight.all { it.cardId in 155..169 && it.element == Element.LIGHT })
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
        // 미보유도 성급 높은 순 → 번호순
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
        assertEquals("빛부터 차례대로", Element.entries.map { it.displayName }, groups.map { it.title })
        assertEquals("묶음 합 = 전체", profiles.size, groups.sumOf { it.entries.size })
        groups.forEach { group ->
            val element = Element.valueOf(group.key)
            assertTrue("${group.title} 묶음은 같은 속성만", group.entries.all { it.element == element })
        }
    }

    @Test
    fun `성급별 묶음은 5성부터 1성까지 내림차순이다`() {
        val groups = CollectionCatalog.group(profiles, CatalogViewMode.STAR, starOf)
        assertEquals(3, groups.size) // 5성·3성·1성만 존재
        assertEquals("5성", groups[0].key, "star_5")
        assertEquals("3성", groups[1].key, "star_3")
        assertEquals("1성", groups[2].key, "star_1")
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
    fun `카드 한 칸에 속성 배지와 성급이 함께 보인다`() {
        val profile = profiles.first { it.cardId == 1 }
        val label = CollectionCatalog.entryLabel(profile, owned = true) + " " + CollectionCatalog.starText(3)
        assertTrue(label.contains("#1"))
        assertTrue(label.contains(profile.displayName))
        assertTrue("성급 표기", label.contains("★★★"))
        assertFalse("보유 카드에는 자물쇠 없음", label.contains("🔒"))

        val locked = CollectionCatalog.entryLabel(profile, owned = false)
        assertTrue("미보유는 자물쇠", locked.contains("🔒"))

        Element.entries.forEach { element ->
            assertTrue("속성 배지 문구", CollectionCatalog.elementBadge(element).isNotBlank())
            assertTrue("속성 색", CollectionCatalog.elementTint(element) != 0L)
        }
    }

    @Test
    fun `필터를 모두 걸면 결과가 비는 조합도 있다`() {
        val none = CollectionCatalog.applyFilters(
            profiles,
            element = Element.FIRE,
            star = 5,
            ownedIds = emptySet(),
            starOf = { 1 }
        )
        assertTrue("불가능한 조합은 빈 목록", none.isEmpty())
    }

    @Test
    fun `신규 15인은 모두 도감에 남아 있다`() {
        val newWave = CollectionCatalog.filterByPrimary(profiles, CatalogFilter.NEW_WAVE, midBoss)
        assertTrue(newWave.all { CardCatalog.find(it.cardId) != null })
    }
}
