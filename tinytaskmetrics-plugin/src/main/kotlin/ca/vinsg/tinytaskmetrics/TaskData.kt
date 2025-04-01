package ca.vinsg.tinytaskmetrics

/**
 * Data class to hold information about a task's execution.
 */
data class TaskData(
    val durationMs: Long,
    val skipped: Boolean,
    val isAndroidTask: Boolean
)