package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.audio.SfxEffect
import com.game.tcg.core.engine.PackOpeningEngine
import com.game.tcg.core.engine.PackOpeningPhase
import com.game.tcg.core.engine.PackOpeningState
import com.game.tcg.core.model.PackDraw
import com.game.tcg.core.model.VisualState
import com.game.tcg.core.video.CharacterVideoCue

/**
 * 카드팩 개봉 연출 (전체 화면 이벤트).
 *
 * 2026-11-16 사용자 요청: "텍스트 목록만 나와 밋밋하다 — 단계별 비주얼 개봉 이벤트로".
 *
 *  ① [PackOpeningPhase.SEALED]  빛나는 봉인 카드팩 (회전 링 + 금빛 입자) → 탭하면 봉인 해제
 *  ② [PackOpeningPhase.REVEAL]  개봉 플래시와 함께 **실제 카드 일러스트**가 카드 형태로 등장
 *     — 등급/속성 테두리 · NEW(신규 계약) / 중복 조각 +10 배지 · ATK·DEF·성급 · 1장씩 넘기기
 *  ③ [PackOpeningPhase.SUMMARY] 전체 카드 그리드로 한눈에 확인
 *
 * 연출 단계 전이는 [com.game.tcg.core.engine.PackOpeningEngine]이 담당하고,
 * 개봉음(`pack_open`)은 봉인 해제 순간에 한 번 울린다.
 *
 * 🎬 2026-11-16 추가 — 등장 영상(`assets/character_videos/{번호}.mp4`)
 * 카드 [PackOpeningPhase.REVEAL] 단계에서 그 캐릭터에 등록된 영상이 있으면,
 * 카드 일러스트보다 **먼저** 영상이 재생되고 `SKIP ▸`/탭으로 건너뛰면 카드 화면으로 이어진다.
 * 재생 동안에는 [onVideoStart] 로 배경 BGM 을 멈추고, 끝나면 [onVideoEnd] 로 복구한다.
 * 어느 영상이 있는지는 [CharacterVideoRegistry] 가 assets 를 스캔해 알려 준다(코드 수정 불필요).
 */
