package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface HydroGrowRepository {
    val isDemoMode: Boolean

    fun observeUserProfile(userId: String): Flow<UserProfile?>
    fun observePlants(userId: String): Flow<List<HydroPlant>>
    fun observeSchedules(userId: String): Flow<List<MaintenanceSchedule>>
    fun observeMonitoringLogs(userId: String): Flow<List<PlantMonitoringLog>>
    fun observeHarvests(userId: String): Flow<List<HarvestRecord>>
    fun observeExpenses(userId: String): Flow<List<FinancialExpense>>
    fun observeRevenues(userId: String): Flow<List<FinancialRevenue>>
    fun observeCart(userId: String): Flow<List<CartItem>>
    fun observeNotifications(userId: String): Flow<List<AppNotification>>
    fun observeProducts(): Flow<List<MarketplaceProduct>>
    fun observeSellerProducts(sellerId: String): Flow<List<MarketplaceProduct>>
    fun observeBuyerOrders(userId: String): Flow<List<MarketplaceOrder>>
    fun observeSellerOrders(userId: String): Flow<List<MarketplaceOrder>>
    fun observeWholesaleRequests(): Flow<List<WholesaleRequest>>
    fun observeForumPosts(): Flow<List<ForumPost>>
    fun observeAdminSettings(): Flow<AdminSetting>

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    suspend fun savePlant(plant: HydroPlant): Result<Unit>
    suspend fun deletePlant(userId: String, plantId: String): Result<Unit>
    suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit>
    suspend fun toggleScheduleCompleted(userId: String, scheduleId: String, completed: Boolean): Result<Unit>
    suspend fun deleteSchedule(userId: String, scheduleId: String): Result<Unit>
    suspend fun addMonitoringLog(log: PlantMonitoringLog): Result<Unit>
    suspend fun addHarvestRecord(record: HarvestRecord): Result<Unit>
    suspend fun addExpense(expense: FinancialExpense): Result<Unit>
    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit>
    suspend fun addRevenue(revenue: FinancialRevenue): Result<Unit>
    suspend fun deleteRevenue(userId: String, revenueId: String): Result<Unit>
    suspend fun saveProduct(product: MarketplaceProduct): Result<Unit>
    suspend fun toggleProductActive(productId: String, active: Boolean): Result<Unit>
    suspend fun addToCart(item: CartItem): Result<Unit>
    suspend fun updateCartQuantity(userId: String, cartItemId: String, quantity: Long): Result<Unit>
    suspend fun clearCart(userId: String): Result<Unit>
    suspend fun placeOrder(order: MarketplaceOrder, purchasedItems: List<CartItem>): Result<Unit>
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, paymentStatus: PaymentStatus? = null): Result<Unit>
    suspend fun submitWholesaleOffer(requestId: String, farmerId: String, offerPricePerKg: Long): Result<Unit>
    suspend fun createWholesaleRequest(request: WholesaleRequest): Result<Unit>
    suspend fun createForumPost(post: ForumPost): Result<Unit>
    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit>
    suspend fun addNotification(notification: AppNotification): Result<Unit>
    suspend fun updateAdminSettings(setting: AdminSetting): Result<Unit>
}

class DemoHydroGrowRepository : HydroGrowRepository {
    override val isDemoMode: Boolean = true

    private val profileFlow = MutableStateFlow<UserProfile?>(DemoSeedData.demoProfile(UserRole.PETANI))
    private val plantsFlow = MutableStateFlow(DemoSeedData.initialPlants)
    private val schedulesFlow = MutableStateFlow(DemoSeedData.initialSchedules)
    private val logsFlow = MutableStateFlow(DemoSeedData.initialMonitoringLogs)
    private val harvestsFlow = MutableStateFlow(DemoSeedData.initialHarvests)
    private val expensesFlow = MutableStateFlow(DemoSeedData.initialExpenses)
    private val revenuesFlow = MutableStateFlow(DemoSeedData.initialRevenues)
    private val cartFlow = MutableStateFlow<List<CartItem>>(emptyList())
    private val notificationsFlow = MutableStateFlow(DemoSeedData.initialNotifications)
    private val productsFlow = MutableStateFlow(DemoSeedData.initialProducts)
    private val ordersFlow = MutableStateFlow(DemoSeedData.initialOrders)
    private val wholesaleFlow = MutableStateFlow(DemoSeedData.initialWholesaleRequests)
    private val forumFlow = MutableStateFlow(DemoSeedData.initialForumPosts)
    private val adminSettingFlow = MutableStateFlow(AdminSetting())

