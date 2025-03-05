package com.example.taskmanager.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.taskmanager.AuthViewModel
import com.example.taskmanager.model.Task
import com.example.taskmanager.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val userNames = remember { mutableStateMapOf<String, String>() }
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    var userNameForBar by remember { mutableStateOf("User") }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documents: QuerySnapshot? = task.result
                        val taskList = documents?.toObjects(Task::class.java) ?: emptyList()
                        tasks = taskList.filter { it.assignedUsers.contains(userId) }
                        fetchUserNames(tasks.flatMap { it.assignedUsers }.toSet(), userNames, db)
                    } else {
                        Log.e("Firestore", "Error getting tasks.", task.exception)
                    }
                    isLoading = false
                }
        } ?: run {
            isLoading = false
        }
    }

    LaunchedEffect(userId) {
        userId?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userNameForBar = document.getString("name") ?: "User"
                    }
                }
                .addOnFailureListener {
                    Log.e("TodoScreen", "Failed to fetch user name", it)
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "All Tasks",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

            },
            modifier = Modifier.background(Color(0xFF3F51B5)),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF3F51B5)
            ),

            actions = {
                Text(
                    text = "UserName : " + userNameForBar
                    ,color = Color.White
                    , fontSize = 15.sp
                    , fontWeight = FontWeight.Bold
                    ,

                    )
            }


        )



        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(task, userNames)
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
fun TaskCard(task: Task, userNames: Map<String, String>) {
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
                        color = if (task.status == TaskStatus.PENDING) Color(0xFFE91E63) else Color(0xFF4CAF50),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Green)
                    )
                }
            }

            Text(text = task.title, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            Text(text = "Description:", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(text = task.description, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))

            if (task.assignedUsers.isNotEmpty()) {
                val assignedNames = task.assignedUsers.mapNotNull { userNames[it] }.joinToString()
                Text(
                    text = "Assigned to: ${if (assignedNames.isNotEmpty()) assignedNames else "Unknown"}",
                    color = Color(0xFF64B5F6),
                    fontSize = 12.sp
                )
            }
        }
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
