package com.rdwatch.androidtv.ui.filebrowser.models

/**
 * Sort options for the file browser
 */
enum class FileSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_ASC("Size (Small)"),
    SIZE_DESC("Size (Large)"),
    DATE_ASC("Date (Oldest)"),
    DATE_DESC("Date (Newest)"),
    TYPE_ASC("Type (A-Z)"),
    TYPE_DESC("Type (Z-A)")
}