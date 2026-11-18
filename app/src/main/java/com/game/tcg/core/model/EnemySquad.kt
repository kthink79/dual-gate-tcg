package com.game.tcg.core.model

/**
 * 적 진영 카드 1장 — 2026-11-18 개편으로 **실제 HP·공격력을 가진 전투 단위**가 되었다.
 *
 * > 사용자: "화면상 적 진영에는 9~12장이 표시되지만 실제 전투 엔진은 단일 enemyHp 풀만
 * > 상대하고 … 적 진영 카드들이 개별 HP·공격력을 가진 다수 카드(전열·후열)로
 * > 순차/동시 교전하여 적 카드를 전부 쓰러뜨려야 승리하도록 개편해 달라"
 *
 * 이전에는 [BattleLineup] 이 만든 카드가 화면 장식이었고 전투는 `enemyHp` 하나로 돌았다.
 * 지금은 이 단위 하나하나가 HP 를 가진 적 카드이며, 아군 공격은 **전열의 살아 있는 적 카드**
 * 한 장씩을 쓰러뜨려 가고, 적은 자기 차례에 개별 공격력을 들고 반격한다.
 *
 * @param slot 진영 안 순번 (0부터). 0~[EnemySquad.FRONT_ROW]-1 이 전열
 * @param cardId 일러스트로 쓰는 캐릭터 번호
 * @param name 표시 이름 (보스는 스테이지 적 이름)
 * @param element 속성 — 아군 공격의 상성 배율은 **맞는 적 카드의 속성**으로 계산한다
 * @param hp 남은 HP
 * @param maxHp 최대 HP
 * @param atk 이 카드 1장의 공격력
 * @param isFront 전열 여부 (전열이 먼저 공격받고, 먼저 반격한다)
 * @param isBoss 보스 카드 여부 (아트 강조·컷인용)
 */
data class EnemyUnit(
    val slot: Int,
    val cardId: Int,
    val name: String,
    val element: Element,
    val hp: Int,
    val maxHp: Int,
    val atk: Int,
    val isFront: Boolean,
    val isBoss: Boolean = false
) {
    val isAlive: Boolean get() = hp > 0

    val hpRatio: Float
        get() = if (maxHp <= 0) 0f else (hp.toFloat() / maxHp).coerceIn(0f, 1f)

    /** 피해를 적용한 사본 (HP 는 0 아래로 내려가지 않는다) */
    fun damaged(amount: Int): EnemyUnit = copy(hp = (hp - amount).coerceAtLeast(0))

    /** TCG 카드로 변환 — 기존 [com.game.tcg.core.engine.BattleEngine] 의 상성 계산에 넘긴다 */
    fun asCard(): TcgCard = TcgCard(
        id = cardId,
        name = name,
        element = element,
        baseCost = 10,
        atk = atk,
        def = maxHp,
        starLevel = if (isBoss) 5 else 3,
        skillText = if (isBoss) "심연의 압박" else "어둠의 일격"
    )
}

/**
 * 적 진영 편성 — 스테이지의 HP·공격력 총량을 **개별 카드 여러 장**으로 나눈다.
 * 나누는 규칙
 *  - 카드 수 = 사용자 덱 크기와 같게(9~12장) → 화면 그리드가 대칭으로 보인다
 *  - 총 HP 합 = [com.game.tcg.core.model.StageDefinition.enemyHp] (스테이지 밸런스 곡선 유지)
 *    · 전열은 두껍게, 후열은 얇게 (전열 1.0 : 후열 0.55)
 *  - 카드 1장 공격력 = [com.game.tcg.core.model.StageDefinition.enemyAtk]
 *    · 후열은 0.7배 (지원 사격)
 *  - 한 턴에 반격하는 적은 **전열 살아 있는 카드 최대 3장** ([MAX_ATTACKERS_PER_TURN])
 *    → 적을 한 장씩 쓰러뜨릴수록 받는 피해가 줄어드는 구조가 생긴다
 */
