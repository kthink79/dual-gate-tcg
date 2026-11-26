package com.game.tcg.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.R
import com.game.tcg.core.engine.IntroAdvancePolicy
import com.game.tcg.core.engine.IntroEffect
import com.game.tcg.core.engine.IntroSceneId
import com.game.tcg.core.engine.IntroScript
import com.game.tcg.core.engine.IntroStoryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 도입부 일러스트 4장 → 드로어블 리소스 매핑.
 * (`res/drawable-nodpi/intro_scene_{1..4}.jpg` · 540×810 · JPEG q75)
 */
object IntroArtRegistry {

    private val ART: IntArray = intArrayOf(
        R.drawable.intro_scene_1,
        R.drawable.intro_scene_2,
        R.drawable.intro_scene_3,
        R.drawable.intro_scene_4
    )

    /** 장면 인덱스(0~3)의 일러스트 리소스 */
    fun resId(sceneIndex: Int): Int = ART[sceneIndex.coerceIn(0, ART.lastIndex)]

    /** 등록된 인트로 그림 수 */
    val size: Int get() = ART.size

    /** 장면 식별자로 바로 조회 */
    fun resIdOf(id: IntroSceneId): Int = resId(IntroScript.SCENES.indexOfFirst { it.id == id })
}

/**
 * 장면 카메라 무빙(Ken Burns) 길이 — 자동 넘김 대신 연출만 천천히 이어진다.
 */
private const val KEN_BURNS_MILLIS = 6_000

/**
 * 《듀얼 게이트》 도입부 스토리 — 4장면 풀스크린 컷신.
 *
 * 연출
 *  - 장면마다 부드러운 Ken Burns 줌/패닝 (장면이 바뀌면 처음부터, 연출 길이 6초)
 *  - Canvas 기반 빛 입자 · 카드팩 섬광/충격 링 · 회전하는 차원의 문 링 · 소환 오라
 *  - 단계별 자막 페이드인 (ACT → 부제 → 제목 → 본문 → 안내)
 *  - **자동 넘김 없음** — 화면을 터치해야 다음 장면으로 넘어간다(2026-11-16 사용자 요청).
 *    하단에 「화면을 터치해 계속 ▶」 안내와 장면 번호(1/4)를 보여 주고, 우상단 SKIP 은 그대로 둔다.
 *  - 마지막 장면에서는 안내 문구 대신 「⚔ 이계로 들어가기」 버튼
 *
 * 상태 전이는 모두 [com.game.tcg.core.engine.IntroStoryEngine]이 담당하고, 이 화면은
 * [onNext]·[onSkip] 콜백만 올려보낸다.
 */
