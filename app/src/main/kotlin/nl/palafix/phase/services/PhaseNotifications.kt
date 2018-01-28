package nl.palafix.phase.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.BaseBundle
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import ca.allanwang.kau.utils.color
import ca.allanwang.kau.utils.dpToPx
import ca.allanwang.kau.utils.string
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import nl.palafix.phase.BuildConfig
import nl.palafix.phase.R
import nl.palafix.phase.activities.PhaseWebActivity
import nl.palafix.phase.dbflow.CookieModel
import nl.palafix.phase.dbflow.NotificationModel
import nl.palafix.phase.dbflow.lastNotificationTime
import nl.palafix.phase.enums.OverlayContext
import nl.palafix.phase.facebook.FbItem
import nl.palafix.phase.glide.GlideApp
import nl.palafix.phase.glide.PhaseGlide
import nl.palafix.phase.parsers.MessageParser
import nl.palafix.phase.parsers.NotifParser
import nl.palafix.phase.parsers.ParseNotification
import nl.palafix.phase.parsers.PhaseParser
import nl.palafix.phase.utils.ARG_USER_ID
import nl.palafix.phase.utils.L
import nl.palafix.phase.utils.Prefs
import nl.palafix.phase.utils.phaseAnswersCustom
import org.jetbrains.anko.runOnUiThread
import java.util.*

/**
 * Created by Allan Wang on 2017-07-08.
 *
 * Logic for build notifications, scheduling notifications, and showing notifications
 */
fun setupNotificationChannels(c: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val mainChannel = NotificationChannel(BuildConfig.APPLICATION_ID, c.getString(R.string.phase_name), NotificationManager.IMPORTANCE_DEFAULT)
    mainChannel.lightColor = c.color(R.color.facebook_blue)
    mainChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    manager.createNotificationChannel(mainChannel)
}

inline val Context.phaseNotification: NotificationCompat.Builder
    get() = NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID).apply {
        setSmallIcon(R.drawable.phase_f_24)
        setAutoCancel(true)
        setStyle(NotificationCompat.BigTextStyle())
        color = color(R.color.phase_notification_accent)
    }

fun NotificationCompat.Builder.withDefaults(ringtone: String = Prefs.notificationRingtone) = apply {
    var defaults = 0
    if (Prefs.notificationVibrate) defaults = defaults or Notification.DEFAULT_VIBRATE
    if (Prefs.notificationSound) {
        if (ringtone.isNotBlank()) setSound(Uri.parse(ringtone))
        else defaults = defaults or Notification.DEFAULT_SOUND
    }
    if (Prefs.notificationLights) defaults = defaults or Notification.DEFAULT_LIGHTS
    setDefaults(defaults)
}

/**
 * Created by Allan Wang on 2017-07-08.
 *
 * Custom target to set the content view and update a given notification
 * 40dp is the size of the right avatar
 */
class PhaseNotificationTarget(val context: Context,
                              val notifId: Int,
                              val notifTag: String,
                              val builder: NotificationCompat.Builder
) : SimpleTarget<Bitmap>(40.dpToPx, 40.dpToPx) {

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        builder.setLargeIcon(resource)
        NotificationManagerCompat.from(context).notify(notifTag, notifId, builder.build())
    }
}

/**
 * Enum to handle notification creations
 */
