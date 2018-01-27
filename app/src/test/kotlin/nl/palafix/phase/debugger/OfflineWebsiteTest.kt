package nl.palafix.phase.debugger

import nl.palafix.phase.facebook.FB_URL_BASE
import nl.palafix.phase.internal.COOKIE
import org.junit.Test
import java.util.concurrent.CountDownLatch

/**
 * Created by Allan Wang on 05/01/18.
 */
class OfflineWebsiteTest {

    @Test
    fun basic() {
        val countdown = CountDownLatch(1)
        OfflineWebsite(FB_URL_BASE, COOKIE, "app/build/offline_test").load {
            println("Outcome $it")
            countdown.countDown()
        }
        countdown.await()
    }

}