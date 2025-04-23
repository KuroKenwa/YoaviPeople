package com.example.taskmanager.pages

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskmanager.model.TaskPriority
import com.example.taskmanager.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Locale

@Composable
fun AddTaskScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.LOW) }
    var selectedStatus by remember { mutableStateOf(TaskStatus.PENDING) }

    var isCollaborative by remember { mutableStateOf(false) }
    var showUserSelection by remember { mutableStateOf(false) }
    var selectedUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").get()
                .addOnSuccessListener { result ->
                    val fetchedUsers = result.documents.mapNotNull { doc ->
                        val name = doc.getString("name")
                        val id = doc.id
                        if (id != uid && name != null) id to name else null
                    }
                    allUsers = fetchedUsers
                }
                .addOnFailureListener {
                    Log.e("AddTaskScreen", "Failed to fetch users", it)
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Create New Task", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

        val context = LocalContext.current
        val calendar = Calendar.getInstance()

        Button(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val formattedMonth = String.format(Locale.US, "%02d", month + 1)
                        val formattedDay = String.format(Locale.US, "%02d", dayOfMonth)
                        dueDate = "$year-$formattedMonth-$formattedDay"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9))
        ) {
            Text(
                text = if (dueDate.isBlank()) "Pick Due Date" else "Due Date: $dueDate",
                color = Color.Black
            )
        }

        var priorityExpanded by remember { mutableStateOf(false) }
        Box {
            Button(onClick = { priorityExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Priority: ${selectedPriority.level}")
            }
            DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                TaskPriority.entries.forEach { priority ->
                    DropdownMenuItem(text = { Text(priority.level) }, onClick = {
                        selectedPriority = priority
                        priorityExpanded = false
                    })
                }
            }
        }

        var statusExpanded by remember { mutableStateOf(false) }
        Box {
            Button(onClick = { statusExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Status: ${selectedStatus.status}")
            }
            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                TaskStatus.entries.forEach { status ->
                    DropdownMenuItem(text = { Text(status.status) }, onClick = {
                        selectedStatus = status
                        statusExpanded = false
                    })
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Collaborative Task:")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = isCollaborative, onCheckedChange = {
                isCollaborative = it
                showUserSelection = it
                if (!it) selectedUsers = emptyList()
            })
        }

        if (showUserSelection) {
            AlertDialog(
                onDismissRequest = { showUserSelection = false },
                confirmButton = {
                    Button(onClick = { showUserSelection = false }) { Text("Done") }
                },
                title = { Text("Select Users") },
                text = {
                    Column {
                        if (allUsers.isEmpty()) {
                            Text("No other users found.")
                        } else {
                            allUsers.forEach { (id, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedUsers.contains(id),
                                        onCheckedChange = { checked ->
                                            selectedUsers = if (checked) selectedUsers + id else selectedUsers - id
                                        }
                                    )
                                    Text(name)
                                }
                            }
                        }
                    }
                }
            )
        }

        Button(
            onClick = {
                currentUser?.let { user ->
                    val finalUsers = (selectedUsers + user.uid).distinct()
                    val task = hashMapOf(
                        "title" to title,
                        "description" to description,
                        "dueDate" to dueDate,
                        "priority" to selectedPriority.name,
                        "status" to selectedStatus.name,
                        "userId" to user.uid,
                        "assignedUsers" to finalUsers,
                        "creatorId" to user.uid
                    )

                    db.collection("users").document(user.uid).collection("tasks").add(task)
                        .addOnSuccessListener {
                            Log.d("AddTaskScreen", "Task added successfully")
                            navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Log.e("AddTaskScreen", "Error adding task", e)
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
        ) {
            Text("Add Task", color = Color.White)
        }
    }
}
