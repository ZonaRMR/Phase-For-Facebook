package nl.palafix.phase.fragments

import nl.palafix.phase.contracts.PhaseContentContainer
import nl.palafix.phase.contracts.PhaseContentCore
import nl.palafix.phase.contracts.PhaseContentParent
import nl.palafix.phase.contracts.MainActivityContract
import nl.palafix.phase.views.PhaseRecyclerView
import io.reactivex.disposables.Disposable

/**
 * Created by Allan Wang on 2017-11-07.
 **/

interface FragmentContract : PhaseContentContainer {

    val content: PhaseContentParent?

    /**
        * Defines whether the fragment is valid in the viewpager
       * Or if it needs to be recreated
        * May be called from any thread to toggle status
        */
       var valid: Boolean

    /**
     * Helper to retrieve the core from [content]
     */
    val core: PhaseContentCore?
        get() = content?.core

    /**
     * Specifies position in Activity's viewpager
     */
    val position: Int

    /**
     * Specifies whether if current load
     * will be fragment's first load
     *
     * Defaults to true
     */
    var firstLoad: Boolean

    /**
     * Called when the fragment is first visible
     * Typically, if [firstLoad] is true,
     * the fragment should call [reload] and make [firstLoad] false
     */
    fun firstLoadRequest()

    /**
     * Single callable action to be executed upon creation
     * Note that this call is not guaranteed
     */
    fun post(action: (fragment: FragmentContract) -> Unit)

    /**
     * Call whenever a fragment is attached so that it may listen
     * to activity emissions
     */
    fun attachMainObservable(contract: MainActivityContract): Disposable


    /**
     * Call when fragment is detached so that any existing
     * observable is disposed
     */
    fun detachMainObservable()

    /*
     * -----------------------------------------
     * Delegates
     * -----------------------------------------
     */

    fun onBackPressed(): Boolean

    fun onTabClick()


}

interface RecyclerContentContract {

    fun bind(recyclerView: PhaseRecyclerView)

    /**
     * Completely handle data reloading
     * Optional progress emission update
     * Callback returns [true] for success, [false] otherwise
     */
    fun reload(progress: (Int) -> Unit, callback: (Boolean) -> Unit)

}