@Composable
fun IntroStoryScreen(
    state: IntroStoryState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scene = state.scene
    val accent = Color(scene.accentHex)

    // ── Ken Burns: 장면이 바뀔 때마다 0 → 1 로 다시 진행 (자동 넘김 없음 — 연출만 이어진다) ──
    val kenBurns = remember { Animatable(0f) }
    LaunchedEffect(state.sceneIndex, state.isPlaying) {
        if (!state.isPlaying) return@LaunchedEffect
        kenBurns.snapTo(0f)
        kenBurns.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = KEN_BURNS_MILLIS,
                easing = LinearEasing
            )
        )
    }

    // 자동 진행 타이머는 없다 — 다음 장면은 사용자가 화면을 터치해야 넘어간다.
    // (엔진 정책: IntroAdvancePolicy.AUTO_ADVANCE_ENABLED = false)

    // ── 입자 · 링 애니메이션용 연속 시계 (0~1 반복) ──
    val clock by rememberInfiniteTransition(label = "intro-clock").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "clock"
    )

    // ── 장면 진입 섬광 (봉인 해제·문 개방에서 강하게) ──
    val flash = remember { Animatable(0f) }
    LaunchedEffect(state.sceneIndex, state.isPlaying) {
        if (!state.isPlaying) return@LaunchedEffect
        val peak = when (scene.effect) {
            IntroEffect.CARD_BURST -> 0.85f
            IntroEffect.GATE_RINGS -> 0.55f
            IntroEffect.SUMMON_AURA -> 0.35f
            IntroEffect.DRIFT_MOTES -> 0.18f
        }
        flash.snapTo(peak)
        flash.animateTo(0f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    // ── 자막 단계별 페이드인 ──
    val actAlpha = stagedAlpha(state.sceneIndex, "act", 60L)
    val titleAlpha = stagedAlpha(state.sceneIndex, "title", 260L)
    val bodyAlpha = stagedAlpha(state.sceneIndex, "body", 620L)
    val hintAlpha = stagedAlpha(state.sceneIndex, "hint", 1_100L)

    val pulsing by rememberInfiniteTransition(label = "intro-pulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1_200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "hint-pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TcgColors.Background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNext() }
    ) {
        // ① 장면 일러스트 (Ken Burns + 장면 전환 크로스페이드)
        IntroSceneArt(sceneIndex = state.sceneIndex, kenBurns = kenBurns.value)

        // ② 가독성 스크림 — 위·아래를 어둡게, 가운데는 그림을 살린다
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xCC05040A),
                        0.28f to Color(0x3305040A),
                        0.62f to Color(0x4D05040A),
                        1f to Color(0xF705040A)
                    )
                )
        )

        // ③ Canvas 연출 (입자 · 섬광 링 · 차원의 문 링 · 소환 오라)
        IntroEffectCanvas(
            effect = scene.effect,
            accent = accent,
            clock = clock,
            sceneKey = state.sceneIndex,
            modifier = Modifier.fillMaxSize()
        )

        // ④ 장면 진입 섬광
        if (flash.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flash.value * 0.9f))
            )
        }

        // ⑤ 상단 바 — 타이틀 · 진행 · SKIP (시스템 상태 바 아래로 내려 터치 가능하게)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill("🎴 듀얼 게이트", TcgColors.Violet)
            Spacer(Modifier.width(6.dp))
            Pill("PROLOGUE", TcgColors.Muted)
            Spacer(Modifier.weight(1f))
            Text(
                state.stepLabel,
                color = TcgColors.Muted,
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x99120E1C))
                    .border(1.dp, TcgColors.Text.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .clickable { onSkip() }
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            ) {
                Text(
                    "SKIP ▸",
                    color = TcgColors.Text,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }

        // ⑥ 하단 자막
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
        ) {
            Text(
                "— ${scene.act} · ${scene.id.label} —",
                color = accent,
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                modifier = Modifier
                    .alpha(actAlpha)
                    .offset { IntOffset(0, (10.dp.toPx() * (1f - actAlpha)).roundToInt()) }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                scene.title,
                color = TcgColors.Text,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 33.sp,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset { IntOffset(0, (14.dp.toPx() * (1f - titleAlpha)).roundToInt()) }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                scene.subtitle,
                color = accent.copy(alpha = 0.92f),
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset { IntOffset(0, (14.dp.toPx() * (1f - titleAlpha)).roundToInt()) }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                scene.narration,
                color = TcgColors.Text.copy(alpha = 0.88f),
                fontSize = TcgType.LABEL.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .alpha(bodyAlpha)
                    .offset { IntOffset(0, (12.dp.toPx() * (1f - bodyAlpha)).roundToInt()) }
            )

            Spacer(Modifier.height(20.dp))

            // 장면 진행 점
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.alpha(bodyAlpha)
            ) {
                repeat(IntroScript.sceneCount) { index ->
                    val active = index == state.sceneIndex
                    val passed = state.isStepPassed(index)
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (active) 20.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    active -> accent
                                    passed -> TcgColors.Muted.copy(alpha = 0.8f)
                                    else -> TcgColors.Line
                                }
                            )
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "장면 ${state.sceneIndex + 1} / ${IntroScript.sceneCount}",
                    color = TcgColors.Muted,
                    fontSize = TcgType.LABEL.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state.isLastScene) {
                // 마지막 장면 — 안내 문구 대신 시작 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(hintAlpha)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.18f))
                        .border(1.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(50))
                        .clickable { onNext() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚔ 이계로 들어가기",
                        color = TcgColors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // 터치 진행 안내 — 자동 넘김이 없으므로 가장 눈에 띄는 위치에 둔다
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(hintAlpha)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x99120E1C))
                        .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .clickable { onNext() }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        IntroAdvancePolicy.TAP_PROMPT,
                        color = TcgColors.Text.copy(alpha = 0.7f + 0.3f * pulsing),
                        fontSize = TcgType.LABEL.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        IntroAdvancePolicy.sceneCounter(state),
                        color = accent,
                        fontSize = TcgType.LABEL.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** 자막 한 줄이 [startDelayMs] 뒤에 서서히 나타나는 알파값 */
