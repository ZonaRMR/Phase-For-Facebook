package nl.palafix.phase.views

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import ca.allanwang.kau.utils.circularReveal
import ca.allanwang.kau.utils.fadeOut
import nl.palafix.phase.contracts.PhaseContentContainer
import nl.palafix.phase.contracts.PhaseContentCore
import nl.palafix.phase.contracts.PhaseContentParent
import nl.palafix.phase.fragments.RecyclerContentContract
import nl.palafix.phase.utils.Prefs

/**
 * Created by Allan Wang on 2017-05-29.
 *
 */
class PhaseRecyclerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr),
        PhaseContentCore {

    override fun reload(animate: Boolean) = reloadBase(animate)

    override lateinit var parent: PhaseContentParent

    override val currentUrl: String
        get() = parent.baseUrl

    lateinit var recyclerContract: RecyclerContentContract

    init {
        layoutManager = LinearLayoutManager(context)
    }

    override fun bind(container: PhaseContentContainer): View {
        if (container !is RecyclerContentContract)
            throw IllegalStateException("PhaseRecyclerView must bind to a container that is a RecyclerContentContract")
        this.recyclerContract = container
        container.bind(this)
        return this
    }

    init {
        isNestedScrollingEnabled = true
    }

    var onReloadClear: () -> Unit = {}

    override fun reloadBase(animate: Boolean) {
        if (Prefs.animate) fadeOut(onFinish = onReloadClear)
        parent.refreshObservable.onNext(true)
        recyclerContract.reload({ parent.progressObservable.onNext(it) }) {
            parent.progressObservable.onNext(100)
            parent.refreshObservable.onNext(false)
            if (Prefs.animate) post { circularReveal() }
        }
    }

    override fun clearHistory() {
        // intentionally blank
    }

    override fun destroy() {
        // todo see if any
    }

    override fun onBackPressed() = false

    /**
     * If recycler is already at the top, refresh
     * Otherwise scroll to top
     */
    override fun onTabClicked() {
        val firstPosition = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        if (firstPosition == 0) reloadBase(true)
        else scrollToTop()
    }

    private fun scrollToTop() {
        stopScroll()
        smoothScrollToPosition(0)
    }

    // nothing running in background; no need to listen
    override var active: Boolean = true

    override fun reloadTheme() {
        reloadThemeSelf()
    }

    override fun reloadThemeSelf() {
        reload(false) // todo see if there's a better solution
    }

    override fun reloadTextSize() {
        reloadTextSizeSelf()
    }

    override fun reloadTextSizeSelf() {
        // todo
    }

}