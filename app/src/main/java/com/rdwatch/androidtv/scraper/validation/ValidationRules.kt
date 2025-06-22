package com.rdwatch.androidtv.scraper.validation

import com.rdwatch.androidtv.scraper.models.ValidationSeverity

/**
 * Configurable validation rules for manifest validation
 */
data class ValidationRules(
    val enforceHttps: Boolean = true,
    val maxNameLength: Int = 100,
    val maxDescriptionLength: Int = 500,
    val maxResourceCount: Int = 50,
    val maxCatalogCount: Int = 20,
    val requireSemVer: Boolean = false,
    val allowedResourceTypes: Set<String> = defaultResourceTypes,
    val allowedCatalogTypes: Set<String> = defaultCatalogTypes,
    val strictValidation: Boolean = false,
    val validationLevel: ValidationLevel = ValidationLevel.STANDARD
) {
    companion object {
        val defaultResourceTypes = setOf(
            "catalog", "meta", "stream", "subtitles", "addon_catalog"
        )
        
        val defaultCatalogTypes = setOf(
            "movie", "series", "channel", "tv", "music", "book", "game"
        )
        
        fun strict() = ValidationRules(
            enforceHttps = true,
            requireSemVer = true,
            strictValidation = true,
            validationLevel = ValidationLevel.STRICT
        )
        
        fun permissive() = ValidationRules(
            enforceHttps = false,
            requireSemVer = false,
            strictValidation = false,
            validationLevel = ValidationLevel.PERMISSIVE
        )
    }
}

/**
 * Validation levels determining rule enforcement
 */
enum class ValidationLevel {
    PERMISSIVE,  // Only critical errors
    STANDARD,    // Standard validation with warnings
    STRICT       // Strict validation with all rules enforced
}

/**
 * Validation rule definitions
 */
data class ValidationRule(
    val name: String,
    val description: String,
    val severity: ValidationSeverity,
    val enabled: Boolean = true,
    val condition: (Any?) -> Boolean,
    val message: String
)

/**
 * Registry of validation rules
 */
object ValidationRuleRegistry {
    
    private val rules = mutableMapOf<String, ValidationRule>()
    
    init {
        registerDefaultRules()
    }
    
    fun registerRule(rule: ValidationRule) {
        rules[rule.name] = rule
    }
    
    fun getRule(name: String): ValidationRule? = rules[name]
    
    fun getAllRules(): Map<String, ValidationRule> = rules.toMap()
    
    fun getRulesByCategory(category: String): Map<String, ValidationRule> {
        return rules.filter { it.key.startsWith(category) }
    }
    
