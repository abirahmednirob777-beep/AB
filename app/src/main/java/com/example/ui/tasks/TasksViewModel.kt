package com.example.ui.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TaskItem
import com.example.data.repository.TasksRepository
import com.example.ui.common.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repository: TasksRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _statusFilter = MutableStateFlow("All") // "All", "Pending", "Completed"
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow("All")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    val rawTasks: StateFlow<UiState<List<TaskItem>>> = repository.observeTasks(currentUserId)
        .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tasks", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val tasks: StateFlow<UiState<List<TaskItem>>> = combine(
        rawTasks,
        _statusFilter,
        _categoryFilter
    ) { state, status, category ->
        when (state) {
            is UiState.Success -> {
                val filtered = state.data.filter { task ->
                    val matchesStatus = when (status) {
                        "Pending" -> !task.isCompleted
                        "Completed" -> task.isCompleted
                        else -> true
                    }
                    val matchesCategory = category == "All" || task.category.equals(category, ignoreCase = true)
                    matchesStatus && matchesCategory
                }
                UiState.Success(filtered)
            }
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    fun onStatusFilterChanged(status: String) {
        _statusFilter.value = status
    }

    fun onCategoryFilterChanged(category: String) {
        _categoryFilter.value = category
    }

    fun createTask(
        title: String,
        description: String = "",
        category: String = "Research",
        priority: String = "medium",
        dueDate: String = "",
        subtasks: List<String> = emptyList(),
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val id = repository.createTask(
                    TaskItem(
                        userId = currentUserId,
                        title = title.trim(),
                        description = description.trim(),
                        category = category.ifBlank { "Research" },
                        priority = priority.lowercase(),
                        dueDate = dueDate,
                        isCompleted = false,
                        subtasks = subtasks
                    )
                )
                onSuccess(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create task", e)
                onError(e.localizedMessage ?: "Failed to create task")
            }
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            try {
                repository.toggleTaskCompleted(task.id, !task.isCompleted)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle task", e)
            }
        }
    }

    fun updateTask(
        task: TaskItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update task", e)
                onError(e.localizedMessage ?: "Failed to update task")
            }
        }
    }

    fun deleteTask(
        taskId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task", e)
                onError(e.localizedMessage ?: "Failed to delete task")
            }
        }
    }

    companion object {
        private const val TAG = "TasksViewModel"
        val PRESET_CATEGORIES = listOf(
            "All",
            "Research",
            "Reading",
            "Lab",
            "Writing",
            "Routine"
        )
    }
}
