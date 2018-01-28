package nl.palafix.phase.fragments

import com.mikepenz.fastadapter.IItem
import nl.palafix.phase.facebook.FbCookie
import nl.palafix.phase.facebook.FbItem
import nl.palafix.phase.facebook.requests.*
import nl.palafix.phase.iitems.*
import nl.palafix.phase.parsers.PhaseNotifs
import nl.palafix.phase.parsers.NotifParser
import nl.palafix.phase.parsers.ParseResponse
import nl.palafix.phase.utils.phaseJsoup
import nl.palafix.phase.views.PhaseRecyclerView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by Allan Wang on 27/12/17.
 */
class NotificationFragment : PhaseParserFragment<PhaseNotifs, NotificationIItem>() {

    override val parser = NotifParser

    override fun getDoc(cookie: String?) = phaseJsoup(cookie, "${FbItem.NOTIFICATIONS.url}?more")

    override fun toItems(response: ParseResponse<PhaseNotifs>): List<NotificationIItem> =
            response.data.notifs.map { NotificationIItem(it, response.cookie) }

    override fun bindImpl(recyclerView: PhaseRecyclerView) {
        NotificationIItem.bindEvents(adapter)
    }
}

class MenuFragment : GenericRecyclerFragment<MenuItemData, IItem<*, *>>() {

    override fun mapper(data: MenuItemData): IItem<*, *> = when (data) {
        is MenuHeader -> MenuHeaderIItem(data)
        is MenuItem -> MenuContentIItem(data)
        is MenuFooterItem ->
            if (data.isSmall) MenuFooterSmallIItem(data)
            else MenuFooterIItem(data)
        else -> throw IllegalArgumentException("Menu item in fragment has invalid type ${data::class.java.simpleName}")
    }

    override fun bindImpl(recyclerView: PhaseRecyclerView) {
        ClickableIItemContract.bindEvents(adapter)
    }

    override fun reloadImpl(progress: (Int) -> Unit, callback: (Boolean) -> Unit) {
        doAsync {
            val cookie = FbCookie.webCookie
            progress(10)
            cookie.fbRequest({ callback(false) }) {
                progress(30)
                val data = getMenuData().invoke() ?: return@fbRequest callback(false)
                if (data.data.isEmpty()) return@fbRequest callback(false)
                progress(70)
                val items = data.flatMapValid()
                progress(90)
                uiThread { adapter.add(items) }
                callback(true)
            }
        }
    }
}