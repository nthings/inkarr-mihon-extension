package eu.kanade.tachiyomi.extension.all.inkarr

import android.content.SharedPreferences
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.extension.all.inkarr.dto.ChapterDto
import eu.kanade.tachiyomi.extension.all.inkarr.dto.MediaFileDto
import eu.kanade.tachiyomi.extension.all.inkarr.dto.PageDto
import eu.kanade.tachiyomi.extension.all.inkarr.dto.SeriesDto
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import keiyoushi.utils.getPreferencesLazy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class Inkarr : HttpSource(), ConfigurableSource, UnmeteredSource {

    internal val preferences: SharedPreferences by getPreferencesLazy()

    override val name = "Inkarr"

    override val lang = "all"

    override val baseUrl by lazy { preferences.getString(PREF_ADDRESS, "")!!.removeSuffix("/") }

    override val supportsLatest = true

    // Unique ID based on source name
    override val id by lazy {
        val key = "inkarr/all/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    private val apiKey by lazy { preferences.getString(PREF_API_KEY, "")!! }

    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .set("User-Agent", "InkarrMihon/1.0")
        .also { builder ->
            if (apiKey.isNotBlank()) {
                builder.set("X-API-Key", apiKey)
            }
        }

    override val client: OkHttpClient =
        network.cloudflareClient.newBuilder()
            .dns(Dns.SYSTEM) // Don't use DNS over HTTPS as it breaks IP addressing
            .build()

    // ========================= Popular Manga =========================

    override fun popularMangaRequest(page: Int): Request {
        val url = "$baseUrl/api/v1/series"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("includeChapters", "false")
            .addQueryParameter("includeVolumes", "false")
            .build()

        return GET(url, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val seriesList = response.parseAs<List<SeriesDto>>()

        val mangaList = seriesList.map { it.toSManga(baseUrl) }

        return MangasPage(mangaList, hasNextPage = false)
    }

    // ========================= Latest Updates =========================

    override fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(page)

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    // ========================= Search =========================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/api/v1/series"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("includeChapters", "false")
            .addQueryParameter("includeVolumes", "false")
            .build()

        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val seriesList = response.parseAs<List<SeriesDto>>()
        val query = response.request.url.queryParameter("q")?.lowercase() ?: ""

        val filteredList = if (query.isNotBlank()) {
            seriesList.filter { series ->
                series.title.lowercase().contains(query) ||
                    series.sortTitle?.lowercase()?.contains(query) == true
            }
        } else {
            seriesList
        }

        val mangaList = filteredList.map { it.toSManga(baseUrl) }

        return MangasPage(mangaList, hasNextPage = false)
    }

    // ========================= Manga Details =========================

    override fun getMangaUrl(manga: SManga): String {
        val id = manga.url.substringAfterLast("/")
        return "$baseUrl/series/$id"
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val url = "$baseUrl${manga.url}"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("includeCreators", "true")
            .build()

        return GET(url, headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val series = response.parseAs<SeriesDto>()
        return series.toSManga(baseUrl)
    }

    // ========================= Chapter List =========================

    override fun getChapterUrl(chapter: SChapter): String {
        return "$baseUrl/series/${chapter.url.split("/")[2]}"
    }

    override fun chapterListRequest(manga: SManga): Request {
        val url = "$baseUrl${manga.url}"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("includeChapters", "true")
            .addQueryParameter("includeMediaFiles", "true")
            .build()

        return GET(url, headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val series = response.parseAs<SeriesDto>()
        val chapters = mutableListOf<SChapter>()

        // Process chapters
        series.chapters?.sortedByDescending { it.chapterNumber ?: 0f }?.forEach { chapter ->
            chapters.add(chapter.toSChapter(series.id))
        }

        // If no chapters, create chapters from media files
        if (chapters.isEmpty() && !series.mediaFiles.isNullOrEmpty()) {
            series.mediaFiles.forEachIndexed { index, mediaFile ->
                chapters.add(
                    SChapter.create().apply {
                        url = "/api/v1/mediafile/${mediaFile.id}"
                        name = mediaFile.filename ?: "File ${index + 1}"
                        chapter_number = (index + 1).toFloat()
                        date_upload = parseDate(mediaFile.createdAt)
                    },
                )
            }
        }

        return chapters.sortedByDescending { it.chapter_number }
    }

    // ========================= Page List =========================

    override fun pageListRequest(chapter: SChapter): Request {
        // Get chapter details which includes mediaFiles
        val url = "$baseUrl${chapter.url}"
            .toHttpUrl()
            .newBuilder()
            .build()

        return GET(url, headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val requestUrl = response.request.url.toString()

        return if (requestUrl.contains("/api/v1/mediafile/")) {
            // It's a media file, return pages from the CBZ
            val mediaFile = response.parseAs<MediaFileDto>()
            mediaFile.toPages(baseUrl)
        } else if (requestUrl.contains("/api/v1/chapter/")) {
            // It's a chapter, get pages from associated media files
            val chapter = response.parseAs<ChapterDto>()
            chapter.mediaFiles?.flatMapIndexed { fileIndex, mediaFile ->
                mediaFile.toPages(baseUrl, fileIndex * 1000)
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    override fun imageUrlParse(response: Response): String =
        throw UnsupportedOperationException("imageUrlParse is not used")

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headersBuilder().add("Accept", "image/*,*/*;q=0.8").build())
    }

    // ========================= Preferences =========================

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        EditTextPreference(screen.context).apply {
            key = PREF_ADDRESS
            title = "Server address"
            summary = if (baseUrl.isBlank()) "Enter your Inkarr server URL" else baseUrl
            dialogTitle = "Server Address"
            dialogMessage = "Enter the URL of your Inkarr server (e.g., http://192.168.1.100:3000)"

            setDefaultValue("")
            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            }
            setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as String
                val isValid = value.isBlank() || (value.toHttpUrlOrNull() != null && !value.endsWith("/"))
                if (!isValid) {
                    Toast.makeText(screen.context, "Invalid URL. Must not end with '/'", Toast.LENGTH_LONG).show()
                } else if (value.isNotBlank()) {
                    Toast.makeText(screen.context, "Restart Tachiyomi to apply changes", Toast.LENGTH_LONG).show()
                }
                isValid
            }
        }.also(screen::addPreference)

        EditTextPreference(screen.context).apply {
            key = PREF_API_KEY
            title = "API Key"
            summary = if (apiKey.isBlank()) "Enter your Inkarr API key (optional)" else "*".repeat(apiKey.length)
            dialogTitle = "API Key"
            dialogMessage = "Enter your Inkarr API key for authentication"

            setDefaultValue("")
            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(screen.context, "Restart Tachiyomi to apply changes", Toast.LENGTH_LONG).show()
                true
            }
        }.also(screen::addPreference)
    }

    // ========================= Utilities =========================

    private inline fun <reified T> Response.parseAs(): T = json.decodeFromString(body.string())

    private fun parseDate(dateString: String?): Long {
        if (dateString == null) return 0L
        return try {
            DATE_FORMATTER.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to parse date: $dateString", e)
            0L
        }
    }

    companion object {
        private const val LOG_TAG = "Inkarr"
        private const val PREF_ADDRESS = "server_address"
        private const val PREF_API_KEY = "api_key"

        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
