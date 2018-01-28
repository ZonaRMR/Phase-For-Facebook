package nl.palafix.phase.web

import android.net.Uri
import android.webkit.*
import ca.allanwang.kau.permissions.PERMISSION_ACCESS_FINE_LOCATION
import ca.allanwang.kau.permissions.kauRequestPermissions
import nl.palafix.phase.R
import nl.palafix.phase.contracts.ActivityContract
import nl.palafix.phase.utils.L
import nl.palafix.phase.views.PhaseWebView
import nl.palafix.phase.utils.phaseSnackbar
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject


/**
 * Created by Allan Wang on 2017-05-31.
 *
 * Collection of chrome clients
 */


/**
 * The default chrome client
 */
open class PhaseChromeClient(web: PhaseWebView) : WebChromeClient() {

    private val progress: Subject<Int> = web.parent.progressObservable
    private val title: BehaviorSubject<String> = web.parent.titleObservable
    private val activity = (web.context as? ActivityContract)
    private val context = web.context!!

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        L.v { "Chrome Console ${consoleMessage.lineNumber()}: ${consoleMessage.message()}" }
        return true
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        if (title.startsWith("http") || this.title.value == title) return
        this.title.onNext(title)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progress.onNext(newProgress)
    }

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: FileChooserParams): Boolean {
        activity?.openFileChooser(filePathCallback, fileChooserParams) ?: webView.phaseSnackbar(R.string.file_chooser_not_found)
        return activity != null
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        L.i { "Requesting geolocation" }
        context.kauRequestPermissions(PERMISSION_ACCESS_FINE_LOCATION) { granted, _ ->
            L.i { "Geolocation response received; ${if (granted) "granted" else "denied"}" }
            callback(origin, granted, true)
        }
    }
}