    private fun registerDefaultRules() {
        // Required field rules
        registerRule(
            ValidationRule(
                name = "required.id",
                description = "Manifest ID is required",
                severity = ValidationSeverity.ERROR,
                condition = { value -> value is String && value.isNotBlank() },
                message = "Manifest ID is required and cannot be empty"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "required.name",
                description = "Manifest name is required",
                severity = ValidationSeverity.ERROR,
                condition = { value -> value is String && value.isNotBlank() },
                message = "Manifest name is required and cannot be empty"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "required.version",
                description = "Manifest version is required",
                severity = ValidationSeverity.ERROR,
                condition = { value -> value is String && value.isNotBlank() },
                message = "Manifest version is required and cannot be empty"
            )
        )
        
        // Format rules
        registerRule(
            ValidationRule(
                name = "format.version.semver",
                description = "Version should follow semantic versioning",
                severity = ValidationSeverity.WARNING,
                condition = { value -> 
                    value is String && value.matches(Regex("^\\d+\\.\\d+\\.\\d+(?:-[a-zA-Z0-9]+)?(?:\\+[a-zA-Z0-9]+)?$"))
                },
                message = "Version should follow semantic versioning format (e.g., 1.0.0)"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "format.id.alphanumeric",
                description = "ID should contain only safe characters",
                severity = ValidationSeverity.WARNING,
                condition = { value -> 
                    value is String && !value.contains(Regex("[^a-zA-Z0-9._-]"))
                },
                message = "Manifest ID should only contain alphanumeric characters, dots, hyphens, and underscores"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "format.email",
                description = "Email should be valid format",
                severity = ValidationSeverity.WARNING,
                condition = { value -> 
                    value == null || (value is String && (value.isBlank() || 
                    value.matches(Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"))))
                },
                message = "Contact email format is invalid"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "format.url",
                description = "URLs should be valid HTTP/HTTPS",
                severity = ValidationSeverity.ERROR,
                condition = { value -> 
                    value == null || (value is String && (value.isBlank() || 
                    value.matches(Regex("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))))
                },
                message = "URL format is invalid"
            )
        )
        
        // Length rules
        registerRule(
            ValidationRule(
                name = "length.name.max",
                description = "Name should not be too long",
                severity = ValidationSeverity.WARNING,
                condition = { value -> value !is String || value.length <= 100 },
                message = "Manifest name is too long (maximum 100 characters)"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "length.description.max",
                description = "Description should not be too long",
                severity = ValidationSeverity.WARNING,
                condition = { value -> value !is String || value.length <= 500 },
                message = "Description is too long (maximum 500 characters)"
            )
        )
        
        // Security rules
        registerRule(
            ValidationRule(
                name = "security.https",
                description = "URLs should use HTTPS",
                severity = ValidationSeverity.WARNING,
                condition = { value -> 
                    value !is String || value.isBlank() || value.startsWith("https://")
                },
                message = "URL should use HTTPS for security"
            )
        )
        
        // Business rules
        registerRule(
            ValidationRule(
                name = "business.resources_or_catalogs",
                description = "Must have resources or catalogs",
                severity = ValidationSeverity.ERROR,
                condition = { value -> 
                    // This rule is more complex and should be handled in validator
                    true
                },
                message = "Manifest must have at least one resource or catalog"
            )
        )
        
        // Range rules
        registerRule(
            ValidationRule(
                name = "range.priority.positive",
                description = "Priority should not be negative",
                severity = ValidationSeverity.ERROR,
                condition = { value -> value !is Number || value.toInt() >= 0 },
                message = "Priority order cannot be negative"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "range.timeout.positive",
                description = "Timeout should be positive",
                severity = ValidationSeverity.ERROR,
                condition = { value -> value is Number && value.toInt() > 0 },
                message = "Timeout must be positive"
            )
        )
        
        registerRule(
            ValidationRule(
                name = "range.ratelimit.nonnegative",
                description = "Rate limit should not be negative",
                severity = ValidationSeverity.ERROR,
                condition = { value -> value !is Number || value.toLong() >= 0 },
                message = "Rate limit cannot be negative"
            )
        )
    }
}

/**
 * Rule-based validator that uses configurable rules
 */
class RuleBasedValidator(
    private val rules: ValidationRules = ValidationRules()
) {
    
    fun validateField(fieldName: String, value: Any?): List<com.rdwatch.androidtv.scraper.models.ValidationError> {
        val errors = mutableListOf<com.rdwatch.androidtv.scraper.models.ValidationError>()
        
        // Get applicable rules for this field
        val applicableRules = ValidationRuleRegistry.getAllRules().values.filter { rule ->
            rule.enabled && rule.name.contains(fieldName) || 
            rule.name.startsWith("format.") || 
            rule.name.startsWith("length.") ||
            rule.name.startsWith("security.")
        }
        
        applicableRules.forEach { rule ->
            if (!rule.condition(value)) {
                // Skip warnings in strict mode if not enforced
                if (rule.severity == ValidationSeverity.WARNING && 
                    rules.validationLevel == ValidationLevel.PERMISSIVE) {
                    return@forEach
                }
                
                errors.add(
                    com.rdwatch.androidtv.scraper.models.ValidationError(
                        field = fieldName,
                        message = rule.message,
                        value = value,
                        rule = rule.name,
                        severity = rule.severity
                    )
                )
            }
        }
        
        return errors
    }
    
    fun validateWithCustomRule(
        fieldName: String, 
        value: Any?, 
        rule: ValidationRule
    ): com.rdwatch.androidtv.scraper.models.ValidationError? {
        return if (!rule.condition(value)) {
            com.rdwatch.androidtv.scraper.models.ValidationError(
                field = fieldName,
                message = rule.message,
                value = value,
                rule = rule.name,
                severity = rule.severity
            )
        } else {
            null
        }
    }
}