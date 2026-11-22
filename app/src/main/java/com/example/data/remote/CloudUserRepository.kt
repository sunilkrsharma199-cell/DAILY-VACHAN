package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.DevotionalEntity
import com.example.data.model.DevotionalLanguage
import com.example.data.model.TimeSlot
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocId(rawId: String): String {
        val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        return cleaned.ifBlank { "verse_${System.currentTimeMillis()}" }
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeSavedVerses(userId: String): Flow<List<CloudSavedVerse>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val colRef = db.collection("users").document(uid).collection("saved_verses")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudSavedVerse::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        user: FirebaseUser,
        phoneNumber: String?,
        language: DevotionalLanguage,
        morningEnabled: Boolean,
        eveningEnabled: Boolean
    ): Result<Unit> {
        val docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val cleanName = (user.displayName ?: user.email?.substringBefore("@") ?: "Devotee")
                .trim()
                .take(100)
                .ifBlank { "Devotee" }
            val cleanEmail = (user.email ?: "").trim().take(200)
            val cleanPhone = phoneNumber?.trim()?.take(20)

            if (!existing.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "email" to cleanEmail,
                    "selectedLanguage" to language.code,
                    "morningEnabled" to morningEnabled,
                    "eveningEnabled" to eveningEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!cleanPhone.isNullOrBlank()) {
                    createPayload["phoneNumber"] = cleanPhone
                }
                docRef.set(createPayload).await()
            } else if (!cleanPhone.isNullOrBlank()) {
                docRef.update(
                    mapOf(
                        "phoneNumber" to cleanPhone,
                        "displayName" to cleanName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val docPath = "users/${uid ?: "unauthenticated"}"
        return try {
            val authUid = requireUserId()
            val cleanPhone = phoneNumber.trim().take(20)
            db.collection("users").document(authUid).update(
                mapOf(
                    "phoneNumber" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSyncPreferences(
        language: DevotionalLanguage,
        morningEnabled: Boolean,
        eveningEnabled: Boolean
    ): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "selectedLanguage" to language.code,
                    "morningEnabled" to morningEnabled,
                    "eveningEnabled" to eveningEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncSavedVerse(entity: DevotionalEntity, bookmarked: Boolean): Result<Unit> {
        val docId = sanitizeDocId(entity.id)
        val docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/saved_verses/$docId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("saved_verses").document(docId)
            val existing = docRef.get().await()

            val slot = TimeSlot.fromKey(entity.timeSlot).key
            val langCode = DevotionalLanguage.fromCode(entity.languageCode).code
            val cleanDate = entity.dateKey.trim().take(20).ifBlank { "2026-11-22" }
            val cleanRef = entity.verseReference.trim().take(120).ifBlank { "Scripture" }
            val cleanVerse = entity.verseText.trim().take(1000).ifBlank { "Peace be with you." }
            val cleanReflection = entity.reflection.trim().take(2500).ifBlank { "Reflect in peace." }
            val cleanPrayer = entity.shortPrayer.trim().take(1000).ifBlank { "Amen." }
            val cleanMood = entity.moodTag.trim().take(60).ifBlank { "Peace" }

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "dateKey" to cleanDate,
                    "timeSlot" to slot,
                    "languageCode" to langCode,
                    "verseReference" to cleanRef,
                    "verseText" to cleanVerse,
                    "reflection" to cleanReflection,
                    "shortPrayer" to cleanPrayer,
                    "moodTag" to cleanMood,
                    "bookmarked" to bookmarked,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "bookmarked" to bookmarked,
                        "moodTag" to cleanMood,
                        "reflection" to cleanReflection,
                        "shortPrayer" to cleanPrayer,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
