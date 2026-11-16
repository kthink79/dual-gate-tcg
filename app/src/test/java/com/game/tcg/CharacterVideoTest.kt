package com.game.tcg

import com.game.tcg.audio.AudioManager
import com.game.tcg.audio.AudioPlayer
import com.game.tcg.audio.AudioScene
import com.game.tcg.audio.BgmTrack
import com.game.tcg.audio.InMemoryAudioPrefs
import com.game.tcg.audio.SfxEffect
import com.game.tcg.core.engine.PackOpeningPhase
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.video.CharacterVideoAssets
import com.game.tcg.core.video.CharacterVideoCue
import com.game.tcg.core.video.CharacterVideoIndex
import com.game.tcg.core.video.CharacterVideoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 캐릭터 등장 영상 — 2026-11-16 사용자 요청 검증
 *
 * 1) 에셋 규약: `assets/character_videos/{캐릭터번호}.mp4` 파일명 → 카드 번호
 * 2) 자동 감지: 폴더에 파일만 넣으면 코드 수정 없이 색인에 잡힌다 (확장 구조의 핵심)
 * 3) 팩 개봉 트리거: REVEAL 단계 + 영상 있음 → 재생, 그 밖의 단계/미등록 카드는 카드만
 * 4) 실제 탑재된 5편이 규약·용량·MP4 시그니처를 만족한다
 * 5) 영상 재생 동안 BGM 이 멈추고, 끝나면 같은 트랙으로 복구된다
 */
class CharacterVideoTest {

    // ── 1. 파일명 규약 ────────────────────────────────────────────

    @Test
    fun `정식 이름과 꼬리표 이름 모두 앞 번호를 캐릭터 번호로 읽는다`() {
        assertEquals(114, CharacterVideoAssets.parseCardId("114.mp4"))
        assertEquals(4, CharacterVideoAssets.parseCardId("4.mp4"))
        assertEquals(4, CharacterVideoAssets.parseCardId("04.mp4"))
        assertEquals(1, CharacterVideoAssets.parseCardId("001.mp4"))
        // 대소문자 무시
        assertEquals(114, CharacterVideoAssets.parseCardId("114.MP4"))
        // 압축 스크립트 없이 원본을 그대로 넣은 경우(제미나이 파일명 규칙 · 다른 테이크)
        assertEquals(114, CharacterVideoAssets.parseCardId("114-5.mp4"))
        assertEquals(4, CharacterVideoAssets.parseCardId("4_alt.mp4"))
        assertEquals(6, CharacterVideoAssets.parseCardId("  6 take2.mp4  "))
    }

    @Test
    fun `번호를 알 수 없거나 범위 밖인 파일은 무시한다`() {
        assertNull(CharacterVideoAssets.parseCardId("gemini_generated_video_77042178.mp4"))
        assertNull(CharacterVideoAssets.parseCardId("readme.txt"))
        assertNull(CharacterVideoAssets.parseCardId("1140.mp4"))
        assertNull(CharacterVideoAssets.parseCardId(".mp4"))
        assertNull(CharacterVideoAssets.parseCardId("0.mp4"))
        assertNull(CharacterVideoAssets.parseCardId("mp4"))
        assertNull(CharacterVideoAssets.parseCardId(""))
    }

    @Test
    fun `규약 경로는 assets 상대 경로다`() {
        assertEquals("character_videos/114.mp4", CharacterVideoAssets.assetPath("114.mp4"))
        assertEquals("114.mp4", CharacterVideoAssets.canonicalFileName(114))
        assertEquals("character_videos", CharacterVideoAssets.DIRECTORY)
    }

    // ── 2. 자동 감지 (확장 구조) ──────────────────────────────────

    @Test
    fun `폴더 목록만 넘기면 등록 영상이 자동으로 잡힌다`() {
        val index = CharacterVideoIndex.fromFileNames(
            listOf("1.mp4", "2.mp4", "114.mp4", "thumb.png", "notes.txt", "1140.mp4")
        )
        assertEquals(3, index.size)
        assertEquals(listOf(1, 2, 114), index.cardIds)
        assertEquals("character_videos/114.mp4", index.videoFor(114))
        assertTrue(index.hasVideo(2))
        assertFalse(index.hasVideo(3))
        assertNull(index.videoFor(999))
    }

    @Test
    fun `새 캐릭터 영상 파일을 넣기만 하면 코드 수정 없이 색인에 추가된다`() {
        val before = CharacterVideoIndex.fromFileNames(listOf("1.mp4", "114.mp4"))
        assertFalse("추가 전에는 6번 영상이 없다", before.hasVideo(6))

        // 사용자가 나중에 {번호}.mp4 를 넣고 APK 만 다시 빌드한 상황
        val after = CharacterVideoIndex.fromFileNames(listOf("1.mp4", "114.mp4", "6.mp4"))
        assertTrue(after.hasVideo(6))
        assertEquals("character_videos/6.mp4", after.videoFor(6))
        assertEquals(3, after.size)
    }

