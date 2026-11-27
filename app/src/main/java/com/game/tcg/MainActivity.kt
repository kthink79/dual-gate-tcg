package com.game.tcg

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.DiamondShopEngine
import com.game.tcg.core.engine.IntroStoryEngine
import com.game.tcg.core.model.VisualState
import com.game.tcg.audio.AudioManager
import com.game.tcg.audio.ApplyBgmScene
import com.game.tcg.audio.AudioScene
import com.game.tcg.audio.SfxEffect
import com.game.tcg.audio.rememberAudioManager
import com.game.tcg.ui.BattleScreen
import com.game.tcg.ui.CardArtImage
import com.game.tcg.ui.CollectionScreen
import com.game.tcg.ui.DeckBuilderScreen
import com.game.tcg.ui.DiamondShopOverlay
import com.game.tcg.ui.ExplorationScreen
import com.game.tcg.ui.GrowthScreen
import com.game.tcg.ui.IntroStoryScreen
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.MainLobbyScreen
import com.game.tcg.ui.PackShopScreen
import com.game.tcg.ui.Pill
import com.game.tcg.ui.RealWorldScreen
import com.game.tcg.ui.ResponsiveLayout
import com.game.tcg.ui.ScenarioScreen
import com.game.tcg.ui.StageScreen
import com.game.tcg.ui.TcgColors
import com.game.tcg.ui.TcgGameViewModel
import com.game.tcg.ui.TcgType
import com.game.tcg.persistence.FileSaveStore
import com.game.tcg.persistence.SaveStore
import java.io.File

/**
 * 듀얼 게이트 — 이계의 문
 * 메인 로비(캐릭터 중심) + 좌·우측 측면 서브메뉴 6종
 *  - 좌측(이세계·모험): 이세계 접속 · 이계 전투 · 탐색
 *  - 우측(현실·관리): 현실 소환 · 캐릭터 관리 · 서약·덱
 *
 * 앱을 처음 켜면 도입부 스토리(4장면 컷신)가 로비 위에 겹쳐 재생되고,
 * 마지막 장면·SKIP 이후 페이드아웃하며 로비로 자연스럽게 이어진다.
 */
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TcgGameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 인트로를 한 번 본 뒤에는 앱을 다시 켜도 로비에서 바로 시작한다.
        val introSeen = getSharedPreferences(IntroStoryEngine.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(IntroStoryEngine.PREF_INTRO_SEEN, false)
        // 진행 상태 자동 저장 — 앱 내부 저장소(filesDir)의 세이브 파일에서 복원하고,
        // 상태가 바뀔 때마다 그 파일에 다시 쓴다.
        viewModel = TcgGameViewModel(
            introAlreadySeen = introSeen,
            saveStore = FileSaveStore(File(filesDir, SaveStore.SAVE_FILE_NAME))
        )
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = TcgColors.Violet,
                    background = TcgColors.Background,
                    surface = TcgColors.Surface,
                    onSurface = TcgColors.Text,
                    onBackground = TcgColors.Text
                )
            ) {
                TcgApp(viewModel = viewModel)
            }
        }
    }

    /** 화면을 떠날 때 마지막 진행 상태를 확정 저장한다 (평소엔 상태가 바뀔 때마다 자동 저장) */
    override fun onStop() {
        viewModel.saveNow()
        super.onStop()
    }
}

