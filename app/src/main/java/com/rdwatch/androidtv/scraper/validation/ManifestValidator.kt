package com.rdwatch.androidtv.scraper.validation

import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ManifestValidationException
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.StremioManifest
import com.rdwatch.androidtv.scraper.models.ValidationError
import com.rdwatch.androidtv.scraper.models.ValidationSeverity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive manifest validation using configurable rules
 */
@Singleton
class ManifestValidator @Inject constructor() {
    
    private val urlRegex = Regex(
        "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    )
    
    private val emailRegex = Regex(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    )
    
    private val versionRegex = Regex(
        "^\\d+\\.\\d+\\.\\d+(?:-[a-zA-Z0-9]+)?(?:\\+[a-zA-Z0-9]+)?$"
    )
    
    /**
     * Validate a Stremio manifest
     */
    fun validateStremioManifest(
        manifest: StremioManifest,
        sourceUrl: String? = null
    ): ManifestResult<Boolean> {
        val errors = mutableListOf<ValidationError>()
        
        // Required field validation
        validateRequiredFields(manifest, errors)
        
        // Format validation
        validateFormats(manifest, errors)
        
        // Business rule validation
        validateBusinessRules(manifest, errors)
        
        // Resource validation
        validateResources(manifest, errors)
        
        // Catalog validation
        validateCatalogs(manifest, errors)
        
        // URL validation
        sourceUrl?.let { validateSourceUrl(it, errors) }
        
        return if (errors.isEmpty()) {
            ManifestResult.Success(true)
        } else {
            ManifestResult.Error(
                ManifestValidationException(
                    "Manifest validation failed with ${errors.size} errors",
                    validationErrors = errors
                )
            )
        }
    }
    
    /**
     * Validate a ScraperManifest
     */
    fun validateScraperManifest(manifest: ScraperManifest): ManifestResult<Boolean> {
        val errors = mutableListOf<ValidationError>()
        
        // Validate the underlying Stremio manifest
        when (val stremioResult = validateStremioManifest(manifest.stremioManifest, manifest.sourceUrl)) {
            is ManifestResult.Error -> {
                if (stremioResult.exception is ManifestValidationException) {
                    errors.addAll(stremioResult.exception.validationErrors)
                } else {
                    errors.add(
                        ValidationError(
                            field = "stremioManifest",
                            message = "Stremio manifest validation failed: ${stremioResult.exception.message}",
                            severity = ValidationSeverity.ERROR
                        )
                    )
                }
            }
            is ManifestResult.Success -> { /* Continue with additional validation */ }
        }
        
        // Additional ScraperManifest validation
        validateScraperManifestFields(manifest, errors)
        
        return if (errors.isEmpty()) {
            ManifestResult.Success(true)
        } else {
            ManifestResult.Error(
                ManifestValidationException(
                    "Scraper manifest validation failed with ${errors.size} errors",
                    validationErrors = errors
                )
            )
        }
    }
    
    /**
     * Validate multiple manifests
     */
    fun validateManifests(manifests: List<ScraperManifest>): ManifestResult<Map<String, Boolean>> {
        val results = mutableMapOf<String, Boolean>()
        val allErrors = mutableListOf<ValidationError>()
        
        manifests.forEach { manifest ->
            when (val result = validateScraperManifest(manifest)) {
                is ManifestResult.Success -> results[manifest.id] = result.data
                is ManifestResult.Error -> {
                    results[manifest.id] = false
                    if (result.exception is ManifestValidationException) {
                        allErrors.addAll(result.exception.validationErrors)
                    }
                }
            }
        }
        
        return if (allErrors.isEmpty()) {
            ManifestResult.Success(results)
        } else {
            ManifestResult.Error(
                ManifestValidationException(
                    "Batch validation failed for some manifests",
                    validationErrors = allErrors
                )
            )
        }
    }
    
    /**
     * Get validation errors for a manifest without throwing
     */
    fun getValidationErrors(manifest: StremioManifest): List<ValidationError> {
        return when (val result = validateStremioManifest(manifest)) {
            is ManifestResult.Error -> {
                if (result.exception is ManifestValidationException) {
                    result.exception.validationErrors
                } else {
                    listOf(
                        ValidationError(
                            field = "manifest",
                            message = result.exception.message ?: "Unknown validation error",
                            severity = ValidationSeverity.ERROR
                        )
                    )
                }
            }
            is ManifestResult.Success -> emptyList()
        }
    }
    
    private fun validateRequiredFields(manifest: StremioManifest, errors: MutableList<ValidationError>) {
        if (manifest.id.isBlank()) {
            errors.add(
                ValidationError(
                    field = "id",
                    message = "Manifest ID is required and cannot be empty",
                    value = manifest.id,
                    rule = "required",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        if (manifest.name.isBlank()) {
            errors.add(
                ValidationError(
                    field = "name",
                    message = "Manifest name is required and cannot be empty",
                    value = manifest.name,
                    rule = "required",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        if (manifest.version.isBlank()) {
            errors.add(
                ValidationError(
                    field = "version",
                    message = "Manifest version is required and cannot be empty",
                    value = manifest.version,
                    rule = "required",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
    }
    
    private fun validateFormats(manifest: StremioManifest, errors: MutableList<ValidationError>) {
        // Validate version format
        if (manifest.version.isNotBlank() && !versionRegex.matches(manifest.version)) {
            errors.add(
                ValidationError(
                    field = "version",
                    message = "Version should follow semantic versioning format (e.g., 1.0.0)",
                    value = manifest.version,
                    rule = "format",
                    severity = ValidationSeverity.WARNING
                )
            )
        }
        
        // Validate email format
        manifest.contactEmail?.let { email ->
            if (email.isNotBlank() && !emailRegex.matches(email)) {
                errors.add(
                    ValidationError(
                        field = "contactEmail",
                        message = "Contact email format is invalid",
                        value = email,
                        rule = "format",
                        severity = ValidationSeverity.WARNING
                    )
                )
            }
        }
        
        // Validate URL formats
        manifest.logo?.let { logo ->
            if (logo.isNotBlank() && !urlRegex.matches(logo)) {
                errors.add(
                    ValidationError(
                        field = "logo",
                        message = "Logo URL format is invalid",
                        value = logo,
                        rule = "format",
                        severity = ValidationSeverity.WARNING
                    )
                )
            }
        }
        
        manifest.background?.let { background ->
            if (background.isNotBlank() && !urlRegex.matches(background)) {
                errors.add(
                    ValidationError(
                        field = "background",
                        message = "Background URL format is invalid",
                        value = background,
                        rule = "format",
                        severity = ValidationSeverity.WARNING
                    )
                )
            }
        }
    }
    
    private fun validateBusinessRules(manifest: StremioManifest, errors: MutableList<ValidationError>) {
        // Manifest must have at least one resource or catalog
        if (manifest.resources.isEmpty() && manifest.catalogs.isEmpty()) {
            errors.add(
                ValidationError(
                    field = "resources/catalogs",
                    message = "Manifest must have at least one resource or catalog",
                    rule = "business",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        // ID should not contain spaces or special characters
        if (manifest.id.contains(Regex("[^a-zA-Z0-9._-]"))) {
            errors.add(
                ValidationError(
                    field = "id",
                    message = "Manifest ID should only contain alphanumeric characters, dots, hyphens, and underscores",
                    value = manifest.id,
                    rule = "format",
                    severity = ValidationSeverity.WARNING
                )
            )
        }
        
        // Check for reasonable limits
        if (manifest.name.length > 100) {
            errors.add(
                ValidationError(
                    field = "name",
                    message = "Manifest name is too long (maximum 100 characters)",
                    value = manifest.name.length,
                    rule = "length",
                    severity = ValidationSeverity.WARNING
                )
            )
        }
        
        if (manifest.description != null && manifest.description.length > 500) {
            errors.add(
                ValidationError(
                    field = "description",
                    message = "Description is too long (maximum 500 characters)",
                    value = manifest.description.length,
                    rule = "length",
                    severity = ValidationSeverity.WARNING
                )
            )
        }
    }
    
    private fun validateResources(manifest: StremioManifest, errors: MutableList<ValidationError>) {
        manifest.resources.forEachIndexed { index, resource ->
            if (resource.name.isBlank()) {
                errors.add(
                    ValidationError(
                        field = "resources[$index].name",
                        message = "Resource name cannot be empty",
                        rule = "required",
                        severity = ValidationSeverity.ERROR
                    )
                )
            }
            
            if (resource.types.isEmpty()) {
                errors.add(
                    ValidationError(
                        field = "resources[$index].types",
                        message = "Resource must have at least one type",
                        rule = "required",
                        severity = ValidationSeverity.ERROR
                    )
                )
            }
            
            // Validate known resource names
            val knownResources = setOf("catalog", "meta", "stream", "subtitles", "addon_catalog")
            if (!knownResources.contains(resource.name.lowercase())) {
                errors.add(
                    ValidationError(
                        field = "resources[$index].name",
                        message = "Unknown resource type: ${resource.name}. Known types: ${knownResources.joinToString()}",
                        value = resource.name,
                        rule = "validation",
                        severity = ValidationSeverity.INFO
                    )
                )
            }
        }
    }
    
    private fun validateCatalogs(manifest: StremioManifest, errors: MutableList<ValidationError>) {
        manifest.catalogs.forEachIndexed { index, catalog ->
            if (catalog.type.isBlank()) {
                errors.add(
                    ValidationError(
                        field = "catalogs[$index].type",
                        message = "Catalog type cannot be empty",
                        rule = "required",
                        severity = ValidationSeverity.ERROR
                    )
                )
            }
            
            if (catalog.id.isBlank()) {
                errors.add(
                    ValidationError(
                        field = "catalogs[$index].id",
                        message = "Catalog ID cannot be empty",
                        rule = "required",
                        severity = ValidationSeverity.ERROR
                    )
                )
            }
            
            if (catalog.name.isBlank()) {
                errors.add(
                    ValidationError(
                        field = "catalogs[$index].name",
                        message = "Catalog name cannot be empty",
                        rule = "required",
                        severity = ValidationSeverity.ERROR
                    )
                )
            }
            
            // Validate known content types
            val knownTypes = setOf("movie", "series", "channel", "tv")
            if (!knownTypes.contains(catalog.type.lowercase())) {
                errors.add(
                    ValidationError(
                        field = "catalogs[$index].type",
                        message = "Unknown catalog type: ${catalog.type}. Known types: ${knownTypes.joinToString()}",
                        value = catalog.type,
                        rule = "validation",
                        severity = ValidationSeverity.INFO
                    )
                )
            }
        }
    }
    
    private fun validateSourceUrl(sourceUrl: String, errors: MutableList<ValidationError>) {
        if (!urlRegex.matches(sourceUrl)) {
            errors.add(
                ValidationError(
                    field = "sourceUrl",
                    message = "Source URL format is invalid",
                    value = sourceUrl,
                    rule = "format",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        if (!sourceUrl.startsWith("https://")) {
            errors.add(
                ValidationError(
                    field = "sourceUrl",
                    message = "Source URL should use HTTPS for security",
                    value = sourceUrl,
                    rule = "security",
                    severity = ValidationSeverity.WARNING
                )
            )
        }
    }
    
    private fun validateScraperManifestFields(manifest: ScraperManifest, errors: MutableList<ValidationError>) {
        // Validate base URL
        if (!urlRegex.matches(manifest.baseUrl)) {
            errors.add(
                ValidationError(
                    field = "baseUrl",
                    message = "Base URL format is invalid",
                    value = manifest.baseUrl,
                    rule = "format",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        // Validate source URL
        if (!urlRegex.matches(manifest.sourceUrl)) {
            errors.add(
                ValidationError(
                    field = "sourceUrl",
                    message = "Source URL format is invalid",
                    value = manifest.sourceUrl,
                    rule = "format",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        // Validate configuration
        if (manifest.configuration.rateLimitMs < 0) {
            errors.add(
                ValidationError(
                    field = "configuration.rateLimitMs",
                    message = "Rate limit cannot be negative",
                    value = manifest.configuration.rateLimitMs,
                    rule = "range",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        if (manifest.configuration.timeoutSeconds <= 0) {
            errors.add(
                ValidationError(
                    field = "configuration.timeoutSeconds",
                    message = "Timeout must be positive",
                    value = manifest.configuration.timeoutSeconds,
                    rule = "range",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        if (manifest.configuration.retryAttempts < 0) {
            errors.add(
                ValidationError(
                    field = "configuration.retryAttempts",
                    message = "Retry attempts cannot be negative",
                    value = manifest.configuration.retryAttempts,
                    rule = "range",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
        
        if (manifest.priorityOrder < 0) {
            errors.add(
                ValidationError(
                    field = "priorityOrder",
                    message = "Priority order cannot be negative",
                    value = manifest.priorityOrder,
                    rule = "range",
                    severity = ValidationSeverity.ERROR
                )
            )
        }
    }
}