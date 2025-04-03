package com.example.taskmanager.model

import com.google.firebase.firestore.PropertyName

// Enum class for Task Status
enum class TaskStatus(val status: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    companion object {
        fun fromString(value: String): TaskStatus {
            // Match Firestore enum name like "PENDING"
            return entries.find { it.name == value } ?: PENDING
        }
    }
}

// Enum class for Task Priority
enum class TaskPriority(val level: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        fun fromString(value: String): TaskPriority {
            // Match Firestore enum name like "LOW"
            return entries.find { it.name == value } ?: LOW
        }
    }
}

// Data class to represent a Task
data class Task(
    @get:PropertyName("taskId") @set:PropertyName("taskId")
    var taskId: String = "",

    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("dueDate") @set:PropertyName("dueDate")
    var dueDate: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var status: TaskStatus = TaskStatus.PENDING,

    @get:PropertyName("priority") @set:PropertyName("priority")
    var priority: TaskPriority = TaskPriority.LOW,

    @get:PropertyName("assignedUsers") @set:PropertyName("assignedUsers")
    var assignedUsers: List<String> = emptyList()
) {
    // Firestore requires a no-argument constructor
    constructor() : this("", "", "", "", "", TaskStatus.PENDING, TaskPriority.LOW, emptyList())
}