object EnemySquad {

    /** 전열 카드 수 (아군 덱의 전열 3칸과 대칭) */
    const val FRONT_ROW: Int = DeckRules.FRONT_SLOTS

    /** 한 턴에 반격하는 적 카드 최대 수 */
    const val MAX_ATTACKERS_PER_TURN: Int = 3

    /** 전열 카드 HP 가중치 */
    const val FRONT_HP_WEIGHT: Double = 1.0

    /** 후열 카드 HP 가중치 */
    const val BACK_HP_WEIGHT: Double = 0.55

    /** 후열 카드 공격력 배율 */
    const val BACK_ATK_SCALE: Double = 0.7

    /**
     * 스테이지 → 적 진영 카드 목록.
     *
     * @param size 진영 카드 수 (사용자 덱 크기, 1~12)
     */
    fun build(
        stage: StageDefinition,
        size: Int,
        isDemonLordBattle: Boolean = false
    ): List<EnemyUnit> = buildUnits(
        totalHp = stage.enemyHp,
        perCardAtk = stage.enemyAtk,
        enemyName = stage.enemyName,
        enemyElement = stage.enemyElement,
        size = size,
        isDemonLordBattle = isDemonLordBattle,
        bossCardId = stage.bossId
    )

    /**
     * 스테이지 없이 "총 HP + 한 턴 공격 총량"으로 편성한다 (탐색 조우·자유 원정·전초전).
     *
     * 예전 단일 적 시절의 공격력 수치를 그대로 받아, 카드 수만큼 나눠 **한 턴 위협이 같아지도록** 한다.
     * ([MAX_ATTACKERS_PER_TURN] 장이 나눠 때리므로 카드 1장 공격력은 1/3)
     */
    fun buildFromTotals(
        totalHp: Int,
        perTurnAtk: Int,
        enemyName: String,
        enemyElement: Element,
        size: Int,
        isDemonLordBattle: Boolean = false,
        bossCardId: Int? = null
    ): List<EnemyUnit> = buildUnits(
        totalHp = totalHp,
        perCardAtk = (perTurnAtk / MAX_ATTACKERS_PER_TURN).coerceAtLeast(1),
        enemyName = enemyName,
        enemyElement = enemyElement,
        size = size,
        isDemonLordBattle = isDemonLordBattle,
        bossCardId = bossCardId
    )

    /** 편성 본체 — 카드 1장 공격력을 직접 받는다 */
    private fun buildUnits(
        totalHp: Int,
        perCardAtk: Int,
        enemyName: String,
        enemyElement: Element,
        size: Int,
        isDemonLordBattle: Boolean,
        bossCardId: Int?
    ): List<EnemyUnit> {
        val count = size.coerceIn(1, DeckRules.MAX_DECK_SIZE)
        val lineup = BattleLineup.enemyLineup(
            enemyElement = enemyElement,
            enemyName = enemyName,
            size = count,
            isDemonLordBattle = isDemonLordBattle,
            bossCardId = bossCardId
        )
        val weights = List(count) { index -> if (index < FRONT_ROW) FRONT_HP_WEIGHT else BACK_HP_WEIGHT }
        val hps = splitTotal(totalHp.coerceAtLeast(count), weights)
        return List(count) { index ->
            val unit = lineup.getOrNull(index) ?: lineup.last()
            val atk = if (index < FRONT_ROW) {
                perCardAtk.coerceAtLeast(1)
            } else {
                (perCardAtk * BACK_ATK_SCALE).toInt().coerceAtLeast(1)
            }
            EnemyUnit(
                slot = index,
                cardId = unit.cardId,
                name = unit.name,
                element = unit.element,
                hp = hps[index],
                maxHp = hps[index],
                atk = atk,
                isFront = index < FRONT_ROW,
                isBoss = index == 0 && unit.isBoss
            )
        }
    }

