package com.rdwatch.androidtv.test.factories

import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.network.models.*
import java.util.*

/**
 * Factory for creating test data objects with predefined values
 * Used across all test classes to ensure consistent test data
 */
object TestDataFactory {

    /**
     * Creates a default TorrentInfo for testing
     */
    fun createTorrentInfo(
        id: String = "test_torrent_id",
        filename: String = "Test.Movie.2023.1080p.mkv",
        hash: String = "1234567890abcdef",
        bytes: Long = 2147483648L,
        originalFilename: String? = "Original.Movie.mkv",
        host: String = "real-debrid.com",
        split: Int = 100,
        progress: Float = 100f,
        status: String = "downloaded",
        added: String = "2023-01-01T12:00:00.000Z",
        ended: String? = "2023-01-01T12:30:00.000Z",
        speed: Long? = 1000L,
        seeders: Int? = 50,
        links: List<String> = listOf("https://example.com/link1", "https://example.com/link2")
    ): TorrentInfo {
        return TorrentInfo(
            id = id,
            filename = filename,
            hash = hash,
            bytes = bytes,
            originalFilename = originalFilename,
            host = host,
            split = split,
            progress = progress,
            status = status,
            added = added,
            ended = ended,
            speed = speed,
            seeders = seeders,
            links = links
        )
    }

    /**
     * Creates a list of TorrentInfo for pagination testing
     */
    fun createTorrentInfoList(
        count: Int,
        startIndex: Int = 0,
        baseFilename: String = "Movie",
        status: String = "downloaded"
    ): List<TorrentInfo> {
        return (startIndex until startIndex + count).map { index ->
            createTorrentInfo(
                id = "torrent_$index",
                filename = "$baseFilename.$index.2023.1080p.mkv",
                hash = "hash_$index",
                bytes = 1000000L + index * 100000L,
                progress = if (status == "downloading") (index * 10f) % 100f else 100f,
                status = status,
                added = "2023-01-${String.format("%02d", (index % 28) + 1)}T12:00:00.000Z",
                ended = if (status == "downloaded") "2023-01-${String.format("%02d", (index % 28) + 1)}T12:30:00.000Z" else null,
                seeders = 10 + (index % 50),
                links = listOf("https://example.com/link_${index}_1", "https://example.com/link_${index}_2")
            )
        }
    }

    /**
     * Creates a default TorrentEntity for testing
     */
    fun createTorrentEntity(
        id: String = "test_torrent_entity_id",
        hash: String = "entity_hash_123",
        filename: String = "Test.Entity.Movie.2023.mkv",
        bytes: Long = 1073741824L,
        links: List<String> = listOf("entity_link1", "entity_link2"),
        split: Int = 100,
        progress: Float = 100f,
        status: String = "downloaded",
        added: Date = Date(),
        speed: Long? = 2000L,
        seeders: Int? = 25,
        created: Date? = null,
        ended: Date? = Date()
    ): TorrentEntity {
        return TorrentEntity(
            id = id,
            hash = hash,
            filename = filename,
            bytes = bytes,
            links = links,
            split = split,
            progress = progress,
            status = status,
            added = added,
            speed = speed,
            seeders = seeders,
            created = created,
            ended = ended
        )
    }

    /**
     * Creates a list of TorrentEntity for testing
     */
    fun createTorrentEntityList(
        count: Int,
        startIndex: Int = 0,
        status: String = "downloaded"
    ): List<TorrentEntity> {
        val baseDate = Date()
        return (startIndex until startIndex + count).map { index ->
            createTorrentEntity(
                id = "entity_$index",
                hash = "entity_hash_$index",
                filename = "Entity.Movie.$index.2023.mkv",
                bytes = 500000L + index * 50000L,
                progress = if (status == "downloading") (index * 15f) % 100f else 100f,
                status = status,
                added = Date(baseDate.time - (index * 3600000L)), // 1 hour apart
                ended = if (status == "downloaded") Date(baseDate.time - (index * 3600000L) + 1800000L) else null, // 30 min later
                seeders = 5 + (index % 100),
                links = (1..3).map { "entity_link_${index}_$it" }
            )
        }
    }

    /**
     * Creates UserInfo for API testing
     */
    fun createUserInfo(
        id: Long = 12345L,
        username: String = "testuser",
        email: String = "test@example.com",
        points: Int = 1000,
        locale: String = "en",
        avatar: String = "https://example.com/avatar.jpg",
        type: String = "premium",
        premium: Int = 1,
        expiration: String = "2024-12-31T23:59:59.000Z"
    ): UserInfo {
        return UserInfo(
            id = id,
            username = username,
            email = email,
            points = points,
            locale = locale,
            avatar = avatar,
            type = type,
            premium = premium,
            expiration = expiration
        )
    }

