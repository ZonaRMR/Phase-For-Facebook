package nl.palafix.phase.utils

import android.graphics.Color
import ca.allanwang.kau.kotlin.lazyResettable
import ca.allanwang.kau.kpref.KPref
import ca.allanwang.kau.kpref.StringSet
import ca.allanwang.kau.kpref.kpref
import ca.allanwang.kau.utils.isColorVisibleOn
import nl.palafix.phase.enums.FACEBOOK_BLUE
import nl.palafix.phase.enums.FeedSort
import nl.palafix.phase.enums.MainActivityLayout
import nl.palafix.phase.enums.Theme
import nl.palafix.phase.injectors.InjectorContract

/**
 * Created by Allan Wang on 2017-05-28.
 *
 * Shared Preference object with lazy cached retrievals
 */
object Prefs : KPref() {

    var lastLaunch: Long by kpref("last_launch", -1L)

    var userId: Long by kpref("user_id", -1L)

    var prevId: Long by kpref("prev_id", -1L)

    var theme: Int by kpref("theme", 0, postSetter = { _: Int -> loader.invalidate() })

    var customTextColor: Int by kpref("color_text", 0xffeceff1.toInt())

    var customAccentColor: Int by kpref("color_accent", 0xff0288d1.toInt())

    var customBackgroundColor: Int by kpref("color_bg", 0xff212121.toInt())

    var customHeaderColor: Int by kpref("color_header", 0xff01579b.toInt())

    var customIconColor: Int by kpref("color_icons", 0xffeceff1.toInt())

    var exitConfirmation: Boolean by kpref("exit_confirmation", true)

    var notificationFreq: Long by kpref("notification_freq", 60L)

    var versionCode: Int by kpref("version_code", -1)

    var installDate: Long by kpref("install_date", -1L)

    var identifier: Int by kpref("identifier", -1)

    private val loader = lazyResettable { Theme.values[Prefs.theme] }

    private val t: Theme by loader

    val textColor: Int
        get() = t.textColor

    val accentColor: Int
        get() = t.accentColor

    val accentColorForWhite: Int
        get() = if (accentColor.isColorVisibleOn(Color.WHITE)) accentColor
        else if (textColor.isColorVisibleOn(Color.WHITE)) textColor
        else FACEBOOK_BLUE

    val bgColor: Int
        get() = t.bgColor

    val headerColor: Int
        get() = t.headerColor

    val iconColor: Int
        get() = t.iconColor

    val themeInjector: InjectorContract
        get() = t.injector

    val isCustomTheme: Boolean
        get() = t == Theme.CUSTOM

    val frostId: String
        get() = "${installDate}-${identifier}"

    var tintNavBar: Boolean by kpref("tint_nav_bar", false)

    var webTextScaling: Int by kpref("web_text_scaling", 100)

    var feedSort: Int by kpref("feed_sort", FeedSort.MOST_RECENT.ordinal)

    var aggressiveRecents: Boolean by kpref("aggressive_recents", false)

    var showRoundedIcons: Boolean by kpref("rounded_icons", true)

    var showComposer: Boolean by kpref("status_composer_feed", true)

    var showSuggestedFriends: Boolean by kpref("suggested_friends_feed", true)

    var showSuggestedGroups: Boolean by kpref("suggested_groups_feed", true)

    var showFacebookAds: Boolean by kpref("facebook_ads", true)

    var animate: Boolean by kpref("fancy_animations", true)

    var notificationKeywords: StringSet by kpref("notification_keywords", mutableSetOf<String>())

    var notificationAllAccounts: Boolean by kpref("notification_all_accounts", true)

    var notificationsInstantMessages: Boolean by kpref("notification_im", true)

    var notificationsImAllAccounts: Boolean by kpref("notification_im_all_accounts", false)

    var notificationVibrate: Boolean by kpref("notification_vibrate", true)

    var notificationSound: Boolean by kpref("notification_sound", true)

    var notificationRingtone: String by kpref("notification_ringtone", "")

    var messageRingtone: String by kpref("message_ringtone", "")

    var notificationLights: Boolean by kpref("notification_lights", true)

    var messageScrollToBottom: Boolean by kpref("message_scroll_to_bottom", false)

    /**
     * Cache like value to determine if user has or had pro
     * In most cases, [nl.palafix.phase.utils.iab.IS_FROST_PRO] should be looked at instead
     * This has been renamed to pro for short, but keep in mind that it only reflects the value
     * of when it was previously verified
     */
    var pro: Boolean by kpref("previously_pro", false)

    var profree: Boolean by kpref("free_pro", false)

    var debugPro: Boolean by kpref("debug_pro", false)

    var verboseLogging: Boolean by kpref("verbose_logging", false)

    var analytics: Boolean by kpref("analytics", true)

    var overlayEnabled: Boolean by kpref("overlay_enabled", true)

    var overlayFullScreenSwipe: Boolean by kpref("overlay_full_screen_swipe", true)

    var viewpagerSwipe: Boolean by kpref("viewpager_swipe", true)

    var loadMediaOnMeteredNetwork: Boolean by kpref("media_on_metered_network", true)

    var debugSettings: Boolean by kpref("debug_settings", true)

    var linksInDefaultApp: Boolean by kpref("link_in_default_app", false)

    var mainActivityLayoutType: Int by kpref("main_activity_layout_type", 0)

    val mainActivityLayout: MainActivityLayout
        get() = MainActivityLayout(mainActivityLayoutType)

    override fun deleteKeys() = arrayOf("search_bar")
}