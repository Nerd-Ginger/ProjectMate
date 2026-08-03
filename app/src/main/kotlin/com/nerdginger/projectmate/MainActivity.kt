package com.nerdginger.projectmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nerdginger.projectmate.designsystem.ProjectMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ProjectMateApplication).container

        setContent {
            ProjectMateTheme {
                ProjectMateApp(container)
            }
        }
    }
}
