package com.game.tcg

import com.game.tcg.ui.ResponsiveLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 태블릿·가로 화면 대응 계산 검증 — 2026-11-20 사용자 요청.
 *
 * > "가로형 태블릿에서 2:3 세로 원화가 Crop 으로 채워지며 머리가 잘리고,
 * >  좌우 메뉴·상단 재화 바가 구석에 작게 몰린다."
 *
 * 화면 크기 판정·원화 프레임 폭·HUD 배율은 모두 [ResponsiveLayout] 의 순수 함수이므로
 * 여기서 기기별 값으로 고정해 둔다(실기기 없이 회귀를 잡는다).
 */
class ResponsiveTabletLayoutTest {

    // 대표 기기 해상도 (dp) — Compose 가 쓰는 화면 크기 단위
    private val phonePortrait = 411f to 866f      // Galaxy S 계열 세로
    private val phoneLandscape = 866f to 411f     // 같은 기기 가로
    private val tabletLandscape = 1280f to 800f   // 12.9" 태블릿 가로
    private val tabletPortrait = 800f to 1280f    // 같은 태블릿 세로
    private val foldableOpen = 673f to 841f       // 폴더블 펼침

    @Test
    fun `원화 비율은 2대3이다`() {
        assertEquals(2f / 3f, ResponsiveLayout.ART_ASPECT, 0.0001f)
    }

    @Test
    fun `세로 화면은 기존처럼 전체를 채우고 가로 화면만 앰비언트 프레임을 쓴다`() {
        assertFalse(
            "휴대폰 세로는 원화보다 세로로 길다 — Crop 유지",
            ResponsiveLayout.isWideArtContainer(phonePortrait.first, phonePortrait.second)
        )
        assertTrue(
            "폴더블 펼침(673×841)은 0.80 비율이라 2:3 보다 가로로 넓다 — 잘림 방지 모드",
            ResponsiveLayout.isWideArtContainer(foldableOpen.first, foldableOpen.second)
        )
        assertTrue(
            "태블릿 가로(1280×800)는 2:3 보다 가로로 넓다 — 잘림 방지 모드",
            ResponsiveLayout.isWideArtContainer(tabletLandscape.first, tabletLandscape.second)
        )
        assertTrue(
            "휴대폰 가로(866×411)도 잘림 방지 모드",
            ResponsiveLayout.isWideArtContainer(phoneLandscape.first, phoneLandscape.second)
        )
        assertFalse(
            "태블릿 세로는 다시 Crop 모드",
            ResponsiveLayout.isWideArtContainer(tabletPortrait.first, tabletPortrait.second)
        )
    }

    @Test
    fun `가로 화면의 원화 폭은 높이 기준 2대3으로 계산된다`() {
        // 800dp 높이 → 폭 533.3dp (화면 폭 1280dp 안에 온전히 들어간다)
        val tablet = ResponsiveLayout.fittedArtWidthDp(tabletLandscape.first, tabletLandscape.second)
        assertEquals(533.33f, tablet, 0.1f)
        assertTrue("원화가 화면보다 좁아 좌우 여백이 생긴다", tablet < tabletLandscape.first)

        // 휴대폰 가로(411dp 높이) → 폭 274dp
        assertEquals(274f, ResponsiveLayout.fittedArtWidthDp(phoneLandscape.first, phoneLandscape.second), 0.1f)

        // 세로 화면은 폭이 그대로 (기존 Crop 동작과 일치)
        assertEquals(
            phonePortrait.first,
            ResponsiveLayout.fittedArtWidthDp(phonePortrait.first, phonePortrait.second),
            0.1f
        )
    }

