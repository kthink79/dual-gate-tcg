package com.game.tcg.core.engine

/**
 * 《듀얼 게이트》 도입부 스토리 — 4단계 컷신의 데이터와 상태 전이.
 *
 * 이 파일은 Android·Compose에 의존하지 않는다(자막 문구·진행 시간·연출 종류만 담는다).
 * 화면(`ui/IntroStoryScreen.kt`)은 이 상태를 읽어 연출만 그리고, 모든 전이는
 * [IntroStoryEngine]을 통해서만 일어나므로 단위 테스트로 4단계 진행·건너뛰기·다시보기를
 * 그대로 검증할 수 있다.
 *
 * 흐름: 우연한 발견 → 봉인 해제 → 이세계로의 연결(듀얼 게이트) → 소환과 시작
 */
enum class IntroSceneId(
    /** 장면 이름 (테스트·로그용) */
    val label: String,
    /** `res/drawable-nodpi`에 들어가는 일러스트 파일 이름 접두사 */
    val artName: String
) {
    DISCOVERY("우연한 발견", "intro_scene_1"),
    UNSEALING("봉인 해제", "intro_scene_2"),
    GATE("이세계로의 연결", "intro_scene_3"),
    SUMMON("소환과 시작", "intro_scene_4")
}

/** 장면 위에 겹치는 Canvas 연출 종류 */
enum class IntroEffect {
    /** 어두운 골목 위로 떠오르는 푸른 빛 입자 */
    DRIFT_MOTES,

    /** 카드팩이 터지는 섬광 + 퍼져 나가는 충격 링 */
    CARD_BURST,

    /** 허공에서 회전하며 열리는 차원의 문 링 */
    GATE_RINGS,

    /** 문 너머에서 걸어 나오는 동료들 위로 피어오르는 금빛 오라 */
    SUMMON_AURA
}

/**
 * 컷신 한 장면.
 *
 * @param act      좌상단 장면 표기 ("ACT I")
 * @param title    화면 대제목
 * @param subtitle 대제목 아래 한 줄
 * @param narration 자막 본문 (2~3줄)
 * @param effect   Canvas 연출
 * @param accentHex 강조색 (0xAARRGGBB)
 * @param autoAdvanceMillis 자동 진행까지의 시간 — 화면을 탭하면 즉시 넘어간다
 */
data class IntroScene(
    val id: IntroSceneId,
    val act: String,
    val title: String,
    val subtitle: String,
    val narration: String,
    val effect: IntroEffect,
    val accentHex: Long,
    val autoAdvanceMillis: Long
) {
    /** 자동 진행 시간 (초) — UI 표기용 */
    val autoAdvanceSeconds: Float get() = autoAdvanceMillis / 1000f
}

/** 4장면 대본 */
object IntroScript {

    /** 장면당 자동 진행 시간의 허용 범위 (사용자 요청: 약 3.5~4초) */
    const val MIN_AUTO_ADVANCE_MS = 3_500L
    const val MAX_AUTO_ADVANCE_MS = 4_000L

    val SCENES: List<IntroScene> = listOf(
        IntroScene(
            id = IntroSceneId.DISCOVERY,
            act = "ACT I",
            title = "우연한 발견",
            subtitle = "비 내리는 퇴근길, 골목의 물웅덩이",
            narration = "야근을 마친 밤, 골목은 비에 젖어 있었다. " +
                "물웅덩이 위에 놓인 낡은 카드팩 하나 — 봉인된 은빛 왁스 틈에서 " +
                "새어 나온 푸른 빛이, 어째서인지 내 이름을 부르는 것 같았다.",
            effect = IntroEffect.DRIFT_MOTES,
            accentHex = 0xFF64B5F6,
            autoAdvanceMillis = 3_700L
        ),
        IntroScene(
            id = IntroSceneId.UNSEALING,
            act = "ACT II",
            title = "봉인 해제",
            subtitle = "호기심이 손끝을 먼저 움직였다",
            narration = "봉인을 뜯는 순간, 카드팩이 손 안에서 터졌다. " +
                "눈부신 마력의 빛이 골목을 하얗게 태우고, 수십 장의 카드가 흩날리며 " +
                "젖은 공기를 갈랐다. 현실의 소리가 점점 멀어졌다.",
            effect = IntroEffect.CARD_BURST,
            accentHex = 0xFFFFD54F,
            autoAdvanceMillis = 3_800L
        ),
        IntroScene(
            id = IntroSceneId.GATE,
            act = "ACT III",
            title = "이세계로의 연결",
            subtitle = "〈듀얼 게이트〉가 열린다",
            narration = "허공에 금빛 문양이 봉인을 풀 듯 갈라지고, 마침내 차원의 문이 열렸다. " +
                "문 너머에는 떠 있는 섬과 금빛 성탑, 낯선 두 개의 달이 보였다. " +
                "두 세계가 한 줄기 빛으로 이어졌다.",
            effect = IntroEffect.GATE_RINGS,
            accentHex = 0xFFB388FF,
            autoAdvanceMillis = 4_000L
        ),
        IntroScene(
            id = IntroSceneId.SUMMON,
            act = "ACT IV",
            title = "소환과 시작",
            subtitle = "부름에 응답한 동료들",
            narration = "문 너머에서 빛으로 빚어진 이들이 걸어 나왔다. " +
                "성기사 아우렐리아가 검을 내리고 손을 내밀었다. " +
                "\"계약자님, 이제 당신의 이야기가 시작됩니다.\" " +
                "현실과 이세계를 넘나드는 모험이 이렇게 열렸다.",
            effect = IntroEffect.SUMMON_AURA,
            accentHex = 0xFFFFD54F,
            autoAdvanceMillis = 4_000L
        )
    )

