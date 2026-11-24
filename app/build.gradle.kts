plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.game.tcg"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.game.tcg"
        minSdk = 26
        targetSdk = 35
        // 2026-11-24: 로스터 192명(190~199 신규 10인) · 영상 12편(누적 120) · 캐릭터 관리 갤러리/확대 뷰어
        versionCode = 3
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── 서명 ────────────────────────────────────────────────────────────
    // 2026-11-17: CI(GitHub Actions)가 러너마다 **새 디버그 키**를 만들어 서명하면
    // 이전에 설치한 APK 위에 업데이트 설치가 되지 않는다(서명 불일치).
    // 그래서 이 프로젝트의 디버그 키(keystore/debug.keystore · androiddebugkey)를
    // 저장소에 함께 두고 모든 빌드(로컬·CI)가 같은 키로 서명하게 고정한다.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    androidResources {
        // 캐릭터 등장 영상(mp4)은 assets 에 **무압축**으로 둔다 —
        // MediaPlayer 가 assets.openFd() 로 바로 읽어 재생할 수 있어야 한다.
        noCompress += listOf("mp4")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
