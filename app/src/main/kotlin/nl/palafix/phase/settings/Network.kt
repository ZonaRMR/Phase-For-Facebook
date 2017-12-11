package nl.palafix.phase.settings

import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import nl.palafix.phase.R
import nl.palafix.phase.activities.SettingsActivity
import nl.palafix.phase.utils.Prefs

/**
 * Created by Allan Wang on 2017-08-08.
 */
fun SettingsActivity.getNetworkPrefs(): KPrefAdapterBuilder.() -> Unit = {

    checkbox(R.string.network_media_on_metered, { !Prefs.loadMediaOnMeteredNetwork }, { Prefs.loadMediaOnMeteredNetwork = !it }) {
        descRes = R.string.network_media_on_metered_desc
    }

}