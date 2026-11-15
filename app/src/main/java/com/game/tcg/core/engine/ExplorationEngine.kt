package com.game.tcg.core.engine

import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import kotlin.random.Random

// ============================================================================
// 「탐색」 — 차원 틈새 탐색 · 기억 파편 수색 · 랜덤 조우
// 메인 로비 좌측 서브메뉴 '탐색'이 여는 전용 모드.
// 기존 이계 전투(전투 조우) · 「기억의 잔향」(기억 파편) · 유혹 페널티(홀림 스택)와
// 직접 연결되는 보상/위험을 돌려준다.
// ============================================================================

/**
 * 탐색 모드 3종 — 각 1 차원 균열 에너지를 소모한다.
 */
enum class ExplorationMode(
    val displayName: String,
    val icon: String,
    val energyCost: Int,
    val description: String
) {
    RIFT(
        displayName = "차원 틈새 탐색",
        icon = "🌀",
        energyCost = 1,
        description = "균열 너머로 뛰어들어 전리품·조우·기습을 노립니다."
    ),
    FRAGMENT(
        displayName = "기억 파편 수색",
        icon = "🌙",
        energyCost = 1,
        description = "잊힌 동료의 잔향을 좇아 기억 파편을 캐냅니다."
    ),
    ENCOUNTER(
        displayName = "랜덤 조우",
        icon = "👤",
        energyCost = 1,
        description = "현실과 이계의 경계에서 낯선 인연과 마주칩니다."
    )
}

/**
 * 탐색 보상 종류
 */
enum class ExplorationRewardType {
    GOLD,
    GEMS,
    HOLY_WATER,
    JUNK_CARD,
    GATE_ENERGY,
    BOND,
    MEMORY_FRAGMENT,
    TEMPTATION,

    /** 동료와의 조우 — 해당 캐릭터의 호감도가 오른다 */
    COMPANION_BOND
}

/**
 * 탐색 보상 1건
 */
data class ExplorationReward(
    val type: ExplorationRewardType,
    val amount: Int,
    val label: String
)

/**
 * 탐색 1회의 결과 — 화면 연출용 서사 + 적용할 보상 + (조우 시) 전투 상대 정보
 */
data class ExplorationResult(
    val mode: ExplorationMode,
    val title: String,
    val narrative: String,
    val rewards: List<ExplorationReward> = emptyList(),
    val enemyName: String? = null,
    val enemyElement: Element = Element.DARK,
    val enemyHp: Int = 0,
    val enemyMaxHp: Int = 0,
    val enemyAtk: Int = 0,
    val fragmentTargetCardId: Int? = null,
    val bondGained: Int = 0,
    val temptationGained: Int = 0,
    /** 조우한 캐릭터의 도감 번호 (동료 조우 시) */
    val companionCardId: Int? = null,
    val companionDisplayName: String? = null
) {
    /** 이번 탐색이 전투 조우로 이어지는지 */
    val startsBattle: Boolean get() = enemyName != null && enemyHp > 0

    /** 보상 요약 한 줄 */
    val rewardSummary: String
        get() = if (rewards.isEmpty()) "획득 없음" else rewards.joinToString(" · ") { it.label }
}

/**
 * 탐색 판정에 필요한 현재 상태 스냅샷 (도메인 계층은 UI 상태를 알지 않는다)
 */
data class ExplorationContext(
    val gateEnergy: Int = 0,
    val temptationStacks: Int = 0,
    val memoryResetCardIds: List<Int> = emptyList(),
    val explorationCount: Int = 0,
    val partnerBond: Int = 0,
    val gold: Int = 0
) {
    /** 기억 파편을 나눠 줄 수 있는 '기억이 지워진 동료'가 있는지 */
    val hasMemoryResetTarget: Boolean get() = memoryResetCardIds.isNotEmpty()
}

/**
 * 탐색 엔진 인터페이스
 */
interface IExplorationEngine {
    fun explore(mode: ExplorationMode, context: ExplorationContext): ExplorationResult
}

/**
 * 탐색 엔진 기본 구현체 — 100면체 판정으로 결과를 고른다.
 * @param d100 0~99 난수 공급자 (테스트에서는 고정값 람다로 주입)
 */
