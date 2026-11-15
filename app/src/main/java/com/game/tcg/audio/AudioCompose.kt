package com.game.tcg.audio

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * AudioManager 를 만들어 기억하고, 생명주기에 맞춰 BGM 을 멈추고 되돌린다.
 *
 * - ON_PAUSE / ON_STOP → [AudioManager.onAppPause] (BGM 일시정지)
 * - ON_RESUME          → [AudioManager.onAppResume] (BGM 재개)
 * - 화면이 사라지면 release
 *
 * Compose 프리뷰·JVM 환경에서 크래시하지 않도록 실제 재생은 no-op 으로도 동작한다.
 */
@Composable
fun rememberAudioManager(): AudioManager {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val manager = remember {
        AudioManager(
            player = AndroidAudioPlayer(context.applicationContext),
            prefs = SharedPrefsAudioPrefs(context.applicationContext)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> manager.onAppPause()
                Lifecycle.Event.ON_RESUME -> manager.onAppResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.release()
        }
    }

    return manager
}

/**
 * 화면 상태 → BGM 장면 반영. 상태 변화가 실제로 장면을 바꿀 때만 재생기에 닿는다.
 */
@Composable
fun ApplyBgmScene(audio: AudioManager, scene: AudioScene) {
    LaunchedEffect(scene) {
        audio.updateScene(scene)
    }
}
