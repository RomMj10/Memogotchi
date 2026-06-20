package com.example.memogotchi.ui.page

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

// ════════════════════════════════════════════════════════════════════════════
//  FILE HELPERS
// ════════════════════════════════════════════════════════════════════════════

private fun createImageFile(context: Context): File {
    val dir = File(context.filesDir, "diary_photos").apply { mkdirs() }
    return File(dir, "photo_${System.currentTimeMillis()}.jpg")
}

private fun createAudioFile(context: Context): File {
    val dir = File(context.filesDir, "diary_audio").apply { mkdirs() }
    return File(dir, "audio_${System.currentTimeMillis()}.m4a")
}

fun uriForFile(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

fun hasMicPermission(context: Context): Boolean = hasPermission(context, Manifest.permission.RECORD_AUDIO)
fun hasCameraPermission(context: Context): Boolean = hasPermission(context, Manifest.permission.CAMERA)

// ════════════════════════════════════════════════════════════════════════════
//  PERMISSION RATIONALE DIALOG
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "Allow",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1F2125),
        title = { Text(title, color = Color(0xFFE8E6F0), fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Color(0xFF888888), fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = Color(0xFF77C59D), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = Color(0xFF888888))
            }
        }
    )
}

fun openAppSettings(context: Context) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

// ════════════════════════════════════════════════════════════════════════════
//  PHOTO PICKER — camera or gallery, with permission flow
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun rememberDiaryPhotoPicker(onPhotoPicked: (String) -> Unit): DiaryPhotoPickerHandle {
    val context = LocalContext.current
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsPrompt by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraFile?.let { file -> onPhotoPicked(Uri.fromFile(file).toString()) }
        }
        pendingCameraFile = null
    }

    fun actuallyLaunchCamera() {
        val file = createImageFile(context)
        pendingCameraFile = file
        cameraLauncher.launch(uriForFile(context, file))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) actuallyLaunchCamera()
        else showSettingsPrompt = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val destFile = createImageFile(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            onPhotoPicked(Uri.fromFile(destFile).toString())
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            title = "Camera access needed",
            message = "Memogotchi needs camera access to attach a photo to your diary entry.",
            onDismiss = { showRationale = false },
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    if (showSettingsPrompt) {
        PermissionRationaleDialog(
            title = "Camera permission denied",
            message = "Camera access was denied. You can enable it anytime in your phone's app settings.",
            onDismiss = { showSettingsPrompt = false },
            onConfirm = { showSettingsPrompt = false; openAppSettings(context) },
            confirmLabel = "Open Settings",
        )
    }

    return remember {
        DiaryPhotoPickerHandle(
            launchCamera = {
                if (hasCameraPermission(context)) actuallyLaunchCamera()
                else showRationale = true
            },
            launchGallery = {
                galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        )
    }
}

data class DiaryPhotoPickerHandle(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF16171C)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text("Add a photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8E6F0))
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoSourceOption(
                    icon = Icons.Outlined.PhotoCamera, label = "Camera",
                    modifier = Modifier.weight(1f),
                    onClick = { onCameraClick(); onDismiss() }
                )
                PhotoSourceOption(
                    icon = Icons.Outlined.Image, label = "Gallery",
                    modifier = Modifier.weight(1f),
                    onClick = { onGalleryClick(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun PhotoSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F2125))
            .border(1.dp, Color(0xFF77C59D).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF77C59D), modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFFE8E6F0))
    }
}

@Composable
fun DiaryPhotoPreview(photoUri: String?, onRemove: () -> Unit) {
    if (photoUri == null) return
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = photoUri, contentDescription = "Attached photo",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun DiaryPhotoThumbnail(photoUri: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = photoUri, contentDescription = "Diary photo",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  VOICE RECORDER — with permission flow
// ════════════════════════════════════════════════════════════════════════════

class DiaryAudioRecorderState(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording by mutableStateOf(false)
    var recordedUri by mutableStateOf<String?>(null)
    var elapsedSeconds by mutableStateOf(0)

    fun start() {
        val file = createAudioFile(context)
        outputFile = file
        try {
            recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(context) else MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
        }
    }

    fun stop() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) { }
        recorder = null
        isRecording = false
        outputFile?.let { recordedUri = Uri.fromFile(it).toString() }
    }

    fun discard() {
        try { recorder?.stop(); recorder?.release() } catch (e: Exception) { }
        recorder = null
        isRecording = false
        outputFile?.delete()
        outputFile = null
        recordedUri = null
    }
}