    fun switchDemoRole(role: UserRole) {
        val current = profileFlow.value
        val template = DemoSeedData.demoProfile(role)
        profileFlow.value = template.copy(
            city = current?.city ?: template.city,
            latitude = current?.latitude ?: template.latitude,
            longitude = current?.longitude ?: template.longitude
        )
    }

    fun updateDemoLocation(city: String, lat: Double, lng: Double) {
        profileFlow.update { it?.copy(city = city, latitude = lat, longitude = lng) }
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileFlow.asStateFlow()
    override fun observePlants(userId: String): Flow<List<HydroPlant>> = plantsFlow.asStateFlow()
    override fun observeSchedules(userId: String): Flow<List<MaintenanceSchedule>> = schedulesFlow.asStateFlow()
    override fun observeMonitoringLogs(userId: String): Flow<List<PlantMonitoringLog>> = logsFlow.asStateFlow()
    override fun observeHarvests(userId: String): Flow<List<HarvestRecord>> = harvestsFlow.asStateFlow()
    override fun observeExpenses(userId: String): Flow<List<FinancialExpense>> = expensesFlow.asStateFlow()
    override fun observeRevenues(userId: String): Flow<List<FinancialRevenue>> = revenuesFlow.asStateFlow()
    override fun observeCart(userId: String): Flow<List<CartItem>> = cartFlow.asStateFlow()
    override fun observeNotifications(userId: String): Flow<List<AppNotification>> = notificationsFlow.asStateFlow()
    override fun observeProducts(): Flow<List<MarketplaceProduct>> = productsFlow.map { list -> list.filter { it.active } }
    override fun observeSellerProducts(sellerId: String): Flow<List<MarketplaceProduct>> = productsFlow.asStateFlow()
    override fun observeBuyerOrders(userId: String): Flow<List<MarketplaceOrder>> = ordersFlow.asStateFlow()
    override fun observeSellerOrders(userId: String): Flow<List<MarketplaceOrder>> = ordersFlow.asStateFlow()
    override fun observeWholesaleRequests(): Flow<List<WholesaleRequest>> = wholesaleFlow.asStateFlow()
    override fun observeForumPosts(): Flow<List<ForumPost>> = forumFlow.asStateFlow()
    override fun observeAdminSettings(): Flow<AdminSetting> = adminSettingFlow.asStateFlow()

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        profileFlow.value = profile
        return Result.success(Unit)
    }

    override suspend fun savePlant(plant: HydroPlant): Result<Unit> {
        val targetId = plant.id.ifBlank { "demo_plant_${UUID.randomUUID().toString().take(8)}" }
        val updated = plant.copy(id = targetId)
        plantsFlow.update { list ->
            if (list.any { it.id == targetId }) list.map { if (it.id == targetId) updated else it }
            else listOf(updated) + list
        }
        return Result.success(Unit)
    }

    override suspend fun deletePlant(userId: String, plantId: String): Result<Unit> {
        plantsFlow.update { list -> list.filterNot { it.id == plantId } }
        return Result.success(Unit)
    }

    override suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit> {
        val targetId = schedule.id.ifBlank { "demo_sched_${UUID.randomUUID().toString().take(8)}" }
        val updated = schedule.copy(id = targetId)
        schedulesFlow.update { list ->
            if (list.any { it.id == targetId }) list.map { if (it.id == targetId) updated else it }
            else listOf(updated) + list
        }
        return Result.success(Unit)
    }

    override suspend fun toggleScheduleCompleted(userId: String, scheduleId: String, completed: Boolean): Result<Unit> {
        schedulesFlow.update { list -> list.map { if (it.id == scheduleId) it.copy(completed = completed) else it } }
        return Result.success(Unit)
    }

    override suspend fun deleteSchedule(userId: String, scheduleId: String): Result<Unit> {
        schedulesFlow.update { list -> list.filterNot { it.id == scheduleId } }
        return Result.success(Unit)
    }

    override suspend fun addMonitoringLog(log: PlantMonitoringLog): Result<Unit> {
        val targetId = log.id.ifBlank { "demo_log_${UUID.randomUUID().toString().take(8)}" }
        logsFlow.update { list -> list + log.copy(id = targetId) }
        return Result.success(Unit)
    }

