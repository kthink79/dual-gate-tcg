package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.BattleResultPanel
import com.game.tcg.core.model.VisualState

/**
 * ⚔ 전투 결과 연출 — VICTORY / DEFEAT 전용 전체 화면 오버레이 (2026-11-17 사용자 요청).
 *
 * 사용자 원문: *"단순 텍스트 토스트/작은 다이얼로그가 아니라 승리(VICTORY) / 패배(DEFEAT)
 * 전용 결과 화면/오버레이 연출을 시각적으로 확실히 구현"*
 *
 * 구성
 *  · 화면 전체를 덮는 결과 배경(승리 = 금빛·보랏빛 광휘 / 패배 = 적흑 그라데이션)
 *  · 회전하는 광환 + 스케일 인 되는 대형 VICTORY / DEFEAT 워드마크
 *  · 결과 배지(중간 보스 격파 · 대마왕 격파 · 전투 패배)
 *  · 대표 캐릭터 일러스트(2:3 · ContentScale.Fit) + 승리 대사/격려 대사 말풍선
 *  · 승리 = 획득 보상 칩 · 패배 = 패인 힌트 목록
 *  · 하단 버튼 — 승리: 다음 스테이지 / 로비 · 패배: 다시 도전 / 강화 / 덱 편성
 */
@Composable
fun BattleResultOverlay(
    panel: BattleResultPanel,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onGrowth: () -> Unit,
    onDeck: () -> Unit,
    onLobby: () -> Unit
) {
    val victory = panel.isVictory
    val accent = if (victory) TcgColors.Gold else TcgColors.Danger
    val accent2 = if (victory) Color(0xFF7C4DFF) else Color(0xFF8E1B1B)

    // 등장 애니메이션 — 배지 팝 + 워드마크 스케일 인
    val appear = remember { Animatable(0f) }
    LaunchedEffect(panel) { appear.animateTo(1f, tween(520, easing = LinearOutSlowInEasing)) }
    // 광환 회전
    val spin = rememberInfiniteTransition(label = "resultSpin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.34f),
                        accent2.copy(alpha = 0.22f),
                        Color(0xF00B0A12)
                    ),
                    radius = 1600f
                )
            )
    ) {
        // 광환 — 결과 색으로 도는 원형 빛
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(1.5f)
                .aspectRatio(1f)
                .rotate(angle)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.20f),
                            Color.Transparent,
                            accent2.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 배지 + 스테이지 ──
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(panel.badge, accent, filled = true)
                Pill(panel.subtitle, TcgColors.Muted)
            }
            Spacer(Modifier.height(10.dp))

            // ── 대형 워드마크 ──
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (victory) "VICTORY" else "DEFEAT",
                    color = accent.copy(alpha = 0.22f),
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 4.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 1.08f + appear.value * 0.04f
                            scaleY = 1.08f + appear.value * 0.04f
                        }
                )
                Text(
                    if (victory) "VICTORY" else "DEFEAT",
                    color = if (victory) Color(0xFFFFF3C4) else Color(0xFFFFC1C1),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(0.78f + appear.value * 0.22f)
                )
            }
            Text(
                if (victory) "전투 승리" else "전투 패배",
                color = accent,
                fontSize = TcgType.BODY.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // ── 대표 캐릭터 + 대사 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(148.dp)
                        .aspectRatio(CardLayoutRules.TILE_ASPECT)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x33120E1C))
                        .border(2.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                ) {
                    val heroId = panel.heroCardId
                    if (heroId != null) {
                        CardArtImage(
                            cardId = heroId,
                            visual = VisualState.BATTLE,
                            contentDescription = panel.heroName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            targetWidthPx = 540
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        panel.heroName,
                        color = TcgColors.Text,
                        fontSize = TcgType.BODY.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    // 말풍선
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC120E1C))
                            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "“${panel.heroQuote}”",
                            color = if (victory) Color(0xFFFFF3C4) else Color(0xFFFFD9D9),
                            fontSize = TcgType.LABEL.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (victory) {
                // ── 획득 보상 ──
                SectionCard(title = "획득 보상", badge = panel.badge, accent = accent) {
                    FlowPills(items = panel.rewardLines.ifEmpty { listOf("보상 없음") }, color = accent)
                    if (panel.nextStageNumber == null) {
                        Text(
                            "🏆 캠페인 30스테이지 완주 — 심연의 문이 닫혔습니다.",
                            color = TcgColors.Gold,
                            fontSize = TcgType.LABEL.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                // ── 패인 힌트 ──
                SectionCard(title = "패인 분석 · 다음 준비", badge = "재도전", accent = accent) {
                    panel.hints.forEach { hint ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .width(5.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(accent)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                hint,
                                color = TcgColors.Text.copy(alpha = 0.92f),
                                fontSize = TcgType.LABEL.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── 버튼 ──
            if (victory) {
                ActionTile(
                    label = panel.primaryLabel ?: "로비로 ▸",
                    hint = panel.nextStageNumber?.let { "스테이지 $it 로 바로 출전" }
                        ?: "메인 로비에서 다음 목표를 정하세요",
                    accent = TcgColors.Gold,
                    onClick = onPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ActionTile(
                            label = "⚒ 강화·합성",
                            hint = "다음 판을 더 강하게",
                            accent = TcgColors.Violet,
                            onClick = onGrowth
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ActionTile(
                            label = "🏛 메인 로비",
                            hint = "보상 정리 · 파트너 교감",
                            accent = TcgColors.Mana,
                            onClick = onLobby
                        )
                    }
                }
            } else {
                ActionTile(
                    label = panel.primaryLabel ?: "다시 도전 ▸",
                    hint = "잃은 동료는 무료 재계약으로 되찾고 바로 출전합니다",
                    accent = TcgColors.Danger,
                    onClick = onPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ActionTile(
                            label = "⚒ 강화·합성",
                            hint = "레벨·성급 올리기",
                            accent = TcgColors.Violet,
                            onClick = onGrowth
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ActionTile(
                            label = "📜 덱 편성",
                            hint = "상성 조합 바꾸기",
                            accent = TcgColors.Mana,
                            onClick = onDeck
                        )
                    }
                }
                ActionTile(
                    label = "🏛 메인 로비로",
                    hint = "카드팩·장비로 덱 보강",
                    accent = TcgColors.Muted,
                    onClick = onLobby
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (victory) "탭하여 계속" else "탭하여 재정비",
                color = TcgColors.Muted.copy(alpha = 0.7f),
                fontSize = TcgType.LABEL.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSecondary() }
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 보상 칩을 줄바꿈 없이 흐르게 배치 (작은 화면에서도 잘리지 않게 스크롤) */
@Composable
private fun FlowPills(items: List<String>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { Pill(it, color) }
            }
        }
    }
}
