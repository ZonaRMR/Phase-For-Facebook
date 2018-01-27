package nl.palafix.phase.parsers

import ca.allanwang.kau.searchview.SearchItem
import nl.palafix.phase.facebook.FbItem
import nl.palafix.phase.facebook.formattedFbUrl
import nl.palafix.phase.parsers.PhaseSearch.Companion.create
import nl.palafix.phase.utils.L
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Created by Allan Wang on 2017-10-09.
 */
object SearchParser : PhaseParser<PhaseSearches> by SearchParserImpl() {
    fun query(cookie: String?, input: String): ParseResponse<PhaseSearches>? {
        val url = "${FbItem._SEARCH.url}?q=${if (input.isNotBlank()) input else "a"}"
        L._i { "Search Query $url" }
        return parseFromUrl(cookie, url)
    }
}

enum class SearchKeys(val key: String) {
    USERS("keywords_users"),
    EVENTS("keywords_events")
}

data class PhaseSearches(val results: List<PhaseSearch>) {

    override fun toString() = StringBuilder().apply {
        append("PhaseSearches {\n")
        append(results.toJsonString("results", 1))
        append("}")
    }.toString()
}

/**
 * As far as I'm aware, all links are independent, so the queries don't matter
 * A lot of it is tracking information, which I'll strip away
 * Other text items are formatted for safety
 *
 * Note that it's best to create search results from [create]
 */
data class PhaseSearch(val href: String, val title: String, val description: String?) {

    fun toSearchItem() = SearchItem(href, title, description)

    companion object {
        fun create(href: String, title: String, description: String?) = PhaseSearch(
                with(href.indexOf("?")) { if (this == -1) href else href.substring(0, this) },
                title.format(),
                description?.format()
        )
    }
}

private class SearchParserImpl : PhaseParserBase<PhaseSearches>(false) {

    override var nameRes = FbItem._SEARCH.titleId

    override val url = "${FbItem._SEARCH.url}?q=a"

    override fun parseImpl(doc: Document): PhaseSearches? {
        val container: Element = doc.getElementById("BrowseResultsContainer")
                ?: doc.getElementById("root")
                ?: return null
        /**
         *
         * Removed [data-store*=result_id]
         */
        return PhaseSearches(container.select("a.touchable[href]").filter(Element::hasText).map {
            PhaseSearch.create(it.attr("href").formattedFbUrl,
                    it.select("._uoi").first()?.text() ?: "",
                    it.select("._1tcc").first()?.text())
        }.filter { it.title.isNotBlank() })
    }

}