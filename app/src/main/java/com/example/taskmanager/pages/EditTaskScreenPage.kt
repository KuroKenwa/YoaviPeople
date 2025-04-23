package com.example.taskmanager.pages

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskmanager.model.TaskPriority
import com.example.taskmanager.model.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditTaskScreenPage(navController: NavController, taskId: String, userId: String) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var priority by remember { mutableStateOf(TaskPriority.LOW) }
    var status by remember { mutableStateOf(TaskStatus.PENDING) }
    var assignedUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(true) }

    LaunchedEffect(taskId) {
        db.collection("users").document(userId).collection("tasks").document(taskId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    title = TextFieldValue(document.getString("title") ?: "")
                    description = TextFieldValue(document.getString("description") ?: "")
                    priority = TaskPriority.fromString(document.getString("priority") ?: "")
                    status = TaskStatus.fromString(document.getString("status") ?: "")
                    assignedUsers = (document.get("assignedUsers") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
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
        AnimatedVisibility(
            visible = showContent,
            exit = scaleOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
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

                var expandedPriority by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expandedPriority = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Priority: ${priority.level}")
                    }
                    DropdownMenu(expanded = expandedPriority, onDismissRequest = { expandedPriority = false }) {
                        TaskPriority.entries.forEach { level ->
                            DropdownMenuItem(text = { Text(level.level) }, onClick = {
                                priority = level
                                expandedPriority = false
                            })
                        }
                    }
                }

                var expandedStatus by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expandedStatus = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Status: ${status.status}")
                    }
                    DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                        TaskStatus.entries.forEach { state ->
                            DropdownMenuItem(text = { Text(state.status) }, onClick = {
                                status = state
                                expandedStatus = false
                            })
                        }
                    }
                }

                Button(
                    onClick = {
                        if (status == TaskStatus.COMPLETED) {
                            showContent = false
                            scope.launch {
                                delay(300)
                                db.collection("users").document(userId).collection("tasks").document(taskId)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d("EditTaskScreen", "Task deleted after marking as completed")
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener {
                                        Log.e("EditTaskScreen", "Error deleting completed task", it)
                                    }
                            }
                        } else {
                            val updatedTask = mapOf(
                                "title" to title.text,
                                "description" to description.text,
                                "priority" to priority.name,
                                "status" to status.name
                            )

                            db.collection("users").document(userId).collection("tasks").document(taskId)
                                .update(updatedTask)
                                .addOnSuccessListener { Log.d("EditTaskScreen", "Task updated!") }
                                .addOnFailureListener { Log.e("EditTaskScreen", "Update failed", it) }

                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}
