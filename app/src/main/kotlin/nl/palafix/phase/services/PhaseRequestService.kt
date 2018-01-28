package nl.palafix.phase.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BaseBundle
import android.os.PersistableBundle
import nl.palafix.phase.facebook.requests.RequestAuth
import nl.palafix.phase.facebook.requests.fbRequest
import nl.palafix.phase.facebook.requests.markNotificationRead
import nl.palafix.phase.utils.EnumBundle
import nl.palafix.phase.utils.EnumBundleCompanion
import nl.palafix.phase.utils.EnumCompanion
import nl.palafix.phase.utils.L
import org.jetbrains.anko.doAsync
import java.util.concurrent.Future

/**
 * Created by Allan Wang on 28/12/17.
 */

/**
 * Private helper data
 */
private enum class PhaseRequestCommands : EnumBundle<PhaseRequestCommands> {

    NOTIF_READ {

        override fun invoke(auth: RequestAuth, bundle: PersistableBundle) {
            val id = bundle.getLong(ARG_0, -1L)
            val success = auth.markNotificationRead(id).invoke()
            L.d { "Marked notif $id as read: $success" }
        }

        override fun propagate(bundle: BaseBundle) =
                PhaseRunnable.prepareMarkNotificationRead(
                        bundle.getLong(ARG_0),
                        bundle.getCookie())

    };

    override val bundleContract: EnumBundleCompanion<PhaseRequestCommands>
        get() = Companion

    /**
     * Call request with arguments inside bundle
     */
    abstract fun invoke(auth: RequestAuth, bundle: PersistableBundle)

    /**
     * Return bundle builder given arguments in the old bundle
     * Must not write to old bundle!
     */
    abstract fun propagate(bundle: BaseBundle): BaseBundle.() -> Unit

    companion object : EnumCompanion<PhaseRequestCommands>("phase_arg_commands", values())

}

private const val ARG_COMMAND = "phase_request_command"
private const val ARG_COOKIE = "phase_request_cookie"
private const val ARG_0 = "phase_request_arg_0"
private const val ARG_1 = "phase_request_arg_1"
private const val ARG_2 = "phase_request_arg_2"
private const val ARG_3 = "phase_request_arg_3"
private const val JOB_REQUEST_BASE = 928

private fun BaseBundle.getCookie() = getString(ARG_COOKIE)
private fun BaseBundle.putCookie(cookie: String) = putString(ARG_COOKIE, cookie)

/**
 * Singleton handler for running requests in [PhaseRequestService]
 * Requests are typically completely decoupled from the UI,
 * and are optional enhancers.
 *
 * Nothing guarantees the completion time, or whether it even executes at all
 *
 * Design:
 * prepare function - creates a bundle binder
 * actor function   - calls the service with the given arguments
 *
 * Global:
 * propagator       - given a bundle with a command, extracts and executes the requests
 */
object PhaseRunnable {

    fun prepareMarkNotificationRead(id: Long, cookie: String): BaseBundle.() -> Unit = {
        PhaseRequestCommands.NOTIF_READ.put(this)
        putLong(ARG_0, id)
        putCookie(cookie)
    }

    fun markNotificationRead(context: Context, id: Long, cookie: String): Boolean {
        if (id <= 0) {
            L.d { "Invalid notification id $id for marking as read" }
            return false
        }
        return schedule(context, PhaseRequestCommands.NOTIF_READ,
                prepareMarkNotificationRead(id, cookie))
    }

    fun propagate(context: Context, intent: Intent?) {
        intent?.extras ?: return
        val command = PhaseRequestCommands[intent] ?: return
        intent.removeExtra(ARG_COMMAND) // reset
        L.d { "Propagating command ${command.name}" }
        val builder = command.propagate(intent.extras)
        schedule(context, command, builder)
    }

    private fun schedule(context: Context,
                         command: PhaseRequestCommands,
                         bundleBuilder: PersistableBundle.() -> Unit): Boolean {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val serviceComponent = ComponentName(context, PhaseRequestService::class.java)
        val bundle = PersistableBundle()
        bundle.bundleBuilder()
        bundle.putString(ARG_COMMAND, command.name)

        if (bundle.getCookie().isNullOrBlank()) {
            L.e { "Scheduled phase request with empty cookie" }
            return false
        }

        val builder = JobInfo.Builder(JOB_REQUEST_BASE + command.ordinal, serviceComponent)
                .setMinimumLatency(0L)
                .setExtras(bundle)
                .setOverrideDeadline(2000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        val result = scheduler.schedule(builder.build())
        if (result <= 0) {
            L.eThrow("PhaseRequestService scheduler failed for ${command.name}")
            return false
        }
        L.d { "Scheduled ${command.name}" }
        return true
    }

}

class PhaseRequestService : JobService() {

    var future: Future<Unit>? = null

    override fun onStopJob(params: JobParameters?): Boolean {
        future?.cancel(true)
        future = null
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val bundle = params?.extras
        if (bundle == null) {
            L.eThrow("Launched ${this::class.java.simpleName} without param data")
            return false
        }
        val cookie = bundle.getCookie()
        if (cookie.isNullOrBlank()) {
            L.eThrow("Launched ${this::class.java.simpleName} without cookie")
            return false
        }
        val command = PhaseRequestCommands[bundle]
        if (command == null) {
            L.eThrow("Launched ${this::class.java.simpleName} without command")
            return false
        }

        future = doAsync {
            val now = System.currentTimeMillis()
            var failed = true
            cookie.fbRequest {
                L.d { "Requesting phase service for ${command.name}" }
                command.invoke(this, bundle)
                failed = false
            }
            L.d {
                "${if (failed) "Failed" else "Finished"} phase service for ${command.name} in ${System.currentTimeMillis() - now} ms"
            }
            jobFinished(params, false)
        }
        return true
    }
}