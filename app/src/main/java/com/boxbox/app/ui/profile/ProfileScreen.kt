package com.boxbox.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

// Full 2026 F1 grid, matching the team keys in Theme.kt's teamAccentColors map.
// "Racing Bulls" replaces "RB" as the current-season name; "Audi" replaces
// "Kick Sauber"; "Cadillac" is the new 2026 entrant. Keeping this list in sync
// with teamAccentColors is what makes every option here actually change the
// app's accent color when selected.
val allTeams = listOf(
    "Red Bull Racing", "Ferrari", "McLaren", "Mercedes",
    "Aston Martin", "Alpine", "Williams", "Racing Bulls",
    "Audi", "Haas", "Cadillac"
)

@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel()) {
    val authState by vm.authState.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val profileState by vm.profile.collectAsState()

    // Keep global ThemeState in sync with the loaded profile's favourite team
    LaunchedEffect(profileState) {
        val state = profileState
        if (state is UiState.Success) {
            ThemeState.favouriteTeam = state.data.favouriteTeam
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionResult) {
        actionResult?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearActionResult()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        when (authState) {
            is AuthState.Unauthenticated -> AuthScreen(vm)
            is AuthState.Authenticated -> ProfileContent(vm)
        }
        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
            .background(AppColors.background)
    ) {
        BoxBoxTopBar(title = if (isSignUp) "CREATE ACCOUNT" else "SIGN IN")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏎️", fontSize = 48.sp)
            Text("BOXBOX", color = AppColors.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isSignUp) "Create Account" else "Sign In", fontWeight = FontWeight.Bold, color = AppColors.onPrimary)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    if (isSignUp) "Already have an account? Sign in" else "No account? Sign up",
                    color = AppColors.onSurfaceVariant
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
            .background(AppColors.background)
    ) {
        BoxBoxTopBar(title = "PROFILE") {
            IconButton(onClick = { vm.signOut() }) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sign out", tint = AppColors.onPrimary)
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
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.primary)
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
                                            color = AppColors.onPrimary,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.surface)
                                        .border(1.dp, AppColors.outline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null,
                                        tint = AppColors.onBackground, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(profile.displayName, color = AppColors.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(profile.email, color = AppColors.onSurfaceVariant, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            if (profile.favouriteTeam.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(6.dp), color = AppColors.primary.copy(alpha = 0.15f)) {
                                    Text(
                                        "${profile.favouriteTeam} fan",
                                        color = AppColors.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox("Races", profile.racesWatched.toString(), modifier = Modifier.weight(1f))
                            StatBox("Driver", profile.favouriteDriver.ifEmpty { "—" }, modifier = Modifier.weight(1f))
                            StatBox("Team", profile.favouriteTeam.take(7).ifEmpty { "—" }, modifier = Modifier.weight(1f))
                        }
                    }

                    // Appearance section
                    item { SectionLabel("Appearance") }
                    item {
                        ThemeToggleRow()
                    }
                    item {
                        TeamThemeRow(currentTeam = profile.favouriteTeam) { newTeam ->
                            vm.updateProfile(
                                profile.displayName,
                                profile.favouriteDriver,
                                newTeam,
                                profile.notificationsEnabled
                            )
                            ThemeState.favouriteTeam = newTeam
                        }
                    }

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
                            subtitle = "Permanently remove all data", iconColor = Color(0xFFFF5252)) {
                            showDeleteDialog = true
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                if (showEditDialog) {
                    EditProfileDialog(profile.displayName, profile.favouriteDriver, profile.favouriteTeam,
                        profile.notificationsEnabled,
                        onDismiss = { showEditDialog = false },
                        onSave = { name, driver, team, notif ->
                            vm.updateProfile(name, driver, team, notif)
                            ThemeState.favouriteTeam = team
                            showEditDialog = false
                        }
                    )
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Account", color = AppColors.onBackground) },
                        text = { Text("This will permanently delete your account and all data.", color = AppColors.onSurfaceVariant) },
                        confirmButton = {
                            TextButton(onClick = { vm.deleteAccount(); showDeleteDialog = false }) {
                                Text("Delete", color = Color(0xFFFF5252))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        },
                        containerColor = AppColors.surface
                    )
                }

                if (showPhotoOptions) {
                    AlertDialog(
                        onDismissRequest = { showPhotoOptions = false },
                        title = { Text("Change Photo", color = AppColors.onBackground) },
                        text = {
                            Column {
                                TextButton(onClick = {
                                    showPhotoOptions = false
                                    val file = File(context.cacheDir, "profile_photo.jpg")
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    cameraUri = uri
                                    cameraLauncher.launch(uri)
                                }) { Text("📷  Take Photo", color = AppColors.onBackground) }
                                TextButton(onClick = {
                                    showPhotoOptions = false
                                    galleryLauncher.launch("image/*")
                                }) { Text("🖼️  Choose from Gallery", color = AppColors.onBackground) }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showPhotoOptions = false }) { Text("Cancel") }
                        },
                        containerColor = AppColors.surface
                    )
                }
            }
        }
    }
}

