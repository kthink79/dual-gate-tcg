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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.game.tcg.core.model.LineupUnit
import com.game.tcg.core.model.VisualState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * ⚔️ 이계 전투 — 비주얼 VS 아레나 (2026-11-16 개편)
 *
 * 타 TCG `FIGHT` 화면 레퍼런스의 구도를 따른다.
 *  - 상단 적 진영: 코랄/레드 그라데이션 위 3열 × 3~4행 보스+수하 카드 그리드
 *  - 중앙: 좌(적) 아바타·이름·총 전투력 | VS 엠블럼 | 우(나) 아바타·이름·총 전투력
 *  - 하단 내 진영: 스카이/블루 그라데이션 위 출전 덱(9~12장) 3열 그리드 — HP 바·피격 팝업·[홀림] 배지
 *  - 최하단 고정 컨트롤 바: 뒤로가기 · 턴 진행 · 큰 FIGHT 필 · 덱 편성
 *
 * 장문 로그는 1줄 티커로 접고, 필요할 때만 「로그」 버튼으로 펼친다.
 * 모든 부가 지령(후열 지원·페어 오의·후퇴·긴급 철수·마왕전)은 「전술」 시트로 모았다.
 *
 * 2026-11-16 사용자 요청 반영:
 *  - 🤖 **기본 자동 전투** — 전투에 들어가면 자동으로 공격·턴이 돌고, 상단 토글로 끈다
 *  - ⚔️ **공격 카드 중앙 확대 컷인** — 공격자 카드가 중앙에 확대되며 속성 빛 테두리·배너를 띄우고,
 *    피격 카드에는 흔들림·타격 플래시·참격·떠오르는 데미지 숫자가 함께 터진다
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
    audioManager: com.game.tcg.audio.AudioManager? = null
) {
    val deckSize = state.unlockedDeckSize
    val rows = ((deckSize + 2) / 3).coerceIn(3, 4)
    val tileHeight = if (rows >= 4) 70.dp else 82.dp

    // 스테이지 보스전이면 그 보스(중간 보스 170~175 · 대마왕 112~114)의 아트를 적 진영에 세운다
    val stageBossId = state.activeStage?.let { com.game.tcg.core.model.StageCatalog.find(it)?.bossId }
    val enemyUnits = remember(state.enemyName, state.enemyElement, deckSize, state.isDemonLordBattle, stageBossId) {
        BattleLineup.enemyLineup(
            enemyElement = state.enemyElement,
            enemyName = state.enemyName,
            size = deckSize,
            isDemonLordBattle = state.isDemonLordBattle,
            bossCardId = stageBossId
        )
    }
    val allyCards = remember(state.party, deckSize) { state.party.sortedBy { it.slotIndex } }

    // ── 전투 연출 상태: 중앙 컷인 · 피격 타격 이펙트 ──
    var activeCutIn by remember { mutableStateOf<BattleCutIn?>(null) }
    /** 적이 맞은 순간의 컷인 (피격 플래시·참격·데미지 숫자) */
    var enemyHit by remember { mutableStateOf<BattleCutIn?>(null) }
    /** 아군이 맞은 순간의 컷인 */
    var allyHit by remember { mutableStateOf<BattleCutIn?>(null) }
    val enemyImpact = remember { Animatable(0f) }
    val allyImpact = remember { Animatable(0f) }

    var showTactics by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf(-1) }

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
    LaunchedEffect(state.lastCutIn?.tick) {
        val cut = state.lastCutIn ?: return@LaunchedEffect
        val speed = state.battleSpeed
        activeCutIn = cut
        delay(BattleAutoDecor.impactMillis(speed))
        if (cut.fromEnemy) {
            allyHit = cut
        } else {
            enemyHit = cut
            // 타격 순간에 맞춰 hit SFX (휘두르는 attack 은 상태 틱에서 이미 울렸다)
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

    // ── 🤖 자동 전투 루프 — 컷인 시간을 포함한 템포로 다음 행동을 진행 ──
    // 전술/로그 시트가 열려 있거나 앱이 백그라운드면 멈추고, 승리·전멸에서도 끝난다.
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(state.isAutoBattle, inForeground, showTactics, showLog) {
        if (!state.isAutoBattle || !inForeground || showTactics || showLog) return@LaunchedEffect
        while (isActive) {
            val s = latestState
            if (!s.isAutoBattle) break
            if (BattleAutoPilot.nextAction(s) == BattleAutoPilot.Action.STOP) {
                // 승리·전멸 — ViewModel 이 자동 전투를 끄고 루프를 빠져나온다
                onAutoStep()
                break
            }
            delay(BattleAutoDecor.stepIntervalMillis(s.battleSpeed))
            if (!latestState.isAutoBattle) break
            onAutoStep()
        }
    }

    val enemyHpRatio = if (state.enemyMaxHp <= 0) 0f else state.enemyHp.toFloat() / state.enemyMaxHp
    val battleOver = state.enemyHp <= 0
    val canFight = !battleOver && state.mana.currentMana >= 1 && state.party.any { it.isAlive && it.isFrontRow }

    Box(modifier = Modifier.fillMaxSize()) {
        ArenaBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 98.dp)
        ) {
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
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(46.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF7C4DFF))
                )
                Spacer(Modifier.weight(1f))
                ArenaPill("턴 ${state.turn}", Color(0xFF00E5FF))
                Spacer(Modifier.width(4.dp))
                ArenaPill("마나 ${state.mana.currentMana}/${state.mana.maxMana}", Color(0xFF64B5F6))
                Spacer(Modifier.width(4.dp))
                ArenaPill(
                    if (state.isDemonLordBattle) "마왕전" else if (state.isChapter2BossBattle) "전초전" else "원정",
                    if (state.isDemonLordBattle) TcgColors.Abyss else Color(0xFFFFB74D)
                )
            }

            // ── 🤖 자동 전투 · ⏩ 배속 바 ──
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

            // ── 적 진영 그리드 ──
            GridSection(
                caption = "${state.enemyName} · 적 진영 ${enemyUnits.size}장",
                accent = Color(0xFFFF6E6E)
            ) {
                enemyUnits.chunked(3).forEachIndexed { rowIndex, rowUnits ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(tileHeight),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowUnits.forEachIndexed { colIndex, unit ->
                            val index = rowIndex * 3 + colIndex
                            val hit = if (index == 0) enemyHit else null
                            val golden = hit?.let { BattleAutoDecor.isGoldenHit(it) } ?: false
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    // 피격 흔들림 — 적 보스 카드가 맞은 순간 좌우로 떨린다
                                    .graphicsLayer {
                                        if (index == 0) {
                                            translationX = BattleAutoDecor.shakeOffset(enemyImpact.value) * 10.dp.toPx()
                                            rotationZ = BattleAutoDecor.shakeOffset(enemyImpact.value) * 2.2f
                                        }
                                    }
                            ) {
                                EnemyTile(
                                    unit = unit,
                                    hpRatio = enemyHpRatio,
                                    isBoss = index == 0,
                                    defeated = battleOver,
                                    onClick = onAttackFront
                                )
                                if (index == 0) {
                                    // 타격 플래시 · 참격/스파크 · 떠오르는 데미지 숫자
                                    HitFlashLayer(
                                        decay = enemyImpact.value,
                                        golden = golden,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
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
                        repeat(3 - rowUnits.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // ── 중앙 VS 바 ──
            VsBar(
                enemyName = state.enemyName,
                enemyAvatarId = enemyUnits.first().cardId,
                enemyPower = BattleLineup.totalDisplayPower(enemyUnits),
                enemyHp = state.enemyHp,
                enemyMaxHp = state.enemyMaxHp,
                myAvatarId = state.lobbyHeroId,
                myName = state.lobbyHeroProfile?.displayName ?: state.lobbyHero.name,
                myPower = state.deckDisplayPower,
                myCount = allyCards.size,
                deckSize = deckSize
            )

            // ── 내 진영 그리드 ──
            GridSection(
                caption = "내 진영 · ${allyCards.size}/${deckSize}장" +
                        (if (state.combatPenalty.isCharmed) " · [홀림] ${state.combatPenalty.totalStacks}" else ""),
                accent = Color(0xFF4FC3F7)
            ) {
                (0 until deckSize).chunked(3).forEachIndexed { _, slotRow ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(tileHeight),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        slotRow.forEach { slot ->
                            val member = state.memberAt(slot)
                            val highlighted = activeCutIn?.attackerSlot == slot && activeCutIn?.fromEnemy == false
                            val hit = if (allyHit?.targetSlot == slot) allyHit else null
                            val golden = hit?.let { BattleAutoDecor.isGoldenHit(it) } ?: false
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    // 적 반격에 맞은 아군 카드 흔들림
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
                                                    Modifier.border(
                                                        3.dp,
                                                        Color(0xFFFFEB3B),
                                                        RoundedCornerShape(10.dp)
                                                    )
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
                                        targetWidthPx = 240,
                                        onClick = {
                                            selectedSlot = if (selectedSlot == slot) -1 else slot
                                            showTactics = true
                                        }
                                    )
                                    // 전열/후열 표시
                                    Text(
                                        DeckRules.slotLabel(slot),
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 2.dp)
                                    )
                                }
                                if (hit != null) {
                                    HitFlashLayer(
                                        decay = allyImpact.value,
                                        golden = golden,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
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
                        repeat(3 - slotRow.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // ── 1줄 전투 티커 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xB0100C18))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(state.statusMessage, color = Color(0xFFD9D1F0), fontSize = 9.sp, maxLines = 1, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                Text(
                    "로그 ▲",
                    color = Color(0xFF00E5FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showLog = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // ── 하단 고정 컨트롤 바 ──
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

        // ── ⚔️ 공격 카드 중앙 확대 컷인 (터치를 가로막지 않는 중앙 오버레이) ──
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
    }
}

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
        // 대각선 화이트 스트라이프
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
    // 도트 질감
    Box(
        modifier = Modifier
            .fillMaxSize()
            .dotPattern(Color.White.copy(alpha = 0.06f), step = 26f)
    )
}

/* ───────────────────────────── 그리드 ───────────────────────────── */

@Composable
private fun GridSection(
    caption: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 11.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(6.dp))
            Text(caption, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun EnemyTile(
    unit: LineupUnit,
    hpRatio: Float,
    isBoss: Boolean,
    defeated: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DuelCardTile(
            cardId = unit.cardId,
            visual = VisualState.BATTLE,
            element = unit.element,
            starLevel = if (isBoss) 5 else 2,
            power = unit.displayPower,
            modifier = Modifier.fillMaxSize(),
            hpRatio = hpRatio,
            dimmed = defeated,
            statusBadge = if (isBoss) "BOSS" else null,
            statusBadgeColor = Color(0xFFFF5252),
            artAlignment = Alignment.TopCenter,
            targetWidthPx = 240,
            onClick = onClick
        )
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
    myAvatarId: Int,
    myName: String,
    myPower: Int,
    myCount: Int,
    deckSize: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 적 측
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66FFFFFF))
                    .border(1.5.dp, Color(0xFFFFEB3B), RoundedCornerShape(8.dp))
            ) {
                CardArtImage(
                    cardId = enemyAvatarId,
                    visual = VisualState.BATTLE,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter,
                    targetWidthPx = 160
                )
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(enemyName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔", fontSize = 9.sp)
                    Spacer(Modifier.width(2.dp))
                    Text("$enemyPower", color = Color(0xFFFFEB3B), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("HP $enemyHp/$enemyMaxHp", color = Color(0xCCFFFFFF), fontSize = 7.sp)
                }
            }
        }

        // VS 엠블럼
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF7C4DFF), Color(0xFFB388FF), Color(0xFF00E5FF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "VS",
                color = Color.White,
                fontSize = 24.sp,
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
                Text(myName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$myCount/$deckSize", color = Color(0xCCFFFFFF), fontSize = 7.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("⚔", fontSize = 9.sp)
                    Spacer(Modifier.width(2.dp))
                    Text("$myPower", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66FFFFFF))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
            ) {
                CardArtImage(
                    cardId = myAvatarId,
                    visual = VisualState.BATTLE,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter,
                    targetWidthPx = 160
                )
            }
        }
    }
}

/* ───────────────────────── 컨트롤 바 ───────────────────────── */

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
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xE6050712), Color(0xF2050712))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 뒤로가기
                RoundAction(
                    glyph = "↩",
                    label = "로비",
                    ringColor = Color(0xFFB0BEC5),
                    onClick = onBack
                )
                // 턴 진행
                RoundAction(
                    glyph = "⏭",
                    label = "턴 $turn",
                    ringColor = Color(0xFF4FC3F7),
                    enabled = !battleOver,
                    onClick = onNextTurn
                )
                // FIGHT 필
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
                            color = Color(0xCC2B1600),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // 덱 편성
                RoundAction(
                    glyph = "🃏",
                    label = "덱",
                    ringColor = Color(0xFFB388FF),
                    onClick = onOpenDeck
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .clickable { onOpenTactics() }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "전술 지휘 · 후열 지원 · 페어 오의 · 후퇴 · 마왕전",
                    color = Color(0xCCFFFFFF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
            Text(glyph, fontSize = 17.sp)
        }
        Text(label, color = Color(0xCCFFFFFF), fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArenaPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x66120E1E))
            .border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
            Text("상세 전투 로그", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "닫기 ✕",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onDismiss() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.battleLog.takeLast(60).forEach { line ->
                Text("· $line", color = Color(0xFFD9D1F0), fontSize = 10.sp, lineHeight = 15.sp)
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
                Text("전술 지휘", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Pill("턴 ${state.turn}", Color(0xFF00E5FF))
                Pill("마나 ${state.mana.currentMana}/${state.mana.maxMana}", Color(0xFF64B5F6))
                Spacer(Modifier.weight(1f))
                Text(
                    "닫기 ✕",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onClose() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
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
                        onClick = { if (state.isDemonLordBattle) onStartFieldBattle() else onStartDemonLordBattle(); onClose() }
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
                    fontSize = 10.sp,
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
                    fontSize = 10.sp,
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
                "적 속성 ${state.enemyElement.displayName} · 공격력 ${state.enemyAtk} · " +
                        "아군 보정 방어 +${state.defenseBonusPercent}% / 급소 +${state.intelBonusPercent}%",
                color = TcgColors.Muted,
                fontSize = 9.sp
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
