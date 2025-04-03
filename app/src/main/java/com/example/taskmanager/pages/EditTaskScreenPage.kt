package com.example.taskmanager.pages

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskmanager.model.TaskPriority
import com.example.taskmanager.model.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditTaskScreenPage(navController: NavController, taskId: String, userId: String) {
    val db = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var priority by remember { mutableStateOf(TaskPriority.LOW) }
    var status by remember { mutableStateOf(TaskStatus.PENDING) }
    var assignedUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(taskId) {
        db.collection("users").document(userId).collection("tasks").document(taskId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    title = TextFieldValue(document.getString("title") ?: "")
                    description = TextFieldValue(document.getString("description") ?: "")
                    priority = TaskPriority.fromString(document.getString("priority") ?: "Low")
                    status = TaskStatus.fromString(document.getString("status") ?: "Pending")
                    assignedUsers = (document.get("assignedUsers") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()
                }
                isLoading = false
            }
            .addOnFailureListener {
                Log.e("EditTaskScreen", "Failed to fetch task", it)
                isLoading = false
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Edit Task", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            // Priority Dropdown
            var expandedPriority by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { expandedPriority = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Priority: ${priority.level}")
                }
                DropdownMenu(expanded = expandedPriority, onDismissRequest = { expandedPriority = false }) {
                    TaskPriority.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.level) },
                            onClick = {
                                priority = level
                                expandedPriority = false
                            }
                        )
                    }
                }
            }

            // Status Dropdown
            var expandedStatus by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { expandedStatus = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Status: ${status.status}")
                }
                DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                    TaskStatus.entries.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state.status) },
                            onClick = {
                                status = state
                                expandedStatus = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val updatedTask = hashMapOf(
                        "title" to title.text,
                        "description" to description.text,
                        "priority" to priority.name,
                        "status" to status.name
                    )

                    assignedUsers.forEach { uid ->
                        db.collection("users").document(uid).collection("tasks").document(taskId)
                            .update(updatedTask as Map<String, Any>)
                            .addOnSuccessListener {
                                Log.d("EditTaskScreen", "Task updated successfully for $uid")
                            }
                            .addOnFailureListener {
                                Log.e("EditTaskScreen", "Error updating task for $uid", it)
                            }
                    }

                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Save Changes")
            }
        }
    }
}
