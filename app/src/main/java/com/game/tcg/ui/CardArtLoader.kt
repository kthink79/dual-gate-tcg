package com.game.tcg.ui

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.game.tcg.core.model.VisualState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 카드 아트 로더 — 720×1080 JPG(약 155KB)를 화면에 필요한 크기로만 디코딩한다.
 *
 * 90장을 전부 원본 해상도로 디코딩하면 1장당 3.1MB(720×1080×4B)라 도감 그리드에서
 * 곧바로 OOM이 난다. 그래서
 *  1) inJustDecodeBounds로 원본 크기만 먼저 읽고,
 *  2) 목표 폭보다 큰 동안 inSampleSize를 2배씩 올려(720→360→180) 축소 디코딩하며,
 *  3) 결과 비트맵을 LRU 캐시(최대 힙의 1/6)에 보관한다.
 * 새 의존성(Coil 등)을 추가하지 않고 플랫폼 API만 쓰므로 CI·로컬 빌드가 그대로 재현된다.
 */
object CardArtLoader {

    private val cache: LruCache<String, ImageBitmap> = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 6).coerceAtMost(64L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    private fun key(resId: Int, reqWidthPx: Int): String = "$resId#$reqWidthPx"

    /** 디코딩 없이 캐시에 있는 비트맵만 조회 (리컴포지션 시 깜빡임 방지) */
    fun peek(resId: Int, reqWidthPx: Int): ImageBitmap? = cache.get(key(resId, reqWidthPx))

    /** 리소스를 목표 폭에 맞춰 축소 디코딩한다. 실패·OOM이면 null. */
    fun load(resources: Resources, resId: Int, reqWidthPx: Int): ImageBitmap? {
        if (resId == 0) return null
        // 0 이하 요청은 축소 배율 루프가 끝나지 않는다 — 최소 1px로 보정
        val reqW = reqWidthPx.coerceAtLeast(1)
        val cacheKey = key(resId, reqW)
        cache.get(cacheKey)?.let { return it }

        return try {
            decode(resources, resId, reqW, cacheKey)
        } catch (oom: OutOfMemoryError) {
            // 메모리 압박: 캐시를 전부 비우고 한 번만 더 시도한다
            cache.evictAll()
            try {
                decode(resources, resId, reqW / 2, cacheKey)
            } catch (second: OutOfMemoryError) {
                null
            }
        } catch (e: Exception) {
            // 잘못된 이미지 · 리소스 문제 등 — 플레이스홀더로 대체하고 게임은 계속된다
            null
        }
    }

    private fun decode(
        resources: Resources,
        resId: Int,
        reqW: Int,
        cacheKey: String
    ): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, resId, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        val target = reqW.coerceAtLeast(1)
        while (bounds.outWidth / (sampleSize * 2) >= target) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeResource(resources, resId, options) ?: return null
        val image = bitmap.asImageBitmap()
        cache.put(cacheKey, image)
        return image
    }

    /** 테스트·메모리 점검용 캐시 비우기 */
    fun clear() = cache.evictAll()
}

/**
 * 카드 아트 이미지 — 카드 번호와 3단 비주얼 상태만 주면 알맞은 JPG를 화면 크기에 맞춰 그린다.
 * 매핑이 없는 번호는 어두운 플레이스홀더로 대체한다(크래시 없음).
 */
@Composable
fun CardArtImage(
    cardId: Int,
    visual: VisualState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    targetWidthPx: Int = 540
) {
    val resId = CharacterArtRegistry.resId(cardId, visual)
    if (resId == null) {
        ArtPlaceholder(modifier)
        return
    }
    ResourceArtImage(
        resId = resId,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
        targetWidthPx = targetWidthPx
    )
}

/**
 * 드로어블 리소스 1장을 화면 크기에 맞춰 축소 디코딩해 그린다.
 * 카드가 아닌 배경(인트로 장면·구역 배너)을 그릴 때도 같은 LRU 캐시를 공유한다.
 */
@Composable
fun ResourceArtImage(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    targetWidthPx: Int = 540
) {
    val context = LocalContext.current
    // 요청 폭이 0/음수여도 캐시 키·축소 배율이 일관되도록 보정
    val targetPx = targetWidthPx.coerceAtLeast(1)
    val bitmap by produceState<ImageBitmap?>(
        initialValue = CardArtLoader.peek(resId, targetPx),
        resId,
        targetPx
    ) {
        value = withContext(Dispatchers.IO) {
            CardArtLoader.load(context.resources, resId, targetPx)
        }
    }
    val loaded = bitmap
    if (loaded == null) {
        ArtPlaceholder(modifier)
    } else {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            contentScale = contentScale,
            alignment = alignment,
            modifier = modifier
        )
    }
}

/** 아트가 아직 없거나 디코딩 전일 때의 무드 플레이스홀더 */
@Composable
fun ArtPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(TcgColors.SurfaceAlt, TcgColors.Background))
        )
    )
}
