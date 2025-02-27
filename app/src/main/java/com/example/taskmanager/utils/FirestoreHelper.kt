package com.example.taskmanager.utils

import android.util.Log
import com.example.taskmanager.model.Task
import com.example.taskmanager.model.TaskPriority
import com.example.taskmanager.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun addTaskToFirestore(title: String, description: String, dueDate: String, priority: TaskPriority, assignedUsers: List<String>) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    currentUser?.uid?.let { userId ->
        val taskId = db.collection("users").document(userId).collection("tasks").document().id
        val newTask = Task(
            taskId = taskId,
            userId = userId,
            title = title,
            description = description,
            dueDate = dueDate,
            priority = priority,
            status = TaskStatus.PENDING,
            assignedUsers = assignedUsers + userId
        )

        // Store task for the creator
        db.collection("users").document(userId).collection("tasks").document(taskId)
            .set(newTask)
            .addOnFailureListener { Log.e("Firestore", "Error adding task", it) }

        // Store task for collaborators
        assignedUsers.forEach { collaboratorId ->
            db.collection("users").document(collaboratorId).collection("tasks").document(taskId)
                .set(newTask)
                .addOnFailureListener { Log.e("Firestore", "Error assigning task", it) }
        }
    }
}
