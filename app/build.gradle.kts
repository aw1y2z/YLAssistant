import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 与正式版共用同一把本机密钥
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasKeystore = keystoreProps.getProperty("storeFile")?.let { File(it).exists() } == true

android {
    namespace = "io.github.aw1y2z.ylassistant"

    // Miuix 0.9.4 要求 compileSdk >= 37
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.aw1y2z.ylassistant"
        minSdk = 26
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile = File(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

    buildFeatures {
        compose = true
        // 数据层 HttpClients 用 BuildConfig.DEBUG 控制「忽略 SSL」开关
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.10.0")
    // Miuix 0.9.4 的 NavigationBar/OverlayBottomSheet 需要 LocalNavigationEventDispatcherOwner；
    // 该 local 由 androidx.navigationevent 提供，activity-compose 尚未自动注入，故显式引入并手动提供
    implementation("androidx.navigationevent:navigationevent-compose:1.0.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // 数据层：分片下载引擎 + 平台 API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Miuix：小米 HyperOS 风格 Compose 组件库
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    // Miuix 图标集（文档要求用 MiuixIcons）
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    // 文件类型图标等 Miuix 未覆盖的仍用 material
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}
