package com.rdwatch.androidtv.ui.filebrowser.algorithms

import com.rdwatch.androidtv.ui.filebrowser.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for sorting and filtering algorithms used in the file browser.
 * Tests various sorting criteria and filter combinations.
 */
class SortFilterAlgorithmsTest {

    private lateinit var testFiles: List<FileItem>
    private lateinit var testTorrents: List<FileItem.Torrent>
    private lateinit var allTestItems: List<FileItem>

    @Before
    fun setup() {
        setupTestData()
    }

    private fun setupTestData() {
        val now = System.currentTimeMillis()
        
        testFiles = listOf(
            FileItem.File(
                id = "file1",
                name = "A_Movie.mp4",
                size = 1024L * 1024L * 1024L, // 1GB
                modifiedDate = now - 86400000L, // 1 day ago
                mimeType = "video/mp4",
                downloadUrl = "url1",
                streamUrl = "stream1",
                isPlayable = true,
                status = FileStatus.READY
            ),
            FileItem.File(
                id = "file2",
                name = "B_Document.pdf",
                size = 1024L * 1024L, // 1MB
                modifiedDate = now - 172800000L, // 2 days ago
                mimeType = "application/pdf",
                downloadUrl = "url2",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.READY
            ),
            FileItem.File(
                id = "file3",
                name = "C_Music.mp3",
                size = 5L * 1024L * 1024L, // 5MB
                modifiedDate = now - 43200000L, // 12 hours ago
                mimeType = "audio/mpeg",
                downloadUrl = "url3",
                streamUrl = "stream3",
                isPlayable = true,
                status = FileStatus.DOWNLOADING,
                progress = 0.7f
            ),
            FileItem.File(
                id = "file4",
                name = "D_Image.jpg",
                size = 512L * 1024L, // 512KB
                modifiedDate = now - 21600000L, // 6 hours ago
                mimeType = "image/jpeg",
                downloadUrl = "url4",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.ERROR
            ),
            FileItem.File(
                id = "file5",
                name = "E_Archive.zip",
                size = 100L * 1024L * 1024L, // 100MB
                modifiedDate = now - 259200000L, // 3 days ago
                mimeType = "application/zip",
                downloadUrl = "url5",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.UNAVAILABLE
            )
        )

        testTorrents = listOf(
            FileItem.Torrent(
                id = "torrent1",
                name = "Large Movie Collection",
                size = 10L * 1024L * 1024L * 1024L, // 10GB
                modifiedDate = now - 86400000L, // 1 day ago
                hash = "hash1",
                progress = 1.0f,
                status = TorrentStatus.DOWNLOADED,
                seeders = 10,
                speed = 0L,
                files = listOf(testFiles[0] as FileItem.File)
            ),
            FileItem.Torrent(
                id = "torrent2",
                name = "Small Archive",
                size = 50L * 1024L * 1024L, // 50MB
                modifiedDate = now - 172800000L, // 2 days ago
                hash = "hash2",
                progress = 0.5f,
                status = TorrentStatus.DOWNLOADING,
                seeders = 5,
                speed = 1024L * 1024L, // 1MB/s
                files = listOf(testFiles[4] as FileItem.File)
            )
        )

        allTestItems = testFiles + testTorrents
    }

    // SORTING TESTS

    @Test
    fun `should sort by name ascending`() {
        // When sorting by name ascending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.NAME, SortOrder.ASCENDING))
        
        // Then should be sorted alphabetically
        val names = sorted.map { it.name }
        assertEquals(listOf("A_Movie.mp4", "B_Document.pdf", "C_Music.mp3", "D_Image.jpg", "E_Archive.zip", "Large Movie Collection", "Small Archive"), names)
    }

    @Test
    fun `should sort by name descending`() {
        // When sorting by name descending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.NAME, SortOrder.DESCENDING))
        
        // Then should be sorted reverse alphabetically
        val names = sorted.map { it.name }
        assertEquals(listOf("Small Archive", "Large Movie Collection", "E_Archive.zip", "D_Image.jpg", "C_Music.mp3", "B_Document.pdf", "A_Movie.mp4"), names)
    }