@Composable
fun PackOpeningOverlay(
    opening: PackOpeningState,
    onTear: () -> Unit,
    onRevealNext: () -> Unit,
    onRevealAll: () -> Unit,
    onClose: () -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenCollection: () -> Unit,
    onSfx: (SfxEffect) -> Unit = {},
    onVideoStart: () -> Unit = {},
    onVideoEnd: () -> Unit = {}
) {
    if (!opening.isActive) return

    // ── 등장 영상 큐 ─────────────────────────────────────────────
    // 지금 공개한 카드에 {번호}.mp4 가 있으면 REVEAL 단계에서 1회 재생한다.
    val context = LocalContext.current
    val revealCardId = opening.currentDraw?.cardId ?: 0
    val videoAssetPath = remember(opening.phase, opening.revealIndex, revealCardId) {
        if (opening.phase == PackOpeningPhase.REVEAL) {
            CharacterVideoRegistry.videoPath(context, revealCardId)
        } else {
            null
        }
    }
    val videoCue = remember(opening.phase, opening.revealIndex, videoAssetPath) {
        CharacterVideoCue.forReveal(opening.phase, revealCardId, videoAssetPath)
    }
    // 이번 카드의 영상이 이미 (끝났거나 건너뛰어) 처리되었는지
    var videoHandled by remember(opening.phase, opening.revealIndex, videoAssetPath) { mutableStateOf(false) }
    val playingVideo = videoCue.isPlaying && !videoHandled

    // 봉인 카드팩의 숨쉬는 빛
    val glow by rememberInfiniteTransition(label = "pack-glow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    // 카드가 바뀔 때의 개봉 플래시
    val flash = remember { Animatable(0f) }
    LaunchedEffect(opening.phase, opening.revealIndex) {
        if (opening.phase == PackOpeningPhase.REVEAL) {
            flash.snapTo(0.92f)
            flash.animateTo(0f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        }
    }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(opening.phase, opening.revealIndex) {
        if (opening.phase == PackOpeningPhase.REVEAL) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF70A0714), Color(0xF2120E1C), Color(0xF70A0714))
                )
            )
    ) {
        // ── 상단 바: 팩 이름 · 진행 · 닫기 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill("🛒 개봉", TcgColors.Gold)
            Spacer(Modifier.width(6.dp))
            Text(
                opening.packName,
                color = TcgColors.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(opening.stepLabel, color = TcgColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x99120E1C))
                    .border(1.dp, TcgColors.Line, RoundedCornerShape(50))
                    .clickable { onSfx(SfxEffect.CLICK); onClose() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("닫기 ✕", color = TcgColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        when (opening.phase) {
            PackOpeningPhase.SEALED -> SealedPackStage(
                opening = opening,
                glow = glow,
                onClick = {
                    onSfx(SfxEffect.PACK_OPEN)
                    onTear()
                }
            )

            PackOpeningPhase.REVEAL -> if (playingVideo && videoCue.assetPath != null) {
                // 영상 재생 1회 = BGM 일시정지 1회 (종료·스킵·화면 이탈 모두 onDispose 로 복구)
                DisposableEffect(Unit) {
                    onVideoStart()
                    onDispose { onVideoEnd() }
                }
                CharacterVideoEntranceStage(
                    assetPath = videoCue.assetPath!!,
                    cardId = videoCue.cardId,
                    name = opening.currentDraw?.name ?: "NO.${videoCue.cardId}",
                    stepLabel = opening.stepLabel,
                    onFinished = { videoHandled = true },
                    onSkip = { onSfx(SfxEffect.CLICK); videoHandled = true }
                )
            } else {
                RevealStage(
                    opening = opening,
                    reveal = reveal.value,
                    onNext = { onSfx(SfxEffect.CLICK); onRevealNext() },
                    onRevealAll = { onSfx(SfxEffect.CLICK); onRevealAll() },
                    hasVideo = videoCue.hasVideo,
                    onReplayVideo = if (videoCue.hasVideo) {
                        { onSfx(SfxEffect.CLICK); videoHandled = false }
                    } else {
                        null
                    }
                )
            }

            PackOpeningPhase.SUMMARY -> SummaryStage(
                opening = opening,
                onClose = onClose,
                onOpenGrowth = onOpenGrowth,
                onOpenCollection = onOpenCollection
            )

            PackOpeningPhase.IDLE -> Unit
        }

        // 개봉 섬광
        if (flash.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flash.value * 0.85f))
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// ① 봉인 카드팩
// ────────────────────────────────────────────────────────────────

@Composable
private fun SealedPackStage(
    opening: PackOpeningState,
    glow: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 카드팩 본체 — 봉인 왁스 + 금빛 테두리
            Box(
                modifier = Modifier
                    .width(196.dp)
                    .height(272.dp)
                    .scale(1f + 0.03f * glow)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2A2140), Color(0xFF4A2F6E), Color(0xFF1B1428))
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(TcgColors.Gold.copy(alpha = glow), TcgColors.Violet, TcgColors.Gold.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎴", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "SEALED",
                        color = TcgColors.Gold.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        opening.packName,
                        color = TcgColors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Pill("${opening.total}장 봉인", TcgColors.Violet)
                }
                // 봉인 왁스 도장
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TcgColors.Danger.copy(alpha = 0.85f))
                        .border(2.dp, TcgColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = TcgColors.Gold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                PackOpeningEngine.SEALED_PROMPT,
                color = TcgColors.Gold.copy(alpha = 0.65f + 0.35f * glow),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "빛이 새어 나오는 봉인 카드팩입니다",
                color = TcgColors.Muted,
                fontSize = 12.sp
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// ② 카드 1장씩 등장
// ────────────────────────────────────────────────────────────────

@Composable
private fun RevealStage(
    opening: PackOpeningState,
    reveal: Float,
    onNext: () -> Unit,
    onRevealAll: () -> Unit,
    hasVideo: Boolean = false,
    onReplayVideo: (() -> Unit)? = null
) {
    val draw: PackDraw = opening.currentDraw ?: return
    val card = TcgGameViewModel.characterCard(draw.cardId)
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(top = 62.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PackRevealCard(
                draw = draw,
                modifier = Modifier
                    .width(236.dp)
                    .height(336.dp)
                    .scale(0.9f + 0.1f * reveal)
            )

            Spacer(Modifier.height(14.dp))

            // 이름 · 등급 · 속성
            Text(
                draw.name,
                color = TcgColors.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            if (hasVideo && onReplayVideo != null) {
                ChipButton("🎬 등장 영상 다시 보기", false, TcgColors.Gold) { onReplayVideo() }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill(draw.rarity.displayName, Color(draw.rarity.color), filled = true)
                Pill(card.element.displayName, elementColor(card.element))
                Pill("${card.starLevel}★", TcgColors.Muted)
                if (draw.isNew) {
                    Pill("NEW · 신규 계약", TcgColors.Heal, filled = true)
                } else {
                    Pill("중복 → 🧩 +${draw.shards} · ${draw.gold}G", TcgColors.Gold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("⚔ ATK ${card.atk}", TcgColors.Danger)
                Pill("🛡 DEF ${card.def}", TcgColors.Mana)
                Pill("코스트 ${card.baseCost}", TcgColors.Violet)
            }

            Spacer(Modifier.height(18.dp))

            // 카드 진행 표시 (n / total)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(opening.total) { index ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (index == opening.revealIndex) 18.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    index < opening.revealIndex -> TcgColors.Muted
                                    index == opening.revealIndex -> if (draw.isNew) TcgColors.Heal else TcgColors.Gold
                                    else -> TcgColors.Line
                                }
                            )
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(opening.stepLabel, color = TcgColors.Muted, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = if (opening.hasNextCard) "다음 카드 ▶" else "전체 결과 보기",
                        hint = if (opening.hasNextCard) "탭하여 다음 카드" else "${opening.total}장 모두 확인",
                        accent = if (draw.isNew) TcgColors.Heal else TcgColors.Gold,
                        onClick = onNext
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "전체 보기 ⊞",
                        hint = "남은 카드를 한 번에",
                        accent = TcgColors.Violet,
                        onClick = onRevealAll
                    )
                }
            }
        }
    }
}

