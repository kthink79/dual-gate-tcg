package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.game.tcg.core.model.VisualState

/**
 * 2:3 세로 원화를 **가로·태블릿 화면에서도 머리가 잘리지 않게** 그리는 공통 배경.
 *
 * 2026-11-20 사용자 요청(태블릿 가로 실기 스크린샷):
 * > "로비/대표 캐릭터 화면이 가로형 태블릿 전체를 Crop 으로 채우면서 2:3 세로 원화가
 * >  과도하게 확대돼 머리 윗부분이 심하게 잘린다."
 *
 * 동작
 *  - 컨테이너가 원화보다 **세로로 긴**(휴대폰 세로) 경우: 예전과 같이 전체를 채운다(Crop).
 *  - 컨테이너가 **가로로 넓은**(태블릿·폴더블 펼침) 경우:
 *      ① 같은 원화를 화면에 가득 채우고 블러 + 어둡게 깐 앰비언트 레이어를 배경으로 두고,
 *      ② 중앙에 2:3 비율 그대로의 원화를 온전히(잘림 없이) 올린다.
 *
 * 판정·크기 계산은 [ResponsiveLayout] 의 순수 함수가 담당하므로 JVM 단위 테스트로 검증한다.
 */
@Composable
fun AmbientCharacterBackdrop(
    cardId: Int,
    visual: VisualState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    targetWidthPx: Int = 1080,
    ambientDarkness: Float = 0.72f
) {
    BoxWithConstraints(modifier) {
        val w = maxWidth.value
        val h = maxHeight.value
        val wide = ResponsiveLayout.isWideArtContainer(w, h)
        if (wide) {
            // ① 앰비언트 배경 — 같은 원화를 흐리게 깔아 좌우 여백이 비지 않게
            CardArtImage(
                cardId = cardId,
                visual = visual,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp),
                targetWidthPx = 360
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06040C).copy(alpha = ambientDarkness))
            )
            // ② 중앙 원화 — 2:3 프레임 안에 잘림 없이 (가로 여백은 앰비언트가 채운다)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(maxWidth * (ResponsiveLayout.fittedArtWidthDp(w, h) / w).coerceIn(0.1f, 1f))
            ) {
                CardArtImage(
                    cardId = cardId,
                    visual = visual,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = targetWidthPx
                )
            }
        } else {
            // 휴대폰 세로 — 기존과 동일한 전체 채움
            CardArtImage(
                cardId = cardId,
                visual = visual,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = targetWidthPx
            )
        }
    }
}

/**
 * 카드 원화가 아닌 배경(인트로 장면·탐색 구역 배너)을 같은 규칙으로 그린다.
 * 가로로 넓은 화면에서는 블러 앰비언트 + 2:3 원본 프레임, 세로 화면에서는 전체 채움.
 */
@Composable
fun AmbientResourceBackdrop(
    resId: Int?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    targetWidthPx: Int = 900,
    ambientDarkness: Float = 0.72f
) {
    if (resId == null || resId == 0) {
        ArtPlaceholder(modifier)
        return
    }
    BoxWithConstraints(modifier) {
        val w = maxWidth.value
        val h = maxHeight.value
        val wide = ResponsiveLayout.isWideArtContainer(w, h)
        if (wide) {
            ResourceArtImage(
                resId = resId,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp),
                targetWidthPx = 360
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06040C).copy(alpha = ambientDarkness))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(maxWidth * (ResponsiveLayout.fittedArtWidthDp(w, h) / w).coerceIn(0.1f, 1f))
            ) {
                ResourceArtImage(
                    resId = resId,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = targetWidthPx
                )
            }
        } else {
            ResourceArtImage(
                resId = resId,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = targetWidthPx
            )
        }
    }
}
