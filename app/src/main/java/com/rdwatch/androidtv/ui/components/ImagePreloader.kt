package com.rdwatch.androidtv.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.launch

enum class ImagePriority {
    HIGH, NORMAL, LOW
}

/**
 * TV-optimized image preloader for smooth scrolling performance
 */
class TVImagePreloader(
    private val imageLoader: ImageLoader,
    private val context: Context
) {
    
    suspend fun preloadImages(
        imageUrls: List<String>,
        priority: ImagePriority = ImagePriority.LOW
    ) {
        imageUrls.forEach { url ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build()
                
                imageLoader.execute(request)
            } catch (e: Exception) {
                // Silently ignore preload failures
            }
        }
    }
}

@Composable
fun rememberTVImagePreloader(): TVImagePreloader {
    val context = LocalContext.current
    
    return remember {
        TVImagePreloader(ImageLoader(context), context)
    }
}

/**
 * Composable that preloads images when they come into view
 */
@Composable
fun PreloadImagesEffect(
    imageUrls: List<String>,
    shouldPreload: Boolean = true,
    priority: ImagePriority = ImagePriority.LOW
) {
    val preloader = rememberTVImagePreloader()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(imageUrls, shouldPreload) {
        if (shouldPreload && imageUrls.isNotEmpty()) {
            scope.launch {
                preloader.preloadImages(imageUrls, priority)
            }
        }
    }
}