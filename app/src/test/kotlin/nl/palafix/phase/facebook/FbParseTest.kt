package nl.palafix.phase.facebook

import nl.palafix.phase.internal.COOKIE
import nl.palafix.phase.internal.assertComponentsNotEmpty
import nl.palafix.phase.internal.assertDescending
import nl.palafix.phase.internal.authDependent
import nl.palafix.phase.parsers.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Created by Allan Wang on 24/12/17.
 */
class FbParseTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun before() {
            authDependent()
        }
    }

    private inline fun <reified T : Any> PhaseParser<T>.test(action: T.() -> Unit = {}) =
            parse(COOKIE).test(url, action)

    private inline fun <reified T : Any> ParseResponse<T>?.test(url: String, action: T.() -> Unit = {}) {
        val response = this
                ?: fail("${T::class.simpleName} parser returned null for $url")
        println(response)
        response.data.action()
    }

    @Test
    fun message() = MessageParser.test {
        threads.forEach {
            it.assertComponentsNotEmpty()
            assertTrue(it.id > FALLBACK_TIME_MOD, "id may not be properly matched")
            assertNotNull(it.img, "img may not be properly matched")
        }
        threads.map(PhaseThread::time).assertDescending("thread time values")
    }

    @Test
    fun messageUser() = MessageParser.queryUser(COOKIE, "allan").test("allan query")

    @Test
    fun search() = SearchParser.test()

    @Test
    fun notif() = NotifParser.test {
        notifs.forEach {
            it.assertComponentsNotEmpty()
            assertTrue(it.id > FALLBACK_TIME_MOD, "id may not be properly matched")
            assertNotNull(it.img, "img may not be properly matched")
        }
        notifs.map(PhaseNotif::time).assertDescending("notif time values")
    }
}