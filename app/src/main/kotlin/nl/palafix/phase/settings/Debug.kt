package nl.palafix.phase.settings

import android.content.Context
import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import ca.allanwang.kau.utils.materialDialog
import ca.allanwang.kau.utils.startActivityForResult
import ca.allanwang.kau.utils.string
import nl.palafix.phase.R
import nl.palafix.phase.activities.DebugActivity
import nl.palafix.phase.activities.SettingsActivity
import nl.palafix.phase.activities.SettingsActivity.Companion.ACTIVITY_REQUEST_DEBUG
import nl.palafix.phase.debugger.OfflineWebsite
import nl.palafix.phase.facebook.FbCookie
import nl.palafix.phase.facebook.FbItem
import nl.palafix.phase.parsers.PhaseParser
import nl.palafix.phase.parsers.MessageParser
import nl.palafix.phase.parsers.NotifParser
import nl.palafix.phase.parsers.SearchParser
import nl.palafix.phase.utils.L
import nl.palafix.phase.utils.phaseUriFromFile
import nl.palafix.phase.utils.sendPhaseEmail
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.concurrent.Future

/**
 * Created by Allan Wang on 2017-06-30.
 *
 * A sub pref section that is enabled through a hidden preference
 * Each category will load a page, extract the contents, remove private info, and create a report
 */
fun SettingsActivity.getDebugPrefs(): KPrefAdapterBuilder.() -> Unit = {

    plainText(R.string.experimental_disclaimer) {
        descRes = R.string.debug_disclaimer_info
    }

    plainText(R.string.debug_web) {
        descRes = R.string.debug_web_desc
        onClick = { this@getDebugPrefs.startActivityForResult<DebugActivity>(ACTIVITY_REQUEST_DEBUG) }
    }

    plainText(R.string.debug_parsers) {
        descRes = R.string.debug_parsers_desc
        onClick = {

            val parsers = arrayOf(NotifParser, MessageParser, SearchParser)

            materialDialog {
                items(parsers.map { string(it.nameRes) })
                itemsCallback { dialog, _, position, _ ->
                    dialog.dismiss()
                    val parser = parsers[position]
                    var attempt: Future<Unit>? = null
                    val loading = materialDialog {
                        content(parser.nameRes)
                        progress(true, 100)
                        negativeText(R.string.kau_cancel)
                        onNegative { dialog, _ ->
                            attempt?.cancel(true)
                            dialog.dismiss()
                        }
                        canceledOnTouchOutside(false)
                    }

                    attempt = loading.doAsync({
                        createEmail(parser, "Error: ${it.message}")
                    }) {
                        val data = parser.parse(FbCookie.webCookie)
                        uiThread {
                            if (it.isCancelled) return@uiThread
                            it.dismiss()
                            createEmail(parser, data?.data)
                        }
                    }
                }
            }

        }
    }
}

private fun Context.createEmail(parser: PhaseParser<*>, content: Any?) =
        sendPhaseEmail("${string(R.string.debug_report)}: ${parser::class.java.simpleName}") {
            addItem("Url", parser.url)
            addItem("Contents", "$content")
        }

private const val ZIP_NAME = "debug"

fun SettingsActivity.sendDebug(urlOrig: String) {

    val url = when {
        urlOrig.endsWith("soft=requests") -> FbItem.FRIENDS.url
        urlOrig.endsWith("soft=messages") -> FbItem.MESSAGES.url
        urlOrig.endsWith("soft=notifications") -> FbItem.NOTIFICATIONS.url
        urlOrig.endsWith("soft=search") -> "${FbItem._SEARCH.url}?q=a"
        else -> urlOrig
    }

    val downloader = OfflineWebsite(url, FbCookie.webCookie ?: "",
            DebugActivity.baseDir(this))

    val md = materialDialog {
        title(R.string.parsing_data)
        progress(false, 100)
        negativeText(R.string.kau_cancel)
        onNegative { dialog, _ -> dialog.dismiss() }
        canceledOnTouchOutside(false)
        dismissListener { downloader.cancel() }
    }

    md.doAsync {
        downloader.loadAndZip(ZIP_NAME, { progress ->
            uiThread { it.setProgress(progress) }
        }) { success ->
            uiThread {
                it.dismiss()
                if (success) {
                    val zipUri = it.context.phaseUriFromFile(
                            File(downloader.baseDir, "$ZIP_NAME.zip"))
                    L.i { "Sending debug zip with uri $zipUri" }
                    sendPhaseEmail(R.string.debug_report_email_title) {
                        addItem("Url", url)
                        addAttachment(zipUri)
                        extras = {
                            type = "application/zip"
                        }
                    }
                } else {
                    toast(R.string.error_generic)
                }
            }
        }

    }

}