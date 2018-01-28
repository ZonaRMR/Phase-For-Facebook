package nl.palafix.phase.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.PendingIntent.getActivity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Environment
import android.support.annotation.NonNull
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.graphics.drawable.ArgbEvaluator
import android.support.v13.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.text.Html
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import ca.allanwang.kau.swipe.kauSwipeOnCreate
import ca.allanwang.kau.swipe.kauSwipeOnDestroy
import ca.allanwang.kau.utils.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.typeface.IIcon
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.login_webview.*
import nl.palafix.phase.R
import nl.palafix.phase.contracts.*
import nl.palafix.phase.enums.OverlayContext
import nl.palafix.phase.facebook.*
import nl.palafix.phase.model.BookmarkModel
import nl.palafix.phase.services.PhaseRunnable
import nl.palafix.phase.utils.*
import nl.palafix.phase.views.PhaseContentWeb
import nl.palafix.phase.views.PhaseVideoViewer
import nl.palafix.phase.views.PhaseWebView
import okhttp3.HttpUrl
import org.jetbrains.anko.custom.ankoView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Allan Wang on 2017-06-01.
 *
 * Collection of overlay activities for Phase
 *
 * Each one is largely the same layout, but is separated so they may run is separate single tasks
 * All overlays support user switches
 */

/**
 * Used by notifications. Unlike the other overlays, this runs as a singleInstance
 * Going back will bring you back to the previous app
 */
class PhaseWebActivity : WebOverlayActivityBase(false) {

    override fun onCreate(savedInstanceState: Bundle?) {
        val requiresAction = !parseActionSend()
        PhasePglAdBlock.init(this)
        super.onCreate(savedInstanceState)
        if (requiresAction) {
            /*
             * Signifies that we need to let the user know of a bad url
             * We will subscribe to the load cycle once,
             * and pop a dialog giving the user the option to copy the shared text
             */
            var disposable: Disposable? = null
            disposable = content.refreshObservable.subscribe {
                disposable?.dispose()
                materialDialogThemed {
                    title(R.string.invalid_share_url)
                    content(R.string.invalid_share_url_desc)
                }
            }
        }
    }

    /**
     * Attempts to parse the action url
     * Returns [true] if no action exists or if the action has been consumed, [false] if we need to notify the user of a bad action
     */
    private fun parseActionSend(): Boolean {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return true
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return true
        val url = HttpUrl.parse(text)?.toString()
        if (url == null) {
            L.i { "Attempted to share a non-url" }
            L._i { "Shared text: $text" }
            copyToClipboard(text, "Text to Share", showToast = false)
            intent.putExtra(ARG_URL, FbItem.FEED.url)
            return false
        } else {
            L.i { "Sharing url through overlay" }
            L._i { "Url: $url" }
            intent.putExtra(ARG_URL, "${FB_URL_BASE}sharer/sharer.php?u=$url")
            return true
        }
    }

}

/**
 * Variant that forces a basic user agent. This is largely internal,
 * and is only necessary when we are launching from an existing [WebOverlayActivityBase]
 */
class WebOverlayBasicActivity : WebOverlayActivityBase(true)

/**
 * Internal overlay for the app; this is tied with the main task and is singleTop as opposed to singleInstance
 */
class WebOverlayActivity : WebOverlayActivityBase(false)


