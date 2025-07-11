package com.rdwatch.androidtv.test.data

import com.rdwatch.androidtv.network.models.tmdb.*
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.ContentMetadata
import com.rdwatch.androidtv.ui.details.models.ContentAction
import com.rdwatch.androidtv.ui.details.models.ContentProgress
import com.rdwatch.androidtv.data.mappers.TMDbMovieContentDetail
import com.rdwatch.androidtv.data.mappers.TMDbTVContentDetail
import com.rdwatch.androidtv.data.mappers.TMDbSearchResultContentDetail

/**
 * Factory for creating test data for TMDb models
 * Provides consistent test data for all TMDb-related tests
 */
object TMDbTestDataFactory {
    
    // Test IDs
    const val TEST_MOVIE_ID = 550
    const val TEST_TV_ID = 1399
    const val TEST_PERSON_ID = 287
    const val TEST_COLLECTION_ID = 9485
    
    // Test image paths
    const val TEST_POSTER_PATH = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg"
    const val TEST_BACKDROP_PATH = "/fCayJrkfRaCRCTh8GqN30f8oyQF.jpg"
    const val TEST_PROFILE_PATH = "/8iATAc5z5XOKFFARLsvaawa8MTY.jpg"
    
    // Test dates
    const val TEST_RELEASE_DATE = "1999-10-15"
    const val TEST_FIRST_AIR_DATE = "2011-04-17"
    const val TEST_LAST_AIR_DATE = "2019-05-19"
    
    /**
     * Creates a sample TMDb movie response
     */
    fun createTMDbMovieResponse(
        id: Int = TEST_MOVIE_ID,
        title: String = "Fight Club",
        overview: String = "A ticking-time-bomb insomniac and a slippery soap salesman channel primal male aggression into a shocking new form of therapy."
    ): TMDbMovieResponse {
        return TMDbMovieResponse(
            id = id,
            title = title,
            originalTitle = title,
            overview = overview,
            releaseDate = TEST_RELEASE_DATE,
            posterPath = TEST_POSTER_PATH,
            backdropPath = TEST_BACKDROP_PATH,
            voteAverage = 8.433,
            voteCount = 26280,
            popularity = 61.416,
            adult = false,
            video = false,
            originalLanguage = "en",
            genres = listOf(
                TMDbGenreResponse(id = 18, name = "Drama"),
                TMDbGenreResponse(id = 53, name = "Thriller")
            ),
            runtime = 139,
            budget = 63000000,
            revenue = 100853753,
            status = "Released",
            tagline = "Mischief. Mayhem. Soap.",
            homepage = "http://www.foxmovies.com/movies/fight-club",
            imdbId = "tt0137523",
            productionCompanies = listOf(
                TMDbProductionCompanyResponse(
                    id = 508,
                    name = "Regency Enterprises",
                    logoPath = "/7PzJdsLGlR7oW4J0J5Xcd0pHGRg.png",
                    originCountry = "US"
                )
            ),
            productionCountries = listOf(
                TMDbProductionCountryResponse(
                    iso31661 = "US",
                    name = "United States of America"
                )
            ),
            spokenLanguages = listOf(
                TMDbSpokenLanguageResponse(
                    englishName = "English",
                    iso6391 = "en",
                    name = "English"
                )
            ),
            belongsToCollection = null
        )
    }
    
    /**
     * Creates a sample TMDb TV response
     */
    fun createTMDbTVResponse(
        id: Int = TEST_TV_ID,
        name: String = "Game of Thrones",
        overview: String = "Seven noble families fight for control of the mythical land of Westeros."
    ): TMDbTVResponse {
        return TMDbTVResponse(
            id = id,
            name = name,
            originalName = name,
            overview = overview,
            firstAirDate = TEST_FIRST_AIR_DATE,
            lastAirDate = TEST_LAST_AIR_DATE,
            posterPath = TEST_POSTER_PATH,
            backdropPath = TEST_BACKDROP_PATH,
            voteAverage = 8.453,
            voteCount = 22075,
            popularity = 369.594,
            adult = false,
            originalLanguage = "en",
            genres = listOf(
                TMDbGenreResponse(id = 18, name = "Drama"),
                TMDbGenreResponse(id = 10759, name = "Action & Adventure"),
                TMDbGenreResponse(id = 10765, name = "Sci-Fi & Fantasy")
            ),
            numberOfEpisodes = 73,
            numberOfSeasons = 8,
            status = "Ended",
            type = "Scripted",
            homepage = "http://www.hbo.com/game-of-thrones",
            inProduction = false,
            networks = listOf(
                TMDbNetworkResponse(
                    id = 49,
                    name = "HBO",
                    logoPath = "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
                    originCountry = "US"
                )
            ),
            originCountry = listOf("US"),
            productionCompanies = listOf(
                TMDbProductionCompanyResponse(
                    id = 76043,
                    name = "Revolution Sun Studios",
                    logoPath = null,
                    originCountry = "US"
                )
            ),
            productionCountries = listOf(
                TMDbProductionCountryResponse(
                    iso31661 = "US",
                    name = "United States of America"
                )
            ),
            spokenLanguages = listOf(
                TMDbSpokenLanguageResponse(
                    englishName = "English",
                    iso6391 = "en",
                    name = "English"
                )
            ),
            episodeRunTime = listOf(60)
        )
    }
    