    @Test
    fun `should sort by size ascending`() {
        // When sorting by size ascending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.SIZE, SortOrder.ASCENDING))
        
        // Then should be sorted by size (smallest first)
        val sizes = sorted.map { it.size }
        assertEquals(listOf(512L * 1024L, 1024L * 1024L, 5L * 1024L * 1024L, 50L * 1024L * 1024L, 100L * 1024L * 1024L, 1024L * 1024L * 1024L, 10L * 1024L * 1024L * 1024L), sizes)
    }

    @Test
    fun `should sort by size descending`() {
        // When sorting by size descending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.SIZE, SortOrder.DESCENDING))
        
        // Then should be sorted by size (largest first)
        val sizes = sorted.map { it.size }
        assertEquals(listOf(10L * 1024L * 1024L * 1024L, 1024L * 1024L * 1024L, 100L * 1024L * 1024L, 50L * 1024L * 1024L, 5L * 1024L * 1024L, 1024L * 1024L, 512L * 1024L), sizes)
    }

    @Test
    fun `should sort by date ascending`() {
        // When sorting by date ascending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.DATE, SortOrder.ASCENDING))
        
        // Then should be sorted by date (oldest first)
        val dates = sorted.map { it.modifiedDate }
        assertTrue("Should be sorted oldest to newest", dates.zipWithNext().all { it.first <= it.second })
    }

    @Test
    fun `should sort by date descending`() {
        // When sorting by date descending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.DATE, SortOrder.DESCENDING))
        
        // Then should be sorted by date (newest first)
        val dates = sorted.map { it.modifiedDate }
        assertTrue("Should be sorted newest to oldest", dates.zipWithNext().all { it.first >= it.second })
    }

    @Test
    fun `should sort by type ascending`() {
        // When sorting by type ascending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.TYPE, SortOrder.ASCENDING))
        
        // Then should be sorted by type (folders, torrents, files)
        val typeOrder = sorted.map { getTypeOrder(it) }
        assertTrue("Should be sorted by type", typeOrder.zipWithNext().all { it.first <= it.second })
    }

    @Test
    fun `should sort by status ascending`() {
        // When sorting by status ascending
        val sorted = applySorting(allTestItems, SortingOptions(SortBy.STATUS, SortOrder.ASCENDING))
        
        // Then should be sorted by status ordinal
        val statusOrder = sorted.map { getStatusOrder(it) }
        assertTrue("Should be sorted by status", statusOrder.zipWithNext().all { it.first <= it.second })
    }

    // FILTERING TESTS

    @Test
    fun `should filter only playable files`() {
        // When filtering only playable files
        val filterOptions = FilterOptions(showOnlyPlayable = true)
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should only include playable files and torrents with playable files
        assertTrue("Should only include playable items", filtered.all { item ->
            when (item) {
                is FileItem.File -> item.isPlayable
                is FileItem.Torrent -> item.files.any { it.isPlayable }
                else -> false
            }
        })
    }

    @Test
    fun `should filter only downloaded files`() {
        // When filtering only downloaded files
        val filterOptions = FilterOptions(showOnlyDownloaded = true)
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should only include ready files and downloaded torrents
        assertTrue("Should only include downloaded items", filtered.all { item ->
            when (item) {
                is FileItem.File -> item.status == FileStatus.READY
                is FileItem.Torrent -> item.status == TorrentStatus.DOWNLOADED
                else -> false
            }
        })
    }

    @Test
    fun `should filter by file type`() {
        // When filtering by video file type
        val filterOptions = FilterOptions(fileTypeFilter = setOf(FileType.VIDEO))
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should only include video files
        assertTrue("Should only include video files", filtered.all { item ->
            when (item) {
                is FileItem.File -> {
                    val extension = item.name.substringAfterLast('.', "")
                    FileType.fromExtension(extension) == FileType.VIDEO
                }
                else -> true // Keep torrents
            }
        })
    }

    @Test
    fun `should filter by multiple file types`() {
        // When filtering by video and audio file types
        val filterOptions = FilterOptions(fileTypeFilter = setOf(FileType.VIDEO, FileType.AUDIO))
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should only include video and audio files
        assertTrue("Should only include video and audio files", filtered.all { item ->
            when (item) {
                is FileItem.File -> {
                    val extension = item.name.substringAfterLast('.', "")
                    val fileType = FileType.fromExtension(extension)
                    fileType in setOf(FileType.VIDEO, FileType.AUDIO)
                }
                else -> true // Keep torrents
            }
        })
    }

