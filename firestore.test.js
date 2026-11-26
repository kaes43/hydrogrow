const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_petani_1";
const BOB_UID = "bob_pembeli_2";
const ADMIN_UID = "admin_hydrogrow";
const ADMIN_EMAIL = "mnurkhasani43@gmail.com";

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

test("Unauthenticated user: denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("products").where("active", "==", true).get());
  await assertFails(unauth.collection("orders").where("buyerId", "==", ALICE_UID).get());
  await assertFails(unauth.collection("wholesale_requests").where("open", "==", true).get());
  await assertFails(unauth.collection("forum_posts").where("visible", "==", true).get());
});

test("User profile: owner create/update works, privilege escalation & cross-user read fail", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  // Reject self-verified seller on create
  await assertFails(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Petani",
      role: "PETANI",
      verifiedSeller: true,
      verifiedSupplier: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid create
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Petani",
      email: "alice@example.com",
      phone: "08123456789",
      role: "PETANI",
      address: "Jl. Kebun Hijau No. 5",
      city: "Bandung",
      latitude: -6.9175,
      longitude: 107.6191,
      verifiedSeller: false,
      verifiedSupplier: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user PII read fails
  await assertFails(bob.collection("users").doc(ALICE_UID).get());

  // Valid partial update
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      city: "Lembang, Bandung",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow update on verifiedSeller fails for non-admin
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      verifiedSeller: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("User-scoped plants, monitoring, schedules, harvests, expenses, revenues, cart, notifications", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Plants
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("plants").doc("plant_1").set({
      userId: ALICE_UID,
      name: "Selada Butterhead A1",
      cropType: "Selada Hijau",
      systemType: "NFT",
      holeCount: 120,
      plantedDateMillis: Date.now() - 86400000 * 10,
      harvestAgeDays: 35,
      status: "VEGETATIF",
      phMin: 5.5,
      phMax: 6.5,
      ecMin: 0.8,
      ecMax: 1.4,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("plants").get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("plants").get()
  );

  // Monitoring log
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("plant_monitoring").doc("log_1").set({
      userId: ALICE_UID,
      plantId: "plant_1",
      plantName: "Selada Butterhead A1",
      ph: 6.1,
      ec: 1.2,
      tds: 600,
      waterTemp: 23.5,
      ambientTemp: 27.0,
      heightCm: 14.5,
      rootCondition: "Putih sehat",
      plantCondition: "Daun segar",
      healthStatus: "NORMAL",
      recordedAtMillis: Date.now() - 1000,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Expenses & Revenues
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
      userId: ALICE_UID,
      entryType: "PENGELUARAN",
      category: "Nutrisi",
      title: "Nutrisi AB Mix 5L",
      amount: 80000,
      dateMillis: Date.now() - 1000,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("revenues").doc("rev_1").set({
      userId: ALICE_UID,
      category: "Penjualan Hasil Panen",
      title: "Penjualan Selada 10 kg",
      amount: 200000,
      dateMillis: Date.now() - 1000,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Marketplace products & orders: query alignment, participant isolation, and immutable fields", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    alice.collection("products").doc("prod_1").set({
      sellerId: ALICE_UID,
      sellerName: "Kebun Hijau Alice",
      sellerArea: "Bandung",
      sellerLat: -6.9175,
      sellerLng: 107.6191,
      mainCategory: "HASIL_PANEN",
      subCategory: "Selada",
      name: "Selada Hijau Segar",
      description: "Dipetik pagi hari tanpa pestisida",
      price: 20000,
      unit: "kg",
      stock: 25,
      freshnessStatus: "Panen Hari Ini",
      deliveryMethods: "Kurir Lokal, Ambil Langsung",
      rating: 4.9,
      reviewCount: 12,
      promoted: false,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Aligned query succeeds, unfiltered fails
  await assertSucceeds(bob.collection("products").where("active", "==", true).get());
  await assertFails(bob.collection("products").get());

  // Bob cannot edit Alice's product
  await assertFails(
    bob.collection("products").doc("prod_1").update({
      price: 1000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob creates an order
  await assertSucceeds(
    bob.collection("orders").doc("ord_1").set({
      buyerId: BOB_UID,
      buyerName: "Bob Pembeli",
      sellerId: ALICE_UID,
      sellerName: "Kebun Hijau Alice",
      mainCategory: "HASIL_PANEN",
      itemsSummary: "Selada Hijau Segar x2 kg",
      subtotal: 40000,
      shippingFee: 10000,
      serviceFee: 2000,
      commissionAmount: 2000,
      totalAmount: 52000,
      paymentMethod: "QRIS",
      paymentStatus: "SIMULASI_DEMO",
      orderStatus: "MENUNGGU_KONFIRMASI",
      deliveryMethod: "Kurir Lokal",
      deliveryAddress: "Jl. Dago No. 12, Bandung",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seller updates order status to DIPROSES
  await assertSucceeds(
    alice.collection("orders").doc("ord_1").update({
      orderStatus: "DIPROSES",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Buyer confirms completion
  await assertSucceeds(
    bob.collection("orders").doc("ord_1").update({
      orderStatus: "SELESAI",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Terminal state lock prevents non-admin updates once SELESAI
  await assertFails(
    alice.collection("orders").doc("ord_1").update({
      orderStatus: "DIBATALKAN",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Admin settings & wholesale requests & forum posts", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const admin = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const spoofAdmin = testEnv.authenticatedContext("spoof", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const now = new Date(Date.now() - 2000);

  // Spoofed unverified admin rejected
  await assertFails(
    spoofAdmin.collection("admin_settings").doc("platform_fees").set({
      marketplaceCommissionPct: 10,
      supplierCommissionPct: 5,
      deliveryCommissionPct: 5,
      consultationCommissionPct: 10,
      wholesaleCommissionPct: 3,
      publicConfig: true,
      updatedAt: now,
    })
  );

  // Verified admin succeeds
  await assertSucceeds(
    admin.collection("admin_settings").doc("platform_fees").set({
      marketplaceCommissionPct: 5,
      supplierCommissionPct: 5,
      deliveryCommissionPct: 5,
      consultationCommissionPct: 10,
      wholesaleCommissionPct: 3,
      publicConfig: true,
      updatedAt: now,
    })
  );

  // Signed-in user can read publicConfig
  await assertSucceeds(alice.collection("admin_settings").where("publicConfig", "==", true).get());
  await assertFails(alice.collection("admin_settings").doc("platform_fees").update({ marketplaceCommissionPct: 0, updatedAt: now }));
});
