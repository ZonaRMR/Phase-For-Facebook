package nl.palafix.phase.settings

import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import ca.allanwang.kau.logging.KL
import nl.palafix.phase.R
import nl.palafix.phase.activities.MainActivity
import nl.palafix.phase.activities.SettingsActivity
import nl.palafix.phase.utils.L
import nl.palafix.phase.utils.Prefs
import nl.palafix.phase.utils.Showcase

/**
 * Created by Allan Wang on 2017-06-29.
 */
fun SettingsActivity.getExperimentalPrefs(): KPrefAdapterBuilder.() -> Unit = {

    plainText(R.string.experimental_disclaimer) {
        descRes = R.string.experimental_disclaimer_info
    }

    checkbox(R.string.experimental_by_default, { Showcase.experimentalDefault }, { Showcase.experimentalDefault = it }) {
        descRes = R.string.experimental_by_default_desc
    }

    // Experimental content starts here ------------------


    // Experimental content ends here --------------------

    checkbox(R.string.verbose_logging, { Prefs.verboseLogging }, {
        Prefs.verboseLogging = it
        KL.debug(it)
        KL.showPrivateText = false
        L.debug(it)
        KL.showPrivateText = false
    }) {
        descRes = R.string.verbose_logging_desc
    }

    plainText(R.string.restart_frost) {
        descRes = R.string.restart_frost_desc
        onClick = { _, _, _ ->
            setFrostResult(MainActivity.REQUEST_RESTART_APPLICATION)
            finish()
            true
        }
    }
}