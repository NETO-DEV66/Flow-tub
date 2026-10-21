/*
 * Copyright (C) 2026 NeuroTube | A-EDev
 *
 * This file is part of NeuroTube (https://github.com/NETO-DEV66/NeuroTube).
 *
 * NeuroTube is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 */

package com.neurotube.app.account

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.neurotube.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sign-in WebView for YouTube.
 *
 * Google's own sign-in page is loaded directly, with YouTube as the `continue` target. That is the
 * only entry point Google completes inside a WebView: loading `youtube.com` and tapping its own
 * "Sign in" button instead hands the account chooser to a popup window, which a WebView with
 * multiple windows off silently drops, so the flow never finishes.
 *
 * Navigation is deliberately unrestricted. Google bounces the user through `accounts.google.com`,
 * `accounts.youtube.com` and `consent.youtube.com` before returning a YouTube page; an allow-list
 * that rejects any one of those hops strands the user mid-flow with no error.
 *
 * Once a YouTube page carries a session, the cookies and the page's own visitor identity are lifted
 * out and handed to [YouTubeLoginBroker]. The jar is wiped on every exit path — success, cancel, or
 * failure — so the credential exists only in the encrypted store the repository owns.
 */
class YouTubeLoginActivity : ComponentActivity() {
    private var completed = false
    private var cleanupStarted = false
    private var captureScheduled = false
    private lateinit var loginWebView: WebView

    /** Read back from the page's own JS, which answers after [WebViewClient.onPageFinished] returns. */
    @Volatile
    private var capturedVisitorData: String? = null

    @Volatile
    private var capturedDataSyncId: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        loginWebView =
            WebView(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                // Google refuses to complete sign-in inside a WebView that announces itself as one.
                settings.userAgentString = settings.userAgentString.replace("; wv", "")
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView,
                            url: String,
                        ) {
                            super.onPageFinished(view, url)
                            captureWhenAuthenticated(view, url)
                        }
                    }
            }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(loginWebView, true)
            // A half-finished attempt must not be resumed from a stale jar.
            removeAllCookies(null)
            flush()
        }

        setContentView(loginWebView)
        loginWebView.loadUrl(SIGN_IN_URL)
    }

    override fun onDestroy() {
        if (!completed && isFinishing) {
            YouTubeLoginBroker.fail(getString(R.string.youtube_login_error_cancelled))
        }
        clearLoginSession()
        super.onDestroy()
    }

    /**
     * A session only exists once Google has sent the user on to a YouTube page with its cookies set.
     * The page reports the visitor identity the session was minted with asynchronously, so the
     * capture waits a moment for it before reporting.
     */
    private fun captureWhenAuthenticated(
        webView: WebView,
        url: String,
    ) {
        if (completed || !isYouTubeUrl(url)) return
        readSessionIdentity(webView)
        if (captureScheduled || !YouTubeCookieExtractor.hasSession(youtubeCookieJar())) return

        captureScheduled = true
        lifecycleScope.launch {
            delay(IDENTITY_SETTLE_MS)
            if (completed) return@launch

            val header = YouTubeCookieExtractor.toCookieHeader(youtubeCookieJar())
            if (header == null || !YouTubeCookieExtractor.hasSession(header)) {
                // Not a session after all; a later page may still carry one.
                captureScheduled = false
                return@launch
            }

            completed = true
            YouTubeLoginBroker.complete(
                YouTubeLoginSession(
                    cookieHeader = header,
                    visitorData = capturedVisitorData,
                    dataSyncId = capturedDataSyncId,
                ),
            )
            clearLoginSession(onComplete = ::finish)
        }
    }

    /** Both values live on the page's own `window.yt.config_`; either may legitimately be absent. */
    private fun readSessionIdentity(webView: WebView) {
        webView.evaluateJavascript(JS_READ_VISITOR_DATA) { raw -> capturedVisitorData = parseJsValue(raw) }
        webView.evaluateJavascript(JS_READ_DATA_SYNC_ID) { raw ->
            capturedDataSyncId = YouTubeCookieExtractor.normalizeDataSyncId(parseJsValue(raw))
        }
    }

    /** [WebView.evaluateJavascript] answers with a JSON literal, so a string arrives quoted. */
    private fun parseJsValue(raw: String?): String? =
        raw?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != "null" }

    /** Every origin the session is set on, so a cookie one host drops is still picked up by another. */
    private fun youtubeCookieJar(): String =
        YOUTUBE_ORIGINS.mapNotNull { CookieManager.getInstance().getCookie(it) }.joinToString("; ")

    private fun clearLoginSession(onComplete: (() -> Unit)? = null) {
        if (cleanupStarted) {
            onComplete?.invoke()
            return
        }
        cleanupStarted = true

        YOUTUBE_ORIGINS.forEach { WebStorage.getInstance().deleteOrigin(it) }
        if (::loginWebView.isInitialized) {
            loginWebView.stopLoading()
            loginWebView.clearFormData()
            loginWebView.clearHistory()
        }
        clearGoogleCookies {
            if (::loginWebView.isInitialized) {
                loginWebView.removeAllViews()
                loginWebView.destroy()
            }
            YouTubeLoginBroker.released()
            onComplete?.invoke()
        }
    }

    /**
     * Expires every cookie the jar holds for the origins Google sign-in touches. The jar is shared
     * with the rest of the app's WebViews, so this also drops the consent cookie, which the network
     * layer re-adds per request when it needs it.
     */
    private fun clearGoogleCookies(onComplete: () -> Unit) {
        val manager = CookieManager.getInstance()
        val names =
            GOOGLE_ORIGINS
                .flatMap { origin -> manager.getCookie(origin).orEmpty().split(';') }
                .mapNotNull { cookie -> cookie.substringBefore('=').trim().takeIf(String::isNotEmpty) }
                .distinct()
        if (names.isEmpty()) {
            manager.flush()
            onComplete()
            return
        }

        var remaining = GOOGLE_ORIGINS.size * names.size
        GOOGLE_ORIGINS.forEach { origin ->
            names.forEach { name ->
                manager.setCookie(origin, "$name=; Max-Age=0; Path=/; Secure") {
                    remaining -= 1
                    if (remaining == 0) {
                        manager.flush()
                        onComplete()
                    }
                }
            }
        }
    }

    private fun isYouTubeUrl(url: String): Boolean = YOUTUBE_ORIGINS.any { url.startsWith(it) }

    private companion object {
        const val SIGN_IN_URL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
        const val IDENTITY_SETTLE_MS = 600L

        const val JS_READ_VISITOR_DATA =
            "(function(){try{return (window.yt&&window.yt.config_&&window.yt.config_.VISITOR_DATA)||null}" +
                "catch(e){return null}})()"
        const val JS_READ_DATA_SYNC_ID =
            "(function(){try{return (window.yt&&window.yt.config_&&window.yt.config_.DATASYNC_ID)||null}" +
                "catch(e){return null}})()"

        val YOUTUBE_ORIGINS =
            listOf(
                "https://www.youtube.com",
                "https://m.youtube.com",
                "https://music.youtube.com",
            )

        val GOOGLE_ORIGINS =
            listOf(
                "https://www.youtube.com",
                "https://m.youtube.com",
                "https://music.youtube.com",
                "https://accounts.google.com",
                "https://accounts.youtube.com",
                "https://consent.youtube.com",
                "https://myaccount.google.com",
                "https://www.google.com",
            )
    }
}
