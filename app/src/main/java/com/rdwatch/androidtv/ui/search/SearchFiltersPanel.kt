package com.rdwatch.androidtv.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * TV-optimized search filters panel with D-pad navigation
 */
@Composable
fun SearchFiltersPanel(
    filters: SearchFilters,
    onFiltersChanged: (SearchFilters) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val panelWidth = (configuration.screenWidthDp * 0.4f).dp.coerceAtLeast(320.dp)
    val firstItemFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }
    
    Surface(
        modifier = modifier
            .width(panelWidth)
            .fillMaxHeight()
            .padding(end = 32.dp, top = 32.dp, bottom = 32.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            FiltersPanelHeader(
                onClose = onClose,
                onClearAll = { onFiltersChanged(SearchFilters()) },
                hasActiveFilters = filters.hasActiveFilters(),
                closeButtonFocusRequester = firstItemFocusRequester
            )
            
            // Filters content
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Content Type Filter
                item {
                    ContentTypeFilter(
                        selectedTypes = filters.contentTypes,
                        onTypesChanged = { types ->
                            onFiltersChanged(filters.copy(contentTypes = types))
                        }
                    )
                }
                
                // Year Range Filter
                item {
                    YearRangeFilter(
                        minYear = filters.minYear,
                        maxYear = filters.maxYear,
                        onYearRangeChanged = { min, max ->
                            onFiltersChanged(filters.copy(minYear = min, maxYear = max))
                        }
                    )
                }
                
                // Rating Filter
                item {
                    RatingFilter(
                        minRating = filters.minRating,
                        onRatingChanged = { rating ->
                            onFiltersChanged(filters.copy(minRating = rating))
                        }
                    )
                }
                
                // Genre Filter
                item {
                    GenreFilter(
                        selectedGenres = filters.genres,
                        onGenresChanged = { genres ->
                            onFiltersChanged(filters.copy(genres = genres))
                        }
                    )
                }
                
                // Quality Filter
                item {
                    QualityFilter(
                        selectedQualities = filters.qualityPreferences,
                        onQualitiesChanged = { qualities ->
                            onFiltersChanged(filters.copy(qualityPreferences = qualities))
                        }
                    )
                }
                
                // Language Filter
                item {
                    LanguageFilter(
                        selectedLanguages = filters.languages,
                        onLanguagesChanged = { languages ->
                            onFiltersChanged(filters.copy(languages = languages))
                        }
                    )
                }
                
                // Adult Content Filter
                item {
                    AdultContentFilter(
                        excludeAdult = filters.excludeAdult,
                        onExcludeAdultChanged = { exclude ->
                            onFiltersChanged(filters.copy(excludeAdult = exclude))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersPanelHeader(
    onClose: () -> Unit,
    onClearAll: () -> Unit,
    hasActiveFilters: Boolean,
    closeButtonFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Search Filters",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasActiveFilters) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
            
            IconButton(
                onClick = onClose,
                modifier = Modifier.focusRequester(closeButtonFocusRequester)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close filters",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ContentTypeFilter(
    selectedTypes: List<String>,
    onTypesChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentTypes = listOf(
        FilterOption("movie", "Movies", Icons.Default.Movie),
        FilterOption("tv", "TV Shows", Icons.Default.Tv),
        FilterOption("anime", "Anime", Icons.Default.Animation),
        FilterOption("documentary", "Documentaries", Icons.Default.Description)
    )
    
    FilterSection(
        title = "Content Type",
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contentTypes) { contentType ->
                FilterChip(
                    option = contentType,
                    isSelected = contentType.value in selectedTypes,
                    onSelectionChanged = { isSelected ->
                        val newTypes = if (isSelected) {
                            selectedTypes + contentType.value
                        } else {
                            selectedTypes - contentType.value
                        }
                        onTypesChanged(newTypes)
                    }
                )
            }
        }
    }
}

@Composable
private fun YearRangeFilter(
    minYear: Int?,
    maxYear: Int?,
    onYearRangeChanged: (Int?, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val currentYear = 2024
    
    FilterSection(
        title = "Release Year",
        isExpandable = true,
        isExpanded = isExpanded,
        onExpandedChanged = { isExpanded = it },
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preset year ranges
                val yearRanges = listOf(
                    "2020s" to (2020 to currentYear),
                    "2010s" to (2010 to 2019),
                    "2000s" to (2000 to 2009),
                    "90s" to (1990 to 1999),
                    "Older" to (1900 to 1989)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(yearRanges) { (label, range) ->
                        val isSelected = minYear == range.first && maxYear == range.second
                        
                        FilterButton(
                            text = label,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    onYearRangeChanged(null, null)
                                } else {
                                    onYearRangeChanged(range.first, range.second)
                                }
                            }
                        )
                    }
                }
                
                // Custom range display
                if (minYear != null || maxYear != null) {
                    Text(
                        text = "Custom: ${minYear ?: "Any"} - ${maxYear ?: "Any"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingFilter(
    minRating: Float?,
    onRatingChanged: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratingOptions = listOf(
        7.0f to "7.0+ (Excellent)",
        6.0f to "6.0+ (Good)",
        5.0f to "5.0+ (Average)",
        4.0f to "4.0+ (Poor)"
    )
    
    FilterSection(
        title = "Minimum Rating",
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ratingOptions) { (rating, label) ->
                FilterButton(
                    text = label,
                    isSelected = minRating == rating,
                    onClick = {
                        val newRating = if (minRating == rating) null else rating
                        onRatingChanged(newRating)
                    }
                )
            }
        }
    }
}

@Composable
private fun GenreFilter(
    selectedGenres: List<String>,
    onGenresChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val genres = listOf(
        "Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary",
        "Drama", "Family", "Fantasy", "Horror", "Mystery", "Romance",
        "Sci-Fi", "Thriller", "War", "Western"
    )
    
    FilterSection(
        title = "Genres",
        isExpandable = true,
        isExpanded = isExpanded,
        onExpandedChanged = { isExpanded = it },
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                genres.chunked(4).forEach { genreRow ->
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(genreRow) { genre ->
                            FilterButton(
                                text = genre,
                                isSelected = genre in selectedGenres,
                                onClick = {
                                    val newGenres = if (genre in selectedGenres) {
                                        selectedGenres - genre
                                    } else {
                                        selectedGenres + genre
                                    }
                                    onGenresChanged(newGenres)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityFilter(
    selectedQualities: List<String>,
    onQualitiesChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val qualities = listOf("4K", "1080p", "720p", "480p")
    
    FilterSection(
        title = "Video Quality",
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(qualities) { quality ->
                FilterButton(
                    text = quality,
                    isSelected = quality in selectedQualities,
                    onClick = {
                        val newQualities = if (quality in selectedQualities) {
                            selectedQualities - quality
                        } else {
                            selectedQualities + quality
                        }
                        onQualitiesChanged(newQualities)
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageFilter(
    selectedLanguages: List<String>,
    onLanguagesChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf(
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese"
    )
    
    FilterSection(
        title = "Language",
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages) { (code, name) ->
                FilterButton(
                    text = name,
                    isSelected = code in selectedLanguages,
                    onClick = {
                        val newLanguages = if (code in selectedLanguages) {
                            selectedLanguages - code
                        } else {
                            selectedLanguages + code
                        }
                        onLanguagesChanged(newLanguages)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdultContentFilter(
    excludeAdult: Boolean,
    onExcludeAdultChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterSection(
        title = "Content Restrictions",
        modifier = modifier
    ) {
        Card(
            onClick = { onExcludeAdultChanged(!excludeAdult) },
            colors = CardDefaults.cardColors(
                containerColor = if (excludeAdult) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exclude Adult Content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (excludeAdult) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Switch(
                    checked = excludeAdult,
                    onCheckedChange = onExcludeAdultChanged
                )
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    onExpandedChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            
            if (isExpandable) {
                IconButton(onClick = { onExpandedChanged(!isExpanded) }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChip(
    option: FilterOption,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    FilterChip(
        selected = isSelected,
        onClick = { onSelectionChanged(!isSelected) },
        label = { Text(option.label) },
        leadingIcon = option.icon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = if (isFocused) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                borderColor = MaterialTheme.colorScheme.outline,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                borderWidth = 2.dp
            )
        } else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isFocused -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isFocused) {
            CardDefaults.outlinedCardBorder(enabled = true).copy(width = 2.dp)
        } else null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Filter option data class
 */
private data class FilterOption(
    val value: String,
    val label: String,
    val icon: ImageVector? = null
)

/**
 * Extension function to check if filters have active selections
 */
fun SearchFilters.hasActiveFilters(): Boolean {
    return contentTypes.isNotEmpty() ||
           minYear != null ||
           maxYear != null ||
           minRating != null ||
           genres.isNotEmpty() ||
           languages.isNotEmpty() ||
           qualityPreferences.isNotEmpty() ||
           !excludeAdult
}