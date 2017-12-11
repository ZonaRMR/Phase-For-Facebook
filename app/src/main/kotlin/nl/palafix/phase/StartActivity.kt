package nl.palafix.phase

import android.os.Bundle
import ca.allanwang.kau.internal.KauBaseActivity
import nl.palafix.phase.activities.LoginActivity
import nl.palafix.phase.activities.MainActivity
import nl.palafix.phase.activities.SelectorActivity
import nl.palafix.phase.activities.TabCustomizerActivity
import nl.palafix.phase.dbflow.loadFbCookiesAsync
import nl.palafix.phase.facebook.FbCookie
import nl.palafix.phase.utils.L
import nl.palafix.phase.utils.Prefs
import nl.palafix.phase.utils.launchNewTask

/**
 * Created by Allan Wang on 2017-05-28.
 */
class StartActivity : KauBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //launchNewTask(TabCustomizerActivity::class.java)
        //return
        FbCookie.switchBackUser {
            loadFbCookiesAsync { cookies ->
                L.d("Cookies loaded at time ${System.currentTimeMillis()}", cookies.toString())
                if (cookies.isNotEmpty())
                    launchNewTask(if (Prefs.userId != -1L) MainActivity::class.java else SelectorActivity::class.java, ArrayList(cookies))
                else
                    launchNewTask(LoginActivity::class.java)
            }
        }
    }
}