package com.game.tcg.core.engine

import com.game.tcg.core.model.BossKind
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.StageDefinition
import com.game.tcg.core.model.TcgCard

/** 전투 결과 종류 */
enum class BattleOutcome(val headline: String, val korean: String) {
    VICTORY("VICTORY", "승리"),
    DEFEAT("DEFEAT", "패배")
}

/**
 * 전투 종료 연출에 필요한 모든 정보 — 화면([com.game.tcg.ui.BattleResultOverlay])은 이 값만 그린다.
 *
 * 2026-11-17 사용자 요청:
 * > "전투 종료 시 단순 텍스트 토스트/작은 다이얼로그가 아니라 승리(VICTORY)/패배(DEFEAT)
 * >  전용 결과 화면/오버레이 연출을 시각적으로 확실히 구현"
 *
 * 승리 — 승전 배지·획득 보상·대표 캐릭터 승리 대사·다음 스테이지/로비
 * 패배 — 패배 배지·격려 대사·패인 힌트·재도전/강화/덱 편성
 */
data class BattleResultPanel(
    val outcome: BattleOutcome,
    val stageNumber: Int,
    val stageName: String,
    val bossKind: BossKind,
    /** 결과 배지 문구 (`☠ 중간 보스 격파` / `👑 대마왕 격파` / `⚔ 전투 승리`) */
    val badge: String,
    /** 부제 (`5스테이지 · 홍장미의 계단`) */
    val subtitle: String,
    /** 대표 캐릭터 카드 번호 (없으면 null) */
    val heroCardId: Int?,
    val heroName: String,
    /** 대표 캐릭터 한마디 */
    val heroQuote: String,
    /** 획득 보상 한 줄들 (`🪙 260G` …) */
    val rewardLines: List<String>,
    /** 패인 힌트 — 승리면 비어 있다 */
    val hints: List<String>,
    /** 주 버튼 문구 (`다음 스테이지 ▸`) */
    val primaryLabel: String?,
    /** 다음 스테이지 번호 (없으면 마지막 스테이지 = 캠페인 완주) */
    val nextStageNumber: Int?
) {
    val isVictory: Boolean get() = outcome == BattleOutcome.VICTORY
}

/**
 * 승리·패배 결과 화면 문구/보상/패인 힌트를 만드는 순수 엔진.
 *
 * Android 에 의존하지 않으므로 JVM 단위 테스트에서 문구·힌트를 그대로 검증한다.
 */
object BattleResultEngine {

    // ── 대표 대사 ───────────────────────────────────────────────
    private val VICTORY_QUOTES: List<String> = listOf(
        "해냈어요! 이 정도면 다음 관문도 문제없겠는데요?",
        "차원의 균열이 잠잠해졌어요. 우리 덱, 꽤 잘 맞네요.",
        "상성도 강화도 전부 통했어요. 다음 스테이지로 가요!"
    )

    private val BOSS_VICTORY_QUOTES: List<String> = listOf(
        "보스도 쓰러졌어요…! 이 힘이면 더 깊은 곳으로 갈 수 있겠어요.",
        "큰 관문을 넘었어요. 조각과 장비를 챙기고 다음을 준비해요.",
        "이 승리로 우리는 한 단계 더 강해졌어요."
    )

    private val DEMON_LORD_VICTORY_QUOTES: List<String> = listOf(
        "마왕이 무릎을 꿇었어요. 이제 이 차원은 우리 편이에요.",
        "심연의 문이 닫혔어요… 정말 해냈어요, 우리."
    )

    private val DEFEAT_QUOTES: List<String> = listOf(
        "미안해요… 제 힘이 부족했어요. 다시 준비해서 도전해요.",
        "이번 판은 우리가 밀렸어요. 강화하고 상성을 맞추면 달라져요.",
        "쓰러진 동료는 무료 재계약으로 되찾을 수 있어요. 포기하지 마요."
    )

    // ── 결과 생성 ───────────────────────────────────────────────

