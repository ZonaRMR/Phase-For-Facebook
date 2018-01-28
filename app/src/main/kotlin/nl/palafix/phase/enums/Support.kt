package nl.palafix.phase.enums

import android.content.Context
import android.support.annotation.StringRes
import ca.allanwang.kau.utils.string
import nl.palafix.phase.R
import nl.palafix.phase.utils.sendPhaseEmail

/**
 * Created by Allan Wang on 2017-06-29.
 **/
enum class Support(@StringRes val title: Int) {
    FEEDBACK(R.string.feedback),
    BUG(R.string.bug_report),
    THEME(R.string.theme_issue),
    FEATURE(R.string.feature_request);

    fun sendEmail(context: Context) {
        with(context) {
            this.sendPhaseEmail("${string(R.string.phase_prefix)} ${string(title)}") {
            }
        }
    }
}