package com.game.tcg.persistence

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * [GameSave] ↔ 텍스트 직렬화.
 *
 * 포맷 (사람이 읽을 수 있는 줄 단위 키=값):
 * ```
 * DUALGATE-SAVE
 * version=1
 * gold=1500
 * card=3|2100|1800|1|0|50|0|0|0|0|0|0|BATTLE
 * ...
 * ```
 * - 문자열은 UTF-8 → Base64 로 감싼다(구분자 `|`·`,`·줄바꿈과 충돌하지 않게).
 * - 목록은 키를 반복해 한 줄에 하나씩 쓴다.
 * - 읽기는 절대 예외를 던지지 않는다: 헤더/버전이 다르거나 줄이 깨졌으면 null 을 돌려주고,
 *   호출자(ViewModel)가 기본 상태로 안전하게 폴백한다.
 */
object SaveCodec {

    /** 파일 첫 줄 — 이 앱의 저장 파일임을 식별한다 */
    const val HEADER: String = "DUALGATE-SAVE"

    private const val VERSION_KEY = "version"

    /** 행 안의 필드 구분자 */
    private const val FIELD = "|"

    /** 목록 안의 구분자 */
    private const val LIST = ","

    // ────────────────────────────────────────────────────────────
    // 쓰기
    // ────────────────────────────────────────────────────────────

    fun encode(save: GameSave): String = buildString {
        appendLine(HEADER)
        appendLine("$VERSION_KEY=${GameSave.VERSION}")

        appendLine("gold=${save.gold}")
        appendLine("gems=${save.gems}")
        appendLine("holy=${save.holyWater}")
        appendLine("junk=${save.junkCards}")
        appendLine("tickets=${save.packTickets}")
        appendLine("exp=${save.playerExp}")
        save.shards.entries
            .sortedBy { it.key }
            .forEach { (cardId, amount) -> appendLine("shard=$cardId$FIELD$amount") }

        appendLine("storyStep=${save.storyStepIndex}")
        appendLine("storyLog=${save.storyReadLog.joinToString(LIST) { b64(it) }}")
        appendLine("evA=${enc(save.eventAChoice)}")
        appendLine("evARes=${enc(save.eventAResult)}")
        appendLine("evB=${enc(save.eventBChoice)}")
        appendLine("evBRes=${enc(save.eventBResult)}")
        appendLine("lily=${save.lilyFamiliarity}")
        appendLine("defBonus=${save.defenseBonusPercent}")
        appendLine("intel=${save.intelBonusPercent}")
        appendLine("ch2=${flag(save.chapter2PreludeOpened)}")

        val t = save.temptation
        appendLine(
            "tempt=" + listOf(
                t.gazeDurationSeconds, t.gazeZoomCount, t.gazeTemptationStacks,
                t.undercoverEncounterCount, t.undercoverFlirtCount,
                t.curseOfArchdemonStacks, t.stolenGold
            ).joinToString(FIELD)
        )

        appendLine("bond=${save.partnerBond}")
        appendLine("pVisual=${save.partnerVisual}")
        appendLine("dateLook=${flag(save.dateLookUnlocked)}")
        // 2026-11-27 v0.6.0 — 💎로 즉시 해금한 데이트 룩 캐릭터 번호
        appendLine("dateLookIds=${save.dateLookUnlockedIds.sorted().joinToString(LIST)}")
        appendLine("ultimate=${flag(save.ultimateUnlocked)}")
        appendLine("frontPenalty=${save.frontRowDefensePenaltyPercent}")
        appendLine("rift=${enc(save.trustRiftChoice)}")

        appendLine("victories=${save.battleVictories}")
        appendLine("pairCount=${save.pairUltimateCount}")
        appendLine("summonCount=${save.summonCount}")
        appendLine("buffPct=${save.summonBuffPercent}")
        appendLine("buffBattles=${save.summonBuffBattles}")
        appendLine("exploreCount=${save.explorationCount}")
        appendLine("exploreLog=${save.explorationLog.joinToString(LIST) { b64(it) }}")
        appendLine("energy=${save.gateEnergy}")

        save.cards.forEach { c ->
            appendLine(
                "card=" + listOf(
                    c.cardId, c.atk, c.def, c.starLevel, c.currentExp, c.bondLevel,
                    c.upgradeLevel, c.memoryAffinity, c.reincarnationCount,
                    c.equippedRelicCount, flag(c.isMemoryReset), c.memoryFragments
                ).joinToString(FIELD) + FIELD + c.visual +
                    // 2026-11-16 추가 필드 — 뒤에 붙여 구버전 저장과 호환된다
                    FIELD + c.battleCount + FIELD + c.lastBondDay
            )
        }
        save.party.forEach { p ->
            appendLine(
                "party=" + listOf(
                    p.cardId, p.slotIndex, p.hp, p.maxHp, p.memoryAffinity
                ).joinToString(FIELD)
            )
        }
        save.reserveIds.forEach { appendLine("reserve=$it") }
        save.reincarnations.forEach { r ->
            appendLine(
                "reinc=" + listOf(
                    r.templateId, r.reincarnationCount, r.lostUpgradeLevel,
                    r.lostMemoryAffinity, r.lostBondLevel, r.lostStarLevel, r.lostRelicCount
                ).joinToString(FIELD) + FIELD + b64(r.causeOfDeath) + FIELD + r.diedAtTurn
            )
        }
        save.equipment.forEach { e ->
            appendLine(
                "equip=" + b64(e.uid) + FIELD + b64(e.itemId) + FIELD + (e.equippedTo ?: -1)
            )
        }

        appendLine("stage=${save.stageCleared.joinToString(LIST)}")
        appendLine("selStage=${save.selectedStage}")
        save.defeatedDemonLords.forEach { appendLine("lord=$it") }
        save.vowedDemonLords.forEach { appendLine("vow=$it") }

        appendLine("hero=${save.lobbyHeroId}")
        appendLine("lVisual=${save.lobbyVisual}")
        // 캐릭터별 복장 — 2026-11-16: 고른 일상복/데이트복을 앱을 껐다 켜도 유지
        save.lobbyOutfits.entries
            .filter { (cardId, visual) -> cardId > 0 && visual.isNotBlank() }
            .sortedBy { it.key }
            .forEach { (cardId, visual) -> appendLine("outfit=$cardId$FIELD$visual") }
        appendLine("growthCard=${save.growthCardId}")
        appendLine("summonCard=${save.summonCardId}")
        appendLine("selCard=${save.selectedCardId}")
        appendLine("selVisual=${save.selectedVisual}")

        appendLine("introSeen=${flag(save.introSeen)}")
        appendLine("introReplay=${save.introReplayCount}")

        appendLine("bgmOn=${flag(save.bgmEnabled)}")
        appendLine("sfxOn=${flag(save.sfxEnabled)}")
        appendLine("bgmVol=${save.bgmVolume}")
        appendLine("sfxVol=${save.sfxVolume}")
    }

