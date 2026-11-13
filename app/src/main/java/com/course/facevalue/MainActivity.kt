package com.course.facevalue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.course.facevalue.ui.FaceValueRoot
import com.course.facevalue.ui.theme.Cream
import com.course.facevalue.ui.theme.FaceValueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceValueTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Cream)) {
                    FaceValueRoot()
                }
            }
        }
    }
}
