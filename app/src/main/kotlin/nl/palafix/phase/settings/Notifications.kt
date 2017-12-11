package nl.palafix.phase.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import ca.allanwang.kau.kpref.activity.items.KPrefText
import ca.allanwang.kau.utils.minuteToText
import ca.allanwang.kau.utils.string
import nl.palafix.phase.R
import nl.palafix.phase.activities.SettingsActivity
import nl.palafix.phase.services.fetchNotifications
import nl.palafix.phase.services.scheduleNotifications
import nl.palafix.phase.utils.Prefs
import nl.palafix.phase.utils.frostSnackbar
import nl.palafix.phase.utils.materialDialogThemed
import nl.palafix.phase.views.Keywords


/**
 * Created by Allan Wang on 2017-06-29.
 */
fun SettingsActivity.getNotificationPrefs(): KPrefAdapterBuilder.() -> Unit = {

    text(R.string.notification_frequency, { Prefs.notificationFreq }, { Prefs.notificationFreq = it }) {
        val options = longArrayOf(-1, 15, 30, 60, 120, 180, 300, 1440, 2880)
        val texts = options.map { if (it <= 0) string(R.string.no_notifications) else minuteToText(it) }
        onClick = { _, _, item ->
            materialDialogThemed {
                title(R.string.notification_frequency)
                items(texts)
                itemsCallbackSingleChoice(options.indexOf(item.pref), { _, _, which, _ ->
                    item.pref = options[which]
                    scheduleNotifications(item.pref)
                    true
                })
            }
            true
        }
        textGetter = { minuteToText(it) }
    }

    plainText(R.string.notification_keywords) {
        descRes = R.string.notification_keywords_desc
        onClick = { _, _, _ ->
            val keywordView = Keywords(this@getNotificationPrefs)
            materialDialogThemed {
                title(R.string.notification_keywords)
                customView(keywordView, false)
                dismissListener { keywordView.save() }
                positiveText(R.string.kau_done)
            }
            true
        }
    }

    checkbox(R.string.notification_all_accounts, { Prefs.notificationAllAccounts }, { Prefs.notificationAllAccounts = it }) {
        descRes = R.string.notification_all_accounts_desc
    }

    checkbox(R.string.notification_messages, { Prefs.notificationsInstantMessages }, { Prefs.notificationsInstantMessages = it; reloadByTitle(R.string.notification_messages_all_accounts) }) {
        descRes = R.string.notification_messages_desc
    }

    checkbox(R.string.notification_messages_all_accounts, { Prefs.notificationsImAllAccounts }, { Prefs.notificationsImAllAccounts = it }) {
        descRes = R.string.notification_messages_all_accounts_desc
        enabler = { Prefs.notificationsInstantMessages }
    }

    checkbox(R.string.notification_sound, { Prefs.notificationSound }, { Prefs.notificationSound = it; reloadByTitle(R.string.notification_ringtone, R.string.message_ringtone) })

    fun KPrefText.KPrefTextContract<String>.ringtone(code: Int) {
        enabler = { Prefs.notificationSound }
        textGetter = {
            if (it.isBlank()) string(R.string.kau_default)
            else RingtoneManager.getRingtone(this@getNotificationPrefs, Uri.parse(it))
                    ?.getTitle(this@getNotificationPrefs) ?: "---" //todo figure out why this happens
        }
        onClick = { _, _, item ->
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, string(R.string.select_ringtone))
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                if (item.pref.isNotBlank())
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(item.pref))
            }
            startActivityForResult(intent, code)
            true
        }
    }

    text(R.string.notification_ringtone, { Prefs.notificationRingtone }, { Prefs.notificationRingtone = it }) {
        ringtone(SettingsActivity.REQUEST_NOTIFICATION_RINGTONE)
    }

    text(R.string.message_ringtone, { Prefs.messageRingtone }, { Prefs.messageRingtone = it }) {
        ringtone(SettingsActivity.REQUEST_MESSAGE_RINGTONE)
    }

    checkbox(R.string.notification_vibrate, { Prefs.notificationVibrate }, { Prefs.notificationVibrate = it })

    checkbox(R.string.notification_lights, { Prefs.notificationLights }, { Prefs.notificationLights = it })

    plainText(R.string.notification_fetch_now) {
        descRes = R.string.notification_fetch_now_desc
        onClick = { _, _, _ ->
            val text = if (fetchNotifications()) R.string.notification_fetch_success else R.string.notification_fetch_fail
            frostSnackbar(text)
            true
        }
    }

}