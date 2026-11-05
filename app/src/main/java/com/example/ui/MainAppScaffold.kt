package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

import com.example.R
import com.example.data.model.ResearchNote
import com.example.data.model.TaskItem
import com.example.data.repository.NotesRepository
import com.example.data.repository.TasksRepository
import com.example.ui.auth.signOut
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.notes.EditNoteDialog
import com.example.ui.notes.NotesScreen
import com.example.ui.notes.NotesViewModel
import com.example.ui.tasks.EditTaskDialog
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val userEmail = remember { Firebase.auth.currentUser?.email }

    // Repositories configured with named firestore_database_id from string resources
    val databaseId = remember { context.getString(R.string.firestore_database_id) }
    val firestore = remember { FirebaseFirestore.getInstance(databaseId) }

    val notesRepo = remember { NotesRepository(firestore) }
    val tasksRepo = remember { TasksRepository(firestore) }

    val notesViewModel: NotesViewModel = viewModel(
        key = "notes_$currentUserId",
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer {
                NotesViewModel(notesRepo, currentUserId)
            }
        }
    )

    val tasksViewModel: TasksViewModel = viewModel(
        key = "tasks_$currentUserId",
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer {
                TasksViewModel(tasksRepo, currentUserId)
            }
        }
    )

    val notesState by notesViewModel.notes.collectAsStateWithLifecycle()
    val rawNotesState by notesViewModel.rawNotes.collectAsStateWithLifecycle()
    val searchQuery by notesViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTopic by notesViewModel.selectedTopic.collectAsStateWithLifecycle()

    val tasksState by tasksViewModel.tasks.collectAsStateWithLifecycle()
    val rawTasksState by tasksViewModel.rawTasks.collectAsStateWithLifecycle()
    val statusFilter by tasksViewModel.statusFilter.collectAsStateWithLifecycle()
    val categoryFilter by tasksViewModel.categoryFilter.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Notes, 2: Tasks

    // BackHandler: returns to Dashboard if in other tabs
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var activeNoteForEdit by remember { mutableStateOf<ResearchNote?>(null) }
    var showNewNoteDialog by remember { mutableStateOf(false) }

    var activeTaskForEdit by remember { mutableStateOf<TaskItem?>(null) }
    var showNewTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.abfs_app_icon_1793891303015),
                            contentDescription = "ABFS Icon",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ABFS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cloud Synced",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                    label = { Text("Research") },
                    modifier = Modifier.testTag("nav_notes")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Tasks") },
                    label = { Text("Tasks") },
                    modifier = Modifier.testTag("nav_tasks")
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showNewNoteDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("New Note") },
                        modifier = Modifier.testTag("fab_dashboard_new_note")
                    )
                }
                1 -> {
                    FloatingActionButton(
                        onClick = { showNewNoteDialog = true },
                        modifier = Modifier.testTag("fab_add_note")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Research Note")
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = { showNewTaskDialog = true },
                        modifier = Modifier.testTag("fab_add_task")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    notesState = rawNotesState,
                    tasksState = rawTasksState,
                    onNewNoteClick = { showNewNoteDialog = true },
                    onNewTaskClick = { showNewTaskDialog = true },
                    onNoteClick = { note -> activeNoteForEdit = note },
                    onTaskToggle = { task -> tasksViewModel.toggleTask(task) },
                    onNavigateToNotes = { selectedTab = 1 },
                    onNavigateToTasks = { selectedTab = 2 },
                    userEmail = userEmail
                )
                1 -> NotesScreen(
                    notesState = notesState,
                    searchQuery = searchQuery,
                    selectedTopic = selectedTopic,
                    onSearchChange = { notesViewModel.onSearchQueryChanged(it) },
                    onTopicSelect = { notesViewModel.onTopicSelected(it) },
                    onNoteClick = { note -> activeNoteForEdit = note },
                    onPinToggle = { note -> notesViewModel.togglePin(note) },
                    onDeleteNote = { noteId -> notesViewModel.deleteNote(noteId) }
                )
                2 -> TasksScreen(
                    tasksState = tasksState,
                    statusFilter = statusFilter,
                    categoryFilter = categoryFilter,
                    onStatusChange = { tasksViewModel.onStatusFilterChanged(it) },
                    onCategoryChange = { tasksViewModel.onCategoryFilterChanged(it) },
                    onTaskToggle = { task -> tasksViewModel.toggleTask(task) },
                    onEditTask = { task -> activeTaskForEdit = task },
                    onDeleteTask = { taskId -> tasksViewModel.deleteTask(taskId) }
                )
            }
        }
    }

    // New / Edit Note Dialog
    if (showNewNoteDialog || activeNoteForEdit != null) {
        val editingNote = activeNoteForEdit
        EditNoteDialog(
            note = editingNote,
            onDismiss = {
                showNewNoteDialog = false
                activeNoteForEdit = null
            },
            onSave = { title, content, topic, tags, date, isPinned ->
                if (editingNote == null) {
                    notesViewModel.createNote(title, content, topic, tags, date, isPinned)
                } else {
                    notesViewModel.updateNote(
                        editingNote.copy(
                            title = title,
                            content = content,
                            topic = topic,
                            tags = tags,
                            date = date,
                            isPinned = isPinned
                        )
                    )
                }
                showNewNoteDialog = false
                activeNoteForEdit = null
            }
        )
    }

    // New / Edit Task Dialog
    if (showNewTaskDialog || activeTaskForEdit != null) {
        val editingTask = activeTaskForEdit
        EditTaskDialog(
            task = editingTask,
            onDismiss = {
                showNewTaskDialog = false
                activeTaskForEdit = null
            },
            onSave = { title, description, category, priority, dueDate, subtasks ->
                if (editingTask == null) {
                    tasksViewModel.createTask(title, description, category, priority, dueDate, subtasks)
                } else {
                    tasksViewModel.updateTask(
                        editingTask.copy(
                            title = title,
                            description = description,
                            category = category,
                            priority = priority,
                            dueDate = dueDate,
                            subtasks = subtasks
                        )
                    )
                }
                showNewTaskDialog = false
                activeTaskForEdit = null
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out from $userEmail?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        signOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = onSignedOut,
                            scope = scope
                        )
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
