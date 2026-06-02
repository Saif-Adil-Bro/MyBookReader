package com.myreader.app.presentation.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myreader.app.presentation.theme.*

// ─── Offline Banner ───────────────────────────────────────────────────────
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WarningAmber,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.WifiOff, null,
                tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "You're offline. Some features may not be available.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

// ─── Loading Overlay ──────────────────────────────────────────────────────
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String = "Loading...",
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = Emerald600)
                    Spacer(Modifier.height(12.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ─── Star Rating Bar ──────────────────────────────────────────────────────
@Composable
fun StarRatingBar(
    rating: Float,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 20.dp,
    interactive: Boolean = false,
    onRatingChange: ((Float) -> Unit)? = null,
) {
    Row(modifier = modifier) {
        repeat(maxStars) { idx ->
            val filled = idx < rating.toInt()
            val halfFilled = !filled && idx < rating
            Icon(
                imageVector = when {
                    filled     -> Icons.Filled.Star
                    halfFilled -> Icons.Filled.StarHalf
                    else       -> Icons.Outlined.StarBorder
                },
                contentDescription = "${idx + 1} star",
                tint = if (filled || halfFilled) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (interactive && onRatingChange != null)
                            Modifier.clickable { onRatingChange((idx + 1).toFloat()) }
                        else Modifier
                    ),
            )
        }
    }
}

// ─── Interactive Rating Dialog ────────────────────────────────────────────
@Composable
fun RatingDialog(
    currentRating: Float = 0f,
    onDismiss: () -> Unit,
    onSubmit: (Float) -> Unit,
) {
    var selectedRating by remember { mutableFloatStateOf(currentRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this Book") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    when (selectedRating.toInt()) {
                        1 -> "😞 Poor"
                        2 -> "😐 Fair"
                        3 -> "🙂 Good"
                        4 -> "😊 Very Good"
                        5 -> "🤩 Excellent!"
                        else -> "Tap a star to rate"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                StarRatingBar(
                    rating = selectedRating,
                    starSize = 36.dp,
                    interactive = true,
                    onRatingChange = { selectedRating = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating); onDismiss() },
                enabled = selectedRating > 0,
            ) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ─── Shimmer Loading Cards ────────────────────────────────────────────────
@Composable
fun ShimmerBookCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(190.dp))
            Column(modifier = Modifier.padding(8.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp))
                Spacer(Modifier.height(4.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp))
            }
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant,
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

// ─── Snackbar Host ────────────────────────────────────────────────────────
@Composable
fun MyReaderSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = Emerald400,
                shape = RoundedCornerShape(12.dp),
            )
        }
    )
}

// ─── Section divider with title ───────────────────────────────────────────
@Composable
fun SectionDivider(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "  $title  ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

// ─── Confirmation Dialog ──────────────────────────────────────────────────
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive)
                    ButtonDefaults.buttonColors(containerColor = ErrorRed)
                else ButtonDefaults.buttonColors(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

// ─── Info Row (label + value) ─────────────────────────────────────────────
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Gradient Button ──────────────────────────────────────────────────────
@Composable
fun GradientButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled)
                    Brush.horizontalGradient(listOf(Emerald600, Emerald500))
                else
                    Brush.horizontalGradient(listOf(
                        MaterialTheme.colorScheme.onSurface.copy(0.12f),
                        MaterialTheme.colorScheme.onSurface.copy(0.12f),
                    )),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = White,
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                icon?.let {
                    Icon(it, null, tint = White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) White else MaterialTheme.colorScheme.onSurface.copy(0.38f),
                )
            }
        }
    }
}

private val Brush.Companion.horizontalGradient: (List<Color>) -> androidx.compose.ui.graphics.Brush
    get() = { colors -> androidx.compose.ui.graphics.Brush.horizontalGradient(colors) }
