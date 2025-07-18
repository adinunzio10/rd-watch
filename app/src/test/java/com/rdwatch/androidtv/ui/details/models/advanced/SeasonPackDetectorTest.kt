package com.rdwatch.androidtv.ui.details.models.advanced

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for SeasonPackDetector
 * Tests season pack detection, episode parsing, and metadata extraction
 */
class SeasonPackDetectorTest {
    private lateinit var detector: SeasonPackDetector

    @Before
    fun setup() {
        detector = SeasonPackDetector()
    }

    @Test
    fun `analyzeSeasonPack - detects single episode correctly`() {
        val testCases =
            listOf(
                "Game.of.Thrones.S01E01.Winter.is.Coming.1080p.BluRay.x264-DEMAND.mkv",
                "Breaking.Bad.S03E07.One.Minute.720p.HDTV.x264-IMMERSE.mkv",
                "The.Office.US.1x05.Basketball.480p.DVD.XviD-OSiTV.avi",
                "Lost.2x15.Maternity.Leave.HDTV.XviD-LOL.avi",
            )

        testCases.forEach { filename ->
            val result = detector.analyzeSeasonPack(filename)

            assertTrue(result.isSingleEpisode, "Should detect single episode for: $filename")
            assertFalse(result.isSeasonPack, "Should not be season pack for: $filename")
            assertEquals(1, result.totalEpisodes, "Single episode should have 1 episode count")
            assertEquals(100.0f, result.completenessPercentage, "Single episode should be 100% complete")
            assertEquals(SeasonPackType.SINGLE_EPISODE, result.packType)
            assertTrue(result.confidence >= 90.0f, "Single episode detection should be high confidence")
        }
    }

    @Test
    fun `analyzeSeasonPack - detects complete season correctly`() {
        val testCases =
            mapOf(
                "Game.of.Thrones.Season.1.Complete.1080p.BluRay.x264-DEMAND" to 1,
                "Breaking.Bad.S02.Complete.720p.HDTV.x264-IMMERSE" to 2,
                "The.Office.US.Season.3.Complete.DVDRip.XviD-SAINTS" to 3,
                "Lost.Complete.Season.4.720p.BluRay.x264-SiNNERS" to 4,
                "Friends.The.Complete.Series.Season.10.DVDRip.XviD-TOPAZ" to 10,
            )

        testCases.forEach { (filename, expectedSeason) ->
            val result = detector.analyzeSeasonPack(filename, fileSize = 15_000_000_000L)

            assertTrue(result.isSeasonPack, "Should detect season pack for: $filename")
            assertFalse(result.isSingleEpisode, "Should not be single episode for: $filename")
            assertEquals(SeasonPackType.COMPLETE_SEASON, result.packType)
            assertTrue(result.seasonNumbers.contains(expectedSeason), "Should detect season $expectedSeason")
            assertEquals(100.0f, result.completenessPercentage, "Complete season should be 100%")
            assertTrue(result.metadata.hasCompleteIndicator, "Should detect complete indicator")
            assertTrue(result.confidence >= 70.0f, "Complete season detection should be high confidence")
        }
    }

    @Test
    fun `analyzeSeasonPack - detects partial season correctly`() {
        val testCases =
            mapOf(
                "Game.of.Thrones.S01E01-E05.1080p.BluRay.x264-DEMAND" to (1 to EpisodeRange(1, 5)),
                "Breaking.Bad.S02E07-E13.720p.HDTV.x264-IMMERSE" to (2 to EpisodeRange(7, 13)),
                "The.Office.Episodes.1-8.Season.3.DVDRip" to (3 to EpisodeRange(1, 8)),
            )

        testCases.forEach { (filename, expected) ->
            val (season, episodeRange) = expected
            val result = detector.analyzeSeasonPack(filename)

            assertTrue(result.isSeasonPack, "Should detect season pack for: $filename")
            assertFalse(result.isSingleEpisode, "Should not be single episode for: $filename")
            assertEquals(SeasonPackType.PARTIAL_SEASON, result.packType)
            assertTrue(result.seasonNumbers.contains(season), "Should detect season $season")
            assertEquals(episodeRange.start, result.episodeRange?.start, "Should detect correct start episode")
            assertEquals(episodeRange.end, result.episodeRange?.end, "Should detect correct end episode")
            assertTrue(result.completenessPercentage < 100.0f, "Partial season should be < 100%")
        }
    }

