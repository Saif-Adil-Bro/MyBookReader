package com.myreader.app.data.repository

import android.net.Uri
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.myreader.app.domain.model.User
import com.myreader.app.domain.model.UserRole
import com.myreader.app.domain.model.MembershipType
import com.myreader.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : AuthRepository {

    companion object {
        const val USERS_COLLECTION = "users"
    }

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val fbUser = firebaseAuth.currentUser
            if (fbUser == null) {
                trySend(null)
            } else {
                // Fetch full user profile from Firestore
                firestore.collection(USERS_COLLECTION)
                    .document(fbUser.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        trySend(doc.toObject(User::class.java) ?: fbUser.toDomainUser())
                    }
                    .addOnFailureListener {
                        trySend(fbUser.toDomainUser())
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val fbUser = result.user ?: throw Exception("Sign in failed")
        fetchOrCreateUserProfile(fbUser)
    }

    override suspend fun signUpWithEmail(
        email: String, password: String, name: String
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val fbUser = result.user ?: throw Exception("Registration failed")

        // Update display name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        fbUser.updateProfile(profileUpdates).await()

        // Send verification email
        fbUser.sendEmailVerification().await()

        // Create user profile in Firestore
        val user = User(
            uid = fbUser.uid,
            displayName = name,
            email = email,
            isEmailVerified = false,
            createdAt = Date(),
        )
        firestore.collection(USERS_COLLECTION).document(fbUser.uid)
            .set(user).await()
        user
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val fbUser = result.user ?: throw Exception("Google sign in failed")
        fetchOrCreateUserProfile(fbUser)
    }

    override suspend fun signInAsGuest(): Result<User> = runCatching {
        val result = auth.signInAnonymously().await()
        val fbUser = result.user ?: throw Exception("Guest sign in failed")
        val user = User(
            uid = fbUser.uid,
            displayName = "Guest Reader",
            isGuest = true,
            createdAt = Date(),
        )
        firestore.collection(USERS_COLLECTION).document(fbUser.uid)
            .set(user).await()
        user
    }

    override suspend fun convertGuestToEmailAccount(
        email: String, password: String, name: String
    ): Result<User> = runCatching {
        val credential = EmailAuthProvider.getCredential(email, password)
        val currentFbUser = auth.currentUser ?: throw Exception("No current user")
        currentFbUser.linkWithCredential(credential).await()

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        currentFbUser.updateProfile(profileUpdates).await()
        currentFbUser.sendEmailVerification().await()

        val user = fetchOrCreateUserProfile(currentFbUser).copy(
            displayName = name,
            email = email,
            isGuest = false,
        )
        firestore.collection(USERS_COLLECTION).document(currentFbUser.uid)
            .set(user).await()
        user
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await()
            ?: throw Exception("Not logged in")
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateProfile(name: String, photoUri: Uri?): Result<User> = runCatching {
        val fbUser = auth.currentUser ?: throw Exception("Not logged in")

        val photoUrl = photoUri?.let { uri ->
            val ref = storage.reference.child("avatars/${fbUser.uid}")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

        val builder = UserProfileChangeRequest.Builder().setDisplayName(name)
        photoUrl?.let { builder.setPhotoUri(Uri.parse(it)) }
        fbUser.updateProfile(builder.build()).await()

        val updates = mutableMapOf<String, Any>("displayName" to name)
        photoUrl?.let { updates["photoUrl"] = it }
        firestore.collection(USERS_COLLECTION).document(fbUser.uid)
            .update(updates).await()

        fetchOrCreateUserProfile(fbUser)
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val fbUser = auth.currentUser ?: throw Exception("Not logged in")
        firestore.collection(USERS_COLLECTION).document(fbUser.uid).delete().await()
        fbUser.delete().await()
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    // ── Helpers ────────────────────────────────────────────────────────────
    private suspend fun fetchOrCreateUserProfile(fbUser: FirebaseUser): User {
        return try {
            val doc = firestore.collection(USERS_COLLECTION).document(fbUser.uid).get().await()
            doc.toObject(User::class.java) ?: run {
                val newUser = fbUser.toDomainUser()
                firestore.collection(USERS_COLLECTION).document(fbUser.uid).set(newUser).await()
                newUser
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user profile")
            fbUser.toDomainUser()
        }
    }

    private fun FirebaseUser.toDomainUser() = User(
        uid = uid,
        displayName = displayName ?: "Reader",
        email = email ?: "",
        photoUrl = photoUrl?.toString() ?: "",
        isGuest = isAnonymous,
        isEmailVerified = isEmailVerified,
        createdAt = Date(),
    )
}
