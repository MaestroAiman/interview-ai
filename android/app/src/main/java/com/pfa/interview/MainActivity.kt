package com.pfa.interview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pfa.interview.core.navigation.InterviewNavGraph
import com.pfa.interview.ui.theme.InterviewSimulatorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InterviewSimulatorTheme {
                InterviewNavGraph()
            }
        }
    }
}
