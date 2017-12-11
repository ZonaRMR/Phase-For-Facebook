package nl.palafix.phase

import android.app.Activity
import android.app.Application
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import ca.allanwang.kau.logging.KL
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ApplicationVersionSignature
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import nl.palafix.phase.BuildConfig
import nl.palafix.phase.dbflow.CookiesDb
import nl.palafix.phase.dbflow.FbTabsDb
import nl.palafix.phase.dbflow.NotificationDb
import nl.palafix.phase.facebook.FbCookie
import nl.palafix.phase.services.scheduleNotifications
import nl.palafix.phase.services.setupNotificationChannels
import nl.palafix.phase.utils.FrostPglAdBlock
import nl.palafix.phase.utils.L
import nl.palafix.phase.utils.Prefs
import nl.palafix.phase.utils.Showcase
import com.raizlabs.android.dbflow.config.DatabaseConfig
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import io.fabric.sdk.android.Fabric
import java.util.*
import kotlin.reflect.KClass


/**
 * Created by Allan Wang on 2017-05-28.
 */
class FrostApp : Application() {

//    companion object {
//        fun refWatcher(c: Context) = (c.applicationContext as FrostApp).refWatcher
//    }

//    lateinit var refWatcher: RefWatcher

    private fun FlowConfig.Builder.withDatabase(name: String, klass: KClass<*>) =
            addDatabaseConfig(DatabaseConfig.builder(klass.java)
                    .databaseName(name)
                    .build())

    override fun onCreate() {
        FlowManager.init(FlowConfig.Builder(this)
                .withDatabase(CookiesDb.NAME, CookiesDb::class)
                .withDatabase(FbTabsDb.NAME, FbTabsDb::class)
                .withDatabase(NotificationDb.NAME, NotificationDb::class)
                .build())
        Showcase.initialize(this, "${BuildConfig.APPLICATION_ID}.showcase")
        Prefs.initialize(this, "${BuildConfig.APPLICATION_ID}.prefs")
        //        if (LeakCanary.isInAnalyzerProcess(this)) return
//        refWatcher = LeakCanary.install(this)
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics(), Answers())
            Crashlytics.setUserIdentifier(Prefs.frostId)
        }
        KL.debug(BuildConfig.DEBUG)
        L.debug(BuildConfig.DEBUG)
        Prefs.verboseLogging = false
        L.i("Begin Phase for Facebook")
        FbCookie()
        FrostPglAdBlock.init(this)
        if (Prefs.installDate == -1L) Prefs.installDate = System.currentTimeMillis()
        if (Prefs.identifier == -1) Prefs.identifier = Random().nextInt(Int.MAX_VALUE)
        Prefs.lastLaunch = System.currentTimeMillis()

        super.onCreate()

        applicationContext.scheduleNotifications(Prefs.notificationFreq)

        setupNotificationChannels(applicationContext)

        /**
         * Drawer profile loading logic
         * Reload the image on every version update
         */
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String) {
                val c = imageView.context
                val old = Glide.with(c).load(uri).apply(RequestOptions().placeholder(placeholder))
                Glide.with(c).load(uri).apply(RequestOptions().signature(ApplicationVersionSignature.obtain(c)))
                        .thumbnail(old).into(imageView)
            }
        })
        if (BuildConfig.DEBUG)
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityDestroyed(activity: Activity) {
                    L.d("Activity ${activity.localClassName} destroyed")
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}

                override fun onActivityStopped(activity: Activity) {}

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    L.d("Activity ${activity.localClassName} created")
                }
            })
    }


}