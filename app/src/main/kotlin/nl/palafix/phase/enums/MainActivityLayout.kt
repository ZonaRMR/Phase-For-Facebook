package nl.palafix.phase.enums

import nl.palafix.phase.R
import nl.palafix.phase.utils.Prefs

/**
 * Created by Allan Wang on 2017-08-19.
 */
enum class MainActivityLayout(
        val titleRes: Int,
        val layoutRes: Int,
        val backgroundColor: () -> Int,
        val iconColor: () -> Int) {

    TOP_BAR(R.string.top_bar,
            R.layout.activity_main,
            { Prefs.headerColor },
            { Prefs.iconColor }),

    BOTTOM_BAR(R.string.bottom_bar,
            R.layout.activity_main_bottom_tabs,
            { Prefs.headerColor },
            { Prefs.iconColor });

    companion object {
        val values = values() //save one instance
        operator fun invoke(index: Int) = values[index]
    }
}