package com.example.taskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.view.animation.AnimationUtils
import android.widget.Button
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.taskmanager.ui.theme.TaskManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.home)


        val buttonflicker = findViewById<Button>(R.id.button_flicker)

        // Load the fade animation
        val fadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade)

        // Start flickering animation immediately when the screen opens
        buttonflicker.startAnimation(fadeAnimation)


        buttonflicker.setOnClickListener {
            val intent = Intent(this, logInActivity::class.java)
            startActivity(intent)

        }


    }
}





