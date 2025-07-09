package com.rdwatch.androidtv.ui.details.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.details.components.HeroSection

/**
 * Base layout for content detail screens
 * Provides a flexible structure that can be used for different content types
 */
@Composable
fun BaseDetailLayout(
    uiState: DetailUiState,
    onActionClick: (ContentAction) -> Unit,
    onRelatedContentClick: (ContentDetail) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester = remember { FocusRequester() },
    customSections: @Composable (DetailSection) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val overscanMargin = uiState.getLayoutConfig().overscanMargin.dp
    
    when {
        uiState.isLoading && !uiState.hasContent -> {
            LoadingState(
                modifier = modifier,
                firstFocusRequester = firstFocusRequester
            )
        }
        uiState.hasError && !uiState.hasContent -> {
            ErrorState(
                error = uiState.error ?: "Unknown error",
                onBackPressed = onBackPressed,
                modifier = modifier,
                firstFocusRequester = firstFocusRequester
            )
        }
        uiState.hasContent -> {
            ContentDetailLayout(
                content = uiState.content!!,
                uiState = uiState,
                onActionClick = onActionClick,
                onRelatedContentClick = onRelatedContentClick,
                onBackPressed = onBackPressed,
                modifier = modifier,
                firstFocusRequester = firstFocusRequester,
                customSections = customSections,
                listState = listState,
                overscanMargin = overscanMargin
            )
        }
        else -> {
            ContentNotFoundState(
                onBackPressed = onBackPressed,
                modifier = modifier,
                firstFocusRequester = firstFocusRequester
            )
        }
    }
}

@Composable
private fun ContentDetailLayout(
    content: ContentDetail,
    uiState: DetailUiState,
    onActionClick: (ContentAction) -> Unit,
    onRelatedContentClick: (ContentDetail) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier,
    firstFocusRequester: FocusRequester,
    customSections: @Composable (DetailSection) -> Unit,
    listState: LazyListState,
    overscanMargin: androidx.compose.ui.unit.Dp
) {
    val sections = uiState.getVisibleSections()
    
    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        sections.forEach { section ->
            when (section) {
                DetailSection.HERO -> {
                    if (uiState.shouldShowSection(section)) {
                        item(key = "hero_${content.id}") {
                            HeroSection(
                                content = content,
                                progress = uiState.progress ?: ContentProgress(),
                                onActionClick = onActionClick,
                                onBackPressed = onBackPressed,
                                firstFocusRequester = firstFocusRequester,
                                overscanMargin = overscanMargin
                            )
                        }
                    }
                }
                DetailSection.INFO -> {
                    if (uiState.shouldShowSection(section)) {
                        item(key = "info_${content.id}") {
                            InfoSectionPlaceholder(
                                content = content,
                                modifier = Modifier.padding(horizontal = overscanMargin)
                            )
                        }
                    }
                }
                DetailSection.ACTIONS -> {
                    if (uiState.shouldShowSection(section)) {
                        item(key = "actions_${content.id}") {
                            ActionsSectionPlaceholder(
                                content = content,
                                onActionClick = onActionClick,
                                modifier = Modifier.padding(horizontal = overscanMargin)
                            )
                        }
                    }
                }
                DetailSection.RELATED -> {
                    if (uiState.shouldShowSection(section)) {
                        item(key = "related_${content.id}") {
                            RelatedContentSectionPlaceholder(
                                relatedContent = uiState.relatedContent,
                                onContentClick = onRelatedContentClick,
                                modifier = Modifier.padding(horizontal = overscanMargin)
                            )
                        }
                    }
                }
                DetailSection.SEASON_EPISODE_GRID -> {
                    if (uiState.shouldShowSection(section)) {
                        item(key = "episodes_${content.id}") {
                            SeasonEpisodeGridPlaceholder(
                                content = content,
                                modifier = Modifier.padding(horizontal = overscanMargin)
                            )
                        }
                    }
                }
                DetailSection.CAST_CREW -> {
                    if (uiState.shouldShowSection(section)) {
                        item(key = "cast_${content.id}") {
                            CastCrewSectionPlaceholder(
                                content = content,
                                modifier = Modifier.padding(horizontal = overscanMargin)
                            )
                        }
                    }
                }
                DetailSection.CUSTOM -> {
                    if (uiState.shouldShowSection(section)) {
                        item(key = "custom_${content.id}") {
                            customSections(section)
                        }
                    }
                }
            }
        }
        
        // Bottom spacing for TV overscan
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(overscanMargin))
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier,
    firstFocusRequester: FocusRequester
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading content details...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onBackPressed: () -> Unit,
    modifier: Modifier,
    firstFocusRequester: FocusRequester
) {
    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Failed to load content",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = onBackPressed,
                modifier = Modifier.focusRequester(firstFocusRequester)
            ) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun ContentNotFoundState(
    onBackPressed: () -> Unit,
    modifier: Modifier,
    firstFocusRequester: FocusRequester
) {
    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Content not found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = onBackPressed,
                modifier = Modifier.focusRequester(firstFocusRequester)
            ) {
                Text("Go Back")
            }
        }
    }
}

// Placeholder components that will be replaced by actual implementations

@Composable
private fun InfoSectionPlaceholder(
    content: ContentDetail,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Info Section",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = content.getDisplayDescription(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ActionsSectionPlaceholder(
    content: ContentDetail,
    onActionClick: (ContentAction) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Actions: ${content.actions.size} available",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun RelatedContentSectionPlaceholder(
    relatedContent: List<ContentDetail>,
    onContentClick: (ContentDetail) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Related Content: ${relatedContent.size} items",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SeasonEpisodeGridPlaceholder(
    content: ContentDetail,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Season/Episode Grid",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun CastCrewSectionPlaceholder(
    content: ContentDetail,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Cast & Crew",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}