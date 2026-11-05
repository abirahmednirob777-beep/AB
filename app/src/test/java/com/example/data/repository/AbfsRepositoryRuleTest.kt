package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ResearchNote
import com.example.data.model.TaskItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AbfsRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveNotes_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val notesRepo = NotesRepository(firestore)

    val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
      notesRepo.createNote(
        ResearchNote(
          userId = uid,
          title = "Quantum Decoherence Research",
          content = "Measuring noise rates on superconductor qubits.",
          topic = "Quantum Physics",
          tags = listOf("quantum", "superconductor"),
          date = "2026-11-05",
          isPinned = true
        )
      )
    }
    assertTrue(noteId.isNotEmpty())

    val notes = withTimeout(FLOW_TIMEOUT_MS) {
      notesRepo.observeNotes(uid).first { list -> list.any { it.id == noteId } }
    }
    assertTrue(notes.any { it.id == noteId && it.title == "Quantum Decoherence Research" })
  }

  @Test
  fun getNote_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = NotesRepository(firestore)
    val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createNote(
        ResearchNote(
          userId = aliceUid,
          title = "Alice's Secret Finding",
          content = "Breakthrough synthesis procedure.",
          topic = "Chemistry",
          tags = listOf("experiment"),
          date = "2026-11-05",
          isPinned = false
        )
      )
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = NotesRepository(firestore)
    var failedWithPermissionDenied = false
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.getNote(noteId)
      }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        failedWithPermissionDenied = true
      }
    } catch (e: Exception) {
      val root = e.unwrapFirestoreException()
      if (root?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        failedWithPermissionDenied = true
      }
    }
    assertTrue("Cross-user read should fail with PERMISSION_DENIED", failedWithPermissionDenied)
  }

  @Test
  fun createAndToggleTask_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val tasksRepo = TasksRepository(firestore)

    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      tasksRepo.createTask(
        TaskItem(
          userId = uid,
          title = "Prepare laboratory sample",
          description = "Use 50ml buffer solution",
          category = "Lab",
          priority = "high",
          dueDate = "2026-11-06",
          isCompleted = false,
          subtasks = listOf("Measure reagents", "Calibrate pH meter")
        )
      )
    }
    assertTrue(taskId.isNotEmpty())

    val tasks = withTimeout(FLOW_TIMEOUT_MS) {
      tasksRepo.observeTasks(uid).first { list -> list.any { it.id == taskId } }
    }
    assertTrue(tasks.any { it.id == taskId })

    withTimeout(DEFAULT_TIMEOUT_MS) {
      tasksRepo.toggleTaskCompleted(taskId, true)
    }
  }

  @Test
  fun observeNotes_unauthenticated_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val notesRepo = NotesRepository(firestore)

    var rootException: FirebaseFirestoreException? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        notesRepo.observeNotes("any_user").first()
      }
    } catch (e: Exception) {
      rootException = e.unwrapFirestoreException()
    }
    assertNotNull("Expected root FirebaseFirestoreException", rootException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, rootException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@example.com"
    const val BOB_EMAIL = "bob@example.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
