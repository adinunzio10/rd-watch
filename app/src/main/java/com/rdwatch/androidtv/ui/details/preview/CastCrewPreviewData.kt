package com.rdwatch.androidtv.ui.details.preview

import com.rdwatch.androidtv.data.mappers.TMDbMovieContentDetail
import com.rdwatch.androidtv.network.models.tmdb.*
import com.rdwatch.androidtv.ui.details.models.*

/**
 * Preview data for Cast & Crew section testing
 */
object CastCrewPreviewData {
    /**
     * Sample credits with cast and crew
     */
    fun createSampleCredits(): TMDbCreditsResponse {
        return TMDbCreditsResponse(
            id = 1,
            cast =
                listOf(
                    TMDbCastResponse(
                        id = 1,
                        name = "Tom Hanks",
                        character = "Forrest Gump",
                        profilePath = "/xndWFsBlClOJFRdhSt4NBwiPq2o.jpg",
                        order = 0,
                    ),
                    TMDbCastResponse(
                        id = 2,
                        name = "Robin Wright",
                        character = "Jenny Curran",
                        profilePath = "/cke0NNZP4lHRtOethRy2XGSOp3E.jpg",
                        order = 1,
                    ),
                    TMDbCastResponse(
                        id = 3,
                        name = "Gary Sinise",
                        character = "Lieutenant Dan Taylor",
                        profilePath = "/gThaIXgpCm3PCiXwFNDBJCme85y.jpg",
                        order = 2,
                    ),
                    TMDbCastResponse(
                        id = 4,
                        name = "Sally Field",
                        character = "Mrs. Gump",
                        profilePath = "/5fBK36MdmdwQQMuP0W70rXwvNGH.jpg",
                        order = 3,
                    ),
                    TMDbCastResponse(
                        id = 5,
                        name = "Mykelti Williamson",
                        character = "Bubba Blue",
                        profilePath = "/kfTwOYr3iUucmYz8kPjhYy07G2Z.jpg",
                        order = 4,
                    ),
                ),
            crew =
                listOf(
                    TMDbCrewResponse(
                        id = 101,
                        name = "Robert Zemeckis",
                        job = "Director",
                        department = "Directing",
                        profilePath = "/lPYDQ5LYNJ12rJZENtyASmVZ1Ql.jpg",
                    ),
                    TMDbCrewResponse(
                        id = 102,
                        name = "Eric Roth",
                        job = "Screenplay",
                        department = "Writing",
                        profilePath = "/5gAi0mp2MKHU8K8ExY9NZBdIR7g.jpg",
                    ),
                    TMDbCrewResponse(
                        id = 103,
                        name = "Wendy Finerman",
                        job = "Producer",
                        department = "Production",
                        profilePath = null,
                    ),
                    TMDbCrewResponse(
                        id = 104,
                        name = "Alan Silvestri",
                        job = "Composer",
                        department = "Sound",
                        profilePath = "/eWGqn6r4F9O5cJKZ5FRe8pzEYP7.jpg",
                    ),
                ),
        )
    }

    /**
     * Sample movie response for testing
     */
    fun createSampleMovie(): TMDbMovieResponse {
        return TMDbMovieResponse(
            id = 13,
            title = "Forrest Gump",
            originalTitle = "Forrest Gump",
            overview = "A man with a low IQ has accomplished great things in his life and been present during significant historic events—in each case, far exceeding what anyone imagined he could do. But despite all he has achieved, his one true love eludes him.",
            releaseDate = "1994-06-23",
            runtime = 142,
            voteAverage = 8.5,
            voteCount = 23456,
            popularity = 98.7,
            adult = false,
            backdropPath = "/7c9UVPPiTPltouxRVY6N9uugaVA.jpg",
            posterPath = "/saHP97rTPS5eLmrLQEcANmKrsFl.jpg",
            originalLanguage = "en",
            genres =
                listOf(
                    TMDbGenreResponse(id = 35, name = "Comedy"),
                    TMDbGenreResponse(id = 18, name = "Drama"),
                    TMDbGenreResponse(id = 10749, name = "Romance"),
                ),
            productionCompanies =
                listOf(
                    TMDbProductionCompanyResponse(
                        id = 1,
                        name = "Paramount Pictures",
                        logoPath = "/gz66EfNoYPqHTYI4q9UEN4CbHRc.png",
                        originCountry = "US",
                    ),
                ),
            productionCountries =
                listOf(
                    TMDbProductionCountryResponse(iso31661 = "US", name = "United States of America"),
                ),
            spokenLanguages =
                listOf(
                    TMDbSpokenLanguageResponse(
                        englishName = "English",
                        iso6391 = "en",
                        name = "English",
                    ),
                ),
            budget = 55000000,
            revenue = 678226465,
            status = "Released",
            tagline = "The world will never be the same once you've seen it through the eyes of Forrest Gump.",
            homepage = "",
            imdbId = "tt0109830",
            video = false,
            belongsToCollection = null,
        )
    }

