package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 비주얼 전투(VS) 화면과 덱 편성(DECK) 화면이 함께 쓰는 카드 타일.
 *
 * 2026-11-16 사용자 요청 반영 (실기기 스크린샷 25048 · 25054):
 *  - **카드 비율 2:3** — 일러스트 원본(540×810)과 같은 세로 카드. 호출부가
 *    `Modifier.aspectRatio(CardLayoutRules.TILE_ASPECT)` 를 준다
 *  - **아트는 잘라내지 않는다** — 기본 [artScale] 이 `ContentScale.Fit` 이라
 *    캐릭터 얼굴·몸통이 잘리지 않고 타일 안에 꼭 맞는다
 *  - **글씨 확대** — 전투력 15sp · 등급/NO./상태 11sp (이전 6~9sp)
 *  - 정보 오버레이는 위·아래 모서리에만 두어 얼굴을 가리지 않는다
 */
@Composable
fun DuelCardTile(
    cardId: Int,
    visual: VisualState,
    element: Element,
    starLevel: Int,
    power: Int,
    modifier: Modifier = Modifier,
    hpRatio: Float? = null,
    hpText: String? = null,
    highlighted: Boolean = false,
    dimmed: Boolean = false,
    equipped: Boolean = false,
    statusBadge: String? = null,
    statusBadgeColor: Color = TcgColors.Danger,
    artAlignment: Alignment = Alignment.Center,
    cornerRadius: Dp = 12.dp,
    targetWidthPx: Int = 380,
    artScale: ContentScale = if (CardLayoutRules.ART_FIT) ContentScale.Fit else ContentScale.Crop,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = when {
        highlighted -> TcgColors.Gold
        equipped -> TcgColors.Violet
        starLevel >= 5 -> Color(0xFFFFC94D)
        starLevel >= 4 -> Color(0xFFB388FF)
        else -> Color(0xFF8C86A6)
    }
    val borderWidth = when {
        highlighted -> 3.dp
        equipped -> 2.dp
        else -> 1.5.dp
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(TcgColors.SurfaceAlt)
            .border(borderWidth, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        // 카드 원본 비율 그대로 — Fit 이므로 좌우가 비면 카드 배경색이 받쳐 준다
        CardArtImage(
            cardId = cardId,
            visual = visual,
            contentDescription = null,
            contentScale = artScale,
            alignment = artAlignment,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = targetWidthPx
        )

        // 위·아래 가독성 스크림 (얼굴을 가리지 않도록 22%만)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.22f)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xB30A0714), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.30f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE00A0714))))
        )

        // 좌상단 전투력
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xD9120E1C))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                "$power",
                color = if (starLevel >= 5) Color(0xFFFFD54F) else TcgColors.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }

        // 우상단 등급 배지
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xD9120E1C))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                when {
                    starLevel >= 5 -> "★5"
                    starLevel >= 4 -> "★4"
                    starLevel >= 3 -> "★3"
                    else -> "★2"
                },
                color = borderColor,
                fontSize = TcgType.TILE_MARK.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // 우하단 속성 아이콘
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(elementColor(element).copy(alpha = 0.95f))
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(elementGlyph(element), fontSize = 13.sp)
        }

        // 좌하단 상태 배지 + NO. + HP 바
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 5.dp, bottom = 5.dp, end = 32.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (statusBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(statusBadgeColor.copy(alpha = 0.95f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        statusBadge,
                        color = Color(0xFF120E1C),
                        fontSize = TcgType.TILE_MARK.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
            Text(
                "NO.$cardId",
                color = Color(0xFFEDE7F6),
                fontSize = TcgType.TILE_MARK.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (hpRatio != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2B2340))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hpRatio.coerceIn(0.001f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    hpRatio <= 0.15f -> TcgColors.Danger
                                    hpRatio <= 0.4f -> Color(0xFFFFB74D)
                                    else -> TcgColors.Heal
                                }
                            )
                    )
                }
            }
            if (hpText != null) {
                Text(
                    "HP $hpText",
                    color = Color(0xFFEFE9F7),
                    fontSize = TcgType.TILE_MARK.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // 장착 표시 — 얼굴이 아니라 하단 띠에 둔다
        if (equipped) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(TcgColors.Violet.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "장착중 ✓",
                    color = Color(0xFF120E1C),
                    fontSize = TcgType.TILE_MARK.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // 피격·전투불능 어둡게
        if (dimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC0120818))
            )
            Text(
                "전투불능",
                color = TcgColors.Danger,
                fontSize = TcgType.BODY_SMALL.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/** 덱 편성 화면의 빈 슬롯 */
@Composable
fun EmptySlotTile(
    label: String,
    modifier: Modifier = Modifier,
    hint: String = "탭하여 장착",
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0x33FFFFFF))
            .border(1.5.dp, Color(0x66FFFFFF), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("＋", color = Color(0xCCFFFFFF), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xE6FFFFFF), fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.SemiBold)
        Text(hint, color = Color(0x99FFFFFF), fontSize = TcgType.TILE_MARK.sp, maxLines = 1)
    }
}