    /** 전체 장면 수 */
    val sceneCount: Int get() = SCENES.size

    /** 마지막 장면 인덱스 */
    val lastIndex: Int get() = SCENES.lastIndex

    /** 인덱스를 안전하게 장면으로 변환 (범위를 벗어나면 마지막 장면) */
    fun sceneAt(index: Int): IntroScene = SCENES[index.coerceIn(0, lastIndex)]

    /** 첫 실행 인트로를 한 번에 넘겼을 때의 총 재생 시간 (ms) */
    val totalDurationMillis: Long get() = SCENES.sumOf { it.autoAdvanceMillis }
}

/**
 * 인트로 재생 상태.
 *
 * @param sceneIndex   현재 장면 (0 ~ 3)
 * @param isPlaying    인트로가 화면을 덮고 있는지 — false면 로비가 보인다
 * @param hasSeenIntro 한 번이라도 끝까지 보거나 건너뛰었는지 (다시 실행 시 생략)
 * @param skipped      이번 재생을 SKIP 버튼으로 끝냈는지
 * @param replayCount  로비에서 "다시보기"를 누른 횟수
 */
data class IntroStoryState(
    val sceneIndex: Int = 0,
    val isPlaying: Boolean = true,
    val hasSeenIntro: Boolean = false,
    val skipped: Boolean = false,
    val replayCount: Int = 0
) {
    /** 현재 장면 데이터 */
    val scene: IntroScene get() = IntroScript.sceneAt(sceneIndex)

    /** 마지막(4번째) 장면인지 — 이 장면에서는 안내 문구 대신 [모험 시작] 버튼을 보여준다 */
    val isLastScene: Boolean get() = sceneIndex >= IntroScript.lastIndex

    /** "1 / 4" 형태의 진행 표기 */
    val stepLabel: String get() = "${sceneIndex + 1} / ${IntroScript.sceneCount}"

    /** 0.25, 0.5, 0.75, 1.0 — 하단 진행 점 표시용 */
    val progress: Float get() = (sceneIndex + 1).toFloat() / IntroScript.sceneCount

    /** 현재 장면 인덱스의 진행 점 상태 (0-based) */
    fun isStepPassed(index: Int): Boolean = index <= sceneIndex
}

/**
 * 인트로 상태 전이 엔진 — 순수 함수만 가진다.
 *
 * - [initialState]  : 앱 시작 시 상태 (이미 본 적이 있으면 재생하지 않는다)
 * - [advance]       : 탭 / 자동 진행 — 마지막 장면이면 인트로를 끝낸다
 * - [skip]          : SKIP 버튼 — 즉시 끝내고 로비로
 * - [replay]        : 로비의 "🎬 인트로 다시보기"
 */
class IntroStoryEngine {

    companion object {
        /** 「인트로를 봤다」를 기억하는 SharedPreferences 파일 이름 (MainActivity가 사용) */
        const val PREFS_NAME = "dual_gate_intro"

        /** 이미 본 인트로는 앱 재실행 시 생략한다 — 저장 키 */
        const val PREF_INTRO_SEEN = "intro_seen"
    }

    /** 앱 시작 시 초기 상태 — [alreadySeen]이면 로비에서 바로 시작한다 */
    fun initialState(alreadySeen: Boolean = false): IntroStoryState = IntroStoryState(
        sceneIndex = 0,
        isPlaying = !alreadySeen,
        hasSeenIntro = alreadySeen,
        skipped = false,
        replayCount = 0
    )

    /**
     * 다음 장면으로. 마지막 장면에서 호출하면 인트로가 끝나고 로비가 드러난다.
     * 재생 중이 아니면 아무 일도 하지 않는다.
     */
    fun advance(state: IntroStoryState): IntroStoryState {
        if (!state.isPlaying) return state
        return if (state.sceneIndex < IntroScript.lastIndex) {
            state.copy(sceneIndex = state.sceneIndex + 1, skipped = false)
        } else {
            finish(state)
        }
    }

    /** SKIP — 장면 위치와 무관하게 즉시 종료하고, 이번 재생은 건너뛴 것으로 기록한다. */
    fun skip(state: IntroStoryState): IntroStoryState {
        if (!state.isPlaying) return state
        return finish(state).copy(skipped = true)
    }

    /** 로비의 "다시보기" — 1장면부터 다시 재생. 이미 본 인트로라는 사실은 유지한다. */
    fun replay(state: IntroStoryState): IntroStoryState = state.copy(
        sceneIndex = 0,
        isPlaying = true,
        skipped = false,
        replayCount = state.replayCount + 1,
        hasSeenIntro = true
    )

    private fun finish(state: IntroStoryState): IntroStoryState = state.copy(
        isPlaying = false,
        hasSeenIntro = true
    )
}
