package com.myreader.app.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myreader.app.domain.model.ReadingStats
import com.myreader.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingAnalyticsScreen(
    stats: ReadingStats,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Analytics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Summary cards ─────────────────────────────────────────────
            Text("Overall Stats", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(Modifier.weight(1f), "📚", "${stats.totalBooksRead}", "Books Read", Emerald600)
                SummaryCard(Modifier.weight(1f), "⏱️", "${stats.totalReadingMinutes / 60}h", "Total Hours", Gold600)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(Modifier.weight(1f), "🔥", "${stats.readingStreak}", "Current Streak", ErrorRed)
                SummaryCard(Modifier.weight(1f), "🏆", "${stats.longestStreak}", "Best Streak", InfoBlue)
            }

            // ── This week / month ─────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(Modifier.weight(1f), "📖", "${stats.booksThisWeek}", "Books This Week", Emerald500)
                SummaryCard(Modifier.weight(1f), "📅", "${stats.booksThisMonth}", "Books This Month", Gold500)
            }

            // ── Weekly bar chart ──────────────────────────────────────────
            WeeklyBarChart(weeklyMinutes = stats.weeklyMinutes)

            // ── Monthly books chart ───────────────────────────────────────
            MonthlyBooksChart(monthlyBooks = stats.monthlyBooks)

            // ── Favorite category ─────────────────────────────────────────
            stats.favoriteCategory?.let { cat ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("❤️", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Favourite Category", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(cat.displayNameEn, style = MaterialTheme.typography.titleMedium,
                                color = Emerald600)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    emoji: String,
    value: String,
    label: String,
    valueColor: Color,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WeeklyBarChart(weeklyMinutes: List<Int>) {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val max = weeklyMinutes.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Weekly Reading", style = MaterialTheme.typography.titleSmall)
                Text("minutes", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                weeklyMinutes.forEachIndexed { idx, mins ->
                    val fraction = mins.toFloat() / max
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        if (mins > 0) {
                            Text("${mins}m", style = MaterialTheme.typography.labelSmall,
                                color = Emerald600)
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (mins > 0) Brush.verticalGradient(listOf(Emerald400, Emerald600))
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(days.getOrElse(idx) { "" }, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Total this week
            val totalMins = weeklyMinutes.sum()
            Text(
                "Total this week: ${totalMins / 60}h ${totalMins % 60}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun MonthlyBooksChart(monthlyBooks: List<Int>) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val max = monthlyBooks.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Books per Month", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                monthlyBooks.forEachIndexed { idx, count ->
                    val fraction = count.toFloat() / max
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    if (count > 0) Gold500
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(months.getOrElse(idx) { "" }.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun Brush.Companion.verticalGradient(colors: List<Color>): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(colors)
