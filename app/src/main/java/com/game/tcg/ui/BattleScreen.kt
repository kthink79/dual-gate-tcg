package com.game.tcg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.game.tcg.core.engine.DeckAutoBuilder
import com.game.tcg.core.model.BattleLineup
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.EnemyUnit
import com.game.tcg.core.model.LineupUnit
import com.game.tcg.core.model.VisualState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * ⚔️ 이계 전투 — 비주얼 VS 아레나 (2026-11-16 개편 2차)
 *
 * 사용자 요청(실기기 스크린샷 25054):
 *  「케릭터가 너무 작고, 일부분만 보이는 경우가 많아 화면에는 최대 12장만 보이게 해주고
 *    스크롤로 위 아래 움직일 수 있게 해줘. 하단 FIGHT 바가 카드를 가린다.」
 *
 * 이제
 *  - 적/내 진영 카드는 **2:3 세로 카드**에 아트를 Fit 으로 꼭 맞춘다(잘림 없음)
 *  - 진영은 **탭(적 진영 / 내 진영)** 으로 나누고, 각 진영은 3열 × 최대 4행 = 12장,
 *    넘치는 만큼은 그리드 안에서 세로 스크롤
 *  - 하단 컨트롤 바는 오버레이가 아니라 **화면 아래 고정 영역**이라 카드를 가리지 않는다
 */
