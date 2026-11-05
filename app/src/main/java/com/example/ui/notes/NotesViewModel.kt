package com.example.ui.notes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ResearchNote
import com.example.data.repository.NotesRepository
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

class NotesViewModel(
    private val repository: NotesRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTopic = MutableStateFlow("All")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    val rawNotes: StateFlow<UiState<List<ResearchNote>>> = repository.observeNotes(currentUserId)
        .map<List<ResearchNote>, UiState<List<ResearchNote>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing notes", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load research notes"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val notes: StateFlow<UiState<List<ResearchNote>>> = combine(
        rawNotes,
        _searchQuery,
        _selectedTopic
    ) { state, query, topic ->
        when (state) {
            is UiState.Success -> {
                val filtered = state.data.filter { note ->
                    val matchesQuery = query.isBlank() ||
                            note.title.contains(query, ignoreCase = true) ||
                            note.content.contains(query, ignoreCase = true) ||
                            note.tags.any { it.contains(query, ignoreCase = true) }
                    val matchesTopic = topic == "All" || note.topic.equals(topic, ignoreCase = true)
                    matchesQuery && matchesTopic
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

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onTopicSelected(topic: String) {
        _selectedTopic.value = topic
    }

    fun createNote(
        title: String,
        content: String,
        topic: String,
        tags: List<String>,
        date: String,
        isPinned: Boolean = false,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val id = repository.createNote(
                    ResearchNote(
                        userId = currentUserId,
                        title = title.trim(),
                        content = content.trim(),
                        topic = topic.ifBlank { "General" },
                        tags = tags,
                        date = date,
                        isPinned = isPinned
                    )
                )
                onSuccess(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create note", e)
                onError(e.localizedMessage ?: "Failed to create note")
            }
        }
    }

    fun updateNote(
        note: ResearchNote,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateNote(note)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update note", e)
                onError(e.localizedMessage ?: "Failed to update note")
            }
        }
    }

    fun togglePin(note: ResearchNote) {
        updateNote(note.copy(isPinned = !note.isPinned))
    }

    fun deleteNote(
        noteId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteNote(noteId)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete note", e)
                onError(e.localizedMessage ?: "Failed to delete note")
            }
        }
    }

    companion object {
        private const val TAG = "NotesViewModel"
        val PRESET_TOPICS = listOf(
            "All",
            "AI & ML",
            "Biotech",
            "Physics",
            "Systems",
            "Economics",
            "Mathematics",
            "General"
        )
    }
}
