package com.example.wordboost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wordboost.ui.theme.Course_project_obile_application_for_memorizing_new_words_WordBoostTheme
import com.google.firebase.FirebaseApp


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

@Preview(
    showBackground =true)
@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Привіт у WordBoost!", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { /* TODO: перехід до перекладу */ }) {
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
