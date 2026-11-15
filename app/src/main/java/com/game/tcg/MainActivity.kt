package com.game.tcg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.ui.BattleScreen
import com.game.tcg.ui.CollectionScreen
import com.game.tcg.ui.DeckScreen
import com.game.tcg.ui.ExplorationScreen
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.MainLobbyScreen
import com.game.tcg.ui.Pill
import com.game.tcg.ui.RealWorldScreen
import com.game.tcg.ui.ScenarioScreen
import com.game.tcg.ui.TcgColors
import com.game.tcg.ui.TcgGameViewModel

/**
 * 듀얼 게이트 — 이계의 문
 * 메인 로비(캐릭터 중심) + 좌·우측 측면 서브메뉴 6종
 *  - 좌측(이세계·모험): 이세계 접속 · 이계 전투 · 탐색
 *  - 우측(현실·관리): 현실 소환 · 캐릭터 관리 · 서약·덱
 */
class MainActivity : ComponentActivity() {
    private val viewModel = TcgGameViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

@Composable
fun TcgApp(viewModel: TcgGameViewModel) {
    val state by viewModel.uiState.collectAsState()
    val screen = state.currentScreen

    // Android 시스템 뒤로가기 → 메인 로비
    BackHandler(enabled = screen != LobbyDestination.LOBBY) { viewModel.backToLobby() }

    Scaffold(
        containerColor = TcgColors.Background,
        bottomBar = { TcgStatusBar(message = state.statusMessage) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (screen != LobbyDestination.LOBBY) {
                LobbyBackBar(
                    screen = screen,
                    gold = state.gold,
                    gems = state.gems,
                    onBack = { viewModel.backToLobby() }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (screen) {
                    LobbyDestination.LOBBY -> MainLobbyScreen(
                        state = state,
                        onOpenScreen = { destination -> viewModel.openScreen(destination) },
                        onSelectHero = { id -> viewModel.selectLobbyHero(id) },
                        onCycleHero = { direction -> viewModel.cycleLobbyHero(direction) },
                        onSelectVisual = { visual -> viewModel.selectLobbyVisual(visual) }
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
                        onPairUltimate = { viewModel.activatePairUltimate() }
                    )
                    LobbyDestination.EXPLORATION -> ExplorationScreen(
                        state = state,
                        onExplore = { mode -> viewModel.explore(mode) },
                        onOpenBattle = { viewModel.openScreen(LobbyDestination.BATTLE) },
                        onOpenCollection = { viewModel.openScreen(LobbyDestination.COLLECTION) },
                        onOpenRealWorld = { viewModel.openScreen(LobbyDestination.REAL_WORLD) }
                    )
                    LobbyDestination.REAL_WORLD -> RealWorldScreen(
                        state = state,
                        onInteract = { activity -> viewModel.interactRealWorld(activity) },
                        onBondMemoryPartner = { id -> viewModel.bondMemoryResetPartner(id) }
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
                        }
                    )
                    LobbyDestination.DECK -> DeckScreen(
                        state = state,
                        onGaze = { viewModel.gazeAtDemonLord() },
                        onPurify = { viewModel.purify() },
                        onSetLobbyHero = { id -> viewModel.selectLobbyHero(id) },
                        onOpenCollection = { viewModel.openScreen(LobbyDestination.COLLECTION) }
                    )
                }
            }
        }
    }
}

/** 기능 화면 상단 바 — 「← 메인 로비」 복귀 버튼 + 화면 이름 + 재화 */
@Composable
private fun LobbyBackBar(
    screen: LobbyDestination,
    gold: Int,
    gems: Int,
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
            Text("← 메인 로비", color = TcgColors.Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${screen.icon} ${screen.label}",
                color = TcgColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(screen.tagline, color = TcgColors.Muted, fontSize = 9.sp)
        }
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
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            color = TcgColors.Muted,
            fontSize = 9.sp,
            maxLines = 2,
            lineHeight = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
