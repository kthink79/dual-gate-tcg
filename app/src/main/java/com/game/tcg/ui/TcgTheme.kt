package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.R
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.VisualState

/**
 * 듀얼 게이트 다크 판타지 팔레트
 */
object TcgColors {
    val Background = Color(0xFF0B0A12)
    val Surface = Color(0xFF17131F)
    val SurfaceAlt = Color(0xFF241C33)
    val Line = Color(0xFF342B47)
    val Violet = Color(0xFFB388FF)
    val Gold = Color(0xFFFFD54F)
    val Text = Color(0xFFEDE7F6)
    val Muted = Color(0xFF9C90B4)
    val Danger = Color(0xFFFF6E6E)
    val Heal = Color(0xFF7BE495)
    val Mana = Color(0xFF64B5F6)
    val Hp = Color(0xFFEF5350)
    val Abyss = Color(0xFF7C4DFF)
}

fun elementColor(element: Element): Color = when (element) {
    Element.FIRE -> Color(0xFFFF7043)
    Element.WIND -> Color(0xFF4DB6AC)
    Element.ICE -> Color(0xFF64B5F6)
    Element.LIGHT -> Color(0xFFFFD54F)
    Element.DARK -> Color(0xFF7C4DFF)
}

/**
 * 카드 도감 번호 + 3단 비주얼 상태 → 실제 드로어블 리소스
 */
fun characterImage(cardId: Int, visual: VisualState): Int = when (cardId) {
    3 -> when (visual) {
        VisualState.BATTLE -> R.drawable.char_aurelia_battle
        VisualState.CASUAL_CUTE -> R.drawable.char_aurelia_casual
        VisualState.ROMANTIC_DATE -> R.drawable.char_aurelia_date
    }
    1 -> when (visual) {
        VisualState.BATTLE -> R.drawable.char_seraphiel_battle
        VisualState.CASUAL_CUTE -> R.drawable.char_seraphiel_casual
        VisualState.ROMANTIC_DATE -> R.drawable.char_seraphiel_date
    }
    2 -> when (visual) {
        VisualState.BATTLE -> R.drawable.char_elyria_battle
        VisualState.CASUAL_CUTE -> R.drawable.char_elyria_casual
        VisualState.ROMANTIC_DATE -> R.drawable.char_elyria_date
    }
    else -> when (visual) {
        VisualState.BATTLE -> R.drawable.char_lilith_battle
        VisualState.CASUAL_CUTE -> R.drawable.char_lilith_casual
        VisualState.ROMANTIC_DATE -> R.drawable.char_lilith_date
    }
}

/** 스크롤 가능한 화면 본문 */
@Composable
fun ScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** 섹션 카드 */
@Composable
fun SectionCard(
    title: String,
    badge: String? = null,
    accent: Color = TcgColors.Violet,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TcgColors.Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, color = TcgColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Pill(badge, accent)
                }
            }
            content()
        }
    }
}

/** 작은 라벨 */
@Composable
fun Pill(text: String, color: Color = TcgColors.Violet, filled: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color.copy(alpha = 0.9f) else color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            color = if (filled) Color(0xFF120E1C) else color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** 게이지 바 */
@Composable
fun StatBar(ratio: Float, color: Color, modifier: Modifier = Modifier, height: Int = 8) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2B2340))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(ratio.coerceIn(0.001f, 1f))
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/** 자원 표시 배지 */
@Composable
fun ResourceBadge(icon: String, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, color = TcgColors.Muted, fontSize = 8.sp)
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 선택지 / 액션 버튼 */
@Composable
fun ActionTile(
    label: String,
    hint: String? = null,
    accent: Color = TcgColors.Violet,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent.copy(alpha = 0.14f) else TcgColors.SurfaceAlt)
            .border(1.dp, if (enabled) accent.copy(alpha = 0.55f) else TcgColors.Line, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (enabled) TcgColors.Text else TcgColors.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (hint != null) {
            Spacer(Modifier.height(2.dp))
            Text(hint, color = if (enabled) accent else TcgColors.Muted, fontSize = 10.sp)
        }
    }
}

/** 탭 전환 칩 */
@Composable
fun ChipButton(label: String, selected: Boolean, accent: Color = TcgColors.Violet, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.9f) else TcgColors.SurfaceAlt)
            .border(1.dp, if (selected) accent else TcgColors.Line, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF120E1C) else TcgColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** 로그 박스 */
@Composable
fun LogBox(lines: List<String>, accent: Color = TcgColors.Violet, maxLines: Int = 6) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF100D18))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.takeLast(maxLines).forEach { line ->
            Text("· $line", color = TcgColors.Muted, fontSize = 10.sp, lineHeight = 15.sp)
        }
        if (lines.isEmpty()) {
            Text("· 기록 없음", color = accent.copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}
