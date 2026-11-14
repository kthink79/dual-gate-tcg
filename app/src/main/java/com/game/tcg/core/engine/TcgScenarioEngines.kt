package com.game.tcg.core.engine

import com.game.tcg.core.model.*

// =============================================================================
// 1. 「기억의 잔향: 환생한 파트너와의 재회」 — 기억 초기화·파편 회상 엔진
// =============================================================================

/**
 * 기억 초기화 재획득 카드의 파편 수집·회상 엔진 인터페이스
 * - 마왕전에서 사망해 환생 풀에 들어간 캐릭터를 재획득하면 단순 복구되지 않고
 *   isMemoryReset = true(기억 초기화) 상태가 되며, 기억 파편 0/3 게이지를 갖는다.
 * - 현실 교감·전투 승리로 파편을 3개 모으면 「기억의 조각 회상」 이벤트가 열린다.
 */
interface IMemoryRecallEngine {

    /** 회상에 필요한 기억 파편 수 (기본 3) */
    val fragmentTarget: Int

    /** 카드의 현재 파편 게이지 */
    fun progressOf(card: TcgCard): MemoryFragmentProgress

    /**
     * 기억 파편 획득 (교감·전투 승리 등)
     * - 기억 초기화 상태가 아니면 아무 일도 일어나지 않는다 (실패 결과 반환).
     * - 목표치(3)를 넘겨 쌓이지 않는다.
     */
    fun grantFragment(
        card: TcgCard,
        amount: Int = 1,
        source: MemoryFragmentSource = MemoryFragmentSource.BATTLE
    ): MemoryFragmentGain

    /** 「기억의 조각 회상」 이벤트 개방 조건 충족 여부 */
    fun canOpenRecallEvent(card: TcgCard): Boolean

    /**
     * 「기억의 조각 회상」 특별 이벤트 진행
     * - 파편 3개를 모두 모은 기억 초기화 카드에 한해 스탯 보너스(원본의 +15%)와
     *   기억 복원(호감도 +20, isMemoryReset 해제)을 적용하고 대화를 돌려준다.
     */
    fun openRecallEvent(card: TcgCard): MemoryRecallResult
}

/**
 * 기억 초기화·파편 회상 엔진 기본 구현체
 */
class MemoryRecallEngine : IMemoryRecallEngine {

    override val fragmentTarget: Int = TcgCard.MEMORY_FRAGMENT_TARGET

    /** 회상 완료 시 원본 스탯 대비 보너스 비율 (15%) */
    private val recallStatBonusRatio: Double = 0.15

    /** 회상 완료 시 복원되는 호감도 */
    private val recalledBond: Int = 20

    override fun progressOf(card: TcgCard): MemoryFragmentProgress =
        MemoryFragmentProgress(
            fragments = card.memoryFragments.coerceIn(0, fragmentTarget),
            target = fragmentTarget
        )

    override fun grantFragment(
        card: TcgCard,
        amount: Int,
        source: MemoryFragmentSource
    ): MemoryFragmentGain {
        val progressBefore = progressOf(card)

        if (!card.isMemoryReset) {
            return MemoryFragmentGain(
                isSuccess = false,
                cardName = card.name,
                progress = progressBefore,
                source = source,
                failureReason = "기억 초기화 상태가 아닙니다.",
                message = "[기억 파편] '${card.name}'은(는) 기억이 온전한 상태라 파편이 필요하지 않습니다."
            )
        }

        if (amount <= 0) {
            return MemoryFragmentGain(
                isSuccess = false,
                cardName = card.name,
                progress = progressBefore,
                source = source,
                failureReason = "획득량이 0 이하입니다.",
                message = "[기억 파편] 획득량이 없어 파편이 쌓이지 않았습니다. (${progressBefore.gaugeText})"
            )
        }

        if (progressBefore.isComplete) {
            return MemoryFragmentGain(
                isSuccess = false,
                cardName = card.name,
                progress = progressBefore,
                source = source,
                failureReason = "이미 파편이 모두 모였습니다.",
                message = "[기억 파편] '${card.name}'의 파편은 이미 3/3 — 「기억의 조각 회상」을 열 수 있습니다."
            )
        }

        val before = card.memoryFragments.coerceIn(0, fragmentTarget)
        card.memoryFragments = (before + amount).coerceAtMost(fragmentTarget)
        val gained = card.memoryFragments - before
        val progressAfter = progressOf(card)

        val message = if (progressAfter.isComplete) {
            "[${source.displayName} · 기억 파편 +$gained] '${card.name}' ${progressAfter.gaugeText} — " +
                "잊혔던 문장들이 한 줄씩 되돌아왔습니다. 「기억의 조각 회상」 이벤트가 열렸습니다."
        } else {
            "[${source.displayName} · 기억 파편 +$gained] '${card.name}' ${progressAfter.gaugeText} — " +
                "기억의 조각이 희미하게 떠오릅니다. (앞으로 ${progressAfter.remaining}개 더 필요)"
        }

        return MemoryFragmentGain(
            isSuccess = true,
            cardName = card.name,
            gained = gained,
            progress = progressAfter,
            source = source,
            message = message
        )
    }