    override suspend fun addHarvestRecord(record: HarvestRecord): Result<Unit> {
        val targetId = record.id.ifBlank { "demo_harv_${UUID.randomUUID().toString().take(8)}" }
        harvestsFlow.update { list -> listOf(record.copy(id = targetId)) + list }
        return Result.success(Unit)
    }

    override suspend fun addExpense(expense: FinancialExpense): Result<Unit> {
        val targetId = expense.id.ifBlank { "demo_exp_${UUID.randomUUID().toString().take(8)}" }
        expensesFlow.update { list -> listOf(expense.copy(id = targetId)) + list }
        return Result.success(Unit)
    }

    override suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> {
        expensesFlow.update { list -> list.filterNot { it.id == expenseId } }
        return Result.success(Unit)
    }

    override suspend fun addRevenue(revenue: FinancialRevenue): Result<Unit> {
        val targetId = revenue.id.ifBlank { "demo_rev_${UUID.randomUUID().toString().take(8)}" }
        revenuesFlow.update { list -> listOf(revenue.copy(id = targetId)) + list }
        return Result.success(Unit)
    }

    override suspend fun deleteRevenue(userId: String, revenueId: String): Result<Unit> {
        revenuesFlow.update { list -> list.filterNot { it.id == revenueId } }
        return Result.success(Unit)
    }

    override suspend fun saveProduct(product: MarketplaceProduct): Result<Unit> {
        val targetId = product.id.ifBlank { "demo_prod_${UUID.randomUUID().toString().take(8)}" }
        val updated = product.copy(id = targetId, isDemo = true)
        productsFlow.update { list ->
            if (list.any { it.id == targetId }) list.map { if (it.id == targetId) updated else it }
            else listOf(updated) + list
        }
        return Result.success(Unit)
    }

    override suspend fun toggleProductActive(productId: String, active: Boolean): Result<Unit> {
        productsFlow.update { list -> list.map { if (it.id == productId) it.copy(active = active) else it } }
        return Result.success(Unit)
    }

    override suspend fun addToCart(item: CartItem): Result<Unit> {
        cartFlow.update { list ->
            val existing = list.firstOrNull { it.productId == item.productId }
            if (existing != null) {
                list.map { if (it.productId == item.productId) it.copy(quantity = it.quantity + item.quantity) else it }
            } else {
                list + item.copy(id = "demo_cart_${UUID.randomUUID().toString().take(8)}")
            }
        }
        return Result.success(Unit)
    }

