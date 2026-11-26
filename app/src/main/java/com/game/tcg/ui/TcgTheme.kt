package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 카드 도감 번호 + 4종 비주얼 상태 → 실제 드로어블 리소스.
 * 매핑은 [CharacterArtRegistry]가 단일 관리하며, 미등록 번호는 null을 돌려준다.
 * 화면에서는 [CardArtImage]를 사용해 화면 크기에 맞춰 축소 디코딩한다.
 */
fun characterImage(cardId: Int, visual: VisualState): Int? =
    CharacterArtRegistry.resId(cardId, visual)

/**
 * 스크롤 가능한 화면 본문.
 *
 * 2026-11-19 사용자 요청: 태블릿·폴더블처럼 넓은 화면에서도
 * 카드(2:3)·일러스트 비율이 유지되도록 **본문 최대 폭을 제한하고 가운데 정렬**한다.
 * (휴대폰은 화면 폭 그대로 — 달라지는 것이 없다)
 */
@Composable
fun ScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background)
    ) {
        val contentMaxWidth = ResponsiveLayout.contentMaxWidthDp(maxWidth.value).dp
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/**
 * 스크롤 없이 한 화면에 담는 본문 — 2026-11-26 사용자 요청
 * 「소환·교감·해금을 세로로 쌓지 말고 탭으로 나눠 스크롤 없이 한 화면에」.
 *
 * [ScreenFrame] 과 같은 폭 제한·여백을 쓰되 **verticalScroll 을 두지 않는다**.
 * 각 화면은 `Modifier.weight(1f)` 로 남는 높이를 그림·그리드에 넘겨
 * 어떤 화면비에서도 스크롤 없이 들어간다(넘칠 때만 내부 목록이 스크롤).
 */
@Composable
fun CompactFrame(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background)
    ) {
        val contentMaxWidth = ResponsiveLayout.contentMaxWidthDp(maxWidth.value).dp
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * 아이콘 중심 탭 바 — 2026-11-26 사용자 요청
 * 「상·하단 중복 설명 문구를 걷어내고 아이콘 + 짧은 1~2단어 라벨로」.
 *
 * 각 탭은 `아이콘(위) · 라벨(아래)` 두 줄짜리 작은 칸이고, 글씨는 [TcgType.LABEL] 이상이다.
 */
@Composable
fun TcgIconTabBar(
    tabs: List<Pair<String, String>>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    accent: Color = TcgColors.Violet,
    onSelect: (Int) -> Unit
) {
    if (tabs.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF120E1C))
            .border(1.dp, TcgColors.Line, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (selected) accent.copy(alpha = 0.92f) else Color(0xFF1E1830)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(icon, fontSize = TcgType.ICON.sp, color = if (selected) Color(0xFF120E1C) else TcgColors.Text)
                Text(
                    label,
                    color = if (selected) Color(0xFF120E1C) else TcgColors.Muted,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 초상화 아이콘 타일 — 2026-11-26 사용자 요청
 * 「세로 리스트를 3~4열 초상화 아이콘 그리드로 압축」.
 *
 * 2:3 원본 비율 그대로(Fit) 그려 얼굴이 잘리지 않고, 선택·잠금 상태만 테두리로 알린다.
 */
@Composable
fun PortraitIconTile(
    cardId: Int,
    visual: VisualState,
    modifier: Modifier = Modifier,
    label: String? = null,
    badge: String? = null,
    selected: Boolean = false,
    locked: Boolean = false,
    accent: Color = TcgColors.Violet,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val borderColor = when {
        selected -> TcgColors.Gold
        locked -> TcgColors.Line
        else -> accent.copy(alpha = 0.55f)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CardLayoutRules.TILE_ASPECT)
                .clip(RoundedCornerShape(9.dp))
                .background(TcgColors.SurfaceAlt)
                .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(9.dp))
        ) {
            CardArtImage(
                cardId = cardId,
                visual = visual,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = 220
            )
            if (locked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xB30B0A12)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = TcgType.ICON.sp)
                }
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC120E1C))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(badge, color = TcgColors.Gold, fontSize = TcgType.TILE_MARK.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        if (label != null) {
            Text(
                label,
                color = if (selected) TcgColors.Gold else TcgColors.Muted,
                fontSize = TcgType.TILE_MARK.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 가로 꽉 찬 탭 바 — 2026-11-16 사용자 요청
 * 「메뉴가 화면에 다 들어가지 못하면 탭을 만들어서 반영해 주면 돼」.
 *
 * 탭이 2~4개일 때 화면 폭을 똑같이 나눠 쓰고, 선택된 탭은 강조색으로 채운다.
 * 글씨는 [TcgType.BODY] 로 읽기 쉽게 키웠다.
 */
@Composable
fun TcgTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    accent: Color = TcgColors.Violet,
    onSelect: (Int) -> Unit
) {
    if (tabs.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF120E1C))
            .border(1.dp, TcgColors.Line, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (selected) {
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.62f))
                            )
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF1E1830), Color(0xFF1E1830)))
                        }
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color(0xFF120E1C) else TcgColors.Muted,
                    fontSize = TcgType.BODY_SMALL.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    title,
                    color = TcgColors.Text,
                    fontSize = TcgType.SECTION.sp,
                    fontWeight = FontWeight.Bold
                )
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
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = if (filled) Color(0xFF120E1C) else color,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** 게이지 바 */
@Composable
fun StatBar(ratio: Float, color: Color, modifier: Modifier = Modifier, height: Int = 10) {
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
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 13.sp)
        Spacer(Modifier.width(5.dp))
        Column {
            Text(label, color = TcgColors.Muted, fontSize = TcgType.LABEL.sp)
            Text(value, color = color, fontSize = TcgType.BODY.sp, fontWeight = FontWeight.Bold)
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
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(
            label,
            color = if (enabled) TcgColors.Text else TcgColors.Muted,
            fontSize = TcgType.BODY.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp
        )
        if (hint != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                hint,
                color = if (enabled) accent else TcgColors.Muted,
                fontSize = TcgType.LABEL.sp,
                lineHeight = 16.sp
            )
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
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF120E1C) else TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
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
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        lines.takeLast(maxLines).forEach { line ->
            Text("· $line", color = TcgColors.Muted, fontSize = TcgType.BODY_SMALL.sp, lineHeight = 19.sp)
        }
        if (lines.isEmpty()) {
            Text("· 기록 없음", color = accent.copy(alpha = 0.6f), fontSize = TcgType.BODY_SMALL.sp)
        }
    }
}