@Composable
fun BattleScreen(
    state: TcgGameState,
    onAttackFront: () -> Unit,
    onAttackBack: () -> Unit,
    onNextTurn: () -> Unit,
    onRetreat: (Int) -> Unit,
    onEmergencyRetreat: () -> Unit,
    onReacquire: (Int) -> Unit,
    onRedeploy: (Int) -> Unit,
    onStartDemonLordBattle: () -> Unit,
    onStartFieldBattle: () -> Unit,
    onPairUltimate: () -> Unit,
    onOpenPartyEdit: () -> Unit,
    onBack: () -> Unit,
    // ── 🤖 자동 전투 · ⚔️ 컷인 (2026-11-16) ──
    onAutoStep: () -> Unit = {},
    onToggleAutoBattle: () -> Unit = {},
    onToggleSpeed: () -> Unit = {},
    // ── ⚔ 전투 결과(VICTORY/DEFEAT) 오버레이 (2026-11-17) ──
    onResultPrimary: () -> Unit = {},
    onResultSecondary: () -> Unit = {},
    onResultGrowth: () -> Unit = {},
    onResultDeck: () -> Unit = {},
    onResultLobby: () -> Unit = {},
    audioManager: com.game.tcg.audio.AudioManager? = null
) {
    val deckSize = state.unlockedDeckSize

    // 스테이지 보스전이면 그 보스(중간 보스 170~175 · 대마왕 112~114)의 아트를 적 진영에 세운다
    val stageBossId = state.activeStage?.let { com.game.tcg.core.model.StageCatalog.find(it)?.bossId }
    /**
     * 적 진영 — 2026-11-18 다중 적 개편.
     *
     * 이제 [com.game.tcg.core.model.EnemyUnit] 이 실제 전투 단위다(개별 HP·공격력).
     * 예전처럼 화면만 보기용으로 편성하지 않고 상태의 적 카드를 그대로 그린다.
     */
    val enemyUnits = state.enemyUnits
    val allyCards = remember(state.party, deckSize) { state.party.sortedBy { it.slotIndex } }

    // ── 전투 연출 상태: 중앙 컷인 · 피격 타격 이펙트 ──
    var activeCutIn by remember { mutableStateOf<BattleCutIn?>(null) }
    /** 적이 맞은 순간의 컷인 (피격 플래시·참격·데미지 숫자) */
    var enemyHit by remember { mutableStateOf<BattleCutIn?>(null) }
    /** 아군이 맞은 순간의 컷인 */ var allyHit by remember { mutableStateOf<BattleCutIn?>(null) }
    val enemyImpact = remember { Animatable(0f) }
    val allyImpact = remember { Animatable(0f) }

    var showTactics by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableIntStateOf(-1) }
    /** 진영 탭 — 0 = 적 진영, 1 = 내 진영 */
    var gridTab by remember { mutableIntStateOf(BATTLE_TAB_ENEMY) }

    // ── 앱이 전면에 있을 때만 자동 전투를 돌린다 (백그라운드 전환 시 정지) ──
    val lifecycleOwner = LocalContext.current as? androidx.lifecycle.LifecycleOwner
    var inForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> inForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> inForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        inForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── ⚔️ 공격 1회 타임라인: 컷인 확대 → 타격(플래시·참격·데미지) → 잔상 ──
    val entryTick = remember { state.lastCutIn?.tick ?: 0 }
    LaunchedEffect(state.lastCutIn?.tick) {
        val cut = state.lastCutIn ?: return@LaunchedEffect
        if (cut.tick == entryTick) return@LaunchedEffect
        val speed = state.battleSpeed
        // 공격이 시작되면 맞는 쪽 진영 탭으로 자동 전환 — 타격 연출이 보이도록
        gridTab = if (cut.fromEnemy) BATTLE_TAB_ALLY else BATTLE_TAB_ENEMY
        activeCutIn = cut
        delay(BattleAutoDecor.impactMillis(speed))
        if (cut.fromEnemy) {
            allyHit = cut
        } else {
            enemyHit = cut
            audioManager?.hit()
        }
        delay(BattleAutoDecor.hitHoldMillis(speed))
        enemyHit = null
        allyHit = null
        activeCutIn = null
    }
    LaunchedEffect(enemyHit) {
        if (enemyHit != null) {
            enemyImpact.snapTo(1f)
            enemyImpact.animateTo(0f, tween(BattleAutoDecor.shakeMillis(state.battleSpeed).toInt()))
        }
    }
    LaunchedEffect(allyHit) {
        if (allyHit != null) {
            allyImpact.snapTo(1f)
            allyImpact.animateTo(0f, tween(BattleAutoDecor.shakeMillis(state.battleSpeed).toInt()))
        }
    }

    // ── 🤖 자동 전투 루프 ──
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(state.isAutoBattle, inForeground, showTactics, showLog) {
        if (!state.isAutoBattle || !inForeground || showTactics || showLog) return@LaunchedEffect
        while (isActive) {
            val s = latestState
            if (!s.isAutoBattle) break
            if (BattleAutoPilot.nextAction(s) == BattleAutoPilot.Action.STOP) {
                onAutoStep()
                break
            }
            delay(BattleAutoDecor.stepIntervalMillis(s.battleSpeed))
            if (!latestState.isAutoBattle) break
            onAutoStep()
        }
    }

    val battleOver = state.enemyHp <= 0 || state.isEnemyWipedOut
    val canFight = !battleOver && state.mana.currentMana >= 1 && state.party.any { it.isAlive && it.isFrontRow }

    Box(modifier = Modifier.fillMaxSize()) {
        ArenaBackdrop()

        Column(modifier = Modifier.fillMaxSize()) {
            // ── 상단 타이틀 · 전황 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FIGHT",
                    color = Color.White,
                    fontSize = TcgType.TITLE.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(5.dp)
                        .width(46.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF7C4DFF))
                )
                Spacer(Modifier.weight(1f))
                ArenaPill("턴 ${state.turn}", Color(0xFF00E5FF))
                Spacer(Modifier.width(4.dp))
                ArenaPill("마나 ${state.mana.currentMana}/${state.mana.maxMana}", Color(0xFF64B5F6))
            }

            // ── 🤖 자동 전투 · ⏩ 배속 ──
            AutoBattleBar(
                isAuto = state.isAutoBattle,
                speed = state.battleSpeed,
                hint = if (state.isAutoBattle) {
                    BattleAutoDecor.autoHint(true, state.battleSpeed) + " · " + BattleAutoPilot.nextAction(state).label
                } else {
                    BattleAutoDecor.autoHint(false, state.battleSpeed)
                },
                onToggleAuto = onToggleAutoBattle,
                onToggleSpeed = onToggleSpeed
            )

            // ── 중앙 VS 바 (고정) ──
            VsBar(
                enemyName = state.enemyName,
                enemyAvatarId = enemyUnits.firstOrNull()?.cardId ?: state.enemyCutInCardId,
                enemyPower = BattleLineup.totalDisplayPower(
                    enemyUnits.map {
                        LineupUnit(
                            cardId = it.cardId,
                            name = it.name,
                            element = it.element,
                            atk = it.atk,
                            def = it.maxHp,
                            isBoss = it.isBoss
                        )
                    }
                ),
                enemyHp = state.enemyHp,
                enemyMaxHp = state.enemyMaxHp,
                enemyAlive = state.enemyAliveCount,
                enemyTotal = state.enemyUnitCount,
                enemyThreat = state.enemyThreatPerTurn,
                myAvatarId = state.lobbyHeroId,
                myName = state.lobbyHeroProfile?.displayName ?: state.lobbyHero.name,
                myPower = state.deckDisplayPower,
                myCount = allyCards.size,
                deckSize = deckSize
            )

            // ── 진영 탭 ──
            TcgTabBar(
                tabs = listOf(
                    "적 진영 ${state.enemyAliveCount}/${state.enemyUnitCount}장",
                    "내 진영 ${allyCards.size}/${deckSize}장"
                ),
                selectedIndex = gridTab,
                accent = if (gridTab == BATTLE_TAB_ENEMY) Color(0xFFFF8A80) else Color(0xFF4FC3F7),
                modifier = Modifier.padding(horizontal = 10.dp),
                onSelect = { gridTab = it }
            )
            Spacer(Modifier.height(4.dp))

            // ── 진영 그리드: 3열 × 최대 4행, 2:3 카드, 세로 스크롤 ──
            if (gridTab == BATTLE_TAB_ENEMY) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(CardLayoutRules.GRID_COLUMNS),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    itemsIndexed(items = enemyUnits, key = { index, unit -> "enemy_${index}_${unit.cardId}" }) { index, unit ->
                        val hit = if (index == state.enemyTarget?.slot) enemyHit else null
                        val golden = hit?.let { BattleAutoDecor.isGoldenHit(it) } ?: false
                        val isTarget = unit.isAlive && state.enemyTarget?.slot == index
                        Box(
                            modifier = Modifier
                                .aspectRatio(CardLayoutRules.TILE_ASPECT)
                                .graphicsLayer {
                                    if (index == state.enemyTarget?.slot) {
                                        translationX = BattleAutoDecor.shakeOffset(enemyImpact.value) * 10.dp.toPx()
                                        rotationZ = BattleAutoDecor.shakeOffset(enemyImpact.value) * 2.2f
                                    }
                                }
                        ) {
                            EnemyTile(
                                unit = unit,
                                isTarget = isTarget,
                                onClick = onAttackFront
                            )
                            if (index == state.enemyTarget?.slot) {
                                HitFlashLayer(
                                    decay = enemyImpact.value,
                                    golden = golden,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                HitSparkLayer(
                                    decay = enemyImpact.value,
                                    color = elementColor(hit?.element ?: state.enemyElement),
                                    golden = golden,
                                    modifier = Modifier.fillMaxSize()
                                )
                                hit?.let { cut ->
                                    BattleDamageNumber(
                                        cutIn = cut,
                                        color = if (golden) Color(0xFFFFD54F) else Color(0xFFFFEB3B),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(CardLayoutRules.GRID_COLUMNS),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    itemsIndexed(
                        items = (0 until deckSize).toList(),
                        key = { _, slot -> "ally_$slot" }
                    ) { _, slot ->
                        val member = state.memberAt(slot)
                        val highlighted = activeCutIn?.attackerSlot == slot && activeCutIn?.fromEnemy == false
                        val hit = if (allyHit?.targetSlot == slot) allyHit else null
                        val golden = hit?.let { BattleAutoDecor.isGoldenHit(it) } ?: false
                        Box(
                            modifier = Modifier
                                .aspectRatio(CardLayoutRules.TILE_ASPECT)
                                .graphicsLayer {
                                    if (hit != null) {
                                        translationX = BattleAutoDecor.shakeOffset(allyImpact.value) * 9.dp.toPx()
                                        rotationZ = BattleAutoDecor.shakeOffset(allyImpact.value) * -2f
                                    }
                                }
                        ) {
                            if (member == null) {
                                EmptySlotTile(
                                    label = DeckRules.slotLabel(slot),
                                    hint = "빈 슬롯",
                                    modifier = Modifier.fillMaxSize(),
                                    onClick = onOpenPartyEdit
                                )
                            } else {
                                DuelCardTileFor(
                                    card = member.card,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (highlighted) {
                                                Modifier.border(3.dp, Color(0xFFFFEB3B), RoundedCornerShape(12.dp))
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    highlighted = highlighted,
                                    dimmed = !member.isAlive,
                                    hpRatio = member.hpRatio,
                                    hpText = "${member.hp}",
                                    statusBadge = when {
                                        !member.isAlive -> null
                                        state.combatPenalty.isCharmed -> "[홀림]"
                                        member.isNearDeath -> "위기"
                                        else -> null
                                    },
                                    statusBadgeColor = if (member.isNearDeath) Color(0xFFFFB74D) else TcgColors.Danger,
                                    targetWidthPx = 320,
                                    onClick = {
                                        selectedSlot = if (selectedSlot == slot) -1 else slot
                                        showTactics = true
                                    }
                                )
                                // 전열/후열 표시
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xAA120E1C))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        DeckRules.slotLabel(slot),
                                        color = Color.White,
                                        fontSize = TcgType.TILE_MARK.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (hit != null) {
                                HitFlashLayer(
                                    decay = allyImpact.value,
                                    golden = golden,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                HitSparkLayer(
                                    decay = allyImpact.value,
                                    color = Color(0xFFFF5252),
                                    golden = golden,
                                    modifier = Modifier.fillMaxSize()
                                )
                                BattleDamageNumber(
                                    cutIn = hit,
                                    color = Color(0xFFFF5252),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            // ── 1줄 전투 티커 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xB0100C18))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    state.statusMessage,
                    color = Color(0xFFD9D1F0),
                    fontSize = TcgType.LABEL.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "로그 ▲",
                    color = Color(0xFF00E5FF),
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showLog = true }
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }

            // ── 하단 고정 컨트롤 바 (겹치지 않는 고정 영역) ──
            BattleControlBar(
                turn = state.turn,
                mana = state.mana.currentMana,
                canFight = canFight,
                battleOver = battleOver,
                onBack = onBack,
                onNextTurn = onNextTurn,
                onFight = if (battleOver) onStartFieldBattle else onAttackFront,
                onOpenDeck = onOpenPartyEdit,
                onOpenTactics = { showTactics = true }
            )
        }

        // ── ⚔️ 공격 카드 중앙 확대 컷인 ──
        activeCutIn?.let { cut ->
            BattleCutInOverlay(
                cutIn = cut,
                speed = state.battleSpeed,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── 전술 시트 ──
        if (showTactics) {
            TacticsSheet(
                state = state,
                selectedSlot = selectedSlot,
                onClose = { showTactics = false; selectedSlot = -1 },
                onAttackBack = onAttackBack,
                onPairUltimate = onPairUltimate,
                onNextTurn = onNextTurn,
                onRetreat = onRetreat,
                onEmergencyRetreat = onEmergencyRetreat,
                onRedeploy = onRedeploy,
                onReacquire = onReacquire,
                onStartDemonLordBattle = onStartDemonLordBattle,
                onStartFieldBattle = onStartFieldBattle,
                onOpenDeck = onOpenPartyEdit
            )
        }

        // ── 상세 로그 시트 ──
        if (showLog) {
            LogSheet(state = state, onDismiss = { showLog = false })
        }

        // ── ⚔ VICTORY / DEFEAT 결과 오버레이 (2026-11-17) ──
        state.battleResult?.let { panel ->
            BattleResultOverlay(
                panel = panel,
                onPrimary = onResultPrimary,
                onSecondary = onResultSecondary,
                onGrowth = onResultGrowth,
                onDeck = onResultDeck,
                onLobby = onResultLobby
            )
        }
    }
}

private const val BATTLE_TAB_ENEMY = 0
private const val BATTLE_TAB_ALLY = 1

/* ───────────────────────────── 배경 ───────────────────────────── */

/** 코랄/레드 → 스카이/블루 대각선 분할 아레나 배경 */
@Composable
private fun ArenaBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE8485E), Color(0xFFF0566C), Color(0xFFFF7C93)),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.62f)
            )
        )
        val splitTop = size.height * 0.43f
        val path = Path().apply {
            moveTo(0f, splitTop)
            lineTo(size.width, splitTop - size.height * 0.08f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF37C6F0), Color(0xFF2FA8E8), Color(0xFF1E7ED6)),
                start = androidx.compose.ui.geometry.Offset(size.width, splitTop),
                end = androidx.compose.ui.geometry.Offset(0f, size.height)
            )
        )
        val stripe = 14f
        val stripePath = Path().apply {
            moveTo(0f, splitTop - stripe)
            lineTo(size.width, splitTop - size.height * 0.08f - stripe)
            lineTo(size.width, splitTop - size.height * 0.08f)
            lineTo(0f, splitTop)
            close()
        }
        drawPath(stripePath, Color(0xFFF6F1FF))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .dotPattern(Color.White.copy(alpha = 0.06f), step = 26f)
    )
}

