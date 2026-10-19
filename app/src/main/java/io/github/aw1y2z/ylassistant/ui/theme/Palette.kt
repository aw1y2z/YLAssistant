package io.github.aw1y2z.ylassistant.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 品牌与平台识别色。
 *
 * 这些是固定值而非主题色：平台色用于让用户一眼区分夸克/UC/迅雷，
 * 品牌渐变用于首页主视觉，都不应随深浅色漂移。
 */
object Palette {
    val Teal = Color(0xFF0F766E)
    val Indigo = Color(0xFF4F46E5)

    val Quark = Color(0xFF2F6BFF)
    val Uc = Color(0xFFFF7A00)
    val Xunlei = Color(0xFF1E88E5)

    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFDC2626)
}