    /**
     * Creates a sample TMDb credits response
     */
    fun createTMDbCreditsResponse(
        id: Int = TEST_MOVIE_ID
    ): TMDbCreditsResponse {
        return TMDbCreditsResponse(
            id = id,
            cast = listOf(
                TMDbCastResponse(
                    id = 819,
                    name = "Edward Norton",
                    originalName = "Edward Norton",
                    character = "The Narrator",
                    castId = 4,
                    creditId = "52fe4250c3a36847f80149f3",
                    order = 0,
                    adult = false,
                    gender = 2,
                    knownForDepartment = "Acting",
                    popularity = 26.99,
                    profilePath = TEST_PROFILE_PATH
                ),
                TMDbCastResponse(
                    id = 287,
                    name = "Brad Pitt",
                    originalName = "Brad Pitt",
                    character = "Tyler Durden",
                    castId = 5,
                    creditId = "52fe4250c3a36847f80149f7",
                    order = 1,
                    adult = false,
                    gender = 2,
                    knownForDepartment = "Acting",
                    popularity = 40.608,
                    profilePath = TEST_PROFILE_PATH
                )
            ),
            crew = listOf(
                TMDbCrewResponse(
                    id = 7467,
                    name = "David Fincher",
                    originalName = "David Fincher",
                    job = "Director",
                    department = "Directing",
                    creditId = "52fe4250c3a36847f8014a05",
                    adult = false,
                    gender = 2,
                    knownForDepartment = "Directing",
                    popularity = 10.992,
                    profilePath = TEST_PROFILE_PATH
                ),
                TMDbCrewResponse(
                    id = 7469,
                    name = "Jim Uhls",
                    originalName = "Jim Uhls",
                    job = "Screenplay",
                    department = "Writing",
                    creditId = "52fe4250c3a36847f8014a0b",
                    adult = false,
                    gender = 2,
                    knownForDepartment = "Writing",
                    popularity = 3.013,
                    profilePath = null
                )
            )
        )
    }
    
    /**
     * Creates a sample TMDb search response
     */
    fun createTMDbSearchResponse(
        page: Int = 1,
        totalPages: Int = 42,
        totalResults: Int = 10000
    ): TMDbSearchResponse {
        return TMDbSearchResponse(
            page = page,
            results = listOf(
                TMDbSearchResultResponse(
                    id = TEST_MOVIE_ID,
                    title = "Fight Club",
                    name = null,
                    overview = "A ticking-time-bomb insomniac and a slippery soap salesman channel primal male aggression into a shocking new form of therapy.",
                    releaseDate = TEST_RELEASE_DATE,
                    firstAirDate = null,
                    posterPath = TEST_POSTER_PATH,
                    backdropPath = TEST_BACKDROP_PATH,
                    voteAverage = 8.433,
                    voteCount = 26280,
                    popularity = 61.416,
                    adult = false,
                    video = false,
                    originalLanguage = "en",
                    originalTitle = "Fight Club",
                    originalName = null,
                    genreIds = listOf(18, 53),
                    mediaType = "movie"
                ),
                TMDbSearchResultResponse(
                    id = TEST_TV_ID,
                    title = null,
                    name = "Game of Thrones",
                    overview = "Seven noble families fight for control of the mythical land of Westeros.",
                    releaseDate = null,
                    firstAirDate = TEST_FIRST_AIR_DATE,
                    posterPath = TEST_POSTER_PATH,
                    backdropPath = TEST_BACKDROP_PATH,
                    voteAverage = 8.453,
                    voteCount = 22075,
                    popularity = 369.594,
                    adult = false,
                    video = false,
                    originalLanguage = "en",
                    originalTitle = null,
                    originalName = "Game of Thrones",
                    genreIds = listOf(18, 10759, 10765),
                    mediaType = "tv"
                )
            ),
            totalPages = totalPages,
            totalResults = totalResults
        )
    }
    
