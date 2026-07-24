package com.example.memogotchi.ui.page

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskVerificationSheet(
    activeTask: ActiveTaskTimer,
    onDismiss: () -> Unit,
    onVerified: (VerificationMethod) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showQuickConfirm by remember { mutableStateOf(false) }
    
    // Reuse the photo picker logic from Diarymediapicker
    val photoPicker = rememberDiaryPhotoPicker { uri ->
        scope.launch {
            isVerifying = true
            errorMessage = null
            try {
                val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
                if (bytes != null) {
                    val task = TaskStore.loadTasksForDate(context, activeTask.dateKey)?.find { it.id == activeTask.taskId }
                    if (task != null) {
                        val success = verifyTaskPhoto(bytes, task)
                        if (success) {
                            onVerified(VerificationMethod.PHOTO)
                        } else {
                            errorMessage = "Hmm, I couldn't verify that. Try another photo?"
                        }
                    } else {
                        errorMessage = "Task not found."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Verification failed. Check your connection."
            } finally {
                isVerifying = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.current.bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (showQuickConfirm) "Quick Confirm" else "Verify Task",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.current.textPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                activeTask.taskTitle,
                fontSize = 16.sp,
                color = AppTheme.current.accent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(24.dp))

            if (isVerifying) {
                CircularProgressIndicator(color = AppTheme.current.accent)
                Spacer(Modifier.height(16.dp))
                Text("Analyzing photo...", color = AppTheme.current.textSecondary)
            } else if (showQuickConfirm) {
                Text(
                    "Did you complete this task fully without your phone?",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = AppTheme.current.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showQuickConfirm = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.current.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back", color = AppTheme.current.textPrimary)
                    }
                    Button(
                        onClick = { onVerified(VerificationMethod.PREDEFINED) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.current.accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Yes, I'm done!", color = AppTheme.current.bg)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    VerificationOption(
                        icon = Icons.Outlined.PhotoCamera,
                        label = "Take a photo",
                        description = "Use AI to verify",
                        modifier = Modifier.weight(1f),
                        onClick = { photoPicker.launchCamera() }
                    )
                    VerificationOption(
                        icon = Icons.Outlined.CheckCircle,
                        label = "Quick confirm",
                        description = "Self-attestation",
                        modifier = Modifier.weight(1f),
                        onClick = { showQuickConfirm = true }
                    )
                }

                errorMessage?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun VerificationOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.current.surface)
            .border(1.dp, AppTheme.current.accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = AppTheme.current.accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.current.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(description, fontSize = 11.sp, color = AppTheme.current.textSecondary)
    }
}
