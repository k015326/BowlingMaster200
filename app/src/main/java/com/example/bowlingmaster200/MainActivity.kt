package com.example.bowlingmaster200

import com.example.bowlingmaster200.ocr.service.OcrServiceFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OcrServiceFactory.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            BowlingMasterApp()
        }
    }
}