    /**
     * Creates UnrestrictLink for API testing
     */
    fun createUnrestrictLink(
        id: String = "unrestrict_123",
        filename: String = "test_video.mp4",
        mimeType: String = "video/mp4",
        filesize: Long = 1073741824L,
        link: String = "https://example.com/original",
        host: String = "example.com",
        chunks: Int = 8,
        crc: Long = 12345L,
        download: String = "https://download.real-debrid.com/test_video.mp4",
        streamable: Int = 1
    ): UnrestrictLink {
        return UnrestrictLink(
            id = id,
            filename = filename,
            mimeType = mimeType,
            filesize = filesize,
            link = link,
            host = host,
            chunks = chunks,
            crc = crc,
            download = download,
            streamable = streamable
        )
    }

    /**
     * Creates ContentEntity for testing (using the actual entity structure)
     */
    fun createContentEntity(
        id: Long = 0L,
        title: String = "Test Content",
        year: Int? = 2023,
        quality: String? = "1080p",
        source: com.rdwatch.androidtv.data.entities.ContentSource = com.rdwatch.androidtv.data.entities.ContentSource.REAL_DEBRID,
        realDebridId: String? = "rd_test_123",
        posterUrl: String? = "https://example.com/poster.jpg",
        backdropUrl: String? = "https://example.com/backdrop.jpg",
        description: String? = "Test description",
        duration: Int? = 120, // in minutes
        rating: Float? = 8.5f,
        genres: List<String>? = listOf("Action", "Drama"),
        cast: List<String>? = listOf("Actor 1", "Actor 2"),
        director: String? = "Test Director",
        imdbId: String? = "tt1234567",
        tmdbId: Int? = 123456,
        addedDate: Date = Date(),
        lastPlayedDate: Date? = null,
        playCount: Int = 0,
        isFavorite: Boolean = false,
        isWatched: Boolean = false
    ): com.rdwatch.androidtv.data.entities.ContentEntity {
        return com.rdwatch.androidtv.data.entities.ContentEntity(
            id = id,
            title = title,
            year = year,
            quality = quality,
            source = source,
            realDebridId = realDebridId,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            description = description,
            duration = duration,
            rating = rating,
            genres = genres,
            cast = cast,
            director = director,
            imdbId = imdbId,
            tmdbId = tmdbId,
            addedDate = addedDate,
            lastPlayedDate = lastPlayedDate,
            playCount = playCount,
            isFavorite = isFavorite,
            isWatched = isWatched
        )
    }

    /**
     * Creates a list of ContentEntity for testing downloads
     */
    fun createContentEntityList(
        count: Int,
        startIndex: Int = 0,
        source: com.rdwatch.androidtv.data.entities.ContentSource = com.rdwatch.androidtv.data.entities.ContentSource.REAL_DEBRID
    ): List<com.rdwatch.androidtv.data.entities.ContentEntity> {
        val baseDate = Date()
        return (startIndex until startIndex + count).map { index ->
            createContentEntity(
                id = 0L, // Let Room auto-generate
                title = "Test Content $index",
                description = "Description for content $index",
                source = source,
                realDebridId = if (source == com.rdwatch.androidtv.data.entities.ContentSource.REAL_DEBRID) "rd_$index" else null,
                year = 2020 + (index % 5),
                rating = 5f + (index % 5),
                addedDate = Date(baseDate.time - (index * 3600000L))
            )
        }
    }

    /**
     * Creates error response JSON for API testing
     */
    fun createErrorResponseJson(
        error: String = "bad_token",
        errorCode: Int = 401,
        errorDetails: String = "Invalid authentication token"
    ): String {
        return """
            {
                "error": "$error",
                "error_code": $errorCode,
                "error_details": "$errorDetails"
            }
        """.trimIndent()
    }

    /**
     * Creates successful API response JSON for torrents list
     */
    fun createTorrentsResponseJson(torrents: List<TorrentInfo>): String {
        val torrentJsonList = torrents.map { torrent ->
            """
                {
                    "id": "${torrent.id}",
                    "filename": "${torrent.filename}",
                    "hash": "${torrent.hash}",
                    "bytes": ${torrent.bytes},
                    "original_filename": ${torrent.originalFilename?.let { "\"$it\"" } ?: "null"},
                    "host": "${torrent.host}",
                    "split": ${torrent.split},
                    "progress": ${torrent.progress},
                    "status": "${torrent.status}",
                    "added": "${torrent.added}",
                    "ended": ${torrent.ended?.let { "\"$it\"" } ?: "null"},
                    "speed": ${torrent.speed ?: "null"},
                    "seeders": ${torrent.seeders ?: "null"},
                    "links": [${torrent.links.joinToString(",") { "\"$it\"" }}]
                }
            """.trimIndent()
        }
        
        return "[\n${torrentJsonList.joinToString(",\n")}\n]"
    }

