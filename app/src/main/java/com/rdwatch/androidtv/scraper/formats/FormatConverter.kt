package com.rdwatch.androidtv.scraper.formats

import com.rdwatch.androidtv.scraper.models.ManifestException
import com.rdwatch.androidtv.scraper.models.ManifestParsingException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.StremioManifest
import com.rdwatch.androidtv.scraper.parser.ManifestFormat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts between different manifest formats
 * Provides format normalization and conversion utilities
 */
@Singleton
class FormatConverter @Inject constructor() {
    
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val stremioAdapter: JsonAdapter<StremioManifest> = 
        moshi.adapter(StremioManifest::class.java)
    
    /**
     * Convert any supported format to normalized JSON format
     */
    fun normalizeToJson(
        content: String,
        sourceFormat: ManifestFormat
    ): ManifestResult<String> {
        return try {
            when (sourceFormat) {
                ManifestFormat.JSON -> validateAndNormalizeJson(content)
                ManifestFormat.YAML -> convertYamlToJson(content)
                ManifestFormat.XML -> convertXmlToJson(content)
                ManifestFormat.AUTO_DETECT -> {
                    ManifestResult.Error(
                        ManifestParsingException(
                            "Cannot normalize auto-detected format. Specify explicit format.",
                            format = sourceFormat.name
                        )
                    )
                }
            }
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestParsingException(
                    "Format conversion failed: ${e.message}",
                    cause = e,
                    format = sourceFormat.name
                )
            )
        }
    }
    
    /**
     * Convert Stremio manifest to different output formats
     */
    fun convertStremioManifest(
        manifest: StremioManifest,
        targetFormat: ManifestFormat
    ): ManifestResult<String> {
        return try {
            when (targetFormat) {
                ManifestFormat.JSON -> {
                    val json = stremioAdapter.toJson(manifest)
                    ManifestResult.Success(json)
                }
                ManifestFormat.YAML -> convertJsonToYaml(stremioAdapter.toJson(manifest))
                ManifestFormat.XML -> convertJsonToXml(stremioAdapter.toJson(manifest))
                ManifestFormat.AUTO_DETECT -> {
                    ManifestResult.Error(
                        ManifestParsingException(
                            "Cannot convert to auto-detect format. Specify explicit format.",
                            format = targetFormat.name
                        )
                    )
                }
            }
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestParsingException(
                    "Manifest conversion failed: ${e.message}",
                    cause = e,
                    format = targetFormat.name
                )
            )
        }
    }
    
    /**
     * Auto-detect format and convert to standard JSON
     */
    fun autoConvertToJson(content: String): ManifestResult<String> {
        val detectedFormat = detectFormat(content)
        return normalizeToJson(content, detectedFormat)
    }
    
    /**
     * Detect format based on content analysis
     */
    fun detectFormat(content: String): ManifestFormat {
        val trimmedContent = content.trim()
        
        return when {
            // JSON detection
            (trimmedContent.startsWith("{") && trimmedContent.endsWith("}")) ||
            (trimmedContent.startsWith("[") && trimmedContent.endsWith("]")) -> ManifestFormat.JSON
            
            // YAML detection
            trimmedContent.startsWith("---") ||
            trimmedContent.contains(Regex("^[a-zA-Z_][a-zA-Z0-9_]*\\s*:", RegexOption.MULTILINE)) ||
            trimmedContent.contains("id:") -> ManifestFormat.YAML
            
            // XML detection
            trimmedContent.startsWith("<?xml") ||
            trimmedContent.startsWith("<manifest") ||
            trimmedContent.startsWith("<addon") ||
            trimmedContent.contains(Regex("<[a-zA-Z][^>]*>")) -> ManifestFormat.XML
            
            else -> ManifestFormat.JSON // Default fallback
        }
    }
    
    /**
     * Validate and prettify JSON
     */
    private fun validateAndNormalizeJson(json: String): ManifestResult<String> {
        return try {
            // Parse to validate
            val manifestData = moshi.adapter(Map::class.java).fromJson(json)
            if (manifestData == null) {
                return ManifestResult.Error(
                    ManifestParsingException(
                        "Invalid JSON content",
                        format = "JSON"
                    )
                )
            }
            
            // Re-serialize with proper formatting
            val normalizedJson = moshi.adapter(Map::class.java).indent("  ").toJson(manifestData)
            ManifestResult.Success(normalizedJson)
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestParsingException(
                    "JSON validation failed: ${e.message}",
                    cause = e,
                    format = "JSON"
                )
            )
        }
    }
    
    /**
     * Convert YAML to JSON (placeholder implementation)
     */
    private fun convertYamlToJson(yaml: String): ManifestResult<String> {
        // TODO: Implement YAML parsing using SnakeYAML library
        // For now, attempt basic YAML-like parsing for simple cases
        return try {
            val jsonLikeContent = convertSimpleYamlToJson(yaml)
            validateAndNormalizeJson(jsonLikeContent)
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestParsingException(
                    "YAML to JSON conversion not fully implemented: ${e.message}",
                    cause = e,
                    format = "YAML"
                )
            )
        }
    }
    
    /**
     * Convert XML to JSON (placeholder implementation)
     */
    private fun convertXmlToJson(xml: String): ManifestResult<String> {
        // TODO: Implement XML parsing and conversion
        return ManifestResult.Error(
            ManifestParsingException(
                "XML to JSON conversion not yet implemented",
                format = "XML"
            )
        )
    }
    
    /**
     * Convert JSON to YAML (placeholder implementation)
     */
    private fun convertJsonToYaml(json: String): ManifestResult<String> {
        // TODO: Implement JSON to YAML conversion
        return ManifestResult.Error(
            ManifestParsingException(
                "JSON to YAML conversion not yet implemented",
                format = "JSON->YAML"
            )
        )
    }
    
    /**
     * Convert JSON to XML (placeholder implementation)
     */
    private fun convertJsonToXml(json: String): ManifestResult<String> {
        // TODO: Implement JSON to XML conversion
        return ManifestResult.Error(
            ManifestParsingException(
                "JSON to XML conversion not yet implemented",
                format = "JSON->XML"
            )
        )
    }
    
    /**
     * Basic YAML-like to JSON conversion for simple structures
     * This is a minimal implementation for demonstration
     */
    private fun convertSimpleYamlToJson(yaml: String): String {
        val lines = yaml.lines().filter { it.isNotBlank() && !it.trim().startsWith("#") }
        val jsonMap = mutableMapOf<String, Any>()
        
        for (line in lines) {
            if (line.contains(":")) {
                val parts = line.split(":", 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSurrounding("\"")
                    
                    // Try to parse as different types
                    jsonMap[key] = when {
                        value.equals("true", true) -> true
                        value.equals("false", true) -> false
                        value.toIntOrNull() != null -> value.toInt()
                        value.toDoubleOrNull() != null -> value.toDouble()
                        value.startsWith("[") && value.endsWith("]") -> {
                            // Simple array parsing
                            value.substring(1, value.length - 1)
                                .split(",")
                                .map { it.trim().removeSurrounding("\"") }
                        }
                        else -> value
                    }
                }
            }
        }
        
        return moshi.adapter(Map::class.java).toJson(jsonMap)
    }
    
    /**
     * Get supported input formats
     */
    fun getSupportedInputFormats(): Set<ManifestFormat> {
        return setOf(
            ManifestFormat.JSON,
            // ManifestFormat.YAML, // TODO: Enable when fully implemented
            // ManifestFormat.XML   // TODO: Enable when fully implemented
        )
    }
    
    /**
     * Get supported output formats
     */
    fun getSupportedOutputFormats(): Set<ManifestFormat> {
        return setOf(
            ManifestFormat.JSON
            // ManifestFormat.YAML, // TODO: Enable when fully implemented
            // ManifestFormat.XML   // TODO: Enable when fully implemented
        )
    }
    
    /**
     * Check if format conversion is supported
     */
    fun isConversionSupported(from: ManifestFormat, to: ManifestFormat): Boolean {
        return when {
            from == to -> true
            from == ManifestFormat.JSON && getSupportedOutputFormats().contains(to) -> true
            getSupportedInputFormats().contains(from) && to == ManifestFormat.JSON -> true
            else -> false
        }
    }
}