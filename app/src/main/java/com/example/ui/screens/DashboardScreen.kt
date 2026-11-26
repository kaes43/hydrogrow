package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.DemoSeedData
import com.example.data.LocationHelper
import com.example.model.*
import com.example.ui.components.DemoModeBanner
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.LeafGreenDeep
import com.example.ui.theme.LeafGreenPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OrangeHighlight
import com.example.util.Formatters
import com.example.util.HydroPhysics
import com.example.viewmodel.HydroGrowViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: HydroGrowViewModel,
    userRole: UserRole,
    userName: String,
    isDemoMode: Boolean,
    plants: List<HydroPlant>,
    schedules: List<MaintenanceSchedule>,
    monitoringLogs: List<PlantMonitoringLog>,
    products: List<MarketplaceProduct>,
    buyerOrders: List<MarketplaceOrder>,
    sellerOrders: List<MarketplaceOrder>,
    expenses: List<FinancialExpense>,
    revenues: List<FinancialRevenue>,
    notifications: List<AppNotification>,
    selectedLocation: CityLocation,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenWholesale: () -> Unit,
    onOpenEducation: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSwitchRoleClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLocationDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                LocationHelper.fetchCoarseLocation(context).onSuccess { result ->
                    viewModel.selectLocation(
                        CityLocation(result.cityLabel, result.cityLabel, result.latitude, result.longitude)
                    )
                    viewModel.showMessage("Lokasi diperbarui: ${result.cityLabel}")
                }.onFailure { err ->
                    viewModel.showMessage(err.localizedMessage ?: "Gagal membaca GPS, gunakan pilih kota manual")
                }
            }
        } else {
            viewModel.showMessage("Izin lokasi ditolak. Silakan pilih lokasi secara manual.")
        }
    }

    val unreadCount = notifications.count { !it.read }
    val activePlants = plants.filter { it.status != PlantStatus.PANEN_SELESAI.name }
    val totalHoles = activePlants.sumOf { it.holeCount }
    val pendingTasks = schedules.count { !it.completed }
    val nearestHarvest = activePlants
        .map { it to HydroPhysics.predictHarvest(it, monitoringLogs) }
        .minByOrNull { it.second.daysRemaining }

    val monthRevenue = revenues.sumOf { it.amount }
    val monthExpense = expenses.filter { it.entryType == ExpenseType.PENGELUARAN.name }.sumOf { it.amount }
    val estimatedProfit = monthRevenue - monthExpense
    val incomingOrdersCount = sellerOrders.count { it.orderStatus != OrderStatus.SELESAI.name && it.orderStatus != OrderStatus.DIBATALKAN.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp)
    ) {
        if (isDemoMode) {
            item {
                DemoModeBanner(onSwitchRoleClick = onSwitchRoleClick)
            }
        }

        // Top Header: Greeting, Location Chip, Notification Bell
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Halo, ${userName.substringBefore(" (")}",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showLocationDialog = true }
                            .padding(vertical = 2.dp)
                            .testTag("dashboard_location_chip")
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Pilih lokasi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${selectedLocation.name} • ${userRole.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = onOpenAiAssistant,
                        modifier = Modifier.testTag("dashboard_ai_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Asisten AI HydroGrow")
                    }
                    Spacer(Modifier.width(6.dp))
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge { Text(unreadCount.toString()) }
                            }
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("dashboard_notifications_button")
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifikasi")
                        }
                    }
                }
            }
        }

        // Global Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari sayur, nutrisi, alat, atau jasa hidroponik...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Bersihkan pencarian")
                        }
                    } else {
                        IconButton(onClick = { onNavigateTab(AppTab.MARKETPLACE) }) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Buka Pasar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input")
            )
            if (searchQuery.isNotBlank()) {
                val matched = products.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.subCategory.contains(searchQuery, ignoreCase = true)
                }.take(4)
                if (matched.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Hasil Pencarian Cepat", style = MaterialTheme.typography.labelLarge)
                            matched.forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateTab(AppTab.MARKETPLACE) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(prod.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        "${Formatters.rupiah(prod.price)}/${prod.unit}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(165.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_hydroponic),
                        contentDescription = "Kebun Hidroponik HydroGrow",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(LeafGreenDeep.copy(alpha = 0.90f), Color.Transparent)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusBadge(
                            text = when (userRole) {
                                UserRole.PETANI -> "SMART FARMING + FINTECH"
                                UserRole.PEMBELI -> "PANEN SEGAR TERDEKAT"
                                UserRole.SUPPLIER -> "MITRA SUPPLIER HIDROPONIK"
                                UserRole.MITRA_PENGIRIMAN -> "KURIR LOKAL CEPAT"
                                UserRole.ADMIN -> "ADMIN KONTROL PLATFORM"
                            },
                            containerColor = MintLight,
                            contentColor = LeafGreenDeep
                        )
                        Column {
                            Text(
                                text = when (userRole) {
                                    UserRole.PETANI -> "${activePlants.size} Tanaman Aktif • $totalHoles Lubang"
                                    UserRole.PEMBELI -> "Belanja Langsung dari Petani ${selectedLocation.name}"
                                    UserRole.SUPPLIER -> "Jual Benih, AB Mix & Instalasi"
                                    UserRole.MITRA_PENGIRIMAN -> "Antar Hasil Panen Segar Hari Ini"
                                    UserRole.ADMIN -> "Pantau GMV & Komisi Aplikasi"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Kelola Kebun, Kembangkan Usaha, Temukan Pasar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }

        // Role-Adaptive Dashboard Content
        when (userRole) {
            UserRole.PETANI -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricKpiCard(
                            title = "Tanaman Aktif",
                            value = "${activePlants.size} Blok",
                            subtitle = "$totalHoles lubang tanam",
                            icon = Icons.Filled.Eco,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.KEBUN) }
                        )
                        MetricKpiCard(
                            title = "Jadwal Hari Ini",
                            value = "$pendingTasks Tugas",
                            subtitle = "Perawatan kebun",
                            icon = Icons.Filled.EventAvailable,
                            accentColor = OrangeHighlight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.KEBUN) }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricKpiCard(
                            title = "Panen Terdekat",
                            value = nearestHarvest?.let { "${it.second.daysRemaining} Hari" } ?: "-",
                            subtitle = nearestHarvest?.first?.cropType ?: "Belum ada tanaman",
                            icon = Icons.Filled.Agriculture,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.KEBUN) }
                        )
                        MetricKpiCard(
                            title = "Pesanan Masuk",
                            value = "$incomingOrdersCount Order",
                            subtitle = "Perlu diproses",
                            icon = Icons.Filled.ShoppingBag,
                            accentColor = OrangeHighlight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.MARKETPLACE) }
                        )
                    }
                }
                // Financial Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateTab(AppTab.KEUANGAN) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ringkasan Keuangan Usaha", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { onNavigateTab(AppTab.KEUANGAN) }) {
                                    Text("Detail")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Penjualan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.rupiah(monthRevenue), style = MaterialTheme.typography.titleSmall, color = LeafGreenPrimary)
                                }
                                Column {
                                    Text("Pengeluaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.rupiah(monthExpense), style = MaterialTheme.typography.titleSmall, color = OrangeHighlight)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Estimasi Laba", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        Formatters.rupiah(estimatedProfit),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            UserRole.PEMBELI -> {
                item {
                    val nearbyHarvests = products
                        .filter { it.mainCategory == MainCategory.HASIL_PANEN.name }
                        .sortedBy {
                            HydroPhysics.calculateDistanceKm(
                                selectedLocation.latitude,
                                selectedLocation.longitude,
                                it.sellerLat,
                                it.sellerLng
                            )
                        }
                    Text("Produk Hidroponik Terdekat", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(nearbyHarvests.take(5), key = { it.id }) { prod ->
                            val dist = HydroPhysics.calculateDistanceKm(
                                selectedLocation.latitude, selectedLocation.longitude,
                                prod.sellerLat, prod.sellerLng
                            )
                            Card(
                                modifier = Modifier
                                    .width(190.dp)
                                    .clickable { onNavigateTab(AppTab.MARKETPLACE) },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (prod.promoted) {
                                        StatusBadge("PROMOSI", AmberContainer, OnAmberContainer)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    Text(prod.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${prod.sellerArea} • ${Formatters.formatDistanceKm(dist)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(6.dp))
                                    Text("${Formatters.rupiah(prod.price)}/${prod.unit}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item {
                    val activeOrders = buyerOrders.filter { it.orderStatus != OrderStatus.SELESAI.name }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricKpiCard(
                            title = "Pesanan Berjalan",
                            value = "${activeOrders.size} Pesanan",
                            subtitle = "Lacak pengiriman",
                            icon = Icons.Filled.LocalShipping,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.MARKETPLACE) }
                        )
                        MetricKpiCard(
                            title = "Riwayat Belanja",
                            value = "${buyerOrders.size} Total",
                            subtitle = "Lihat semua transaksi",
                            icon = Icons.Filled.ReceiptLong,
                            accentColor = OrangeHighlight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.MARKETPLACE) }
                        )
                    }
                }
            }

            UserRole.SUPPLIER -> {
                val supplyProducts = products.filter { it.mainCategory == MainCategory.PERLENGKAPAN.name }
                val totalStock = supplyProducts.sumOf { it.stock }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricKpiCard(
                            title = "Produk Perlengkapan",
                            value = "${supplyProducts.size} SKU",
                            subtitle = "Total stok: $totalStock unit",
                            icon = Icons.Filled.Inventory2,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.MARKETPLACE) }
                        )
                        MetricKpiCard(
                            title = "Pesanan Masuk",
                            value = "$incomingOrdersCount Order",
                            subtitle = "Pendapatan: ${Formatters.rupiah(monthRevenue)}",
                            icon = Icons.Filled.Storefront,
                            accentColor = OrangeHighlight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.MARKETPLACE) }
                        )
                    }
                }
            }

            UserRole.MITRA_PENGIRIMAN -> {
                val activeDeliveries = sellerOrders.count { it.orderStatus == OrderStatus.DIKIRIM.name || it.orderStatus == OrderStatus.DIPROSES.name }
                val finishedDeliveries = sellerOrders.count { it.orderStatus == OrderStatus.SELESAI.name }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricKpiCard(
                            title = "Siap Diantar",
                            value = "$activeDeliveries Paket",
                            subtitle = "Rute sekitar ${selectedLocation.name}",
                            icon = Icons.Filled.TwoWheeler,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.MARKETPLACE) }
                        )
                        MetricKpiCard(
                            title = "Selesai Antar",
                            value = "$finishedDeliveries Paket",
                            subtitle = "Komisi kurir lokal",
                            icon = Icons.Filled.CheckCircle,
                            accentColor = OrangeHighlight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.MARKETPLACE) }
                        )
                    }
                }
            }

            UserRole.ADMIN -> {
                val gmv = sellerOrders.sumOf { it.subtotal }
                val commissionTotal = sellerOrders.sumOf { it.commissionAmount }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricKpiCard(
                            title = "Total GMV Penjual",
                            value = Formatters.rupiah(gmv),
                            subtitle = "${sellerOrders.size} transaksi",
                            icon = Icons.Filled.Analytics,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenAdmin
                        )
                        MetricKpiCard(
                            title = "Komisi Platform",
                            value = Formatters.rupiah(commissionTotal),
                            subtitle = "Terpisah dari GMV",
                            icon = Icons.Filled.AccountBalanceWallet,
                            accentColor = OrangeHighlight,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenAdmin
                        )
                    }
                }
            }
        }

        // Quick Action Shortcuts
        item {
            Text("Akses Cepat Fitur", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickShortcutChip("Sekitar Saya", Icons.Filled.Map, Modifier.weight(1f)) {
                    onNavigateTab(AppTab.MARKETPLACE)
                }
                QuickShortcutChip("Pembeli Grosir", Icons.Filled.Handshake, Modifier.weight(1f)) {
                    onOpenWholesale()
                }
                QuickShortcutChip("Edukasi & Ahli", Icons.Filled.MenuBook, Modifier.weight(1f)) {
                    onOpenEducation()
                }
                QuickShortcutChip("Asisten AI", Icons.Filled.AutoAwesome, Modifier.weight(1f)) {
                    onOpenAiAssistant()
                }
            }
        }

        // Important Notifications Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifikasi & Info Penting", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenNotifications) {
                    Text("Lihat Semua")
                }
            }
        }

        items(notifications.take(3), key = { it.id }) { notif ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (notif.read) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.markNotificationRead(notif.id)
                        onOpenNotifications()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notif.title, style = MaterialTheme.typography.titleSmall)
                        Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (showLocationDialog) {
        LocationPickerDialog(
            currentLocation = selectedLocation,
            onUseDeviceGps = {
                showLocationDialog = false
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    scope.launch {
                        LocationHelper.fetchCoarseLocation(context).onSuccess { res ->
                            viewModel.selectLocation(CityLocation(res.cityLabel, res.cityLabel, res.latitude, res.longitude))
                            viewModel.showMessage("Lokasi GPS terdeteksi: ${res.cityLabel}")
                        }.onFailure {
                            viewModel.showMessage(it.localizedMessage ?: "Lokasi GPS belum tersedia")
                        }
                    }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onSelectCity = { city ->
                viewModel.selectLocation(city)
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }
}

@Composable
private fun QuickShortcutChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LocationPickerDialog(
    currentLocation: CityLocation,
    onUseDeviceGps: () -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onDismiss: () -> Unit
) {
    var customCity by remember { mutableStateOf(currentLocation.name) }
    var customLat by remember { mutableStateOf(currentLocation.latitude.toString()) }
    var customLng by remember { mutableStateOf(currentLocation.longitude.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Lokasi Pengguna") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onUseDeviceGps,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gunakan Lokasi Perangkat (GPS)")
                }
                HorizontalDivider()
                Text("Atau pilih area demo di Indonesia:", style = MaterialTheme.typography.labelMedium)
                DemoSeedData.indonesianCities.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { city ->
                            FilterChip(
                                selected = city.name == currentLocation.name,
                                onClick = { onSelectCity(city) },
                                label = { Text(city.name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = customCity,
                    onValueChange = { customCity = it },
                    label = { Text("Nama Kota / Area Manual") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customLat,
                        onValueChange = { customLat = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customLng,
                        onValueChange = { customLng = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val lat = customLat.toDoubleOrNull() ?: currentLocation.latitude
                val lng = customLng.toDoubleOrNull() ?: currentLocation.longitude
                onSelectCity(CityLocation(customCity.ifBlank { "Area Kustom" }, customCity, lat, lng))
            }) {
                Text("Simpan Lokasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
