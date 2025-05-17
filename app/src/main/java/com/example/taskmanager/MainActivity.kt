package com.example.taskmanager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.taskmanager.notifications.NotificationWorker
import com.example.taskmanager.ui.theme.TaskManagerTheme
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authViewModel: AuthViewModel by viewModels()

        setContent {
            TaskManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyAppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    fun runNotificationWorkersIfLoggedIn() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            Log.d("MainActivity", "User logged in: ${user.uid}, scheduling workers")
            scheduleDailyNotificationWorker()
            triggerInstantNotificationCheck()
        } else {
            Log.d("MainActivity", "No user logged in, skipping notification workers")
        }
    }

    private fun scheduleDailyNotificationWorker() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .addTag("daily_task_reminder")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_task_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun triggerInstantNotificationCheck() {
        val tag = "instant_task_reminder"
        val testWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .addTag(tag)
            .build()

        // 🔁 Cancel any existing instances before enqueueing
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(tag)
        WorkManager.getInstance(applicationContext).enqueue(testWork)

        Log.d("MainActivity", "Instant notification worker enqueued")
    }

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, 9)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        if (now.after(target)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
