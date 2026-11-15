package com.game.tcg.core.engine

import com.game.tcg.core.model.EquipmentCatalog
import com.game.tcg.core.model.EquipmentItem
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.StageDefinition
import kotlin.random.Random

/** 스테이지 난이도 판정 — 내 덱 전투력과 권장 전투력 비교 */
enum class StageReadiness(val label: String, val advice: String) {
    EASY("여유", "덱 전투력이 권장치를 크게 웃돕니다."),
    EVEN("호각", "권장 전투력에 근접합니다. 속성 상성으로 승부하세요."),
    HARD("열세", "권장 전투력에 못 미칩니다. 강화·장비·카드팩으로 덱을 보강하세요."),
    LETHAL("위험", "전투력 차이가 큽니다. 전술 후퇴를 준비하거나 덱을 다시 짜세요.")
}

/**
 * 스테이지 클리어 정산 결과
 */
data class StageClearOutcome(
    val isNewClear: Boolean,
    val stage: StageDefinition,
    val gold: Int,
    val exp: Int,
    val tickets: Int,
    val gems: Int,
    val shardCardId: Int? = null,
    val shardCount: Int = 0,
    val equipmentDrop: EquipmentItem? = null,
    val nextStageNumber: Int? = null,
    val message: String = ""
)

/**
 * 스테이지 캠페인 엔진 — 2026-11-16 사용자 요청 "메인 플로우".
 *
 * ```
 * 스테이지 클리어 → 골드·경험치·카드팩 티켓·조각·확률 장비 → 카드팩/강화/연성 → 덱 전투력 상승 → 다음 스테이지
 * ```
 *
 * - 1스테이지부터 순서대로 열린다 (앞 스테이지를 깨야 다음 스테이지가 열린다).
 * - 보상은 재클리어해도 절반만 지급된다(파밍 폭주 방지).
 * - 장비 드롭은 스테이지별 확률표([com.game.tcg.core.model.StageReward.equipmentChance])를 따른다.
 */
object StageEngine {

    /** 이미 깬 스테이지를 다시 깰 때 보상 배율 (%) */
    const val REPEAT_REWARD_PERCENT: Int = 50

    /** 1스테이지 또는 앞 스테이지를 깬 경우에만 열린다 */
    fun isUnlocked(stageNumber: Int, clearedStages: Set<Int>): Boolean {
        if (stageNumber <= 1) return true
        if (stageNumber > StageCatalog.LAST_STAGE) return false
        return (stageNumber - 1) in clearedStages
    }

    /** 지금 도전할 수 있는 가장 앞 스테이지 */
    fun frontStage(clearedStages: Set<Int>): Int {
        val next = (1..StageCatalog.LAST_STAGE).firstOrNull { it !in clearedStages } ?: StageCatalog.LAST_STAGE
        return next
    }

    /**
     * 전투력 대비 난이도.
     *
     * 권장 전투력은 "그 스테이지를 무리 없이 넘기는 덱" 기준이라, 실제 전투에서는
     * 0.75배(권장의 3/4)만 되어도 호각으로 싸울 수 있다. (실측: 30스테이지 권장 230,000 대비
     * 150,000 덱으로 클리어, 대신 카드 2~3장 영구 상실)
     */
    fun readiness(deckPower: Int, stage: StageDefinition): StageReadiness {
        val ratio = deckPower.toDouble() / stage.recommendedPower
        return when {
            ratio >= 1.15 -> StageReadiness.EASY
            ratio >= 0.75 -> StageReadiness.EVEN
            ratio >= 0.55 -> StageReadiness.HARD
            else -> StageReadiness.LETHAL
        }
    }

    /** 전투력 비율 (%) — 화면 게이지용 */
    fun powerRatio(deckPower: Int, stage: StageDefinition): Int =
        if (stage.recommendedPower <= 0) 100 else (deckPower * 100 / stage.recommendedPower)

    /**
     * 예상 승률(%) — 전투력 비율을 0~100으로 압축한 참고 수치.
     * 실제 승패는 VS 전투 화면의 턴 진행으로 결정된다.
     */
    fun estimatedWinChance(deckPower: Int, stage: StageDefinition): Int {
        val ratio = deckPower.toDouble() / stage.recommendedPower
        val chance = when {
            ratio >= 1.20 -> 95.0
            ratio >= 1.00 -> 75.0 + (ratio - 1.00) / 0.20 * 20.0
            ratio >= 0.75 -> 60.0 + (ratio - 0.75) / 0.25 * 15.0
            ratio >= 0.50 -> 35.0 + (ratio - 0.50) / 0.25 * 25.0
            else -> 35.0 * (ratio / 0.50)
        }.coerceIn(2.0, 99.0)
        return chance.toInt()
    }

    /** 장비 드롭 판정 (확률표) */
    fun rollEquipment(stage: StageDefinition, random: Random = Random.Default): EquipmentItem? {
        val itemId = stage.reward.equipmentId ?: return null
        val chance = stage.reward.equipmentChance
        if (chance <= 0.0) return null
        if (chance < 1.0 && random.nextDouble() >= chance) return null
        return EquipmentCatalog.instantiate(itemId)
    }

    /** 다음 스테이지 번호 (마지막이면 null) */
    fun nextStageNumber(stageNumber: Int): Int? =
        if (stageNumber >= StageCatalog.LAST_STAGE) null else stageNumber + 1

    /**
     * 클리어 정산 — 보상·장비 드롭·다음 스테이지를 한 번에 계산한다.
     * 상태 변경(골드·조각·장비 지급)은 ViewModel이 이 결과를 받아 적용한다.
     */
    fun settle(
        stage: StageDefinition,
        alreadyCleared: Boolean,
        random: Random = Random.Default
    ): StageClearOutcome {
        val percent = if (alreadyCleared) REPEAT_REWARD_PERCENT else 100
        fun scaled(value: Int): Int = value * percent / 100
        val reward = stage.reward
        val drop = rollEquipment(stage, random)
        val next = if (alreadyCleared) null else nextStageNumber(stage.number)
        val bossLine = when (stage.bossKind) {
            com.game.tcg.core.model.BossKind.MID_BOSS -> " · 중간 보스 격파!"
            com.game.tcg.core.model.BossKind.DEMON_LORD -> " · 대마왕 격파!!"
            else -> ""
        }
        val rewardLine = buildList {
            add("${scaled(reward.gold)}G")
            add("EXP ${scaled(reward.exp)}")
            if (scaled(reward.packTickets) > 0) add("🎟 ${scaled(reward.packTickets)}")
            if (scaled(reward.gems) > 0) add("💎 ${scaled(reward.gems)}")
            if (reward.shardCardId != null && scaled(reward.shardCount) > 0) add("조각 +${scaled(reward.shardCount)}")
            if (drop != null) add("장비 ${drop.name}")
        }.joinToString(" · ")
        return StageClearOutcome(
            isNewClear = !alreadyCleared,
            stage = stage,
            gold = scaled(reward.gold),
            exp = scaled(reward.exp),
            tickets = scaled(reward.packTickets),
            gems = scaled(reward.gems),
            shardCardId = reward.shardCardId,
            shardCount = if (reward.shardCardId != null) scaled(reward.shardCount) else 0,
            equipmentDrop = drop,
            nextStageNumber = next,
            message = "[스테이지 ${stage.number} 클리어]${bossLine} $rewardLine"
        )
    }
}
