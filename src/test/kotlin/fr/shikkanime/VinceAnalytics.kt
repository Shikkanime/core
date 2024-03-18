package fr.shikkanime

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*

data class AnalyticSource(
    val source: String,
    val visitors: Int,
    val visits: Int,
    val pageviews: Int,
    val viewsPerVisit: Double,
    val bounceRate: Double,
    val visitDuration: Double
)

data class AnalyticPage(
    val page: String,
    val visitors: Int,
    val visits: Int,
    val pageviews: Int,
    val viewsPerVisit: Double,
    val bounceRate: Double,
    val visitDuration: Double
)

suspend fun main() {
    val httpRequest = HttpRequest()

    val baseUrl = ""
    val siteId = ""

    val response = httpRequest.get(
        "$baseUrl/api/v1/stats/breakdown?site_id=$siteId&period=6mo&property=source,page&metrics=visitors,visits,pageviews,views_per_visit,bounce_rate,visit_duration",
        mapOf(
            HttpHeaders.Authorization to "Bearer "
        )
    )

    if (response.status != HttpStatusCode.OK) {
        println("Error: ${response.status}")
        return
    }

    val asJsonArray = ObjectParser.fromJson(response.bodyAsText())
        .getAsJsonArray("results")

    val sources = asJsonArray
        .map { it.asJsonObject }
        .first { it.get("property").asString == "source" }
        .getAsJsonArray("values").map {
            val values = it.asJsonObject.get("value").asJsonObject

            AnalyticSource(
                it.asJsonObject.get("key").asString,
                values.get("visitors").asInt,
                values.get("visits").asInt,
                values.get("pageviews").asInt,
                values.get("views_per_visit").asDouble,
                values.get("bounce_rate").asDouble,
                values.get("visit_duration").asDouble
            )
        }
        .sortedWith(compareByDescending<AnalyticSource> { it.visitors }.thenByDescending { it.visits })
        .take(10)

    val pages = asJsonArray
        .map { it.asJsonObject }
        .first { it.get("property").asString == "page" }
        .getAsJsonArray("values").map {
            val values = it.asJsonObject.get("value").asJsonObject

            AnalyticPage(
                it.asJsonObject.get("key").asString,
                values.get("visitors").asInt,
                values.get("visits").asInt,
                values.get("pageviews").asInt,
                values.get("views_per_visit").asDouble,
                values.get("bounce_rate").asDouble,
                values.get("visit_duration").asDouble
            )
        }
        .sortedWith(compareByDescending<AnalyticPage> { it.visitors }.thenByDescending { it.visits })
        .take(10)

    println(sources.joinToString("\n\n${"-".repeat(25)}\n\n") {
        """
        |Source: ${it.source}
        |Visitors: ${it.visitors}
        |Visits: ${it.visits}
        |Pageviews: ${it.pageviews}
        |Views per visit: ${it.viewsPerVisit}
        |Bounce rate: ${it.bounceRate}
        |Visit duration: ${it.visitDuration}
        """.trimMargin()
    })

    println()

    println(pages.joinToString("\n\n${"-".repeat(25)}\n\n") {
        """
        |Page: ${it.page}
        |Visitors: ${it.visitors}
        |Visits: ${it.visits}
        |Pageviews: ${it.pageviews}
        |Views per visit: ${it.viewsPerVisit}
        |Bounce rate: ${it.bounceRate}
        |Visit duration: ${it.visitDuration}
        """.trimMargin()
    })
}