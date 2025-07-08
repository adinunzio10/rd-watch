package com.rdwatch.androidtv.ui.filebrowser.models

/**
 * Data class representing user preferences for the file browser
 * These preferences are persisted across sessions
 */
data class FileBrowserPreferences(
    val defaultSortBy: SortBy = SortBy.NAME,
    val defaultSortOrder: SortOrder = SortOrder.ASCENDING,
    val showHiddenFiles: Boolean = false,
    val rememberLastPath: Boolean = true,
    val lastVisitedPath: String = "/",
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val gridColumnCount: Int = 4,
    val showFileSize: Boolean = true,
    val showModifiedDate: Boolean = true,
    val showFileStatus: Boolean = true,
    val autoPlayOnSelect: Boolean = false,
    val confirmBeforeDelete: Boolean = true,
    val defaultFileTypeFilter: Set<FileType> = emptySet()
)


/**
 * Preference keys for DataStore
 */
object FileBrowserPreferenceKeys {
    const val PREF_DEFAULT_SORT_BY = "file_browser_default_sort_by"
    const val PREF_DEFAULT_SORT_ORDER = "file_browser_default_sort_order"
    const val PREF_SHOW_HIDDEN_FILES = "file_browser_show_hidden_files"
    const val PREF_REMEMBER_LAST_PATH = "file_browser_remember_last_path"
    const val PREF_LAST_VISITED_PATH = "file_browser_last_visited_path"
    const val PREF_DEFAULT_VIEW_MODE = "file_browser_default_view_mode"
    const val PREF_GRID_COLUMN_COUNT = "file_browser_grid_column_count"
    const val PREF_SHOW_FILE_SIZE = "file_browser_show_file_size"
    const val PREF_SHOW_MODIFIED_DATE = "file_browser_show_modified_date"
    const val PREF_SHOW_FILE_STATUS = "file_browser_show_file_status"
    const val PREF_AUTO_PLAY_ON_SELECT = "file_browser_auto_play_on_select"
    const val PREF_CONFIRM_BEFORE_DELETE = "file_browser_confirm_before_delete"
    const val PREF_DEFAULT_FILE_TYPE_FILTER = "file_browser_default_file_type_filter"
}