/* ───────────────────────────── 진영 카드 ───────────────────────────── */

/**
 * 적 진영 카드 1장 — 2026-11-18 다중 적 개편.
 *
 * 카드마다 **자기 HP 게지**를 들고 있고, HP 0 이면 격파 표시로 흐려진다.
 * 아군이 지금 때릴 카드([isTarget])에는 「표적」 배지를 붙인다.
 */
@Composable
private fun EnemyTile(
    unit: EnemyUnit,
    isTarget: Boolean,
    onClick: () -> Unit
) {
    val defeated = !unit.isAlive
    Box(modifier = Modifier.fillMaxSize()) {
        DuelCardTile(
            cardId = unit.cardId,
            visual = VisualState.BATTLE,
            element = unit.element,
            starLevel = if (unit.isBoss) 5 else 2,
            power = (unit.atk + unit.maxHp) / 100,
            modifier = Modifier.fillMaxSize(),
            hpRatio = unit.hpRatio,
            dimmed = defeated,
            statusBadge = when {
                defeated -> "격파"
                unit.isBoss -> "BOSS"
                isTarget -> "표적"
                else -> null
            },
            statusBadgeColor = when {
                defeated -> Color(0xFF9E9E9E)
                unit.isBoss -> Color(0xFFFF5252)
                else -> Color(0xFF00E5FF)
            },
            targetWidthPx = 320,
            onClick = onClick
        )
        if (isTarget) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xCC00E5FF))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("⚔", color = Color(0xFF120E1C), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/* ───────────────────────────── VS 바 ───────────────────────────── */

@Composable
private fun VsBar(
    enemyName: String,
    enemyAvatarId: Int,
    enemyPower: Int,
    enemyHp: Int,
    enemyMaxHp: Int,
    enemyAlive: Int,
    enemyTotal: Int,
    enemyThreat: Int,
    myAvatarId: Int,
    myName: String,
    myPower: Int,
    myCount: Int,
    deckSize: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 적 측
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .aspectRatio(CardLayoutRules.TILE_ASPECT)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x66FFFFFF))
                    .border(1.5.dp, Color(0xFFFFEB3B), RoundedCornerShape(7.dp))
            ) {
                CardArtImage(
                    cardId = enemyAvatarId,
                    visual = VisualState.BATTLE,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 160
                )
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    enemyName,
                    color = Color.White,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔", fontSize = 10.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "$enemyPower",
                        color = Color(0xFFFFEB3B),
                        fontSize = TcgType.BODY.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    "적 ${enemyAlive}/${enemyTotal}장 · HP $enemyHp/$enemyMaxHp",
                    color = Color(0xE6FFFFFF),
                    fontSize = TcgType.TILE_MARK.sp,
                    maxLines = 1
                )
                Text(
                    "반격 위협 ${enemyThreat}/턴",
                    color = Color(0xFFFF8A80),
                    fontSize = TcgType.TILE_MARK.sp,
                    maxLines = 1
                )
            }
        }

        // VS 엠블럼
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFFB388FF), Color(0xFF00E5FF)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "VS",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
        }

        // 내 측
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    myName,
                    color = Color.White,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$myCount/$deckSize",
                        color = Color(0xE6FFFFFF),
                        fontSize = TcgType.TILE_MARK.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("⚔", fontSize = 10.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "$myPower",
                        color = Color(0xFF00E5FF),
                        fontSize = TcgType.BODY.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .aspectRatio(CardLayoutRules.TILE_ASPECT)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x66FFFFFF))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(7.dp))
            ) {
                CardArtImage(
                    cardId = myAvatarId,
                    visual = VisualState.BATTLE,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 160
                )
            }
        }
    }
}