    @Test
    fun `analyzeSeasonPack - detects multi-season packs correctly`() {
        val testCases =
            mapOf(
                "Game.of.Thrones.Seasons.1-3.Complete.1080p.BluRay.x264-DEMAND" to (1 to 3),
                "Breaking.Bad.S01-S05.Complete.Series.720p.HDTV.x264-IMMERSE" to (1 to 5),
                "The.Office.US.Seasons.1-9.Complete.DVDRip.XviD-SAINTS" to (1 to 9),
            )

        testCases.forEach { (filename, expected) ->
            val (startSeason, endSeason) = expected
            val result = detector.analyzeSeasonPack(filename, fileSize = 50_000_000_000L)

            assertTrue(result.isSeasonPack, "Should detect season pack for: $filename")
            assertTrue(result.isMultiSeasonPack, "Should detect multi-season pack for: $filename")
            assertFalse(result.isSingleEpisode, "Should not be single episode for: $filename")
            assertEquals(SeasonPackType.MULTI_SEASON, result.packType)

            val expectedSeasons = (startSeason..endSeason).toList()
            assertEquals(expectedSeasons, result.seasonNumbers, "Should detect seasons $startSeason-$endSeason")
            assertTrue(result.totalEpisodes > 20, "Multi-season should have many episodes")
            assertTrue(result.confidence >= 70.0f, "Multi-season detection should be high confidence")
        }
    }

    @Test
    fun `analyzeSeasonPack - detects complete series correctly`() {
        val testCases =
            listOf(
                "Breaking.Bad.Complete.Series.1080p.BluRay.x264-DEMAND",
                "The.Wire.Complete.Collection.720p.HDTV.x264-IMMERSE",
                "Friends.The.Complete.Series.DVDRip.XviD-SAINTS",
            )

        testCases.forEach { filename ->
            val result = detector.analyzeSeasonPack(filename, fileSize = 100_000_000_000L)

            assertTrue(result.isSeasonPack, "Should detect season pack for: $filename")
            assertTrue(result.isCompleteSeriesPack, "Should detect complete series for: $filename")
            assertTrue(result.isMultiSeasonPack, "Complete series should be multi-season for: $filename")
            assertEquals(SeasonPackType.COMPLETE_SERIES, result.packType)
            assertEquals(100.0f, result.completenessPercentage, "Complete series should be 100%")
            assertTrue(result.totalEpisodes >= 50, "Complete series should have many episodes")
            assertTrue(result.metadata.hasCompleteIndicator, "Should detect complete indicator")
        }
    }

    @Test
    fun `analyzeSeasonPack - handles quality pack indicators`() {
        val testCases =
            listOf(
                "Game.of.Thrones.Season.1.BluRay.Pack.1080p.x264-DEMAND",
                "Breaking.Bad.S02.REMUX.Pack.2160p.UHD.BluRay.x265-TERMINAL",
                "The.Office.Season.3.WEB-DL.Pack.720p.H264-KINGS",
            )

        testCases.forEach { filename ->
            val result = detector.analyzeSeasonPack(filename)

            assertTrue(result.metadata.hasQualityPackIndicator, "Should detect quality pack indicator for: $filename")
            assertTrue(result.confidence >= 75.0f, "Quality pack should increase confidence")
        }
    }

    @Test
    fun `analyzeSeasonPack - calculates file size metadata correctly`() {
        val filename = "Game.of.Thrones.Season.1.Complete.1080p.BluRay.x264-DEMAND"
        val fileSize = 20_000_000_000L // 20GB

        val result = detector.analyzeSeasonPack(filename, fileSize = fileSize)

        assertNotNull(result.metadata.averageEpisodeSizeMB, "Should calculate average episode size")

        val expectedAverageSize = (fileSize / (1024 * 1024) / result.totalEpisodes).toInt()
        assertEquals(expectedAverageSize, result.metadata.averageEpisodeSizeMB, "Average size calculation should be correct")
    }

