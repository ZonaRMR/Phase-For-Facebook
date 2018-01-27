package nl.palafix.phase

import android.os.Bundle
import android.content.Intent
import ca.allanwang.kau.internal.KauBaseActivity
import ca.allanwang.kau.utils.startActivity
import nl.palafix.phase.activities.LoginActivity
import nl.palafix.phase.activities.MainActivity
import nl.palafix.phase.activities.SelectorActivity
import nl.palafix.phase.dbflow.loadFbCookiesAsync
import nl.palafix.phase.facebook.FbCookie
import nl.palafix.phase.utils.EXTRA_COOKIES
import nl.palafix.phase.utils.L
import nl.palafix.phase.utils.Prefs
import nl.palafix.phase.utils.launchNewTask

/**
 * Created by Allan Wang on 2017-05-28.
 */
class StartActivity : KauBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FbCookie.switchBackUser {
            loadFbCookiesAsync {
                val cookies = ArrayList(it)
                L.i { "Cookies loaded at time ${System.currentTimeMillis()}" }
                L._d { "Cookies: ${cookies.joinToString("\t")}" }
                if (cookies.isNotEmpty()) {
                    if (Prefs.userId != -1L)
                        startActivity<MainActivity>(intentBuilder = {
                            putParcelableArrayListExtra(EXTRA_COOKIES, cookies)
                            //flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    else
                        launchNewTask<SelectorActivity>(cookies)
                } else
                    launchNewTask<LoginActivity>()
            }
        }
    }
}