package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters
import com.example.viewmodel.HydroGrowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: HydroGrowViewModel,
    plants: List<HydroPlant>,
    monitoringLogs: List<PlantMonitoringLog>,
    messages: List<AiChatMessage>,
    streamingText: String?,
    aiError: String?,
    initialContextText: String? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var promptInput by remember { mutableStateOf("") }
    var selectedPlantId by remember { mutableStateOf(plants.firstOrNull()?.id.orEmpty()) }
    val listState = rememberLazyListState()

    val selectedPlant = remember(plants, selectedPlantId) { plants.find { it.id == selectedPlantId } }
    val latestLog = remember(selectedPlant, monitoringLogs) {
        selectedPlant?.let { p ->
            monitoringLogs.filter { it.plantId == p.id }.maxByOrNull { it.recordedAtMillis }
        }
    }

    LaunchedEffect(initialContextText) {
        if (!initialContextText.isNullOrBlank()) {
            promptInput = initialContextText
        }
    }

    LaunchedEffect(messages.size, streamingText) {
        val total = messages.size + if (streamingText != null) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    fun buildActualPlantContext(): String? {
        val plant = selectedPlant ?: return null
        val logText = if (latestLog != null) {
            "Log monitoring terakhir (${Formatters.formatDateTime(latestLog.recordedAtMillis)}): " +
                "pH=${latestLog.ph}, EC=${latestLog.ec} mS/cm, TDS=${latestLog.tds} ppm, " +
                "Suhu Air=${latestLog.waterTemp}°C, Suhu Ruang=${latestLog.ambientTemp}°C, " +
                "Tinggi=${latestLog.heightCm} cm, Akar=${latestLog.rootCondition}, Daun=${latestLog.plantCondition}"
        } else {
            "Belum ada catatan monitoring sensor (pH/EC/TDS belum diinput pengguna)."
        }
        return "Tanaman: ${plant.name} (${plant.cropType}), Sistem: ${plant.systemType}, " +
            "Lubang: ${plant.holeCount}, Target pH: ${plant.phMin}-${plant.phMax}, Target EC: ${plant.ecMin}-${plant.ecMax}. $logText"
    }

    val quickPrompts = remember {
        listOf(
            "Analisis kondisi pH, EC & pertumbuhan tanaman saya saat ini",
            "Daun tanaman terlihat agak layu dan menguning, apa langkah penanganannya?",
            "Bantu hitung estimasi modal awal instalasi NFT 100 lubang",
            "Buatkan simulasi perkiraan keuntungan panen selada 100 lubang",
            "Susun rencana jadwal tanam & perawatan 30 hari ke depan"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Asisten AI HydroGrow")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Surface(
                color = AmberContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Rekomendasi AI bersifat panduan edukatif dan bukan diagnosis laboratorium pasti.",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnAmberContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (plants.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedPlantId.isBlank(),
                            onClick = { selectedPlantId = "" },
                            label = { Text("Tanpa Konteks", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(plants, key = { it.id }) { p ->
                        FilterChip(
                            selected = selectedPlantId == p.id,
                            onClick = { selectedPlantId = p.id },
                            label = { Text(p.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    AiMessageBubble(msg.fromUser, msg.text)
                }
                if (streamingText != null) {
                    item {
                        AiMessageBubble(
                            fromUser = false,
                            text = streamingText.ifBlank { "Menyusun analisis hidroponik..." }
                        )
                    }
                }
            }

            if (aiError != null) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    ErrorStateCard(message = aiError)
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickPrompts) { qp ->
                    AssistChip(
                        onClick = {
                            viewModel.clearAiError()
                            viewModel.askAiAssistant(qp, buildActualPlantContext())
                        },
                        label = { Text(qp.take(32) + if (qp.length > 32) "..." else "", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("Tanya nutrisi, hama, modal, atau panen...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input")
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val toSend = promptInput.trim()
                        if (toSend.isNotBlank()) {
                            promptInput = ""
                            viewModel.clearAiError()
                            viewModel.askAiAssistant(toSend, buildActualPlantContext())
                        }
                    },
                    enabled = streamingText == null && promptInput.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim pertanyaan")
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(fromUser: Boolean, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (fromUser) 18.dp else 4.dp,
                bottomEnd = if (fromUser) 4.dp else 18.dp
            ),
            tonalElevation = if (fromUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: HydroGrowViewModel,
    isDemoMode: Boolean,
    adminSetting: AdminSetting,
    orders: List<MarketplaceOrder>,
    products: List<MarketplaceProduct>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var marketPct by remember(adminSetting) { mutableStateOf(adminSetting.marketplaceCommissionPct.toString()) }
    var supplierPct by remember(adminSetting) { mutableStateOf(adminSetting.supplierCommissionPct.toString()) }
    var deliveryPct by remember(adminSetting) { mutableStateOf(adminSetting.deliveryCommissionPct.toString()) }
    var consultPct by remember(adminSetting) { mutableStateOf(adminSetting.consultationCommissionPct.toString()) }
    var wholesalePct by remember(adminSetting) { mutableStateOf(adminSetting.wholesaleCommissionPct.toString()) }

    val verifiedSellers = remember { mutableStateMapOf("demo_seller_bandung_1" to true, "demo_supplier_1" to true) }

    val totalGmv = orders.sumOf { it.subtotal }
    val totalCommission = orders.sumOf { it.commissionAmount }
    val harvestCommission = orders.filter { it.mainCategory == MainCategory.HASIL_PANEN.name }.sumOf { it.commissionAmount }
    val supplyCommission = orders.filter { it.mainCategory == MainCategory.PERLENGKAPAN.name }.sumOf { it.commissionAmount }
    val serviceCommission = orders.filter { it.mainCategory == MainCategory.JASA.name }.sumOf { it.commissionAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Platform & Monetisasi") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Surface(color = AmberContainer, shape = RoundedCornerShape(14.dp)) {
                    Text(
                        text = if (isDemoMode) {
                            "Ringkasan DEMO: GMV transaksi milik penjual dipisahkan secara tegas dari pendapatan komisi platform."
                        } else {
                            "GMV merupakan total nilai transaksi penjual; pendapatan platform hanya dihitung dari komisi layanan."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnAmberContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricKpiCard(
                        title = "Total GMV Penjual",
                        value = Formatters.rupiah(totalGmv),
                        subtitle = "${orders.size} transaksi tercatat",
                        icon = Icons.Filled.Storefront,
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        title = "Komisi Platform",
                        value = Formatters.rupiah(totalCommission),
                        subtitle = "Pendapatan bersih aplikasi",
                        icon = Icons.Filled.AccountBalanceWallet,
                        accentColor = OrangeHighlight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Rincian Sumber Pendapatan Platform", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Komisi Penjualan Hasil Panen", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.rupiah(harvestCommission), style = MaterialTheme.typography.labelLarge)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Komisi Supplier Perlengkapan", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.rupiah(supplyCommission), style = MaterialTheme.typography.labelLarge)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Komisi Jasa & Konsultasi", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.rupiah(serviceCommission), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pengaturan Persentase Komisi (%)", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = marketPct,
                                onValueChange = { marketPct = it },
                                label = { Text("Hasil Panen %") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = supplierPct,
                                onValueChange = { supplierPct = it },
                                label = { Text("Supplier %") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = deliveryPct,
                                onValueChange = { deliveryPct = it },
                                label = { Text("Pengiriman %") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = consultPct,
                                onValueChange = { consultPct = it },
                                label = { Text("Konsultasi %") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = wholesalePct,
                            onValueChange = { wholesalePct = it },
                            label = { Text("Kemitraan Grosir (B2B) %") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.updateAdminCommission(
                                    AdminSetting(
                                        marketplaceCommissionPct = marketPct.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 5.0,
                                        supplierCommissionPct = supplierPct.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 5.0,
                                        deliveryCommissionPct = deliveryPct.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 5.0,
                                        consultationCommissionPct = consultPct.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 10.0,
                                        wholesaleCommissionPct = wholesalePct.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 3.0,
                                        publicConfig = true
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("admin_save_commission_button")
                        ) {
                            Text("Simpan Pengaturan Komisi")
                        }
                    }
                }
            }

            item {
                Text("Verifikasi Penjual & Moderasi Produk", style = MaterialTheme.typography.titleMedium)
            }

            items(products.distinctBy { it.sellerId }, key = { "seller_${it.sellerId}" }) { sellerProd ->
                val verified = verifiedSellers[sellerProd.sellerId] ?: false
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sellerProd.sellerName, style = MaterialTheme.typography.titleSmall)
                            Text(sellerProd.sellerArea, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(
                                text = if (verified) "Terverifikasi" else "Menunggu",
                                containerColor = if (verified) MaterialTheme.colorScheme.primaryContainer else AmberContainer,
                                contentColor = if (verified) MaterialTheme.colorScheme.onPrimaryContainer else OnAmberContainer
                            )
                            Switch(
                                checked = verified,
                                onCheckedChange = { checked ->
                                    verifiedSellers[sellerProd.sellerId] = checked
                                    viewModel.showMessage(
                                        if (checked) "${sellerProd.sellerName} diverifikasi" else "Verifikasi ${sellerProd.sellerName} ditangguhkan"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            items(products.take(6), key = { "mod_${it.id}" }) { prod ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${Formatters.rupiah(prod.price)} • ${prod.sellerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prod.active,
                            onCheckedChange = { active -> viewModel.toggleProductActive(prod.id, active) }
                        )
                    }
                }
            }
        }
    }
}
