package com.example.memogotchi.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay

private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)

private const val MAX_NAME_LENGTH = 24

fun isValidName(name: String): Boolean {
    val trimmed = name.trim()
    return trimmed.isNotEmpty() && trimmed.length <= MAX_NAME_LENGTH

}

@Composable
fun NameInputScreen(onSubmit: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {isVisible = true }
    Column(
        modifier = Modifier.fillMaxSize().background(BgColor).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                initialOffsetY = { -40 },
                animationSpec = tween(800)
            ),
            exit = fadeOut(),

            ) {
            Text("WELCOME", fontSize = 32.sp, fontFamily = GildaDisplay, color = AccentGreen)
        }

        Spacer(Modifier.height(20.dp))
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                initialOffsetY = { -40 },
                animationSpec = tween(2000)
            ),
            exit = fadeOut(),

        ) {
            Text("Name your Memogotchi", fontFamily = GildaDisplay, fontSize = 16.sp, fontWeight = FontWeight.Bold, color= TextPrimary)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Give your companion a name before you get started.",
            fontFamily = Comfortaa, fontSize = 13.sp, color = TextSecondary,
            textAlign = TextAlign.Center, lineHeight = 18.sp,
        )
        Spacer(Modifier.height(24.dp))
        TextField(
            value = name,
            onValueChange = {
                if (it.length <= MAX_NAME_LENGTH) name = it
                error = null
            },
            placeholder = { Text("e.g Mochi", color = TextSecondary) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, color= TextPrimary, fontFamily = Comfortaa),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )

        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = Color(0xFFE05252), fontSize = 12.sp, fontFamily = Comfortaa)
        }
        Spacer(Modifier.height(20.dp))


        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(2000)
            ),
            exit = fadeOut(),
            ) {
            Button(
                onClick = {
                    if (isValidName(name)) onSubmit(name.trim())
                    else error = "Name must be between 1 and $MAX_NAME_LENGTH characters"
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),

                ) {
                Text("Continue", color = TextPrimary, fontSize = 16.sp, fontFamily = Comfortaa)
            }
        }

    }
}