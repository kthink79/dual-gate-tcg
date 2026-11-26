package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.VisualState
import kotlin.math.cos
import kotlin.math.sin

/**
 * ⚔️ 공격 카드 중앙 확대 컷인 · 타격 이펙트 (2026-11-16 사용자 요청).
 *
 * 공격이 확정될 때마다 공격자의 실제 일러스트가 화면 중앙으로 확대되며
 * 속성 빛 테두리 + `⚔️ 이름 · 공격!` 배너를 띄우고, 피격 카드에는
 * 흔들림·타격 플래시·참격/스파크·데미지 숫자가 함께 터진다.
 * 수치·곡선은 [BattleAutoDecor] 가 들고 있고, 이 파일은 그리기만 한다.
 */

/* ───────────────────────── 🤖 자동 전투 바 ───────────────────────── */

/**
 * 화면 상단의 `🤖 자동 전투 ON/OFF` + `1x/2x` 배속 토글.
 * 자동 전투 중에도 배속을 즉시 바꿀 수 있고, 끄면 곧바로 수동 조작으로 돌아간다.
 */
@Composable
fun AutoBattleBar(
    isAuto: Boolean,
    speed: BattleSpeed,
    hint: String,
    onToggleAuto: () -> Unit,
    onToggleSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 자동 전투 토글
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (isAuto) {
                        Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047), Color(0xFF7BE495)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0x66120E1E), Color(0x88120E1E)))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isAuto) Color(0xFFB9F6CA) else Color(0x66FFFFFF),
                    shape = RoundedCornerShape(50)
                )
                .clickable { onToggleAuto() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                BattleAutoDecor.autoLabel(isAuto),
                color = if (isAuto) Color(0xFF06210B) else Color(0xFFEDE7F6),
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(5.dp))
        // 배속 토글
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (speed == BattleSpeed.X2) {
                        Brush.horizontalGradient(listOf(Color(0xFFFF8A3D), Color(0xFFFFD54F)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0x66120E1E), Color(0x88120E1E)))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (speed == BattleSpeed.X2) Color(0xFFFFF59D) else Color(0x66FFFFFF),
                    shape = RoundedCornerShape(50)
                )
                .clickable { onToggleSpeed() }
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                "⏩ ${speed.label}",
                color = if (speed == BattleSpeed.X2) Color(0xFF2B1600) else Color(0xFFEDE7F6),
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            hint,
            color = Color(0xCCFFFFFF),
            fontSize = TcgType.TILE_MARK.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

/* ───────────────────── ⚔️ 중앙 확대 컷인 ───────────────────── */

/**
 * 공격 카드 컷인 오버레이 — 화면 중앙에만 그려 터치를 가로막지 않는다.
 * (뒤의 FIGHT 필·전술 시트는 컷인 중에도 그대로 눌린다)
 */