    // ────────────────────────────────────────────────────────────
    // 읽기
    // ────────────────────────────────────────────────────────────

    /**
     * 저장 텍스트를 [GameSave] 로 되돌린다.
     * 손상·구버전·타 앱 파일이면 null (크래시 없음).
     */
    fun decode(text: String?): GameSave? {
        if (text.isNullOrBlank()) return null
        return runCatching { decodeOrThrow(text) }.getOrNull()
    }

    private fun decodeOrThrow(text: String): GameSave? {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty() || lines.first() != HEADER) return null

        val version = lines.asSequence()
            .firstOrNull { it.startsWith("$VERSION_KEY=") }
            ?.substringAfter('=')
            ?.trim()
            ?.toIntOrNull()
            ?: return null
        if (version < GameSave.MIN_SUPPORTED_VERSION || version > GameSave.VERSION) return null

        val bag = HashMap<String, MutableList<String>>()
        lines.drop(1).forEach { line ->
            val at = line.indexOf('=')
            if (at <= 0) return@forEach
            val key = line.substring(0, at).trim()
            val value = line.substring(at + 1).trim()
            bag.getOrPut(key) { mutableListOf() }.add(value)
        }

        val defaults = GameSave()
        fun raw(key: String): String? = bag[key]?.firstOrNull()

        fun int(key: String, fallback: Int, min: Int = Int.MIN_VALUE): Int =
            (raw(key)?.toIntOrNull() ?: fallback).coerceAtLeast(min)

        fun long(key: String, fallback: Long): Long = raw(key)?.toLongOrNull() ?: fallback

        fun bool(key: String, fallback: Boolean): Boolean = when (raw(key)) {
            "1", "true" -> true
            "0", "false" -> false
            else -> fallback
        }

        fun str(key: String): String? = raw(key)?.let { dec(it) }?.takeIf { it.isNotEmpty() }

        fun strList(key: String): List<String> =
            raw(key)?.split(LIST)
                ?.filter { it.isNotEmpty() }
                ?.mapNotNull { dec(it) }
                ?: emptyList()

