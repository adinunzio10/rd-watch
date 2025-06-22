package com.rdwatch.androidtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun TVHomeScreen() {
    var isDrawerOpen by remember { mutableStateOf(false) }
    val drawerFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    // Handle keyboard/D-pad input
    val keyHandler = remember {
        { keyEvent: KeyEvent ->
            when {
                keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
                    if (!isDrawerOpen) {
                        isDrawerOpen = true
                        true
                    } else false
                }
                keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyDown -> {
                    if (isDrawerOpen) {
                        isDrawerOpen = false
                        true
                    } else false
                }
                else -> false
            }
        }
    }
    
    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) {
            drawerFocusRequester.requestFocus()
        } else {
            contentFocusRequester.requestFocus()
        }
    }
    
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent(keyHandler)
    ) {
        // Main content with overscan safety
        SafeAreaContent(
            modifier = Modifier.fillMaxSize(),
            contentFocusRequester = contentFocusRequester,
            onDrawerToggle = { isDrawerOpen = !isDrawerOpen }
        )
        
        // Navigation drawer
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            ),
            modifier = Modifier.zIndex(1f)
        ) {
            TVNavigationDrawer(
                focusRequester = drawerFocusRequester,
                onItemSelected = { isDrawerOpen = false },
                onBackPressed = { isDrawerOpen = false }
            )
        }
        
        // Backdrop overlay
        if (isDrawerOpen) {
            val alpha by animateFloatAsState(
                targetValue = 0.5f,
                animationSpec = tween(300),
                label = "backdrop_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
                    .zIndex(0.5f)
            )
        }
    }
}

@Composable
fun TVNavigationDrawer(
    focusRequester: FocusRequester,
    onItemSelected: () -> Unit,
    onBackPressed: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val drawerWidth = (configuration.screenWidthDp * 0.25f).dp.coerceAtLeast(200.dp)
    
    val navigationItems = listOf(
        NavigationItem("Home", Icons.Default.Home),
        NavigationItem("Search", Icons.Default.Search),
        NavigationItem("Library", Icons.Default.VideoLibrary),
        NavigationItem("Downloads", Icons.Default.Download),
        NavigationItem("Settings", Icons.Default.Settings)
    )
    
    Surface(
        modifier = Modifier
            .width(drawerWidth)
            .fillMaxHeight()
            .padding(start = 32.dp, top = 32.dp, bottom = 32.dp), // TV overscan safety
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drawer header
            Text(
                text = "RD Watch",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Navigation items
            navigationItems.forEachIndexed { index, item ->
                NavigationDrawerItem(
                    item = item,
                    modifier = if (index == 0) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                    onClick = {
                        onItemSelected()
                        // TODO: Handle navigation
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerItem(
    item: NavigationItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun SafeAreaContent(
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester,
    onDrawerToggle: () -> Unit
) {
    val overscanMargin = 32.dp // 5% for most TVs
    
    Column(
        modifier = modifier
            .padding(overscanMargin)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App title with menu button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDrawerToggle,
                modifier = Modifier.focusRequester(contentFocusRequester)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open navigation menu",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Text(
                text = "RD Watch",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }
        
        // Content rows placeholder
        TVContentGrid()
    }
}

@Composable
fun TVContentGrid() {
    val categories = listOf("Featured", "Movies", "TV Shows", "Sports", "News")
    val sampleContent = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        items(categories) { category ->
            TVContentSection(
                title = category,
                items = sampleContent
            )
        }
    }
}

@Composable
fun TVContentSection(
    title: String,
    items: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                TVContentCard(title = item)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVContentCard(
    title: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .size(200.dp, 120.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector
)