@Composable
private fun stagedAlpha(sceneIndex: Int, tag: String, startDelayMs: Long): Float {
    val anim = remember(sceneIndex, tag) { Animatable(0f) }
    LaunchedEffect(sceneIndex, tag) {
        anim.snapTo(0f)
        delay(startDelayMs)
        anim.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }
    return anim.value
}

/**
 * 장면 일러스트 — 장면이 바뀌면 700ms 크로스페이드로 겹치고,
 * 같은 그림 안에서 Ken Burns(확대 + 좌우 패닝)가 이어진다.
 * 540×810 JPG를 화면 폭에 맞춰 축소 디코딩하므로(약 900px 목표) 메모리 부담이 작다.
 */
@Composable
private fun IntroSceneArt(sceneIndex: Int, kenBurns: Float) {
    // 태블릿·가로 화면: 2:3 장면화를 잘리지 않게(앰비언트 + 원본 프레임) — Ken Burns 는 세로 화면 전용
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (ResponsiveLayout.isWideArtContainer(maxWidth.value, maxHeight.value)) {
            AmbientResourceBackdrop(
                resId = IntroArtRegistry.resId(sceneIndex),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                targetWidthPx = 900
            )
            return@BoxWithConstraints
        }
        IntroSceneArtKenBurns(sceneIndex = sceneIndex, kenBurns = kenBurns)
    }
}

/** 세로 화면용 Ken Burns 장면 연출 (기존 동작 유지) */
@Composable
private fun IntroSceneArtKenBurns(sceneIndex: Int, kenBurns: Float) {
    val context = LocalContext.current
    Crossfade(
        targetState = sceneIndex,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "intro-scene"
    ) { index ->
        val resId = IntroArtRegistry.resId(index)
        val bitmap by produceState<ImageBitmap?>(
            initialValue = CardArtLoader.peek(resId, 900),
            resId
        ) {
            value = withContext(Dispatchers.IO) {
                CardArtLoader.load(context.resources, resId, 900)
            }
        }
        val direction = if (index % 2 == 0) 1f else -1f
        val image = bitmap
        if (image == null) {
            ArtPlaceholder(Modifier.fillMaxSize())
        } else {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1.05f + 0.13f * kenBurns
                        scaleX = scale
                        scaleY = scale
                        translationX = size.width * 0.045f * direction * (kenBurns - 0.5f)
                        translationY = -size.height * 0.035f * (kenBurns - 0.5f)
                    }
            )
        }
    }
}

/** Canvas 연출 묶음 — 장면 종류에 따라 입자 · 링 · 오라를 그린다. */
@Composable
private fun IntroEffectCanvas(
    effect: IntroEffect,
    accent: Color,
    clock: Float,
    sceneKey: Int,
    modifier: Modifier = Modifier
) {
    // 결정적(재현 가능한) 입자 배치 — 장면마다 고정 시드
    val motes = remember(sceneKey) {
        val rnd = Random(sceneKey * 7_919 + 17)
        List(56) {
            IntroMote(
                x = rnd.nextFloat(),
                phase = rnd.nextFloat(),
                speed = 0.35f + rnd.nextFloat() * 0.65f,
                size = 0.0012f + rnd.nextFloat() * 0.0030f,
                sway = 0.006f + rnd.nextFloat() * 0.022f
            )
        }
    }

    Canvas(modifier = modifier) {
        when (effect) {
            IntroEffect.DRIFT_MOTES -> drawMotes(motes, accent, clock, alphaBase = 0.55f)
            IntroEffect.CARD_BURST -> {
                drawBurstRings(accent, clock)
                drawMotes(motes, accent, clock, alphaBase = 0.45f)
            }
            IntroEffect.GATE_RINGS -> {
                drawGateRings(accent, clock)
                drawMotes(motes, Color(0xFFB388FF), clock, alphaBase = 0.5f)
            }
            IntroEffect.SUMMON_AURA -> {
                drawSummonAura(accent, clock)
                drawMotes(motes, Color(0xFFFFD54F), clock, alphaBase = 0.6f)
            }
        }
    }
}

/** 위로 떠오르며 흔들리는 빛 입자 */
private data class IntroMote(
    val x: Float,
    val phase: Float,
    val speed: Float,
    val size: Float,
    val sway: Float
)

