package com.game.tcg.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.game.tcg.core.model.CardCatalog
import java.io.File
import kotlin.math.min

/**
 * 캐릭터 등장 영상 재생 (TextureView + MediaPlayer).
 *
 * 새 의존성(ExoPlayer 등) 없이 플랫폼 API 만 쓴다 — CI·로컬 빌드가 그대로 재현된다.
 *
 * · `assets/character_videos/{번호}.mp4` 를 `assets.openFd()` 로 바로 연다.
 *   (MP4 는 `androidResources.noCompress` 로 무압축 저장 → openFd 가능.
 *    혹시 압축되어 openFd 가 실패하면 cacheDir 로 한 번 복사해 재생한다 — 조용한 실패 방지)
 * · 영상 비율은 잘라내지 않고 **화면 안에 맞춰**(letterbox) 그린다 — 세로 화면에서 얼굴이 잘리지 않게.
 * · 재생 실패·디코딩 오류는 예외를 던지지 않고 [onFailed] 로 알린다(게임은 계속 진행).
 */
@Composable
fun CharacterVideoSurface(
    assetPath: String,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
    onFailed: () -> Unit = onFinished
) {
    val context = LocalContext.current
    // 콜백이 최신 람다를 보도록 유지 (재생 도중 재컴포지션에도 안전)
    val finished by rememberUpdatedState(onFinished)
    val failed by rememberUpdatedState(onFailed)
    val playback = remember(assetPath) { CharacterVideoPlayback(context.applicationContext, assetPath) }

    DisposableEffect(playback) {
        playback.onFinished = { finished() }
        playback.onFailed = { failed() }
        onDispose { playback.release() }
    }

    // 앱이 백그라운드로 가면 영상도 멈춘다 — 화면을 떠났는데 소리만 남지 않게
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(playback, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> playback.pauseForBackground()
                Lifecycle.Event.ON_RESUME -> playback.resumeAfterBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // assetPath 가 바뀌면 뷰를 새로 만든다 — 이전 카드 영상이 남아 재생되지 않게
    key(assetPath) {
        AndroidView(
            modifier = modifier.onSizeChanged { playback.refit() },
            factory = { ctx ->
                TextureView(ctx).also { view ->
                    view.surfaceTextureListener = playback.listenerFor(view)
                    playback.attach(view)
                }
            }
        )
    }
}

/**
 * TextureView 1개에 MediaPlayer 1개를 붙이는 작은 재생기.
 *
 * Compose 상태와 무관한 순수 Android 객체라 화면이 재컴포지션돼도 재생이 끊기지 않는다.
 */
private class CharacterVideoPlayback(
    private val context: Context,
    private val assetPath: String
) {
    var onFinished: () -> Unit = {}
    var onFailed: () -> Unit = {}

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var view: TextureView? = null
    private var attachedTexture: SurfaceTexture? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var pausedByBackground = false
    private var released = false

    fun attach(textureView: TextureView) {
        view = textureView
        // 화면에 이미 붙어 있어 콜백이 오지 않은 경우를 대비해 직접 시작
        textureView.surfaceTexture?.let { start(it) }
    }

    fun listenerFor(textureView: TextureView): TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                start(st)
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                refit()
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                release()
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }

    private fun start(st: SurfaceTexture) {
        if (released) return
        // 같은 서피스로 두 번 시작하지 않는다 (attach 와 onSurfaceTextureAvailable 중복 호출 방어)
        if (player != null && attachedTexture === st) return
        release()
        attachedTexture = st
        val target = Surface(st)
        surface = target
        val mp = MediaPlayer()
        player = mp
        try {
            openDataSource(mp)
            mp.setSurface(target)
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            mp.isLooping = false
            mp.setOnPreparedListener { prepared ->
                videoWidth = prepared.videoWidth
                videoHeight = prepared.videoHeight
                refit()
                runCatching { prepared.start() }
            }
            mp.setOnCompletionListener { onFinished() }
            mp.setOnErrorListener { _, _, _ ->
                onFailed()
                true
            }
            mp.prepareAsync()
        } catch (error: Throwable) {
            release()
            onFailed()
        }
    }

    /** assets.openFd 우선, 실패하면 cacheDir 복사본으로 (압축된 에셋 대비) */
    private fun openDataSource(mp: MediaPlayer) {
        val descriptor = runCatching { context.assets.openFd(assetPath) }.getOrNull()
        if (descriptor != null) {
            descriptor.use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            return
        }
        val cached = File(context.cacheDir, "character_video_" + assetPath.hashCode() + ".mp4")
        if (!cached.exists() || cached.length() == 0L) {
            context.assets.open(assetPath).use { input ->
                cached.outputStream().use { output -> input.copyTo(output) }
            }
        }
        mp.setDataSource(cached.absolutePath)
    }

    /** 홈·다른 앱으로 나가면 일시정지 (돌아오면 이어서) */
    fun pauseForBackground() {
        val mp = player ?: return
        pausedByBackground = runCatching {
            if (mp.isPlaying) {
                mp.pause()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun resumeAfterBackground() {
        if (!pausedByBackground) return
        pausedByBackground = false
        player?.let { mp -> runCatching { mp.start() } }
    }

    /** 영상 비율 유지 · 화면 안에 맞춤(레터박스) — 좌우/상하 여백은 배경색이 채운다 */
    fun refit() {
        val textureView = view ?: return
        if (videoWidth <= 0 || videoHeight <= 0) return
        val viewWidth = textureView.width.toFloat()
        val viewHeight = textureView.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return
        val scale = min(viewWidth / videoWidth, viewHeight / videoHeight)
        val dx = (viewWidth - videoWidth * scale) / 2f
        val dy = (viewHeight - videoHeight * scale) / 2f
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        runCatching { textureView.setTransform(matrix) }
    }

    fun release() {
        player?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.setSurface(null) }
            runCatching { mp.release() }
        }
        player = null
        surface?.let { runCatching { it.release() } }
        surface = null
        attachedTexture = null
    }
}

// ────────────────────────────────────────────────────────────────
// 카드팩 개봉 — 카드 일러스트보다 먼저 재생되는 등장 연출
// ────────────────────────────────────────────────────────────────

/**
 * 등장 영상 무대 — 16:9 영상 + 캐릭터 이름 + `SKIP ▸`.
 *
 * 화면 아무 곳이나 탭해도 건너뛴다(브리프: SKIP 버튼 또는 터치로 스킵).
 * 종료·스킵·실패 모두 [onFinished] 로 모여 **카드 스탯/등급 화면으로** 이어진다.
 */
@Composable
fun CharacterVideoEntranceStage(
    assetPath: String,
    cardId: Int,
    name: String,
    stepLabel: String? = null,
    onFinished: () -> Unit,
    onSkip: () -> Unit
) {
    val profile = remember(cardId) {
        CardCatalog.ALL.firstOrNull { it.id == cardId }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07050E), Color(0xFF15102A), Color(0xFF07050E))
                )
            )
            .clickable { onSkip() },
        contentAlignment = Alignment.Center
    ) {
        // 2026-11-21 태블릿 가로 대응 — 16:9 무대를 **화면 높이에 맞춰** 줄인다.
        // 예전에는 fillMaxWidth() × 16:9 라서 가로 화면에서 무대 높이가 화면을 넘어
        // 영상이 아래로 밀려 보이지 않았다(사용자 피드백 "가로 모드일 때 영상이 안 보여").
        val stageHorizontalPadding = 14.dp
        val reservedForTexts = 232.dp          // 상단 표시·이름·칭호·스탯·안내 문구 자리
        val maxStageHeight = (maxHeight - reservedForTexts).coerceAtLeast(120.dp)
        val stageWidth = minOf(
            (maxWidth - stageHorizontalPadding * 2).coerceAtLeast(120.dp),
            maxStageHeight * 16f / 9f
        )
        // 상단: 등장 연출 표시 + 남은 카드
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill("🎬 등장 연출", TcgColors.Gold, filled = true)
            Spacer(Modifier.width(8.dp))
            Text(
                "NO.$cardId · $name",
                color = TcgColors.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            stepLabel?.let { Text(it, color = TcgColors.Muted, fontSize = 12.sp) }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = stageHorizontalPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .width(stageWidth)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(TcgColors.Gold.copy(alpha = 0.9f), TcgColors.Violet, TcgColors.Gold.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                CharacterVideoSurface(
                    assetPath = assetPath,
                    modifier = Modifier.fillMaxSize(),
                    onFinished = onFinished,
                    onFailed = onFinished
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                name,
                color = TcgColors.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            profile?.let { record ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "「${record.title}」",
                    color = TcgColors.Gold.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(record.element.displayName, elementColor(record.element))
                    Pill("⚔ ${record.atk}", TcgColors.Danger)
                    Pill("🛡 ${record.def}", TcgColors.Mana)
                }
            }
        }

        // SKIP ▸
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x99120E1C))
                .border(1.dp, TcgColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(50))
                .clickable { onSkip() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text("SKIP ▸", color = TcgColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }

        Text(
            "화면을 탭하면 카드로 넘어갑니다",
            color = TcgColors.Muted.copy(alpha = 0.75f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
        )
    }
}

/**
 * 도감·캐릭터 상세에서 `🎬 등장 영상` 다시 보기 — 전체 화면 다이얼로그로 띄운다.
 *
 * 다이얼로그를 닫으면(재생 종료·건너뛰기·바깥 탭) [onClose] 가 불리고,
 * 호출부가 BGM 을 복구한다.
 */
@Composable
fun CharacterVideoOverlay(
    cardId: Int,
    assetPath: String,
    onClose: () -> Unit
) {
    val record = remember(cardId) { CardCatalog.ALL.firstOrNull { it.id == cardId } }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CharacterVideoEntranceStage(
            assetPath = assetPath,
            cardId = cardId,
            name = record?.name ?: "NO.$cardId",
            stepLabel = null,
            onFinished = onClose,
            onSkip = onClose
        )
    }
}
