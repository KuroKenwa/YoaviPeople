package com.example.taskmanager.notifications

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.taskmanager.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid

    override fun doWork(): Result {
        Log.d("NotificationWorker", "Worker started")
        if (userId == null) {
            Log.d("NotificationWorker", "User not logged in, skipping")
            return Result.success()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowDate = dateFormat.format(calendar.time)

        Log.d("NotificationWorker", "Looking for tasks due on $tomorrowDate")

        firestore.collectionGroup("tasks")
            .whereEqualTo("dueDate", tomorrowDate)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("NotificationWorker", "Found ${documents.size()} tasks")

                // 🔔 Always send test notification
                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = "Worker Test",
                    message = "Checked ${documents.size()} tasks"
                )

                for (doc in documents) {
                    val task = doc.toObject(Task::class.java)
                    val isAssignedToUser = task.assignedUsers.contains(userId)
                    val isOwnedByUser = task.creatorId == userId

                    Log.d("NotificationWorker", "Task: ${task.title}, Assigned: $isAssignedToUser, Owned: $isOwnedByUser")

                    if (isAssignedToUser || isOwnedByUser) {
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            title = "Task due tomorrow",
                            message = "${task.title} is due on $tomorrowDate"
                        )
                        Log.d("NotificationWorker", "Notification sent for task: ${task.title}")
                    }
                }
            }
            .addOnFailureListener {
                Log.e("NotificationWorker", "Firestore error: ", it)
            }

        return Result.success()
    }
}
