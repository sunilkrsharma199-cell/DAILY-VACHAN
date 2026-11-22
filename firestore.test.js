const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated context: cannot read or write users or saved_verses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_verses")
      .get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      selectedLanguage: "en",
      morningEnabled: true,
      eveningEnabled: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner UserProfile: create, partial phone/language update, and immutability guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sunil Sharma",
      email: "sunil@example.com",
      phoneNumber: "+919876543210",
      selectedLanguage: "hinglish",
      morningEnabled: true,
      eveningEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update of phone number and language succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phoneNumber: "+919999988888",
      selectedLanguage: "hi",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid language code
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedLanguage: "xx",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sunil",
      selectedLanguage: "en",
      morningEnabled: true,
      eveningEnabled: true,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's profile or saved_verses", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = new Date(Date.now() - 5000);
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      phoneNumber: "+919800000000",
      selectedLanguage: "en",
      morningEnabled: true,
      eveningEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_verses")
      .doc("v_1")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-22",
        timeSlot: "MORNING",
        languageCode: "en",
        verseReference: "Psalm 23:1",
        verseText: "The Lord is my shepherd.",
        reflection: "Peaceful reflection.",
        shortPrayer: "Amen.",
        moodTag: "Peace",
        bookmarked: true,
        createdAt: ts,
        updatedAt: ts,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .update({ phoneNumber: "+910000000000" })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_verses")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_verses")
      .doc("v_1")
      .delete()
  );
});

test("Owner saved_verses: create, list, bookmark update, and validation bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const col = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_verses");

  // List on empty subcollection must succeed
  await assertSucceeds(col.get());

  // Valid create (no @DocumentId `id` field)
  await assertSucceeds(
    col.doc("2026-11-22_MORNING_en").set({
      userId: ALICE_UID,
      dateKey: "2026-11-22",
      timeSlot: "MORNING",
      languageCode: "en",
      verseReference: "Psalm 23:1",
      verseText: "The Lord is my shepherd; I shall not want.",
      reflection: "Trust God gently today.",
      shortPrayer: "Lord, guide me. Amen.",
      moodTag: "Peace",
      bookmarked: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List with documents must succeed
  await assertSucceeds(col.get());

  // Partial update toggling bookmarked succeeds
  await assertSucceeds(
    col.doc("2026-11-22_MORNING_en").update({
      bookmarked: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject payload containing redundant `@DocumentId` `id` key
  await assertFails(
    col.doc("bad_doc_with_id").set({
      id: "bad_doc_with_id",
      userId: ALICE_UID,
      dateKey: "2026-11-22",
      timeSlot: "MORNING",
      languageCode: "en",
      verseReference: "Psalm 23:1",
      verseText: "Text",
      reflection: "Reflection",
      shortPrayer: "Amen",
      moodTag: "Peace",
      bookmarked: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    col.doc("future_ts_doc").set({
      userId: ALICE_UID,
      dateKey: "2026-11-22",
      timeSlot: "MORNING",
      languageCode: "en",
      verseReference: "Psalm 23:1",
      verseText: "Text",
      reflection: "Reflection",
      shortPrayer: "Amen",
      moodTag: "Peace",
      bookmarked: true,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
