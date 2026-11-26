package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiAssistantRepository
import com.example.data.DemoHydroGrowRepository
import com.example.data.DemoSeedData
import com.example.data.HydroGrowRepository
import com.example.model.*
import com.example.util.HydroPhysics
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class FinancialSummary(
    val totalRevenue: Long = 0L,
    val totalOperationalExpense: Long = 0L,
    val totalCapital: Long = 0L,
    val netProfit: Long = 0L
)

class HydroGrowViewModel(
    private val repository: HydroGrowRepository,
    val currentUserId: String,
    private val aiRepository: AiAssistantRepository = AiAssistantRepository(),
    private val defaultDisplayName: String = "Pengguna HydroGrow",
    private val defaultEmail: String = ""
) : ViewModel() {

    val isDemoMode: Boolean = repository.isDemoMode

    private fun <T> Flow<T>.toUiState(defaultMessage: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, defaultMessage, rootError)
                emit(UiState.Error(rootError.message ?: defaultMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("Gagal memuat profil pengguna")

    val plantsState: StateFlow<UiState<List<HydroPlant>>> =
        repository.observePlants(currentUserId).toUiState("Gagal memuat data tanaman")

    val schedulesState: StateFlow<UiState<List<MaintenanceSchedule>>> =
        repository.observeSchedules(currentUserId).toUiState("Gagal memuat jadwal perawatan")

    val monitoringLogsState: StateFlow<UiState<List<PlantMonitoringLog>>> =
        repository.observeMonitoringLogs(currentUserId).toUiState("Gagal memuat log monitoring")

    val harvestsState: StateFlow<UiState<List<HarvestRecord>>> =
        repository.observeHarvests(currentUserId).toUiState("Gagal memuat riwayat panen")

    val expensesState: StateFlow<UiState<List<FinancialExpense>>> =
        repository.observeExpenses(currentUserId).toUiState("Gagal memuat catatan pengeluaran")

    val revenuesState: StateFlow<UiState<List<FinancialRevenue>>> =
        repository.observeRevenues(currentUserId).toUiState("Gagal memuat catatan pendapatan")

    val cartState: StateFlow<UiState<List<CartItem>>> =
        repository.observeCart(currentUserId).toUiState("Gagal memuat keranjang")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId).toUiState("Gagal memuat notifikasi")

    val productsState: StateFlow<UiState<List<MarketplaceProduct>>> =
        repository.observeProducts().toUiState("Gagal memuat katalog produk")

    val sellerProductsState: StateFlow<UiState<List<MarketplaceProduct>>> =
        repository.observeSellerProducts(currentUserId).toUiState("Gagal memuat produk penjual")

    val buyerOrdersState: StateFlow<UiState<List<MarketplaceOrder>>> =
        repository.observeBuyerOrders(currentUserId).toUiState("Gagal memuat pesanan pembeli")

    val sellerOrdersState: StateFlow<UiState<List<MarketplaceOrder>>> =
        repository.observeSellerOrders(currentUserId).toUiState("Gagal memuat pesanan masuk")

    val wholesaleState: StateFlow<UiState<List<WholesaleRequest>>> =
        repository.observeWholesaleRequests().toUiState("Gagal memuat permintaan grosir")

    val forumPostsState: StateFlow<UiState<List<ForumPost>>> =
        repository.observeForumPosts().toUiState("Gagal memuat forum diskusi")

    val adminSettingsState: StateFlow<AdminSetting> =
        repository.observeAdminSettings()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Gagal memuat admin settings", error)
                emit(AdminSetting())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AdminSetting())

    // Local interactive states
    private val _selectedLocation = MutableStateFlow(DemoSeedData.indonesianCities.first())
    val selectedLocation: StateFlow<CityLocation> = _selectedLocation.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _reviews = MutableStateFlow(DemoSeedData.initialReviews)
    val reviews: StateFlow<List<ProductReview>> = _reviews.asStateFlow()

    private val _sellerChats = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val sellerChats: StateFlow<Map<String, List<ChatMessage>>> = _sellerChats.asStateFlow()

    private val _consultationsBooked = MutableStateFlow<List<String>>(emptyList())
    val consultationsBooked: StateFlow<List<String>> = _consultationsBooked.asStateFlow()

    private val _aiMessages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "ai_welcome",
                fromUser = false,
                text = "Halo! Saya Asisten AI HydroGrow. Tanyakan seputar pH/EC, penanganan hama, estimasi modal, simulasi keuntungan, atau pilih tanaman Anda untuk dianalisis. (Catatan: saran AI bersifat panduan, bukan diagnosis laboratorium pasti)."
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _aiStreamingText = MutableStateFlow<String?>(null)
    val aiStreamingText: StateFlow<String?> = _aiStreamingText.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun updateGlobalSearch(query: String) {
        _globalSearchQuery.value = query
    }

    fun selectLocation(city: CityLocation) {
        _selectedLocation.value = city
        if (repository is DemoHydroGrowRepository) {
            repository.updateDemoLocation(city.name, city.latitude, city.longitude)
        }
        val current = (userProfileState.value as? UiState.Success)?.data
        if (current != null && !isDemoMode) {
            viewModelScope.launch {
                repository.saveUserProfile(
                    current.copy(city = city.name, latitude = city.latitude, longitude = city.longitude)
                )
            }
        }
    }

    fun switchDemoRole(role: UserRole) {
        if (repository is DemoHydroGrowRepository) {
            repository.switchDemoRole(role)
            _snackbarMessage.value = "Berhasil beralih ke tampilan ${role.label} (Mode Demo)"
        }
    }

    fun saveProfile(
        fullName: String,
        phone: String,
        role: UserRole,
        address: String,
        city: CityLocation,
        photoUrl: String = ""
    ) {
        if (fullName.trim().length < 2) {
            _snackbarMessage.value = "Nama lengkap minimal 2 karakter"
            return
        }
        viewModelScope.launch {
            val existing = (userProfileState.value as? UiState.Success)?.data
            val profile = (existing ?: UserProfile(userId = currentUserId, email = defaultEmail)).copy(
                userId = currentUserId,
                fullName = fullName.trim(),
                phone = phone.trim(),
                role = role.name,
                address = address.trim(),
                city = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
                photoUrl = photoUrl
            )
            repository.saveUserProfile(profile).onSuccess {
                _selectedLocation.value = city
                _snackbarMessage.value = "Profil berhasil disimpan"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menyimpan profil"
            }
        }
    }

    fun savePlant(plant: HydroPlant) {
        if (plant.name.isBlank()) {
            _snackbarMessage.value = "Nama tanaman wajib diisi"
            return
        }
        viewModelScope.launch {
            repository.savePlant(plant.copy(userId = currentUserId)).onSuccess {
                _snackbarMessage.value = if (plant.id.isBlank()) "Tanaman ditambahkan" else "Data tanaman diperbarui"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal menyimpan tanaman"
            }
        }
    }

    fun deletePlant(plantId: String) {
        viewModelScope.launch {
            repository.deletePlant(currentUserId, plantId).onSuccess {
                _snackbarMessage.value = "Tanaman dihapus"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal menghapus tanaman"
            }
        }
    }

    fun saveSchedule(schedule: MaintenanceSchedule) {
        if (schedule.title.isBlank()) {
            _snackbarMessage.value = "Judul jadwal perawatan wajib diisi"
            return
        }
        viewModelScope.launch {
            repository.saveSchedule(schedule.copy(userId = currentUserId)).onSuccess {
                _snackbarMessage.value = "Jadwal perawatan disimpan"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal menyimpan jadwal"
            }
        }
    }

    fun toggleSchedule(schedule: MaintenanceSchedule) {
        viewModelScope.launch {
            repository.toggleScheduleCompleted(currentUserId, schedule.id, !schedule.completed)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            repository.deleteSchedule(currentUserId, scheduleId).onSuccess {
                _snackbarMessage.value = "Jadwal dihapus"
            }
        }
    }

    fun addMonitoringLog(
        plant: HydroPlant,
        ph: Double,
        ec: Double,
        tds: Long,
        waterTemp: Double,
        ambientTemp: Double,
        heightCm: Double,
        rootCondition: String,
        plantCondition: String,
        notes: String
    ) {
        val status = HydroPhysics.evaluateHealth(plant, ph, ec, waterTemp)
        viewModelScope.launch {
            val log = PlantMonitoringLog(
                userId = currentUserId,
                plantId = plant.id,
                plantName = plant.name,
                ph = ph,
                ec = ec,
                tds = tds,
                waterTemp = waterTemp,
                ambientTemp = ambientTemp,
                heightCm = heightCm,
                rootCondition = rootCondition,
                plantCondition = plantCondition,
                healthStatus = status.name,
                notes = notes,
                recordedAtMillis = System.currentTimeMillis()
            )
            repository.addMonitoringLog(log).onSuccess {
                _snackbarMessage.value = "Monitoring dicatat (${status.label})"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal mencatat monitoring"
            }
        }
    }

    fun recordHarvest(plant: HydroPlant, weightKg: Double, cycleLabel: String, notes: String) {
        viewModelScope.launch {
            val durationDays = ((System.currentTimeMillis() - plant.plantedDateMillis) / 86_400_000L).coerceIn(1L, 365L)
            val record = HarvestRecord(
                userId = currentUserId,
                plantId = plant.id,
                plantName = plant.name,
                cropType = plant.cropType,
                cycleLabel = cycleLabel.ifBlank { "Siklus ${plant.cropType}" },
                weightKg = weightKg,
                holeCount = plant.holeCount,
                durationDays = durationDays,
                notes = notes,
                harvestedAtMillis = System.currentTimeMillis()
            )
            repository.addHarvestRecord(record).onSuccess {
                repository.savePlant(plant.copy(status = PlantStatus.PANEN_SELESAI.name))
                _snackbarMessage.value = "Hasil panen ${weightKg} kg tersimpan ke riwayat"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal mencatat panen"
            }
        }
    }

    fun addExpense(entryType: ExpenseType, category: String, title: String, amount: Long, cycleLabel: String, notes: String) {
        if (title.isBlank() || amount <= 0L) {
            _snackbarMessage.value = "Judul dan nominal pengeluaran harus valid"
            return
        }
        viewModelScope.launch {
            repository.addExpense(
                FinancialExpense(
                    userId = currentUserId,
                    entryType = entryType.name,
                    category = category,
                    title = title.trim(),
                    amount = amount,
                    cycleLabel = cycleLabel.trim(),
                    notes = notes.trim(),
                    dateMillis = System.currentTimeMillis()
                )
            ).onSuccess {
                _snackbarMessage.value = "${entryType.label} berhasil dicatat"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal menyimpan catatan"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(currentUserId, expenseId).onSuccess {
                _snackbarMessage.value = "Catatan pengeluaran dihapus"
            }
        }
    }

    fun addRevenue(category: String, title: String, amount: Long, cycleLabel: String, notes: String) {
        if (title.isBlank() || amount <= 0L) {
            _snackbarMessage.value = "Judul dan nominal pendapatan harus valid"
            return
        }
        viewModelScope.launch {
            repository.addRevenue(
                FinancialRevenue(
                    userId = currentUserId,
                    category = category,
                    title = title.trim(),
                    amount = amount,
                    cycleLabel = cycleLabel.trim(),
                    notes = notes.trim(),
                    dateMillis = System.currentTimeMillis()
                )
            ).onSuccess {
                _snackbarMessage.value = "Pendapatan berhasil dicatat"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal menyimpan pendapatan"
            }
        }
    }

    fun deleteRevenue(revenueId: String) {
        viewModelScope.launch {
            repository.deleteRevenue(currentUserId, revenueId).onSuccess {
                _snackbarMessage.value = "Catatan pendapatan dihapus"
            }
        }
    }

    fun saveProduct(
        existingId: String,
        name: String,
        description: String,
        mainCategory: MainCategory,
        subCategory: String,
        price: Long,
        unit: String,
        stock: Long,
        freshnessStatus: String,
        deliveryMethods: String,
        promoted: Boolean,
        photoUrl: String = ""
    ) {
        if (name.trim().length < 2 || price < 0L) {
            _snackbarMessage.value = "Nama produk dan harga harus valid"
            return
        }
        val profile = (userProfileState.value as? UiState.Success)?.data
        val loc = _selectedLocation.value
        viewModelScope.launch {
            val product = MarketplaceProduct(
                id = existingId,
                sellerId = currentUserId,
                sellerName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName,
                sellerArea = profile?.city ?: loc.name,
                sellerLat = profile?.latitude ?: loc.latitude,
                sellerLng = profile?.longitude ?: loc.longitude,
                mainCategory = mainCategory.name,
                subCategory = subCategory,
                name = name.trim(),
                description = description.trim(),
                price = price,
                unit = unit.trim().ifBlank { "kg" },
                stock = stock.coerceAtLeast(0L),
                freshnessStatus = freshnessStatus.ifBlank { "Segar" },
                deliveryMethods = deliveryMethods.ifBlank { "Kurir Lokal, Ambil Langsung" },
                promoted = promoted,
                active = true,
                photoUrl = photoUrl
            )
            repository.saveProduct(product).onSuccess {
                _snackbarMessage.value = if (existingId.isBlank()) "Produk ditambahkan ke Marketplace" else "Produk diperbarui"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal menyimpan produk"
            }
        }
    }

    fun toggleProductActive(productId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleProductActive(productId, active)
        }
    }

    fun addToCart(product: MarketplaceProduct, quantity: Long = 1L) {
        if (product.stock < quantity) {
            _snackbarMessage.value = "Stok tidak mencukupi (tersisa ${product.stock})"
            return
        }
        viewModelScope.launch {
            repository.addToCart(
                CartItem(
                    userId = currentUserId,
                    productId = product.id,
                    productName = product.name,
                    sellerId = product.sellerId,
                    sellerName = product.sellerName,
                    mainCategory = product.mainCategory,
                    unitPrice = product.price,
                    unit = product.unit,
                    quantity = quantity
                )
            ).onSuccess {
                _snackbarMessage.value = "${product.name} masuk ke keranjang"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal menambah ke keranjang"
            }
        }
    }

    fun updateCartQuantity(item: CartItem, newQty: Long) {
        viewModelScope.launch {
            repository.updateCartQuantity(currentUserId, item.id, newQty)
        }
    }

    fun checkoutCart(
        cartItems: List<CartItem>,
        deliveryMethod: String,
        deliveryAddress: String,
        paymentMethod: String
    ) {
        if (cartItems.isEmpty()) {
            _snackbarMessage.value = "Keranjang masih kosong"
            return
        }
        if (deliveryAddress.isBlank()) {
            _snackbarMessage.value = "Alamat pengiriman wajib diisi"
            return
        }
        val profile = (userProfileState.value as? UiState.Success)?.data
        val buyerName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName
        val settings = adminSettingsState.value
        val groupedBySeller = cartItems.groupBy { it.sellerId }

        viewModelScope.launch {
            groupedBySeller.values.forEach { items ->
                val first = items.first()
                val subtotal = items.sumOf { it.unitPrice * it.quantity }
                val shippingFee = if (deliveryMethod == "Ambil Langsung" || first.mainCategory == MainCategory.JASA.name) 0L else 12000L
                val serviceFee = 2000L
                val commissionPct = when (first.mainCategory) {
                    MainCategory.PERLENGKAPAN.name -> settings.supplierCommissionPct
                    MainCategory.JASA.name -> settings.consultationCommissionPct
                    else -> settings.marketplaceCommissionPct
                }
                val commissionAmount = ((subtotal * commissionPct) / 100.0).toLong()
                val summary = items.joinToString(", ") { "${it.productName} x${it.quantity} ${it.unit}" }

                val order = MarketplaceOrder(
                    buyerId = currentUserId,
                    buyerName = buyerName,
                    sellerId = first.sellerId,
                    sellerName = first.sellerName,
                    mainCategory = first.mainCategory,
                    itemsSummary = summary,
                    subtotal = subtotal,
                    shippingFee = shippingFee,
                    serviceFee = serviceFee,
                    commissionAmount = commissionAmount,
                    totalAmount = subtotal + shippingFee + serviceFee,
                    paymentMethod = paymentMethod,
                    paymentStatus = if (isDemoMode) PaymentStatus.SIMULASI_DEMO.name else PaymentStatus.MENUNGGU.name,
                    orderStatus = OrderStatus.MENUNGGU_KONFIRMASI.name,
                    deliveryMethod = deliveryMethod,
                    deliveryAddress = deliveryAddress.trim(),
                    isDemo = isDemoMode
                )
                repository.placeOrder(order, items).onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Checkout gagal"
                    return@launch
                }
            }
            _snackbarMessage.value = if (isDemoMode) {
                "Pesanan DEMO dibuat (simulasi pembayaran tanpa uang sungguhan)"
            } else {
                "Pesanan dibuat. Menunggu konfirmasi pembayaran & penjual."
            }
        }
    }

    fun advanceOrderStatus(order: MarketplaceOrder, nextStatus: OrderStatus) {
        viewModelScope.launch {
            val newPaymentStatus = if (nextStatus == OrderStatus.SELESAI && !isDemoMode) {
                PaymentStatus.TERKONFIRMASI
            } else null
            repository.updateOrderStatus(order.id, nextStatus, newPaymentStatus).onSuccess {
                _snackbarMessage.value = "Status pesanan diperbarui ke ${nextStatus.label}"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal memperbarui pesanan"
            }
        }
    }

    fun addProductReview(productId: String, rating: Int, comment: String) {
        if (comment.isBlank()) return
        val profile = (userProfileState.value as? UiState.Success)?.data
        val newReview = ProductReview(
            id = "rev_${UUID.randomUUID().toString().take(6)}",
            productId = productId,
            reviewerName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName,
            rating = rating.coerceIn(1, 5),
            comment = comment.trim(),
            dateLabel = "Baru saja"
        )
        _reviews.update { listOf(newReview) + it }
        _snackbarMessage.value = "Ulasan Anda ditambahkan"
    }

    fun sendSellerChat(sellerId: String, sellerName: String, message: String) {
        if (message.isBlank()) return
        val profile = (userProfileState.value as? UiState.Success)?.data
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString().take(8),
            sellerId = sellerId,
            senderName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName,
            fromUser = true,
            text = message.trim(),
            timeLabel = "Baru saja"
        )
        _sellerChats.update { map ->
            val current = map[sellerId].orEmpty()
            val reply = if (isDemoMode) {
                listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString().take(8),
                        sellerId = sellerId,
                        senderName = sellerName,
                        fromUser = false,
                        text = "[DEMO Balasan Otomatis] Halo! Stok produk kami tersedia untuk pengiriman lokal hari ini. Ada jumlah tertentu yang dibutuhkan?",
                        timeLabel = "Baru saja"
                    )
                )
            } else emptyList()
            map + (sellerId to (current + userMsg + reply))
        }
    }

    fun submitWholesaleOffer(requestId: String, offerPricePerKg: Long) {
        viewModelScope.launch {
            repository.submitWholesaleOffer(requestId, currentUserId, offerPricePerKg).onSuccess {
                _snackbarMessage.value = "Penawaran harga grosir terkirim"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Gagal mengirim penawaran"
            }
        }
    }

    fun createWholesaleRequest(
        company: String,
        buyerType: String,
        cropNeeded: String,
        quantityKg: Long,
        targetPrice: Long,
        schedule: String,
        area: String
    ) {
        if (company.isBlank() || cropNeeded.isBlank()) {
            _snackbarMessage.value = "Lengkapi nama usaha dan komoditas"
            return
        }
        viewModelScope.launch {
            repository.createWholesaleRequest(
                WholesaleRequest(
                    buyerId = currentUserId,
                    buyerCompany = company.trim(),
                    buyerType = buyerType,
                    cropNeeded = cropNeeded.trim(),
                    quantityKg = quantityKg.coerceAtLeast(1L),
                    targetPricePerKg = targetPrice.coerceAtLeast(0L),
                    deliverySchedule = schedule.trim().ifBlank { "Mingguan" },
                    area = area.trim().ifBlank { _selectedLocation.value.name },
                    status = WholesaleStatus.TERBUKA.name,
                    open = true
                )
            ).onSuccess {
                _snackbarMessage.value = "Permintaan pengadaan grosir dipublikasikan"
            }
        }
    }

    fun createForumPost(title: String, content: String, topic: String) {
        if (title.trim().length < 3 || content.trim().length < 3) {
            _snackbarMessage.value = "Judul dan isi diskusi minimal 3 karakter"
            return
        }
        val profile = (userProfileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.createForumPost(
                ForumPost(
                    authorId = currentUserId,
                    authorName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName,
                    title = title.trim(),
                    content = content.trim(),
                    topic = topic,
                    visible = true
                )
            ).onSuccess {
                _snackbarMessage.value = "Topik diskusi berhasil dibuat"
            }
        }
    }

    fun bookConsultation(expert: ExpertConsultant) {
        _consultationsBooked.update { it + expert.id }
        _snackbarMessage.value = "Jadwal konsultasi dengan ${expert.name} tercatat (${if (isDemoMode) "DEMO" else "Menunggu jadwal"})"
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun updateAdminCommission(setting: AdminSetting) {
        viewModelScope.launch {
            repository.updateAdminSettings(setting).onSuccess {
                _snackbarMessage.value = "Pengaturan komisi platform diperbarui"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Membutuhkan hak akses Admin"
            }
        }
    }

    fun askAiAssistant(prompt: String, plantContext: String? = null) {
        if (prompt.isBlank() || _aiStreamingText.value != null) return
        _aiError.value = null
        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            fromUser = true,
            text = prompt.trim()
        )
        _aiMessages.update { it + userMessage }
        _aiStreamingText.value = ""

        val fullPrompt = if (!plantContext.isNullOrBlank()) {
            "Konteks data tanaman aktual yang dimasukkan pengguna:\n$plantContext\n\nPertanyaan pengguna: ${prompt.trim()}"
        } else {
            prompt.trim()
        }

        viewModelScope.launch {
            aiRepository.streamAdvice(fullPrompt).collect { result ->
                result.onSuccess { partial ->
                    _aiStreamingText.value = partial
                }.onFailure { err ->
                    _aiStreamingText.value = null
                    _aiError.value = err.localizedMessage ?: "Gagal mendapatkan respons AI"
                }
            }
            val finalText = _aiStreamingText.value
            if (!finalText.isNullOrBlank()) {
                _aiMessages.update {
                    it + AiChatMessage(
                        id = UUID.randomUUID().toString(),
                        fromUser = false,
                        text = finalText
                    )
                }
                _aiStreamingText.value = null
            }
        }
    }

    fun clearAiError() {
        _aiError.value = null
    }

    private companion object {
        const val TAG = "HydroGrowVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