// ---- Light/Dark theme toggle ----
@Composable
fun ThemeToggleRow() {
    var isDark by remember { mutableStateOf(ThemeState.isDarkMode) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppColors.primary.copy(alpha = 0.18f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dark Mode", color = AppColors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (isDark) "Currently dark" else "Currently light", color = AppColors.onSurfaceVariant, fontSize = 11.sp)
            }
            Switch(
                checked = isDark,
                onCheckedChange = {
                    isDark = it
                    ThemeState.isDarkMode = it
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppColors.primary,
                    checkedTrackColor = AppColors.primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = AppColors.primary,
                    uncheckedTrackColor = AppColors.primary.copy(alpha = 0.25f),
                    uncheckedBorderColor = AppColors.primary.copy(alpha = 0.4f)
                )
            )
        }
    }
}

// ---- Team theme picker - the full grid, each swatch shows its real accent color ----
@Composable
fun TeamThemeRow(currentTeam: String, onTeamSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = resolveTeamAccent(currentTeam).copy(alpha = 0.18f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(resolveTeamAccent(currentTeam))
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Team Theme", color = AppColors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        currentTeam.ifEmpty { "Default (F1 Red)" },
                        color = AppColors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AppColors.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allTeams.size) { index ->
                        val team = allTeams[index]
                        val color = resolveTeamAccent(team)
                        val isSelected = team.equals(currentTeam, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color.copy(alpha = 0.22f) else AppColors.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier.clickable { onTeamSelected(team) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    team,
                                    color = AppColors.onBackground,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = AppColors.surface, modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = AppColors.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = AppColors.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val resolvedIconColor = if (iconColor == Color.Unspecified) AppColors.onSurfaceVariant else iconColor
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = resolvedIconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = resolvedIconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AppColors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = AppColors.onSurfaceVariant, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColors.outline)
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
    var notif by remember { mutableStateOf(initialNotif) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = AppColors.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Display Name") }, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = driver, onValueChange = { driver = it },
                    label = { Text("Favourite Driver (e.g. VER)") }, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Race Notifications", color = AppColors.onBackground, modifier = Modifier.weight(1f))
                    Switch(checked = notif, onCheckedChange = { notif = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.primary,
                            checkedTrackColor = AppColors.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = AppColors.primary,
                            uncheckedTrackColor = AppColors.primary.copy(alpha = 0.25f),
                            uncheckedBorderColor = AppColors.primary.copy(alpha = 0.4f)
                        ))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, driver, initialTeam, notif) }) {
                Text("Save", color = AppColors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.onSurfaceVariant) }
        },
        containerColor = AppColors.surface
    )
}

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.primary,
    unfocusedBorderColor = AppColors.outline,
    focusedLabelColor = AppColors.primary,
    cursorColor = AppColors.primary,
    focusedTextColor = AppColors.onBackground,
    unfocusedTextColor = AppColors.onBackground
)