/** 개봉 카드 1장 — 실제 일러스트 + 등급 테두리 + 신규/중복 배지 */
@Composable
private fun PackRevealCard(
    draw: PackDraw,
    modifier: Modifier = Modifier
) {
    val card = TcgGameViewModel.characterCard(draw.cardId)
    val rarityColor = Color(draw.rarity.color)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(TcgColors.SurfaceAlt)
            .border(2.5.dp, rarityColor, shape)
    ) {
        CardArtImage(
            cardId = draw.cardId,
            visual = VisualState.BATTLE,
            contentDescription = draw.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 720
        )
        // 하단 가독성 스크림
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60A0714))))
        )
        // 신규 / 중복 배지
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (draw.isNew) TcgColors.Heal else TcgColors.Gold)
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                if (draw.isNew) "★ NEW · 신규" else "중복 🧩 +${draw.shards}",
                color = Color(0xFF120E1C),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        // 등급 표기
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC120E1C))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(draw.rarity.displayName, color = rarityColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        // 하단 이름 · 속성
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElementBadge(card.element, size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(draw.name, color = TcgColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("NO.${draw.cardId} · 코스트 ${card.baseCost}", color = TcgColors.Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("⚔ ${card.atk}  🛡 ${card.def}", color = TcgColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ────────────────────────────────────────────────────────────────
// ③ 전체 그리드
// ────────────────────────────────────────────────────────────────

@Composable
private fun SummaryStage(
    opening: PackOpeningState,
    onClose: () -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenCollection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 58.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "개봉 결과 · ${opening.packName}",
            color = TcgColors.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Pill("신규 ${opening.newCount}명", TcgColors.Heal)
            Pill("중복 ${opening.duplicateCount}장", TcgColors.Gold)
            opening.result?.let { r ->
                Pill("🧩 +${r.totalShards}", TcgColors.Abyss)
                if (r.totalGold > 0) Pill("🪙 +${r.totalGold}G", TcgColors.Gold)
                if (r.ticketsSpent > 0) Pill("🎟 ${r.ticketsSpent} 사용", TcgColors.Mana)
                else if (r.goldSpent > 0) Pill("${r.goldSpent}G 사용", TcgColors.Muted)
            }
        }
        Spacer(Modifier.height(12.dp))

        // 카드 그리드 (한 줄에 3장)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            opening.draws.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { draw ->
                        val card = TcgGameViewModel.characterCard(draw.cardId)
                        Box(modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                DuelCardTile(
                                    cardId = draw.cardId,
                                    visual = VisualState.BATTLE,
                                    element = card.element,
                                    starLevel = card.starLevel,
                                    power = (card.atk + card.def) / 100,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(148.dp),
                                    highlighted = draw.isNew,
                                    statusBadge = if (draw.isNew) "NEW" else "🧩 +${draw.shards}",
                                    statusBadgeColor = if (draw.isNew) TcgColors.Heal else TcgColors.Gold,
                                    targetWidthPx = 360
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    draw.name,
                                    color = TcgColors.Text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    // 마지막 줄 빈칸 채우기
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ActionTile(
                    label = "⚒ 강화·합성으로",
                    hint = "조각으로 성장",
                    accent = TcgColors.Violet,
                    onClick = onOpenGrowth
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ActionTile(
                    label = "🃏 도감에서 확인",
                    hint = "새 동료 보기",
                    accent = TcgColors.Gold,
                    onClick = onOpenCollection
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            ActionTile(
                label = "닫기",
                hint = "상점으로 돌아갑니다",
                accent = TcgColors.Muted,
                onClick = onClose
            )
        }
    }
}