    @Test
    fun `HUD 배율은 휴대폰 1_0 태블릿 1_8로 커진다`() {
        assertEquals(1f, ResponsiveLayout.uiScale(phonePortrait.first, phonePortrait.second), 0.01f)
        assertEquals(1f, ResponsiveLayout.uiScale(360f, 780f), 0.01f)
        assertEquals(1.8f, ResponsiveLayout.uiScale(tabletLandscape.first, tabletLandscape.second), 0.01f)
        // 태블릿 세로도 짧은 변 800dp 라 같은 배율
        assertEquals(1.8f, ResponsiveLayout.uiScale(tabletPortrait.first, tabletPortrait.second), 0.01f)
        // 폴더블 펼침(짧은 변 673dp) → 1.6 배
        assertEquals(1.6f, ResponsiveLayout.uiScale(foldableOpen.first, foldableOpen.second), 0.05f)
    }

    @Test
    fun `배율 1_0 초과일 때만 대화면으로 본다`() {
        assertFalse(ResponsiveLayout.isLargeScreen(phonePortrait.first, phonePortrait.second))
        assertTrue(ResponsiveLayout.isLargeScreen(tabletLandscape.first, tabletLandscape.second))
        assertTrue(ResponsiveLayout.isLargeScreen(foldableOpen.first, foldableOpen.second))
    }

    // ── 2026-11-21 가로 모드 높이 맞춤 ────────────────────────────────────

    @Test
    fun `가로 모드일 때만 높이 맞춤 프레임을 쓴다`() {
        assertFalse("세로 화면은 전체 사용", ResponsiveLayout.isLandscape(phonePortrait.first, phonePortrait.second))
        assertFalse("태블릿 세로도 전체 사용", ResponsiveLayout.isLandscape(tabletPortrait.first, tabletPortrait.second))
        assertTrue("태블릿 가로", ResponsiveLayout.isLandscape(tabletLandscape.first, tabletLandscape.second))
        assertTrue("휴대폰 가로", ResponsiveLayout.isLandscape(phoneLandscape.first, phoneLandscape.second))
    }

    @Test
    fun `가로 모드 프레임은 높이를 채우고 폭은 2대3으로 줄어든다`() {
        // 1280×800 태블릿 가로 → 프레임 533.3×800 (화면 높이 그대로)
        val w = ResponsiveLayout.portraitFrameWidthDp(tabletLandscape.first, tabletLandscape.second)
        assertEquals(533.33f, w, 0.1f)
        assertEquals(ResponsiveLayout.LANDSCAPE_FRAME_ASPECT, w / tabletLandscape.second, 0.001f)
        // 프레임은 화면 폭을 넘지 않는다
        assertTrue(w <= tabletLandscape.first)
        // 양옆 여백은 (1280 - 533.3) / 2
        assertEquals((1280f - w) / 2f, ResponsiveLayout.landscapeSideBarWidthDp(tabletLandscape.first, tabletLandscape.second), 0.1f)
    }

    @Test
    fun `좁은 창에서는 프레임이 화면 폭을 넘지 않는다`() {
        // 폭이 높이×2/3 보다 좁으면 화면 폭 그대로 (음수·초과 방지)
        assertEquals(300f, ResponsiveLayout.portraitFrameWidthDp(300f, 800f), 0.01f)
        assertEquals(0f, ResponsiveLayout.landscapeSideBarWidthDp(300f, 800f), 0.01f)
    }

    @Test
    fun `등장 영상 무대는 컨테이너 높이를 넘지 않는 16대9 폭을 고른다`() {
        // 800dp 높이 컨테이너의 16:9 무대 → 폭 1422dp 가 아니라 높이 제한으로 355.5dp
        assertEquals(16f / 9f * 200f, ResponsiveLayout.fitVideoWidthDp(1280f, 200f), 0.01f)
        // 폭이 더 좁으면 폭이 기준
        assertEquals(300f, ResponsiveLayout.fitVideoWidthDp(300f, 800f), 0.01f)
        // 값이 0 이하이면 0 (레이아웃 붕괴 방지)
        assertEquals(0f, ResponsiveLayout.fitVideoWidthDp(0f, 100f), 0.01f)
    }
}
