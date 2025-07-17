package com.rdwatch.androidtv.ui.settings.scrapers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Dialog for adding a new scraper from URL
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScraperDialog(
    urlText: String,
    isLoading: Boolean,
    onUrlChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
            ),
    ) {
        Card(
            modifier =
                modifier
                    .wrapContentSize()
                    .widthIn(min = 400.dp, max = 600.dp)
                    .clip(RoundedCornerShape(16.dp)),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Add Scraper",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    var closeFocused by remember { mutableStateOf(false) }
                    TVFocusIndicator(isFocused = closeFocused) {
                        IconButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier.tvFocusable(
                                    onFocusChanged = { closeFocused = it.isFocused },
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint =
                                    if (closeFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }
                }

                // Description
                Text(
                    text =
                        "Enter the URL of a scraper manifest to add it to your collection. " +
                            "The manifest will be validated before being added.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start,
                )

                // URL input field
                var textFieldFocused by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = urlText,
                    onValueChange = onUrlChanged,
                    label = { Text("Manifest URL") },
                    placeholder = { Text("https://example.com/manifest.json") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { textFieldFocused = it.isFocused },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (urlText.isNotBlank()) {
                                    onAdd()
                                }
                            },
                        ),
                    enabled = !isLoading,
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                )

                // Example URLs section
                Text(
                    text = "Examples:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ExampleUrl(
                        url = "https://stremio-jackett.sleeyax.dev/manifest.json",
                        description = "Jackett Addon",
                        onClick = { onUrlChanged(it) },
                    )
                    ExampleUrl(
                        url = "https://torrentio.strem.fun/manifest.json",
                        description = "Torrentio",
                        onClick = { onUrlChanged(it) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    var cancelFocused by remember { mutableStateOf(false) }
                    var addFocused by remember { mutableStateOf(false) }

                    TVFocusIndicator(isFocused = cancelFocused) {
                        TextButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier.tvFocusable(
                                    onFocusChanged = { cancelFocused = it.isFocused },
                                ),
                            enabled = !isLoading,
                        ) {
                            Text("Cancel")
                        }
                    }

                    TVFocusIndicator(isFocused = addFocused) {
                        Button(
                            onClick = onAdd,
                            modifier =
                                Modifier.tvFocusable(
                                    onFocusChanged = { addFocused = it.isFocused },
                                ),
                            enabled = !isLoading && urlText.isNotBlank(),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Add Scraper")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExampleUrl(
    url: String,
    description: String,
    onClick: (String) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        TextButton(
            onClick = { onClick(url) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .tvFocusable(onFocusChanged = { isFocused = it.isFocused }),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        },
                )
            }
        }
    }
}
