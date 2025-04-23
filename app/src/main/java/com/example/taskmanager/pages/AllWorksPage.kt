
// AllWorksPage.kt – Final Version with Functional Delete (Collaborative + Normal)

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
fun TodoScreen(modifier: Modifier = Modifier, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    val userNames = remember { mutableStateMapOf<String, String>() }
    var userNameForBar by remember { mutableStateOf("User") }
    var isLoading by remember { mutableStateOf(true) }

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
                        val taskList = it.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Task::class.java)?.copy(
                                    taskId = doc.id,
                                    priority = TaskPriority.fromString(doc.getString("priority") ?: ""),
                                    status = TaskStatus.fromString(doc.getString("status") ?: "")
                                )
                            } catch (e: Exception) {
                                Log.e("Firestore", "Parse error: ${e.localizedMessage}")
                                null
                            }
                        }.filter { task -> task.assignedUsers.contains(uid) }

                        tasks = taskList
                        fetchUserNames(taskList.flatMap { task -> task.assignedUsers }.toSet(), userNames, db)
                    }

                    isLoading = false
                }
        }
    }

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E3A59))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Date & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.dueDate,
                    color = Color(0xFF90CAF9),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.status.status,
                        color = when (task.status) {
                            TaskStatus.PENDING -> Color(0xFFFF6F61)
                            TaskStatus.IN_PROGRESS -> Color(0xFFFFC107)
                            TaskStatus.COMPLETED -> Color(0xFF66BB6A)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (task.status) {
                                    TaskStatus.PENDING -> Color(0xFFFF6F61)
                                    TaskStatus.IN_PROGRESS -> Color(0xFFFFC107)
                                    TaskStatus.COMPLETED -> Color(0xFF66BB6A)
                                }
                            )
                    )
                }
            }

            // Title
            Text(
                text = task.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Description
            Text(
                text = task.description,
                color = Color(0xFFCFD8DC),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Priority
            Text(
                text = "Priority: ${task.priority.level}",
                color = when (task.priority) {
                    TaskPriority.HIGH -> Color(0xFFFF5252)
                    TaskPriority.MEDIUM -> Color(0xFFFFA726)
                    TaskPriority.LOW -> Color(0xFF81C784)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Assigned Users
            val assignedNames = task.assignedUsers.mapNotNull { userNames[it] }.ifEmpty { listOf("Unknown") }
            Text(
                text = "Assigned to: ${assignedNames.joinToString()}",
                color = Color(0xFF90CAF9),
                fontSize = 13.sp
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { navController.navigate("editTask/${task.taskId}/$userId") }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = Color(0xFFBBDEFB)
                    )
                }
                IconButton(onClick = { deleteTask(task.taskId, db, userId, task.userId) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = Color(0xFFFF8A65)
                    )
                }
            }
        }
    }
}


fun deleteTask(taskId: String, db: FirebaseFirestore, currentUserId: String, creatorId: String) {
    val creatorTaskRef = db.collection("users").document(creatorId).collection("tasks").document(taskId)

    creatorTaskRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            val assignedUsers = (document.get("assignedUsers") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: return@addOnSuccessListener

            if (currentUserId == creatorId) {
                // Creator: just delete the task
                creatorTaskRef.delete()
            } else {
                assignedUsers.remove(currentUserId)
                if (assignedUsers.isEmpty()) {
                    creatorTaskRef.delete()
                } else {
                    creatorTaskRef.update("assignedUsers", assignedUsers)
                }
            }
        }
    }.addOnFailureListener {
        Log.e("Firestore", "Error deleting task", it)
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
