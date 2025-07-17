package com.rdwatch.androidtv.ui.settings.scrapers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Scraper Settings Screen for managing scraper manifests
 * Provides UI for adding, removing, enabling/disabling scrapers
 */
@Composable
fun ScraperSettingsScreen(
    viewModel: ScraperSettingsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overscanMargin = 32.dp
    val firstFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(overscanMargin),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header
        ScraperSettingsHeader(
            onBackPressed = onBackPressed,
            onAddScraper = { viewModel.showAddDialog() },
            onRefreshAll = { viewModel.refreshAllScrapers() },
            isRefreshing = uiState.isRefreshing,
            firstFocusRequester = firstFocusRequester,
        )

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                !uiState.hasScrapers -> {
                    EmptyState(
                        onAddScraper = { viewModel.showAddDialog() },
                    )
                }

                else -> {
                    ScrapersList(
                        uiState = uiState,
                        onToggleEnabled = { scraperId, enabled ->
                            viewModel.toggleScraperEnabled(scraperId, enabled)
                        },
                        onRefreshScraper = { scraperId ->
                            viewModel.refreshScraper(scraperId)
                        },
                        onRemoveScraper = { scraperId ->
                            viewModel.removeScraper(scraperId)
                        },
                        listState = listState,
                    )
                }
            }

            // Status messages
            StatusMessages(
                error = uiState.error,
                success = uiState.successMessage,
                onErrorDismiss = { viewModel.clearError() },
            )
        }
    }

    // Add scraper dialog
    if (uiState.showAddDialog) {
        AddScraperDialog(
            urlText = uiState.addUrlText,
            isLoading = uiState.isAddingFromUrl,
            onUrlChanged = { viewModel.updateAddUrlText(it) },
            onAdd = { viewModel.addScraperFromUrl(uiState.addUrlText) },
            onDismiss = { viewModel.hideAddDialog() },
        )
    }
}

@Composable
private fun ScraperSettingsHeader(
    onBackPressed: () -> Unit,
    onAddScraper: () -> Unit,
    onRefreshAll: () -> Unit,
    isRefreshing: Boolean,
    firstFocusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button and title
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var backButtonFocused by remember { mutableStateOf(false) }

            TVFocusIndicator(isFocused = backButtonFocused) {
                IconButton(
                    onClick = onBackPressed,
                    modifier =
                        Modifier
                            .focusRequester(firstFocusRequester)
                            .tvFocusable(onFocusChanged = { backButtonFocused = it.isFocused }),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint =
                            if (backButtonFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                    )
                }
            }

            Text(
                text = "Scraper Settings",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Refresh all button
            var refreshFocused by remember { mutableStateOf(false) }
            TVFocusIndicator(isFocused = refreshFocused) {
                Button(
                    onClick = onRefreshAll,
                    modifier =
                        Modifier.tvFocusable(
                            onFocusChanged = { refreshFocused = it.isFocused },
                        ),
                    enabled = !isRefreshing,
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh All")
                }
            }

            // Add scraper button
            var addFocused by remember { mutableStateOf(false) }
            TVFocusIndicator(isFocused = addFocused) {
                Button(
                    onClick = onAddScraper,
                    modifier =
                        Modifier.tvFocusable(
                            onFocusChanged = { addFocused = it.isFocused },
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Scraper")
                }
            }
        }
    }
}

@Composable
private fun ScrapersList(
    uiState: ScraperSettingsUiState,
    onToggleEnabled: (String, Boolean) -> Unit,
    onRefreshScraper: (String) -> Unit,
    onRemoveScraper: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Enabled scrapers section
        if (uiState.enabledScrapers.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Enabled Scrapers (${uiState.enabledScrapers.size})",
                    icon = Icons.Default.CheckCircle,
                )
            }

            items(
                items = uiState.enabledScrapers,
                key = { it.id },
            ) { scraper ->
                ScraperListItem(
                    scraper = scraper,
                    onToggleEnabled = { enabled ->
                        onToggleEnabled(scraper.id, enabled)
                    },
                    onRefresh = { onRefreshScraper(scraper.id) },
                    onRemove = { onRemoveScraper(scraper.id) },
                )
            }
        }

        // Disabled scrapers section
        if (uiState.disabledScrapers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Disabled Scrapers (${uiState.disabledScrapers.size})",
                    icon = Icons.Default.Pause,
                )
            }

            items(
                items = uiState.disabledScrapers,
                key = { it.id },
            ) { scraper ->
                ScraperListItem(
                    scraper = scraper,
                    onToggleEnabled = { enabled ->
                        onToggleEnabled(scraper.id, enabled)
                    },
                    onRefresh = { onRefreshScraper(scraper.id) },
                    onRemove = { onRemoveScraper(scraper.id) },
                )
            }
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Loading scrapers...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun EmptyState(onAddScraper: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Text(
                text = "No Scrapers Installed",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text =
                    "Add scrapers to start finding content from various sources. " +
                        "Scrapers help you discover movies and shows from different providers.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            var addButtonFocused by remember { mutableStateOf(false) }
            TVFocusIndicator(isFocused = addButtonFocused) {
                Button(
                    onClick = onAddScraper,
                    modifier =
                        Modifier.tvFocusable(
                            onFocusChanged = { addButtonFocused = it.isFocused },
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Your First Scraper")
                }
            }
        }
    }
}

@Composable
private fun StatusMessages(
    error: String?,
    success: String?,
    onErrorDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        error?.let { errorMessage ->
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }

                    IconButton(onClick = onErrorDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        success?.let { successMessage ->
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = successMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
