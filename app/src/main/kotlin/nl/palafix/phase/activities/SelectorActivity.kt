package nl.palafix.phase.activities

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import ca.allanwang.kau.utils.bindView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import nl.palafix.phase.R
import nl.palafix.phase.facebook.FbCookie
import nl.palafix.phase.utils.cookies
import nl.palafix.phase.utils.launchNewTask
import nl.palafix.phase.utils.setPhaseColors
import nl.palafix.phase.views.AccountItem

/**
 * Created by Allan Wang on 2017-06-04.
 **/
class SelectorActivity : BaseActivity() {

    val recycler: RecyclerView by bindView(R.id.selector_recycler)
    val adapter = FastItemAdapter<AccountItem>()
    val text: AppCompatTextView by bindView(R.id.text_select_account)
    val container: ConstraintLayout by bindView(R.id.container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter
        adapter.add(cookies().map { AccountItem(it) })
        adapter.add(AccountItem(null)) // add account
        adapter.withEventHook(object : ClickEventHook<AccountItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? = (viewHolder as? AccountItem.ViewHolder)?.v

            override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<AccountItem>, item: AccountItem) {
                if (item.cookie == null) this@SelectorActivity.launchNewTask<LoginActivity>()
                else FbCookie.switchUser(item.cookie, { launchNewTask<MainActivity>(cookies()) })
            }
        })
        setPhaseColors {
            text(text)
            background(container)
           }
    }
}