    /**
     * 총량을 가중치대로 정확히 나눈다 (합계 = [total]).
     * 나머지는 전열 앞쪽부터 1씩 더해 총합이 절대 어긋나지 않게 한다.
     */
    fun splitTotal(total: Int, weights: List<Double>): List<Int> {
        if (weights.isEmpty()) return emptyList()
        val safeTotal = total.coerceAtLeast(0)
        val weightSum = weights.sum().takeIf { it > 0.0 } ?: return List(weights.size) { safeTotal / weights.size }
        val exact = weights.map { safeTotal * it / weightSum }
        val base = exact.map { it.toInt() }.toMutableList()
        var remainder = safeTotal - base.sum()
        // 남은 1점은 소수부가 큰 순서 → 같으면 앞 카드부터
        val order = exact.indices.sortedWith(
            compareByDescending<Int> { exact[it] - base[it] }.thenBy { it }
        )
        var cursor = 0
        while (remainder > 0 && order.isNotEmpty()) {
            base[order[cursor % order.size]] += 1
            cursor += 1
            remainder -= 1
        }
        return base
    }

    /** 아군이 때릴 적 카드 — 전열의 살아 있는 첫 카드, 전열이 비면 후열 첫 카드 */
    fun focusTarget(units: List<EnemyUnit>): EnemyUnit? =
        units.firstOrNull { it.isAlive && it.isFront } ?: units.firstOrNull { it.isAlive }

    /** 아군이 때릴 대상의 인덱스 (없으면 -1) */
    fun focusIndex(units: List<EnemyUnit>): Int = units.indexOfFirst { it == focusTarget(units) }

    /** 이번 턴에 반격하는 적 카드 — 전열 살아 있는 카드 → 모자라면 후열로 채운다 */
    fun attackersThisTurn(units: List<EnemyUnit>, limit: Int = MAX_ATTACKERS_PER_TURN): List<EnemyUnit> {
        val alive = units.filter { it.isAlive }
        if (alive.isEmpty()) return emptyList()
        val front = alive.filter { it.isFront }
        val back = alive.filterNot { it.isFront }
        return (front + back).take(limit)
    }

    /** 남은 카드 수 */
    fun aliveCount(units: List<EnemyUnit>): Int = units.count { it.isAlive }

    /**
     * 피해를 전열부터 차례로 적용한다 — 페어 오의처럼 큰 피해를 한 장에 몰아 주면
     * 남는 피해가 다음 카드로 이어져 "다수 적을 쓸어버리는" 손맛을 준다.
     */
    fun applyDamageSequential(units: List<EnemyUnit>, damage: Int): List<EnemyUnit> {
        if (damage <= 0 || units.isEmpty()) return units
        var remain = damage
        val order = units.indices.sortedWith(
            compareByDescending<Int> { units[it].isFront }.thenBy { it }
        )
        val out = units.toMutableList()
        order.forEach { index ->
            if (remain <= 0) return@forEach
            val unit = out[index]
            if (!unit.isAlive) return@forEach
            val used = minOf(remain, unit.hp)
            out[index] = unit.damaged(used)
            remain -= used
        }
        return out
    }

    /** 남은 HP 합 */
    fun totalHp(units: List<EnemyUnit>): Int = units.sumOf { it.hp.coerceAtLeast(0) }

    /** 최대 HP 합 */
    fun totalMaxHp(units: List<EnemyUnit>): Int = units.sumOf { it.maxHp }

    /** 전투 종료(전멸) 여부 */
    fun isWipedOut(units: List<EnemyUnit>): Boolean = units.isNotEmpty() && units.none { it.isAlive }

    /** 이번 턴 반격 총량 (화면·로그의 위협 표시용) */
    fun threatPerTurn(units: List<EnemyUnit>): Int = attackersThisTurn(units).sumOf { it.atk }

    /** "전멸까지 남은 카드" 표기 — 예: "적 4/9장" */
    fun remainingLabel(units: List<EnemyUnit>): String =
        "적 ${aliveCount(units)}/${units.size}장"
}
