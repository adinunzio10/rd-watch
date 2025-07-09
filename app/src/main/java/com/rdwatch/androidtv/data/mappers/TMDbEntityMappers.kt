package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.network.models.tmdb.*

/**
 * Extension functions for converting between TMDb DTOs and entities
 * These are simplified implementations focusing on core functionality
 */

// TMDbCreditsResponse to Entity conversion
fun TMDbCreditsResponse.toEntity(contentId: Int, contentType: String): TMDbCreditsEntity {
    return TMDbCreditsEntity(
        id = "${contentId}_${contentType}",
        contentId = contentId,
        contentType = contentType,
        cast = cast.map { it.toCastMemberEntity() },
        crew = crew.map { it.toCrewMemberEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

fun TMDbCastMemberResponse.toCastMemberEntity(): TMDbCastMemberEntity {
    return TMDbCastMemberEntity(
        id = id,
        name = name,
        character = character,
        profilePath = profilePath,
        order = order,
        castId = castId,
        creditId = creditId,
        adult = adult,
        gender = gender,
        knownForDepartment = knownForDepartment,
        originalName = originalName,
        popularity = popularity.toFloat()
    )
}

fun TMDbCrewMemberResponse.toCrewMemberEntity(): TMDbCrewMemberEntity {
    return TMDbCrewMemberEntity(
        id = id,
        name = name,
        job = job,
        department = department,
        profilePath = profilePath,
        creditId = creditId,
        adult = adult,
        gender = gender,
        knownForDepartment = knownForDepartment,
        originalName = originalName,
        popularity = popularity.toFloat()
    )
}

// TMDbCreditsEntity to Response conversion
fun TMDbCreditsEntity.toCreditsResponse(): TMDbCreditsResponse {
    return TMDbCreditsResponse(
        id = contentId,
        cast = cast.map { it.toCastMemberResponse() },
        crew = crew.map { it.toCrewMemberResponse() }
    )
}

fun TMDbCastMemberEntity.toCastMemberResponse(): TMDbCastMemberResponse {
    return TMDbCastMemberResponse(
        id = id,
        name = name,
        character = character,
        profilePath = profilePath,
        order = order,
        castId = castId ?: 0,
        creditId = creditId,
        adult = adult,
        gender = gender ?: 0,
        knownForDepartment = knownForDepartment ?: "",
        originalName = originalName,
        popularity = popularity.toDouble()
    )
}

fun TMDbCrewMemberEntity.toCrewMemberResponse(): TMDbCrewMemberResponse {
    return TMDbCrewMemberResponse(
        id = id,
        name = name,
        job = job,
        department = department,
        profilePath = profilePath,
        creditId = creditId,
        adult = adult,
        gender = gender ?: 0,
        knownForDepartment = knownForDepartment ?: "",
        originalName = originalName,
        popularity = popularity.toDouble()
    )
}

// TMDbRecommendationsResponse to Entity conversion
fun TMDbRecommendationsResponse.toEntity(
    contentId: Int,
    contentType: String,
    recommendationType: String,
    page: Int
): TMDbRecommendationEntity {
    return TMDbRecommendationEntity(
        id = "${contentId}_${contentType}_${recommendationType}_${page}",
        contentId = contentId,
        contentType = contentType,
        recommendationType = recommendationType,
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results.map { it.toSearchItemEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

// TMDbRecommendationItemResponse is an alias for TMDbSearchResultResponse, 
// so we use the same toSearchItemEntity function

// TMDbRecommendationEntity to Response conversion
fun TMDbRecommendationEntity.toRecommendationsResponse(): TMDbRecommendationsResponse {
    return TMDbRecommendationsResponse(
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results.map { it.toRecommendationItemResponse() }
    )
}

fun TMDbSearchItemEntity.toRecommendationItemResponse(): TMDbRecommendationItemResponse {
    return TMDbRecommendationItemResponse(
        id = id,
        title = title,
        name = name,
        originalTitle = originalTitle,
        originalName = originalName,
        overview = overview ?: "",
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toDouble(),
        voteCount = voteCount,
        popularity = popularity.toDouble(),
        adult = adult,
        video = video ?: false,
        originalLanguage = originalLanguage,
        genreIds = genreIds
    )
}

// TMDbMovieImagesResponse to Entity conversion
fun TMDbMovieImagesResponse.toEntity(contentId: Int, contentType: String): TMDbImagesEntity {
    return TMDbImagesEntity(
        id = "${contentId}_${contentType}",
        contentId = contentId,
        contentType = contentType,
        backdrops = backdrops.map { it.toImageEntity() },
        posters = posters.map { it.toImageEntity() },
        logos = logos?.map { it.toImageEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

fun TMDbImageResponse.toImageEntity(): TMDbImageEntity {
    return TMDbImageEntity(
        filePath = filePath,
        width = width,
        height = height,
        aspectRatio = aspectRatio.toFloat(),
        voteAverage = voteAverage.toFloat(),
        voteCount = voteCount,
        iso6391 = iso6391
    )
}

// TMDbImagesEntity to Response conversion
fun TMDbImagesEntity.toImagesResponse(): TMDbMovieImagesResponse {
    return TMDbMovieImagesResponse(
        backdrops = backdrops.map { it.toImageResponse() },
        posters = posters.map { it.toImageResponse() },
        logos = logos?.map { it.toImageResponse() } ?: emptyList()
    )
}

fun TMDbImageEntity.toImageResponse(): TMDbImageResponse {
    return TMDbImageResponse(
        filePath = filePath,
        width = width,
        height = height,
        aspectRatio = aspectRatio.toDouble(),
        voteAverage = voteAverage.toDouble(),
        voteCount = voteCount,
        iso6391 = iso6391
    )
}

// TMDbMovieVideosResponse to Entity conversion
fun TMDbMovieVideosResponse.toEntity(contentId: Int, contentType: String): TMDbVideosEntity {
    return TMDbVideosEntity(
        id = "${contentId}_${contentType}",
        contentId = contentId,
        contentType = contentType,
        results = results.map { it.toVideoEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

fun TMDbVideoResponse.toVideoEntity(): TMDbVideoEntity {
    return TMDbVideoEntity(
        id = id,
        key = key,
        name = name,
        site = site,
        type = type,
        size = size,
        official = official,
        publishedAt = publishedAt,
        iso6391 = iso6391,
        iso31661 = iso31661
    )
}

// TMDbVideosEntity to Response conversion
fun TMDbVideosEntity.toVideosResponse(): TMDbMovieVideosResponse {
    return TMDbMovieVideosResponse(
        results = results.map { it.toVideoResponse() }
    )
}

fun TMDbVideoEntity.toVideoResponse(): TMDbVideoResponse {
    return TMDbVideoResponse(
        id = id,
        key = key,
        name = name,
        site = site,
        type = type,
        size = size,
        official = official,
        publishedAt = publishedAt,
        iso6391 = iso6391,
        iso31661 = iso31661
    )
}

// Helper function to determine media type
private fun TMDbSearchResultResponse.getMediaType(): String {
    return when {
        title != null && releaseDate != null -> "movie"
        name != null && firstAirDate != null -> "tv"
        title != null -> "movie"
        name != null -> "tv"
        else -> "unknown"
    }
}

// TMDbSearchResponse to Entity conversion with search-specific handling
fun TMDbSearchResponse.toRecommendationEntity(
    contentId: Int,
    contentType: String,
    recommendationType: String,
    page: Int
): TMDbRecommendationEntity {
    return TMDbRecommendationEntity(
        id = "${contentId}_${contentType}_${recommendationType}_${page}",
        contentId = contentId,
        contentType = contentType,
        recommendationType = recommendationType,
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results.map { it.toSearchItemEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

// TMDbSearchResponse to Entity conversion
fun TMDbSearchResponse.toEntity(
    searchId: String,
    query: String,
    page: Int,
    searchType: String
): TMDbSearchResultEntity {
    return TMDbSearchResultEntity(
        id = searchId,
        query = query,
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        searchType = searchType,
        results = results.map { it.toSearchItemEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

fun TMDbSearchResultResponse.toSearchItemEntity(): TMDbSearchItemEntity {
    return TMDbSearchItemEntity(
        id = id,
        mediaType = getMediaType(),
        title = title,
        name = name,
        originalTitle = originalTitle,
        originalName = originalName,
        overview = overview,
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toFloat(),
        voteCount = voteCount,
        popularity = popularity.toFloat(),
        adult = adult,
        video = video,
        originalLanguage = originalLanguage,
        genreIds = genreIds
    )
}

fun TMDbMultiSearchResultResponse.toSearchItemEntity(): TMDbSearchItemEntity {
    return TMDbSearchItemEntity(
        id = id,
        mediaType = mediaType,
        title = title,
        name = name,
        originalTitle = originalTitle,
        originalName = originalName,
        overview = overview,
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toFloat(),
        voteCount = voteCount,
        popularity = popularity.toFloat(),
        adult = adult,
        video = video,
        originalLanguage = originalLanguage,
        genreIds = genreIds
    )
}

// TMDbSearchItemResponse is an alias for TMDbSearchResultResponse, so we'll use the existing function above

// TMDbRecommendationEntity to SearchResponse conversion
fun TMDbRecommendationEntity.toSearchResponse(): TMDbSearchResponse {
    return TMDbSearchResponse(
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results.map { it.toSearchItemResponse() }
    )
}

fun TMDbSearchItemEntity.toSearchItemResponse(): TMDbSearchItemResponse {
    return TMDbSearchItemResponse(
        id = id,
        title = title,
        name = name,
        originalTitle = originalTitle,
        originalName = originalName,
        overview = overview ?: "",
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toDouble(),
        voteCount = voteCount,
        popularity = popularity.toDouble(),
        adult = adult,
        video = video ?: false,
        originalLanguage = originalLanguage,
        genreIds = genreIds
    )
}

// TMDbMultiSearchResponse conversion functions
fun TMDbMultiSearchResponse.toEntity(
    searchId: String,
    query: String,
    page: Int,
    searchType: String
): TMDbSearchResultEntity {
    return TMDbSearchResultEntity(
        id = searchId,
        query = query,
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        searchType = searchType,
        results = results.map { it.toSearchItemEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

fun TMDbSearchResultEntity.toMultiSearchResponse(): TMDbMultiSearchResponse {
    return TMDbMultiSearchResponse(
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results.map { it.toMultiSearchItemResponse() }
    )
}

fun TMDbSearchItemEntity.toMultiSearchItemResponse(): TMDbMultiSearchResultResponse {
    return TMDbMultiSearchResultResponse(
        id = id,
        mediaType = mediaType,
        title = title,
        name = name,
        originalTitle = originalTitle,
        originalName = originalName,
        overview = overview ?: "",
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toDouble(),
        voteCount = voteCount,
        popularity = popularity.toDouble(),
        adult = adult,
        video = video ?: false,
        originalLanguage = originalLanguage,
        genreIds = genreIds
    )
}

// TMDbRecommendationEntity to RecommendationsResponse conversion (duplicate removed)

// TMDbMovieResponse to Entity conversion
fun TMDbMovieResponse.toEntity(): TMDbMovieEntity {
    return TMDbMovieEntity(
        id = id,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        releaseDate = releaseDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toFloat(),
        voteCount = voteCount,
        popularity = popularity.toFloat(),
        adult = adult,
        video = video,
        originalLanguage = originalLanguage,
        runtime = runtime,
        budget = budget,
        revenue = revenue,
        status = status,
        tagline = tagline,
        homepage = homepage,
        imdbId = imdbId,
        genreIds = genres.map { it.id },
        spokenLanguages = spokenLanguages.map { it.name },
        productionCompanies = productionCompanies.map { it.name },
        productionCountries = productionCountries.map { it.name },
        genres = genres.map { it.name }
    )
}

// TMDbMovieEntity to Response conversion
fun TMDbMovieEntity.toMovieResponse(): TMDbMovieResponse {
    return TMDbMovieResponse(
        id = id,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        releaseDate = releaseDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toDouble(),
        voteCount = voteCount,
        popularity = popularity.toDouble(),
        adult = adult,
        video = video,
        originalLanguage = originalLanguage,
        runtime = runtime,
        budget = budget ?: 0,
        revenue = revenue ?: 0,
        status = status ?: "",
        tagline = tagline,
        homepage = homepage,
        imdbId = imdbId,
        genres = genres?.map { TMDbGenreResponse(id = 0, name = it) } ?: emptyList(),
        spokenLanguages = spokenLanguages?.map { TMDbSpokenLanguageResponse(name = it) } ?: emptyList(),
        productionCompanies = productionCompanies?.map { TMDbProductionCompanyResponse(name = it) } ?: emptyList(),
        productionCountries = productionCountries?.map { TMDbProductionCountryResponse(name = it) } ?: emptyList()
    )
}

// TMDbTVResponse to Entity conversion
fun TMDbTVResponse.toEntity(): TMDbTVEntity {
    return TMDbTVEntity(
        id = id,
        name = name,
        originalName = originalName,
        overview = overview,
        firstAirDate = firstAirDate,
        lastAirDate = lastAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toFloat(),
        voteCount = voteCount,
        popularity = popularity.toFloat(),
        adult = adult,
        originalLanguage = originalLanguage,
        numberOfEpisodes = numberOfEpisodes,
        numberOfSeasons = numberOfSeasons,
        status = status,
        type = type,
        homepage = homepage,
        inProduction = inProduction,
        languages = languages,
        originCountry = originCountry,
        genreIds = genres.map { it.id },
        genres = genres.map { it.name },
        networks = networks.map { it.name },
        productionCompanies = productionCompanies.map { it.name },
        productionCountries = productionCountries.map { it.name },
        spokenLanguages = spokenLanguages.map { it.name },
        seasons = seasons.map { it.name },
        lastEpisodeToAir = lastEpisodeToAir?.name,
        nextEpisodeToAir = nextEpisodeToAir?.name,
        lastUpdated = System.currentTimeMillis()
    )
}

// TMDbTVEntity to Response conversion
fun TMDbTVEntity.toTVResponse(): TMDbTVResponse {
    return TMDbTVResponse(
        id = id,
        name = name,
        originalName = originalName,
        overview = overview,
        firstAirDate = firstAirDate,
        lastAirDate = lastAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toDouble(),
        voteCount = voteCount,
        popularity = popularity.toDouble(),
        adult = adult,
        originalLanguage = originalLanguage,
        numberOfEpisodes = numberOfEpisodes ?: 0,
        numberOfSeasons = numberOfSeasons ?: 0,
        status = status ?: "",
        type = type ?: "",
        homepage = homepage,
        inProduction = inProduction ?: false,
        languages = languages ?: emptyList(),
        originCountry = originCountry ?: emptyList(),
        genres = genres?.map { TMDbGenreResponse(id = 0, name = it) } ?: emptyList(),
        networks = networks?.map { TMDbNetworkResponse(name = it) } ?: emptyList(),
        productionCompanies = productionCompanies?.map { TMDbProductionCompanyResponse(name = it) } ?: emptyList(),
        productionCountries = productionCountries?.map { TMDbProductionCountryResponse(name = it) } ?: emptyList(),
        spokenLanguages = spokenLanguages?.map { TMDbSpokenLanguageResponse(name = it) } ?: emptyList(),
        seasons = seasons?.map { TMDbSeasonResponse(name = it) } ?: emptyList(),
        episodeRunTime = emptyList(),
        createdBy = networks?.map { TMDbCreatedByResponse(name = it) } ?: emptyList(),
        lastEpisodeToAir = null,
        nextEpisodeToAir = null
    )
}

// TMDbTVImagesResponse to Entity conversion
fun TMDbTVImagesResponse.toEntity(contentId: Int, contentType: String): TMDbImagesEntity {
    return TMDbImagesEntity(
        id = "${contentId}_${contentType}",
        contentId = contentId,
        contentType = contentType,
        backdrops = backdrops.map { it.toImageEntity() },
        posters = posters.map { it.toImageEntity() },
        logos = logos?.map { it.toImageEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

// TMDbImagesEntity to TV Images Response conversion
fun TMDbImagesEntity.toTVImagesResponse(): TMDbTVImagesResponse {
    return TMDbTVImagesResponse(
        backdrops = backdrops.map { it.toImageResponse() },
        posters = posters.map { it.toImageResponse() },
        logos = logos?.map { it.toImageResponse() } ?: emptyList()
    )
}

// TMDbTVVideosResponse to Entity conversion
fun TMDbTVVideosResponse.toEntity(contentId: Int, contentType: String): TMDbVideosEntity {
    return TMDbVideosEntity(
        id = "${contentId}_${contentType}",
        contentId = contentId,
        contentType = contentType,
        results = results.map { it.toVideoEntity() },
        lastUpdated = System.currentTimeMillis()
    )
}

// TMDbVideosEntity to TV Videos Response conversion
fun TMDbVideosEntity.toTVVideosResponse(): TMDbTVVideosResponse {
    return TMDbTVVideosResponse(
        results = results.map { it.toVideoResponse() }
    )
}