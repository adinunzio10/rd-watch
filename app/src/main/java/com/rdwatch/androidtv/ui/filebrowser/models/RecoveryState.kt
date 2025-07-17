package com.rdwatch.androidtv.ui.filebrowser.models

/**
 * State of the recovery operation
 */
data class RecoveryState(
    val isRecovering: Boolean = false,
    val operationType: BulkOperationType = BulkOperationType.DELETE,
    val totalFailures: Int = 0,
    val recoveredCount: Int = 0,
    val currentAttempt: Int = 0,
    val maxAttempts: Int = 0,
    val finalResult: BulkOperationResult? = null,
) {
    val recoveryProgress: Float
        get() = if (totalFailures > 0) recoveredCount.toFloat() / totalFailures else 0f

    val isComplete: Boolean
        get() = !isRecovering && finalResult != null
}
