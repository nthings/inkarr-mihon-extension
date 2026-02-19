package eu.kanade.tachiyomi.extension.all.inkarr.dto

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Series DTO representing a manga/comic series from Inkarr API
 */
@Serializable
data class SeriesDto(
    val id: Int,
    val foreignId: String? = null,
    val title: String,
    val sortTitle: String? = null,
    val cleanTitle: String? = null,
    val overview: String? = null,
    val status: String? = null,
    val mediaType: String? = null,
    val year: Int? = null,
    val publisher: String? = null,
    val genres: String? = null,
    val imageUrl: String? = null,
    val bannerUrl: String? = null,
    @SerialName("_count")
    val count: SeriesCountDto? = null,
    val volumes: List<VolumeDto>? = null,
    val chapters: List<ChapterDto>? = null,
    val creators: List<SeriesCreatorDto>? = null,
    val mediaFiles: List<MediaFileDto>? = null,
    val monitored: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    fun toSManga(baseUrl: String): SManga = SManga.create().apply {
        url = "/api/v1/series/$id"
        title = this@SeriesDto.title
        thumbnail_url = this@SeriesDto.imageUrl ?: "$baseUrl/api/v1/series/$id/cover"
        description = buildDescription()
        author = getAuthors()
        artist = getArtists()
        genre = this@SeriesDto.genres?.replace(",", ", ")
        status = parseStatus(this@SeriesDto.status)
        initialized = true
    }

    private fun buildDescription(): String = buildString {
        overview?.let { append(it) }
        if (publisher != null || year != null) {
            if (isNotEmpty()) append("\n\n")
            publisher?.let { append("Publisher: $it") }
            if (publisher != null && year != null) append(" | ")
            year?.let { append("Year: $it") }
        }
        count?.let { c ->
            if (isNotEmpty()) append("\n")
            append("Volumes: ${c.volumes} | Chapters: ${c.chapters} | Files: ${c.mediaFiles}")
        }
    }

    private fun getAuthors(): String? {
        return creators
            ?.filter { it.creator?.metadata?.role?.lowercase() in listOf("writer", "author", "story") }
            ?.mapNotNull { it.creator?.metadata?.name }
            ?.distinct()
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
    }

    private fun getArtists(): String? {
        return creators
            ?.filter { it.creator?.metadata?.role?.lowercase() in listOf("artist", "penciller", "illustrator", "art") }
            ?.mapNotNull { it.creator?.metadata?.name }
            ?.distinct()
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
    }

    private fun parseStatus(status: String?): Int = when (status?.lowercase()) {
        "ongoing", "continuing" -> SManga.ONGOING
        "ended", "completed", "complete" -> SManga.COMPLETED
        "hiatus", "on hiatus" -> SManga.ON_HIATUS
        "cancelled", "canceled" -> SManga.CANCELLED
        "licensed" -> SManga.LICENSED
        else -> SManga.UNKNOWN
    }
}

@Serializable
data class SeriesCountDto(
    val volumes: Int = 0,
    val chapters: Int = 0,
    val mediaFiles: Int = 0,
)

/**
 * Volume DTO representing a volume/tankōbon
 */
@Serializable
data class VolumeDto(
    val id: Int,
    val seriesId: Int,
    val volumeNumber: Float? = null,
    val title: String? = null,
    val overview: String? = null,
    val imageUrl: String? = null,
    val releaseDate: String? = null,
    val monitored: Boolean = false,
    val chapters: List<ChapterDto>? = null,
    val mediaFiles: List<MediaFileDto>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * Chapter DTO representing a chapter/issue
 */
@Serializable
data class ChapterDto(
    val id: Int,
    val seriesId: Int,
    val volumeId: Int? = null,
    val chapterNumber: Float? = null,
    val issueNumber: String? = null,
    val title: String? = null,
    val overview: String? = null,
    val imageUrl: String? = null,
    val releaseDate: String? = null,
    val monitored: Boolean = false,
    val mediaFiles: List<MediaFileDto>? = null,
    val volume: VolumeRefDto? = null,
    val series: SeriesRefDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    fun toSChapter(seriesId: Int): SChapter = SChapter.create().apply {
        url = "/api/v1/chapter/$id"
        name = buildChapterName()
        chapter_number = this@ChapterDto.chapterNumber ?: 0f
        date_upload = parseDate(this@ChapterDto.releaseDate ?: this@ChapterDto.createdAt)
    }

    private fun buildChapterName(): String = buildString {
        if (title != null && title.isNotBlank()) {
            if (chapterNumber != null) {
                append("Chapter ${formatNumber(chapterNumber)} - $title")
            } else {
                append(title)
            }
        } else if (chapterNumber != null) {
            append("Chapter ${formatNumber(chapterNumber)}")
            issueNumber?.let { append(" (#$it)") }
        } else if (issueNumber != null) {
            append("Issue #$issueNumber")
        } else {
            append("Chapter")
        }
    }

    private fun formatNumber(number: Float): String {
        return if (number == number.toLong().toFloat()) {
            number.toLong().toString()
        } else {
            number.toString()
        }
    }

    private fun parseDate(dateString: String?): Long {
        if (dateString == null) return 0L
        return try {
            DATE_FORMATTER.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}

@Serializable
data class VolumeRefDto(
    val id: Int,
    val volumeNumber: Float? = null,
    val title: String? = null,
)

@Serializable
data class SeriesRefDto(
    val id: Int,
    val title: String? = null,
)

/**
 * MediaFile DTO representing a CBZ/CBR file
 */
@Serializable
data class MediaFileDto(
    val id: Int,
    val seriesId: Int? = null,
    val volumeId: Int? = null,
    val chapterId: Int? = null,
    val path: String,
    val filename: String? = null,
    val size: Long? = null,
    val format: String? = null,
    val pageCount: Int? = null,
    val series: SeriesRefDto? = null,
    val volume: VolumeRefDto? = null,
    val chapter: ChapterRefDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    fun toPages(baseUrl: String, startIndex: Int = 0): List<Page> {
        val count = pageCount ?: 1
        return (0 until count).map { pageIndex ->
            Page(
                index = startIndex + pageIndex,
                imageUrl = "$baseUrl/api/v1/mediafile/$id/page/${pageIndex + 1}",
            )
        }
    }
}

@Serializable
data class ChapterRefDto(
    val id: Int,
    val chapterNumber: Float? = null,
)

/**
 * Page DTO for page list responses
 */
@Serializable
data class PageDto(
    val number: Int,
    val url: String? = null,
    val filename: String? = null,
    val mediaType: String? = null,
)

/**
 * Creator DTO for author/artist information
 */
@Serializable
data class SeriesCreatorDto(
    val id: Int? = null,
    val seriesId: Int? = null,
    val creatorId: Int? = null,
    val role: String? = null,
    val creator: CreatorDto? = null,
)

@Serializable
data class CreatorDto(
    val id: Int,
    val foreignId: String? = null,
    val metadata: CreatorMetadataDto? = null,
)

@Serializable
data class CreatorMetadataDto(
    val name: String? = null,
    val role: String? = null,
    val biography: String? = null,
    val imageUrl: String? = null,
)
