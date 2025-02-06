package com.example.taskmanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskmanager.pages.AssignWorkPage
import com.example.taskmanager.pages.HomePage
import com.example.taskmanager.pages.LoginPage
import com.example.taskmanager.pages.SignupPage
import com.example.taskmanager.pages.TodoScreen
import com.example.taskmanager.pages.TodoScreen

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier,authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login", builder = {
        composable("login") {
            LoginPage(modifier,navController,authViewModel)

        }
        composable("signup") {
            SignupPage(modifier,navController,authViewModel)

        }
        composable("home") {
            HomePage(modifier,navController,authViewModel)

        }
        composable("assign") {
            AssignWorkPage(modifier,navController,authViewModel)
        }
        composable("allworks") {
            TodoScreen(modifier,navController,authViewModel)
        }


    })


}