    override fun canOpenRecallEvent(card: TcgCard): Boolean =
        card.isMemoryReset && progressOf(card).isComplete

    override fun openRecallEvent(card: TcgCard): MemoryRecallResult {
        if (!card.isMemoryReset) {
            return MemoryRecallResult(
                isSuccess = false,
                cardName = card.name,
                failureReason = "기억 초기화 상태가 아닙니다.",
                message = "[회상 불가] '${card.name}'은(는) 기억이 온전한 상태입니다."
            )
        }

        val progress = progressOf(card)
        if (!progress.isComplete) {
            return MemoryRecallResult(
                isSuccess = false,
                cardName = card.name,
                failureReason = "기억 파편이 부족합니다. (${progress.gaugeText})",
                message = "[회상 불가] 기억 파편이 부족합니다. (${progress.gaugeText} — " +
                    "${progress.remaining}개 더 필요)"
            )
        }

        val atkBonus = (card.baseAtk * recallStatBonusRatio).toInt().coerceAtLeast(1)
        val defBonus = (card.baseDef * recallStatBonusRatio).toInt().coerceAtLeast(1)
        card.atk += atkBonus
        card.def += defBonus
        card.bondLevel = (card.bondLevel + recalledBond).coerceAtMost(100)
        // 기억 복원: 환생 횟수(reincarnationCount)만 상흔으로 남기고 초기화 플래그를 해제한다.
        card.isMemoryReset = false
        card.memoryFragments = 0

        val dialogue = MemoryRecallDialogue(
            subtitle = "「기억의 조각 회상」 — ${card.name}",
            lines = listOf(
                "「…이 목소리, 이 온기. 나, 당신을 알고 있어요. 그런데 왜 이름이 떠오르지 않죠?」",
                "파편이 손끝에서 빛으로 풀리며 검을 쥔 손이 떨린다. 당신은 그녀의 이름을 천천히 불렀다.",
                "「${card.name}. 그래요… 그 이름이었어요. 죽음의 문턱에서도 잊지 않으려 했던 이름.」",
                "「이번 생에는 당신보다 먼저 쓰러지지 않을게요. 다시, 계약하죠.」"
            ),
            closingLine = "기억 복원 완료 — ATK +${atkBonus} · DEF +${defBonus} · 호감도 +${recalledBond} " +
                "(${card.bondLevel}/100)"
        )

        return MemoryRecallResult(
            isSuccess = true,
            cardName = card.name,
            atkBonus = atkBonus,
            defBonus = defBonus,
            restoredBond = recalledBond,
            dialogue = dialogue,
            message = "[기억 복원] '${card.name}'이(가) 잊혔던 기억을 되찾았습니다. " +
                "ATK +${atkBonus} · DEF +${defBonus} · 호감도 +${recalledBond}"
        )
    }
}

// =============================================================================
// 2. 「신뢰의 균열: 마왕의 홀림을 눈치챈 파트너」 — 현실·이세계 교차 이벤트 엔진
// =============================================================================

/**
 * 긴급 교차 이벤트 엔진 인터페이스
 * - [홀림] 스택이 2 이상이면 파트너 아우렐리아가 눈치채고 추궁하는 이벤트가 열린다.
 */
interface ITrustRiftEngine {

