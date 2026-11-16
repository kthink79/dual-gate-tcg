package com.game.tcg.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.PackDefinition
import com.game.tcg.core.model.PackKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * 봉인 카드팩 그래픽 — 2026-11-16 사용자 요청
 * 「카드팩 상점은 텍스트 박스만 나열돼 있다. 봉인 카드팩 그래픽·속성 오라·구성 배지를 넣어라」.
 *
 * 이미지 리소스를 늘리지 않고 Compose Canvas 로 그린다(APK 용량·CI 영향 없음).
 *  - 본체: 등급/속성 색 그라데이션 + 금속 테두리
 *  - 봉인: 카드팩 위를 가로지르는 리본 + 중앙 인장(속성 문양)
 *  - 오라: 인장 뒤로 퍼지는 빛무리 + 별가루
 */
@Composable
fun PackArtwork(
    pack: PackDefinition,
    modifier: Modifier = Modifier
) {
    val accent = packAccent(pack)
    val glyph = when {
        pack.element != null -> elementGlyph(pack.element)
        pack.kind == PackKind.PREMIUM -> "💎"
        else -> "🎴"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.34f), Color(0xFF120E1C)))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bodyLeft = w * 0.16f
            val bodyTop = h * 0.08f
            val bodyW = w * 0.68f
            val bodyH = h * 0.84f

            // 오라 (인장 뒤 빛무리)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w / 2f, h * 0.42f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w / 2f, h * 0.42f)
            )

            // 카드팩 본체
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.55f), Color(0xFF1B1430)),
                    startY = bodyTop,
                    endY = bodyTop + bodyH
                ),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f)
            )
            // 금속 테두리
            drawRoundRect(
                color = Color(0xFFFFE08A),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f),
                style = Stroke(width = w * 0.022f)
            )
            // 카드팩 안쪽 어두운 창
            drawRoundRect(
                color = Color(0x33000000),
                topLeft = Offset(bodyLeft + w * 0.07f, bodyTop + h * 0.10f),
                size = Size(bodyW - w * 0.14f, bodyH - h * 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f)
            )

            // 리본 (세로) — 봉인
            val ribbonW = w * 0.14f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFF3C4), Color(0xFFFFD54F), Color(0xFFC79A2B))
                ),
                topLeft = Offset(w / 2f - ribbonW / 2f, bodyTop),
                size = Size(ribbonW, bodyH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(ribbonW / 2f)
            )

            // 아래로 늘어지는 리본 꼬리
            val tail = Path().apply {
                moveTo(w / 2f - ribbonW / 2f, bodyTop + bodyH - h * 0.02f)
                lineTo(w / 2f - ribbonW * 1.5f, bodyTop + bodyH + h * 0.07f)
                lineTo(w / 2f - ribbonW * 0.2f, bodyTop + bodyH + h * 0.03f)
                close()
            }
            drawPath(tail, color = Color(0xFFE8B93A))

            // 별가루
            val sparkles = listOf(0.18f to 0.20f, 0.82f to 0.26f, 0.24f to 0.76f, 0.78f to 0.82f, 0.6f to 0.12f)
            sparkles.forEachIndexed { index, (px, py) ->
                val r = if (index % 2 == 0) w * 0.028f else w * 0.018f
                drawCircle(
                    color = Color(0xFFFFF6D0).copy(alpha = 0.85f),
                    radius = r,
                    center = Offset(w * px, h * py)
                )
            }
            // 인장 고리 (얇은 광선)
            val rayRadius = w * 0.20f
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45).toDouble())
                val sx = (w / 2f + cos(angle).toFloat() * rayRadius)
                val sy = (h * 0.42f + sin(angle).toFloat() * rayRadius)
                drawCircle(
                    color = Color(0xFFFFF3C4).copy(alpha = 0.55f),
                    radius = w * 0.012f,
                    center = Offset(sx, sy)
                )
            }
        }

        // 중앙 인장 (속성 문양)
        Box(
            modifier = Modifier
                .padding(top = 0.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xE6120E1C))
                .border(2.dp, Color(0xFFFFE08A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, fontSize = 22.sp)
        }

        // 구성 장수 배지
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xE6120E1C))
                .border(1.dp, accent, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "${pack.cardCount}장",
                color = Color(0xFFFFF3C4),
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** 카드팩 강조색 — 속성팩은 속성 색, 고급팩은 금색 */
fun packAccent(pack: PackDefinition): Color = when {
    pack.element != null -> elementColor(pack.element)
    pack.kind == PackKind.PREMIUM -> TcgColors.Gold
    else -> TcgColors.Violet
}

/** 카드팩 강조색 (속성만 넘길 때) */
fun packAccentOf(element: Element?): Color =
    element?.let { elementColor(it) } ?: TcgColors.Violet