    /**
     * Creates a sample TMDb recommendations response
     */
    fun createTMDbRecommendationsResponse(
        page: Int = 1,
        totalPages: Int = 10,
        totalResults: Int = 200
    ): TMDbRecommendationsResponse {
        return TMDbRecommendationsResponse(
            page = page,
            results = listOf(
                TMDbRecommendationResponse(
                    id = 807,
                    title = "Se7en",
                    originalTitle = "Se7en",
                    overview = "Two homicide detectives are on a desperate hunt for a serial killer whose crimes are based on the 'seven deadly sins' in this dark and haunting film.",
                    releaseDate = "1995-09-22",
                    posterPath = "/6yoghtyTpznpBik8EngEmJskVUO.jpg",
                    backdropPath = "/ba4GELhxJrNDNNLHxUntQP9RJlg.jpg",
                    voteAverage = 8.374,
                    voteCount = 13811,
                    popularity = 89.104,
                    adult = false,
                    video = false,
                    originalLanguage = "en",
                    genreIds = listOf(80, 18, 9648, 53)
                )
            ),
            totalPages = totalPages,
            totalResults = totalResults
        )
    }
    
    /**
     * Creates a sample TMDb movie images response
     */
    fun createTMDbMovieImagesResponse(
        id: Int = TEST_MOVIE_ID
    ): TMDbMovieImagesResponse {
        return TMDbMovieImagesResponse(
            id = id,
            backdrops = listOf(
                TMDbImageResponse(
                    aspectRatio = 1.778,
                    height = 1080,
                    iso6391 = null,
                    filePath = TEST_BACKDROP_PATH,
                    voteAverage = 5.246,
                    voteCount = 2,
                    width = 1920
                )
            ),
            logos = listOf(
                TMDbImageResponse(
                    aspectRatio = 3.389,
                    height = 360,
                    iso6391 = "en",
                    filePath = "/tEiIH5QesdheJmDAqfKP6TsxvEO.png",
                    voteAverage = 5.312,
                    voteCount = 1,
                    width = 1220
                )
            ),
            posters = listOf(
                TMDbImageResponse(
                    aspectRatio = 0.667,
                    height = 3000,
                    iso6391 = "en",
                    filePath = TEST_POSTER_PATH,
                    voteAverage = 5.388,
                    voteCount = 13,
                    width = 2000
                )
            )
        )
    }
    
    /**
     * Creates a sample TMDb movie videos response
     */
    fun createTMDbMovieVideosResponse(
        id: Int = TEST_MOVIE_ID
    ): TMDbMovieVideosResponse {
        return TMDbMovieVideosResponse(
            id = id,
            results = listOf(
                TMDbVideoResponse(
                    id = "533ec654c3a36854480003eb",
                    key = "SUXWAEX2jlg",
                    name = "Fight Club | #TBT Trailer | 20th Century FOX",
                    site = "YouTube",
                    size = 1080,
                    type = "Trailer",
                    official = true,
                    publishedAt = "2014-10-02T19:20:22.000Z",
                    iso6391 = "en",
                    iso31661 = "US"
                )
            )
        )
    }
    
    /**
     * Creates a sample API response wrapper
     */
    fun <T> createSuccessApiResponse(data: T): ApiResponse<T> {
        return ApiResponse.Success(data)
    }
    
    /**
     * Creates a sample error API response
     */
    fun <T> createErrorApiResponse(
        message: String = "API Error",
        code: Int = 404
    ): ApiResponse<T> {
        return ApiResponse.Error(Exception("HTTP $code: $message"))
    }
    
    /**
     * Creates a sample loading API response
     */
    fun <T> createLoadingApiResponse(): ApiResponse<T> {
        return ApiResponse.Loading
    }
    
    // Entity factory methods
    
