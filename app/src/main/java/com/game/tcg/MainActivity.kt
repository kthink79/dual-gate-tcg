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
import com.game.tcg.core.engine.*
import com.game.tcg.core.model.*

class MainActivity : ComponentActivity() {
    private val battleEngine = BattleEngine()
    private val synthesisEngine = SynthesisEngine()
    private val dualWorldEngine = DualWorldEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TcgMainScreen(
                    battleEngine = battleEngine,
                    synthesisEngine = synthesisEngine,
                    dualWorldEngine = dualWorldEngine
                )
            }
        }
    }
}

@Composable
fun TcgMainScreen(
    battleEngine: BattleEngine,
    synthesisEngine: SynthesisEngine,
    dualWorldEngine: DualWorldEngine
) {
    var statusMessage by remember { mutableStateOf("듀얼 게이트 TCG 엔진 준비 완료") }

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
            text = "현실–이세계 크로스 모바일 TCG (Kotlin/Compose)",
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
                    onClick = {
                        val seraphiel = TcgCard(
                            id = 1,
                            name = "태양의 대천사 세라피엘",
                            element = Element.LIGHT,
                            baseCost = 8,
                            atk = 8500,
                            def = 7500,
                            bondLevel = 100, // 오의 개방
                            skillText = "필드 전개 시 아군 전체 실드 부여"
                        )
                        val carmilla = TcgCard(
                            id = 6,
                            name = "뱀파이어 퀸 카밀라",
                            element = Element.DARK,
                            baseCost = 8,
                            atk = 8200,
                            def = 7100,
                            skillText = "피해의 30% 생명력 흡수"
                        )
                        val result = battleEngine.calculateDamage(seraphiel, carmilla)
                        statusMessage = "[배틀 상성 연산 결과]\n${result.summaryMessage}\n원 데미지: ${result.rawDamage} → 최종 데미지: ${result.finalDamage} (빛↔어둠 상극 +50%, 오의 +25%)"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1. 속성 상성 및 페어 얼티밋 오의 검증")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val card = TcgCard(
                            id = 51,
                            name = "백호령 신녀 단아",
                            element = Element.WIND,
                            baseCost = 7,
                            atk = 7400,
                            def = 6200,
                            bondLevel = 20,
                            skillText = "아군 공격 속도 및 회피율 대폭 상승"
                        )
                        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.ROMANTIC_EVENING_DATE)
                        statusMessage = "[현실 소환 교감 결과]\n캐릭터: ${card.name}\n현재 비주얼 상태: ${card.currentVisual.displayName}\n호감도: ${card.bondLevel}/100"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2. 현실 일상 소환 (데이트 룩/호감도) 검증")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val base = TcgCard(1, "태양의 대천사 세라피엘", Element.LIGHT, 8, 8500, 7500, starLevel = 1, skillText = "")
                        val material = TcgCard(1, "태양의 대천사 세라피엘", Element.LIGHT, 8, 8500, 7500, starLevel = 1, skillText = "")
                        synthesisEngine.synthesizeCard(base, material)
                        statusMessage = "[카드 합성 결과]\n한계 돌파: ${base.starLevel}성 달성!\nATK 스케일링: 8500 → ${base.atk} (+20%)\nDEF 스케일링: 7500 → ${base.def} (+20%)"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("3. 동일 카드 한계 돌파(5성) 합성 검증")
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
                Text(text = statusMessage, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