@Composable
fun TcgApp(viewModel: TcgGameViewModel) {
    val state by viewModel.uiState.collectAsState()
    val screen = state.currentScreen
    val isIntroPlaying = state.isIntroPlaying
    val context = LocalContext.current

    // ── 오디오: BGM 루프 재생기 + 생명주기 연동 + SFX 퍼사드 ──
    val audio = rememberAudioManager()

    // 화면 상태 → BGM 장면 (인트로 / 로비계 / 일반 전투 / 보스전)
    ApplyBgmScene(audio, viewModel.audioSceneOf(state))

    // 인트로를 끝까지 보거나 SKIP 했다면 다음 실행부터는 재생하지 않는다.
    LaunchedEffect(state.introStory.hasSeenIntro) {
        if (state.introStory.hasSeenIntro) {
            context.getSharedPreferences(IntroStoryEngine.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(IntroStoryEngine.PREF_INTRO_SEEN, true)
                .apply()
        }
    }

    // Android 시스템 뒤로가기 → 인트로 중이면 SKIP, 개봉 연출/정보 패널이면 닫기,
    // 그 외에는 메인 로비로 복귀
    BackHandler(
        enabled = isIntroPlaying || state.packOpening.isActive || state.isInfoPanelOpen ||
            state.isDiamondShopOpen || screen != LobbyDestination.LOBBY
    ) {
        audio.click()
        when {
            isIntroPlaying -> viewModel.skipIntro()
            state.isDiamondShopOpen -> viewModel.closeDiamondShop()
            state.packOpening.isActive -> viewModel.closePackOpening()
            state.isInfoPanelOpen -> viewModel.closeInfoPanel()
            else -> viewModel.backToLobby()
        }
    }

    // 2026-11-21 태블릿 가로 대응 — 가로에서는 **높이에 맞춘** 2:3 세로 프레임 안에 게임을 담는다.
    HeightFitGameFrame(
        backdropCardId = state.lobbyHeroId,
        backdropVisual = state.lobbyVisual
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // 전투 화면은 자체 티커·컨트롤 바를 쓰므로 하단 상태 바를 겹치지 않게 숨긴다.
                if (screen != LobbyDestination.BATTLE) TcgStatusBar(message = state.statusMessage)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 전투 · 덱 편성은 자체 전체 화면 헤더(FIGHT / DECK)를 쓰므로 상단 복귀 바를 겹치지 않게 한다.
                if (screen != LobbyDestination.LOBBY && screen != LobbyDestination.BATTLE && screen != LobbyDestination.DECK) {
                    LobbyBackBar(
                        screen = screen,
                        gold = state.gold,
                        gems = state.gems,
                        audio = audio,
                        audioSettings = state.audioSettings,
                        onBack = { audio.click(); viewModel.backToLobby() }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        LobbyDestination.LOBBY -> MainLobbyScreen(
                            state = state,
                            audioManager = audio,
                            audioSettings = state.audioSettings,
                            onOpenScreen = { destination -> audio.click(); viewModel.openScreen(destination) },
                            onSelectHero = { id -> audio.click(); viewModel.selectLobbyHero(id) },
                            onCycleHero = { direction -> audio.click(); viewModel.cycleLobbyHero(direction) },
                            onSelectVisual = { visual -> audio.click(); viewModel.selectLobbyVisual(visual) },
                            onReplayIntro = { audio.click(); viewModel.replayIntro() },
                            onResetGame = { audio.click(); viewModel.resetGame() },
                            onOpenInfo = { audio.click(); viewModel.openInfoPanel() },
                            onCloseInfo = { audio.click(); viewModel.closeInfoPanel() },
                            // 🔊 아이콘·정보 패널의 BGM/SFX 토글 — 재생기와 화면 상태를 함께 맞춘다
                            onToggleBgm = { syncAudio(audio, viewModel) { it.toggleBgm() } },
                            onToggleSfx = { syncAudio(audio, viewModel) { it.toggleSfx() } }
                        )
                        LobbyDestination.SCENARIO -> ScenarioScreen(
                            state = state,
                            onNextStep = { viewModel.advanceStory() },
                            onPrevStep = { viewModel.previousStory() },
                            onChooseEventA = { talk -> viewModel.chooseEventA(talk) },
                            onChooseEventB = { fight -> viewModel.chooseEventB(fight) },
                            onResolveTrustRift = { choice -> viewModel.resolveTrustRift(choice) },
                            onOpenChapter2 = { viewModel.openChapter2Prelude() },
                            onStartChapter2Boss = { viewModel.startChapter2BossBattle() }
                        )
                        LobbyDestination.BATTLE -> BattleScreen(
                            state = state,
                            onAttackFront = { viewModel.attackFront() },
                            onAttackBack = { viewModel.attackBack() },
                            onNextTurn = { viewModel.nextTurn() },
                            onRetreat = { slot -> viewModel.retreat(slot) },
                            onEmergencyRetreat = { viewModel.emergencyRetreat() },
                            onReacquire = { id -> viewModel.reacquire(id) },
                            onRedeploy = { id -> viewModel.redeploy(id) },
                            onStartDemonLordBattle = { viewModel.startDemonLordBattle() },
                            onStartFieldBattle = { viewModel.startFieldBattle() },
                            onPairUltimate = { viewModel.activatePairUltimate() },
                            onOpenPartyEdit = { viewModel.openScreen(LobbyDestination.DECK) },
                            onBack = { viewModel.backToLobby() },
                            // 🤖 자동 전투 · ⏩ 배속 · ⚔️ 컷인 타격음
                            onAutoStep = { viewModel.autoBattleStep() },
                            onToggleAutoBattle = { audio.click(); viewModel.toggleAutoBattle() },
                            onToggleSpeed = { audio.click(); viewModel.toggleBattleSpeed() },
                            // ⚔ VICTORY / DEFEAT 결과 오버레이 버튼
                            onResultPrimary = {
                                audio.click()
                                val panel = viewModel.uiState.value.battleResult
                                if (panel?.isVictory == true) viewModel.battleResultNextStage() else viewModel.retryStageFromResult()
                            },
                            onResultSecondary = {
                                audio.click()
                                val panel = viewModel.uiState.value.battleResult
                                if (panel?.isVictory == true) viewModel.battleResultToLobby() else viewModel.battleResultToDeck()
                            },
                            onResultGrowth = { audio.click(); viewModel.battleResultToGrowth() },
                            onResultDeck = { audio.click(); viewModel.battleResultToDeck() },
                            onResultLobby = { audio.click(); viewModel.battleResultToLobby() },
                            audioManager = audio
                        )
                        LobbyDestination.EXPLORATION -> ExplorationScreen(
                            state = state,
                            onExplore = { mode -> viewModel.explore(mode) },
                            onOpenBattle = { viewModel.openScreen(LobbyDestination.BATTLE) },
                            onOpenCollection = { viewModel.openScreen(LobbyDestination.COLLECTION) },
                            onOpenRealWorld = { viewModel.openScreen(LobbyDestination.REAL_WORLD) }
                        )
                        LobbyDestination.STAGE -> StageScreen(
                            state = state,
                            onSelectStage = { number -> viewModel.selectStage(number) },
                            onStartStage = { number -> viewModel.startStageBattle(number) },
                            onOpenDeck = { viewModel.openScreen(LobbyDestination.DECK) },
                            onOpenGrowth = { viewModel.openScreen(LobbyDestination.GROWTH) },
                            onOpenShop = { viewModel.openScreen(LobbyDestination.SHOP) },
                            onReclaimLost = { audio.click(); viewModel.reclaimAllLostCards() },
                            onClaimSupply = { audio.click(); viewModel.claimEmergencySupply() }
                        )
                        LobbyDestination.SHOP -> PackShopScreen(
                            state = state,
                            onOpenPack = { packId -> viewModel.openPack(packId) },
                            onOpenDiamondShop = { audio.click(); viewModel.openDiamondShop() },
                            onOpenGrowth = { viewModel.openScreen(LobbyDestination.GROWTH) },
                            onOpenCollection = { viewModel.openScreen(LobbyDestination.COLLECTION) },
                            onTearPack = { viewModel.tearPackSeal() },
                            onRevealNextDraw = { viewModel.revealNextPackDraw() },
                            onRevealAllDraws = { viewModel.revealAllPackDraws() },
                            onCloseOpening = { viewModel.closePackOpening() },
                            onReplayOpening = { viewModel.replayPackOpening() },
                            onSfx = { effect -> audio.playSfx(effect) },
                            // 🎬 등장 영상 재생 동안 BGM 일시정지 → 종료·스킵 시 복구
                            onVideoStart = { audio.suspendBgmForVideo() },
                            onVideoEnd = { audio.resumeBgmAfterVideo() }
                        )
                        LobbyDestination.GROWTH -> GrowthScreen(
                            state = state,
                            onSelectCard = { id -> viewModel.selectGrowthCard(id) },
                            onUpgrade = { id -> viewModel.upgradeCard(id) },
                            onCraft = { id -> viewModel.craftCharacter(id) },
                            onTranscend = { id -> viewModel.transcendCard(id) },
                            onBuyEquipment = { itemId -> viewModel.buyEquipment(itemId) },
                            onEquip = { uid, cardId -> viewModel.equipItem(uid, cardId) },
                            onUnequip = { uid -> viewModel.unequipItem(uid) },
                            onOpenShop = { viewModel.openScreen(LobbyDestination.SHOP) },
                            onOpenStage = { viewModel.openScreen(LobbyDestination.STAGE) }
                        )
                        LobbyDestination.REAL_WORLD -> RealWorldScreen(
                            state = state,
                            onInteract = { activity -> viewModel.interactRealWorld(activity) },
                            onBondMemoryPartner = { id -> viewModel.bondMemoryResetPartner(id) },
                            onSummon = { id -> viewModel.summonToReality(id) },
                            onSelectSummonCard = { id -> viewModel.selectSummonCard(id) },
                            // 2026-11-27 v0.6.0 — 💎 지정 소환 (다이아 교환소 1번 탭으로 바로)
                            onOpenDiamondShop = {
                                audio.click()
                                viewModel.openDiamondShop(DiamondShopEngine.Tab.TARGET_SUMMON.ordinal)
                            }
                        )
                        LobbyDestination.COLLECTION -> CollectionScreen(
                            state = state,
                            onSelectCard = { id -> viewModel.selectCard(id) },
                            onSelectVisual = { visual -> viewModel.selectVisual(visual) },
                            onGaze = { viewModel.gazeAtDemonLord() },
                            onPurify = { viewModel.purify() },
                            onLimitBreak = { viewModel.limitBreakSelected() },
                            onCraftNew = { viewModel.craftNewCharacter() },
                            onMemoryRecall = { id -> viewModel.openMemoryRecall(id) },
                            onSetLobbyHero = { id ->
                                viewModel.selectLobbyHero(id)
                                viewModel.openScreen(LobbyDestination.LOBBY)
                            },
                            onDeployToParty = { id, slot -> viewModel.deployToParty(id, slot) },
                            onUndeployFromParty = { id -> viewModel.undeployFromParty(id) },
                            // 🎬 도감 「등장 영상 다시 보기」 재생 동안 BGM 일시정지
                            onVideoStart = { audio.suspendBgmForVideo() },
                            onVideoEnd = { audio.resumeBgmAfterVideo() },
                            // 2026-11-19: 도감 서브 탭 [강화][합성]에서 바로 실행
                            onUpgrade = { id -> viewModel.upgradeCard(id) },
                            onCraft = { id -> viewModel.craftCharacter(id) },
                            onTranscend = { id -> viewModel.transcendCard(id) },
                            onOpenShop = { viewModel.openScreen(LobbyDestination.SHOP) }
                        )
                        LobbyDestination.DECK -> DeckBuilderScreen(
                            state = state,
                            onBack = { viewModel.backToLobby() },
                            onAutoEquip = { viewModel.autoEquipParty() },
                            onUnloadAll = { viewModel.unloadAllParty() },
                            onToggleCard = { id -> viewModel.toggleEquipCard(id) },
                            onUnloadSlot = { slot -> viewModel.unloadSlot(slot) },
                            onTrain = { viewModel.trainForExp() },
                            onOpenCollection = { viewModel.openScreen(LobbyDestination.COLLECTION) },
                            onOpenBattle = { viewModel.openScreen(LobbyDestination.BATTLE) },
                            onGaze = { viewModel.gazeAtDemonLord() },
                            onPurify = { viewModel.purify() },
                            onSetLobbyHero = { id -> viewModel.selectLobbyHero(id) },
                            // 2026-11-22: 덱 편성 화면에서 복장 변경 · 성장 화면 바로가기
                            onOpenDeckCard = { id -> audio.click(); viewModel.openDeckCharacterSheet(id) },
                            onCloseDeckSheet = { viewModel.closeDeckCharacterSheet() },
                            onSelectDeckOutfit = { visual -> viewModel.selectDeckOutfit(visual) },
                            onOpenGrowthFromDeck = { id -> audio.click(); viewModel.openGrowthFromDeck(id) }
                        )
                    }
                }
            }
        }

        // ── 도입부 스토리 컷신 — 로비 위에 겹쳐 재생하고, 끝나면 페이드아웃으로 로비가 드러난다 ──
        AnimatedVisibility(
            visible = isIntroPlaying,
            enter = fadeIn(tween(durationMillis = 450)),
            exit = fadeOut(tween(durationMillis = 650)),
            modifier = Modifier.fillMaxSize()
        ) {
            IntroStoryScreen(
                state = state.introStory,
                onNext = { audio.click(); viewModel.advanceIntro() },
                onSkip = { audio.click(); viewModel.skipIntro() }
            )
        }

        // ── 💎 다이아 교환소 — 상점·현실 소환 위에 겹치는 패널 (2026-11-27 v0.6.0) ──
        AnimatedVisibility(
            visible = state.isDiamondShopOpen,
            enter = fadeIn(tween(durationMillis = 160)),
            exit = fadeOut(tween(durationMillis = 160)),
            modifier = Modifier.fillMaxSize()
        ) {
            DiamondShopOverlay(
                state = state,
                onClose = { audio.click(); viewModel.closeDiamondShop() },
                onSelectTab = { tab -> viewModel.selectDiamondShopTab(tab) },
                onSelectTarget = { id -> viewModel.selectDiamondShopTarget(id) },
                onBuyTargetSummon = { audio.click(); viewModel.buyTargetSummon() },
                onRecharge = { kind -> audio.click(); viewModel.rechargeWithGems(kind) },
                onExchange = { kind -> audio.click(); viewModel.exchangeWithGems(kind) },
                onUnlockDateLook = { audio.click(); viewModel.unlockDateLookWithGems() }
            )
        }

        // ── 결과성 SFX — 상태 변화를 감지해 튼다 (공격·피격·개봉·강화·소환·승패) ──
        GameAudioEffects(state = state, audio = audio)
    }
}