    /** 이벤트 활성화에 필요한 최소 [홀림] 스택 */
    val activationThreshold: Int

    /** 이벤트 선택지 A — 솔직히 털어놓고 함께 정화한다 */
    val honestChoiceId: String

    /** 이벤트 선택지 B — 아무 일 아니라고 숨긴다 */
    val hideChoiceId: String

    /** 선택지 목록 */
    fun choices(): List<TrustRiftChoice>

    /** 이벤트 활성 여부 (이미 선택했으면 닫힌다) */
    fun isAvailable(temptationStacks: Int, alreadyResolved: Boolean): Boolean

    /** 선택지 처리 */
    fun resolve(choiceId: String, state: TemptationState, bondLevel: Int): TrustRiftResolution
}

/**
 * 긴급 교차 이벤트 엔진 기본 구현체
 */
class TrustRiftEngine : ITrustRiftEngine {

    override val activationThreshold: Int = 2
    override val honestChoiceId: String = "RIFT_A_CONFESS"
    override val hideChoiceId: String = "RIFT_B_HIDE"

    /** 고백 선택 시 상승하는 호감도 */
    private val honestBondGain: Int = 10

    /** 숨김 선택 시 하락하는 호감도 */
    private val hideBondLoss: Int = 15

    /** 숨김 선택 시 다음 전투 전열 방어 저하 (%) */
    private val hideDefensePenalty: Int = 10

    override fun choices(): List<TrustRiftChoice> = listOf(
        TrustRiftChoice(
            id = honestChoiceId,
            type = TrustRiftChoiceType.HONEST_CONFESSION,
            label = "솔직히 털어놓고 함께 정화한다",
            hint = "성수 없이 [홀림] 스택 -1 · 아우렐리아 호감도 +10"
        ),
        TrustRiftChoice(
            id = hideChoiceId,
            type = TrustRiftChoiceType.HIDE_IT,
            label = "아무 일 아니라고 숨긴다",
            hint = "[홀림] 스택 +1 · 호감도 -15 · 다음 전투 전열 방어 -10%"
        )
    )

    override fun isAvailable(temptationStacks: Int, alreadyResolved: Boolean): Boolean =
        !alreadyResolved && temptationStacks >= activationThreshold

    override fun resolve(
        choiceId: String,
        state: TemptationState,
        bondLevel: Int
    ): TrustRiftResolution {
        val choice = choices().firstOrNull { it.id == choiceId }
            ?: return TrustRiftResolution(
                isSuccess = false,
                choiceId = choiceId,
                choiceType = TrustRiftChoiceType.HIDE_IT,
                resolvedState = state.copy(),
                newBondLevel = bondLevel,
                failureReason = "알 수 없는 선택지입니다. (id=$choiceId)",
                message = "[교차 이벤트 실패] 알 수 없는 선택지입니다. (id=$choiceId)"
            )

        return when (choice.type) {
            TrustRiftChoiceType.HONEST_CONFESSION -> {
                // 성수 없이도 홀림 스택 1 정화 (시선 → 저주 순으로 차감)
                val purified = state.copy()
                if (purified.gazeTemptationStacks > 0) {
                    purified.gazeTemptationStacks -= 1
                } else if (purified.curseOfArchdemonStacks > 0) {
                    purified.curseOfArchdemonStacks -= 1
                }
                val newBond = (bondLevel + honestBondGain).coerceAtMost(100)

                TrustRiftResolution(
                    isSuccess = true,
                    choiceId = choiceId,
                    choiceType = choice.type,
                    resolvedState = purified,
                    bondDelta = honestBondGain,
                    newBondLevel = newBond,
                    frontRowDefensePenaltyPercent = 0,
                    message = "[신뢰 회복] 털어놓은 고백을 아우렐리아가 받아들였다. " +
                        "\"그 카페의 여자… 릴리라고 했죠. 혼자 감당하지 말아요. 제가 함께 정화하겠습니다.\"\n" +
                        "[홀림] 스택 -1 (성수 소모 없음) · 호감도 +$honestBondGain → $newBond/100"
                )
            }

            TrustRiftChoiceType.HIDE_IT -> {
                val stained = state.copy()
                stained.curseOfArchdemonStacks = (stained.curseOfArchdemonStacks + 1).coerceAtMost(5)
                val newBond = (bondLevel - hideBondLoss).coerceAtLeast(0)

                TrustRiftResolution(
                    isSuccess = true,
                    choiceId = choiceId,
                    choiceType = choice.type,
                    resolvedState = stained,
                    bondDelta = -hideBondLoss,
                    newBondLevel = newBond,
                    frontRowDefensePenaltyPercent = hideDefensePenalty,
                    message = "[신뢰의 균열] 눈을 피한 순간, 그녀의 목소리가 반 발낮아졌다. " +
                        "\"…알겠습니다. 계약자께서 그렇게 말씀하신다면.\"\n" +
                        "[홀림] 스택 +1 · 호감도 -$hideBondLoss → $newBond/100 · " +
                        "다음 전투 전열 방어 -$hideDefensePenalty%"
                )
            }
        }
    }
}

