package com.example.wordboost.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.wordboost.R

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
) {
    companion object {
        private val defaultIconSizeModifier = Modifier.size(24.dp)
    }

    object Home : BottomNavItem(
        route = "home",
        label = "Головна",
        icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Головна", modifier = defaultIconSizeModifier) }
    )

    object Sets : BottomNavItem(
        route = "sets",
        label = "Набори",
        icon = { Icon(painter = painterResource(id = R.drawable.archive_svgrepo_com), contentDescription = "Набори", modifier = defaultIconSizeModifier) }
    )

//    object Articles : BottomNavItem(
//        route = "articles",
//        label = "Статті",
//        icon = { Icon(painter = painterResource(id = R.drawable.open_book_svgrepo_com), contentDescription = "Статті", modifier = defaultIconSizeModifier) }
//    )
}