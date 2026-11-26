package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String, val subtitle: String) {
    PETANI("Petani / Pelaku Usaha", "Kelola kebun, pantau pH/EC, jual hasil panen & catat keuangan"),
    PEMBELI("Pembeli / Konsumen", "Belanja sayur hidroponik segar terdekat & lacak pesanan"),
    SUPPLIER("Supplier Perlengkapan", "Jual benih, nutrisi AB Mix, rockwool, & instalasi hidroponik"),
    MITRA_PENGIRIMAN("Mitra Pengiriman", "Antar pesanan hasil panen lokal & pantau komisi kirim"),
    ADMIN("Admin Platform", "Pantau GMV, atur komisi, verifikasi penjual & moderasi")
}

enum class PlantStatus(val label: String) {
    SEMAI("Fase Semai"),
    VEGETATIF("Fase Vegetatif"),
    SIAP_PANEN("Siap Panen"),
    PANEN_SELESAI("Panen Selesai")
}

enum class HealthStatus(val label: String) {
    NORMAL("Normal"),
    PERLU_DIPERIKSA("Perlu Diperiksa"),
    PERINGATAN("Peringatan")
}

enum class MainCategory(val label: String) {
    HASIL_PANEN("Hasil Panen"),
    PERLENGKAPAN("Perlengkapan"),
    JASA("Jasa Hidroponik")
}

enum class PaymentStatus(val label: String) {
    MENUNGGU("Menunggu Pembayaran"),
    TERKONFIRMASI("Pembayaran Terkonfirmasi"),
    SIMULASI_DEMO("Simulasi Demo (Lunas)")
}

enum class OrderStatus(val label: String) {
    MENUNGGU_KONFIRMASI("Menunggu Konfirmasi"),
    DIPROSES("Diproses Penjual"),
    DIKIRIM("Dalam Pengiriman"),
    SELESAI("Selesai"),
    DIBATALKAN("Dibatalkan")
}

enum class ExpenseType(val label: String) {
    MODAL("Modal Usaha"),
    PENGELUARAN("Pengeluaran Operasional")
}

enum class WholesaleStatus(val label: String) {
    TERBUKA("Terbuka"),
    PENAWARAN("Menunggu Persetujuan"),
    DISEPAKATI("Disepakati"),
    SELESAI("Selesai")
}

