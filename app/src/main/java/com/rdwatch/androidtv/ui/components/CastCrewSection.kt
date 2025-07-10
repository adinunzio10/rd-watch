package com.rdwatch.androidtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rdwatch.androidtv.ui.details.models.CastMember
import com.rdwatch.androidtv.ui.details.models.CrewMember
import com.rdwatch.androidtv.ui.details.models.ExtendedContentMetadata

/**
 * Horizontally scrollable cast and crew section for TV detail screens
 */
@Composable
fun CastCrewSection(
    metadata: ExtendedContentMetadata,
    modifier: Modifier = Modifier,
    onCastMemberClick: (CastMember) -> Unit = {},
    onCrewMemberClick: (CrewMember) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Cast Section
        if (metadata.fullCast.isNotEmpty()) {
            Text(
                text = "Cast",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = metadata.fullCast,
                    key = { "cast_${it.id}" }
                ) { castMember ->
                    CastMemberCard(
                        castMember = castMember,
                        onClick = { onCastMemberClick(castMember) }
                    )
                }
            }
        }
        
        // Crew Section
        val keyCrew = metadata.getKeyCrew()
        if (keyCrew.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Crew",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = keyCrew,
                    key = { "crew_${it.id}" }
                ) { crewMember ->
                    CrewMemberCard(
                        crewMember = crewMember,
                        onClick = { onCrewMemberClick(crewMember) }
                    )
                }
            }
        }
    }
}

/**
 * Individual cast member card with profile image and character name
 */
@Composable
fun CastMemberCard(
    castMember: CastMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(140.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(
                            width = if (isFocused) 3.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        CircleShape
                    )
            ) {
                if (castMember.profileImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(castMember.profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = castMember.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback person icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = castMember.name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Actor Name
            Text(
                text = castMember.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Character Name
            if (castMember.character.isNotBlank()) {
                Text(
                    text = castMember.character,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Individual crew member card with profile image and job title
 */
@Composable
fun CrewMemberCard(
    crewMember: CrewMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(140.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(
                            width = if (isFocused) 3.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        CircleShape
                    )
            ) {
                if (crewMember.profileImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(crewMember.profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = crewMember.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback person icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = crewMember.name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Crew Member Name
            Text(
                text = crewMember.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Job Title
            Text(
                text = crewMember.job,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Preview for Cast/Crew Section
 */
@Preview(device = "id:tv_1080p")
@Composable
fun CastCrewSectionPreview() {
    val demoMetadata = ExtendedContentMetadata(
        fullCast = listOf(
            CastMember(1, "Actor One", "Main Character", null, 0),
            CastMember(2, "Actor Two", "Supporting Character", null, 1),
            CastMember(3, "Actor Three", "Villain", null, 2),
            CastMember(4, "Actor Four", "Comic Relief", null, 3),
            CastMember(5, "Actor Five", "Love Interest", null, 4)
        ),
        crew = listOf(
            CrewMember(101, "Director Name", "Director", "Directing", null),
            CrewMember(102, "Producer Name", "Executive Producer", "Production", null),
            CrewMember(103, "Writer Name", "Screenplay", "Writing", null),
            CrewMember(104, "Composer Name", "Composer", "Sound", null)
        )
    )
    
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            CastCrewSection(
                metadata = demoMetadata,
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }
    }
}