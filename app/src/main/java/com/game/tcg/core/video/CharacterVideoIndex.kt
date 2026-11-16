package com.game.tcg.core.video

/**
 * 캐릭터 등장 영상 에셋 규약 — `assets/character_videos/{캐릭터번호}.mp4`
 *
 * 2026-11-16 사용자 요청: "카드팩에서 이 캐릭터가 나올 때 등장 영상이 재생되고,
 * 앞으로 `{캐릭터번호}.mp4` 파일만 추가하면 코드 수정 없이 자동 반영" 되도록.
 *
 * 그래서 이 파일은 **파일명 → 카드 번호** 규칙만 담는 순수 Kotlin 이다(Android API 0개).
 * 실제 폴더 스캔은 `ui/CharacterVideoRegistry` 가 [CharacterVideoAssets.DIRECTORY] 를
 * `AssetManager.list` 로 읽어 [CharacterVideoIndex.fromFileNames] 에 넘긴다.
 *
 * 허용 파일명 (대소문자 무시)
 *  - `114.mp4`      정식 규약 (압축 스크립트가 만드는 이름)
 *  - `114-5.mp4`    원본을 그대로 넣은 경우(제미나이 생성 파일명) — 앞 번호를 쓴다
 *  - `114_alt.mp4`  같은 캐릭터의 다른 테이크
 *  - 그 밖의 이름(`gemini_generated_video_77042178.mp4`, `readme.txt`)은 **무시**한다
 *    (번호를 알 수 없는 파일을 아무 캐릭터에 붙이지 않는다 — 잘못된 매칭보다 미등록이 낫다)
 */
object CharacterVideoAssets {

    /** assets 하위 폴더 이름 */
    const val DIRECTORY: String = "character_videos"

    /** 확장자 (소문자 기준) */
    const val EXTENSION: String = "mp4"

    /** 도감 No.1 ~ No.184(+중간 보스 170~175)을 넉넉히 덮는 번호 범위 */
    const val MIN_CARD_ID: Int = 1
    const val MAX_CARD_ID: Int = 999

    /**
     * `{1~3자리 번호}` + (선택: `-` `_` 공백 뒤 아무 꼬리표) + `.mp4`
     * — `1140.mp4` 같은 네 자리는 번호로 읽지 않는다(다른 캐릭터를 덮지 않게).
     */
    private val FILE_NAME = Regex("""^(\d{1,3})(?:[-_ ].*)?\.mp4$""", RegexOption.IGNORE_CASE)

    /** 파일명에서 카드 번호를 뽑는다. 규약 밖이면 null. */
    fun parseCardId(fileName: String): Int? {
        val match = FILE_NAME.matchEntire(fileName.trim()) ?: return null
        val id = match.groupValues[1].toIntOrNull() ?: return null
        return if (isValidCardId(id)) id else null
    }

    /** 규약에 맞는 번호인지 */
    fun isValidCardId(cardId: Int): Boolean = cardId in MIN_CARD_ID..MAX_CARD_ID

    /** assets 상대 경로 — 실제 파일명을 그대로 쓴다 */
    fun assetPath(fileName: String): String = "$DIRECTORY/$fileName"

    /** 정식 파일명 (`4` → `4.mp4`) — 압축 스크립트·문서용 */
    fun canonicalFileName(cardId: Int): String = "$cardId.$EXTENSION"
}

/**
 * assets 스캔 결과 — 카드 번호 → assets 경로.
 *
 * 순수 데이터라 JVM 단위 테스트로 "파일만 추가하면 자동 인식"을 그대로 검증한다.
 */
class CharacterVideoIndex private constructor(
    private val videos: Map<Int, String>
) {

    /** 등록된 영상 수 */
    val size: Int get() = videos.size

    val isEmpty: Boolean get() = videos.isEmpty()

    /** 번호 오름차순 목록 */
    val cardIds: List<Int> get() = videos.keys.sorted()

    /** 이 캐릭터의 등장 영상 assets 경로 (없으면 null) */
    fun videoFor(cardId: Int): String? = videos[cardId]

    fun hasVideo(cardId: Int): Boolean = videos.containsKey(cardId)

    /** 테스트·디버그용 전체 매핑 사본 */
    fun asMap(): Map<Int, String> = videos.toMap()

    override fun toString(): String =
        "CharacterVideoIndex(${size}편: ${cardIds.joinToString(",")})"

    companion object {

        val EMPTY: CharacterVideoIndex = CharacterVideoIndex(emptyMap())

        /**
         * `AssetManager.list(DIRECTORY)` 결과를 색인으로.
         *
         * 같은 번호가 여러 파일로 들어온 경우(예: `4.mp4` + `4_alt.mp4`):
         *  ① 정식 이름(`4.mp4`) 을 우선하고,
         *  ② 없으면 이름 사전순 첫 파일을 쓴다 — 재시작해도 같은 영상이 나오게(결정적).
         */
        fun fromFileNames(fileNames: Iterable<String>): CharacterVideoIndex {
            val picked = LinkedHashMap<Int, String>()
            fileNames
                .map { it.trim() }
                .distinct()
                .sorted()
                .forEach { name ->
                    val cardId = CharacterVideoAssets.parseCardId(name) ?: return@forEach
                    val current = picked[cardId]
                    val canonical = CharacterVideoAssets.canonicalFileName(cardId)
                    when {
                        current == null -> picked[cardId] = name
                        current == canonical -> Unit                       // 정식 이름 유지
                        name == canonical -> picked[cardId] = name         // 정식 이름이 뒤에 와도 승격
                        else -> Unit                                       // 사전순 첫 파일 유지
                    }
                }
            if (picked.isEmpty()) return EMPTY
            return CharacterVideoIndex(picked.mapValues { (_, name) -> CharacterVideoAssets.assetPath(name) })
        }
    }
}
