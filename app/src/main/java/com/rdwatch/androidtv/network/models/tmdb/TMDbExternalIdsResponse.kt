package com.rdwatch.androidtv.network.models.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TMDb External IDs response model for TV shows
 * Used to get external identifiers like IMDb ID, TVDB ID, etc.
 * 
 * API endpoint: /tv/{tv_id}/external_ids
 * 
 * TODO: Future refactoring - consolidate with movie/person external IDs for unified architecture
 * Currently movies get imdb_id directly from TMDbMovieResponse, persons from TMDbCreditsResponse
 * This creates inconsistent architecture - see Option A in planning docs for unified approach
 */
@JsonClass(generateAdapter = true)
data class TMDbExternalIdsResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "imdb_id") val imdbId: String? = null,
    @Json(name = "tvdb_id") val tvdbId: Int? = null,
    @Json(name = "freebase_mid") val freebaseMid: String? = null,
    @Json(name = "freebase_id") val freebaseId: String? = null,
    @Json(name = "tvrage_id") val tvrageId: Int? = null,
    @Json(name = "wikidata_id") val wikidataId: String? = null,
    @Json(name = "facebook_id") val facebookId: String? = null,
    @Json(name = "instagram_id") val instagramId: String? = null,
    @Json(name = "twitter_id") val twitterId: String? = null
)