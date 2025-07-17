package com.rdwatch.androidtv.ui.filebrowser.navigation

import androidx.navigation.NavController
import com.rdwatch.androidtv.presentation.navigation.Screen
import com.rdwatch.androidtv.ui.filebrowser.models.AccountType
import com.rdwatch.androidtv.ui.filebrowser.models.FileItem

/**
 * Helper class for file browser navigation
 */
object FileBrowserNavigationHelper {
    /**
     * Navigate to the file browser screen
     */
    fun navigateToFileBrowser(
        navController: NavController,
        accountType: AccountType = AccountType.REAL_DEBRID,
    ) {
        navController.navigate(
            Screen.AccountFileBrowser(accountType = accountType.name.lowercase()),
        )
    }

    /**
     * Navigate to video player for a playable file
     */
    fun navigateToPlayer(
        navController: NavController,
        file: FileItem.File,
        playbackUrl: String,
    ) {
        if (file.isPlayable) {
            navController.navigate(
                Screen.VideoPlayer(
                    videoUrl = playbackUrl,
                    title = file.name,
                ),
            )
        }
    }

    /**
     * Navigate back from file browser
     */
    fun navigateBack(navController: NavController) {
        navController.navigateUp()
    }
}