    override suspend fun updateCartQuantity(userId: String, cartItemId: String, quantity: Long): Result<Unit> {
        cartFlow.update { list ->
            if (quantity <= 0L) list.filterNot { it.id == cartItemId }
            else list.map { if (it.id == cartItemId) it.copy(quantity = quantity) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun clearCart(userId: String): Result<Unit> {
        cartFlow.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun placeOrder(order: MarketplaceOrder, purchasedItems: List<CartItem>): Result<Unit> {
        val orderId = order.id.ifBlank { "demo_ord_${UUID.randomUUID().toString().take(6)}" }
        val savedOrder = order.copy(id = orderId, isDemo = true)
        ordersFlow.update { list -> listOf(savedOrder) + list }
        // Reduce stock safely
        productsFlow.update { prods ->
            prods.map { p ->
                val qtyBought = purchasedItems.filter { it.productId == p.id }.sumOf { it.quantity }
                if (qtyBought > 0L) p.copy(stock = (p.stock - qtyBought).coerceAtLeast(0L)) else p
            }
        }
        // Record purchase as expense if PERLENGKAPAN
        if (order.mainCategory == MainCategory.PERLENGKAPAN.name) {
            addExpense(
                FinancialExpense(
                    userId = order.buyerId,
                    entryType = ExpenseType.PENGELUARAN.name,
                    category = "Perlengkapan",
                    title = "Pembelian DEMO: ${order.itemsSummary}",
                    amount = order.totalAmount,
                    cycleLabel = "Marketplace Demo",
                    notes = "Otomatis dari checkout simulasi demo #$orderId",
                    dateMillis = System.currentTimeMillis()
                )
            )
        }
        addNotification(
            AppNotification(
                userId = order.buyerId,
                title = "Pesanan Demo Dibuat (#$orderId)",
                body = "Simulasi pembayaran ${order.paymentMethod} berhasil dicatat. Status: Menunggu Konfirmasi.",
                type = "PESANAN",
                read = false
            )
        )
        cartFlow.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        paymentStatus: PaymentStatus?
    ): Result<Unit> {
        var completedOrder: MarketplaceOrder? = null
        ordersFlow.update { list ->
            list.map { ord ->
                if (ord.id == orderId) {
                    val updated = ord.copy(
                        orderStatus = newStatus.name,
                        paymentStatus = paymentStatus?.name ?: ord.paymentStatus
                    )
                    if (newStatus == OrderStatus.SELESAI && ord.orderStatus != OrderStatus.SELESAI.name) {
                        completedOrder = updated
                    }
                    updated
                } else ord
            }
        }
        completedOrder?.let { ord ->
            addRevenue(
                FinancialRevenue(
                    userId = ord.sellerId,
                    category = if (ord.mainCategory == MainCategory.PERLENGKAPAN.name) "Penjualan Perlengkapan" else "Penjualan Hasil Panen",
                    title = "Pesanan Selesai #${ord.id}: ${ord.itemsSummary}",
                    amount = ord.subtotal,
                    orderId = ord.id,
                    cycleLabel = "Marketplace Demo",
                    notes = "Komisi platform ilustratif: Rp${ord.commissionAmount}",
                    dateMillis = System.currentTimeMillis()
                )
            )
        }
        return Result.success(Unit)
    }

    override suspend fun submitWholesaleOffer(requestId: String, farmerId: String, offerPricePerKg: Long): Result<Unit> {
        wholesaleFlow.update { list ->
            list.map {
                if (it.id == requestId) it.copy(farmerId = farmerId, farmerOfferPrice = offerPricePerKg, status = WholesaleStatus.PENAWARAN.name)
                else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun createWholesaleRequest(request: WholesaleRequest): Result<Unit> {
        val targetId = "demo_ws_${UUID.randomUUID().toString().take(6)}"
        wholesaleFlow.update { list -> listOf(request.copy(id = targetId)) + list }
        return Result.success(Unit)
    }

    override suspend fun createForumPost(post: ForumPost): Result<Unit> {
        val targetId = "demo_forum_${UUID.randomUUID().toString().take(6)}"
        forumFlow.update { list -> listOf(post.copy(id = targetId)) + list }
        return Result.success(Unit)
    }

    override suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        notificationsFlow.update { list ->
            list.map { if (it.id == notificationId) it.copy(read = true) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun addNotification(notification: AppNotification): Result<Unit> {
        val targetId = "demo_notif_${UUID.randomUUID().toString().take(6)}"
        notificationsFlow.update { list -> listOf(notification.copy(id = targetId)) + list }
        return Result.success(Unit)
    }

    override suspend fun updateAdminSettings(setting: AdminSetting): Result<Unit> {
        adminSettingFlow.value = setting
        return Result.success(Unit)
    }
}

class FirestoreHydroGrowRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : HydroGrowRepository {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    override val isDemoMode: Boolean = false

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    private fun <T> Flow<T>.guardFirestoreErrors(path: String, op: OperationType, expectedUid: String? = null): Flow<T> {
        return catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId).snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.GET, userId)
        )
    }

    override fun observePlants(userId: String): Flow<List<HydroPlant>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/plants"
        emitAll(
            db.collection("users").document(userId).collection("plants").snapshots()
                .map { snap -> snap.toObjects(HydroPlant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeSchedules(userId: String): Flow<List<MaintenanceSchedule>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/maintenance_schedules"
        emitAll(
            db.collection("users").document(userId).collection("maintenance_schedules").snapshots()
                .map { snap -> snap.toObjects(MaintenanceSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeMonitoringLogs(userId: String): Flow<List<PlantMonitoringLog>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/plant_monitoring"
        emitAll(
            db.collection("users").document(userId).collection("plant_monitoring").snapshots()
                .map { snap -> snap.toObjects(PlantMonitoringLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeHarvests(userId: String): Flow<List<HarvestRecord>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/harvests"
        emitAll(
            db.collection("users").document(userId).collection("harvests").snapshots()
                .map { snap -> snap.toObjects(HarvestRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeExpenses(userId: String): Flow<List<FinancialExpense>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/expenses"
        emitAll(
            db.collection("users").document(userId).collection("expenses").snapshots()
                .map { snap -> snap.toObjects(FinancialExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeRevenues(userId: String): Flow<List<FinancialRevenue>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/revenues"
        emitAll(
            db.collection("users").document(userId).collection("revenues").snapshots()
                .map { snap -> snap.toObjects(FinancialRevenue::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/carts"
        emitAll(
            db.collection("users").document(userId).collection("carts").snapshots()
                .map { snap -> snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications").snapshots()
                .map { snap -> snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(path, OperationType.LIST, userId)
        )
    }

    override fun observeProducts(): Flow<List<MarketplaceProduct>> = flow {
        requireUserId()
        emitAll(
            db.collection("products")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    val live = snap.toObjects(MarketplaceProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (live.isEmpty()) DemoSeedData.initialProducts else live
                }
                .guardFirestoreErrors("products", OperationType.LIST)
        )
    }

    override fun observeSellerProducts(sellerId: String): Flow<List<MarketplaceProduct>> = flow {
        require(sellerId.isNotBlank())
        emitAll(
            db.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { snap -> snap.toObjects(MarketplaceProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors("products", OperationType.LIST, sellerId)
        )
    }

    override fun observeBuyerOrders(userId: String): Flow<List<MarketplaceOrder>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("orders")
                .whereEqualTo("buyerId", userId)
                .snapshots()
                .map { snap -> snap.toObjects(MarketplaceOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors("orders", OperationType.LIST, userId)
        )
    }

    override fun observeSellerOrders(userId: String): Flow<List<MarketplaceOrder>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("orders")
                .whereEqualTo("sellerId", userId)
                .snapshots()
                .map { snap -> snap.toObjects(MarketplaceOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors("orders", OperationType.LIST, userId)
        )
    }

    override fun observeWholesaleRequests(): Flow<List<WholesaleRequest>> = flow {
        requireUserId()
        emitAll(
            db.collection("wholesale_requests")
                .whereEqualTo("open", true)
                .snapshots()
                .map { snap ->
                    val live = snap.toObjects(WholesaleRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (live.isEmpty()) DemoSeedData.initialWholesaleRequests else live
                }
                .guardFirestoreErrors("wholesale_requests", OperationType.LIST)
        )
    }

    override fun observeForumPosts(): Flow<List<ForumPost>> = flow {
        requireUserId()
        emitAll(
            db.collection("forum_posts")
                .whereEqualTo("visible", true)
                .snapshots()
                .map { snap ->
                    val live = snap.toObjects(ForumPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (live.isEmpty()) DemoSeedData.initialForumPosts else live
                }
                .guardFirestoreErrors("forum_posts", OperationType.LIST)
        )
    }

    override fun observeAdminSettings(): Flow<AdminSetting> = flow {
        requireUserId()
        emitAll(
            db.collection("admin_settings")
                .whereEqualTo("publicConfig", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminSetting::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).firstOrNull()
                        ?: AdminSetting()
                }
                .guardFirestoreErrors("admin_settings", OperationType.LIST)
        )
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            val docRef = db.collection("users").document(currentUid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "fullName" to profile.fullName.take(100),
                    "email" to profile.email.take(150),
                    "phone" to profile.phone.take(30),
                    "role" to profile.role,
                    "address" to profile.address.take(300),
                    "city" to profile.city.take(100),
                    "latitude" to profile.latitude,
                    "longitude" to profile.longitude,
                    "photoUrl" to profile.photoUrl.take(500).ifBlank { null },
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to currentUid,
                    "fullName" to profile.fullName.ifBlank { "Pengguna HydroGrow" }.take(100),
                    "email" to profile.email.take(150),
                    "phone" to profile.phone.take(30),
                    "role" to profile.role,
                    "address" to profile.address.take(300),
                    "city" to profile.city.take(100),
                    "latitude" to profile.latitude,
                    "longitude" to profile.longitude,
                    "photoUrl" to profile.photoUrl.take(500).ifBlank { null },
                    "verifiedSeller" to false,
                    "verifiedSupplier" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun savePlant(plant: HydroPlant): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val isCreate = plant.id.isBlank()
        val docRef = if (isCreate) {
            db.collection("users").document(uid).collection("plants").document()
        } else {
            db.collection("users").document(uid).collection("plants").document(plant.id)
        }
        return try {
            val currentUid = requireUserId()
            val ref = if (isCreate) db.collection("users").document(currentUid).collection("plants").document()
            else db.collection("users").document(currentUid).collection("plants").document(plant.id)
            if (isCreate) {
                val payload = mapOf(
                    "userId" to currentUid,
                    "name" to plant.name.take(100),
                    "cropType" to plant.cropType.take(60),
                    "systemType" to plant.systemType.take(60),
                    "holeCount" to plant.holeCount.coerceIn(1L, 100000L),
                    "plantedDateMillis" to plant.plantedDateMillis,
                    "transplantDateMillis" to plant.transplantDateMillis,
                    "harvestAgeDays" to plant.harvestAgeDays.coerceIn(1L, 365L),
                    "status" to plant.status,
                    "phMin" to plant.phMin,
                    "phMax" to plant.phMax,
                    "ecMin" to plant.ecMin,
                    "ecMax" to plant.ecMax,
                    "photoUrl" to plant.photoUrl.take(500).ifBlank { null },
                    "notes" to plant.notes.take(500).ifBlank { null },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to plant.name.take(100),
                    "cropType" to plant.cropType.take(60),
                    "systemType" to plant.systemType.take(60),
                    "holeCount" to plant.holeCount.coerceIn(1L, 100000L),
                    "plantedDateMillis" to plant.plantedDateMillis,
                    "transplantDateMillis" to plant.transplantDateMillis,
                    "harvestAgeDays" to plant.harvestAgeDays.coerceIn(1L, 365L),
                    "status" to plant.status,
                    "phMin" to plant.phMin,
                    "phMax" to plant.phMax,
                    "ecMin" to plant.ecMin,
                    "ecMax" to plant.ecMax,
                    "photoUrl" to plant.photoUrl.take(500).ifBlank { null },
                    "notes" to plant.notes.take(500).ifBlank { null },
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deletePlant(userId: String, plantId: String): Result<Unit> {
        val path = "users/$userId/plants/$plantId"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid).collection("plants").document(plantId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val isCreate = schedule.id.isBlank()
        val path = "users/$uid/maintenance_schedules"
        return try {
            val currentUid = requireUserId()
            val col = db.collection("users").document(currentUid).collection("maintenance_schedules")
            val ref = if (isCreate) col.document() else col.document(schedule.id)
            if (isCreate) {
                val payload = mapOf(
                    "userId" to currentUid,
                    "plantId" to schedule.plantId.ifBlank { null },
                    "plantName" to schedule.plantName.ifBlank { null },
                    "taskType" to schedule.taskType.take(60),
                    "title" to schedule.title.take(120),
                    "dueDateMillis" to schedule.dueDateMillis,
                    "completed" to schedule.completed,
                    "notes" to schedule.notes.take(500).ifBlank { null },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()
            } else {
                val updates = mapOf(
                    "plantId" to schedule.plantId.ifBlank { null },
                    "plantName" to schedule.plantName.ifBlank { null },
                    "taskType" to schedule.taskType.take(60),
                    "title" to schedule.title.take(120),
                    "dueDateMillis" to schedule.dueDateMillis,
                    "completed" to schedule.completed,
                    "notes" to schedule.notes.take(500).ifBlank { null },
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun toggleScheduleCompleted(userId: String, scheduleId: String, completed: Boolean): Result<Unit> {
        val path = "users/$userId/maintenance_schedules/$scheduleId"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid).collection("maintenance_schedules").document(scheduleId)
                .update(mapOf("completed" to completed, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteSchedule(userId: String, scheduleId: String): Result<Unit> {
        val path = "users/$userId/maintenance_schedules/$scheduleId"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid).collection("maintenance_schedules").document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun addMonitoringLog(log: PlantMonitoringLog): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/plant_monitoring"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid).collection("plant_monitoring").document()
            val payload = mapOf(
                "userId" to currentUid,
                "plantId" to log.plantId,
                "plantName" to log.plantName.take(100),
                "ph" to log.ph,
                "ec" to log.ec,
                "tds" to log.tds,
                "waterTemp" to log.waterTemp,
                "ambientTemp" to log.ambientTemp,
                "heightCm" to log.heightCm,
                "rootCondition" to log.rootCondition.take(100).ifBlank { null },
                "plantCondition" to log.plantCondition.take(100).ifBlank { null },
                "healthStatus" to log.healthStatus,
                "notes" to log.notes.take(500).ifBlank { null },
                "recordedAtMillis" to log.recordedAtMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun addHarvestRecord(record: HarvestRecord): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/harvests"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid).collection("harvests").document()
            val payload = mapOf(
                "userId" to currentUid,
                "plantId" to record.plantId.ifBlank { null },
                "plantName" to record.plantName.take(100),
                "cropType" to record.cropType.take(60),
                "cycleLabel" to record.cycleLabel.take(80),
                "weightKg" to record.weightKg,
                "holeCount" to record.holeCount.coerceAtLeast(1L),
                "durationDays" to record.durationDays.coerceIn(1L, 365L),
                "notes" to record.notes.take(500).ifBlank { null },
                "harvestedAtMillis" to record.harvestedAtMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun addExpense(expense: FinancialExpense): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/expenses"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid).collection("expenses").document()
            val payload = mapOf(
                "userId" to currentUid,
                "entryType" to expense.entryType,
                "category" to expense.category.take(60),
                "title" to expense.title.take(120),
                "amount" to expense.amount.coerceAtLeast(0L),
                "cycleLabel" to expense.cycleLabel.take(80).ifBlank { null },
                "notes" to expense.notes.take(500).ifBlank { null },
                "dateMillis" to expense.dateMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> {
        val path = "users/$userId/expenses/$expenseId"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid).collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun addRevenue(revenue: FinancialRevenue): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/revenues"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid).collection("revenues").document()
            val payload = mapOf(
                "userId" to currentUid,
                "category" to revenue.category.take(60),
                "title" to revenue.title.take(120),
                "amount" to revenue.amount.coerceAtLeast(0L),
                "orderId" to revenue.orderId.take(128).ifBlank { null },
                "cycleLabel" to revenue.cycleLabel.take(80).ifBlank { null },
                "notes" to revenue.notes.take(500).ifBlank { null },
                "dateMillis" to revenue.dateMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteRevenue(userId: String, revenueId: String): Result<Unit> {
        val path = "users/$userId/revenues/$revenueId"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid).collection("revenues").document(revenueId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun saveProduct(product: MarketplaceProduct): Result<Unit> {
        val isCreate = product.id.isBlank()
        val ref = if (isCreate) db.collection("products").document() else db.collection("products").document(product.id)
        return try {
            val currentUid = requireUserId()
            if (isCreate) {
                val payload = mapOf(
                    "sellerId" to currentUid,
                    "sellerName" to product.sellerName.take(100),
                    "sellerArea" to product.sellerArea.take(100),
                    "sellerLat" to product.sellerLat,
                    "sellerLng" to product.sellerLng,
                    "mainCategory" to product.mainCategory,
                    "subCategory" to product.subCategory.take(60),
                    "name" to product.name.take(120),
                    "description" to product.description.take(1000).ifBlank { null },
                    "price" to product.price.coerceAtLeast(0L),
                    "unit" to product.unit.take(30),
                    "stock" to product.stock.coerceAtLeast(0L),
                    "freshnessStatus" to product.freshnessStatus.take(60).ifBlank { null },
                    "deliveryMethods" to product.deliveryMethods.take(120).ifBlank { null },
                    "rating" to product.rating,
                    "reviewCount" to product.reviewCount,
                    "promoted" to product.promoted,
                    "active" to product.active,
                    "photoUrl" to product.photoUrl.take(500).ifBlank { null },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to product.name.take(120),
                    "description" to product.description.take(1000).ifBlank { null },
                    "mainCategory" to product.mainCategory,
                    "subCategory" to product.subCategory.take(60),
                    "price" to product.price.coerceAtLeast(0L),
                    "unit" to product.unit.take(30),
                    "stock" to product.stock.coerceAtLeast(0L),
                    "freshnessStatus" to product.freshnessStatus.take(60).ifBlank { null },
                    "deliveryMethods" to product.deliveryMethods.take(120).ifBlank { null },
                    "promoted" to product.promoted,
                    "active" to product.active,
                    "photoUrl" to product.photoUrl.take(500).ifBlank { null },
                    "sellerArea" to product.sellerArea.take(100),
                    "sellerLat" to product.sellerLat,
                    "sellerLng" to product.sellerLng,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    override suspend fun toggleProductActive(productId: String, active: Boolean): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId)
                .update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun addToCart(item: CartItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/carts"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid).collection("carts").document(item.productId)
            val snap = ref.get().await()
            if (snap.exists()) {
                val existingQty = snap.getLong("quantity") ?: 0L
                ref.update(
                    mapOf(
                        "quantity" to (existingQty + item.quantity).coerceIn(1L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to currentUid,
                    "productId" to item.productId,
                    "productName" to item.productName.take(120),
                    "sellerId" to item.sellerId,
                    "sellerName" to item.sellerName.take(100),
                    "mainCategory" to item.mainCategory,
                    "unitPrice" to item.unitPrice,
                    "unit" to item.unit.take(30),
                    "quantity" to item.quantity.coerceIn(1L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateCartQuantity(userId: String, cartItemId: String, quantity: Long): Result<Unit> {
        val path = "users/$userId/carts/$cartItemId"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid).collection("carts").document(cartItemId)
            if (quantity <= 0L) {
                ref.delete().await()
            } else {
                ref.update(mapOf("quantity" to quantity.coerceIn(1L, 10000L), "updatedAt" to FieldValue.serverTimestamp())).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun clearCart(userId: String): Result<Unit> {
        val path = "users/$userId/carts"
        return try {
            val currentUid = requireUserId()
            val snaps = db.collection("users").document(currentUid).collection("carts").get().await()
            snaps.documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun placeOrder(order: MarketplaceOrder, purchasedItems: List<CartItem>): Result<Unit> {
        val ref = db.collection("orders").document()
        return try {
            val currentUid = requireUserId()
            val payload = mapOf(
                "buyerId" to currentUid,
                "buyerName" to order.buyerName.take(100),
                "sellerId" to order.sellerId,
                "sellerName" to order.sellerName.take(100),
                "courierId" to order.courierId.ifBlank { null },
                "mainCategory" to order.mainCategory,
                "itemsSummary" to order.itemsSummary.take(500),
                "subtotal" to order.subtotal,
                "shippingFee" to order.shippingFee,
                "serviceFee" to order.serviceFee,
                "commissionAmount" to order.commissionAmount,
                "totalAmount" to order.totalAmount,
                "paymentMethod" to order.paymentMethod.take(60),
                "paymentStatus" to order.paymentStatus,
                "orderStatus" to OrderStatus.MENUNGGU_KONFIRMASI.name,
                "deliveryMethod" to order.deliveryMethod.take(80).ifBlank { null },
                "deliveryAddress" to order.deliveryAddress.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            clearCart(currentUid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        paymentStatus: PaymentStatus?
    ): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            val updates = mapOf(
                "orderStatus" to newStatus.name,
                "paymentStatus" to paymentStatus?.name,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun submitWholesaleOffer(requestId: String, farmerId: String, offerPricePerKg: Long): Result<Unit> {
        val path = "wholesale_requests/$requestId"
        return try {
            val currentUid = requireUserId()
            db.collection("wholesale_requests").document(requestId)
                .update(
                    mapOf(
                        "farmerId" to currentUid,
                        "farmerOfferPrice" to offerPricePerKg,
                        "status" to WholesaleStatus.PENAWARAN.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun createWholesaleRequest(request: WholesaleRequest): Result<Unit> {
        val ref = db.collection("wholesale_requests").document()
        return try {
            val currentUid = requireUserId()
            val payload = mapOf(
                "buyerId" to currentUid,
                "buyerCompany" to request.buyerCompany.take(120),
                "buyerType" to request.buyerType.take(60),
                "cropNeeded" to request.cropNeeded.take(80),
                "quantityKg" to request.quantityKg.coerceAtLeast(1L),
                "targetPricePerKg" to request.targetPricePerKg.coerceAtLeast(0L),
                "deliverySchedule" to request.deliverySchedule.take(100),
                "area" to request.area.take(100),
                "status" to WholesaleStatus.TERBUKA.name,
                "open" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    override suspend fun createForumPost(post: ForumPost): Result<Unit> {
        val ref = db.collection("forum_posts").document()
        return try {
            val currentUid = requireUserId()
            val payload = mapOf(
                "authorId" to currentUid,
                "authorName" to post.authorName.take(100),
                "title" to post.title.take(160),
                "content" to post.content.take(2000),
                "topic" to post.topic.take(60),
                "replyCount" to 0L,
                "visible" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    override suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid).collection("notifications").document(notificationId)
                .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun addNotification(notification: AppNotification): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/notifications"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid).collection("notifications").document()
            val payload = mapOf(
                "userId" to currentUid,
                "title" to notification.title.take(120),
                "body" to notification.body.take(500),
                "type" to notification.type.take(40),
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateAdminSettings(setting: AdminSetting): Result<Unit> {
        val path = "admin_settings/platform_fees"
        return try {
            requireUserId()
            val payload = mapOf(
                "marketplaceCommissionPct" to setting.marketplaceCommissionPct,
                "supplierCommissionPct" to setting.supplierCommissionPct,
                "deliveryCommissionPct" to setting.deliveryCommissionPct,
                "consultationCommissionPct" to setting.consultationCommissionPct,
                "wholesaleCommissionPct" to setting.wholesaleCommissionPct,
                "publicConfig" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Repo", "Admin setting write requires admin privileges", e)
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
