package nl.palafix.phase.contracts

import io.reactivex.subjects.PublishSubject
import nl.palafix.phase.fragments.BaseFragment

/**
 * All the contracts for [MainActivity]
 */
interface ActivityContract : FileChooserActivityContract

interface MainActivityContract : ActivityContract {
    val fragmentSubject: PublishSubject<Int>
    fun setTitle(res: Int)
    fun setTitle(text: CharSequence)
    fun collapseAppBar()
    fun reloadFragment(fragment: BaseFragment)
}