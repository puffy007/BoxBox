package com.boxbox.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.boxbox.app.data.model.UiState
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import com.boxbox.app.viewmodel.AuthState
import com.boxbox.app.viewmodel.ProfileViewModel
import java.io.File

@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel()) {
    val authState by vm.authState.collectAsState()
    val actionResult by vm.actionResult.collectAsState()

    // Show snackbar for action results
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionResult) {
        actionResult?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearActionResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = F1Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (authState) {
                is AuthState.Unauthenticated -> AuthScreen(vm)
                is AuthState.Authenticated -> ProfileContent(vm)
            }
        }
    }
}

@Composable
fun AuthScreen(vm: ProfileViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Black)
    ) {
        BoxBoxTopBar(title = if (isSignUp) "CREATE ACCOUNT" else "SIGN IN")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Text("🏎️", fontSize = 48.sp)
            Text("BOXBOX", color = F1Red, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Spacer(Modifier.height(32.dp))

            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = authFieldColors()
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = authFieldColors()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = authFieldColors()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (isSignUp) vm.signUp(email, password, displayName)
                    else vm.signIn(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = F1Red),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isSignUp) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    if (isSignUp) "Already have an account? Sign in" else "No account? Sign up",
                    color = F1LightGray
                )
            }
        }
    }
}

@Composable
fun ProfileContent(vm: ProfileViewModel) {
    val profileState by vm.profile.collectAsState()
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Camera
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { vm.uploadPhoto(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.uploadPhoto(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Black)
    ) {
        BoxBoxTopBar(title = "PROFILE") {
            IconButton(onClick = { vm.signOut() }) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sign out", tint = Color.White)
            }
        }

        when (val state = profileState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(state.message) {
                val uid = (vm.authState.value as? AuthState.Authenticated)?.uid ?: return@ErrorScreen
                vm.loadProfile(uid)
            }
            is UiState.Success -> {
                val profile = state.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar + name
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(F1Red)
                                        .clickable { showPhotoOptions = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profile.photoUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = profile.photoUrl,
                                            contentDescription = "Profile photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            profile.displayName.take(2).uppercase(),
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                // Camera icon
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(F1DarkGray)
                                        .border(1.dp, F1MidGray, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null,
                                        tint = F1White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(profile.displayName, color = F1White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(profile.email, color = F1LightGray, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            if (profile.favouriteTeam.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(6.dp), color = F1Red.copy(alpha = 0.15f)) {
                                    Text(
                                        "${profile.favouriteTeam} fan",
                                        color = F1Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Stats grid
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox("Races", profile.racesWatched.toString(), modifier = Modifier.weight(1f))
                            StatBox("Driver", profile.favouriteDriver.ifEmpty { "—" }, modifier = Modifier.weight(1f))
                            StatBox("Team", profile.favouriteTeam.take(7).ifEmpty { "—" }, modifier = Modifier.weight(1f))
                        }
                    }

                    // Settings
                    item { SectionLabel("Settings") }

                    item {
                        SettingRow(icon = Icons.Default.PhotoCamera, title = "Change photo", subtitle = "Camera or gallery") {
                            showPhotoOptions = true
                        }
                    }
                    item {
                        SettingRow(icon = Icons.Default.Edit, title = "Edit profile", subtitle = "Name, team, favourite") {
                            showEditDialog = true
                        }
                    }
                    item {
                        SettingRow(icon = Icons.Default.Notifications, title = "Notifications",
                            subtitle = if (profile.notificationsEnabled) "Enabled" else "Disabled") {}
                    }
                    item {
                        SettingRow(icon = Icons.Default.Delete, title = "Delete account",
                            subtitle = "Permanently remove all data", iconColor = F1Red) {
                            showDeleteDialog = true
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                // Edit dialog
                if (showEditDialog) {
                    EditProfileDialog(profile.displayName, profile.favouriteDriver, profile.favouriteTeam,
                        profile.notificationsEnabled,
                        onDismiss = { showEditDialog = false },
                        onSave = { name, driver, team, notif ->
                            vm.updateProfile(name, driver, team, notif)
                            showEditDialog = false
                        }
                    )
                }

                // Delete dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Account", color = F1White) },
                        text = { Text("This will permanently delete your account and all data.", color = F1LightGray) },
                        confirmButton = {
                            TextButton(onClick = { vm.deleteAccount(); showDeleteDialog = false }) {
                                Text("Delete", color = F1Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        },
                        containerColor = F1DarkGray
                    )
                }

                // Photo options
                if (showPhotoOptions) {
                    AlertDialog(
                        onDismissRequest = { showPhotoOptions = false },
                        title = { Text("Change Photo", color = F1White) },
                        text = {
                            Column {
                                TextButton(onClick = {
                                    showPhotoOptions = false
                                    val file = File(context.cacheDir, "profile_photo.jpg")
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    cameraUri = uri
                                    cameraLauncher.launch(uri)
                                }) { Text("📷  Take Photo", color = F1White) }
                                TextButton(onClick = {
                                    showPhotoOptions = false
                                    galleryLauncher.launch("image/*")
                                }) { Text("🖼️  Choose from Gallery", color = F1White) }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showPhotoOptions = false }) { Text("Cancel") }
                        },
                        containerColor = F1DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = F1DarkGray, modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = F1White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = F1LightGray, fontSize = 10.sp)
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = F1LightGray,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = F1DarkGray,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = F1White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = F1LightGray, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = F1MidGray)
        }
    }
}

@Composable
fun EditProfileDialog(
    initialName: String,
    initialDriver: String,
    initialTeam: String,
    initialNotif: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var driver by remember { mutableStateOf(initialDriver) }
    var team by remember { mutableStateOf(initialTeam) }
    var notif by remember { mutableStateOf(initialNotif) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = F1White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Display Name") }, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = driver, onValueChange = { driver = it },
                    label = { Text("Favourite Driver (e.g. VER)") }, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = team, onValueChange = { team = it },
                    label = { Text("Favourite Team") }, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Race Notifications", color = F1White, modifier = Modifier.weight(1f))
                    Switch(checked = notif, onCheckedChange = { notif = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = F1Red, checkedTrackColor = F1Red.copy(alpha = 0.3f)))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, driver, team, notif) }) {
                Text("Save", color = F1Red, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = F1LightGray) }
        },
        containerColor = F1DarkGray
    )
}

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = F1Red,
    unfocusedBorderColor = F1MidGray,
    focusedLabelColor = F1Red,
    cursorColor = F1Red,
    focusedTextColor = F1White,
    unfocusedTextColor = F1White
)