// =============================================================================
// 3. 「페어 얼티밋 오의」 컷인 엔진
// =============================================================================

/**
 * 페어 얼티밋 오의 엔진 인터페이스
 * - 아군 2명 이상 생존 + 마나 3 이상일 때 발동 가능한 협동 오의.
 * - 전투당 1회 제한, 황금·네온 컷인 배너와 전투 로그를 함께 돌려준다.
 */
interface IPairUltimateEngine {

    /** 발동에 필요한 최소 마나 */
    val requiredMana: Int

    /** 발동에 필요한 최소 생존 아군 수 */
    val requiredSurvivors: Int

    /** 2인 생존 시 배율 */
    val duoMultiplier: Double

    /** 3인 이상 생존 시 배율 */
    val trioMultiplier: Double

    /** 발동 가능 여부 판정 */
    fun check(aliveCount: Int, mana: ManaState, usedThisBattle: Boolean): PairUltimateReady

    /** 페어 얼티밋 오의 발동 (데미지·컷인 배너·로그) */
    fun activate(
        attacker: TcgCard,
        defender: TcgCard,
        penalty: CombatPenalty,
        aliveCount: Int,
        mana: ManaState,
        usedThisBattle: Boolean
    ): PairUltimateResult
}

/**
 * 페어 얼티밋 오의 엔진 기본 구현체
 */
class PairUltimateEngine : IPairUltimateEngine {

    override val requiredMana: Int = 3
    override val requiredSurvivors: Int = 2
    override val duoMultiplier: Double = 2.2
    override val trioMultiplier: Double = 2.6

    /** 오의 각성(호감도 100) 시 추가 배율 */
    private val awakenedBonus: Double = 1.25

    override fun check(aliveCount: Int, mana: ManaState, usedThisBattle: Boolean): PairUltimateReady {
        val reason = when {
            usedThisBattle -> "이번 전투에서 이미 페어 오의를 사용했습니다. (전투당 1회)"
            aliveCount < requiredSurvivors -> "생존한 아군이 ${requiredSurvivors}명 이상이어야 합니다. (현재 ${aliveCount}명)"
            mana.currentMana < requiredMana -> "마나가 부족합니다. (필요 ${requiredMana}, 보유 ${mana.currentMana})"
            else -> "아군 ${aliveCount}인의 합격 준비 완료 — 마나 ${requiredMana} 소모"
        }
        return PairUltimateReady(
            canActivate = usedThisBattle.not() &&
                aliveCount >= requiredSurvivors &&
                mana.currentMana >= requiredMana,
            reason = reason,
            aliveSurvivors = aliveCount,
            currentMana = mana.currentMana
        )
    }

