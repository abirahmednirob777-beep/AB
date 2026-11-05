const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- NOTES TESTS ---

test("Unauthenticated user: cannot read notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("notes").get());
});

test("Authenticated user: can create valid research note", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Quantum Computing Principles",
      content: "Exploring error correction thresholds and surface codes.",
      topic: "Quantum Physics",
      tags: ["quantum", "physics", "error-correction"],
      date: "2026-11-05",
      isPinned: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot spoof another user's ID on note creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("notes").doc("note_spoof").set({
      userId: BOB_UID, // Alice trying to spoof Bob
      title: "Spoofed Note",
      content: "Content",
      topic: "General",
      tags: [],
      date: "2026-11-05",
      isPinned: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read another user's note", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("notes").doc("bob_note").set({
      userId: BOB_UID,
      title: "Bob's Research",
      content: "Confidential Findings",
      topic: "Biotech",
      tags: [],
      date: "2026-11-05",
      isPinned: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("notes").doc("bob_note").get());
});

test("Authenticated user: can query their own notes with matching filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("notes").doc("alice_note").set({
      userId: ALICE_UID,
      title: "Alice's Note",
      content: "Content",
      topic: "CS",
      tags: [],
      date: "2026-11-05",
      isPinned: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("notes").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails query without userId filter (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("notes").get());
});

// --- TASKS TESTS ---

test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
});

test("Authenticated user: can create valid task item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Read arXiv:2603.12345",
      description: "Analyze experimental results section",
      category: "Reading",
      priority: "high",
      dueDate: "2026-11-06",
      isCompleted: false,
      subtasks: ["Download PDF", "Check benchmark table"],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read or update another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("bob_task").set({
      userId: BOB_UID,
      title: "Bob's Task",
      category: "Lab",
      priority: "medium",
      dueDate: "2026-11-07",
      isCompleted: false,
      subtasks: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("bob_task").get());
  await assertFails(
    aliceDb.collection("tasks").doc("bob_task").update({ isCompleted: true })
  );
});
