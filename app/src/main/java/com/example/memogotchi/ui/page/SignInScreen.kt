package com.example.memogotchi.ui.page

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.memogotchi.auth.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(onSignedIn: (FirebaseUser) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column {
        Button(onClick = {
            scope.launch {
                try {
                    val user = authRepository.signInWithGoogle()
                    if (user != null) onSignedIn(user)
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Sign-in failed", e)
                    throw e
                }
            }
        }) {
            Text("Sign in with Google")
        }

        errorMessage?.let { Text(it, color = Color.Red) }
    }
}