    override fun activate(
        attacker: TcgCard,
        defender: TcgCard,
        penalty: CombatPenalty,
        aliveCount: Int,
        mana: ManaState,
        usedThisBattle: Boolean
    ): PairUltimateResult {
        val readiness = check(aliveCount, mana, usedThisBattle)
        if (!readiness.canActivate) {
            return PairUltimateResult(
                isSuccess = false,
                failureReason = readiness.reason,
                bannerSubtitle = readiness.reason
            )
        }

        val elementMultiplier = attacker.element.getDamageMultiplier(defender.element)
        val survivorMultiplier = if (aliveCount >= 3) trioMultiplier else duoMultiplier
        val bondMultiplier = if (attacker.isUltimateUnlocked) awakenedBonus else 1.0
        val penaltyMultiplier =
            (100 - penalty.attackReductionPercent).coerceAtLeast(10) / 100.0

        val damage = (attacker.atk * elementMultiplier * survivorMultiplier * bondMultiplier * penaltyMultiplier)
            .toInt()

        val bannerTitle = "⚡ 페어 얼티밋 오의 발동 — 쌍성의 심연참"
        val bannerSubtitle = "${attacker.name} × 아군 ${aliveCount}인의 합격 — ${damage} 피해"

        val logLines = mutableListOf(
            "[⚡ 페어 얼티밋 오의] 아군 ${aliveCount}인의 마나가 하나로 겹쳐졌다. " +
                "황금빛 궤적과 네온 잔상이 교차하며 ${defender.name}을(를) 꿰뚫는다!",
            "[⚡ 오의 판정] ${attacker.name} → ${defender.name} : ${damage} 데미지 " +
                "(생존 ${aliveCount}인 합격 x${survivorMultiplier}" +
                (if (elementMultiplier != 1.0) ", 상성 x$elementMultiplier" else "") +
                (if (attacker.isUltimateUnlocked) ", 각성 오의 +25%" else "") +
                (if (penalty.isCharmed) ", 홀림 -${penalty.attackReductionPercent}%" else "") +
                ") · 마나 -${requiredMana}"
        )

        return PairUltimateResult(
            isSuccess = true,
            damage = damage,
            manaSpent = requiredMana,
            survivorBonusMultiplier = survivorMultiplier,
            bannerTitle = bannerTitle,
            bannerSubtitle = bannerSubtitle,
            style = PairUltimateStyle.GOLDEN_NEON,
            logLines = logLines
        )
    }
}

// =============================================================================
// 4. 제2장 프롤로그 — 제1군단장 '그림자 기사' 전초전 노드 모델
// =============================================================================

/**
 * 5화 이후 열리는 다음 여정 노드 (챕터 프롤로그)
 */
data class StoryArcNode(
    val id: String,
    val chapter: String,
    val title: String,
    val body: String,
    val objective: String,
    val requiredStepIndex: Int,
    val bossName: String,
    val bossElement: Element,
    val bossHp: Int,
    val bossAtk: Int,
    val bossSkill: String
) {
    /** 이 노드가 열리는지 (제1장 마지막 화를 읽은 뒤) */
    fun isUnlocked(storyStepIndex: Int): Boolean = storyStepIndex >= requiredStepIndex
}

/**
 * 제2장 프롤로그: 제1군단장 '그림자 기사' 전초전
 */
object ChapterTwoScript {

    val SHADOW_KNIGHT_PRELUDE = StoryArcNode(
        id = "CH2_PRELUDE_SHADOW_KNIGHT",
        chapter = "제2장 프롤로그",
        title = "제1군단장 '그림자 기사' 전초전",
        body = "릴리스가 쓰러진 자리에는 검은 갑주의 흔적만 남았다. 그날 밤, 매탄동 아파트 단지의 " +
            "가로등이 일제히 꺼지고 굳은 발소리가 성문 앞에서 멈췄다.\n" +
            "「황혼의 마왕을 베었다고? 그렇다면 너희가 계약자의 무리군. " +
            "제1군단장의 이름으로, 이 문 뒤의 모든 것을 회수하러 왔다.」\n" +
            "가면 아래에서 붉은 눈이 열리고, 갑주 사이로 심연의 안개가 새어 나온다. " +
            "아직 제2장은 시작되지 않았다. 지금은 전초전이다.",
        objective = "제1군단장 그림자 기사와 전초전 · 마나 3 페어 오의 준비",
        requiredStepIndex = 5,
        bossName = "제1군단장 그림자 기사",
        bossElement = Element.DARK,
        bossHp = 32000,
        bossAtk = 3400,
        bossSkill = "심연의 갑주 · 매 턴 전열 방어를 갉아먹는 암흑 파동"
    )

    /** 모든 챕터 프롤로그 노드 */
    val ALL: List<StoryArcNode> get() = listOf(SHADOW_KNIGHT_PRELUDE)
}