class ExplorationEngine(
    private val d100: () -> Int = { Random.nextInt(0, 100) }
) : IExplorationEngine {

    override fun explore(mode: ExplorationMode, context: ExplorationContext): ExplorationResult =
        when (mode) {
            ExplorationMode.RIFT -> exploreRift(context)
            ExplorationMode.FRAGMENT -> exploreFragment(context)
            ExplorationMode.ENCOUNTER -> exploreEncounter(context)
        }

    private fun roll(): Int = d100().coerceIn(0, 99)

    private fun amount(range: IntRange): Int {
        val span = range.last - range.first + 1
        return range.first + (roll() % span)
    }

    // ── 🌀 차원 틈새 탐색 ──
    private fun exploreRift(context: ExplorationContext): ExplorationResult {
        val r = roll()
        return when {
            r < 30 -> {
                val gold = amount(150..400)
                ExplorationResult(
                    mode = ExplorationMode.RIFT,
                    title = "균열에 흩어진 금화 더미",
                    narrative = "틈새 안쪽, 무너진 제단 위에 옛 원정대의 금화 주머니가 흩어져 있었다. " +
                            "균열이 닫히기 전에 전부 챙겼다.",
                    rewards = listOf(ExplorationReward(ExplorationRewardType.GOLD, gold, "금화 +${gold}"))
                )
            }
            r < 48 -> {
                val gems = amount(2..6)
                ExplorationResult(
                    mode = ExplorationMode.RIFT,
                    title = "수정으로 굳은 마나 결정",
                    narrative = "차원 마나가 응결된 보석이 발밑에서 은은하게 빛났다. 손에 쥐자 손끝이 얼얼하다.",
                    rewards = listOf(ExplorationReward(ExplorationRewardType.GEMS, gems, "보석 +${gems}"))
                )
            }
            r < 60 -> {
                val water = 1
                ExplorationResult(
                    mode = ExplorationMode.RIFT,
                    title = "빛나는 성수 샘",
                    narrative = "틈새의 바닥에서 성스러운 물이 솟는다. 심연의 잔향이 옅은 곳에만 맺히는 샘이다.",
                    rewards = listOf(ExplorationReward(ExplorationRewardType.HOLY_WATER, water, "성수 +${water}"))
                )
            }
            r < 72 -> {
                if (context.hasMemoryResetTarget) {
                    val targetId = context.memoryResetCardIds[context.explorationCount % context.memoryResetCardIds.size]
                    ExplorationResult(
                        mode = ExplorationMode.RIFT,
                        title = "잊힌 목소리의 잔향",
                        narrative = "균열 저편에서 누군가 당신의 이름을 불렀다. 돌아보니 아무도 없고, " +
                                "발치에 기억의 파편 하나가 떨어져 있었다.",
                        rewards = listOf(
                            ExplorationReward(ExplorationRewardType.MEMORY_FRAGMENT, 1, "기억 파편 +1")
                        ),
                        fragmentTargetCardId = targetId
                    )
                } else {
                    val gold = amount(120..200)
                    ExplorationResult(
                        mode = ExplorationMode.RIFT,
                        title = "정적만 남은 틈새",
                        narrative = "균열 안은 조용했다. 부서진 카드 조각과 잔돈만이 남아 있었다.",
                        rewards = listOf(ExplorationReward(ExplorationRewardType.GOLD, gold, "금화 +${gold}"))
                    )
                }
            }
            r < 88 -> {
                val variant = roll() % 3
                when (variant) {
                    0 -> ExplorationResult(
                        mode = ExplorationMode.RIFT,
                        title = "틈새의 그림자 마수",
                        narrative = "어둠이 접히더니 비늘 달린 그림자가 튀어나왔다. 균열이 닫히기 전에 정면에서 막아야 한다.",
                        rewards = listOf(ExplorationReward(ExplorationRewardType.GEMS, 0, "조우")),
                        enemyName = "틈새의 그림자 마수",
                        enemyElement = Element.DARK,
                        enemyHp = 8000,
                        enemyMaxHp = 8000,
                        enemyAtk = 700
                    )
                    1 -> ExplorationResult(
                        mode = ExplorationMode.RIFT,
                        title = "차원 수호자와 조우",
                        narrative = "빛나는 문양의 수호자가 길을 막아섰다. 통과하려면 계약자의 검을 보여야 한다.",
                        rewards = listOf(ExplorationReward(ExplorationRewardType.GEMS, 0, "조우")),
                        enemyName = "차원 수호자",
                        enemyElement = Element.LIGHT,
                        enemyHp = 12000,
                        enemyMaxHp = 12000,
                        enemyAtk = 1100
                    )
                    else -> ExplorationResult(
                        mode = ExplorationMode.RIFT,
                        title = "심연의 사냥개",
                        narrative = "심연의 사냥개가 균열 냄새를 맡고 달려들었다. 물러서면 뒤편의 도시가 위험하다.",
                        rewards = listOf(ExplorationReward(ExplorationRewardType.GEMS, 0, "조우")),
                        enemyName = "심연의 사냥개",
                        enemyElement = Element.DARK,
                        enemyHp = 9000,
                        enemyMaxHp = 9000,
                        enemyAtk = 850
                    )
                }
            }
            else -> {
                val gems = amount(2..4)
                ExplorationResult(
                    mode = ExplorationMode.RIFT,
                    title = "심연의 속삭임",
                    narrative = "틈새 깊은 곳에서 달콤한 목소리가 당신의 이름을 불렀다. 돌아서는 순간, " +
                            "보석이 손에 쥐어져 있었다. 손끝이 이상하게 뜨겁다.",
                    rewards = listOf(
                        ExplorationReward(ExplorationRewardType.GEMS, gems, "보석 +${gems}"),
                        ExplorationReward(ExplorationRewardType.TEMPTATION, 1, "[홀림] +1")
                    ),
                    temptationGained = 1
                )
            }
        }
    }

    // ── 🌙 기억 파편 수색 ──
    private fun exploreFragment(context: ExplorationContext): ExplorationResult {
        if (!context.hasMemoryResetTarget) {
            val r = roll()
            return if (r < 50) {
                ExplorationResult(
                    mode = ExplorationMode.FRAGMENT,
                    title = "메아리 없는 잔향",
                    narrative = "수색을 마쳤지만 되찾아 줄 기억이 없다. 균열 벽에 박힌 마나 결정 하나를 챙겼다.",
                    rewards = listOf(ExplorationReward(ExplorationRewardType.GEMS, 1, "보석 +1"))
                )
            } else {
                val gold = amount(80..150)
                ExplorationResult(
                    mode = ExplorationMode.FRAGMENT,
                    title = "잔향이 스러진 자리",
                    narrative = "기억이 지워진 동료가 없어 파편은 맺히지 않았다. 대신 잊힌 원정대의 잔돈을 주웠다.",
                    rewards = listOf(ExplorationReward(ExplorationRewardType.GOLD, gold, "금화 +${gold}"))
                )
            }
        }
        val targetId = context.memoryResetCardIds[context.explorationCount % context.memoryResetCardIds.size]
        val gold = amount(60..140)
        return ExplorationResult(
            mode = ExplorationMode.FRAGMENT,
            title = "기억의 파편 수색 성공",
            narrative = "잔향이 짙은 길을 따라갔다. 균열 틈에 박힌 빛의 파편이 손바닥에 닿자, " +
                    "잊힌 목소리가 한 마디 떠올랐다. 「…그때, 고마웠어요.」",
            rewards = listOf(
                ExplorationReward(ExplorationRewardType.MEMORY_FRAGMENT, 1, "기억 파편 +1"),
                ExplorationReward(ExplorationRewardType.GOLD, gold, "금화 +${gold}")
            ),
            fragmentTargetCardId = targetId
        )
    }

    // ── 👤 랜덤 조우 ──
    private fun exploreEncounter(context: ExplorationContext): ExplorationResult {
        val r = roll()
        return when {
            r < 20 -> {
                val gold = amount(200..320)
                ExplorationResult(
                    mode = ExplorationMode.ENCOUNTER,
                    title = "유랑 상인의 수레",
                    narrative = "이계의 물건을 현실 돈으로 바꿔 주는 유랑 상인을 만났다. " +
                            "값을 깎아 주며 잡카드 한 장을 덤으로 얹어 줬다.",
                    rewards = listOf(
                        ExplorationReward(ExplorationRewardType.GOLD, gold, "금화 +${gold}"),
                        ExplorationReward(ExplorationRewardType.JUNK_CARD, 1, "잡카드 +1")
                    )
                )
            }
            r < 40 -> {
                val gold = amount(350..500)
                ExplorationResult(
                    mode = ExplorationMode.ENCOUNTER,
                    title = "은발의 소녀 '릴리'",
                    narrative = "비를 피한 카페, 은발의 소녀가 마주 앉았다. 「이런 밤엔 혼자 차를 마시기엔 쓸쓸하지 않아?」 " +
                            "이야기를 나누는 사이 지갑이 가벼워졌지만, 이상하게 기분은 좋았다. " +
                            "그녀의 목덜미에서 심연의 향이 옅게 났다.",
                    rewards = listOf(
                        ExplorationReward(ExplorationRewardType.GOLD, gold, "금화 +${gold}"),
                        ExplorationReward(ExplorationRewardType.TEMPTATION, 1, "[홀림] +1")
                    ),
                    temptationGained = 1
                )
            }
            r < 60 -> {
                ExplorationResult(
                    mode = ExplorationMode.ENCOUNTER,
                    title = "아우렐리아와의 밤 산책",
                    narrative = "평상복 차림의 아우렐리아가 조용히 곁을 걸었다. 「계약자님, 오늘은… 조금 쉬어도 되나요?」 " +
                            "그녀의 손끝이 당신의 봉인 문양 위에 잠시 머물렀다.",
                    rewards = listOf(ExplorationReward(ExplorationRewardType.BOND, 10, "호감도 +10")),
                    bondGained = 10
                )
            }
            r < 70 -> companionEncounter(context)
            r < 80 -> {
                ExplorationResult(
                    mode = ExplorationMode.ENCOUNTER,
                    title = "봉인된 제단",
                    narrative = "폐건물 옥상에 세워진 작은 제단. 성수를 담아 두는 그릇이 먼지 속에서 굴러나왔다.",
                    rewards = listOf(
                        ExplorationReward(ExplorationRewardType.HOLY_WATER, 1, "성수 +1"),
                        ExplorationReward(ExplorationRewardType.GEMS, 2, "보석 +2")
                    )
                )
            }
            else -> ExplorationResult(
                mode = ExplorationMode.ENCOUNTER,
                title = "그림자 기사의 잔향",
                narrative = "골목의 반사된 유리창에 갑주를 입은 그림자가 비쳤다. 제1군단장의 잔향이다. " +
                        "그림자가 검을 뽑아 들자 공기가 무거워졌다.",
                rewards = listOf(ExplorationReward(ExplorationRewardType.GEMS, 0, "조우")),
                enemyName = "그림자 기사의 잔향",
                enemyElement = Element.DARK,
                enemyHp = 9000,
                enemyMaxHp = 9000,
                enemyAtk = 950
            )
        }
    }
    // ── 🎭 동료 조우 (170명 전원) ──
    /**
     * 도감 No.1~169 · No.184의 170명 중 한 명과 길에서 마주친다.
     * 해당 캐릭터의 호감도가 오르고, 현실 교감·데이트 룩 해금으로 이어진다.
     */
    private fun companionEncounter(context: ExplorationContext): ExplorationResult {
        val roster = CardCatalog.ALL
        // 탐색 횟수까지 반영해 같은 판정값이라도 매번 다른 인연이 걸리도록 한다
        val index = (context.explorationCount + roll()) % roster.size
        val record = roster[index]
        val bond = 10 + (index % 3) * 5   // 10 / 15 / 20
        return ExplorationResult(
            mode = ExplorationMode.ENCOUNTER,
            title = "인연 조우 · ${record.fullName}",
            narrative = encounterNarrative(record.name, record.title, record.id),
            rewards = listOf(
                ExplorationReward(
                    ExplorationRewardType.COMPANION_BOND,
                    bond,
                    "${record.name} 호감도 +${bond}"
                )
            ),
            bondGained = bond,
            companionCardId = record.id,
            companionDisplayName = record.fullName
        )
    }

    /** 조우 연출 문구 — 캐릭터마다 다른 장면을 보여 준다 */
    private fun encounterNarrative(name: String, title: String, id: Int): String = when (id % 5) {
        0 -> "골목 끝, 「$title」 $name" + "(이)가 낡은 상자에 걸터앉아 손을 흔들었다. " +
                "\"계약자지? 마침 네 얼굴을 한 번 보고 싶었어.\""
        1 -> "비를 피한 처마 밑에서 「$title」 $name" + "(을)를 만났다. " +
                "그녀는 아무 말 없이 자리를 반쯤 비켜 주고, 옆에 서 있는 게 허락이라는 듯 고개를 끄덕였다."
        2 -> "차원 균열의 잔광이 비치는 광장에서 「$title」 $name" + "(이)가 다가왔다. " +
                "\"이 근처는 위험해. …딱 오늘만, 내가 데려다줄게.\""
        3 -> "포장마차 불빛 아래 「$title」 $name" + "(이)가 컵을 하나 더 꺼내 놓았다. " +
                "\"말 안 해도 알아. 이런 밤엔 누군가 옆에 있어야 하잖아.\""
        else -> "계약자의 그림자를 밟고 「$title」 $name" + "(이)가 조용히 따라붙었다. " +
                "\"방해할 생각은 없어. 그냥… 네가 어디까지 가는지 보고 싶어서.\""
    }
}
