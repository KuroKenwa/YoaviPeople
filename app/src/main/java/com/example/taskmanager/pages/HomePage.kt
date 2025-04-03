package com.example.taskmanager.pages

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskmanager.AuthState
import com.example.taskmanager.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val authState = authViewModel.authState.observeAsState()

    // Auto-redirect to login if unauthenticated
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login") {
                popUpTo(0) { inclusive = true } // Clears all previous screens
            }
            else -> Unit
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home Page", fontSize = 32.sp)

        // Improved Sign-Out Button
        TextButton(onClick = {
            signOut(navController, authViewModel)
        }) {
            Text(text = "Sign out")
        }

        TextButton(onClick = {
            navController.navigate("assign")
        }) {
            Text(text = "test test Assign")
        }

        TextButton(onClick = {
            navController.navigate("allworks")
        }) {
            Text(text = "test test allworks")
        }
    }
}

// Improved Sign-Out Function
fun signOut(navController: NavController, authViewModel: AuthViewModel) {
    Log.d("AuthDebug", "User is attempting to sign out...")

    FirebaseAuth.getInstance().currentUser?.reload()?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            FirebaseAuth.getInstance().signOut()
            authViewModel.signout()

            Log.d("AuthDebug", "Sign-out successful. Navigating to login.")

            // Ensure Firebase state updates before navigating
            Handler(Looper.getMainLooper()).postDelayed({
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }, 500) // Small delay ensures proper state update
        } else {
            Log.e("AuthDebug", "Error reloading Firebase user: ${task.exception?.message}")
        }
    }
}
