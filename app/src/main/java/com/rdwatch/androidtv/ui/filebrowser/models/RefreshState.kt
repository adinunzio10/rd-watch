package com.rdwatch.androidtv.ui.filebrowser.models

/**
 * State of the pull-to-refresh operation
 */
data class RefreshState(
    val isRefreshing: Boolean = false,
    val lastRefreshTime: Long = 0L,
    val isSuccess: Boolean = false,
    val error: String? = null,
) {
    val canRefresh: Boolean
        get() = !isRefreshing

    val timeSinceLastRefresh: Long
        get() = System.currentTimeMillis() - lastRefreshTime

    val hasError: Boolean
        get() = error != null
}