    @Test
    fun `analyzeSeasonPack - handles edge cases gracefully`() {
        val edgeCases =
            listOf(
                // Empty string
                "",
                "random.file.name.without.season.info.mkv",
                "Movie.2023.1080p.BluRay.x264-GROUP.mkv",
                "S1E1E2E3E4E5.weird.naming.mkv",
                "Season.1.Episode.1.but.also.Season.2.mkv",
            )

        edgeCases.forEach { filename ->
            val result = detector.analyzeSeasonPack(filename)

            // Should not crash and should return reasonable defaults
            assertNotNull(result, "Should return result for: $filename")
            assertTrue(
                result.confidence <= 50.0f || result.packType == SeasonPackType.UNKNOWN,
                "Edge case should have low confidence or unknown type for: $filename",
            )
        }
    }

    @Test
    fun `analyzeSeasonPack - various season notation formats`() {
        val seasonFormats =
            mapOf(
                "Show.S01.Complete.1080p.mkv" to 1,
                "Show.Season.1.Complete.1080p.mkv" to 1,
                "Show.Series.1.Complete.1080p.mkv" to 1,
                "Show.Complete.Season.1.1080p.mkv" to 1,
                "Show.The.Complete.Season.1.1080p.mkv" to 1,
            )

        seasonFormats.forEach { (filename, expectedSeason) ->
            val result = detector.analyzeSeasonPack(filename)

            assertTrue(
                result.seasonNumbers.contains(expectedSeason),
                "Should detect season $expectedSeason in: $filename",
            )
        }
    }

    @Test
    fun `analyzeSeasonPack - episode range formats`() {
        val rangeFormats =
            mapOf(
                "Show.S01E01-E05.1080p.mkv" to EpisodeRange(1, 5),
                "Show.S01E01-05.1080p.mkv" to EpisodeRange(1, 5),
                "Show.Episodes.1-5.Season.1.mkv" to EpisodeRange(1, 5),
                "Show.1-5.Episodes.S01.mkv" to EpisodeRange(1, 5),
            )

        rangeFormats.forEach { (filename, expectedRange) ->
            val result = detector.analyzeSeasonPack(filename)

            assertEquals(
                expectedRange.start,
                result.episodeRange?.start,
                "Should detect start episode in: $filename",
            )
            assertEquals(
                expectedRange.end,
                result.episodeRange?.end,
                "Should detect end episode in: $filename",
            )
        }
    }

    @Test
    fun `analyzeSeasonPack - confidence scoring works correctly`() {
        val highConfidenceFiles =
            listOf(
                "Show.Season.1.Complete.1080p.BluRay.Pack.x264-GROUP.mkv",
                "Show.S01.Complete.720p.HDTV.x264-GROUP.mkv",
            )

        val lowConfidenceFiles =
            listOf(
                "show.s1.hdtv.mkv",
                "random.video.file.mkv",
            )

        highConfidenceFiles.forEach { filename ->
            val result = detector.analyzeSeasonPack(filename)
            assertTrue(result.confidence >= 70.0f, "High confidence file should score high: $filename")
        }

        lowConfidenceFiles.forEach { filename ->
            val result = detector.analyzeSeasonPack(filename)
            assertTrue(result.confidence <= 50.0f, "Low confidence file should score low: $filename")
        }
    }

