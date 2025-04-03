package com.example.taskmanager.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskmanager.model.Task
import com.example.taskmanager.model.TaskPriority
import com.example.taskmanager.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    val userNames = remember { mutableStateMapOf<String, String>() }
    val userId = currentUser?.uid
    var userNameForBar by remember { mutableStateOf("User") }
    var isLoading by remember { mutableStateOf(true) }

    // Real-time Firestore listener
    LaunchedEffect(userId) {
        userId?.let { uid ->
            db.collectionGroup("tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Error getting tasks: ", error)
                        isLoading = false
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        val taskList = snapshot.documents.mapNotNull { document ->
                            try {
                                val task = document.toObject(Task::class.java)?.copy(
                                    taskId = document.id,
                                    priority = TaskPriority.fromString(document.getString("priority") ?: ""),
                                    status = TaskStatus.fromString(document.getString("status") ?: "")
                                )
                                task
                            } catch (e: Exception) {
                                Log.e("Firestore", "Error parsing task: ${e.localizedMessage}")
                                null
                            }
                        }
                        tasks = taskList.filter { task -> task.assignedUsers.contains(uid) }
                        fetchUserNames(tasks.flatMap { it.assignedUsers }.toSet(), userNames, db)
                    }

                    isLoading = false
                }
        }
    }

    // Fetch logged-in user's name
    LaunchedEffect(userId) {
        userId?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    userNameForBar = document.getString("name") ?: "User"
                }
                .addOnFailureListener {
                    Log.e("TodoScreen", "Failed to fetch user name", it)
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("All Tasks", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.background(Color(0xFF3F51B5)),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3F51B5)),
            actions = {
                Text(text = "User: $userNameForBar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(tasks) { task ->
                    TaskCard(task, userNames, navController, db, userId!!)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { navController.navigate("addTask") },
            containerColor = Color(0xFF3F51B5)
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    userNames: Map<String, String>,
    navController: NavController,
    db: FirebaseFirestore,
    userId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF363636))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = task.dueDate, color = Color(0xFF64B5F6), fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.status.status,
                        color = when (task.status) {
                            TaskStatus.PENDING -> Color(0xFFE91E63)
                            TaskStatus.IN_PROGRESS -> Color(0xFFFF9800)
                            TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                        },
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Green)
                    )
                }
            }

            Text(text = task.title, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            Text(text = "Description:", color = Color.White, fontSize = 14.sp)
            Text(text = task.description, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))

            Text(
                text = "Priority: ${task.priority.level}",
                color = Color(0xFFFFC107),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            val assignedNames = task.assignedUsers.mapNotNull { userNames[it] }.ifEmpty { listOf("Unknown") }
            Text(
                text = "Assigned to: ${assignedNames.joinToString()}",
                color = Color(0xFF64B5F6),
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { navController.navigate("editTask/${task.taskId}/$userId") }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Task", tint = Color.White)
                }
                IconButton(onClick = { deleteTask(task.taskId, db, userId) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Task", tint = Color.Red)
                }
            }
        }
    }
}

fun deleteTask(taskId: String, db: FirebaseFirestore, userId: String) {
    db.collection("users").document(userId).collection("tasks").document(taskId)
        .delete()
        .addOnSuccessListener {
            Log.d("Firestore", "Task successfully deleted!")
        }
        .addOnFailureListener { e ->
            Log.w("Firestore", "Error deleting task", e)
        }
}

fun fetchUserNames(userIds: Set<String>, userNames: MutableMap<String, String>, db: FirebaseFirestore) {
    userIds.forEach { userId ->
        if (!userNames.containsKey(userId)) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    document.getString("name")?.let { name ->
                        userNames[userId] = name
                    }
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Error fetching user name for ID: $userId", it)
                }
        }
    }
}