/** 레벨로 잠긴 덱 슬롯 */
@Composable
fun LockedSlotTile(
    unlockLevel: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0x55000000))
            .border(1.dp, Color(0x55FFFFFF), shape)
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔒", fontSize = 24.sp)
        Text("Lv.$unlockLevel", color = Color(0xFFE6DDF7), fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0x99FFFFFF), fontSize = TcgType.TILE_MARK.sp, textAlign = TextAlign.Center)
    }
}

/** 속성 원형 아이콘 (카드 타일 밖에서 쓰는 큰 배지) */
@Composable
fun ElementBadge(element: Element, size: Dp = 22.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(elementColor(element))
            .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(elementGlyph(element), fontSize = (size.value * 0.5f).sp)
    }
}

fun elementGlyph(element: Element): String = when (element) {
    Element.FIRE -> "🔥"
    Element.WIND -> "🍃"
    Element.ICE -> "❄"
    Element.LIGHT -> "✦"
    Element.DARK -> "☾"
}

/** [TcgCard]에서 바로 타일을 그리는 편의 함수 (카탈로그 조회 없이) */
@Composable
fun DuelCardTileFor(
    card: TcgCard,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    dimmed: Boolean = false,
    equipped: Boolean = false,
    hpRatio: Float? = null,
    hpText: String? = null,
    statusBadge: String? = null,
    statusBadgeColor: Color = TcgColors.Danger,
    targetWidthPx: Int = 380,
    onClick: (() -> Unit)? = null
) {
    DuelCardTile(
        cardId = card.id,
        visual = card.currentVisual,
        element = card.element,
        starLevel = card.starLevel,
        power = (card.atk + card.def) / 100,
        modifier = modifier,
        hpRatio = hpRatio,
        hpText = hpText,
        highlighted = highlighted,
        dimmed = dimmed,
        equipped = equipped,
        statusBadge = statusBadge,
        statusBadgeColor = statusBadgeColor,
        targetWidthPx = targetWidthPx,
        onClick = onClick
    )
}

/**
 * 배경 도트 패턴 — 레퍼런스의 폴리카보네이트 질감을 가볍게 재현한다.
 * 그라데이션은 호출부가 깔고, 이 모디파이어는 점만 얹는다.
 */
fun Modifier.dotPattern(color: Color = Color.White.copy(alpha = 0.07f), step: Float = 34f): Modifier =
    drawBehind {
        var y = step / 2f
        while (y < size.height) {
            var x = step / 2f
            while (x < size.width) {
                drawCircle(color = color, radius = 1.6f, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }

/** 카드 타일 아래 붙는 이름 라벨 */
@Composable
fun CardNameLabel(name: String, modifier: Modifier = Modifier, color: Color = Color(0xFFEDE7F6)) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(2.dp))
        Text(name, color = color, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