    @Test
    fun `getSeasonPackQualityScore - scores different pack types correctly`() {
        val completeSeriesInfo =
            SeasonPackInfo(
                isSeasonPack = true,
                isCompleteSeriesPack = true,
                isMultiSeasonPack = true,
                totalEpisodes = 100,
                completenessPercentage = 100.0f,
                packType = SeasonPackType.COMPLETE_SERIES,
                confidence = 95.0f,
            )

        val completeSeasonInfo =
            SeasonPackInfo(
                isSeasonPack = true,
                totalEpisodes = 22,
                completenessPercentage = 100.0f,
                packType = SeasonPackType.COMPLETE_SEASON,
                confidence = 90.0f,
            )

        val partialSeasonInfo =
            SeasonPackInfo(
                isSeasonPack = true,
                totalEpisodes = 5,
                completenessPercentage = 50.0f,
                packType = SeasonPackType.PARTIAL_SEASON,
                confidence = 80.0f,
            )

        val singleEpisodeInfo =
            SeasonPackInfo(
                isSingleEpisode = true,
                totalEpisodes = 1,
                completenessPercentage = 100.0f,
                packType = SeasonPackType.SINGLE_EPISODE,
                confidence = 95.0f,
            )

        val completeSeriesScore = detector.getSeasonPackQualityScore(completeSeriesInfo)
        val completeSeasonScore = detector.getSeasonPackQualityScore(completeSeasonInfo)
        val partialSeasonScore = detector.getSeasonPackQualityScore(partialSeasonInfo)
        val singleEpisodeScore = detector.getSeasonPackQualityScore(singleEpisodeInfo)

        assertTrue(completeSeriesScore > completeSeasonScore, "Complete series should score highest")
        assertTrue(completeSeasonScore > partialSeasonScore, "Complete season should score higher than partial")
        assertTrue(partialSeasonScore > singleEpisodeScore, "Partial season should score higher than single episode")

        // All scores should be within valid range
        assertTrue(completeSeriesScore in 0..300, "Score should be in valid range")
        assertTrue(completeSeasonScore in 0..300, "Score should be in valid range")
        assertTrue(partialSeasonScore in 0..300, "Score should be in valid range")
        assertTrue(singleEpisodeScore in 0..300, "Score should be in valid range")
    }

    @Test
    fun `SeasonPackInfo getDisplayText works correctly`() {
        val completeSeriesInfo =
            SeasonPackInfo(
                isCompleteSeriesPack = true,
                packType = SeasonPackType.COMPLETE_SERIES,
            )
        assertEquals("Complete Series", completeSeriesInfo.getDisplayText())

        val multiSeasonInfo =
            SeasonPackInfo(
                isMultiSeasonPack = true,
                seasonNumbers = listOf(1, 2, 3),
                packType = SeasonPackType.MULTI_SEASON,
            )
        assertEquals("Seasons 1-3", multiSeasonInfo.getDisplayText())

        val completeSeasonInfo =
            SeasonPackInfo(
                isSeasonPack = true,
                seasonNumbers = listOf(1),
                completenessPercentage = 100.0f,
                packType = SeasonPackType.COMPLETE_SEASON,
            )
        assertEquals("Season 1", completeSeasonInfo.getDisplayText())

        val partialSeasonInfo =
            SeasonPackInfo(
                isSeasonPack = true,
                seasonNumbers = listOf(2),
                completenessPercentage = 75.0f,
                packType = SeasonPackType.PARTIAL_SEASON,
            )
        assertEquals("Season 2 (75%)", partialSeasonInfo.getDisplayText())

        val singleEpisodeInfo =
            SeasonPackInfo(
                isSingleEpisode = true,
                seasonNumbers = listOf(1),
                episodeRange = EpisodeRange(5, 5),
                packType = SeasonPackType.SINGLE_EPISODE,
            )
        assertEquals("S1E5", singleEpisodeInfo.getDisplayText())
    }