enum class AppTab(val label: String) {
    BERANDA("Beranda"),
    KEBUN("Kebun"),
    MARKETPLACE("Pasar"),
    KEUANGAN("Keuangan"),
    PROFIL("Profil")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = UserRole.PETANI.name,
    val address: String = "",
    val city: String = "Bandung",
    val latitude: Double = -6.9175,
    val longitude: Double = 107.6191,
    val photoUrl: String = "",
    val verifiedSeller: Boolean = false,
    val verifiedSupplier: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HydroPlant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val cropType: String = "Selada Hijau",
    val systemType: String = "NFT",
    val holeCount: Long = 40L,
    val plantedDateMillis: Long = System.currentTimeMillis(),
    val transplantDateMillis: Long = System.currentTimeMillis(),
    val harvestAgeDays: Long = 35L,
    val status: String = PlantStatus.VEGETATIF.name,
    val phMin: Double = 5.5,
    val phMax: Double = 6.5,
    val ecMin: Double = 0.8,
    val ecMax: Double = 1.4,
    val photoUrl: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MaintenanceSchedule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val plantId: String = "",
    val plantName: String = "",
    val taskType: String = "Cek pH",
    val title: String = "",
    val dueDateMillis: Long = System.currentTimeMillis(),
    val completed: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlantMonitoringLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val plantId: String = "",
    val plantName: String = "",
    val ph: Double = 6.0,
    val ec: Double = 1.2,
    val tds: Long = 600L,
    val waterTemp: Double = 24.0,
    val ambientTemp: Double = 27.0,
    val heightCm: Double = 10.0,
    val rootCondition: String = "Putih bersih",
    val plantCondition: String = "Segar dan tegak",
    val healthStatus: String = HealthStatus.NORMAL.name,
    val notes: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HarvestRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val plantId: String = "",
    val plantName: String = "",
    val cropType: String = "",
    val cycleLabel: String = "",
    val weightKg: Double = 0.0,
    val holeCount: Long = 0L,
    val durationDays: Long = 35L,
    val notes: String = "",
    val harvestedAtMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketplaceProduct(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerArea: String = "",
    val sellerLat: Double = -6.9175,
    val sellerLng: Double = 107.6191,
    val mainCategory: String = MainCategory.HASIL_PANEN.name,
    val subCategory: String = "Selada",
    val name: String = "",
    val description: String = "",
    val price: Long = 0L,
    val unit: String = "kg",
    val stock: Long = 0L,
    val freshnessStatus: String = "Panen Segar Hari Ini",
    val deliveryMethods: String = "Kurir Lokal, Ambil Langsung",
    val rating: Double = 4.8,
    val reviewCount: Long = 0L,
    val promoted: Boolean = false,
    val active: Boolean = true,
    val photoUrl: String = "",
    val isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketplaceOrder(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val courierId: String = "",
    val mainCategory: String = MainCategory.HASIL_PANEN.name,
    val itemsSummary: String = "",
    val subtotal: Long = 0L,
    val shippingFee: Long = 0L,
    val serviceFee: Long = 0L,
    val commissionAmount: Long = 0L,
    val totalAmount: Long = 0L,
    val paymentMethod: String = "QRIS",
    val paymentStatus: String = PaymentStatus.SIMULASI_DEMO.name,
    val orderStatus: String = OrderStatus.MENUNGGU_KONFIRMASI.name,
    val deliveryMethod: String = "Kurir Lokal",
    val deliveryAddress: String = "",
    val isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinancialExpense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val entryType: String = ExpenseType.PENGELUARAN.name,
    val category: String = "Nutrisi",
    val title: String = "",
    val amount: Long = 0L,
    val cycleLabel: String = "",
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinancialRevenue(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "Penjualan Hasil Panen",
    val title: String = "",
    val amount: Long = 0L,
    val orderId: String = "",
    val cycleLabel: String = "",
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val mainCategory: String = MainCategory.HASIL_PANEN.name,
    val unitPrice: Long = 0L,
    val unit: String = "kg",
    val quantity: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "INFO",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WholesaleRequest(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerCompany: String = "",
    val buyerType: String = "Restoran",
    val cropNeeded: String = "",
    val quantityKg: Long = 10L,
    val targetPricePerKg: Long = 18000L,
    val deliverySchedule: String = "",
    val area: String = "",
    val farmerId: String = "",
    val farmerOfferPrice: Long = 0L,
    val status: String = WholesaleStatus.TERBUKA.name,
    val open: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ForumPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val content: String = "",
    val topic: String = "Nutrisi & pH",
    val replyCount: Long = 0L,
    val visible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminSetting(
    @DocumentId val id: String = "platform_fees",
    val marketplaceCommissionPct: Double = 5.0,
    val supplierCommissionPct: Double = 5.0,
    val deliveryCommissionPct: Double = 5.0,
    val consultationCommissionPct: Double = 10.0,
    val wholesaleCommissionPct: Double = 3.0,
    val publicConfig: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Local / Rich UI models
data class CropPreset(
    val cropType: String,
    val defaultPhMin: Double,
    val defaultPhMax: Double,
    val defaultEcMin: Double,
    val defaultEcMax: Double,
    val defaultHarvestDays: Long,
    val estimatedKgPerHole: Double
)

data class CityLocation(
    val name: String,
    val areaLabel: String,
    val latitude: Double,
    val longitude: Double
)

data class ProductReview(
    val id: String,
    val productId: String,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val dateLabel: String
)

data class ChatMessage(
    val id: String,
    val sellerId: String,
    val senderName: String,
    val fromUser: Boolean,
    val text: String,
    val timeLabel: String
)

data class EducationArticle(
    val id: String,
    val title: String,
    val category: String,
    val readMinutes: Int,
    val summary: String,
    val content: String,
    val videoTitle: String,
    val videoDuration: String
)

data class ExpertConsultant(
    val id: String,
    val name: String,
    val specialty: String,
    val experienceYears: Int,
    val feePerSession: Long,
    val rating: Double,
    val area: String
)

data class AiChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class HarvestPrediction(
    val estimatedHarvestMillis: Long,
    val daysRemaining: Long,
    val estimatedReadyHoles: Long,
    val estimatedWeightKg: Double,
    val readinessLabel: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
