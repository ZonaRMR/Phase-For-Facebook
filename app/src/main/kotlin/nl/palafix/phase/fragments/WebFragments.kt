package nl.palafix.phase.fragments

import android.webkit.WebView
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import nl.palafix.phase.contracts.MainFabContract
import nl.palafix.phase.R
import nl.palafix.phase.facebook.FbItem
import nl.palafix.phase.views.PhaseWebView
import nl.palafix.phase.injectors.JsActions
import nl.palafix.phase.utils.L
import nl.palafix.phase.web.PhaseWebViewClient
import nl.palafix.phase.web.PhaseWebViewClientMenu

/**
 * Created by Allan Wang on 27/12/17.
 *
 * Basic webfragment
 * Do not extend as this is always a fallback
 */
class WebFragment : BaseFragment() {

    override val layoutRes: Int = R.layout.view_content_web

    /**
     * Given a webview, output a client
     */
    fun client(web: PhaseWebView) = when (baseEnum) {
        FbItem.MENU -> PhaseWebViewClientMenu(web)
        else -> PhaseWebViewClient(web)
    }

    override fun updateFab(contract: MainFabContract) {
        L.e { "Update fab" }
        val web = core as? WebView
        if (web == null) {
            L.e { "Webview not found in fragment $baseEnum" }
            return super.updateFab(contract)
        }
        if (baseEnum.isFeed) {
            contract.showFab(GoogleMaterial.Icon.gmd_edit) {
                JsActions.CREATE_POST.inject(web)
            }
            L.e { "UPP" }
            return
        }
        super.updateFab(contract)
    }
}