    /**
     * 승리 패널.
     * @param outcome [StageEngine.settle] 결과 (보상 금액·다음 스테이지)
     * @param hero 승리 대사를 말할 대표 캐릭터
     */
    fun victory(
        stage: StageDefinition,
        outcome: StageClearOutcome,
        hero: TcgCard?,
        seed: Int = stage.number
    ): BattleResultPanel {
        val badge = when (stage.bossKind) {
            BossKind.DEMON_LORD -> "👑 대마왕 격파"
            BossKind.MID_BOSS -> "☠ 중간 보스 격파"
            else -> "⚔ 전투 승리"
        }
        val quotes = when (stage.bossKind) {
            BossKind.DEMON_LORD -> DEMON_LORD_VICTORY_QUOTES
            BossKind.MID_BOSS -> BOSS_VICTORY_QUOTES
            else -> VICTORY_QUOTES
        }
        val rewards = buildList {
            add("🪙 ${outcome.gold}G")
            add("✨ EXP ${outcome.exp}")
            if (outcome.tickets > 0) add("🎟 ${outcome.tickets}")
            if (outcome.gems > 0) add("💎 ${outcome.gems}")
            if (outcome.shardCount > 0) add("🧩 조각 ${outcome.shardCount}")
            outcome.equipmentDrop?.let { add("🗡 ${it.name}") }
        }
        return BattleResultPanel(
            outcome = BattleOutcome.VICTORY,
            stageNumber = stage.number,
            stageName = stage.name,
            bossKind = stage.bossKind,
            badge = badge,
            subtitle = "${stage.number}스테이지 · ${stage.name}",
            heroCardId = hero?.id,
            heroName = hero?.name ?: "파트너",
            heroQuote = quotes[seed % quotes.size],
            rewardLines = rewards,
            hints = emptyList(),
            primaryLabel = if (outcome.nextStageNumber != null) "다음 스테이지 ▸" else "캠페인 완주 🎉",
            nextStageNumber = outcome.nextStageNumber
        )
    }

    /**
     * 패배 패널 — 패인 힌트를 계산해 "무엇을 올려야 다시 도전할 수 있는지" 알려 준다.
     *
     * @param deckPower 내 덱 전투력
     * @param deckCards 출전 덱 (상성·강화·성급 분석용)
     * @param lostCards 이번 전투에서 잃은 카드 수
     */
    fun defeat(
        stage: StageDefinition,
        hero: TcgCard?,
        deckPower: Int,
        deckCards: List<TcgCard>,
        lostCards: Int,
        seed: Int = stage.number
    ): BattleResultPanel {
        val hints = buildList {
            // ① 상성 — 유리 속성 비율
            val share = BalanceRules.advantageShare(deckCards.map { it.element }, stage.enemyElement)
            if (share < 0.34) {
                add(
                    "🧭 상성 불리 — 적은 ${stage.enemyElement.displayName}. " +
                        "유리한 ${favourableNames(stage.enemyElement)} 카드를 덱에 넣으세요."
                )
            }
            // ② 강화(레벨)
            val avgUpgrade = if (deckCards.isEmpty()) 0.0 else deckCards.map { it.upgradeLevel }.average()
            if (avgUpgrade < 3.0) {
                add("⚒ 강화 부족 — 평균 Lv.${String.format("%.1f", avgUpgrade)}. 성장 화면에서 레벨을 올리면 ATK·DEF가 커집니다.")
            }
            // ③ 합성(성급)
            val avgStar = if (deckCards.isEmpty()) 0.0 else deckCards.map { it.starLevel }.average()
            if (avgStar < 2.5) {
                add("🧪 합성 부족 — 평균 ${String.format("%.1f", avgStar)}★. 같은 카드 합성으로 성급을 올리면 스탯이 ×1.26 씩 뜁니다.")
            }
            // ④ 전투력
            if (deckPower < stage.recommendedPower * 0.85) {
                add("⚔ 전투력 미달 — 내 덱 $deckPower / 권장 ${stage.recommendedPower}. 카드팩·장비로 덱을 보강하세요.")
            }
            // ⑤ 손실
            if (lostCards > 0) {
                add("💔 잃은 동료 ${lostCards}명 — 무료 재계약으로 즉시 되찾을 수 있습니다.")
            }
            if (isEmpty()) add("🔁 같은 덱으로 재도전하거나, 덱 편성에서 상성 조합을 바꿔 보세요.")
        }
        return BattleResultPanel(
            outcome = BattleOutcome.DEFEAT,
            stageNumber = stage.number,
            stageName = stage.name,
            bossKind = stage.bossKind,
            badge = if (stage.isBoss) "☠ 보스에게 패배" else "💀 전투 패배",
            subtitle = "${stage.number}스테이지 · ${stage.name}",
            heroCardId = hero?.id,
            heroName = hero?.name ?: "파트너",
            heroQuote = DEFEAT_QUOTES[seed % DEFEAT_QUOTES.size],
            rewardLines = emptyList(),
            hints = hints,
            primaryLabel = "다시 도전 ▸",
            nextStageNumber = null
        )
    }

    /** 적 속성에 유리한 속성 이름 (`빛` / `화염` …) */
    fun favourableNames(enemy: Element): String = Element.entries
        .filter { it.isAdvantageAgainst(enemy) }
        .joinToString("·") { it.displayName }
        .ifBlank { "상성" }
}
