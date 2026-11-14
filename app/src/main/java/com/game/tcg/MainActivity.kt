package com.game.tcg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.ui.BattleScreen
import com.game.tcg.ui.CollectionScreen
import com.game.tcg.ui.Pill
import com.game.tcg.ui.RealWorldScreen
import com.game.tcg.ui.ScenarioScreen
import com.game.tcg.ui.TcgColors
import com.game.tcg.ui.TcgGameViewModel

/**
 * 듀얼 게이트 — 이계의 문
 * 4탭 하단 내비게이션: 📖 시나리오 · ⚔️ 이계 전투 · 🃏 도감·서약 · 🏙️ 현실 교감
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
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = TcgColors.Background,
        bottomBar = { TcgBottomBar(selected = tab, onSelect = { tab = it }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 상단 타이틀 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TcgColors.Surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🎴 듀얼 게이트 : 이계의 문",
                        color = TcgColors.Violet,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "현실–이세계 크로스 TCG · 제1장 황혼의 계약",
                        color = TcgColors.Muted,
                        fontSize = 9.sp
                    )
                }
                if (state.combatPenalty.totalStacks > 0) {
                    Pill("[홀림] ${state.combatPenalty.totalStacks}", TcgColors.Danger)
                } else {
                    Pill("정신 맑음", TcgColors.Heal)
                }
                Spacer(Modifier.width(6.dp))
                Pill("${state.gold}G", TcgColors.Gold)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> ScenarioScreen(
                        state = state,
                        onNextStep = { viewModel.advanceStory() },
                        onPrevStep = { viewModel.previousStory() },
                        onChooseEventA = { talk -> viewModel.chooseEventA(talk) },
                        onChooseEventB = { fight -> viewModel.chooseEventB(fight) }
                    )
                    1 -> BattleScreen(
                        state = state,
                        onAttackFront = { viewModel.attackFront() },
                        onAttackBack = { viewModel.attackBack() },
                        onNextTurn = { viewModel.nextTurn() },
                        onRetreat = { slot -> viewModel.retreat(slot) },
                        onEmergencyRetreat = { viewModel.emergencyRetreat() },
                        onReacquire = { id -> viewModel.reacquire(id) },
                        onRedeploy = { id -> viewModel.redeploy(id) },
                        onStartDemonLordBattle = { viewModel.startDemonLordBattle() },
                        onStartFieldBattle = { viewModel.startFieldBattle() }
                    )
                    2 -> CollectionScreen(
                        state = state,
                        onSelectCard = { id -> viewModel.selectCard(id) },
                        onSelectVisual = { visual -> viewModel.selectVisual(visual) },
                        onGaze = { viewModel.gazeAtDemonLord() },
                        onPurify = { viewModel.purify() },
                        onLimitBreak = { viewModel.limitBreakSelected() },
                        onCraftNew = { viewModel.craftNewCharacter() }
                    )
                    else -> RealWorldScreen(
                        state = state,
                        onInteract = { activity -> viewModel.interactRealWorld(activity) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TcgBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        "📖" to "시나리오",
        "⚔️" to "이계 전투",
        "🃏" to "도감·서약",
        "🏙️" to "현실 교감"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TcgColors.Surface)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.first, fontSize = 17.sp)
                Text(
                    item.second,
                    color = if (index == selected) TcgColors.Violet else TcgColors.Muted,
                    fontSize = 9.sp,
                    fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
