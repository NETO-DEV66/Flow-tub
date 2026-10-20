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
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.neurotube.app.R

/**
 * Sign-in WebView for YouTube. Loads Google's own account page, waits until the user has an
 * authenticated YouTube session in the jar, lifts the session cookies out, and gets out of the way.
 *
 * The jar is wiped on every exit path — success, cancel, or failure — so the credential exists only
 * in the encrypted store the repository owns.
 */
class YouTubeLoginActivity : ComponentActivity() {
    private var completed = false
    private var cleanupStarted = false
    private lateinit var loginWebView: WebView

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
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                // Google refuses to complete sign-in inside a WebView that announces itself as one.
                settings.userAgentString = settings.userAgentString.replace("; wv", "")
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            if (!request.isForMainFrame) return false
                            return handleNavigation(view, request.url.toString())
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String,
                        ): Boolean = handleNavigation(view, url)

                        override fun onPageFinished(
                            view: WebView,
                            url: String,
                        ) {
                            super.onPageFinished(view, url)
                            captureWhenAuthenticated(view, url)
                        }
                    }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                loadUrl(YOUTUBE_HOME_URL)
            }
        setContentView(loginWebView)
    }

    override fun onDestroy() {
        if (!completed && isFinishing) {
            YouTubeLoginBroker.fail(getString(R.string.youtube_login_error_cancelled))
        }
        clearLoginSession()
        super.onDestroy()
    }

    private fun handleNavigation(
        webView: WebView,
        url: String,
    ): Boolean {
        if (!isAllowedNavigation(url)) return true
        captureWhenAuthenticated(webView, url)
        return false
    }

    /** A session only exists once Google has sent the user on to youtube.com with its cookies set. */
    private fun captureWhenAuthenticated(
        webView: WebView,
        url: String,
    ) {
        if (completed || !isAuthenticatedUrl(url)) return
        val jar = CookieManager.getInstance().getCookie(YOUTUBE_ORIGIN).orEmpty()
        if (!YouTubeCookieExtractor.hasSession(jar)) return

        val header = YouTubeCookieExtractor.toCookieHeader(jar) ?: return
        completed = true
        YouTubeLoginBroker.complete(header)
        clearLoginSession(onComplete = ::finish)
    }

    private fun clearLoginSession(onComplete: (() -> Unit)? = null) {
        if (cleanupStarted) {
            onComplete?.invoke()
            return
        }
        cleanupStarted = true

        WebStorage.getInstance().deleteOrigin(YOUTUBE_ORIGIN)
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

    private fun isAllowedNavigation(url: String): Boolean = url.startsWith("https://") && GOOGLE_ORIGINS.any { url.startsWith(it) }

    private fun isAuthenticatedUrl(url: String): Boolean =
        url.startsWith("https://www.youtube.com") || url.startsWith("https://m.youtube.com")

    private companion object {
        const val YOUTUBE_HOME_URL = "https://www.youtube.com/"
        const val YOUTUBE_ORIGIN = "https://www.youtube.com"

        val GOOGLE_ORIGINS =
            listOf(
                "https://www.youtube.com",
                "https://m.youtube.com",
                "https://accounts.google.com",
                "https://accounts.youtube.com",
                "https://myaccount.google.com",
                "https://www.google.com",
            )
    }
}