/**
 * 콤보박스(드롭다운) 1개 — 2026-11-19 사용자 요청.
 *
 * > "상단 5줄 필터 버튼·장황한 설명 텍스트를 대폭 줄이고
 * >  콤보박스(보기 기준/속성/성급) + '보유만' 토글 1줄로 압축해 달라."
 *
 * 칩을 여러 줄 늘어놓는 대신 `라벨 값 ▾` 한 칸으로 접고, 누르면 목록이 열린다.
 */
@Composable
fun TcgDropdown(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    accent: Color = TcgColors.Violet,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(TcgColors.SurfaceAlt)
                .border(1.dp, if (expanded) accent else TcgColors.Line, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TcgColors.Muted, fontSize = TcgType.LABEL.sp, maxLines = 1)
            Spacer(Modifier.width(5.dp))
            Text(
                value,
                color = TcgColors.Text,
                fontSize = TcgType.BODY_SMALL.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            Text("▾", color = accent, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                val isCurrent = option == value
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (isCurrent) accent else TcgColors.Text,
                            fontSize = TcgType.BODY_SMALL.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}

/** '보유만' 같은 1줄 토글 — 콤보박스 옆에 나란히 놓는다 */
@Composable
fun TcgToggleChip(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = TcgColors.Heal,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) accent.copy(alpha = 0.22f) else TcgColors.SurfaceAlt)
            .border(1.dp, if (checked) accent else TcgColors.Line, RoundedCornerShape(10.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (checked) "✓" else "○", color = if (checked) accent else TcgColors.Muted, fontSize = TcgType.LABEL.sp)
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            color = if (checked) TcgColors.Text else TcgColors.Muted,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
