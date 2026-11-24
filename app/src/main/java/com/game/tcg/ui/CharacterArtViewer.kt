package com.game.tcg.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.model.VisualState

/**
 * 캐릭터 관리 — **4종 일러스트 전체화면 감상 뷰어** (2026-11-24 사용자 요청).
 *
 * 사용자 원문:
 * > "Tcg 게임 캐릭터 관리 메뉴에서 보유 캐릭터를 더 잘 볼수 있게 해줘 4종을 각각 볼수 있게도
 * >  해주고 확대도 할수 있고 보유 케릭터들을 다 둘러 볼때 편하게 구현해줘"
 *
 *  - ① 전투 원화 · ② 일상복 · ③ 데이트 · ④ 3D 치비 미니어처를 **탭으로 각각 크게** 감상
 *  - 두 손가락 핀치 확대·축소(최대 [MAX_SCALE]배), 확대 상태에서 **드래그 이동**
 *  - **더블 탭** = 확대/원래 크기 토글, 화면 오른쪽 위 ✕ · 뒤로 가기로 닫기
 *  - 하단 ‹ › 버튼으로 **보유 캐릭터를 그 자리에서 계속 넘겨 본다**
 *
 * 잠긴 복장(일상 30 · 데이트 70)은 탭에 🔒 로 표시되고 눌러도 전환되지 않는다.
 */
private const val MAX_SCALE = 6f

@Composable
fun CharacterArtViewer(
    cardId: Int,
    cardName: String,
    bondLevel: Int,
    initialVisual: VisualState,
    browseIds: List<Int>,
    onSelectCard: (Int) -> Unit,
    onClose: () -> Unit
) {
    var visual by remember(cardId) { mutableStateOf(initialVisual) }
    var zoomed by remember(cardId, visual) { mutableStateOf(false) }

    BackHandler { onClose() }

    val index = browseIds.indexOf(cardId)
    val prevId = if (index > 0) browseIds[index - 1] else null
    val nextId = if (index >= 0 && index < browseIds.lastIndex) browseIds[index + 1] else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF70B0A12))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 상단 바 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "#${cardId} ${cardName}",
                        color = TcgColors.Text,
                        fontSize = TcgType.BODY.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        if (zoomed) "핀치·드래그로 이동 · 더블 탭으로 원래 크기" else "더블 탭 또는 두 손가락으로 확대",
                        color = TcgColors.Muted,
                        fontSize = TcgType.TILE_MARK.sp,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(TcgColors.SurfaceAlt)
                        .border(1.dp, TcgColors.Line, RoundedCornerShape(50))
                        .clickable { onClose() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("✕ 닫기", color = TcgColors.Text, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── 확대 가능한 일러스트 ──
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ZoomableArt(
                    cardId = cardId,
                    visual = visual,
                    modifier = Modifier.fillMaxSize(),
                    onZoomChanged = { zoomed = it }
                )
            }

            // ── 4종 선택 탭 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VisualState.values().forEachIndexed { i, state ->
                    val unlocked = AffinityRules.isVisualUnlocked(state, bondLevel)
                    val selected = visual == state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) elementTint(state).copy(alpha = 0.9f) else TcgColors.SurfaceAlt)
                            .border(
                                1.dp,
                                if (selected) TcgColors.Gold else TcgColors.Line,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = unlocked) { visual = state }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1} ${visualShortLabel(state)}${if (unlocked) "" else " 🔒"}",
                            color = when {
                                selected -> Color(0xFF120E1C)
                                unlocked -> TcgColors.Text
                                else -> TcgColors.Muted
                            },
                            fontSize = TcgType.LABEL.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // ── 좌우로 캐릭터 계속 넘기기 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavButton(
                    label = "‹ 이전",
                    enabled = prevId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    prevId?.let { onSelectCard(it) }
                }
                Text(
                    if (index >= 0) "${index + 1} / ${browseIds.size}" else "-",
                    color = TcgColors.Muted,
                    fontSize = TcgType.TILE_MARK.sp,
                    fontWeight = FontWeight.Bold
                )
                NavButton(
                    label = "다음 ›",
                    enabled = nextId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    nextId?.let { onSelectCard(it) }
                }
            }
        }
    }
}

/**
 * 핀치 확대·축소 + 확대 상태 드래그 + 더블 탭 토글.
 * 확대하지 않은 상태(1배)에서는 이동을 무시해 화면 스크롤·스와이프를 방해하지 않는다.
 */
@Composable
fun ZoomableArt(
    cardId: Int,
    visual: VisualState,
    modifier: Modifier = Modifier,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    var scale by remember(cardId, visual) { mutableFloatStateOf(1f) }
    var offset by remember(cardId, visual) { mutableStateOf(Offset.Zero) }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
        onZoomChanged(false)
    }

    Box(
        modifier = modifier
            .pointerInput(cardId, visual) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val target = (scale * zoom).coerceIn(1f, MAX_SCALE)
                    scale = target
                    if (target <= 1.001f) {
                        offset = Offset.Zero
                        onZoomChanged(false)
                    } else {
                        val maxX = (size.width * (target - 1f)) / 2f
                        val maxY = (size.height * (target - 1f)) / 2f
                        offset = Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                        onZoomChanged(true)
                    }
                }
            }
            .pointerInput(cardId, visual) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.001f) {
                            reset()
                        } else {
                            scale = 2.5f
                            onZoomChanged(true)
                        }
                    }
                )
            }
    ) {
        CardArtImage(
            cardId = cardId,
            visual = visual,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            targetWidthPx = 1080
        )
    }
}

@Composable
private fun NavButton(label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) TcgColors.SurfaceAlt else TcgColors.Surface)
            .border(1.dp, if (enabled) TcgColors.Violet else TcgColors.Line, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) TcgColors.Text else TcgColors.Muted,
            fontSize = TcgType.BODY_SMALL.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** 뷰어 탭에 쓰는 짧은 이름 (전투 · 일상 · 데이트 · 미니어처) */
fun visualShortLabel(state: VisualState): String = when (state) {
    VisualState.BATTLE -> "전투"
    VisualState.CASUAL_CUTE -> "일상"
    VisualState.ROMANTIC_DATE -> "데이트"
    VisualState.MINIATURE -> "미니어처"
}

private fun elementTint(state: VisualState): Color = when (state) {
    VisualState.BATTLE -> TcgColors.Hp
    VisualState.CASUAL_CUTE -> TcgColors.Heal
    VisualState.ROMANTIC_DATE -> TcgColors.Gold
    VisualState.MINIATURE -> TcgColors.Abyss
}
