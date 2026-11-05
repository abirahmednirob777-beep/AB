package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ResearchNote
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotesRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeNotes(userId: String): Flow<List<ResearchNote>> =
        db.collection("notes")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ResearchNote::class.java)
                    .sortedWith(
                        compareByDescending<ResearchNote> { it.isPinned }
                            .thenByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                    )
            }

    suspend fun getNote(noteId: String): ResearchNote? {
        val docRef = db.collection("notes").document(noteId)
        return try {
            val snapshot = docRef.get().await()
            snapshot.toObject(ResearchNote::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createNote(note: ResearchNote): String {
        require(note.userId.isNotBlank()) { "userId must not be blank" }
        require(note.title.isNotBlank()) { "title must not be blank" }
        val colRef = db.collection("notes")
        val docRef = if (note.id.isNotBlank()) colRef.document(note.id) else colRef.document()
        val data = hashMapOf<String, Any>(
            "userId" to note.userId,
            "title" to note.title,
            "content" to note.content,
            "topic" to note.topic,
            "tags" to note.tags,
            "date" to note.date,
            "isPinned" to note.isPinned,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateNote(note: ResearchNote) {
        require(note.id.isNotBlank()) { "Note ID must not be blank for update" }
        val docRef = db.collection("notes").document(note.id)
        val data = hashMapOf<String, Any>(
            "title" to note.title,
            "content" to note.content,
            "topic" to note.topic,
            "tags" to note.tags,
            "date" to note.date,
            "isPinned" to note.isPinned,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteNote(noteId: String) {
        val docRef = db.collection("notes").document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