/* ───────────────────────── 컨트롤 바 ───────────────────────── */

/**
 * 하단 고정 컨트롤 바 — 예전에는 `Box(fillMaxSize)` 오버레이라 아군 카드를 가렸다.
 * 이제는 화면 맨 아래 **고정 영역**으로 배치되어 카드와 겹치지 않는다.
 */
@Composable
private fun BattleControlBar(
    turn: Int,
    mana: Int,
    canFight: Boolean,
    battleOver: Boolean,
    onBack: () -> Unit,
    onNextTurn: () -> Unit,
    onFight: () -> Unit,
    onOpenDeck: () -> Unit,
    onOpenTactics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF0050712), Color(0xFF050712), Color(0xFF050712))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoundAction(glyph = "↩", label = "로비", ringColor = Color(0xFFB0BEC5), onClick = onBack)
            RoundAction(
                glyph = "⏭",
                label = "턴 $turn",
                ringColor = Color(0xFF4FC3F7),
                enabled = !battleOver,
                onClick = onNextTurn
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (canFight || battleOver) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFB74D), Color(0xFFFF8A3D), Color(0xFFFF7043))
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF6D6A7A), Color(0xFF4E4B5A)))
                        }
                    )
                    .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(50))
                    .clickable(enabled = canFight || battleOver) { onFight() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (battleOver) "NEXT" else "FIGHT",
                        color = Color(0xFF2B1600),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        when {
                            battleOver -> "새 원정 시작"
                            canFight -> "전열 공격 · 마나 1"
                            mana < 1 -> "마나 부족 — 턴 진행"
                            else -> "공격 가능한 전열 없음"
                        },
                        color = Color(0xE62B1600),
                        fontSize = TcgType.TILE_MARK.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            RoundAction(glyph = "🃏", label = "덱", ringColor = Color(0xFFB388FF), onClick = onOpenDeck)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .clickable { onOpenTactics() }
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "전술 지휘 · 후열 지원 · 페어 오의 · 후퇴 · 마왕전",
                color = Color(0xE6FFFFFF),
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RoundAction(
    glyph: String,
    label: String,
    ringColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xB3120E1E))
                .border(2.dp, if (enabled) ringColor else Color(0x55FFFFFF), CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, fontSize = 18.sp)
        }
        Text(
            label,
            color = Color(0xE6FFFFFF),
            fontSize = TcgType.TILE_MARK.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ArenaPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x66120E1E))
            .border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = Color.White, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
    }
}

