package com.myreader.app.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

// ─── Network Monitor ──────────────────────────────────────────────────────
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { trySend(true) }
            override fun onLost(network: android.net.Network) { trySend(false) }
        }
        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        // Initial value
        trySend(cm.isCurrentlyConnected())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
        val network = activeNetwork ?: return false
        val caps = getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

// ─── Extension Functions ──────────────────────────────────────────────────
fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidPassword(): Boolean = length >= 8

fun Long.toReadableSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0  -> "%.1f GB".format(gb)
        mb >= 1.0  -> "%.1f MB".format(mb)
        kb >= 1.0  -> "%.1f KB".format(kb)
        else       -> "$this B"
    }
}

fun Long.toReadableTime(): String {
    val hours   = this / 60
    val minutes = this % 60
    return when {
        hours > 0  -> "${hours}h ${minutes}m"
        else       -> "${minutes}m"
    }
}

fun Int.toPageLabel(): String = "Page $this"

fun Float.toPercent(): String = "${(this * 100).toInt()}%"

// ─── Date formatting helpers ──────────────────────────────────────────────
import java.text.SimpleDateFormat
import java.util.*

fun Date.toRelativeString(): String {
    val now = Date()
    val diffMs = now.time - this.time
    val diffMins = diffMs / 60_000
    val diffHours = diffMins / 60
    val diffDays = diffHours / 24
    return when {
        diffMins < 1   -> "Just now"
        diffMins < 60  -> "$diffMins min ago"
        diffHours < 24 -> "$diffHours hours ago"
        diffDays < 7   -> "$diffDays days ago"
        else           -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(this)
    }
}

fun Date.toDisplayDate(): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(this)

fun Date.toDisplayDateTime(): String =
    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(this)

// ─── Resource wrapper ─────────────────────────────────────────────────────
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError   get() = this is Error
}

fun <T> Result<T>.toResource(): Resource<T> = fold(
    onSuccess = { Resource.Success(it) },
    onFailure = { Resource.Error(it.localizedMessage ?: "Unknown error", it) },
)
