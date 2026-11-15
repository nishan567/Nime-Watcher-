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

function validProfilePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    firstName: "Sakura",
    lastName: "Haruno",
    surname: "Leaf",
    displayName: "Sakura Watcher",
    email: "sakura@example.com",
    dateOfBirth: "2002-03-28",
    avatarId: "samurai_ember",
    profileCompleted: true,
    termsAccepted: true,
    preferredLanguage: "en",
    darkModeEnabled: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validGenrePreferencePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    preferredTags: ["Action", "Fantasy", "Isekai"],
    sortBy: "TRENDING_DESC",
    formatFilter: "ALL",
    onlyShowPreferred: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validDiscoveryCategoryPayload(uid, name = "Cyberpunk Worlds", queryValue = "Cyberpunk") {
  const now = new Date();
  return {
    userId: uid,
    name,
    queryValue,
    filterKind: "tag",
    accentKey: "orange",
    isPinned: true,
    sortOrder: 0,
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated caller cannot read or list users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated owner can create, update avatar & details, and delete their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      avatarId: "ninja_kage",
      firstName: "Hinata",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("Authenticated user cannot read or delete another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).delete());
  await assertFails(aliceDb.collection("users").get());
});

test("Shadow update with extra ghost field or oversized firstName is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID),
      isAdmin: true,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID),
      firstName: "A".repeat(45),
    })
  );
});

test("Owner cannot mutate userId or createdAt on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userId: BOB_UID,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(0),
      updatedAt: new Date(),
    })
  );
});

test("Owner can manage genre_preferences and discovery_categories while cross-user and unauthenticated callers are denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const prefRef = aliceDb.collection("users").doc(ALICE_UID).collection("genre_preferences").doc("default");
  const catRef = aliceDb.collection("users").doc(ALICE_UID).collection("discovery_categories").doc("cat_cyberpunk");

  await assertSucceeds(prefRef.set(validGenrePreferencePayload(ALICE_UID)));
  await assertSucceeds(prefRef.update({ preferredTags: ["Action", "Cyberpunk"], sortBy: "SCORE_DESC", updatedAt: new Date() }));
  await assertSucceeds(catRef.set(validDiscoveryCategoryPayload(ALICE_UID)));
  await assertSucceeds(catRef.update({ isPinned: false, sortOrder: 4, updatedAt: new Date() }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("discovery_categories").get());

  // Cross-user and unauthenticated access denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("genre_preferences").doc("default").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("discovery_categories").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("discovery_categories").doc("cat_cyberpunk").delete());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("genre_preferences").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("discovery_categories").get());

  // Owner can delete own category
  await assertSucceeds(catRef.delete());
});

test("Genre preferences and discovery categories reject invalid payloads, oversized arrays, and ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prefRef = aliceDb.collection("users").doc(ALICE_UID).collection("genre_preferences").doc("default");
  const catRef = aliceDb.collection("users").doc(ALICE_UID).collection("discovery_categories").doc("cat_bad");

  // Oversized preferredTags (>25)
  await assertFails(
    prefRef.set({
      ...validGenrePreferencePayload(ALICE_UID),
      preferredTags: Array.from({ length: 26 }, (_, i) => `Tag${i}`),
    })
  );

  // Invalid sortBy enum
  await assertFails(
    prefRef.set({
      ...validGenrePreferencePayload(ALICE_UID),
      sortBy: "INVALID_SORT",
    })
  );

  // Invalid filterKind or ghost field on category
  await assertFails(
    catRef.set({
      ...validDiscoveryCategoryPayload(ALICE_UID),
      filterKind: "studio",
    })
  );
  await assertFails(
    catRef.set({
      ...validDiscoveryCategoryPayload(ALICE_UID),
      extraGhost: "nope",
    })
  );
});
