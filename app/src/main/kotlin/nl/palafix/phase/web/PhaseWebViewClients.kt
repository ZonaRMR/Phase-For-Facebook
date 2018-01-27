package nl.palafix.phase.web

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.os.Message
import android.provider.Settings
import android.util.AttributeSet
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import nl.palafix.phase.facebook.FB_URL_BASE
import nl.palafix.phase.facebook.FbItem
import nl.palafix.phase.injectors.*
import nl.palafix.phase.utils.*
import nl.palafix.phase.utils.iab.IS_Phase_PRO
import nl.palafix.phase.views.PhaseWebView
import io.reactivex.subjects.Subject
import org.jetbrains.anko.withAlpha
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * Collection of webview clients
 */

/**
 * The base of all webview clients
 * Used to ensure that resources are properly intercepted
 */

open class BaseWebViewClient : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = view.shouldPhaseInterceptRequest(request)
}
    /**
     * The default webview client
     */
    open class PhaseWebViewClient(val web: PhaseWebView) : BaseWebViewClient() {

        private val refresh: Subject<Boolean> = web.parent.refreshObservable
        private val isMain = web.parent.baseEnum != null

        protected inline fun v(crossinline message: () -> Any?) = L.v { "web client: ${message()}" }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url == null) return
            v { "loading $url" }
            refresh.onNext(true)
        }

        private fun injectBackgroundColor() {
            web.setBackgroundColor(
                    when {
                        isMain -> Color.TRANSPARENT
                        web.url.isFacebookUrl -> Prefs.bgColor.withAlpha(255)
                        else -> Color.WHITE
                    }
            )
        }

        override fun onPageCommitVisible(view: WebView, url: String?) {
            super.onPageCommitVisible(view, url)
            injectBackgroundColor()
            if (url.isFacebookUrl)
                view.jsInject(
                        CssAssets.ROUND_ICONS.maybe(Prefs.showRoundedIcons),
                        CssHider.HEADER,
                        CssHider.CORE,
                        CssHider.COMPOSER.maybe(!Prefs.showComposer),
                        CssHider.PEOPLE_YOU_MAY_KNOW.maybe(!Prefs.showSuggestedFriends && IS_Phase_PRO),
                        CssHider.SUGGESTED_GROUPS.maybe(!Prefs.showSuggestedGroups && IS_Phase_PRO),
                        Prefs.themeInjector,
                        CssHider.NON_RECENT.maybe((web.url?.contains("?sk=h_chr") ?: false)
                                && Prefs.aggressiveRecents),
                        JsAssets.DOCUMENT_WATCHER,
                        JsAssets.CLICK_A,
                        CssHider.ADS.maybe(!Prefs.showFacebookAds && IS_Phase_PRO),
                        JsAssets.CONTEXT_A,
                        JsAssets.MEDIA)
            else
                refresh.onNext(false)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            url ?: return
            v { "finished $url" }
            if (!url.isFacebookUrl) {
                refresh.onNext(false)
                return
            }
            onPageFinishedActions(url)
        }

        internal open fun onPageFinishedActions(url: String) {
            if (url.startsWith("${FbItem.MESSAGES.url}/read/") && Prefs.messageScrollToBottom)
                web.pageDown(true)
            injectAndFinish()
        }

        internal fun injectAndFinish() {
            v { "page finished reveal" }
            refresh.onNext(false)
            injectBackgroundColor()
            web.jsInject(
                    JsActions.LOGIN_CHECK,
                    JsAssets.TEXTAREA_LISTENER,
                    JsAssets.HEADER_BADGES.maybe(isMain))
        }

        open fun handleHtml(html: String?) {
            L.d { "Handle Html" }
        }

        open fun emit(flag: Int) {
            L.d { "Emit $flag" }
        }

        /**
         * Helper to format the request and launch it
         * returns true to override the url
         * returns false if we are already in an overlaying activity
         */
        private fun launchRequest(request: WebResourceRequest): Boolean {
            v { "Launching url: ${request.url}" }
            return web.requestWebOverlay(request.url.toString())
        }

        private fun launchImage(url: String, text: String? = null): Boolean {
            v { "Launching image: $url" }
            web.context.launchImageActivity(url, text)
            if (web.canGoBack()) web.goBack()
            return true
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            v { "Url loading: ${request.url}" }
            val path = request.url?.path ?: return super.shouldOverrideUrlLoading(view, request)
            v { "Url path $path" }
            val url = request.url.toString()
            if (url.isExplicitIntent) {
                view.context.resolveActivityForUri(request.url)
                return true
            }
            if (path.startsWith("/composer/")) return launchRequest(request)
            if (url.isImageUrl)
                return launchImage(url)
            if (Prefs.linksInDefaultApp && view.context.resolveActivityForUri(request.url)) return true
            return super.shouldOverrideUrlLoading(view, request)
        }
    }

    private const val EMIT_THEME = 0b1
    private const val EMIT_ID = 0b10
    private const val EMIT_COMPLETE = EMIT_THEME or EMIT_ID
    private const val EMIT_FINISH = 0

    /**
     * Client variant for the menu view
     */
    class PhaseWebViewClientMenu(web: PhaseWebView) : PhaseWebViewClient(web) {

        private val String.shouldInjectMenu
            get() = when (removePrefix(FB_URL_BASE)) {
                "settings",
                "settings#",
                "settings#!/settings?soft=bookmarks" -> true
                else -> false
            }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            if (url == null) return
            if (url.shouldInjectMenu) jsInject(JsAssets.MENU)
        }

        override fun emit(flag: Int) {
            super.emit(flag)
            when (flag) {
                EMIT_FINISH -> super.injectAndFinish()
            }
        }

        override fun onPageFinishedActions(url: String) {
            v { "Should inject ${url.shouldInjectMenu}" }
            if (!url.shouldInjectMenu) injectAndFinish()
        }
    }