/**
 * 가로 모드 높이 맞춤 프레임 — 2026-11-21 사용자 피드백.
 *
 * > "tcg 게임 테블릿 에서 실행하면 여전히 가로 모드일때 풀 화면에 맞춰 지면서 영상이 안보여
 * >  가로 모드일때는 높이에 맞춰 화면 사이즈가 조절 되도록 해줘"
 *
 * 세로 화면(휴대폰 기본)은 지금까지처럼 화면 전체를 쓴다. 가로(폭 > 높이, 태블릿 가로 포함)에서는
 *
 *  · 게임 컨테이너를 **화면 높이에 꽉 맞추고** 폭은 2:3([ResponsiveLayout.LANDSCAPE_FRAME_ASPECT])로 줄인다
 *  · 그래서 로비·도감·전투·등장 영상이 폭에 끌려 늘어나거나 아래로 밀리지 않는다
 *  · 양옆에는 대표 캐릭터 원화를 흐리게(블러) 깐 앰비언트 배경을 둔다 — 빈 공간이 아니라 '무대 밖 조명'처럼 보인다
 */
@Composable
private fun HeightFitGameFrame(
    backdropCardId: Int = 3,
    backdropVisual: VisualState = VisualState.BATTLE,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background)
    ) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        val landscape = ResponsiveLayout.isLandscape(widthDp, heightDp)

        if (landscape) {
            // 무대 밖 — 대표 캐릭터 원화를 깊게 흐리게 깔고 어둡게 덮는다
            // (AmbientCharacterBackdrop 과 같은 방식: 블러 앰비언트 + 어두운 스크림)
            CardArtImage(
                cardId = backdropCardId,
                visual = backdropVisual,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(32.dp),
                targetWidthPx = 360
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TcgColors.Background.copy(alpha = 0.78f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                TcgColors.Violet.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )
            val frameWidth = ResponsiveLayout.portraitFrameWidthDp(widthDp, heightDp).dp
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(frameWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                TcgColors.Gold.copy(alpha = 0.45f),
                                TcgColors.Violet.copy(alpha = 0.35f),
                                TcgColors.Gold.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                content()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/** 기능 화면 상단 바 — 「← 메인 로비」 복귀 버튼 + 화면 이름 + 재화 + 🔊 오디오 토글 */
@Composable
private fun LobbyBackBar(
    screen: LobbyDestination,
    gold: Int,
    gems: Int,
    audio: AudioManager,
    audioSettings: com.game.tcg.audio.AudioSettings,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TcgColors.Surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(TcgColors.Violet.copy(alpha = 0.18f))
                .border(1.dp, TcgColors.Violet.copy(alpha = 0.6f), RoundedCornerShape(50))
                .clickable { onBack() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("← 메인 로비", color = TcgColors.Violet, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${screen.icon} ${screen.label}",
                color = TcgColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(screen.tagline, color = TcgColors.Muted, fontSize = TcgType.TILE_MARK.sp)
        }
        Spacer(Modifier.width(5.dp))
        AudioToggleRow(audio = audio, settings = audioSettings)
        Spacer(Modifier.width(6.dp))
        Pill("${gold}G", TcgColors.Gold)
        Spacer(Modifier.width(5.dp))
        Pill("${gems}💎", TcgColors.Mana)
    }
}

/** 화면 하단 상태 메시지 바 */
@Composable
private fun TcgStatusBar(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TcgColors.Surface)
            // targetSdk 35 엣지-투-엣지: 제스처 내비게이션 바에 글자가 가리지 않도록
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            color = TcgColors.Muted,
            fontSize = TcgType.TILE_MARK.sp,
            maxLines = 2,
            lineHeight = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 🔊/🔇 BGM·SFX 토글 행 — 로비 상단 바와 메인 로비 상단 스트립에서 쓴다.
 * 각 토글은 즉시 적용되고(SharedPreferences 저장), BGM 토글을 끄면 현재 곡이 멈춘다.
 */
@Composable
fun AudioToggleRow(
    audio: AudioManager,
    settings: com.game.tcg.audio.AudioSettings,
    compact: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AudioToggleButton(
            label = if (settings.bgmEnabled) "🔊BGM" else "🔇BGM",
            enabled = settings.bgmEnabled,
            onClick = { audio.toggleBgm() }
        )
        Spacer(Modifier.width(4.dp))
        AudioToggleButton(
            label = if (settings.sfxEnabled) "🔊SFX" else "🔇SFX",
            enabled = settings.sfxEnabled,
            onClick = {
                audio.toggleSfx()
                // 켜는 순간 클릭음으로 바로 확인시켜 준다
                if (!settings.sfxEnabled) audio.click()
            }
        )
    }
}

@Composable
private fun AudioToggleButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (enabled) TcgColors.Violet.copy(alpha = 0.18f)
                else TcgColors.SurfaceAlt.copy(alpha = 0.9f)
            )
            .border(
                1.dp,
                if (enabled) TcgColors.Violet.copy(alpha = 0.6f) else TcgColors.Line,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            color = if (enabled) TcgColors.Violet else TcgColors.Muted,
            fontSize = TcgType.TILE_MARK.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 결과성 SFX — ViewModel 상태의 변화 "틱"을 감지해 효과음을 튼다.
 * (공격/피격은 버튼 시점 재생이 아니라 실제 데미지가 발생했을 때 울리도록 틱으로 감지)
 */
@Composable
fun GameAudioEffects(state: com.game.tcg.ui.TcgGameState, audio: AudioManager) {
    // 적 타격 성공 (공격 데미지 발생)
    androidx.compose.runtime.remember(state.lastEnemyHitTick) {
        if (state.lastEnemyHitTick > 0) audio.attack()
        state.lastEnemyHitTick
    }
    // 아군 피격 (적 반격 데미지 발생)
    androidx.compose.runtime.remember(state.lastAllyHitTick) {
        if (state.lastAllyHitTick > 0) audio.hit()
        state.lastAllyHitTick
    }
    // 카드팩 개봉 결과 — 상점 화면에서는 개봉 연출(봉인 해제 순간)이 직접 pack_open 을 울리므로
    // 여기서는 중복 재생을 피한다.
    androidx.compose.runtime.remember(state.lastPackOpen, state.currentScreen) {
        if (state.lastPackOpen != null && state.currentScreen != LobbyDestination.SHOP) audio.packOpen()
        state.lastPackOpen
    }
    // 강화·연성·초월·장비 성공 (growthMessage 가 바뀔 때)
    androidx.compose.runtime.remember(state.growthMessage) {
        if (state.growthMessage != null) audio.upgrade()
        state.growthMessage
    }
    // 현실 소환 성공
    androidx.compose.runtime.remember(state.lastSummon) {
        if (state.lastSummon != null) audio.summon()
        state.lastSummon
    }
    // 페어 얼티밋 오의 컷인
    androidx.compose.runtime.remember(state.pairUltimateBanner) {
        if (state.pairUltimateBanner != null) audio.skill()
        state.pairUltimateBanner
    }
    // 전투 승리 (전투 승리 문구가 새로 생겼을 때)
    androidx.compose.runtime.remember(state.battleVictories) {
        if (state.battleVictories > 0) audio.victory()
        state.battleVictories
    }
    // 전투 패배 (생존 아군이 0이 되는 순간)
    androidx.compose.runtime.remember(state.aliveSurvivorCount) {
        if (state.enemyHp > 0 && state.aliveSurvivorCount == 0) audio.defeat()
        state.aliveSurvivorCount
    }
    // 영구 상실 (소유 카드가 줄어드는 순간은 전투 내 패배 신호로도 쓴다)
    androidx.compose.runtime.remember(state.reincarnationPool.size) {
        if (state.reincarnationPool.isNotEmpty()) audio.hit()
        state.reincarnationPool.size
    }
}

/**
 * 🔊 BGM·SFX 토글 — 재생기(AudioManager) 설정과 화면 상태(TcgGameState.audioSettings)를
 * 함께 갱신해 로비 정보 패널의 표시가 실제 재생 상태와 어긋나지 않게 한다.
 */
private fun syncAudio(
    audio: AudioManager,
    viewModel: TcgGameViewModel,
    toggle: (AudioManager) -> Unit
) {
    toggle(audio)
    val settings = audio.settings()
    viewModel.updateAudioSettings {
        it.copy(
            bgmEnabled = settings.bgmEnabled,
            sfxEnabled = settings.sfxEnabled,
            bgmVolume = settings.bgmVolume,
            sfxVolume = settings.sfxVolume
        )
    }
}
