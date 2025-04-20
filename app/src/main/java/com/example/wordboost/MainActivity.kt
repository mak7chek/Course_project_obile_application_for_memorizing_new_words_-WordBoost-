package com.example.wordboost

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wordboost.ui.theme.Course_project_obile_application_for_memorizing_new_words_WordBoostTheme
import com.google.firebase.FirebaseApp

import com.example.wordboost.ui.screens.TranslateScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)


        setContent {
            Course_project_obile_application_for_memorizing_new_words_WordBoostTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}
@Composable
fun MainScreen() {
    var showTranslateScreen by remember { mutableStateOf(false) }

    if (showTranslateScreen) {
        TranslateScreen()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Привіт у WordBoost!", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { showTranslateScreen = true }) {
                Text("Перекласти слово")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { /* TODO: словник */ }) {
                Text("Мій словник")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { /* TODO: практика */ }) {
                Text("Практика")
            }
        }
    }
}
