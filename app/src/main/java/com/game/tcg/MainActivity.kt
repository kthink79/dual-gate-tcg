package com.game.tcg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.ui.ITcgUiContract
import com.game.tcg.ui.TcgUiState
import com.game.tcg.ui.TcgViewModel

class MainActivity : ComponentActivity() {
    private val viewModel = TcgViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TcgMainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TcgMainScreen(viewModel: ITcgUiContract) {
    val uiState by viewModel.uiState.collectAsState()

    TcgMainContent(
        uiState = uiState,
        onSimulateBattle = { viewModel.executeBattleSimulation() },
        onInteractRealWorld = { viewModel.executeRealWorldInteraction(RealWorldActivity.ROMANTIC_EVENING_DATE) },
        onSynthesize = { viewModel.executeSynthesisAscension() },
        onExpedition = { viewModel.executeIsekaiExpedition() }
    )
}

@Composable
fun TcgMainContent(
    uiState: TcgUiState,
    onSimulateBattle: () -> Unit,
    onInteractRealWorld: () -> Unit,
    onSynthesize: () -> Unit,
    onExpedition: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1016))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎴 듀얼 게이트: 이계의 문",
            color = Color(0xFF64B5F6),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "현실–이세계 크로스 모바일 TCG (Kotlin/Compose Clean Architecture)",
            color = Color.LightGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C28))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "핵심 TCG 룰 및 크로스 루프 검증",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onSimulateBattle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1. 속성 상성 및 페어 얼티밋 오의 검증")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onInteractRealWorld,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2. 현실 일상 소환 (데이트 룩/호감도) 검증")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSynthesize,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("3. 동일 카드 한계 돌파(5성) 합성 검증")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onExpedition,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("4. 이세계 원정 배틀 및 EXP 스탯 성장 검증")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141620))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "시뮬레이션 출력 로그", color = Color(0xFF90CAF9), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = uiState.statusMessage, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