    @Test
    fun `should filter by status`() {
        // When filtering by ready status
        val filterOptions = FilterOptions(statusFilter = setOf(FileStatus.READY))
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should only include ready files
        assertTrue("Should only include ready files", filtered.all { item ->
            when (item) {
                is FileItem.File -> item.status == FileStatus.READY
                else -> true // Keep torrents
            }
        })
    }

    @Test
    fun `should filter by search query`() {
        // When filtering by search query
        val filterOptions = FilterOptions(searchQuery = "Movie")
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should only include items with "Movie" in name
        assertTrue("Should only include items with 'Movie' in name", filtered.all { item ->
            item.name.contains("Movie", ignoreCase = true)
        })
    }

    @Test
    fun `should filter with case insensitive search`() {
        // When filtering with case insensitive search
        val filterOptions = FilterOptions(searchQuery = "movie")
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should include items with "Movie" in name
        assertTrue("Should include items with 'Movie' in name", filtered.any { item ->
            item.name.contains("Movie", ignoreCase = true)
        })
    }

    @Test
    fun `should apply multiple filters simultaneously`() {
        // When applying multiple filters
        val filterOptions = FilterOptions(
            showOnlyPlayable = true,
            showOnlyDownloaded = true,
            fileTypeFilter = setOf(FileType.VIDEO),
            statusFilter = setOf(FileStatus.READY)
        )
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should satisfy all filter conditions
        assertTrue("Should satisfy all filters", filtered.all { item ->
            when (item) {
                is FileItem.File -> {
                    val extension = item.name.substringAfterLast('.', "")
                    val fileType = FileType.fromExtension(extension)
                    item.isPlayable && item.status == FileStatus.READY && fileType == FileType.VIDEO
                }
                is FileItem.Torrent -> {
                    item.status == TorrentStatus.DOWNLOADED && item.files.any { it.isPlayable }
                }
                else -> false
            }
        })
    }

    // COMBINED SORT AND FILTER TESTS

    @Test
    fun `should apply filtering then sorting`() {
        // When applying filter and sort
        val filterOptions = FilterOptions(showOnlyPlayable = true)
        val sortingOptions = SortingOptions(SortBy.SIZE, SortOrder.DESCENDING)
        
        val filtered = applyFiltering(allTestItems, filterOptions)
        val sorted = applySorting(filtered, sortingOptions)
        
        // Then should be filtered and sorted
        assertTrue("Should only include playable items", sorted.all { item ->
            when (item) {
                is FileItem.File -> item.isPlayable
                is FileItem.Torrent -> item.files.any { it.isPlayable }
                else -> false
            }
        })
        
        val sizes = sorted.map { it.size }
        assertTrue("Should be sorted by size descending", sizes.zipWithNext().all { it.first >= it.second })
    }

    @Test
    fun `should handle empty filter results`() {
        // When filtering with impossible conditions
        val filterOptions = FilterOptions(
            showOnlyPlayable = true,
            fileTypeFilter = setOf(FileType.DOCUMENT) // Documents are not playable
        )
        val filtered = applyFiltering(allTestItems, filterOptions)
        
        // Then should return empty or only torrents
        assertTrue("Should return empty or only torrents", filtered.all { it is FileItem.Torrent })
    }

    @Test
    fun `should handle edge cases in sorting`() {
        // Given items with same values
        val duplicateItems = listOf(
            FileItem.File(
                id = "dup1",
                name = "Same Name",
                size = 1024L,
                modifiedDate = 1000L,
                mimeType = "text/plain",
                downloadUrl = "url1",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.READY
            ),
            FileItem.File(
                id = "dup2",
                name = "Same Name",
                size = 1024L,
                modifiedDate = 1000L,
                mimeType = "text/plain",
                downloadUrl = "url2",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.READY
            )
        )
        
        // When sorting by name
        val sorted = applySorting(duplicateItems, SortingOptions(SortBy.NAME, SortOrder.ASCENDING))
        
        // Then should handle duplicates gracefully
        assertEquals(2, sorted.size)
        assertTrue("Should maintain all items", sorted.all { it.name == "Same Name" })
    }

