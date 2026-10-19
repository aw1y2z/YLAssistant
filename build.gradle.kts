// AGP 9 起 Kotlin 支持已内置，额外 apply org.jetbrains.kotlin.android 会报错。
// Miuix 0.9.4 依赖的 compose 1.12 / material-color-utilities 要求 compileSdk >= 37。
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