@Composable
fun rememberDiaryAudioRecorder(): DiaryAudioRecorderState {
    val context = LocalContext.current
    return remember { DiaryAudioRecorderState(context) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderSheet(
    recorder: DiaryAudioRecorderState,
    onDismiss: () -> Unit,
    onAttach: (String) -> Unit,
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsPrompt by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recorder.elapsedSeconds = 0
            recorder.start()
        } else {
            showSettingsPrompt = true
        }
    }

    LaunchedEffect(recorder.isRecording) {
        while (recorder.isRecording) {
            kotlinx.coroutines.delay(1000)
            recorder.elapsedSeconds++
        }
    }

    ModalBottomSheet(
        onDismissRequest = { recorder.discard(); onDismiss() },
        containerColor = Color(0xFF16171C)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (recorder.recordedUri != null) "Recording ready"
                else if (recorder.isRecording) "Recording..." else "Voice note",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8E6F0)
            )
            Spacer(Modifier.height(16.dp))

            val mins = recorder.elapsedSeconds / 60
            val secs = recorder.elapsedSeconds % 60
            Text(
                String.format("%02d:%02d", mins, secs),
                fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = if (recorder.isRecording) Color(0xFFD4537E) else Color(0xFF77C59D)
            )

            Spacer(Modifier.height(20.dp))

            when {
                recorder.recordedUri != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF2A2A30))
                                .clickable { recorder.discard(); recorder.elapsedSeconds = 0 }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) { Text("Discard", fontSize = 13.sp, color = Color(0xFF888888)) }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF77C59D))
                                .clickable { recorder.recordedUri?.let { onAttach(it) }; onDismiss() }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) { Text("Attach", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16171C)) }
                    }
                }
                recorder.isRecording -> {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFD4537E))
                            .clickable { recorder.stop() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
                else -> {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF77C59D))
                            .clickable {
                                if (hasMicPermission(context)) {
                                    recorder.elapsedSeconds = 0
                                    recorder.start()
                                } else {
                                    showRationale = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Mic, contentDescription = "Record", tint = Color(0xFF16171C), modifier = Modifier.size(28.dp)) }
                }
            }
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            title = "Microphone access needed",
            message = "Memogotchi needs microphone access to record a voice note for your diary entry.",
            onDismiss = { showRationale = false },
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }

    if (showSettingsPrompt) {
        PermissionRationaleDialog(
            title = "Microphone permission denied",
            message = "Microphone access was denied. You can enable it anytime in your phone's app settings.",
            onDismiss = { showSettingsPrompt = false },
            onConfirm = { showSettingsPrompt = false; openAppSettings(context) },
            confirmLabel = "Open Settings",
        )
    }
}

@Composable
fun DiaryAudioPlayer(audioUri: String, onRemove: (() -> Unit)? = null) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(audioUri) {
        onDispose { player?.release(); player = null }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1F2125))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF77C59D))
                .clickable {
                    if (isPlaying) {
                        player?.pause()
                        isPlaying = false
                    } else {
                        if (player == null) {
                            player = MediaPlayer().apply {
                                setDataSource(context, Uri.parse(audioUri))
                                prepare()
                                setOnCompletionListener { isPlaying = false }
                            }
                        }
                        player?.start()
                        isPlaying = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color(0xFF16171C), modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("Voice note", fontSize = 12.sp, color = Color(0xFFE8E6F0), modifier = Modifier.weight(1f))
        if (onRemove != null) {
            Icon(
                Icons.Outlined.Close, contentDescription = "Remove voice note",
                tint = Color(0xFF888888), modifier = Modifier.size(16.dp).clickable(onClick = onRemove)
            )
        }
    }
}