        fun intList(key: String): List<Int> =
            bag[key].orEmpty()
                .flatMap { it.split(LIST) }
                .mapNotNull { it.trim().toIntOrNull() }

        val rows = { key: String -> bag[key].orEmpty().map { it.split(FIELD) } }

        val cards = rows("card").mapNotNull { f ->
            if (f.size < 13) return@mapNotNull null
            val cardId = f[0].toIntOrNull() ?: return@mapNotNull null
            if (cardId <= 0) return@mapNotNull null
            SavedCard(
                cardId = cardId,
                atk = f[1].toIntOrNull()?.coerceAtLeast(1) ?: return@mapNotNull null,
                def = f[2].toIntOrNull()?.coerceAtLeast(1) ?: return@mapNotNull null,
                starLevel = f[3].toIntOrNull() ?: 1,
                currentExp = f[4].toIntOrNull() ?: 0,
                bondLevel = f[5].toIntOrNull() ?: 0,
                upgradeLevel = f[6].toIntOrNull() ?: 0,
                memoryAffinity = f[7].toIntOrNull() ?: 0,
                reincarnationCount = f[8].toIntOrNull() ?: 0,
                equippedRelicCount = f[9].toIntOrNull() ?: 0,
                isMemoryReset = f[10] == "1" || f[10] == "true",
                memoryFragments = f[11].toIntOrNull() ?: 0,
                visual = f[12],
                // 뒤에 붙은 선택 필드 — 없으면(구버전 저장) 0
                battleCount = f.getOrNull(13)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                lastBondDay = f.getOrNull(14)?.toIntOrNull() ?: 0
            )
        }.distinctBy { it.cardId }

        val party = rows("party").mapNotNull { f ->
            if (f.size < 5) return@mapNotNull null
            SavedPartyMember(
                cardId = f[0].toIntOrNull() ?: return@mapNotNull null,
                slotIndex = f[1].toIntOrNull() ?: return@mapNotNull null,
                hp = f[2].toIntOrNull() ?: return@mapNotNull null,
                maxHp = f[3].toIntOrNull() ?: return@mapNotNull null,
                memoryAffinity = f[4].toIntOrNull() ?: 0
            )
        }.distinctBy { it.slotIndex }

        val reincarnations = rows("reinc").mapNotNull { f ->
            if (f.size < 9) return@mapNotNull null
            val templateId = f[0].toIntOrNull() ?: return@mapNotNull null
            SavedReincarnation(
                templateId = templateId,
                reincarnationCount = f[1].toIntOrNull() ?: 1,
                lostUpgradeLevel = f[2].toIntOrNull() ?: 0,
                lostMemoryAffinity = f[3].toIntOrNull() ?: 0,
                lostBondLevel = f[4].toIntOrNull() ?: 0,
                lostStarLevel = f[5].toIntOrNull() ?: 1,
                lostRelicCount = f[6].toIntOrNull() ?: 0,
                causeOfDeath = dec(f[7]) ?: "",
                diedAtTurn = f[8].toIntOrNull() ?: 0
            )
        }

        val equipment = rows("equip").mapNotNull { f ->
            if (f.size < 3) return@mapNotNull null
            val uid = dec(f[0]) ?: return@mapNotNull null
            val itemId = dec(f[1]) ?: return@mapNotNull null
            val target = f[2].toIntOrNull() ?: -1
            SavedEquipment(
                uid = uid,
                itemId = itemId,
                equippedTo = if (target >= 0) target else null
            )
        }

        val tempt = raw("tempt")?.split(FIELD).orEmpty()
        fun t(i: Int, fallback: Int): Int = tempt.getOrNull(i)?.toIntOrNull() ?: fallback

        val shards = rows("shard").mapNotNull { f ->
            if (f.size < 2) return@mapNotNull null
            val cardId = f[0].toIntOrNull() ?: return@mapNotNull null
            val amount = f[1].toIntOrNull() ?: return@mapNotNull null
            if (cardId <= 0 || amount <= 0) null else cardId to amount
        }.toMap()

