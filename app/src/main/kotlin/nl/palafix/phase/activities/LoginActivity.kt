package nl.palafix.phase.activities

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.Toolbar
import android.widget.ImageView
import ca.allanwang.kau.utils.bindView
import ca.allanwang.kau.utils.fadeIn
import ca.allanwang.kau.utils.fadeOut
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.answers.LoginEvent
import nl.palafix.phase.R
import nl.palafix.phase.dbflow.CookieModel
import nl.palafix.phase.dbflow.fetchUsername
import nl.palafix.phase.dbflow.loadFbCookiesAsync
import nl.palafix.phase.facebook.FbCookie
import nl.palafix.phase.facebook.PROFILE_PICTURE_URL
import nl.palafix.phase.glide.PhaseGlide
import nl.palafix.phase.glide.GlideApp
import nl.palafix.phase.glide.transform
import nl.palafix.phase.utils.*
import nl.palafix.phase.web.LoginWebView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.SingleSubject


/**
 * Created by Allan Wang on 2017-06-01.
 */
class LoginActivity : BaseActivity() {

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val web: LoginWebView by bindView(R.id.login_webview)
    private val swipeRefresh: SwipeRefreshLayout by bindView(R.id.swipe_refresh)
    private val textview: AppCompatTextView by bindView(R.id.textview)
    private val profile: ImageView by bindView(R.id.profile)

    private val profileSubject = SingleSubject.create<Boolean>()
    private val usernameSubject = SingleSubject.create<String>()
    private lateinit var profileLoader: RequestManager

    // Helper to set and enable swipeRefresh
    private var refresh: Boolean
        get() = swipeRefresh.isRefreshing
        set(value) {
            if (value) swipeRefresh.isEnabled = true
            swipeRefresh.isRefreshing = value
            if (!value) swipeRefresh.isEnabled = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)
        setTitle(R.string.kau_login)
        setPhaseColors{
            toolbar(toolbar)
        }
        web.loadLogin({ refresh = it != 100 }) { cookie ->
            L.d { "Login found" }
            FbCookie.save(cookie.id)
            web.fadeOut(onFinish = {
                profile.fadeIn()
                loadInfo(cookie)
            })
        }
        profileLoader = GlideApp.with(profile)
    }

    private fun loadInfo(cookie: CookieModel) {
        refresh = true
        Single.zip<Boolean, String, Pair<Boolean, String>>(
                profileSubject,
                usernameSubject,
                BiFunction(::Pair))
                .observeOn(AndroidSchedulers.mainThread()).subscribe { (foundImage, name) ->
            refresh = false
            if (!foundImage) {
                L.e { "Could not get profile photo; Invalid userId?" }
                L._i { cookie }
            }
            textview.text = String.format(getString(R.string.welcome), name)
            textview.fadeIn()
            phaseAnswers {
                logLogin(LoginEvent()
                        .putMethod("phase_browser")
                        .putSuccess(true))
            }
            /*
             * The user may have logged into an account that is already in the database
             * We will let the db handle duplicates and load it now after the new account has been saved
             */
            loadFbCookiesAsync {
                val cookies = ArrayList(it)
                Handler().postDelayed({
                    if (Showcase.intro)
                        launchNewTask<IntroActivity>(cookies, true)
                    else
                        launchNewTask<MainActivity>(cookies, true)
                }, 1000)
            }
        }
        loadProfile(cookie.id)
        loadUsername(cookie)
    }


    private fun loadProfile(id: Long) {
        profileLoader.load(PROFILE_PICTURE_URL(id))
                .transform(PhaseGlide.roundCorner).listener(object : RequestListener<Drawable> {
            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                profileSubject.onSuccess(true)
                return false
            }

            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                e.logPhaseAnswers("Profile loading exception")
                profileSubject.onSuccess(false)
                return false
            }
        }).into(profile)
    }

    private fun loadUsername(cookie: CookieModel) {
        cookie.fetchUsername(usernameSubject::onSuccess)
    }

    override fun backConsumer(): Boolean {
        if (web.canGoBack()) {
            web.goBack()
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
    }

    override fun onPause() {
        web.pauseTimers()
        super.onPause()
    }

}