@Composable
fun BattleCutInOverlay(
    cutIn: BattleCutIn,
    speed: BattleSpeed,
    modifier: Modifier = Modifier
) {
    val accent = if (cutIn.fromEnemy) Color(0xFFFF5252) else elementColor(cutIn.element)
    val progress = remember(cutIn.tick) { Animatable(0f) }
    LaunchedEffect(cutIn.tick) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = BattleAutoDecor.cutInMillis(speed).toInt(),
                easing = LinearOutSlowInEasing
            )
        )
    }
    val p = progress.value
    val alpha = BattleAutoDecor.cutInAlpha(p)
    if (alpha <= 0.01f) return

    Box(
        modifier = modifier
            .width(196.dp)
            .graphicsLayer {
                this.alpha = alpha
                val scale = BattleAutoDecor.cutInScale(p)
                scaleX = scale
                scaleY = scale
                rotationZ = BattleAutoDecor.cutInTilt(p)
            },
        contentAlignment = Alignment.Center
    ) {
        // 속성 광륜 + 방사 스피드 라인
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
        )
        Canvas(modifier = Modifier.size(320.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outer = size.width * 0.5f
            repeat(14) { index ->
                val angle = (index / 14f) * Math.PI.toFloat() * 2f + p * 0.6f
                val start = outer * (0.42f + 0.12f * (index % 3))
                drawLine(
                    color = accent.copy(alpha = 0.55f * (1f - p * 0.6f)),
                    start = Offset(cx + cos(angle) * start, cy + sin(angle) * start),
                    end = Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
                    strokeWidth = 3f
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 공격자 카드 일러스트 — 속성 빛 테두리
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 206.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E0A16))
                    .border(3.dp, accent, RoundedCornerShape(16.dp))
            ) {
                CardArtImage(
                    cardId = cutIn.attackerCardId,
                    visual = VisualState.BATTLE,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter,
                    targetWidthPx = 480
                )
                // 안쪽 백색 라인 + 하단 스크림
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9100A1C)))
                        )
                )
                // 속성 배지
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(elementGlyph(cutIn.element), fontSize = TcgType.LABEL.sp)
                }
                if (cutIn.ultimate) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8A3D)))
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("ULTIMATE", color = Color(0xFF2B1600), fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            // ⚔️ 이름 · 공격! 배너
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xE6120E1E), accent, Color(0xE6120E1E))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    BattleAutoDecor.bannerText(cutIn),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1
                )
            }
            if (cutIn.subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    cutIn.subtitle,
                    color = Color(0xEEFFFFFF),
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/* ───────────────────── 💥 피격 타격 이펙트 ───────────────────── */

/**
 * 피격 카드 위에 얹는 참격·스파크 — [decay] 가 1 → 0 으로 줄면 사라진다.
 * (피격 직후 1, [BattleAutoDecor.shakeMillis] 동안 감쇠)
 */
@Composable
fun HitSparkLayer(
    decay: Float,
    color: Color,
    golden: Boolean,
    modifier: Modifier = Modifier
) {
    if (decay <= 0.01f) return
    val streak = if (golden) Color(0xFFFFE082) else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = BattleAutoDecor.slashProgress(decay)
        // 참격 3줄 — 대각으로 그어지며 사라진다
        repeat(3) { index ->
            val dx = (index - 1) * w * 0.14f
            drawLine(
                color = streak.copy(alpha = (decay * 0.9f).coerceIn(0f, 1f)),
                start = Offset(dx + w * 0.04f, h * 0.96f),
                end = Offset(dx + w * 0.96f, h * 0.04f + h * 0.12f * p),
                strokeWidth = 1.5f + 4.5f * decay
            )
        }
        // 불꽃 파편
        repeat(10) { index ->
            val angle = (index / 10f) * Math.PI.toFloat() * 2f + 0.4f
            val radius = w * (0.12f + 0.5f * p)
            drawCircle(
                color = (if (golden) Color(0xFFFFD54F) else color).copy(alpha = (decay * 0.95f).coerceIn(0f, 1f)),
                radius = 1.5f + 3.2f * decay,
                center = Offset(
                    x = w / 2f + cos(angle) * radius,
                    y = h / 2f + sin(angle) * radius * 0.75f
                )
            )
        }
    }
}

/**
 * 떠오르는 데미지 숫자 — `-1,240` + 상성/급소/오의 배지.
 * [cutIn] 이 바뀔 때마다 다시 떠오른다.
 */
@Composable
fun BattleDamageNumber(
    cutIn: BattleCutIn,
    color: Color,
    modifier: Modifier = Modifier
) {
    val anim = remember(cutIn.tick) { Animatable(0f) }
    LaunchedEffect(cutIn.tick) {
        anim.snapTo(0f)
        anim.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }
    val badge = BattleAutoDecor.badgeText(cutIn)
    Column(
        modifier = modifier.graphicsLayer {
            translationY = -36f * anim.value
            alpha = (1f - anim.value * 0.9f).coerceIn(0f, 1f)
            val grow = 1f + 0.25f * (1f - anim.value)
            scaleX = grow
            scaleY = grow
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            BattleAutoDecor.damageText(cutIn.damage),
            color = color,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xE6120E1E))
                    .border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    badge,
                    color = color,
                    fontSize = TcgType.TILE_MARK.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 피격 타격 플래시 — 붉은/금빛 섬광 오버레이 ([decay] 1 → 0).
 * 타격 순간 카드 위를 덮었다가 빠르게 사라진다.
 */
@Composable
fun HitFlashLayer(
    decay: Float,
    golden: Boolean,
    modifier: Modifier = Modifier
) {
    if (decay <= 0.01f) return
    val flash = if (golden) Color(0xFFFFD54F) else Color(0xFFFF5252)
    Box(
        modifier = modifier.background(flash.copy(alpha = BattleAutoDecor.flashAlpha(decay)))
    )
}
