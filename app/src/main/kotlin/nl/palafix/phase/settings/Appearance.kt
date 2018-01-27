package nl.palafix.phase.settings

import android.os.Build
import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import ca.allanwang.kau.kpref.activity.items.KPrefColorPicker
import ca.allanwang.kau.kpref.activity.items.KPrefSeekbar
import ca.allanwang.kau.ui.views.RippleCanvas
import ca.allanwang.kau.utils.string
import nl.palafix.phase.R
import nl.palafix.phase.activities.SettingsActivity
import nl.palafix.phase.enums.MainActivityLayout
import nl.palafix.phase.enums.Theme
import nl.palafix.phase.injectors.CssAssets
import nl.palafix.phase.utils.*
import nl.palafix.phase.utils.iab.IS_Phase_PRO
import nl.palafix.phase.views.KPrefTextSeekbar

/**
 * Created by Allan Wang on 2017-06-29.
 **/
fun SettingsActivity.getAppearancePrefs(): KPrefAdapterBuilder.() -> Unit = {

    header(R.string.theme_customization)

    text(R.string.theme, Prefs::theme, { Prefs.theme = it }) {
        onClick = {
            materialDialogThemed {
                title(R.string.theme)
                items(Theme.values()
                        .map { if (it == Theme.CUSTOM && !IS_Phase_PRO) R.string.custom_pro else it.textRes }
                        .map { string(it) })
                itemsCallbackSingleChoice(item.pref) { _, _, which, _ ->
                    if (item.pref != which) {
                        if (which == Theme.CUSTOM.ordinal && !IS_Phase_PRO) {
                            purchasePro()
                            return@itemsCallbackSingleChoice true
                        }
                        item.pref = which
                        shouldRestartMain()
                        reload()
                        setPhaseTheme(true)
                        themeExterior()
                        invalidateOptionsMenu()
                        phaseAnswersCustom("Theme", "Count" to Theme(which).name)
                    }
                    true
                }
            }
        }
        textGetter = {
            string(Theme(it).textRes)
        }
    }

    fun KPrefColorPicker.KPrefColorContract.dependsOnCustom() {
        enabler = Prefs::isCustomTheme
        onDisabledClick = { phaseSnackbar(R.string.requires_custom_theme) }
        allowCustom = true
    }

    fun invalidateCustomTheme() {
        CssAssets.CUSTOM.injector.invalidate()
    }

    colorPicker(R.string.text_color, Prefs::customTextColor, {
        Prefs.customTextColor = it
        reload()
        invalidateCustomTheme()
        shouldRestartMain()
    }) {
        dependsOnCustom()
        allowCustomAlpha = false
    }

    colorPicker(R.string.accent_color, Prefs::customAccentColor, {
        Prefs.customAccentColor = it
        reload()
        invalidateCustomTheme()
        shouldRestartMain()
    }) {
        dependsOnCustom()
        allowCustomAlpha = false
    }


    colorPicker(R.string.background_color, Prefs::customBackgroundColor, {
        Prefs.customBackgroundColor = it
        bgCanvas.ripple(it, duration = 500L)
        invalidateCustomTheme()
        setPhaseTheme(true)
        shouldRestartMain()
    }) {
        dependsOnCustom()
        allowCustomAlpha = true
    }

    colorPicker(R.string.header_color, Prefs::customHeaderColor, {
        Prefs.customHeaderColor = it
        phaseNavigationBar()
        toolbarCanvas.ripple(it, RippleCanvas.MIDDLE, RippleCanvas.END, duration = 500L)
        reload()
        shouldRestartMain()
    }) {
        dependsOnCustom()
        allowCustomAlpha = true
    }

    colorPicker(R.string.icon_color, Prefs::customIconColor, {
        Prefs.customIconColor = it
        invalidateOptionsMenu()
        shouldRestartMain()
    }) {
        dependsOnCustom()
        allowCustomAlpha = false
    }

    colorPicker(R.string.noti_color, Prefs::customNotiColor, {
        Prefs.customNotiColor = it
        reload()
        invalidateCustomTheme()
        shouldRestartMain()
    }) {
        dependsOnCustom()
        allowCustomAlpha = false
    }

    header(R.string.global_customization)

    text(R.string.main_activity_layout, Prefs::mainActivityLayoutType, { Prefs.mainActivityLayoutType = it }) {
        textGetter = { string(Prefs.mainActivityLayout.titleRes) }
        onClick = {
            materialDialogThemed {
                title(R.string.main_activity_layout_desc)
                items(MainActivityLayout.values.map { string(it.titleRes) })
                itemsCallbackSingleChoice(item.pref) { _, _, which, _ ->
                    if (item.pref != which) {
                        item.pref = which
                        shouldRestartMain()
                        phaseAnswersCustom("Main Layout", "Type" to MainActivityLayout(which).name)
                    }
                    true
                }
            }
        }
    }

    plainText(R.string.main_tabs) {
        descRes = R.string.main_tabs_desc
        onClick = { launchTabCustomizerActivity() }
    }

    list.add(KPrefTextSeekbar(
            KPrefSeekbar.KPrefSeekbarBuilder(
                    globalOptions,
                    R.string.web_text_scaling, Prefs::webTextScaling, { Prefs.webTextScaling = it; setPhaseResult(REQUEST_TEXT_ZOOM) })))


    checkbox(R.string.rounded_icons, Prefs::showRoundedIcons, {
        Prefs.showRoundedIcons = it
        setPhaseResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.rounded_icons_desc
    }

    checkbox(R.string.tint_nav, Prefs::tintNavBar, {
        Prefs.tintNavBar = it
        phaseNavigationBar()
        setPhaseResult(REQUEST_NAV)
    }) {
        descRes = R.string.tint_nav_desc
    }
}