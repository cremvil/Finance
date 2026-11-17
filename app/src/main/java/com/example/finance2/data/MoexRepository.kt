package com.example.finance2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object MoexRepository {

    private const val MOEX_URL = "https://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities.json?iss.meta=off&iss.only=securities,marketdata"

    data class MoexQuote(
        val secId: String,
        val shortName: String,
        val price: Double,
        val changePercent: Double
    )

    suspend fun fetchLiveQuotes(): Map<String, MoexQuote> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<String, MoexQuote>()
        var connection: HttpURLConnection? = null

        try {
            val url = URL(MOEX_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseText)

                // 1. Parse Securities block for PREVPRICE & SHORTNAME
                val secPrices = mutableMapOf<String, Pair<String, Double>>()
                if (rootJson.has("securities")) {
                    val securitiesObj = rootJson.getJSONObject("securities")
                    val columns = securitiesObj.getJSONArray("columns")
                    val data = securitiesObj.getJSONArray("data")

                    var secIdIdx = -1
                    var shortNameIdx = -1
                    var prevPriceIdx = -1

                    for (i in 0 until columns.length()) {
                        when (columns.getString(i)) {
                            "SECID" -> secIdIdx = i
                            "SHORTNAME" -> shortNameIdx = i
                            "PREVPRICE" -> prevPriceIdx = i
                        }
                    }

                    if (secIdIdx != -1) {
                        for (i in 0 until data.length()) {
                            val row = data.getJSONArray(i)
                            val secId = row.optString(secIdIdx, "")
                            val shortName = if (shortNameIdx != -1) row.optString(shortNameIdx, "") else ""
                            val prevPrice = if (prevPriceIdx != -1) row.optDouble(prevPriceIdx, 0.0) else 0.0

                            if (secId.isNotEmpty()) {
                                secPrices[secId] = Pair(shortName, prevPrice)
                            }
                        }
                    }
                }

                // 2. Parse Marketdata block for LAST & LASTCHANGEPRCNT
                if (rootJson.has("marketdata")) {
                    val marketObj = rootJson.getJSONObject("marketdata")
                    val columns = marketObj.getJSONArray("columns")
                    val data = marketObj.getJSONArray("data")

                    var secIdIdx = -1
                    var lastIdx = -1
                    var changePrcntIdx = -1

                    for (i in 0 until columns.length()) {
                        when (columns.getString(i)) {
                            "SECID" -> secIdIdx = i
                            "LAST" -> lastIdx = i
                            "LASTCHANGEPRCNT" -> changePrcntIdx = i
                        }
                    }

                    if (secIdIdx != -1 && lastIdx != -1) {
                        for (i in 0 until data.length()) {
                            val row = data.getJSONArray(i)
                            val secId = row.optString(secIdIdx, "")
                            var lastPrice = row.optDouble(lastIdx, Double.NaN)
                            val changePrcntRaw = if (changePrcntIdx != -1) row.optDouble(changePrcntIdx, Double.NaN) else Double.NaN

                            val secMeta = secPrices[secId]
                            val prevPrice = secMeta?.second ?: 0.0
                            val shortName = secMeta?.first ?: secId

                            if (lastPrice.isNaN() || lastPrice <= 0.0) {
                                lastPrice = prevPrice
                            }

                            if (secId.isNotEmpty() && lastPrice > 0.0) {
                                var changePercent = 0.0
                                if (!changePrcntRaw.isNaN()) {
                                    changePercent = changePrcntRaw
                                } else if (prevPrice > 0.0) {
                                    changePercent = ((lastPrice - prevPrice) / prevPrice) * 100.0
                                }

                                resultMap[secId] = MoexQuote(
                                    secId = secId,
                                    shortName = shortName,
                                    price = lastPrice,
                                    changePercent = changePercent
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        resultMap
    }

    fun updateStockListWithMoexQuotes(
        originalStocks: List<Stock>,
        moexQuotes: Map<String, MoexQuote>
    ): List<Stock> {
        if (moexQuotes.isEmpty()) return originalStocks

        return originalStocks.map { stock ->
            val quote = moexQuotes[stock.ticker]
            if (quote != null) {
                val formattedPrice = if (quote.price >= 1.0) {
                    String.format(Locale.forLanguageTag("ru-RU"), "%.2f ₽", quote.price)
                } else {
                    String.format(Locale.forLanguageTag("ru-RU"), "%.4f ₽", quote.price)
                }

                val sign = if (quote.changePercent >= 0) "+" else ""
                val formattedChange = String.format(Locale.forLanguageTag("ru-RU"), "$sign%.1f%%", quote.changePercent)

                stock.copy(
                    priceValue = quote.price,
                    priceText = formattedPrice,
                    changePercentText = formattedChange,
                    isPositive = quote.changePercent >= 0
                )
            } else stock
        }
    }
}
