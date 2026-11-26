package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GrowthLineChart
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters
import com.example.util.HydroPhysics
import com.example.viewmodel.HydroGrowViewModel
import java.util.Locale

private enum class KebunTab(val label: String) {
    TANAMAN("Tanaman"),
    PERAWATAN("Perawatan"),
    MONITORING("Monitoring"),
    PANEN("Panen & Riwayat")
}

@Composable
fun KebunScreen(
    viewModel: HydroGrowViewModel,
    plants: List<HydroPlant>,
    schedules: List<MaintenanceSchedule>,
    monitoringLogs: List<PlantMonitoringLog>,
    harvests: List<HarvestRecord>,
    onAskAiForPlant: (HydroPlant, PlantMonitoringLog?) -> Unit
) {
    var activeTab by remember { mutableStateOf(KebunTab.TANAMAN) }
    var selectedCropFilter by remember { mutableStateOf("Semua") }
    var editingPlant by remember { mutableStateOf<HydroPlant?>(null) }
    var showAddPlantDialog by remember { mutableStateOf(false) }
    var deletePlantTarget by remember { mutableStateOf<HydroPlant?>(null) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showAddMonitoringDialog by remember { mutableStateOf(false) }
    var harvestTargetPlant by remember { mutableStateOf<HydroPlant?>(null) }
    var selectedChartPlantId by remember(plants) { mutableStateOf(plants.firstOrNull()?.id ?: "") }

    val cropCategories = remember(plants) {
        listOf("Semua") + plants.map { it.cropType }.distinct()
    }
    val filteredPlants = remember(plants, selectedCropFilter) {
        if (selectedCropFilter == "Semua") plants
        else plants.filter { it.cropType == selectedCropFilter }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (activeTab) {
                        KebunTab.TANAMAN -> {
                            editingPlant = null
                            showAddPlantDialog = true
                        }
                        KebunTab.PERAWATAN -> showAddScheduleDialog = true
                        KebunTab.MONITORING -> {
                            if (plants.isEmpty()) viewModel.showMessage("Tambahkan tanaman terlebih dahulu")
                            else showAddMonitoringDialog = true
                        }
                        KebunTab.PANEN -> {
                            val firstActive = plants.firstOrNull { it.status != PlantStatus.PANEN_SELESAI.name }
                            if (firstActive == null) viewModel.showMessage("Belum ada tanaman aktif untuk dipanen")
                            else harvestTargetPlant = firstActive
                        }
                    }
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = {
                    Text(
                        when (activeTab) {
                            KebunTab.TANAMAN -> "Tambah Tanaman"
                            KebunTab.PERAWATAN -> "Tambah Jadwal"
                            KebunTab.MONITORING -> "Catat pH/EC"
                            KebunTab.PANEN -> "Catat Panen"
                        }
                    )
                },
                modifier = Modifier.testTag("kebun_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Sub-tab selector
            ScrollableTabRow(
                selectedTabIndex = KebunTab.entries.indexOf(activeTab),
                edgePadding = 16.dp
            ) {
                KebunTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.label) },
                        modifier = Modifier.testTag("kebun_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (activeTab) {
                KebunTab.TANAMAN -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(cropCategories) { crop ->
                                    FilterChip(
                                        selected = selectedCropFilter == crop,
                                        onClick = { selectedCropFilter = crop },
                                        label = { Text(crop) }
                                    )
                                }
                            }
                        }

                        if (filteredPlants.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Tanaman Hidroponik",
                                    subtitle = "Tambahkan blok tanaman pertama Anda untuk memantau jadwal, nutrisi, dan estimasi panen.",
                                    actionLabel = "Tambah Tanaman",
                                    onAction = { showAddPlantDialog = true }
                                )
                            }
                        } else {
                            items(filteredPlants, key = { it.id }) { plant ->
                                val latestLog = monitoringLogs.filter { it.plantId == plant.id }.maxByOrNull { it.recordedAtMillis }
                                val prediction = HydroPhysics.predictHarvest(plant, monitoringLogs)
                                val statusEnum = PlantStatus.entries.firstOrNull { it.name == plant.status } ?: PlantStatus.VEGETATIF
                                val healthEnum = latestLog?.let { HealthStatus.entries.firstOrNull { h -> h.name == it.healthStatus } } ?: HealthStatus.NORMAL

                                ElevatedCard(
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                if (plant.photoUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = plant.photoUrl,
                                                        contentDescription = plant.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                }
                                                Column {
                                                    Text(plant.name, style = MaterialTheme.typography.titleMedium)
                                                    Text(
                                                        "${plant.cropType} • Sistem ${plant.systemType} • ${plant.holeCount} lubang",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            StatusBadge(
                                                text = statusEnum.label,
                                                containerColor = LeafGreenContainer,
                                                contentColor = OnLeafGreenContainer
                                            )
                                        }

                                        Spacer(Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Acuan pH ${plant.phMin}-${plant.phMax} • EC ${plant.ecMin}-${plant.ecMax}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val healthColor = when (healthEnum) {
                                                HealthStatus.NORMAL -> LeafGreenPrimary
                                                HealthStatus.PERLU_DIPERIKSA -> OrangeHighlight
                                                HealthStatus.PERINGATAN -> ErrorRed
                                            }
                                            Text(
                                                healthEnum.label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = healthColor
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Semai: ${Formatters.formatDate(plant.plantedDateMillis)} • Perkiraan panen: ${Formatters.formatDate(prediction.estimatedHarvestMillis)} (${prediction.readinessLabel})",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Spacer(Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onAskAiForPlant(plant, latestLog) },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Analisis AI", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    editingPlant = plant
                                                    showAddPlantDialog = true
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                            }
                                            OutlinedButton(
                                                onClick = { deletePlantTarget = plant },
                                                contentPadding = PaddingValues(horizontal = 10.dp)
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                KebunTab.PERAWATAN -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (schedules.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Jadwal Perawatan",
                                    subtitle = "Tambahkan pengingat cek pH, EC/TDS, ganti air nutrisi, atau pembersihan instalasi.",
                                    actionLabel = "Tambah Jadwal",
                                    onAction = { showAddScheduleDialog = true }
                                )
                            }
                        } else {
                            items(schedules, key = { it.id }) { sched ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (sched.completed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = sched.completed,
                                            onCheckedChange = { viewModel.toggleSchedule(sched) }
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sched.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${sched.taskType}${if (sched.plantName.isNotBlank()) " • ${sched.plantName}" else ""} • ${Formatters.formatDate(sched.dueDateMillis)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (sched.notes.isNotBlank()) {
                                                Text(sched.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteSchedule(sched.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus jadwal", tint = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                KebunTab.MONITORING -> {
                    val chartLogs = if (selectedChartPlantId.isBlank()) monitoringLogs
                    else monitoringLogs.filter { it.plantId == selectedChartPlantId }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                color = AmberContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "Catatan Panduan: Rentang pH dan EC per tanaman dapat disesuaikan di menu Edit Tanaman. Rekomendasi status bersifat panduan praktis, bukan pengganti pengukuran aktual alat kalibrasi Anda.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnAmberContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (plants.isNotEmpty()) {
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(plants, key = { it.id }) { p ->
                                        FilterChip(
                                            selected = selectedChartPlantId == p.id,
                                            onClick = { selectedChartPlantId = p.id },
                                            label = { Text(p.name) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            GrowthLineChart(logs = chartLogs)
                        }

                        if (monitoringLogs.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Catatan Monitoring",
                                    subtitle = "Catat pH, EC, TDS, suhu air, suhu lingkungan, dan tinggi tanaman secara berkala.",
                                    actionLabel = "Catat Sekarang",
                                    onAction = { showAddMonitoringDialog = true }
                                )
                            }
                        } else {
                            items(monitoringLogs.sortedByDescending { it.recordedAtMillis }, key = { it.id }) { log ->
                                val statusEnum = HealthStatus.entries.firstOrNull { it.name == log.healthStatus } ?: HealthStatus.NORMAL
                                val (bg, fg) = when (statusEnum) {
                                    HealthStatus.NORMAL -> LeafGreenContainer to OnLeafGreenContainer
                                    HealthStatus.PERLU_DIPERIKSA -> AmberContainer to OnAmberContainer
                                    HealthStatus.PERINGATAN -> MaterialTheme.colorScheme.errorContainer to ErrorRed
                                }
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(log.plantName, style = MaterialTheme.typography.titleSmall)
                                            StatusBadge(statusEnum.label, bg, fg)
                                        }
                                        Text(
                                            Formatters.formatDateTime(log.recordedAtMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "pH ${log.ph} • EC ${log.ec} mS/cm • TDS ${log.tds} ppm • Air ${log.waterTemp}°C • Lingk ${log.ambientTemp}°C • Tinggi ${log.heightCm} cm",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (log.rootCondition.isNotBlank() || log.plantCondition.isNotBlank()) {
                                            Text(
                                                "Akar: ${log.rootCondition} | Kondisi: ${log.plantCondition}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (log.notes.isNotBlank()) {
                                            Text(log.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                KebunTab.PANEN -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "Catatan Prediksi Panen: Angka estimasi berat dan tanggal panen dihitung dari umur panen & catatan pertumbuhan, dan dapat berubah sesuai kondisi aktual kebun.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        item {
                            Text("Estimasi Panen Tanaman Aktif", style = MaterialTheme.typography.titleMedium)
                        }

                        val activePlants = plants.filter { it.status != PlantStatus.PANEN_SELESAI.name }
                        if (activePlants.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Tanaman Aktif",
                                    subtitle = "Tambahkan tanaman aktif untuk melihat estimasi tanggal dan berat panen."
                                )
                            }
                        } else {
                            items(activePlants, key = { it.id }) { plant ->
                                val pred = HydroPhysics.predictHarvest(plant, monitoringLogs)
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(plant.name, style = MaterialTheme.typography.titleSmall)
                                                Text(pred.readinessLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Button(onClick = { harvestTargetPlant = plant }) {
                                                Text("Panen")
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Perkiraan Tanggal: ${Formatters.formatDate(pred.estimatedHarvestMillis)} • Siap Panen: ${pred.estimatedReadyHoles} lubang • Est. Berat: ${String.format(Locale("in", "ID"), "%.1f", pred.estimatedWeightKg)} kg",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(6.dp))
                            Text("Riwayat Budidaya & Perbandingan Siklus", style = MaterialTheme.typography.titleMedium)
                        }

                        if (harvests.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Riwayat Panen",
                                    subtitle = "Setelah panen dicatat, riwayat hasil setiap periode tanam akan muncul di sini."
                                )
                            }
                        } else {
                            items(harvests, key = { it.id }) { harv ->
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(harv.plantName, style = MaterialTheme.typography.titleSmall)
                                            StatusBadge("${harv.weightKg} kg", LeafGreenContainer, OnLeafGreenContainer)
                                        }
                                        Text(
                                            "${harv.cycleLabel} • ${harv.cropType} • ${harv.holeCount} lubang • ${harv.durationDays} hari",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (harv.notes.isNotBlank()) {
                                            Text(harv.notes, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPlantDialog) {
        PlantFormDialog(
            initial = editingPlant,
            onSave = { plant ->
                viewModel.savePlant(plant)
                showAddPlantDialog = false
            },
            onDismiss = { showAddPlantDialog = false }
        )
    }

    deletePlantTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Tanaman?",
            message = "Anda yakin ingin menghapus data tanaman \"${target.name}\"?",
            onConfirm = {
                viewModel.deletePlant(target.id)
                deletePlantTarget = null
            },
            onDismiss = { deletePlantTarget = null }
        )
    }

    if (showAddScheduleDialog) {
        ScheduleFormDialog(
            plants = plants,
            onSave = { sched ->
                viewModel.saveSchedule(sched)
                showAddScheduleDialog = false
            },
            onDismiss = { showAddScheduleDialog = false }
        )
    }

    if (showAddMonitoringDialog && plants.isNotEmpty()) {
        MonitoringFormDialog(
            plants = plants,
            onSave = { plant, ph, ec, tds, waterTemp, ambTemp, height, rootCond, plantCond, notes ->
                viewModel.addMonitoringLog(plant, ph, ec, tds, waterTemp, ambTemp, height, rootCond, plantCond, notes)
                showAddMonitoringDialog = false
            },
            onDismiss = { showAddMonitoringDialog = false }
        )
    }

    harvestTargetPlant?.let { plant ->
        RecordHarvestDialog(
            plant = plant,
            onConfirm = { weightKg, cycleLabel, notes ->
                viewModel.recordHarvest(plant, weightKg, cycleLabel, notes)
                harvestTargetPlant = null
            },
            onDismiss = { harvestTargetPlant = null }
        )
    }
}

@Composable
private fun PlantFormDialog(
    initial: HydroPlant?,
    onSave: (HydroPlant) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var cropType by remember { mutableStateOf(initial?.cropType ?: "Selada Hijau") }
    var systemType by remember { mutableStateOf(initial?.systemType ?: "NFT") }
    var holeCountStr by remember { mutableStateOf((initial?.holeCount ?: 40L).toString()) }
    var harvestDaysStr by remember { mutableStateOf((initial?.harvestAgeDays ?: 35L).toString()) }
    var phMinStr by remember { mutableStateOf((initial?.phMin ?: 5.5).toString()) }
    var phMaxStr by remember { mutableStateOf((initial?.phMax ?: 6.5).toString()) }
    var ecMinStr by remember { mutableStateOf((initial?.ecMin ?: 0.8).toString()) }
    var ecMaxStr by remember { mutableStateOf((initial?.ecMax ?: 1.4).toString()) }
    var status by remember { mutableStateOf(initial?.status ?: PlantStatus.VEGETATIF.name) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var photoUri by remember { mutableStateOf(initial?.photoUrl ?: "") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Tanaman Baru" else "Edit Data Tanaman") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Blok / Tanaman") }, modifier = Modifier.fillMaxWidth())
                Text("Jenis Tanaman:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(HydroPhysics.cropPresets) { preset ->
                        FilterChip(
                            selected = cropType == preset.cropType,
                            onClick = {
                                cropType = preset.cropType
                                if (initial == null) {
                                    harvestDaysStr = preset.defaultHarvestDays.toString()
                                    phMinStr = preset.defaultPhMin.toString()
                                    phMaxStr = preset.defaultPhMax.toString()
                                    ecMinStr = preset.defaultEcMin.toString()
                                    ecMaxStr = preset.defaultEcMax.toString()
                                }
                            },
                            label = { Text(preset.cropType, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text("Sistem Hidroponik:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("NFT", "DFT", "Wick", "Rakit Apung", "Dutch Bucket")) { sys ->
                        FilterChip(
                            selected = systemType == sys,
                            onClick = { systemType = sys },
                            label = { Text(sys, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = holeCountStr, onValueChange = { holeCountStr = it }, label = { Text("Lubang Tanam") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = harvestDaysStr, onValueChange = { harvestDaysStr = it }, label = { Text("Umur Panen (hr)") }, modifier = Modifier.weight(1f))
                }
                Text("Nilai Acuan pH & EC (Dapat Diedit):", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = phMinStr, onValueChange = { phMinStr = it }, label = { Text("pH Min") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = phMaxStr, onValueChange = { phMaxStr = it }, label = { Text("pH Max") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ecMinStr, onValueChange = { ecMinStr = it }, label = { Text("EC Min") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ecMaxStr, onValueChange = { ecMaxStr = it }, label = { Text("EC Max") }, modifier = Modifier.weight(1f))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PlantStatus.entries) { st ->
                        FilterChip(selected = status == st.name, onClick = { status = st.name }, label = { Text(st.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoUri.isBlank()) "Unggah Foto Perkembangan" else "Foto Dipilih ✓")
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val phMin = phMinStr.toDoubleOrNull()?.coerceIn(0.0, 14.0) ?: 5.5
                val phMax = phMaxStr.toDoubleOrNull()?.coerceIn(phMin, 14.0) ?: 6.5
                val ecMin = ecMinStr.toDoubleOrNull()?.coerceIn(0.0, 20.0) ?: 0.8
                val ecMax = ecMaxStr.toDoubleOrNull()?.coerceIn(ecMin, 20.0) ?: 1.4
                onSave(
                    (initial ?: HydroPlant()).copy(
                        name = name.ifBlank { "$cropType $systemType" },
                        cropType = cropType,
                        systemType = systemType,
                        holeCount = holeCountStr.toLongOrNull()?.coerceIn(1L, 100000L) ?: 40L,
                        harvestAgeDays = harvestDaysStr.toLongOrNull()?.coerceIn(1L, 365L) ?: 35L,
                        status = status,
                        phMin = phMin,
                        phMax = phMax,
                        ecMin = ecMin,
                        ecMax = ecMax,
                        photoUrl = photoUri,
                        notes = notes
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ScheduleFormDialog(
    plants: List<HydroPlant>,
    onSave: (MaintenanceSchedule) -> Unit,
    onDismiss: () -> Unit
) {
    val taskTypes = listOf("Cek pH", "Cek EC/TDS", "Ganti Air Nutrisi", "Pemberian Nutrisi", "Pembersihan Instalasi", "Pemeriksaan Akar", "Pemeriksaan Hama")
    var selectedType by remember { mutableStateOf(taskTypes.first()) }
    var selectedPlant by remember { mutableStateOf(plants.firstOrNull()) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Jadwal Perawatan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jenis Tugas:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(taskTypes) { t ->
                        FilterChip(selected = selectedType == t, onClick = { selectedType = t }, label = { Text(t, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                if (plants.isNotEmpty()) {
                    Text("Tanaman Terkait:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(plants) { p ->
                            FilterChip(selected = selectedPlant?.id == p.id, onClick = { selectedPlant = p }, label = { Text(p.name, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Pengingat") },
                    placeholder = { Text("Contoh: Cek pH tandon pagi") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    MaintenanceSchedule(
                        plantId = selectedPlant?.id ?: "",
                        plantName = selectedPlant?.name ?: "",
                        taskType = selectedType,
                        title = title.ifBlank { "$selectedType ${selectedPlant?.name.orEmpty()}".trim() },
                        dueDateMillis = System.currentTimeMillis(),
                        completed = false,
                        notes = notes
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun MonitoringFormDialog(
    plants: List<HydroPlant>,
    onSave: (HydroPlant, Double, Double, Long, Double, Double, Double, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlant by remember { mutableStateOf(plants.first()) }
    var phStr by remember { mutableStateOf("6.0") }
    var ecStr by remember { mutableStateOf("1.2") }
    var tdsStr by remember { mutableStateOf("600") }
    var waterTempStr by remember { mutableStateOf("24.0") }
    var ambientTempStr by remember { mutableStateOf("27.0") }
    var heightStr by remember { mutableStateOf("14.0") }
    var rootCond by remember { mutableStateOf("Putih bersih") }
    var plantCond by remember { mutableStateOf("Daun hijau segar") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Monitoring Tanaman") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(plants) { p ->
                        FilterChip(selected = selectedPlant.id == p.id, onClick = { selectedPlant = p }, label = { Text(p.name, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Text(
                    "Acuan ${selectedPlant.cropType}: pH ${selectedPlant.phMin}-${selectedPlant.phMax}, EC ${selectedPlant.ecMin}-${selectedPlant.ecMax}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = phStr, onValueChange = { phStr = it }, label = { Text("pH Air") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ecStr, onValueChange = { ecStr = it }, label = { Text("EC (mS/cm)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tdsStr, onValueChange = { tdsStr = it }, label = { Text("TDS (ppm)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = heightStr, onValueChange = { heightStr = it }, label = { Text("Tinggi (cm)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = waterTempStr, onValueChange = { waterTempStr = it }, label = { Text("Suhu Air °C") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ambientTempStr, onValueChange = { ambientTempStr = it }, label = { Text("Suhu Lingk °C") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = rootCond, onValueChange = { rootCond = it }, label = { Text("Kondisi Akar") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = plantCond, onValueChange = { plantCond = it }, label = { Text("Kondisi Tanaman") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan Pertumbuhan") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    selectedPlant,
                    phStr.toDoubleOrNull()?.coerceIn(0.0, 14.0) ?: 6.0,
                    ecStr.toDoubleOrNull()?.coerceIn(0.0, 20.0) ?: 1.2,
                    tdsStr.toLongOrNull()?.coerceIn(0L, 10000L) ?: 600L,
                    waterTempStr.toDoubleOrNull()?.coerceIn(0.0, 80.0) ?: 24.0,
                    ambientTempStr.toDoubleOrNull()?.coerceIn(0.0, 80.0) ?: 27.0,
                    heightStr.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 10.0,
                    rootCond,
                    plantCond,
                    notes
                )
            }) { Text("Simpan Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun RecordHarvestDialog(
    plant: HydroPlant,
    onConfirm: (Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var weightStr by remember { mutableStateOf("12.0") }
    var cycleLabel by remember { mutableStateOf("Siklus ${plant.cropType}") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Panen: ${plant.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = weightStr, onValueChange = { weightStr = it }, label = { Text("Berat Hasil Panen (kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cycleLabel, onValueChange = { cycleLabel = it }, label = { Text("Label Periode Tanam") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan Panen") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(weightStr.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0, cycleLabel, notes)
            }) { Text("Simpan Panen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