    @Test
    fun `같은 번호가 여러 테이크로 있으면 정식 이름을 고른다`() {
        val canonical = CharacterVideoIndex.fromFileNames(listOf("4_alt.mp4", "4.mp4"))
        assertEquals("character_videos/4.mp4", canonical.videoFor(4))
        assertEquals(1, canonical.size)

        // 정식 이름이 없으면 사전순 첫 파일 (재시작해도 같은 영상 — 결정적)
        val alternate = CharacterVideoIndex.fromFileNames(listOf("4_take2.mp4", "4-take1.mp4"))
        assertEquals("character_videos/4-take1.mp4", alternate.videoFor(4))

        // 파일이 하나도 없으면 빈 색인 (크래시 없음)
        assertTrue(CharacterVideoIndex.fromFileNames(emptyList()).isEmpty)
    }

    // ── 3. 팩 개봉 트리거 ─────────────────────────────────────────

    @Test
    fun `REVEAL 단계에서 등록된 카드만 영상을 재생한다`() {
        val cue = CharacterVideoCue.forReveal(
            phase = PackOpeningPhase.REVEAL,
            cardId = 114,
            assetPath = "character_videos/114.mp4"
        )
        assertEquals(CharacterVideoStatus.PLAYING, cue.status)
        assertTrue(cue.isPlaying)
        assertTrue(cue.hasVideo)
        assertEquals(114, cue.cardId)
        assertEquals("character_videos/114.mp4", cue.assetPath)
    }

    @Test
    fun `영상이 없거나 등록되지 않은 카드는 곧바로 카드 화면으로 간다`() {
        val noVideo = CharacterVideoCue.forReveal(PackOpeningPhase.REVEAL, 3, null)
        assertEquals(CharacterVideoStatus.NONE, noVideo.status)
        assertFalse(noVideo.isPlaying)
        assertFalse(noVideo.hasVideo)
        assertNull(noVideo.assetPath)

        val blankPath = CharacterVideoCue.forReveal(PackOpeningPhase.REVEAL, 3, "  ")
        assertEquals(CharacterVideoStatus.NONE, blankPath.status)

        val noCard = CharacterVideoCue.forReveal(PackOpeningPhase.REVEAL, 0, "character_videos/1.mp4")
        assertEquals(CharacterVideoStatus.NONE, noCard.status)
    }

    @Test
    fun `봉인 카드팩·전체 그리드 단계에서는 영상이 끼어들지 않는다`() {
        listOf(PackOpeningPhase.IDLE, PackOpeningPhase.SEALED, PackOpeningPhase.SUMMARY).forEach { phase ->
            val cue = CharacterVideoCue.forReveal(phase, 114, "character_videos/114.mp4")
            assertEquals("$phase 에서는 재생하지 않는다", CharacterVideoStatus.NONE, cue.status)
        }
    }

    @Test
    fun `재생이 끝나거나 건너뛰면 카드 화면으로 넘어가고 다시 보기는 다시 재생한다`() {
        val playing = CharacterVideoCue.forReveal(PackOpeningPhase.REVEAL, 6, "character_videos/6.mp4")

        val finished = playing.finished()
        assertEquals(CharacterVideoStatus.DONE, finished.status)
        assertFalse(finished.isPlaying)
        assertEquals("카드 번호는 유지 — 어떤 카드의 영상이었는지 남는다", 6, finished.cardId)

        val skipped = playing.skipped()
        assertEquals(CharacterVideoStatus.DONE, skipped.status)

        // 이미 끝난 큐를 또 끝내도 그대로 (중복 콜백 방어)
        assertEquals(CharacterVideoStatus.DONE, finished.finished().status)
        assertEquals(CharacterVideoStatus.DONE, finished.skipped().status)

        // 영상이 없던 큐는 끝내도 NONE 을 유지한다
        assertEquals(CharacterVideoStatus.NONE, CharacterVideoCue.NONE.finished().status)

        // 「🎬 등장 영상」 다시 보기 = 같은 큐를 새로 만든다
        val replay = CharacterVideoCue.forReveal(PackOpeningPhase.REVEAL, 6, "character_videos/6.mp4")
        assertTrue(replay.isPlaying)
    }

    @Test
    fun `같은 카드라도 다음 장으로 넘어가면 새 재생 단위가 된다`() {
        val first = CharacterVideoCue.revealKey(PackOpeningPhase.REVEAL, 0)
        val second = CharacterVideoCue.revealKey(PackOpeningPhase.REVEAL, 1)
        assertTrue(first != second)
        assertEquals("REVEAL#0", first)
    }

    // ── 4. 실제 탑재 에셋 ─────────────────────────────────────────