enum class NotificationType(
        private val overlayContext: OverlayContext,
        private val fbItem: FbItem,
        private val parser: PhaseParser<ParseNotification>,
        private val getTime: (notif: NotificationModel) -> Long,
        private val putTime: (notif: NotificationModel, time: Long) -> NotificationModel,
        private val ringtone: () -> String) {

    GENERAL(OverlayContext.NOTIFICATION,
            FbItem.NOTIFICATIONS,
            NotifParser,
            NotificationModel::epoch,
            { notif, time -> notif.copy(epoch = time) },
            Prefs::notificationRingtone) {

        override fun bindRequest(content: NotificationContent, cookie: String) =
                PhaseRunnable.prepareMarkNotificationRead(content.id, cookie)
    },

    MESSAGE(OverlayContext.MESSAGE,
            FbItem.MESSAGES,
            MessageParser,
            NotificationModel::epochIm,
            { notif, time -> notif.copy(epochIm = time) },
            Prefs::messageRingtone);

    private val groupPrefix = "phase_${name.toLowerCase(Locale.CANADA)}"

    /**
     * Optional binder to return the request bundle builder
     */
    internal open fun bindRequest(content: NotificationContent, cookie: String): (BaseBundle.() -> Unit)? = null

    private fun bindRequest(intent: Intent, content: NotificationContent, cookie: String?) {
        cookie ?: return
        val binder = bindRequest(content, cookie) ?: return
        val bundle = Bundle()
        bundle.binder()
        intent.putExtras(bundle)
    }

    /**
     * Get unread data from designated parser
     * Display notifications for those after old epoch
     * Save new epoch
     */
    fun fetch(context: Context, data: CookieModel) {
        val response = parser.parse(data.cookie)
                ?: return L.v { "$name notification data not found" }
        val notifs = response.data.getUnreadNotifications(data).filter {
            val text = it.text
            Prefs.notificationKeywords.none { text.contains(it, true) }
        }
        if (notifs.isEmpty()) return
        var notifCount = 0
        val userId = data.id
        val prevNotifTime = lastNotificationTime(userId)
        val prevLatestEpoch = getTime(prevNotifTime)
        L.v { "Notif $name prev epoch $prevLatestEpoch" }
        var newLatestEpoch = prevLatestEpoch
        notifs.forEach { notif ->
            L.v { "Notif timestamp ${notif.timestamp}" }
            if (notif.timestamp <= prevLatestEpoch) return@forEach
            createNotification(context, notif, notifCount == 0)
            if (notif.timestamp > newLatestEpoch)
                newLatestEpoch = notif.timestamp
            notifCount++
        }
        if (newLatestEpoch > prevLatestEpoch)
            putTime(prevNotifTime, newLatestEpoch).save()
        L.d { "Notif $name new epoch ${getTime(lastNotificationTime(userId))}" }
        summaryNotification(context, userId, notifCount)
    }

    /**
     * Create and submit a new notification with the given [content]
     * If [withDefaults] is set, it will also add the appropriate sound, vibration, and light
     * Note that when we have multiple notifications coming in at once, we don't want to have defaults for all of them
     */
    private fun createNotification(context: Context, content: NotificationContent, withDefaults: Boolean) {
        with(content) {
            val intent = Intent(context, PhaseWebActivity::class.java)
            intent.data = Uri.parse(href)
            intent.putExtra(ARG_USER_ID, data.id)
            overlayContext.put(intent)
            bindRequest(intent, content, data.cookie)

            val group = "${groupPrefix}_${data.id}"
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val notifBuilder = context.phaseNotification
                    .setContentTitle(title ?: context.string(R.string.phase_name))
                    .setContentText(text)
                    .setContentIntent(pendingIntent)
                    .setCategory(Notification.CATEGORY_SOCIAL)
                    .setSubText(data.name)
                    .setGroup(group)

            if (withDefaults)
                notifBuilder.withDefaults(ringtone())

            if (timestamp != -1L) notifBuilder.setWhen(timestamp * 1000)
            L.v { "Notif load $content" }
            NotificationManagerCompat.from(context).notify(group, notifId, notifBuilder.build())

            if (profileUrl != null) {
                context.runOnUiThread {
                    //todo verify if context is valid?
                    GlideApp.with(context)
                            .asBitmap()
                            .load(profileUrl)
                            .transform(PhaseGlide.circleCrop)
                            .into(PhaseNotificationTarget(context, notifId, group, notifBuilder))
                }
            }
        }
    }

    /**
     * Create a summary notification to wrap the previous ones
     * This will always produce sound, vibration, and lights based on preferences
     * and will only show if we have at least 2 notifications
     */
    private fun summaryNotification(context: Context, userId: Long, count: Int) {
        phaseAnswersCustom("Notifications", "Type" to name, "Count" to count)
        if (count <= 1) return
        val intent = Intent(context, PhaseWebActivity::class.java)
        intent.data = Uri.parse(fbItem.url)
        intent.putExtra(ARG_USER_ID, userId)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notifBuilder = context.phaseNotification.withDefaults(ringtone())
                .setContentTitle(context.string(R.string.phase_name))
                .setContentText("$count ${context.string(fbItem.titleId)}")
                .setGroup("${groupPrefix}_$userId")
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_SOCIAL)

        NotificationManagerCompat.from(context).notify("${groupPrefix}_$userId", userId.toInt(), notifBuilder.build())
    }
}

/**
 * Notification data holder
 */
data class NotificationContent(val data: CookieModel,
                               val id: Long,
                               val href: String,
                               val title: String? = null, // defaults to phase title
                               val text: String,
                               val timestamp: Long,
                               val profileUrl: String?) {

    val notifId = Math.abs(id.toInt())

}

const val NOTIFICATION_PERIODIC_JOB = 7

/**
 * [interval] is # of min, which must be at least 15
 * returns false if an error occurs; true otherwise
 */
fun Context.scheduleNotifications(minutes: Long): Boolean {
    val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    scheduler.cancel(NOTIFICATION_PERIODIC_JOB)
    if (minutes < 0L) return true
    val serviceComponent = ComponentName(this, NotificationService::class.java)
    val builder = JobInfo.Builder(NOTIFICATION_PERIODIC_JOB, serviceComponent)
            .setPeriodic(minutes * 60000)
            .setPersisted(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY) //TODO add options
    val result = scheduler.schedule(builder.build())
    if (result <= 0) {
        L.eThrow("Notification scheduler failed")
        return false
    }
    return true
}

const val NOTIFICATION_JOB_NOW = 6

/**
 * Run notification job right now
 */
fun Context.fetchNotifications(): Boolean {
    val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val serviceComponent = ComponentName(this, NotificationService::class.java)
    val builder = JobInfo.Builder(NOTIFICATION_JOB_NOW, serviceComponent)
            .setMinimumLatency(0L)
            .setOverrideDeadline(2000L)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
    val result = scheduler.schedule(builder.build())
    if (result <= 0) {
        L.eThrow("Notification scheduler failed")
        return false
    }
    return true
}