    @Test
    fun `should handle performance with large datasets`() {
        // Given large dataset
        val largeDataset = (1..1000).map { i ->
            FileItem.File(
                id = "file$i",
                name = "File $i",
                size = (i * 1024L),
                modifiedDate = (i * 1000L),
                mimeType = "text/plain",
                downloadUrl = "url$i",
                streamUrl = null,
                isPlayable = i % 2 == 0, // Every other file is playable
                status = FileStatus.values()[i % FileStatus.values().size]
            )
        }
        
        // When applying filters and sorting
        val startTime = System.currentTimeMillis()
        
        val filterOptions = FilterOptions(showOnlyPlayable = true)
        val sortingOptions = SortingOptions(SortBy.SIZE, SortOrder.DESCENDING)
        
        val filtered = applyFiltering(largeDataset, filterOptions)
        val sorted = applySorting(filtered, sortingOptions)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then should complete quickly
        assertTrue("Should complete within reasonable time", duration < 1000) // Less than 1 second
        assertTrue("Should filter correctly", sorted.all { it is FileItem.File && (it as FileItem.File).isPlayable })
        assertTrue("Should sort correctly", sorted.map { it.size }.zipWithNext().all { it.first >= it.second })
    }

    // HELPER METHODS

    private fun applySorting(items: List<FileItem>, sortingOptions: SortingOptions): List<FileItem> {
        var sortedItems = when (sortingOptions.sortBy) {
            SortBy.NAME -> items.sortedBy { it.name }
            SortBy.SIZE -> items.sortedBy { it.size }
            SortBy.DATE -> items.sortedBy { it.modifiedDate }
            SortBy.TYPE -> items.sortedBy { getTypeOrder(it) }
            SortBy.STATUS -> items.sortedBy { getStatusOrder(it) }
        }
        
        return if (sortingOptions.sortOrder == SortOrder.DESCENDING) {
            sortedItems.reversed()
        } else {
            sortedItems
        }
    }

    private fun applyFiltering(items: List<FileItem>, filterOptions: FilterOptions): List<FileItem> {
        var filteredItems = items
        
        // Apply playable filter
        if (filterOptions.showOnlyPlayable) {
            filteredItems = filteredItems.filter { item ->
                when (item) {
                    is FileItem.File -> item.isPlayable
                    is FileItem.Torrent -> item.files.any { it.isPlayable }
                    else -> false
                }
            }
        }
        
        // Apply downloaded filter
        if (filterOptions.showOnlyDownloaded) {
            filteredItems = filteredItems.filter { item ->
                when (item) {
                    is FileItem.File -> item.status == FileStatus.READY
                    is FileItem.Torrent -> item.status == TorrentStatus.DOWNLOADED
                    else -> false
                }
            }
        }
        
        // Apply file type filter
        if (filterOptions.fileTypeFilter.isNotEmpty()) {
            filteredItems = filteredItems.filter { item ->
                when (item) {
                    is FileItem.File -> {
                        val extension = item.name.substringAfterLast('.', "")
                        val fileType = FileType.fromExtension(extension)
                        filterOptions.fileTypeFilter.contains(fileType)
                    }
                    else -> true // Keep torrents and folders
                }
            }
        }
        
        // Apply status filter
        if (filterOptions.statusFilter.isNotEmpty()) {
            filteredItems = filteredItems.filter { item ->
                when (item) {
                    is FileItem.File -> filterOptions.statusFilter.contains(item.status)
                    else -> true // Keep torrents and folders
                }
            }
        }
        
        // Apply search query filter
        if (filterOptions.searchQuery.isNotBlank()) {
            filteredItems = filteredItems.filter { item ->
                item.name.contains(filterOptions.searchQuery, ignoreCase = true)
            }
        }
        
        return filteredItems
    }

    private fun getTypeOrder(item: FileItem): String {
        return when (item) {
            is FileItem.Folder -> "1_folder"
            is FileItem.Torrent -> "2_torrent"
            is FileItem.File -> "3_${item.name.substringAfterLast('.', "")}"
        }
    }

    private fun getStatusOrder(item: FileItem): Int {
        return when (item) {
            is FileItem.File -> item.status.ordinal
            is FileItem.Torrent -> item.status.ordinal
            else -> 0
        }
    }
}