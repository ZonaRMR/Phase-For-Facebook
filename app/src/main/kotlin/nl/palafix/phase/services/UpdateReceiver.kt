package nl.palafix.phase.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.palafix.phase.utils.L
import nl.palafix.phase.utils.Prefs

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * Receiver that is triggered whenever the app updates so it can bind the notifications again
 */
class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        L.d("Phase has updated")
        context.scheduleNotifications(Prefs.notificationFreq) //Update notifications
    }

}