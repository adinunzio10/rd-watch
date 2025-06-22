package com.rdwatch.androidtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.theme.RdwatchTheme

/**
 * Main Activity using Jetpack Compose for TV interface
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RdwatchTheme {
                TVHomeScreen()
            }
        }
    }
}

@Composable
fun TVHomeScreen() {
    val focusRequester = remember { FocusRequester() }
    
    // Sample data for TV content categories
    val categories = listOf("Featured", "Movies", "TV Shows", "Sports", "News")
    val featuredContent = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App title
        Text(
            text = "RD Watch",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        
        // Main content area with categories
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            items(categories.take(3)) { category ->
                TVContentSection(
                    title = category,
                    items = featuredContent,
                    isFirst = category == categories.first(),
                    focusRequester = if (category == categories.first()) focusRequester else null
                )
            }
        }
    }
}

@Composable
fun TVContentSection(
    title: String,
    items: List<String>,
    isFirst: Boolean = false,
    focusRequester: FocusRequester? = null
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
            items(items.size) { index ->
                TVContentCard(
                    title = items[index],
                    modifier = if (isFirst && index == 0 && focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
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
    Card(
        modifier = modifier
            .size(200.dp, 120.dp)
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}