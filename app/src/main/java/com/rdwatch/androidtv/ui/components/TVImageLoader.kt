package com.rdwatch.androidtv.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun TVImageLoader(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    showPlaceholder: Boolean = true,
    showShimmer: Boolean = true,
    crossfadeEnabled: Boolean = true,
    errorIcon: ImageVector = Icons.Default.ErrorOutline,
    placeholderIcon: ImageVector = Icons.Default.ImageNotSupported,
) {
    val painter =
        rememberAsyncImagePainter(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .size(Size.ORIGINAL)
                    .crossfade(if (crossfadeEnabled) 300 else 0)
                    .build(),
        )

    val state = painter.state

    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is AsyncImagePainter.State.Loading -> {
                if (showShimmer) {
                    TVImageShimmer(
                        modifier = Modifier.fillMaxSize(),
                        shape = shape,
                    )
                } else if (showPlaceholder) {
                    TVImagePlaceholder(
                        icon = placeholderIcon,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                TVImageError(
                    icon = errorIcon,
                    modifier = Modifier.fillMaxSize(),
                    onClick = {
                        // TODO: Implement retry logic
                    },
                )
            }
            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                if (showPlaceholder) {
                    TVImagePlaceholder(
                        icon = placeholderIcon,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun TVImageShimmer(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer_alpha",
    )

    val shimmerColors =
        listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = alpha),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
            MaterialTheme.colorScheme.surface.copy(alpha = alpha),
        )

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(
                    brush = Brush.horizontalGradient(shimmerColors),
                ),
    )
}

@Composable
fun TVImagePlaceholder(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Image placeholder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVImageError(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Error loading image",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to retry",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
fun TVLazyImageRow(
    images: List<String>,
    contentDescriptions: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
    onImageClick: (Int, String) -> Unit = { _, _ -> },
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(images.size) { index ->
            Card(
                onClick = { onImageClick(index, images[index]) },
                modifier =
                    itemModifier
                        .size(160.dp, 90.dp),
            ) {
                TVImageLoader(
                    imageUrl = images[index],
                    contentDescription = contentDescriptions.getOrNull(index),
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

@Composable
fun TVBackgroundImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    overlayAlpha: Float = 0.5f,
) {
    Box(modifier = modifier) {
        TVImageLoader(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(0.dp),
            showShimmer = false,
        )

        // Dark overlay for text readability
        if (overlayAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = overlayAlpha),
                                    ),
                                startY = 0.3f,
                            ),
                        ),
            )
        }
    }
}

/**
 * Enhanced TV image loader with smart caching and TV-optimized loading
 */
@Composable
fun SmartTVImageLoader(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    priority: ImagePriority = ImagePriority.NORMAL,
    cacheStrategy: CacheStrategy = CacheStrategy.MEMORY_AND_DISK,
) {
    val context = LocalContext.current

    val imageRequest =
        remember(imageUrl, priority, cacheStrategy) {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(300)
                .apply {
                    when (priority) {
                        ImagePriority.HIGH -> {
                            memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        }
                        ImagePriority.NORMAL -> {
                            memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        }
                        ImagePriority.LOW -> {
                            memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                            diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        }
                    }

                    when (cacheStrategy) {
                        CacheStrategy.MEMORY_ONLY -> {
                            diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        }
                        CacheStrategy.DISK_ONLY -> {
                            memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        }
                        CacheStrategy.MEMORY_AND_DISK -> {
                            // Default behavior
                        }
                        CacheStrategy.NO_CACHE -> {
                            memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                            diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        }
                    }
                }
                .build()
        }

    val painter = rememberAsyncImagePainter(model = imageRequest)

    TVImageLoader(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

enum class CacheStrategy {
    MEMORY_ONLY, // Fast access, limited storage
    DISK_ONLY, // Persistent, slower access
    MEMORY_AND_DISK, // Balanced approach
    NO_CACHE, // Always fetch from network
}