@Suppress("NAME_SHADOWING", "DEPRECATED_IDENTITY_EQUALS", "DEPRECATION")
@SuppressLint("Registered")
open class WebOverlayActivityBase(private val forceBasicAgent: Boolean) : BaseActivity(),
        ActivityContract, PhaseContentContainer,
        VideoViewHolder, FileChooserContract by FileChooserDelegate() {

    var bundle: Bundle? = null
    var realm: Realm by Delegates.notNull()

    override val frameWrapper: FrameLayout by bindView(R.id.frame_wrapper)
    val toolbar: Toolbar by bindView(R.id.overlay_toolbar)
    val content: PhaseContentWeb by bindView(R.id.phase_content_web)
    val web: PhaseWebView
        get() = content.coreView
    val coordinator: CoordinatorLayout by bindView(R.id.overlay_main_content)

    private inline val urlTest: String?
        get() = intent.getStringExtra(ARG_URL) ?: intent.dataString

    override val baseUrl: String
        get() = (intent.getStringExtra(ARG_URL) ?: intent.dataString).formattedFbUrl

    override val baseEnum: FbItem? = null

    private inline val userId: Long
        get() = intent.getLongExtra(ARG_USER_ID, Prefs.userId) ?: Prefs.userId

    private val overlayContext: OverlayContext?
        get() = OverlayContext[intent.extras]

    override fun setTitle(title: String) {
        toolbar.title = title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (urlTest == null) {
            L.e { "Empty link on web overlay" }
            toast(R.string.null_url_overlay)
            finish()
            return
        }
        setFrameContentView(R.layout.activity_web_overlay)
        setSupportActionBar(toolbar)
        Realm.init(this)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon = GoogleMaterial.Icon.gmd_close.toDrawable(this, 16, Prefs.iconColor)
        toolbar.setNavigationOnClickListener { finishSlideOut() }
        PhasePglAdBlock.init(this)
        realm = Realm.getDefaultInstance()
        setPhaseColors {
            toolbar(toolbar)
            themeWindow = false
        }
        coordinator.setBackgroundColor(Prefs.bgColor.withAlpha(255))

        content.bind(this)

        content.titleObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { toolbar.title = it }

        with(web) {
            if (forceBasicAgent) //todo check; the webview already adds it dynamically
                userAgentString = USER_AGENT_BASIC
            Prefs.prevId = Prefs.userId
            if (userId != Prefs.userId) FbCookie.switchUser(userId) { reloadBase(true) }
            else reloadBase(true)
            if (Showcase.firstWebOverlay) {
                coordinator.phaseSnackbar(R.string.web_overlay_swipe_hint) {
                    duration = Snackbar.LENGTH_INDEFINITE
                    setAction(R.string.got_it) { _ -> this.dismiss() }
                }
            }
        }

        PhaseRunnable.propagate(this, intent)
        L.v { "Done propagation" }

        kauSwipeOnCreate {
            if (!Prefs.overlayFullScreenSwipe) edgeSize = 20.dpToPx
            transitionSystemBars = false
        }
        val settings = web.settings
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.allowFileAccess = true
        // Allow use of Local Storage
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        // Enable pinch to zoom without the zoom buttons
        settings.builtInZoomControls = true
        settings.setSupportZoom(true)
        settings.displayZoomControls = false
        // Use WideViewport and Zoom out if there is no viewport defined
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
    }

    /**
     * Manage url loadings
     * This is usually only called when multiple listeners are added and inject the same url
     * We will avoid reloading if the url is the same
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        L.d { "New intent" }
        val newUrl = (intent.getStringExtra(ARG_URL) ?: intent.dataString)?.formattedFbUrl ?: return
        if (baseUrl != newUrl) {
            this.intent = intent
            content.baseUrl = newUrl
            web.reloadBase(true)
        }
    }

    override fun backConsumer(): Boolean {
        if (web.canGoBack()) {
            web.goBack()
            return true
        }
        else (finishSlideOut())
        web.clearCache(true)
        return true
    }

    /**
     * Our theme for the overlay should be fully opaque
     */
    fun theme() {
        val opaqueAccent = Prefs.headerColor.withAlpha(255)
        statusBarColor = opaqueAccent.darken()
        navigationBarColor = opaqueAccent
        toolbar.setBackgroundColor(opaqueAccent)
        toolbar.setTitleTextColor(Prefs.iconColor)
        coordinator.setBackgroundColor(Prefs.bgColor.withAlpha(255))
        toolbar.overflowIcon?.setTint(Prefs.iconColor)
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
    }

    override fun onPause() {
        web.pauseTimers()
        L.v { "Pause overlay web timers" }
        super.onPause()
    }

    override fun onDestroy() {
        web.destroy()
        web.clearCache(true)
        super.onDestroy()
        kauSwipeOnDestroy()
    }

    override fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams) {
        openMediaPicker(filePathCallback, fileChooserParams)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (onActivityResultWeb(requestCode, resultCode, data)) return
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_web, menu)
        overlayContext?.onMenuCreate(this, menu)
        toolbar.tint(Prefs.iconColor)
        setMenuIcons(menu, Prefs.iconColor,
                R.id.action_share to CommunityMaterial.Icon.cmd_share,
                R.id.action_copy_link to GoogleMaterial.Icon.gmd_content_copy,
                R.id.addbookmark to GoogleMaterial.Icon.gmd_bookmark_border)
        return true
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_copy_link -> copyToClipboard(web.currentUrl)
            R.id.action_share -> shareText(web.currentUrl)
            R.id.addbookmark -> {
                var flag: Boolean = false
                val bookmark = realm.where(BookmarkModel::class.java).findAll()
                for (model in bookmark) {
                    if (model.bookMark == web.url.toString()){
                        flag = true
                        break
                    }
                    flag = false
                }
                when (item.itemId) {
                    R.id.addbookmark -> {
                        realm.executeTransaction {
                            if (!flag) {
                                item.icon = getDrawable(R.drawable.ic_bookmark)
                                item.icon.setTint(Prefs.iconColor)
                                val bookmark = realm.createObject(BookmarkModel::class.java)
                                bookmark.bookMark = web.url.toString()
                                bookmark.title = web.title.toString()
                                Toast.makeText(baseContext, getString(R.string.added) + web.title + getString(R.string.added_string), Toast.LENGTH_LONG).show()
                            } else {
                                item.icon = getDrawable(R.drawable.ic_bookmark_border)
                                item.icon.setTint(Prefs.iconColor)
                                val results: RealmResults<BookmarkModel> = realm.where(BookmarkModel::class.java).equalTo("bookMark", web.url.toString()).findAll()
                                results.deleteAllFromRealm()
                            }
                        }
                        return true
                    }
                 }
               }
            }
        return false
         }

    fun getBookmark(realm: Realm): OrderedRealmCollection<BookmarkModel> {
        return realm.where(BookmarkModel::class.java).findAll()
    }

    /*
     * ----------------------------------------------------
     * Video Contract
     * ----------------------------------------------------
     */
    override var videoViewer: PhaseVideoViewer? = null
    override val lowerVideoPadding: PointF = PointF(0f, 0f)
}




