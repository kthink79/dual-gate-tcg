package com.game.tcg.ui

import android.content.Context
import android.content.res.AssetManager
import com.game.tcg.core.video.CharacterVideoAssets
import com.game.tcg.core.video.CharacterVideoIndex

/**
 * 등장 영상 자동 감지 레지스트리.
 *
 * `assets/character_videos/` 를 **런타임에 한 번 스캔**해 `{캐릭터번호}.mp4` 를 찾아 둔다.
 * 그래서 새 캐릭터 영상을 추가할 때 Kotlin 코드를 고칠 필요가 없다 —
 * `scripts/compress_character_videos.py` 로 압축해 파일을 넣고 APK 를 다시 빌드하면 끝이다.
 *
 * 규칙 해석은 [CharacterVideoAssets]·[CharacterVideoIndex](순수 Kotlin, 단위 테스트 대상)가
 * 담당하고, 이 객체는 "AssetManager 를 읽어 캐시한다"만 한다.
 */
object CharacterVideoRegistry {

    @Volatile
    private var cached: CharacterVideoIndex? = null

    /** 스캔 결과 (최초 1회만 실제 I/O, 이후 캐시) */
    fun index(context: Context): CharacterVideoIndex = indexOf(context.assets)

    fun indexOf(assets: AssetManager): CharacterVideoIndex {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val scanned = scan(assets)
            cached = scanned
            return scanned
        }
    }

    /** assets 폴더를 실제로 읽어 색인을 만든다 (폴더가 없거나 실패하면 빈 색인) */
    fun scan(assets: AssetManager): CharacterVideoIndex {
        val names = runCatching {
            assets.list(CharacterVideoAssets.DIRECTORY)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
        return CharacterVideoIndex.fromFileNames(names)
    }

    /** 이 캐릭터의 등장 영상 assets 경로 — 없으면 null */
    fun videoPath(context: Context, cardId: Int): String? =
        index(context).videoFor(cardId)

    fun hasVideo(context: Context, cardId: Int): Boolean =
        index(context).hasVideo(cardId)

    /** 설치된 등장 영상 수 (로비 정보 패널·QA 용) */
    fun installedCount(context: Context): Int = index(context).size

    /** 캐시 무효화 — 테스트나 APK 갱신 직후 강제 재스캔 */
    fun reset() {
        cached = null
    }
}
