package nl.palafix.phase.contracts

import nl.palafix.phase.facebook.FbItem

/**
 * Created by Allan Wang on 19/12/17.
 **/
interface PhaseUrlData {

    /**
     * The main (and fallback) url
     */
    var baseUrl: String

    /**
     * Only base viewpager should pass an enum
     */
    var baseEnum: FbItem?

    fun passUrlDataTo(other: PhaseUrlData) {
        other.baseUrl = baseUrl
        other.baseEnum = baseEnum
    }

}