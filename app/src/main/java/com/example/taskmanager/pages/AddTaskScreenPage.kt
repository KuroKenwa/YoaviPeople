package com.example.taskmanager.pages

import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import com.example.taskmanager.utils.addTaskToFirestore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskmanager.model.TaskPriority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@Composable
fun AddTaskScreen(navController: NavController, onTaskAdded: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.LOW) }
    var isCollaborative by remember { mutableStateOf(false) }
    var showUserSelection by remember { mutableStateOf(false) }
    var selectedUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            db.collection("users").get()
                .addOnSuccessListener { result ->
                    val fetchedUsers = result.documents.mapNotNull { doc ->
                        val name = doc.getString("name") // Fetch name field
                        val id = doc.id
                        if (id != userId && name != null) id to name else null
                    }
                    if (fetchedUsers.isEmpty()) {
                        Log.e("Firestore", "No other users found in Firestore.")
                    } else {
                        Log.d("Firestore", "Fetched users: $fetchedUsers")
                    }
                    allUsers = fetchedUsers
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Error fetching users", it)
                }
        }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create New Task", fontSize = 20.sp, fontWeight = FontWeight.Bold)

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
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dueDate,
            onValueChange = { dueDate = it },
            label = { Text("Due Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Collaborative Task:")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = isCollaborative, onCheckedChange = {
                isCollaborative = it
                if (it) showUserSelection = true else selectedUsers = emptyList()
            })
        }

        if (showUserSelection) {
            AlertDialog(
                onDismissRequest = { showUserSelection = false },
                confirmButton = {
                    Button(onClick = { showUserSelection = false }) {
                        Text("Done")
                    }
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
                addTaskToFirestore(title, description, dueDate, selectedPriority, selectedUsers)
                onTaskAdded()
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
        ) {
            Text("Add Task", color = Color.White)
        }


    }
    fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
            onDateSelected("$selectedDay/${selectedMonth + 1}/$selectedYear")
        }, year, month, day).show()
    }
}
