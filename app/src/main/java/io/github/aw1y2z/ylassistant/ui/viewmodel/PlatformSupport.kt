package io.github.aw1y2z.ylassistant.ui.viewmodel

import io.github.aw1y2z.ylassistant.data.network.QuarkConstants
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.data.network.UCConstants

/**
 * 平台差异统一收敛：根目录 fid / 直链请求头 / 展示名。
 * 解析下载（ResolveViewModel）与个人文件浏览（DriveBrowseViewModel）共用，避免两处各写一套。
 */
object PlatformSupport {

    /** 各平台「根目录」fid：夸克/UC 用 "0"，迅雷 parent_id 空串即根 */
    fun rootDir(p: SharePlatform): String = when (p) {
        SharePlatform.QUARK -> QuarkConstants.DEFAULT_PDIR_FID
        SharePlatform.UC -> UCConstants.DEFAULT_PDIR_FID
        SharePlatform.XUNLEI -> ""
    }

    fun name(p: SharePlatform): String = when (p) {
        SharePlatform.QUARK -> "夸克网盘"
        SharePlatform.UC -> "UC 网盘"
        SharePlatform.XUNLEI -> "迅雷网盘"
    }

    /** 下载直链所需请求头（夸克/UC 带 Cookie+防盗链，迅雷直链自带 CDN 签名不带） */
    fun headers(p: SharePlatform, cred: String): Map<String, String> = when (p) {
        SharePlatform.QUARK -> mapOf(
            "Cookie" to cred,
            "User-Agent" to QuarkConstants.API_USER_AGENT,
            "Referer" to QuarkConstants.DOWNLOAD_REFERER,
            "Accept-Encoding" to "identity",
        )
        SharePlatform.UC -> mapOf(
            "Cookie" to cred,
            "User-Agent" to UCConstants.CLOUD_UA,
            "Referer" to UCConstants.DOWNLOAD_REFERER,
            "Origin" to UCConstants.WEB_ORIGIN,
        )
        SharePlatform.XUNLEI -> emptyMap()
    }
}
