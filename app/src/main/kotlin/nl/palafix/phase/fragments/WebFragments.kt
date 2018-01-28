package nl.palafix.phase.fragments

import nl.palafix.phase.R
import nl.palafix.phase.facebook.FbItem
import nl.palafix.phase.views.PhaseWebView
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

}