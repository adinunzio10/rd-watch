package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.rdwatch.androidtv.data.converters.Converters
import com.rdwatch.androidtv.ui.filebrowser.models.FileSource
import com.rdwatch.androidtv.ui.filebrowser.models.FileTypeCategory

/**
 * Entity representing a cached account file from Real-Debrid API
 * 
 * This entity caches file information locally to reduce API calls
 * and improve performance for the file browser interface.
 */
@Entity(
    tableName = "account_files",
    indices = [
        Index(value = ["filename"]),
        Index(value = ["fileTypeCategory"]),
        Index(value = ["source"]),
        Index(value = ["dateAdded"]),
        Index(value = ["filesize"]),
        Index(value = ["isStreamable"]),
        Index(value = ["parentTorrentId"])
    ]
)
@TypeConverters(Converters::class)
data class AccountFileEntity(
    @PrimaryKey
    val id: String,
    
    val filename: String,
    val filesize: Long,
    val source: FileSource,
    val mimeType: String? = null,
    val downloadUrl: String? = null,
    val streamUrl: String? = null,
    val host: String? = null,
    val dateAdded: Long, // Unix timestamp
    val isStreamable: Boolean = false,
    
    // Torrent-specific fields
    val parentTorrentId: String? = null,
    val parentTorrentName: String? = null,
    val torrentProgress: Float? = null,
    val torrentStatus: String? = null,
    
    // File categorization
    val fileTypeCategory: FileTypeCategory,
    val fileExtension: String,
    
    // Caching metadata
    val lastUpdated: Long = System.currentTimeMillis(),
    val alternativeUrls: List<String> = emptyList()
) {
    
    /**
     * Converts entity to UI model
     */
    fun toAccountFileItem() = com.rdwatch.androidtv.ui.filebrowser.models.AccountFileItem(
        id = id,
        filename = filename,
        filesize = filesize,
        source = source,
        mimeType = mimeType,
        downloadUrl = downloadUrl,
        streamUrl = streamUrl,
        host = host,
        dateAdded = java.util.Date(dateAdded),
        isStreamable = isStreamable,
        parentTorrentId = parentTorrentId,
        parentTorrentName = parentTorrentName,
        torrentProgress = torrentProgress,
        torrentStatus = torrentStatus,
        alternativeUrls = alternativeUrls
    )
    
    /**
     * Checks if the cached data is still fresh (within TTL)
     */
    fun isFresh(ttlMillis: Long = 300_000): Boolean { // 5 minutes default TTL
        return System.currentTimeMillis() - lastUpdated < ttlMillis
    }
    
    companion object {
        /**
         * Creates entity from UI model
         */
        fun fromAccountFileItem(item: com.rdwatch.androidtv.ui.filebrowser.models.AccountFileItem): AccountFileEntity {
            return AccountFileEntity(
                id = item.id,
                filename = item.filename,
                filesize = item.filesize,
                source = item.source,
                mimeType = item.mimeType,
                downloadUrl = item.downloadUrl,
                streamUrl = item.streamUrl,
                host = item.host,
                dateAdded = item.dateAdded.time,
                isStreamable = item.isStreamable,
                parentTorrentId = item.parentTorrentId,
                parentTorrentName = item.parentTorrentName,
                torrentProgress = item.torrentProgress,
                torrentStatus = item.torrentStatus,
                fileTypeCategory = item.fileTypeCategory,
                fileExtension = item.fileExtension,
                alternativeUrls = item.alternativeUrls
            )
        }
    }
}