    @Test
    fun `탑재된 등장 영상은 모두 규약 이름의 1_5MB 이하 MP4다`() {
        val dir = assetsDir()
        val files = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
        assertTrue("assets/character_videos 가 비어 있다: ${dir.absolutePath}", files.isNotEmpty())

        val limit = (1.5 * 1024 * 1024).toLong()
        files.forEach { file ->
            val cardId = CharacterVideoAssets.parseCardId(file.name)
            assertNotNull("${file.name} 은 {번호}.mp4 규약이 아니다", cardId)
            assertTrue("${file.name} 크기 ${file.length()}B", file.length() in 1..limit)
            val header = file.inputStream().use { it.readNBytes(12) }
            assertTrue("${file.name} 가 MP4 가 아니다", header.size == 12)
            assertEquals("${file.name} ftyp 시그니처", "ftyp", String(header, 4, 4, Charsets.US_ASCII))
        }
    }

    @Test
    fun `이번에 탑재한 5명(1·2·4·6·114)이 실제 색인에 잡힌다`() {
        val names = assetsDir().listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()
        val index = CharacterVideoIndex.fromFileNames(names)
        listOf(1, 2, 4, 6, 114).forEach { cardId ->
            assertTrue("No.$cardId 등장 영상이 없다", index.hasVideo(cardId))
            assertEquals("character_videos/$cardId.mp4", index.videoFor(cardId))
            assertTrue(
                "No.$cardId 는 카드 카탈로그에 있어야 한다",
                CardCatalog.ALL.any { it.id == cardId }
            )
        }
        assertTrue("등록 영상은 5편 이상", index.size >= 5)
    }

    // ── 5. BGM 일시정지 · 복구 ────────────────────────────────────

    @Test
    fun `영상 재생 동안 BGM 이 멈추고 끝나면 같은 트랙으로 돌아온다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player = player, prefs = InMemoryAudioPrefs())
        audio.updateScene(AudioScene.LOBBY)
        assertEquals(BgmTrack.LOBBY, player.lastTrack)

        audio.suspendBgmForVideo()
        assertTrue(audio.isBgmSuspendedByVideo())
        assertEquals(1, player.pauseCount)

        // 영상 중 화면 장면이 바뀌어도 BGM 을 새로 틀지 않는다
        audio.updateScene(AudioScene.BATTLE)
        assertEquals("영상 중 새 트랙 재생 금지", BgmTrack.LOBBY, player.lastTrack)

        audio.resumeBgmAfterVideo()
        assertFalse(audio.isBgmSuspendedByVideo())
        assertEquals(1, player.resumeCount)
        assertEquals("영상 중 바뀐 장면이 복구 시 반영된다", BgmTrack.BATTLE, player.lastTrack)
        assertEquals("멈추기 전 트랙에서 재개", BgmTrack.LOBBY, player.resumedTrack)

        // 중복 호출에 안전하다
        audio.resumeBgmAfterVideo()
        assertEquals(1, player.resumeCount)
    }

    @Test
    fun `BGM 을 꺼 둔 상태에서는 영상이 끝나도 소리를 되살리지 않는다`() {
        val player = RecordingPlayer()
        val audio = AudioManager(player = player, prefs = InMemoryAudioPrefs())
        audio.updateScene(AudioScene.LOBBY)
        audio.toggleBgm()   // 사용자가 음소거
        assertFalse(audio.settings().bgmEnabled)

        audio.suspendBgmForVideo()
        audio.resumeBgmAfterVideo()
        assertFalse(audio.isBgmSuspendedByVideo())
        assertEquals("음소거 상태에서는 재개하지 않는다", 0, player.resumeCount)
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────

    /** 단위 테스트 작업 디렉터리는 모듈(app) 기준 — 실행 위치에 따라 후보를 훑는다 */
    private fun assetsDir(): File {
        val candidates = listOf(
            File("src/main/assets/character_videos"),
            File("app/src/main/assets/character_videos"),
            File("../app/src/main/assets/character_videos")
        )
        return candidates.firstOrNull { it.isDirectory } ?: candidates.first()
    }

    /** BGM 재생·일시정지·재개를 기록하는 테스트 더블 */
    private class RecordingPlayer : AudioPlayer {
        var lastTrack: BgmTrack? = null
        var resumedTrack: BgmTrack? = null
        var pauseCount = 0
        var resumeCount = 0
        var bgmOn = true
        private var paused: BgmTrack? = null

        override fun playBgm(track: BgmTrack) {
            if (!bgmOn) return
            lastTrack = track
        }

        override fun playScene(scene: AudioScene) {
            scene.track?.let { playBgm(it) }
        }

        override fun stopBgm() {
            lastTrack = null
        }

        override fun pauseBgm() {
            pauseCount++
            paused = lastTrack
        }

        override fun resumeBgm() {
            if (!bgmOn) return
            resumeCount++
            resumedTrack = paused
            paused?.let { lastTrack = it }
            paused = null
        }

        override fun currentTrack(): BgmTrack? = lastTrack

        override fun setBgmVolume(volume: Float) = Unit

        override fun setBgmEnabled(enabled: Boolean) {
            bgmOn = enabled
        }

        override fun playSfx(effect: SfxEffect) = Unit

        override fun setSfxVolume(volume: Float) = Unit

        override fun setSfxEnabled(enabled: Boolean) = Unit

        override fun release() = Unit
    }
}
