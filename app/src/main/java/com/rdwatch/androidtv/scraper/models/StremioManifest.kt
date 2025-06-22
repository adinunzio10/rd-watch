package com.rdwatch.androidtv.scraper.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Stremio addon manifest format
 * Compatible with Torrentio/Knightcrawler and other Stremio addons
 */
@JsonClass(generateAdapter = true)
data class StremioManifest(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "name")
    val name: String,
    
    @Json(name = "version")
    val version: String,
    
    @Json(name = "description")
    val description: String? = null,
    
    @Json(name = "logo")
    val logo: String? = null,
    
    @Json(name = "background")
    val background: String? = null,
    
    @Json(name = "contactEmail")
    val contactEmail: String? = null,
    
    @Json(name = "catalogs")
    val catalogs: List<StremioManifestCatalog> = emptyList(),
    
    @Json(name = "resources")
    val resources: List<StremioManifestResource> = emptyList(),
    
    @Json(name = "types")
    val types: List<String> = emptyList(),
    
    @Json(name = "idPrefixes")
    val idPrefixes: List<String>? = null,
    
    @Json(name = "behaviorHints")
    val behaviorHints: StremioManifestBehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class StremioManifestCatalog(
    @Json(name = "type")
    val type: String,
    
    @Json(name = "id")
    val id: String,
    
    @Json(name = "name")
    val name: String,
    
    @Json(name = "genres")
    val genres: List<String>? = null,
    
    @Json(name = "extra")
    val extra: List<StremioManifestExtra>? = null
)

@JsonClass(generateAdapter = true)
data class StremioManifestResource(
    @Json(name = "name")
    val name: String,
    
    @Json(name = "types")
    val types: List<String>,
    
    @Json(name = "idPrefixes")
    val idPrefixes: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class StremioManifestExtra(
    @Json(name = "name")
    val name: String,
    
    @Json(name = "isRequired")
    val isRequired: Boolean? = null,
    
    @Json(name = "options")
    val options: List<String>? = null,
    
    @Json(name = "optionsLimit")
    val optionsLimit: Int? = null
)

@JsonClass(generateAdapter = true)
data class StremioManifestBehaviorHints(
    @Json(name = "adult")
    val adult: Boolean? = null,
    
    @Json(name = "p2p")
    val p2p: Boolean? = null,
    
    @Json(name = "configurable")
    val configurable: Boolean? = null,
    
    @Json(name = "configurationRequired")
    val configurationRequired: Boolean? = null
)