private fun DrawScope.drawMotes(
    motes: List<IntroMote>,
    color: Color,
    clock: Float,
    alphaBase: Float
) {
    val w = size.width
    val h = size.height
    motes.forEach { mote ->
        val t = ((clock * mote.speed + mote.phase) % 1f)
        val y = h * (1.08f - t * 0.95f)
        val x = w * mote.x + sin((t * 2f + mote.phase) * 2f * PI.toFloat()) * w * mote.sway
        val fade = when {
            t < 0.12f -> t / 0.12f
            t > 0.78f -> (1f - t) / 0.22f
            else -> 1f
        }
        val radius = w * mote.size
        guardPositive(radius) {
            drawCircle(
                color = color.copy(alpha = (alphaBase * fade).coerceIn(0f, 1f)),
                radius = radius,
                center = Offset(x, y)
            )
            drawCircle(
                color = color.copy(alpha = (alphaBase * fade * 0.25f).coerceIn(0f, 1f)),
                radius = radius * 3.2f,
                center = Offset(x, y)
            )
        }
    }
}

/** 카드팩이 터질 때의 중심 섬광 + 퍼져 나가는 충격 링 */
private fun DrawScope.drawBurstRings(color: Color, clock: Float) {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.5f, h * 0.47f)

    // 중심 잔광
    val glowRadius = w * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    repeat(3) { index ->
        val t = ((clock * 2.4f + index * 0.33f) % 1f)
        val radius = w * 0.06f + w * 0.62f * t
        val alpha = ((1f - t) * 0.5f).coerceIn(0f, 1f)
        guardPositive(radius) {
            drawCircle(
                color = color.copy(alpha = alpha * 0.55f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.22f),
                radius = radius * 0.82f,
                center = center,
                style = Stroke(width = 0.8.dp.toPx())
            )
        }
    }
}

/** 허공에서 회전하며 열리는 차원의 문 — 3겹 링 + 중심 보이드 글로우 */
private fun DrawScope.drawGateRings(color: Color, clock: Float) {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.5f, h * 0.44f)

    val coreRadius = w * 0.40f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.22f), Color(0xFF64B5F6).copy(alpha = 0.10f), Color.Transparent),
            center = center,
            radius = coreRadius
        ),
        radius = coreRadius,
        center = center
    )

    rotate(degrees = clock * 360f, pivot = center) {
        repeat(3) { index ->
            val radius = w * (0.34f + 0.055f * index)
            val widthPx = (2.4f - index * 0.5f).dp.toPx()
            val alpha = 0.55f - index * 0.12f
            guardPositive(radius) {
                drawArc(
                    color = color.copy(alpha = alpha),
                    startAngle = index * 46f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = widthPx)
                )
                drawArc(
                    color = Color.White.copy(alpha = alpha * 0.35f),
                    startAngle = index * 46f + 180f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = widthPx * 0.6f)
                )
            }
        }
    }

    // 문 아래로 흘러내리는 빛기둥
    val beamWidth = w * 0.30f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.16f), Color.Transparent),
            startY = center.y,
            endY = h * 1.02f
        ),
        topLeft = Offset(center.x - beamWidth / 2f, center.y),
        size = Size(beamWidth, h * 1.02f - center.y)
    )
}

/** 소환된 동료들 위로 피어오르는 금빛 오라 + 하단 라디얼 글로우 */
private fun DrawScope.drawSummonAura(color: Color, clock: Float) {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.5f, h * 0.95f)
    val glowRadius = w * 0.85f

    val pulse = 0.85f + 0.15f * sin(clock * 2f * PI.toFloat())
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.26f * pulse),
                Color(0xFFB388FF).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // 문 양옆에서 피어오르는 광선
    repeat(5) { index ->
        val x = w * (0.12f + 0.19f * index)
        val t = ((clock + index * 0.17f) % 1f)
        val height = h * (0.16f + 0.22f * t)
        val alpha = (0.22f * (1f - t)).coerceIn(0f, 1f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = alpha)),
                startY = h - height,
                endY = h
            ),
            topLeft = Offset(x - w * 0.04f, h - height),
            size = Size(w * 0.08f, height)
        )
    }
}

/** 반지름이 0 이하이면 그리지 않는다 (Canvas 예외 방지) */
private inline fun DrawScope.guardPositive(radius: Float, block: DrawScope.() -> Unit) {
    if (radius > 0.5f) block()
}