    @Test
    fun `SeasonPackInfo getSeasonPackBadge works correctly`() {
        val completeSeriesInfo =
            SeasonPackInfo(
                isCompleteSeriesPack = true,
                packType = SeasonPackType.COMPLETE_SERIES,
            )
        val completeSeriesBadge = completeSeriesInfo.getSeasonPackBadge()
        assertNotNull(completeSeriesBadge)
        assertEquals("Complete Series", completeSeriesBadge.text)
        assertEquals(QualityBadge.Type.FEATURE, completeSeriesBadge.type)

        val multiSeasonInfo =
            SeasonPackInfo(
                isMultiSeasonPack = true,
                packType = SeasonPackType.MULTI_SEASON,
            )
        val multiSeasonBadge = multiSeasonInfo.getSeasonPackBadge()
        assertNotNull(multiSeasonBadge)
        assertEquals("Multi-Season", multiSeasonBadge.text)

        val completeSeasonInfo =
            SeasonPackInfo(
                isSeasonPack = true,
                completenessPercentage = 100.0f,
                packType = SeasonPackType.COMPLETE_SEASON,
            )
        val completeSeasonBadge = completeSeasonInfo.getSeasonPackBadge()
        assertNotNull(completeSeasonBadge)
        assertEquals("Complete Season", completeSeasonBadge.text)

        val partialSeasonInfo =
            SeasonPackInfo(
                isSeasonPack = true,
                completenessPercentage = 85.0f,
                packType = SeasonPackType.PARTIAL_SEASON,
            )
        val partialSeasonBadge = partialSeasonInfo.getSeasonPackBadge()
        assertNotNull(partialSeasonBadge)
        assertEquals("Most Episodes", partialSeasonBadge.text)

        val singleEpisodeInfo =
            SeasonPackInfo(
                isSingleEpisode = true,
                packType = SeasonPackType.SINGLE_EPISODE,
            )
        val singleEpisodeBadge = singleEpisodeInfo.getSeasonPackBadge()
        assertNull(singleEpisodeBadge, "Single episode should not have season pack badge")
    }

    @Test
    fun `EpisodeRange count property works correctly`() {
        val range1 = EpisodeRange(1, 5)
        assertEquals(5, range1.count, "Range 1-5 should have count 5")

        val range2 = EpisodeRange(7, 13)
        assertEquals(7, range2.count, "Range 7-13 should have count 7")

        val singleEpisode = EpisodeRange(1, 1)
        assertEquals(1, singleEpisode.count, "Single episode range should have count 1")
    }

    @Test
    fun `detector handles case insensitive matching`() {
        val testCases =
            listOf(
                "game.of.thrones.season.1.complete.1080p.bluray.x264-demand",
                "GAME.OF.THRONES.SEASON.1.COMPLETE.1080P.BLURAY.X264-DEMAND",
                "Game.Of.Thrones.Season.1.Complete.1080p.BluRay.x264-DEMAND",
            )

        testCases.forEach { filename ->
            val result = detector.analyzeSeasonPack(filename)

            assertTrue(result.isSeasonPack, "Should detect season pack regardless of case: $filename")
            assertTrue(result.seasonNumbers.contains(1), "Should detect season 1 regardless of case")
            assertTrue(result.metadata.hasCompleteIndicator, "Should detect complete indicator regardless of case")
        }
    }

    @Test
    fun `detector handles real-world filenames correctly`() {
        val realWorldFiles =
            mapOf(
                "Game.of.Thrones.S08.COMPLETE.720p.HDTV.x264-AVS[rarbg]" to (8 to SeasonPackType.COMPLETE_SEASON),
                "The.Walking.Dead.S10E01-E06.720p.HDTV.x264-KILLERS[rartv]" to (10 to SeasonPackType.PARTIAL_SEASON),
                "Breaking.Bad.S05E14.Ozymandias.1080p.WEB-DL.DD5.1.H.264-Coo7[rarbg]" to (5 to SeasonPackType.SINGLE_EPISODE),
                "Friends.The.Complete.Series.1994-2004.720p.BluRay.x264-PSYCHD" to (0 to SeasonPackType.COMPLETE_SERIES),
                "The.Office.US.S01-S09.COMPLETE.720p.WEB-DL.x264-MIXED[rartv]" to (1 to SeasonPackType.MULTI_SEASON),
            )

        realWorldFiles.forEach { (filename, expected) ->
            val (season, expectedType) = expected
            val result = detector.analyzeSeasonPack(filename)

            assertEquals(expectedType, result.packType, "Should detect correct pack type for: $filename")
            if (season > 0) {
                assertTrue(result.seasonNumbers.contains(season), "Should detect season $season in: $filename")
            }
            assertTrue(result.confidence > 0.0f, "Should have some confidence for real-world file: $filename")
        }
    }
}
