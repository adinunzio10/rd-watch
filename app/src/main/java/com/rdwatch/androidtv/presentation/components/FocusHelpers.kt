package com.rdwatch.androidtv.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.tvFocusable(
    onFocusChanged: ((Boolean) -> Unit)? = null
): Modifier = this
    .focusable()
    .onFocusChanged { focusState ->
        onFocusChanged?.invoke(focusState.isFocused)
    }

@Composable
fun Modifier.tvClickable(
    onClick: () -> Unit,
    onFocusChanged: ((Boolean) -> Unit)? = null
): Modifier = this
    .tvFocusable(onFocusChanged)
    .clip(RoundedCornerShape(8.dp))

@Composable
fun rememberTvFocusRequester(): FocusRequester = remember { FocusRequester() }

@Composable
fun rememberTVFocusRequester(): FocusRequester = remember { FocusRequester() }

@Composable
fun Modifier.tvFocusRequester(
    focusRequester: FocusRequester,
    requestFocus: Boolean = false
): Modifier {
    if (requestFocus) {
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }
    }
    return this.focusRequester(focusRequester)
}

@Composable
fun Modifier.tvFocusBorder(
    isFocused: Boolean,
    focusedColor: Color = MaterialTheme.colorScheme.primary,
    unfocusedColor: Color = Color.Transparent
): Modifier = this.border(
    width = if (isFocused) 3.dp else 0.dp,
    color = if (isFocused) focusedColor else unfocusedColor,
    shape = RoundedCornerShape(8.dp)
)

@Composable
fun Modifier.tvFocusScale(
    isFocused: Boolean,
    focusedScale: Float = 1.1f,
    unfocusedScale: Float = 1.0f
): Modifier = this.scale(if (isFocused) focusedScale else unfocusedScale)

@Composable
fun Modifier.tvCardPadding(
    isFocused: Boolean = false
): Modifier = this.padding(
    horizontal = if (isFocused) 8.dp else 12.dp,
    vertical = if (isFocused) 8.dp else 12.dp
)

@Composable
fun Modifier.tvSafeFocusable(): Modifier = this
    .focusable()
    .padding(4.dp)
    .clip(RoundedCornerShape(4.dp))

@Composable
fun Modifier.tvCardFocus(
    isFocused: Boolean,
    onClick: (() -> Unit)? = null
): Modifier = this
    .tvFocusBorder(isFocused)
    .tvFocusScale(isFocused)
    .tvCardPadding(isFocused)
    .tvSafeFocusable()