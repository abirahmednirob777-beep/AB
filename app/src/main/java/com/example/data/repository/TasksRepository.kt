package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.TaskItem
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TasksRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeTasks(userId: String): Flow<List<TaskItem>> =
        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TaskItem::class.java)
                    .sortedWith(
                        compareBy<TaskItem> { it.isCompleted }
                            .thenByDescending { it.priority == "high" }
                            .thenByDescending { it.priority == "medium" }
                            .thenBy { it.dueDate }
                    )
            }

    suspend fun createTask(task: TaskItem): String {
        require(task.userId.isNotBlank()) { "userId must not be blank" }
        require(task.title.isNotBlank()) { "title must not be blank" }
        val colRef = db.collection("tasks")
        val docRef = if (task.id.isNotBlank()) colRef.document(task.id) else colRef.document()
        val data = hashMapOf<String, Any>(
            "userId" to task.userId,
            "title" to task.title,
            "category" to task.category,
            "priority" to task.priority,
            "dueDate" to task.dueDate,
            "isCompleted" to task.isCompleted,
            "subtasks" to task.subtasks,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (task.description.isNotBlank()) {
            data["description"] = task.description
        }
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

    suspend fun updateTask(task: TaskItem) {
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        val docRef = db.collection("tasks").document(task.id)
        val data = hashMapOf<String, Any>(
            "title" to task.title,
            "category" to task.category,
            "priority" to task.priority,
            "dueDate" to task.dueDate,
            "isCompleted" to task.isCompleted,
            "subtasks" to task.subtasks,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (task.description.isNotBlank()) {
            data["description"] = task.description
        }
        try {
            docRef.update(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleTaskCompleted(taskId: String, isCompleted: Boolean) {
        val docRef = db.collection("tasks").document(taskId)
        try {
            docRef.update(
                mapOf(
                    "isCompleted" to isCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String) {
        val docRef = db.collection("tasks").document(taskId)
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
