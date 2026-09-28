package com.example.shelfpalace.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object BarcodeLookupService {

    private const val TAG = "BarcodeLookupService"

    suspend fun lookupBarcodeTitle(barcode: String): String? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim().filter { it.isDigit() }
        if (cleanBarcode.length < 6) return@withContext null

        val stripped = cleanBarcode.trimStart('0')
        val barcodeVariations = listOf(
            cleanBarcode,
            stripped,
            stripped.padStart(13, '0'),
            stripped.padStart(12, '0'),
        ).filter { it.isNotBlank() && it.length in 6..18 }.distinct()

        // 0. Try Direct IGDB Barcode / EAN Catalog Lookup (Highest Accuracy)
        try {
            val directIgdbGame = IgdbService.searchByBarcode(cleanBarcode)
            if (directIgdbGame != null && !directIgdbGame.name.isNullOrBlank()) {
                val cleanName = extractGameNameFromWebTitle(directIgdbGame.name)
                if (cleanName.isNotBlank()) {
                    return@withContext cleanName
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct IGDB barcode lookup error", e)
        }

        val candidatesList = mutableListOf<String>()

        // 1. Try Google Web Search
        for (variant in barcodeVariations) {
            lookupGoogleWeb(variant)?.let { candidatesList.add(it) }
        }

        // 2. Try PriceCharting
        for (variant in barcodeVariations) {
            lookupPriceCharting(variant)?.let { candidatesList.add(it) }
        }

        // 3. Try OpenGTINDB
        for (variant in barcodeVariations) {
            lookupOpenGtinDb(variant)?.let { candidatesList.add(it) }
        }

        // 4. Try UPCitemdb API
        for (variant in barcodeVariations) {
            lookupUpcItemDb(variant)?.let { candidatesList.add(it) }
        }

        // 5. Try Barcodelookup.com
        for (variant in barcodeVariations) {
            lookupBarcodeLookupCom(variant)?.let { candidatesList.add(it) }
        }

        // 6. Try Go-UPC
        for (variant in barcodeVariations) {
            lookupGoUpc(variant)?.let { candidatesList.add(it) }
        }

        // 7. Try EANData
        for (variant in barcodeVariations) {
            lookupEanData(variant)?.let { candidatesList.add(it) }
        }

        // 8. Try Open Products Facts
        for (variant in barcodeVariations) {
            lookupOpenProductsFacts(variant)?.let { candidatesList.add(it) }
        }

        // 9. Try DuckDuckGo web barcode lookup
        for (variant in barcodeVariations) {
            lookupDuckDuckGoWeb(variant)?.let { candidatesList.add(it) }
        }

        if (candidatesList.isEmpty()) return@withContext null

        // Select the candidate title with the most specific tokens (highest word count)
        val bestRawTitle = candidatesList.maxByOrNull { title ->
            val cleaned = extractGameNameFromWebTitle(title)
            cleaned.split(" ").filter { it.length >= 2 }.size
        } ?: candidatesList.first()

        val finalCleanedTitle = extractGameNameFromWebTitle(bestRawTitle)
        val translatedTitle = extractGameNameFromWebTitle(translateGermanToEnglish(finalCleanedTitle))

        // 11. Query IGDB with extracted title to return official IGDB main name if matching
        try {
            val igdbResults = IgdbService.search(translatedTitle)
            val igdbGame = igdbResults.firstOrNull()
            if (igdbGame != null && !igdbGame.name.isNullOrBlank()) {
                val cleanIgdbName = extractGameNameFromWebTitle(igdbGame.name)
                if (cleanIgdbName.isNotBlank()) {
                    return@withContext cleanIgdbName
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "IGDB lookup failed for $translatedTitle", e)
        }

        return@withContext translatedTitle
    }

    private fun isValidMediaProductTitle(title: String): Boolean {
        val lower = title.lowercase().trim()
        if (lower.length < 3) return false

        val rejectedPhrases = listOf(
            "barcode lookup", "ean search", "gtin lookup", "barcode database",
            "barcode generator", "free barcode", "ean-13", "upc lookup",
            "duckduckgo", "bing", "google search", "buy online", "product search",
            "wiki", "wikipedia", "how to", "convert barcode", "scan barcode",
            "cookie", "privacy policy", "anmeldung", "login", "register"
        )
        if (rejectedPhrases.any { lower.contains(it) }) return false

        // Reject if mostly digits
        if (lower.filter { it.isDigit() }.length >= lower.length - 2) return false

        return true
    }

    private val GERMAN_TO_ENGLISH_MAP = mapOf(
        "schachmaster" to "chessmaster",
        "schach" to "chess",
        "großmeister" to "grandmaster",
        "grossmeister" to "grandmaster",
        "der herr der ringe" to "the lord of the rings",
        "herr der ringe" to "lord of the rings",
        "die eroberung" to "the lord of the rings: conquest",
        "eroberung" to "the lord of the rings: conquest",
        "conquest" to "the lord of the rings: conquest",
        "schlacht um mittelerde" to "the lord of the rings: battle for middle earth",
        "das dritte zeitalter" to "the lord of the rings: the third age",
        "die rückkehr des königs" to "the lord of the rings: return of the king",
        "die rueckkehr des koenigs" to "the lord of the rings: return of the king",
        "die zwei türme" to "the lord of the rings: the two towers",
        "die zwei tuerme" to "the lord of the rings: the two towers",
        "die gefährten" to "the lord of the rings: fellowship of the ring",
        "die gefaehrten" to "the lord of the rings: fellowship of the ring",
        "krieg der sterne" to "star wars",
        "fluch der karibik" to "pirates of the caribbean",
        "die hüter des lichts" to "rise of the guardians",
        "hueter des lichts" to "rise of the guardians",
        "moorhuhn" to "crazy chicken",
        "schlag den raab" to "beat the raab",
        "landwirtschafts simulator" to "farming simulator",
        "landwirtschafts-simulator" to "farming simulator",
        "feuerwehr simulator" to "firefighting simulator",
        "bau simulator" to "construction simulator",
        "die siedler" to "the settlers",
        "die schlümpfe" to "the smurfs",
        "die schlumpfe" to "the smurfs",
        "wickie und die starken männer" to "vicky the viking",
        "die biene maja" to "maya the bee",
        "die simpsons" to "the simpsons",
        "drachenzähmen leicht gemacht" to "how to train your dragon",
        "ich einfach unverbesserlich" to "despicable me",
        "zoomania" to "zootopia",
        "rapunzel neu verföhnt" to "tangled",
        "die eiskönigin" to "frozen",
        "alles steht kopf" to "inside out",
        "findet nemo" to "finding nemo",
        "findet dorie" to "finding dory",
        "die unglaublichen" to "the incredibles",
        "glücksbärchis" to "care bears",
        "der magische stift" to "drawn to life",
        "magische stift" to "drawn to life",
        "das geheimnisvolle dorf" to "curious village",
        "die schatulle der panik" to "diabolical box",
        "die verlorene zukunft" to "unwound future",
        "zusammen durch die zeit" to "partners in time",
        "yogi bär" to "yogi bear",
        "yogi baer" to "yogi bear",
        "spongebob schwammkopf" to "spongebob squarepants",
        "winnie puuh" to "winnie the pooh",
        "das geheimnis" to "the secret",
        "geheimnis" to "secret",
        "die rückkehr" to "the return",
        "die rueckkehr" to "the return",
        "rückkehr" to "return",
        "die rache" to "the revenge",
        "rache" to "revenge",
        "die legende" to "the legend",
        "legende" to "legend",
        "das schicksal" to "the fate",
        "schicksal" to "fate",
        "der fluch" to "the curse",
        "fluch" to "curse",
        "der krieg" to "the war",
        "krieg" to "war",
        "die schlacht" to "the battle",
        "schlacht" to "battle"
    )

    private fun toTitleCase(input: String): String {
        if (input.isBlank()) return ""
        val lowercaseWords = setOf("a", "an", "the", "and", "or", "of", "for", "in", "on", "at", "to", "with")
        return input.split(" ").joinToString(" ") { word ->
            if (word.isBlank()) ""
            else {
                val lower = word.lowercase()
                if (lowercaseWords.contains(lower) && input.indexOf(word) > 0) lower
                else lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        }
    }

    private fun translateGermanToEnglish(raw: String): String {
        var title = raw.lowercase()
        GERMAN_TO_ENGLISH_MAP.entries.sortedByDescending { it.key.length }.forEach { (german, english) ->
            val regex = Regex("(?i)\\b${Regex.escape(german)}\\b")
            if (regex.containsMatchIn(title)) {
                title = title.replace(regex, english)
            }
        }
        return toTitleCase(title)
    }

    private fun isTitleMatch(title1: String, title2: String): Boolean {
        if (title1.isBlank() || title2.isBlank()) return false

        val translated1 = translateGermanToEnglish(title1)
        val translated2 = translateGermanToEnglish(title2)

        val clean1 = translated1.lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()
        val clean2 = translated2.lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()

        if (clean1.isEmpty() || clean2.isEmpty()) return false
        if (clean1 == clean2) return true
        if (clean1.length >= 4 && clean2.length >= 4 && (clean1.contains(clean2) || clean2.contains(clean1))) return true

        val words1 = clean1.split(" ").filter { it.length >= 2 }.toSet()
        val words2 = clean2.split(" ").filter { it.length >= 2 }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) return false

        val common = words1.intersect(words2)
        val ratio = common.size.toDouble() / minOf(words1.size, words2.size)
        return ratio >= 0.50
    }

    private fun lookupGoogleWeb(barcode: String): String? {
        return try {
            val encodedQuery = URLEncoder.encode(barcode, "UTF-8")
            val url = URL("https://www.google.com/search?q=$encodedQuery&hl=en")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.connect()

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val matches = Regex("(?i)<h3[^>]*>(.*?)</h3>").findAll(html)
                for (match in matches) {
                    val rawTitle = match.groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()

                    val cleaned = extractGameNameFromWebTitle(rawTitle)
                    if (isValidMediaProductTitle(cleaned)) {
                        return cleaned
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Google web lookup error for $barcode", e)
            null
        }
    }

    private fun lookupPriceCharting(barcode: String): String? {
        return try {
            val url = URL("https://www.pricecharting.com/search-products?q=$barcode&type=videogames")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.connect()

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("(?i)<h1[^>]*id=\"product_name\"[^>]*>(.*?)</h1>").find(html)
                    ?: Regex("(?i)<td[^>]*class=\"title\"[^>]*>\\s*<a[^>]*>(.*?)</a>").find(html)
                    ?: Regex("(?i)<a[^>]*href=\"/game/[^\"]*\"[^>]*>(.*?)</a>").find(html)
                if (match != null) {
                    val rawTitle = match.groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .trim()
                    if (rawTitle.isNotBlank()) return rawTitle
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "PriceCharting lookup error for $barcode", e)
            null
        }
    }

    private fun lookupOpenGtinDb(barcode: String): String? {
        return try {
            val url = URL("https://opengtindb.org/?ean=$barcode&cmd=query&queryid=400000000")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader(Charsets.ISO_8859_1).use { it.readText() }
                if (text.contains("error=0")) {
                    val lines = text.lines()
                    val nameLine = lines.find { it.startsWith("name=") }?.substringAfter("name=")?.trim()
                    val detailLine = lines.find { it.startsWith("detailname=") }?.substringAfter("detailname=")?.trim()

                    val combined = listOfNotNull(nameLine.takeIf { !it.isNullOrBlank() }, detailLine.takeIf { !it.isNullOrBlank() }).joinToString(" ")
                    if (combined.isNotBlank()) return combined
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "OpenGTINDB lookup error for $barcode", e)
            null
        }
    }

    private fun lookupBarcodeLookupCom(barcode: String): String? {
        return try {
            val url = URL("https://www.barcodelookup.com/$barcode")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("(?i)<div[^>]*class=\"[^\"]*product-details[^\"]*\"[^>]*>\\s*<h4[^>]*>(.*?)</h4>").find(html)
                    ?: Regex("(?i)itemprop=\"name\">(.*?)</span>").find(html)
                if (match != null) {
                    val rawTitle = match.groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .trim()
                    if (rawTitle.isNotBlank()) return rawTitle
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Barcodelookup.com error for $barcode", e)
            null
        }
    }

    private fun lookupEanData(barcode: String): String? {
        return try {
            val url = URL("https://eandata.com/lookup/$barcode/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("(?i)<a[^>]*class=\"product-title\"[^>]*>(.*?)</a>").find(html)
                    ?: Regex("(?i)class=\"product-name\">(.*?)<").find(html)
                if (match != null) {
                    val rawTitle = match.groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .trim()
                    if (rawTitle.isNotBlank()) return rawTitle
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "EANData error for $barcode", e)
            null
        }
    }

    private fun lookupGoUpc(barcode: String): String? {
        return try {
            val url = URL("https://go-upc.com/search?q=$barcode")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("(?i)<h1[^>]*class=\"[^\"]*product-name[^\"]*\"[^>]*>(.*?)</h1>").find(html)
                    ?: Regex("(?i)<h2[^>]*class=\"[^\"]*product-name[^\"]*\"[^>]*>(.*?)</h2>").find(html)
                if (match != null) {
                    val rawTitle = match.groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .trim()
                    if (rawTitle.isNotBlank()) return rawTitle
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Go-UPC lookup error for $barcode", e)
            null
        }
    }

    private fun lookupUpcItemDb(barcode: String): String? {
        return try {
            val url = URL("https://api.upcitemdb.com/prod/trial/lookup?upc=$barcode")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(jsonStr)
                if (jsonObj.optString("code") == "OK") {
                    val items = jsonObj.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        val firstItem = items.getJSONObject(0)
                        return firstItem.optString("title")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "UPCitemdb lookup error for $barcode", e)
            null
        }
    }

    private fun lookupDuckDuckGoWeb(barcode: String): String? {
        return try {
            val encodedQuery = URLEncoder.encode(barcode, "UTF-8")
            val url = URL("https://html.duckduckgo.com/html/?q=$encodedQuery")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val matches = Regex("(?i)<a[^>]*class=\"result__a\"[^>]*>(.*?)</a>").findAll(html)
                for (match in matches) {
                    val rawTitle = match.groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()

                    val cleaned = extractGameNameFromWebTitle(rawTitle)
                    if (isValidMediaProductTitle(cleaned)) {
                        return cleaned
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "DuckDuckGo web lookup error for $barcode", e)
            null
        }
    }

    private fun lookupOpenProductsFacts(barcode: String): String? {
        return try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "ShelfPalace/1.0")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(jsonStr)
                if (jsonObj.optInt("status") == 1) {
                    val product = jsonObj.optJSONObject("product")
                    if (product != null) {
                        val name = product.optString("product_name").ifBlank { product.optString("product_name_en") }
                        if (name.isNotBlank()) return name
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "OpenProductsFacts error for $barcode", e)
            null
        }
    }

    fun extractGameNameFromWebTitle(rawTitle: String): String {
        if (rawTitle.isBlank()) return ""

        var title = rawTitle

        // 1. Cut at website domain / pipe / dash separators for store names
        val pipeIdx = title.indexOf('|')
        if (pipeIdx > 0) title = title.substring(0, pipeIdx)

        // Cut store domain endings like "- Amazon.de", "- MediaMarkt", "- GameStop"
        val storeSuffixRegex = Regex("(?i)\\s*[-–—|:]\\s*(amazon|ebay|gamestop|mediamarkt|saturn|otto|shop|store|kaufen|buy|preis|price|ean|upc|gtin|code|barcode|cdkeys|instant-gaming).*$")
        title = title.replace(storeSuffixRegex, "")

        // Remove redundant series/base game tags after dashes (e.g. "Drawn to Life SpongeBob - Drawn to Life")
        if (title.contains('-') || title.contains('–') || title.contains('—')) {
            val parts = title.split(Regex("[-–—]")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val clean0 = parts[0].lowercase().replace(Regex("[^a-z0-9]"), "")
                val clean1 = parts[1].lowercase().replace(Regex("[^a-z0-9]"), "")
                if (clean0.contains(clean1) && clean1.length >= 3) {
                    title = parts[0]
                } else if (clean1.contains(clean0) && clean0.length >= 3) {
                    title = parts[1]
                }
            }
        }

        // Remove German database prefixes like "Keine Informationen -", "Keine Information -", "Keine Details -", "Keine Beschreibung -"
        val germanNoInfoPrefixRegex = Regex("(?i)^\\s*(keine informationen|keine information|keine details|keine beschreibung|kein titel|ohne titel)\\s*[-–—:]\\s*")
        title = title.replace(germanNoInfoPrefixRegex, "")

        // 2. Cut internal seller SKU / hash tags (e.g. "#A 44564654635", "#12345", "#B-9988")
        val hashSkuRegex = Regex("\\s*#.*$")
        title = title.replace(hashSkuRegex, "")

        // 3. Cut trailing long numeric EAN / SKU sequences (>= 6 digits) at end of string
        val trailingBarcodeDigitsRegex = Regex("\\s+\\d{6,}\\b.*$")
        title = title.replace(trailingBarcodeDigitsRegex, "")

        // 4. Cut publisher / author / platform attribution suffixes starting with "von", "by", "für", "for"
        val attributionSuffixRegex = Regex("(?i)\\s*\\b(von|by|für|for)\\s+[A-Za-z0-9&\\s.-]+.*$")
        title = title.replace(attributionSuffixRegex, "")

        // 3. Remove brackets and parentheses containing platform, store, rating, or region info
        title = title.replace(Regex("(?i)\\[[^]]*\\]|\\([^)]*\\)"), " ")

        // 4. Strip condition, packaging, sealing, grade, and listing descriptor noise
        val descriptorNoiseRegex = Regex(
            "(?i)\\b(brand new & sealed|brand new and sealed|new & sealed|new and sealed|& sealed|and sealed|factory sealed|brand new|sealed|versiegelt|foliert|in folie|neuware|" +
            "neu & ovp|neu in ovp|mit ovp|ohne ovp|ovp|" +
            "das videospiel|das spiel zum film|das offizielle spiel|offizielles spiel|das spiel|the video game|the official game|" +
            "original game|official game|authentic game|video game|videogame|original|authentic|genuine|" +
            "cartridge only|loose cartridge|cartridge|modul nur|nur modul|modul|cib|complete in box|complete|incomplete|" +
            "game only|card only|disc only|disk only|cd only|spiel nur|nur spiel|loose|boxed|unboxed|" +
            "very good|like new|near mint|good|acceptable|mint|fair|poor|refurbished|sehr gut|wie neu|gut|akzeptabel|" +
            "mit anleitung|ohne anleitung|anleitung|manual|tested|geprüft|working|funktionstüchtig|top zustand|zustand)\\b"
        )
        title = title.replace(descriptorNoiseRegex, " ")

        // 5. Strip platform, console, rating, region, and media type noise words
        val platformNoiseRegex = Regex(
            "(?i)\\b(nintendo switch|nintendo 64|nintendo 3ds|nintendo 2ds|nintendo dsi|nintendo ds|nintendo wii u|nintendo wii|nintendo|" +
            "switch|wii u|wii|n64|new 3ds xl|new 3ds|new 2ds xl|new 2ds|3ds xl|3ds|2ds xl|2ds|dsi xl|dsi|nds|ds lite|ds|" +
            "game boy advance|game boy color|game boy|gba|gbc|gb|" +
            "playstation 5|playstation 4|playstation 3|playstation 2|playstation 1|playstation portable|playstation vita|playstation|" +
            "ps vita|psvita|psp|ps5|ps4|ps3|ps2|ps1|psx|" +
            "xbox series x s|xbox series x|xbox series s|xbox series|xbox one x|xbox one s|xbox one|xbox 360|xbox|" +
            "pc game|pc dvd|pc cd|pc|" +
            "sega genesis|sega mega drive|sega saturn|sega dreamcast|game gear|sega|atari|" +
            "4k ultra hd|4k uhd|4k|uhd|blu-ray|bluray|blu ray|dvd|cd|vinyl|lp|cassette|disc|disk|" +
            "usk 18|usk 16|usk 12|usk 6|usk 0|usk|pegi 18|pegi 16|pegi|pal|ntsc|neu|gebraucht|deutsch|german edition|import|eu import|uk import)\\b"
        )
        title = title.replace(platformNoiseRegex, " ")

        // 6. Strip generic "game", "games", "spiel", "spiele", "videogame" words (unless part of "Game of Thrones" etc.)
        val genericGameWordsRegex = Regex("(?i)\\b(video game|video games|videogame|videogames|videospiel|videospiele|game|games|spiel|spiele|software|item|product|artikel)\\b(?!\\s+of\\b)")
        title = title.replace(genericGameWordsRegex, " ")

        // 7. Remove publisher/brand prefixes if at start of string
        val publisherPrefixesRegex = Regex(
            "(?i)^\\b(bandai namco|bandai partners|bandai|namco|cd projekt red|cd projekt|warner bros|warner|square enix|capcom|ubisoft|" +
            "electronic arts|ea sports|ea|konami|thq nordic|thq|sega|koch media|plaion|bethesda|2k games|2k|take two|take-two|" +
            "atlus|blizzard|activision|microids|nacon|aerosoft|focus home|focus entertainment|deep silver|marvelous|" +
            "nis america|505 games|rebellion|snk|spike chunsoft|merge games|limited run|strictly limited)\\b[:\\s]*"
        )
        title = title.replace(publisherPrefixesRegex, "")

        // 8. Clean store keywords
        val storeKeywordsRegex = Regex("(?i)\\b(amazon|ebay|gamestop|mediamarkt|saturn|otto|shop|store|kaufen|buy|preis|price)\\b")
        title = title.replace(storeKeywordsRegex, " ")

        // Remove standalone German conjunction "und"
        val germanUndRegex = Regex("(?i)\\b(und)\\b")
        title = title.replace(germanUndRegex, " ")

        // 9. Clean dangling conjunctions (e.g. "und", "and", "&") at end of string
        val trailingConjunctionsRegex = Regex("(?i)\\s*\\b(und|and|oder|or|mit|with|&)\\s*$")
        title = title.replace(trailingConjunctionsRegex, "")

        // 10. Clean commas, brackets, quotes, punctuation noise, and double spaces (preserving colons)
        return title
            .replace(Regex("[,;\\[\\]()|_\"']"), " ")
            .replace(Regex("^[-–—\\s]+|[-–—\\s]+$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
