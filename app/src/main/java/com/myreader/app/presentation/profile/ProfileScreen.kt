package com.myreader.app.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myreader.app.domain.model.*
import com.myreader.app.presentation.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onAdminClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    // Dark mode toggle
                    IconButton(onClick = viewModel::toggleDarkMode) {
                        Icon(if (state.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Theme")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Outlined.Logout, "Logout")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            // Profile Header
            item { ProfileHeader(user = state.user) }

            // Reading Stats
            item { ReadingStatsSection(stats = state.stats) }

            // Weekly Activity
            item { WeeklyActivity(weeklyMinutes = state.stats.weeklyMinutes) }

            // Reading Goals
            item { ReadingGoalsSection(user = state.user) }

            // Achievements
            if (state.achievements.isNotEmpty()) {
                item { SectionHeader("Achievements 🏆") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.achievements) { achievement ->
                            AchievementBadge(achievement = achievement)
                        }
                    }
                }
            }

            // Settings
            item { SectionHeader("Settings") }
            item { SettingsSection(onAdminClick = if (state.user?.role == UserRole.ADMIN) onAdminClick else null) }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.signOut(); onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                ) { Text("Sign Out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileHeader(user: User?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Emerald700, Emerald500)))
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // Avatar
            Box(modifier = Modifier.size(90.dp)) {
                if (user?.photoUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(White.copy(0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            user?.displayName?.firstOrNull()?.uppercase() ?: "R",
                            style = MaterialTheme.typography.displaySmall,
                            color = White,
                        )
                    }
                }
                // Edit icon
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(26.dp),
                    shape = CircleShape,
                    color = Gold500,
                ) {
                    Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.padding(4.dp), tint = White)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(user?.displayName ?: "Reader", style = MaterialTheme.typography.headlineSmall, color = White)
            Text(user?.email ?: "Guest", style = MaterialTheme.typography.bodyMedium, color = White.copy(0.8f))

            Spacer(Modifier.height(8.dp))

            // Membership badge
            val membership = user?.membershipType ?: MembershipType.FREE
            Surface(
                color = when (membership) {
                    MembershipType.PRO -> Gold500
                    MembershipType.PREMIUM -> Emerald300
                    MembershipType.FREE -> White.copy(0.2f)
                },
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    "${membership.displayName} Member",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (membership == MembershipType.FREE) White else Emerald900,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            // Streak
            if ((user?.readingStreak ?: 0) > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", style = MaterialTheme.typography.titleMedium)
                    Text(
                        " ${user?.readingStreak} day streak!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold200,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingStatsSection(stats: ReadingStats) {
    Column(modifier = Modifier.padding(16.dp)) {
        SectionHeader("Reading Stats", withPadding = false)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), Icons.Filled.MenuBook, "${stats.totalBooksRead}", "Books Read", Emerald600)
            StatCard(Modifier.weight(1f), Icons.Filled.AccessTime, "${stats.totalReadingMinutes / 60}h", "Hours Read", Gold600)
            StatCard(Modifier.weight(1f), Icons.Filled.LocalFireDepartment, "${stats.readingStreak}", "Day Streak", ErrorRed)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WeeklyActivity(weeklyMinutes: List<Int>) {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxMins = weeklyMinutes.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This Week", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                weeklyMinutes.forEachIndexed { idx, mins ->
                    val heightFraction = mins.toFloat() / maxMins
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height((80 * heightFraction).coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (heightFraction > 0.1f) Emerald500 else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(days.getOrElse(idx) { "" }, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (mins > 0) {
                            Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = Emerald600)
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ReadingGoalsSection(user: User?) {
    val dailyGoal = user?.dailyReadingGoalMinutes ?: 30
    val weeklyGoal = user?.weeklyReadingGoalBooks ?: 1

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Reading Goals", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Outlined.Edit, "Edit goals", modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            GoalRow(icon = Icons.Filled.Timer, label = "Daily goal", value = "$dailyGoal min/day", progress = 0.6f)
            Spacer(Modifier.height(8.dp))
            GoalRow(icon = Icons.Filled.Book, label = "Weekly goal", value = "$weeklyGoal books/week", progress = 0.3f)
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun GoalRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Emerald600, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.bodySmall)
                Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = Emerald600,
            )
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (achievement.isUnlocked) Gold100 else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (achievement.isUnlocked) Gold500 else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.EmojiEvents, null,
                modifier = Modifier.size(32.dp),
                tint = if (achievement.isUnlocked) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            achievement.title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private val Gold100 = Color(0xFFFEF9C3)

@Composable
private fun SettingsSection(onAdminClick: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            SettingsItem(Icons.Outlined.Notifications, "Notifications") {}
            HorizontalDivider()
            SettingsItem(Icons.Outlined.Language, "Language") {}
            HorizontalDivider()
            SettingsItem(Icons.Outlined.CloudSync, "Sync & Backup") {}
            if (onAdminClick != null) {
                HorizontalDivider()
                SettingsItem(Icons.Outlined.AdminPanelSettings, "Admin Panel", tint = Emerald600, onClick = onAdminClick)
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String, withPadding: Boolean = true) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(
            horizontal = if (withPadding) 16.dp else 0.dp,
            vertical = if (withPadding) 12.dp else 0.dp,
        )
    )
}