    /**
     * Creates a sample TMDb movie entity
     */
    fun createTMDbMovieEntity(
        id: Int = TEST_MOVIE_ID,
        title: String = "Fight Club"
    ): TMDbMovieEntity {
        return TMDbMovieEntity(
            id = id,
            title = title,
            originalTitle = title,
            overview = "A ticking-time-bomb insomniac and a slippery soap salesman channel primal male aggression into a shocking new form of therapy.",
            releaseDate = TEST_RELEASE_DATE,
            posterPath = TEST_POSTER_PATH,
            backdropPath = TEST_BACKDROP_PATH,
            voteAverage = 8.433f,
            voteCount = 26280,
            popularity = 61.416f,
            adult = false,
            video = false,
            originalLanguage = "en",
            genreIds = listOf(18, 53),
            runtime = 139,
            budget = 63000000,
            revenue = 100853753,
            status = "Released",
            tagline = "Mischief. Mayhem. Soap.",
            homepage = "http://www.foxmovies.com/movies/fight-club",
            imdbId = "tt0137523",
            spokenLanguages = listOf("English"),
            productionCompanies = listOf("Regency Enterprises"),
            productionCountries = listOf("United States of America"),
            genres = listOf("Drama", "Thriller"),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Creates a sample TMDb TV entity
     */
    fun createTMDbTVEntity(
        id: Int = TEST_TV_ID,
        name: String = "Game of Thrones"
    ): TMDbTVEntity {
        return TMDbTVEntity(
            id = id,
            name = name,
            originalName = name,
            overview = "Seven noble families fight for control of the mythical land of Westeros.",
            firstAirDate = TEST_FIRST_AIR_DATE,
            lastAirDate = TEST_LAST_AIR_DATE,
            posterPath = TEST_POSTER_PATH,
            backdropPath = TEST_BACKDROP_PATH,
            voteAverage = 8.453f,
            voteCount = 22075,
            popularity = 369.594f,
            adult = false,
            originalLanguage = "en",
            genreIds = listOf(18, 10759, 10765),
            numberOfEpisodes = 73,
            numberOfSeasons = 8,
            status = "Ended",
            type = "Scripted",
            homepage = "http://www.hbo.com/game-of-thrones",
            inProduction = false,
            networks = listOf("HBO"),
            originCountry = listOf("US"),
            productionCompanies = listOf("Revolution Sun Studios"),
            productionCountries = listOf("United States of America"),
            spokenLanguages = listOf("English"),
            genres = listOf("Drama", "Action & Adventure", "Sci-Fi & Fantasy"),
            episodeRunTime = listOf(60),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    // ContentDetail factory methods
    
    /**
     * Creates a sample TMDb movie ContentDetail
     */
    fun createTMDbMovieContentDetail(
        movieResponse: TMDbMovieResponse = createTMDbMovieResponse(),
        credits: TMDbCreditsResponse? = createTMDbCreditsResponse(),
        progress: ContentProgress = ContentProgress(),
        isInWatchlist: Boolean = false,
        isLiked: Boolean = false,
        isDownloaded: Boolean = false,
        isDownloading: Boolean = false
    ): TMDbMovieContentDetail {
        return TMDbMovieContentDetail(
            tmdbMovie = movieResponse,
            credits = credits,
            progress = progress,
            isInWatchlist = isInWatchlist,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading
        )
    }
    
    /**
     * Creates a sample TMDb TV ContentDetail
     */
    fun createTMDbTVContentDetail(
        tvResponse: TMDbTVResponse = createTMDbTVResponse(),
        credits: TMDbCreditsResponse? = createTMDbCreditsResponse(),
        progress: ContentProgress = ContentProgress(),
        isInWatchlist: Boolean = false,
        isLiked: Boolean = false,
        isDownloaded: Boolean = false,
        isDownloading: Boolean = false
    ): TMDbTVContentDetail {
        return TMDbTVContentDetail(
            tmdbTV = tvResponse,
            credits = credits,
            progress = progress,
            isInWatchlist = isInWatchlist,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading
        )
    }
    
    /**
     * Creates a sample TMDb search result ContentDetail
     */
    fun createTMDbSearchResultContentDetail(
        searchResult: TMDbSearchResultResponse = createTMDbSearchResponse().results.first(),
        isInWatchlist: Boolean = false,
        isLiked: Boolean = false
    ): TMDbSearchResultContentDetail {
        return TMDbSearchResultContentDetail(
            searchResult = searchResult,
            isInWatchlist = isInWatchlist,
            isLiked = isLiked
        )
    }
    
    /**
     * Creates a sample content progress
     */
    fun createContentProgress(
        watchPercentage: Float = 0.35f,
        isCompleted: Boolean = false,
        resumePosition: Long = 1800000L, // 30 minutes
        totalDuration: Long = 8340000L // 139 minutes
    ): ContentProgress {
        return ContentProgress(
            watchPercentage = watchPercentage,
            isCompleted = isCompleted,
            resumePosition = resumePosition,
            totalDuration = totalDuration
        )
    }
    
    /**
     * Creates error scenarios for testing
     */
    object ErrorScenarios {
        fun networkError(): Exception = Exception("Network error")
        fun unauthorizedError(): Exception = Exception("HTTP 401: Unauthorized")
        fun notFoundError(): Exception = Exception("HTTP 404: Not Found")
        fun serverError(): Exception = Exception("HTTP 500: Internal Server Error")
        fun rateLimitError(): Exception = Exception("HTTP 429: Too Many Requests")
        fun timeoutError(): Exception = Exception("Timeout error")
        fun parseError(): Exception = Exception("JSON parsing error")
        fun databaseError(): Exception = Exception("Database error")
    }
}