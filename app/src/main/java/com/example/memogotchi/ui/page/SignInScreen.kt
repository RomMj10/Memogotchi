package com.example.memogotchi.ui.page

import com.example.memogotchi.R
import com.airbnb.lottie.compose.LottieConstants
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.memogotchi.auth.AuthRepository
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

private val BgColor       = Color(0xFFEFEDE9)
private val SurfaceColor  = Color(0xFFFFFBF8)
private val Accent   = Color(0xFF77C59D)
private val TextPrimary   = Color(0xFF1F1E1D)
private val TextSecondary = Color(0xFF343230)

@Composable
fun SignInScreen(onSignedIn: (FirebaseUser) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {isVisible = true }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.pet_happy))

    Column(
       modifier = Modifier.fillMaxSize()
           .background(BgColor)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = tween(2000)
                ),
                exit = fadeOut(),

                ) {
                Text(
                    "Let's get started",
                    fontSize = 32.sp,
                    fontFamily = GildaDisplay,
                    color = TextPrimary
                )
            }
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(3000)),
                exit = fadeOut(),
                ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Accent.copy(alpha = 0.24f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxWidth()
                            .scale(1.2f)
                    )
                }
            }
            Text("Log-in to your account", fontSize = 16.sp, fontFamily = GildaDisplay, color = TextSecondary)
            Spacer(Modifier.height(24.dp))
            Button(
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                onClick = {
                    scope.launch {
                        try {
                            val user = authRepository.signInWithGoogle()
                            if (user != null) onSignedIn(user)
                        } catch (e: Exception) {
                            Log.e("GoogleSignIn", "Sign-in failed", e)
                            throw e
                        }
                    }
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    Text("Sign in with Google", fontFamily = Comfortaa, color = TextPrimary)
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_icon),
                            contentDescription = "Google Icon",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Unspecified
                        )
                    }

                }
            }
        }


        errorMessage?.let { Text(it, color = Color.Red) }
    }
}