/* ───────────────────── 시트 (전술/로그) ───────────────────── */

@Composable
private fun LogSheet(state: TcgGameState, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 90.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xF0120E1C))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("상세 전투 로그", color = Color.White, fontSize = TcgType.SECTION.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "닫기 ✕",
                color = Color(0xFF00E5FF),
                fontSize = TcgType.BODY_SMALL.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onDismiss() }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            state.battleLog.takeLast(60).forEach { line ->
                Text("· $line", color = Color(0xFFD9D1F0), fontSize = TcgType.BODY_SMALL.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun TacticsSheet(
    state: TcgGameState,
    selectedSlot: Int,
    onClose: () -> Unit,
    onAttackBack: () -> Unit,
    onPairUltimate: () -> Unit,
    onNextTurn: () -> Unit,
    onRetreat: (Int) -> Unit,
    onEmergencyRetreat: () -> Unit,
    onRedeploy: (Int) -> Unit,
    onReacquire: (Int) -> Unit,
    onStartDemonLordBattle: () -> Unit,
    onStartFieldBattle: () -> Unit,
    onOpenDeck: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC05030A))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color(0xFF15101F))
                .clickable { }
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("전술 지휘", color = Color.White, fontSize = TcgType.SECTION.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Pill("턴 ${state.turn}", Color(0xFF00E5FF))
                Pill("마나 ${state.mana.currentMana}/${state.mana.maxMana}", Color(0xFF64B5F6))
                Spacer(Modifier.weight(1f))
                Text(
                    "닫기 ✕",
                    color = Color(0xFF00E5FF),
                    fontSize = TcgType.BODY_SMALL.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onClose() }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            if (selectedSlot >= 0) {
                val member = state.memberAt(selectedSlot)
                if (member != null) {
                    ActionTile(
                        label = "🏃 ${member.card.name} 전술 후퇴 (${DeckRules.slotLabel(selectedSlot)})",
                        hint = "턴당 1회 · 마나 ${if (state.isDemonLordBattle) 2 else 1} · 손패/대기열로 철수",
                        accent = TcgColors.Gold,
                        enabled = member.isAlive,
                        onClick = { onRetreat(selectedSlot); onClose() }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "🏹 후열 지원 사격",
                        hint = "마나 1 · 후열 생존 카드가 공격",
                        accent = TcgColors.Mana,
                        onClick = { onAttackBack(); onClose() }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "⏭ 턴 진행",
                        hint = "적 반격 · 마나 회복",
                        accent = TcgColors.Heal,
                        onClick = { onNextTurn(); onClose() }
                    )
                }
            }

            ActionTile(
                label = "⚡ 페어 얼티밋 오의",
                hint = "생존 ${state.aliveSurvivorCount}명 · 마나 ${state.mana.currentMana}/${TcgGameState.PAIR_ULTIMATE_MANA} · " +
                    if (state.pairUltimateUsedThisBattle) "이번 전투 사용 완료" else "발동 가능",
                accent = TcgColors.Gold,
                enabled = state.canActivatePairUltimate && state.enemyHp > 0,
                onClick = { onPairUltimate(); onClose() }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = "🏳 파티 긴급 철수",
                        hint = "전리품 포기 · 카드 상실 없음",
                        accent = TcgColors.Danger,
                        onClick = { onEmergencyRetreat(); onClose() }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ActionTile(
                        label = if (state.isDemonLordBattle) "🐺 마수 원정" else "👑 마왕전 개시",
                        hint = if (state.isDemonLordBattle) "일반 원정으로 전환" else "마왕 릴리스 HP 25000",
                        accent = TcgColors.Abyss,
                        onClick = {
                            if (state.isDemonLordBattle) onStartFieldBattle() else onStartDemonLordBattle()
                            onClose()
                        }
                    )
                }
            }

            ActionTile(
                label = "🃏 덱 편성 열기 (${state.party.size}/${state.unlockedDeckSize}장)",
                hint = "전열 3 · 후열 대기 · Lv.${state.playerLevel}",
                accent = TcgColors.Violet,
                onClick = { onOpenDeck(); onClose() }
            )

            if (state.battleReserve.isNotEmpty()) {
                Text(
                    "후퇴 대기열 ${state.battleReserve.size}장 — 빈 슬롯에 재투입",
                    color = TcgColors.Muted,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.SemiBold
                )
                state.battleReserve.take(4).forEach { card ->
                    ActionTile(
                        label = "↩ 재투입 · ${card.name}",
                        hint = "가장 앞선 빈 슬롯으로",
                        accent = TcgColors.Heal,
                        onClick = { onRedeploy(card.id); onClose() }
                    )
                }
            }

            if (state.reincarnationPool.isNotEmpty()) {
                Text(
                    "로그라이크 상실 카드 ${state.reincarnationPool.size}장 — 재획득 필요",
                    color = TcgColors.Danger,
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.SemiBold
                )
                state.reincarnationPool.take(3).forEach { lost ->
                    ActionTile(
                        label = "✨ 재획득 · ${lost.name}",
                        hint = "${lost.reincarnationCount}회차 환생 대기 · 초기 상태 복귀",
                        accent = TcgColors.Danger,
                        onClick = { onReacquire(lost.templateId); onClose() }
                    )
                }
            }

            Text(
                "적 진영 ${state.enemyUnitCount}장 — 전열부터 한 장씩 격파해야 승리 · 카드 공격력 ${state.enemyAtk} · " +
                    "적 속성 ${state.enemyElement.displayName}\n" +
                    "아군 보정 방어 +${state.defenseBonusPercent}% / 급소 +${state.intelBonusPercent}%",
                color = TcgColors.Muted,
                fontSize = TcgType.LABEL.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** 적 속성 표시용 (테스트/미리보기에서 직접 쓰는 헬퍼) */
fun enemyElementColor(element: Element): Color = elementColor(element)

/** 카드 전투력 표기 (다른 화면에서 재사용) */
fun cardPowerLabel(cardId: Int): String {
    val card = TcgGameViewModel.characterCard(cardId)
    return "${DeckAutoBuilder.displayPower(card)}"
}
