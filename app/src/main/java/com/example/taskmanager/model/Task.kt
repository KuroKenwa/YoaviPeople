package com.example.taskmanager.model

// Enum class for Task Status
enum class TaskStatus(val status: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

// Enum class for Task Priority
enum class TaskPriority(val level: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

// Data class to represent a Task
data class Task(
    val taskId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.LOW,
    val assignedUsers: List<String> = emptyList() // Added this field
)
