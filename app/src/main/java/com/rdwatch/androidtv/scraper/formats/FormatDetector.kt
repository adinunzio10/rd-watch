package com.rdwatch.androidtv.scraper.formats

import com.rdwatch.androidtv.scraper.parser.ManifestFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced format detection for manifest files
 */
@Singleton
class FormatDetector
    @Inject
    constructor() {
        /**
         * Detect format with confidence scoring
         */
        fun detectWithConfidence(
            content: String,
            url: String? = null,
        ): FormatDetectionResult {
            val trimmedContent = content.trim()
            val scores = mutableMapOf<ManifestFormat, Double>()

            // URL-based detection
            url?.let { urlString ->
                when {
                    urlString.endsWith(".json", ignoreCase = true) ->
                        scores[ManifestFormat.JSON] = (scores[ManifestFormat.JSON] ?: 0.0) + 0.3
                    urlString.endsWith(".yml", ignoreCase = true) ||
                        urlString.endsWith(".yaml", ignoreCase = true) ->
                        scores[ManifestFormat.YAML] = (scores[ManifestFormat.YAML] ?: 0.0) + 0.3
                    urlString.endsWith(".xml", ignoreCase = true) ->
                        scores[ManifestFormat.XML] = (scores[ManifestFormat.XML] ?: 0.0) + 0.3
                }
            }

            // Content-based detection
            analyzeJsonContent(trimmedContent, scores)
            analyzeYamlContent(trimmedContent, scores)
            analyzeXmlContent(trimmedContent, scores)

            // Additional heuristics
            analyzeContentStructure(trimmedContent, scores)

            val bestMatch = scores.maxByOrNull { it.value }
            val confidence = bestMatch?.value ?: 0.0
            val detectedFormat = bestMatch?.key ?: ManifestFormat.JSON

            return FormatDetectionResult(
                format = detectedFormat,
                confidence = confidence,
                allScores = scores.toMap(),
                reasoning = generateReasoning(trimmedContent, scores),
            )
        }

        /**
         * Simple format detection (legacy compatibility)
         */
        fun detectFormat(
            content: String,
            url: String? = null,
        ): ManifestFormat {
            return detectWithConfidence(content, url).format
        }

        private fun analyzeJsonContent(
            content: String,
            scores: MutableMap<ManifestFormat, Double>,
        ) {
            var score = 0.0

            // Structure indicators
            when {
                content.startsWith("{") && content.endsWith("}") -> score += 0.5
                content.startsWith("[") && content.endsWith("]") -> score += 0.3
            }

            // JSON-specific patterns
            if (content.contains(Regex("\"[^\"]+\"\\s*:"))) score += 0.2
            if (content.contains(Regex("\\{[^}]*\"[^\"]+\"[^}]*\\}"))) score += 0.2

            // Stremio manifest specific patterns
            if (content.contains("\"id\"")) score += 0.1
            if (content.contains("\"name\"")) score += 0.1
            if (content.contains("\"version\"")) score += 0.1
            if (content.contains("\"resources\"")) score += 0.1
            if (content.contains("\"catalogs\"")) score += 0.1

            // JSON syntax validation
            try {
                parseJsonBasic(content)
                score += 0.3
            } catch (e: Exception) {
                score -= 0.5
            }

            if (score > 0) {
                scores[ManifestFormat.JSON] = score
            }
        }

        private fun analyzeYamlContent(
            content: String,
            scores: MutableMap<ManifestFormat, Double>,
        ) {
            var score = 0.0

            // YAML indicators
            if (content.startsWith("---")) score += 0.3
            if (content.contains(Regex("^[a-zA-Z_][a-zA-Z0-9_]*\\s*:", RegexOption.MULTILINE))) score += 0.4
            if (content.contains(Regex("^\\s*-\\s+", RegexOption.MULTILINE))) score += 0.2

            // YAML-specific patterns
            if (content.contains("id:")) score += 0.1
            if (content.contains("name:")) score += 0.1
            if (content.contains("version:")) score += 0.1
            if (content.contains("resources:")) score += 0.1
            if (content.contains("catalogs:")) score += 0.1

            // Absence of JSON indicators
            if (!content.contains("{") && !content.contains("}")) score += 0.1
            if (!content.contains("\"")) score += 0.1

            if (score > 0) {
                scores[ManifestFormat.YAML] = score
            }
        }

        private fun analyzeXmlContent(
            content: String,
            scores: MutableMap<ManifestFormat, Double>,
        ) {
            var score = 0.0

            // XML indicators
            if (content.startsWith("<?xml")) score += 0.4
            if (content.startsWith("<manifest")) score += 0.3
            if (content.startsWith("<addon")) score += 0.3

            // XML structure patterns
            if (content.contains(Regex("<[a-zA-Z][^>]*>"))) score += 0.3
            if (content.contains(Regex("</[a-zA-Z][^>]*>"))) score += 0.2

            // XML-specific patterns for manifests
            if (content.contains("<id>")) score += 0.1
            if (content.contains("<name>")) score += 0.1
            if (content.contains("<version>")) score += 0.1
            if (content.contains("<resources>")) score += 0.1
            if (content.contains("<catalogs>")) score += 0.1

            if (score > 0) {
                scores[ManifestFormat.XML] = score
            }
        }

        private fun analyzeContentStructure(
            content: String,
            scores: MutableMap<ManifestFormat, Double>,
        ) {
            val lines = content.lines()
            val nonEmptyLines = lines.filter { it.trim().isNotEmpty() }

            // Analyze indentation patterns
            val indentationPattern = detectIndentationPattern(nonEmptyLines)
            when (indentationPattern) {
                IndentationPattern.SPACES -> {
                    scores[ManifestFormat.YAML] = (scores[ManifestFormat.YAML] ?: 0.0) + 0.1
                }
                IndentationPattern.MIXED -> {
                    scores[ManifestFormat.JSON] = (scores[ManifestFormat.JSON] ?: 0.0) + 0.1
                }
                else -> { /* No adjustment */ }
            }

            // Character frequency analysis
            val charFreq = analyzeCharacterFrequency(content)

            // High brace frequency suggests JSON
            if (charFreq.braceRatio > 0.02) {
                scores[ManifestFormat.JSON] = (scores[ManifestFormat.JSON] ?: 0.0) + 0.1
            }

            // High colon frequency with low brace frequency suggests YAML
            if (charFreq.colonRatio > 0.01 && charFreq.braceRatio < 0.01) {
                scores[ManifestFormat.YAML] = (scores[ManifestFormat.YAML] ?: 0.0) + 0.1
            }

            // High angle bracket frequency suggests XML
            if (charFreq.angleRatio > 0.02) {
                scores[ManifestFormat.XML] = (scores[ManifestFormat.XML] ?: 0.0) + 0.1
            }
        }

        private fun detectIndentationPattern(lines: List<String>): IndentationPattern {
            val indentedLines = lines.filter { it.startsWith(" ") || it.startsWith("\t") }

            val spaceIndents = indentedLines.count { it.startsWith(" ") }
            val tabIndents = indentedLines.count { it.startsWith("\t") }

            return when {
                spaceIndents > tabIndents * 2 -> IndentationPattern.SPACES
                tabIndents > spaceIndents * 2 -> IndentationPattern.TABS
                indentedLines.isNotEmpty() -> IndentationPattern.MIXED
                else -> IndentationPattern.NONE
            }
        }

        private fun analyzeCharacterFrequency(content: String): CharacterFrequency {
            val total = content.length.toDouble()
            return CharacterFrequency(
                braceRatio = (content.count { it == '{' || it == '}' }) / total,
                colonRatio = content.count { it == ':' } / total,
                angleRatio = (content.count { it == '<' || it == '>' }) / total,
                quoteRatio = content.count { it == '"' } / total,
            )
        }

        private fun generateReasoning(
            content: String,
            scores: Map<ManifestFormat, Double>,
        ): List<String> {
            val reasoning = mutableListOf<String>()

            if (content.startsWith("{")) {
                reasoning.add("Content starts with '{' (JSON indicator)")
            }

            if (content.startsWith("---")) {
                reasoning.add("Content starts with '---' (YAML document separator)")
            }

            if (content.startsWith("<?xml")) {
                reasoning.add("Content starts with '<?xml' (XML declaration)")
            }

            if (content.contains("\"id\"") || content.contains("id:")) {
                reasoning.add("Contains 'id' field (manifest structure)")
            }

            scores.forEach { (format, score) ->
                if (score > 0.5) {
                    reasoning.add("High confidence for $format (score: ${"%.2f".format(score)})")
                }
            }

            return reasoning
        }

        private fun parseJsonBasic(content: String) {
            // Basic JSON validation - check balanced braces and quotes
            var braceDepth = 0
            var inString = false
            var escaped = false

            for (char in content) {
                when {
                    escaped -> escaped = false
                    char == '\\' && inString -> escaped = true
                    char == '"' -> inString = !inString
                    !inString && char == '{' -> braceDepth++
                    !inString && char == '}' -> braceDepth--
                }
            }

            if (braceDepth != 0) {
                throw IllegalArgumentException("Unbalanced braces")
            }

            if (inString) {
                throw IllegalArgumentException("Unterminated string")
            }
        }
    }

/**
 * Result of format detection with confidence and reasoning
 */
data class FormatDetectionResult(
    val format: ManifestFormat,
    val confidence: Double,
    val allScores: Map<ManifestFormat, Double>,
    val reasoning: List<String>,
) {
    val isHighConfidence: Boolean get() = confidence >= 0.7
    val isMediumConfidence: Boolean get() = confidence >= 0.4
    val isLowConfidence: Boolean get() = confidence < 0.4
}

/**
 * Indentation pattern analysis
 */
private enum class IndentationPattern {
    SPACES,
    TABS,
    MIXED,
    NONE,
}

/**
 * Character frequency analysis
 */
private data class CharacterFrequency(
    val braceRatio: Double,
    val colonRatio: Double,
    val angleRatio: Double,
    val quoteRatio: Double,
)