    /**
     * Creates user info response JSON
     */
    fun createUserInfoResponseJson(userInfo: UserInfo): String {
        return """
            {
                "id": ${userInfo.id},
                "username": "${userInfo.username}",
                "email": "${userInfo.email}",
                "points": ${userInfo.points},
                "locale": "${userInfo.locale}",
                "avatar": "${userInfo.avatar}",
                "type": "${userInfo.type}",
                "premium": ${userInfo.premium},
                "expiration": "${userInfo.expiration}"
            }
        """.trimIndent()
    }

    /**
     * Creates unrestrict link response JSON
     */
    fun createUnrestrictLinkResponseJson(unrestrictLink: UnrestrictLink): String {
        return """
            {
                "id": "${unrestrictLink.id}",
                "filename": "${unrestrictLink.filename}",
                "mimeType": "${unrestrictLink.mimeType}",
                "filesize": ${unrestrictLink.filesize},
                "link": "${unrestrictLink.link}",
                "host": "${unrestrictLink.host}",
                "chunks": ${unrestrictLink.chunks},
                "crc": ${unrestrictLink.crc},
                "download": "${unrestrictLink.download}",
                "streamable": ${unrestrictLink.streamable}
            }
        """.trimIndent()
    }

    /**
     * Special test data sets for edge cases
     */
    object EdgeCases {
        
        /**
         * Creates torrents with various statuses for testing filtering
         */
        fun createMixedStatusTorrents(): List<TorrentInfo> {
            return listOf(
                createTorrentInfo(id = "downloading_1", status = "downloading", progress = 45f, ended = null),
                createTorrentInfo(id = "downloaded_1", status = "downloaded", progress = 100f),
                createTorrentInfo(id = "error_1", status = "error", progress = 0f, ended = null),
                createTorrentInfo(id = "waiting_1", status = "waiting_files_selection", progress = 0f, ended = null),
                createTorrentInfo(id = "magnet_error_1", status = "magnet_error", progress = 0f, ended = null)
            )
        }

        /**
         * Creates torrents with invalid or edge case data
         */
        fun createInvalidDataTorrents(): List<TorrentInfo> {
            return listOf(
                createTorrentInfo(
                    id = "invalid_date",
                    added = "invalid-date-format",
                    ended = "also-invalid",
                    filename = "",
                    bytes = 0L
                ),
                createTorrentInfo(
                    id = "null_values",
                    originalFilename = null,
                    ended = null,
                    speed = null,
                    seeders = null,
                    links = emptyList()
                ),
                createTorrentInfo(
                    id = "extreme_values",
                    bytes = Long.MAX_VALUE,
                    progress = 999f, // Invalid progress > 100
                    split = -1, // Invalid split
                    seeders = -10 // Invalid seeders
                )
            )
        }

        /**
         * Creates large dataset for performance testing
         */
        fun createLargeDataset(size: Int = 1000): List<TorrentInfo> {
            return createTorrentInfoList(size)
        }

        /**
         * Creates empty response data
         */
        fun createEmptyResponses(): Map<String, Any> {
            return mapOf(
                "torrents" to emptyList<TorrentInfo>(),
                "downloads" to emptyList<ContentEntity>(),
                "emptyJson" to "[]",
                "nullJson" to "null"
            )
        }
    }

    /**
     * Test scenarios for different file types and sizes
     */
    object FileTypes {
        
        fun createVideoFiles(): List<TorrentInfo> {
            return listOf(
                createTorrentInfo(id = "video_1", filename = "Movie.2023.1080p.mkv", bytes = 4294967296L),
                createTorrentInfo(id = "video_2", filename = "Series.S01E01.720p.mp4", bytes = 1073741824L),
                createTorrentInfo(id = "video_3", filename = "Documentary.4K.HDR.mkv", bytes = 8589934592L)
            )
        }

        fun createAudioFiles(): List<TorrentInfo> {
            return listOf(
                createTorrentInfo(id = "audio_1", filename = "Album.FLAC.rar", bytes = 536870912L),
                createTorrentInfo(id = "audio_2", filename = "Podcast.Episode.mp3", bytes = 52428800L),
                createTorrentInfo(id = "audio_3", filename = "Audiobook.Complete.m4a", bytes = 268435456L)
            )
        }

        fun createOtherFiles(): List<TorrentInfo> {
            return listOf(
                createTorrentInfo(id = "game_1", filename = "Game.Setup.exe", bytes = 10737418240L),
                createTorrentInfo(id = "software_1", filename = "Software.v2.0.zip", bytes = 1073741824L),
                createTorrentInfo(id = "book_1", filename = "Book.Collection.pdf", bytes = 104857600L)
            )
        }
    }
}