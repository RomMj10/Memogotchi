package com.example.memogotchi.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun TasksScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF16171C)),
        contentAlignment = Alignment.Center
    ) {
        Text("Daily Tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

