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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 비주얼 전투(VS) 화면과 덱 편성(DECK) 화면이 함께 쓰는 카드 타일.
 *
 * 레퍼런스(타 TCG `FIGHT`/`DECK` 화면)의 구성을 그대로 따른다.
 *  - 좌상단: 카드 전투력 (ATK+DEF)/100
 *  - 우상단: 등급(★) + 속성 배지
 *  - 좌하단: NO.{도감번호}
 *  - 우하단: 속성 원형 아이콘
 *  - 하단: HP 바(있을 때) · 장착/잠금/선택 표시
 * 모든 일러스트는 [CardArtImage]가 화면 크기에 맞춰 축소 디코딩하므로 12칸 그리드에서도 OOM이 없다.
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
    cornerRadius: Dp = 10.dp,
    targetWidthPx: Int = 300,
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
        highlighted -> 2.5.dp
        equipped -> 2.dp
        else -> 1.dp
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(TcgColors.SurfaceAlt)
            .border(borderWidth, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        CardArtImage(
            cardId = cardId,
            visual = visual,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = artAlignment,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = targetWidthPx
        )

        // 하단 가독성 스크림
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD90A0714))))
        )

        // 좌상단 전투력
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(3.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC120E1C))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                "$power",
                color = if (starLevel >= 5) Color(0xFFFFD54F) else TcgColors.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // 우상단 등급·속성 배지
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xCC120E1C))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    when {
                        starLevel >= 5 -> "LEGEND"
                        starLevel >= 4 -> "RARE"
                        starLevel >= 3 -> "UNCOMMON"
                        else -> "COMMON"
                    },
                    color = borderColor,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // 우하단 속성 아이콘
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(elementColor(element).copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(elementGlyph(element), fontSize = 9.sp)
        }

        // 좌하단 NO. 라벨 + 상태 배지
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 3.dp, bottom = 3.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (statusBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBadgeColor.copy(alpha = 0.92f))
                        .padding(horizontal = 4.dp)
                ) {
                    Text(statusBadge, color = Color(0xFF120E1C), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "NO.$cardId",
                color = Color(0xFFEDE7F6),
                fontSize = 7.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (hpRatio != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
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
        }
        if (hpText != null) {
            Text(
                hpText,
                color = Color(0xFFEFE9F7),
                fontSize = 6.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp)
            )
        }

        // 장착 체크 (컬렉션 장착중 표시)
        if (equipped) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(TcgColors.Violet.copy(alpha = 0.85f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("장착중 ✓", color = Color(0xFF120E1C), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
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
                fontSize = 9.sp,
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
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x66FFFFFF), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("＋", color = Color(0xCCFFFFFF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xCCFFFFFF), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        Text(hint, color = Color(0x99FFFFFF), fontSize = 6.sp, maxLines = 1)
    }
}

/** 레벨로 잠긴 덱 슬롯 */
@Composable
fun LockedSlotTile(
    unlockLevel: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0x55000000))
            .border(1.dp, Color(0x55FFFFFF), shape)
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔒", fontSize = 13.sp)
        Text("Lv.$unlockLevel", color = Color(0xFFE6DDF7), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0x99FFFFFF), fontSize = 6.sp)
    }
}

/** 속성 원형 아이콘 (카드 타일 밖에서 쓰는 큰 배지) */
@Composable
fun ElementBadge(element: Element, size: Dp = 16.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(elementColor(element))
            .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(elementGlyph(element), fontSize = (size.value * 0.45f).sp)
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
    targetWidthPx: Int = 300,
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
        Text(name, color = color, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
