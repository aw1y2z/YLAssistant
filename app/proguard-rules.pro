# 当前模块只有 Compose，无反射入口。
# 后续接入 WebView 登录时保留 @JavascriptInterface 方法。
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