        return GameSave(
            gold = int("gold", defaults.gold, 0),
            gems = int("gems", defaults.gems, 0),
            holyWater = int("holy", defaults.holyWater, 0),
            junkCards = int("junk", defaults.junkCards, 0),
            packTickets = int("tickets", defaults.packTickets, 0),
            shards = shards,
            playerExp = int("exp", defaults.playerExp, 0),
            storyStepIndex = int("storyStep", defaults.storyStepIndex, 0),
            storyReadLog = strList("storyLog"),
            eventAChoice = str("evA"),
            eventAResult = str("evARes"),
            eventBChoice = str("evB"),
            eventBResult = str("evBRes"),
            lilyFamiliarity = int("lily", defaults.lilyFamiliarity, 0),
            defenseBonusPercent = int("defBonus", defaults.defenseBonusPercent, 0),
            intelBonusPercent = int("intel", defaults.intelBonusPercent, 0),
            chapter2PreludeOpened = bool("ch2", defaults.chapter2PreludeOpened),
            temptation = SavedTemptation(
                gazeDurationSeconds = tempt.getOrNull(0)?.toLongOrNull() ?: 0L,
                gazeZoomCount = t(1, 0),
                gazeTemptationStacks = t(2, 0),
                undercoverEncounterCount = t(3, 0),
                undercoverFlirtCount = t(4, 0),
                curseOfArchdemonStacks = t(5, 0),
                stolenGold = t(6, 0)
            ),
            partnerBond = int("bond", defaults.partnerBond, 0),
            partnerVisual = raw("pVisual") ?: defaults.partnerVisual,
            dateLookUnlocked = bool("dateLook", defaults.dateLookUnlocked),
            dateLookUnlockedIds = intList("dateLookIds").filter { it > 0 }.distinct().sorted(),
            ultimateUnlocked = bool("ultimate", defaults.ultimateUnlocked),
            frontRowDefensePenaltyPercent = int("frontPenalty", defaults.frontRowDefensePenaltyPercent, 0),
            trustRiftChoice = str("rift"),
            battleVictories = int("victories", defaults.battleVictories, 0),
            pairUltimateCount = int("pairCount", defaults.pairUltimateCount, 0),
            summonCount = int("summonCount", defaults.summonCount, 0),
            summonBuffPercent = int("buffPct", defaults.summonBuffPercent, 0),
            summonBuffBattles = int("buffBattles", defaults.summonBuffBattles, 0),
            explorationCount = int("exploreCount", defaults.explorationCount, 0),
            explorationLog = strList("exploreLog"),
            gateEnergy = int("energy", defaults.gateEnergy, 0),
            cards = cards,
            party = party,
            reserveIds = intList("reserve").filter { it > 0 }.distinct(),
            reincarnations = reincarnations,
            equipment = equipment,
            stageCleared = intList("stage").filter { it > 0 }.distinct().sorted(),
            selectedStage = int("selStage", defaults.selectedStage, 1),
            defeatedDemonLords = intList("lord").filter { it > 0 }.distinct(),
            vowedDemonLords = intList("vow").filter { it > 0 }.distinct(),
            lobbyHeroId = int("hero", defaults.lobbyHeroId, 1),
            lobbyVisual = raw("lVisual") ?: defaults.lobbyVisual,
            lobbyOutfits = rows("outfit")
                .mapNotNull { f ->
                    if (f.size < 2) return@mapNotNull null
                    val cardId = f[0].toIntOrNull() ?: return@mapNotNull null
                    val visual = f[1].trim()
                    if (cardId <= 0 || visual.isEmpty()) null else cardId to visual
                }
                .toMap(),
            growthCardId = int("growthCard", defaults.growthCardId, 0),
            summonCardId = int("summonCard", defaults.summonCardId, 0),
            selectedCardId = int("selCard", defaults.selectedCardId, 1),
            selectedVisual = raw("selVisual") ?: defaults.selectedVisual,
            introSeen = bool("introSeen", defaults.introSeen),
            introReplayCount = int("introReplay", defaults.introReplayCount, 0),
            bgmEnabled = bool("bgmOn", defaults.bgmEnabled),
            sfxEnabled = bool("sfxOn", defaults.sfxEnabled),
            bgmVolume = raw("bgmVol")?.toFloatOrNull() ?: defaults.bgmVolume,
            sfxVolume = raw("sfxVol")?.toFloatOrNull() ?: defaults.sfxVolume
        )
    }

    // ────────────────────────────────────────────────────────────
    // 인코딩 도우미
    // ────────────────────────────────────────────────────────────

    private fun flag(value: Boolean): String = if (value) "1" else "0"

    private fun enc(value: String?): String = value?.let { b64(it) } ?: ""

    private fun b64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun dec(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            String(Base64.getDecoder().decode(trimmed), StandardCharsets.UTF_8)
        }.getOrNull()
    }
}
