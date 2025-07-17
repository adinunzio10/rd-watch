package com.rdwatch.androidtv.scraper.validation

import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ManifestValidationException
import com.rdwatch.androidtv.scraper.models.ValidationError
import com.rdwatch.androidtv.scraper.models.ValidationSeverity
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON Schema validation for manifests
 */
@Singleton
class SchemaValidator
    @Inject
    constructor() {
        private val moshi: Moshi =
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

        /**
         * Validate JSON against Stremio manifest schema
         */
        fun validateJsonSchema(jsonContent: String): ManifestResult<Boolean> {
            val errors = mutableListOf<ValidationError>()

            try {
                // Parse JSON to check basic structure
                val jsonAdapter: JsonAdapter<Map<String, Any>> =
                    moshi.adapter(Map::class.java) as JsonAdapter<Map<String, Any>>

                val manifestMap = jsonAdapter.fromJson(jsonContent)

                if (manifestMap == null) {
                    errors.add(
                        ValidationError(
                            field = "root",
                            message = "Invalid JSON structure",
                            rule = "schema",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                    return ManifestResult.Error(
                        ManifestValidationException(
                            "JSON schema validation failed",
                            validationErrors = errors,
                        ),
                    )
                }

                // Validate required root fields
                validateRequiredRootFields(manifestMap, errors)

                // Validate field types
                validateFieldTypes(manifestMap, errors)

                // Validate array structures
                validateArrayStructures(manifestMap, errors)
            } catch (e: Exception) {
                errors.add(
                    ValidationError(
                        field = "json",
                        message = "JSON parsing failed: ${e.message}",
                        rule = "syntax",
                        severity = ValidationSeverity.ERROR,
                    ),
                )
            }

            return if (errors.isEmpty()) {
                ManifestResult.Success(true)
            } else {
                ManifestResult.Error(
                    ManifestValidationException(
                        "JSON schema validation failed with ${errors.size} errors",
                        validationErrors = errors,
                    ),
                )
            }
        }

        /**
         * Validate that a JSON object conforms to the expected manifest structure
         */
        fun validateStremioSchema(manifestData: Map<String, Any>): List<ValidationError> {
            val errors = mutableListOf<ValidationError>()

            validateRequiredRootFields(manifestData, errors)
            validateFieldTypes(manifestData, errors)
            validateArrayStructures(manifestData, errors)

            return errors
        }

        private fun validateRequiredRootFields(
            data: Map<String, Any>,
            errors: MutableList<ValidationError>,
        ) {
            val requiredFields = listOf("id", "name", "version")

            requiredFields.forEach { field ->
                if (!data.containsKey(field)) {
                    errors.add(
                        ValidationError(
                            field = field,
                            message = "Required field '$field' is missing",
                            rule = "required",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                } else {
                    val value = data[field]
                    if (value == null || (value is String && value.isBlank())) {
                        errors.add(
                            ValidationError(
                                field = field,
                                message = "Required field '$field' cannot be null or empty",
                                value = value,
                                rule = "required",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }
                }
            }
        }

        private fun validateFieldTypes(
            data: Map<String, Any>,
            errors: MutableList<ValidationError>,
        ) {
            // String fields
            val stringFields = listOf("id", "name", "version", "description", "logo", "background", "contactEmail")
            stringFields.forEach { field ->
                data[field]?.let { value ->
                    if (value !is String) {
                        errors.add(
                            ValidationError(
                                field = field,
                                message = "Field '$field' must be a string",
                                value = value,
                                rule = "type",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }
                }
            }

            // Array fields
            val arrayFields = listOf("catalogs", "resources", "types", "idPrefixes")
            arrayFields.forEach { field ->
                data[field]?.let { value ->
                    if (value !is List<*>) {
                        errors.add(
                            ValidationError(
                                field = field,
                                message = "Field '$field' must be an array",
                                value = value,
                                rule = "type",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }
                }
            }

            // Object fields
            data["behaviorHints"]?.let { value ->
                if (value !is Map<*, *>) {
                    errors.add(
                        ValidationError(
                            field = "behaviorHints",
                            message = "Field 'behaviorHints' must be an object",
                            value = value,
                            rule = "type",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                }
            }
        }

        private fun validateArrayStructures(
            data: Map<String, Any>,
            errors: MutableList<ValidationError>,
        ) {
            // Validate catalogs array
            (data["catalogs"] as? List<*>)?.let { catalogs ->
                catalogs.forEachIndexed { index, catalog ->
                    if (catalog is Map<*, *>) {
                        validateCatalogStructure(catalog, "catalogs[$index]", errors)
                    } else {
                        errors.add(
                            ValidationError(
                                field = "catalogs[$index]",
                                message = "Catalog items must be objects",
                                value = catalog,
                                rule = "type",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }
                }
            }

            // Validate resources array
            (data["resources"] as? List<*>)?.let { resources ->
                resources.forEachIndexed { index, resource ->
                    if (resource is Map<*, *>) {
                        validateResourceStructure(resource, "resources[$index]", errors)
                    } else {
                        errors.add(
                            ValidationError(
                                field = "resources[$index]",
                                message = "Resource items must be objects",
                                value = resource,
                                rule = "type",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }
                }
            }

            // Validate types array contains only strings
            (data["types"] as? List<*>)?.let { types ->
                types.forEachIndexed { index, type ->
                    if (type !is String) {
                        errors.add(
                            ValidationError(
                                field = "types[$index]",
                                message = "Type items must be strings",
                                value = type,
                                rule = "type",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }
                }
            }
        }

        private fun validateCatalogStructure(
            catalog: Map<*, *>,
            fieldPath: String,
            errors: MutableList<ValidationError>,
        ) {
            val requiredFields = listOf("type", "id", "name")

            requiredFields.forEach { field ->
                if (!catalog.containsKey(field)) {
                    errors.add(
                        ValidationError(
                            field = "$fieldPath.$field",
                            message = "Catalog field '$field' is required",
                            rule = "required",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                } else {
                    val value = catalog[field]
                    if (value !is String || value.isBlank()) {
                        errors.add(
                            ValidationError(
                                field = "$fieldPath.$field",
                                message = "Catalog field '$field' must be a non-empty string",
                                value = value,
                                rule = "type",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }
                }
            }

            // Optional fields type validation
            catalog["genres"]?.let { genres ->
                if (genres !is List<*>) {
                    errors.add(
                        ValidationError(
                            field = "$fieldPath.genres",
                            message = "Genres must be an array",
                            value = genres,
                            rule = "type",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                } else {
                    genres.forEachIndexed { index, genre ->
                        if (genre !is String) {
                            errors.add(
                                ValidationError(
                                    field = "$fieldPath.genres[$index]",
                                    message = "Genre items must be strings",
                                    value = genre,
                                    rule = "type",
                                    severity = ValidationSeverity.ERROR,
                                ),
                            )
                        }
                    }
                }
            }

            catalog["extra"]?.let { extra ->
                if (extra !is List<*>) {
                    errors.add(
                        ValidationError(
                            field = "$fieldPath.extra",
                            message = "Extra must be an array",
                            value = extra,
                            rule = "type",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                }
            }
        }

        private fun validateResourceStructure(
            resource: Map<*, *>,
            fieldPath: String,
            errors: MutableList<ValidationError>,
        ) {
            val requiredFields = listOf("name", "types")

            requiredFields.forEach { field ->
                if (!resource.containsKey(field)) {
                    errors.add(
                        ValidationError(
                            field = "$fieldPath.$field",
                            message = "Resource field '$field' is required",
                            rule = "required",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                }
            }

            // Validate name is string
            resource["name"]?.let { name ->
                if (name !is String || name.isBlank()) {
                    errors.add(
                        ValidationError(
                            field = "$fieldPath.name",
                            message = "Resource name must be a non-empty string",
                            value = name,
                            rule = "type",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                }
            }

            // Validate types is array of strings
            resource["types"]?.let { types ->
                if (types !is List<*>) {
                    errors.add(
                        ValidationError(
                            field = "$fieldPath.types",
                            message = "Resource types must be an array",
                            value = types,
                            rule = "type",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                } else {
                    if (types.isEmpty()) {
                        errors.add(
                            ValidationError(
                                field = "$fieldPath.types",
                                message = "Resource types array cannot be empty",
                                rule = "required",
                                severity = ValidationSeverity.ERROR,
                            ),
                        )
                    }

                    types.forEachIndexed { index, type ->
                        if (type !is String) {
                            errors.add(
                                ValidationError(
                                    field = "$fieldPath.types[$index]",
                                    message = "Resource type items must be strings",
                                    value = type,
                                    rule = "type",
                                    severity = ValidationSeverity.ERROR,
                                ),
                            )
                        }
                    }
                }
            }

            // Validate optional idPrefixes
            resource["idPrefixes"]?.let { idPrefixes ->
                if (idPrefixes !is List<*>) {
                    errors.add(
                        ValidationError(
                            field = "$fieldPath.idPrefixes",
                            message = "ID prefixes must be an array",
                            value = idPrefixes,
                            rule = "type",
                            severity = ValidationSeverity.ERROR,
                        ),
                    )
                } else {
                    idPrefixes.forEachIndexed { index, prefix ->
                        if (prefix !is String) {
                            errors.add(
                                ValidationError(
                                    field = "$fieldPath.idPrefixes[$index]",
                                    message = "ID prefix items must be strings",
                                    value = prefix,
                                    rule = "type",
                                    severity = ValidationSeverity.ERROR,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
