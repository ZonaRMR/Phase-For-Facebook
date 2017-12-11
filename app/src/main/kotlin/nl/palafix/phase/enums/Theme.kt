package nl.palafix.phase.enums

import android.graphics.Color
import android.support.annotation.StringRes
import nl.palafix.phase.R
import nl.palafix.phase.injectors.CssAssets
import nl.palafix.phase.injectors.InjectorContract
import nl.palafix.phase.utils.Prefs

/**
 * Created by Allan Wang on 2017-06-14.
 */
const val FACEBOOK_BLUE = 0xff3b5998.toInt()
const val FACEBOOK_BLACK = 0xff444444.toInt()
const val BLUE_LIGHT = 0xff5d86dd.toInt()

enum class Theme(@StringRes val textRes: Int,
                 val injector: InjectorContract,
                 private val textColorGetter: () -> Int,
                 private val accentColorGetter: () -> Int,
                 private val backgroundColorGetter: () -> Int,
                 private val headerColorGetter: () -> Int,
                 private val iconColorGetter: () -> Int) {

    DEFAULT(R.string.theme_default,
            CssAssets.FACEBOOK_MOBILE,
            { 0xde000000.toInt() },
            { FACEBOOK_BLUE },
            { 0xfffafafa.toInt() },
            { FACEBOOK_BLUE },
            { Color.WHITE }),

    GRAY(R.string.theme_gray,
            CssAssets.MATERIAL_GRAY,
            { Color.BLACK },
            { Color.GRAY},
            { Color.LTGRAY },
            { FACEBOOK_BLACK },
            { Color.WHITE }),

    DARK(R.string.theme_dark,
            CssAssets.MATERIAL_DARK,
            { Color.WHITE },
            { BLUE_LIGHT },
            { 0xff303030.toInt() },
            { 0xff2e4b86.toInt() },
            { Color.WHITE }),

    AMOLED(R.string.theme_amoled,
            CssAssets.MATERIAL_AMOLED,
            { Color.WHITE },
            { BLUE_LIGHT },
            { Color.BLACK },
            { Color.BLACK },
            { Color.WHITE }),

    GLASS(R.string.theme_glass,
            CssAssets.MATERIAL_GLASS,
            { Color.WHITE },
            { BLUE_LIGHT },
            { 0x80000000.toInt() },
            { 0xb3000000.toInt() },
            { Color.WHITE }),

    CUSTOM(R.string.theme_custom,
            CssAssets.CUSTOM,
            { Prefs.customTextColor },
            { Prefs.customAccentColor },
            { Prefs.customBackgroundColor },
            { Prefs.customHeaderColor },
            { Prefs.customIconColor });

    val textColor: Int
        get() = textColorGetter()

    val accentColor: Int
        get() = accentColorGetter()

    val bgColor: Int
        get() = backgroundColorGetter()

    val headerColor: Int
        get() = headerColorGetter()

    val iconColor: Int
        get() = iconColorGetter()

    companion object {
        val values = values() //save one instance
        operator fun invoke(index: Int) = values[index]
    }
}