    /**
     * Create a movie content detail with credits for preview
     */
    fun createMovieWithCredits(): TMDbMovieContentDetail {
        return TMDbMovieContentDetail(
            tmdbMovie = createSampleMovie(),
            credits = createSampleCredits(),
        )
    }

    /**
     * Sample TV show credits
     */
    fun createSampleTVCredits(): TMDbCreditsResponse {
        return TMDbCreditsResponse(
            id = 2,
            cast =
                listOf(
                    TMDbCastResponse(
                        id = 10,
                        name = "Bryan Cranston",
                        character = "Walter White",
                        profilePath = "/7Jahy5LZX2Fo8fGJltMreAI49hC.jpg",
                        order = 0,
                    ),
                    TMDbCastResponse(
                        id = 11,
                        name = "Aaron Paul",
                        character = "Jesse Pinkman",
                        profilePath = "/u8UdsB9yenM4uHEjmce4nkBn48X.jpg",
                        order = 1,
                    ),
                    TMDbCastResponse(
                        id = 12,
                        name = "Anna Gunn",
                        character = "Skyler White",
                        profilePath = "/adppyeu1a4REN3khtgmXusrapFi.jpg",
                        order = 2,
                    ),
                    TMDbCastResponse(
                        id = 13,
                        name = "Dean Norris",
                        character = "Hank Schrader",
                        profilePath = "/500eNlLVVz3Tgo8cav1yCwQxaAH.jpg",
                        order = 3,
                    ),
                ),
            crew =
                listOf(
                    TMDbCrewResponse(
                        id = 201,
                        name = "Vince Gilligan",
                        job = "Creator",
                        department = "Writing",
                        profilePath = "/uFh3OrBvkwKSU3N5y0XnXOhqBJz.jpg",
                    ),
                    TMDbCrewResponse(
                        id = 202,
                        name = "Michelle MacLaren",
                        job = "Director",
                        department = "Directing",
                        profilePath = "/7pSOTNnQB5GhXRaDsGHSL5LTKvC.jpg",
                    ),
                    TMDbCrewResponse(
                        id = 203,
                        name = "Dave Porter",
                        job = "Composer",
                        department = "Sound",
                        profilePath = null,
                    ),
                ),
        )
    }

    /**
     * Create extended metadata for preview
     */
    fun createExtendedMetadata(): ExtendedContentMetadata {
        val credits = createSampleCredits()
        return ExtendedContentMetadata(
            year = "1994",
            duration = "2h 22m",
            rating = "8.5/10",
            language = "English",
            genre = listOf("Comedy", "Drama", "Romance"),
            studio = "Paramount Pictures",
            cast = credits.cast.take(5).map { it.name },
            fullCast =
                credits.cast.map { cast ->
                    CastMember(
                        id = cast.id,
                        name = cast.name,
                        character = cast.character,
                        profileImageUrl = cast.profilePath?.let { CastMember.buildProfileImageUrl(it) },
                        order = cast.order,
                    )
                },
            director = "Robert Zemeckis",
            crew =
                credits.crew.filter { CrewMember.isKeyRole(it.job) }.map { crew ->
                    CrewMember(
                        id = crew.id,
                        name = crew.name,
                        job = crew.job,
                        department = crew.department,
                        profileImageUrl = crew.profilePath?.let { CrewMember.buildProfileImageUrl(it) },
                    )
                },
        )
    }
}
