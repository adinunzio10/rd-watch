package com.rdwatch.androidtv.ui.filebrowser.models

/**
 * State for bulk operations with progress tracking
 */
data class BulkOperationState(
    val isRunning: Boolean = false,
    val operationType: BulkOperationType = BulkOperationType.DELETE,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val failedItems: Int = 0,
    val currentItem: String? = null,
    val errors: List<BulkOperationError> = emptyList(),
    val canCancel: Boolean = true,
    val canRetry: Boolean = false,
    val rollbackAvailable: Boolean = false,
) {
    val progressPercentage: Float
        get() = if (totalItems > 0) ((completedItems + failedItems).toFloat() / totalItems) * 100f else 0f

    val isComplete: Boolean
        get() = !isRunning && (completedItems + failedItems) >= totalItems

    val hasErrors: Boolean
        get() = failedItems > 0

    val successCount: Int
        get() = completedItems
}

/**
 * Type of bulk operation being performed
 */
enum class BulkOperationType(val displayName: String) {
    DELETE("Deleting"),
    DOWNLOAD("Downloading"),
    MOVE("Moving"),
    COPY("Copying"),
    ARCHIVE("Archiving"),
    SELECT_FILES("Selecting Files"),
}

/**
 * Error information for failed bulk operations
 */
data class BulkOperationError(
    val itemId: String,
    val itemName: String,
    val error: String,
    val isRetryable: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Result of a bulk operation
 */
data class BulkOperationResult(
    val operationType: BulkOperationType,
    val totalItems: Int,
    val successCount: Int,
    val failedCount: Int,
    val errors: List<BulkOperationError>,
    val rollbackActions: List<RollbackAction> = emptyList(),
    val completionTime: Long = System.currentTimeMillis(),
) {
    val isPartialSuccess: Boolean
        get() = successCount > 0 && failedCount > 0

    val isCompleteSuccess: Boolean
        get() = successCount == totalItems && failedCount == 0

    val isCompleteFailure: Boolean
        get() = successCount == 0 && failedCount > 0
}

/**
 * Action that can be performed to rollback a bulk operation
 */
data class RollbackAction(
    val itemId: String,
    val itemName: String,
    val actionType: RollbackActionType,
    val originalData: String? = null,
)

/**
 * Type of rollback action
 */
enum class RollbackActionType(val displayName: String) {
    RESTORE_DELETED("Restore Deleted"),
    CANCEL_DOWNLOAD("Cancel Download"),
    UNDO_MOVE("Undo Move"),
    UNDO_COPY("Delete Copy"),
    EXTRACT_ARCHIVE("Extract Archive"),
    DESELECT_FILES("Deselect Files"),
}

/**
 * Progress update for bulk operations
 */
data class BulkOperationProgress(
    val itemId: String,
    val itemName: String,
    val progress: Float, // 0.0 to 1.0
    val status: BulkOperationItemStatus,
    val error: String? = null,
)

/**
 * Status of individual items in bulk operation
 */
enum class BulkOperationItemStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    RETRYING,
}

/**
 * Configuration for bulk operations
 */
data class BulkOperationConfig(
    val enableRollback: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val batchSize: Int = 5,
    val timeoutMs: Long = 30000,
    val continueOnError: Boolean = true,
)
