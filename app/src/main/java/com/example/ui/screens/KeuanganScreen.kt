package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExpenseType
import com.example.model.FinancialExpense
import com.example.model.FinancialRevenue
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinanceBarComparisonChart
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.FinancialPdfExporter
import com.example.util.Formatters
import com.example.viewmodel.HydroGrowViewModel

private enum class FinancePeriod(val label: String, val daysBack: Long?) {
    HARIAN("Harian", 1L),
    MINGGUAN("Mingguan", 7L),
    BULANAN("Bulanan", 30L),
    TAHUNAN("Tahunan", 365L),
    SEMUA("Semua / Periode", null)
}

private enum class FinanceTab(val label: String) {
    SEMUA("Semua"),
    PENDAPATAN("Pendapatan"),
    PENGELUARAN("Pengeluaran"),
    MODAL("Modal")
}

@Composable
fun KeuanganScreen(
    viewModel: HydroGrowViewModel,
    expenses: List<FinancialExpense>,
    revenues: List<FinancialRevenue>
) {
    val context = LocalContext.current
    var activePeriod by remember { mutableStateOf(FinancePeriod.BULANAN) }
    var activeTab by remember { mutableStateOf(FinanceTab.SEMUA) }
    var selectedCycle by remember { mutableStateOf("Semua Periode") }

    var showAddFinanceDialog by remember { mutableStateOf(false) }
    var deleteExpenseTarget by remember { mutableStateOf<FinancialExpense?>(null) }
    var deleteRevenueTarget by remember { mutableStateOf<FinancialRevenue?>(null) }

    val now = remember { System.currentTimeMillis() }
    val cycleOptions = remember(expenses, revenues) {
        val cycles = (expenses.map { it.cycleLabel } + revenues.map { it.cycleLabel })
            .filter { it.isNotBlank() }
            .distinct()
        listOf("Semua Periode") + cycles
    }

    val filteredExpenses = remember(expenses, activePeriod, selectedCycle) {
        expenses.filter { exp ->
            val periodOk = activePeriod.daysBack == null || (now - exp.dateMillis) <= activePeriod.daysBack!! * 86_400_000L
            val cycleOk = selectedCycle == "Semua Periode" || exp.cycleLabel == selectedCycle
            periodOk && cycleOk
        }
    }

    val filteredRevenues = remember(revenues, activePeriod, selectedCycle) {
        revenues.filter { rev ->
            val periodOk = activePeriod.daysBack == null || (now - rev.dateMillis) <= activePeriod.daysBack!! * 86_400_000L
            val cycleOk = selectedCycle == "Semua Periode" || rev.cycleLabel == selectedCycle
            periodOk && cycleOk
        }
    }

    val totalRevenue = filteredRevenues.sumOf { it.amount }
    val totalOpsExpense = filteredExpenses.filter { it.entryType == ExpenseType.PENGELUARAN.name }.sumOf { it.amount }
    val totalCapital = filteredExpenses.filter { it.entryType == ExpenseType.MODAL.name }.sumOf { it.amount }
    val profit = totalRevenue - totalOpsExpense

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            FinancialPdfExporter.exportToUri(
                context = context,
                uri = uri,
                periodLabel = "${activePeriod.label} ($selectedCycle)",
                expenses = filteredExpenses,
                revenues = filteredRevenues
            ).onSuccess {
                viewModel.showMessage("Laporan PDF berhasil diekspor!")
            }.onFailure { err ->
                viewModel.showMessage(err.localizedMessage ?: "Gagal mengekspor PDF")
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddFinanceDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Catat Keuangan") },
                modifier = Modifier.testTag("finance_add_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Keuangan Usaha", style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(
                        onClick = { pdfExportLauncher.launch("Laporan_HydroGrow.pdf") },
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ekspor PDF")
                    }
                }
            }

            // Period & Cycle Filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FinancePeriod.entries) { p ->
                        FilterChip(
                            selected = activePeriod == p,
                            onClick = { activePeriod = p },
                            label = { Text(p.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (cycleOptions.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cycleOptions) { cy ->
                            FilterChip(
                                selected = selectedCycle == cy,
                                onClick = { selectedCycle = cy },
                                label = { Text(cy, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Profit / Loss Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (profit >= 0) LeafGreenContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Laba / Rugi Bersih (Pendapatan - Pengeluaran)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (profit >= 0) OnLeafGreenContainer else ErrorRed
                        )
                        Text(
                            text = Formatters.rupiah(profit),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (profit >= 0) LeafGreenDeep else ErrorRed
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Pendapatan", style = MaterialTheme.typography.labelSmall)
                                Text(Formatters.rupiah(totalRevenue), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Pengeluaran Ops", style = MaterialTheme.typography.labelSmall)
                                Text(Formatters.rupiah(totalOpsExpense), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Modal Kebun", style = MaterialTheme.typography.labelSmall)
                                Text(Formatters.rupiah(totalCapital), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                FinanceBarComparisonChart(
                    revenueTotal = totalRevenue,
                    expenseTotal = totalOpsExpense,
                    capitalTotal = totalCapital
                )
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = FinanceTab.entries.indexOf(activeTab),
                    edgePadding = 0.dp
                ) {
                    FinanceTab.entries.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }

            val showRevenues = activeTab == FinanceTab.SEMUA || activeTab == FinanceTab.PENDAPATAN
            val shownExpenses = when (activeTab) {
                FinanceTab.SEMUA -> filteredExpenses
                FinanceTab.PENGELUARAN -> filteredExpenses.filter { it.entryType == ExpenseType.PENGELUARAN.name }
                FinanceTab.MODAL -> filteredExpenses.filter { it.entryType == ExpenseType.MODAL.name }
                FinanceTab.PENDAPATAN -> emptyList()
            }

            if ((if (showRevenues) filteredRevenues.size else 0) + shownExpenses.size == 0) {
                item {
                    EmptyStateCard(
                        title = "Belum Ada Catatan Keuangan",
                        subtitle = "Catat modal awal, pembelian nutrisi/benih, atau pendapatan penjualan panen Anda.",
                        actionLabel = "Catat Sekarang",
                        onAction = { showAddFinanceDialog = true }
                    )
                }
            }

            if (showRevenues && filteredRevenues.isNotEmpty()) {
                item { Text("Riwayat Pendapatan", style = MaterialTheme.typography.titleSmall) }
                items(filteredRevenues, key = { "rev_${it.id}" }) { rev ->
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge("MASUK", LeafGreenContainer, OnLeafGreenContainer)
                                    Text(rev.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(rev.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${Formatters.formatDate(rev.dateMillis)}${if (rev.cycleLabel.isNotBlank()) " • ${rev.cycleLabel}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "+${Formatters.rupiah(rev.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = LeafGreenPrimary
                            )
                            IconButton(onClick = { deleteRevenueTarget = rev }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus pendapatan", tint = ErrorRed)
                            }
                        }
                    }
                }
            }

            if (shownExpenses.isNotEmpty()) {
                item { Text("Riwayat Modal & Pengeluaran", style = MaterialTheme.typography.titleSmall) }
                items(shownExpenses, key = { "exp_${it.id}" }) { exp ->
                    val isModal = exp.entryType == ExpenseType.MODAL.name
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(
                                        if (isModal) "MODAL" else "KELUAR",
                                        if (isModal) AmberContainer else MaterialTheme.colorScheme.errorContainer,
                                        if (isModal) OnAmberContainer else ErrorRed
                                    )
                                    Text(exp.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(exp.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${Formatters.formatDate(exp.dateMillis)}${if (exp.cycleLabel.isNotBlank()) " • ${exp.cycleLabel}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "-${Formatters.rupiah(exp.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = OrangeHighlight
                            )
                            IconButton(onClick = { deleteExpenseTarget = exp }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus pengeluaran", tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFinanceDialog) {
        AddFinanceDialog(
            onSaveExpense = { type, cat, title, amount, cycle, notes ->
                viewModel.addExpense(type, cat, title, amount, cycle, notes)
                showAddFinanceDialog = false
            },
            onSaveRevenue = { cat, title, amount, cycle, notes ->
                viewModel.addRevenue(cat, title, amount, cycle, notes)
                showAddFinanceDialog = false
            },
            onDismiss = { showAddFinanceDialog = false }
        )
    }

    deleteExpenseTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Catatan Pengeluaran?",
            message = "Hapus \"${target.title}\" senilai ${Formatters.rupiah(target.amount)}?",
            onConfirm = {
                viewModel.deleteExpense(target.id)
                deleteExpenseTarget = null
            },
            onDismiss = { deleteExpenseTarget = null }
        )
    }

    deleteRevenueTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Catatan Pendapatan?",
            message = "Hapus \"${target.title}\" senilai ${Formatters.rupiah(target.amount)}?",
            onConfirm = {
                viewModel.deleteRevenue(target.id)
                deleteRevenueTarget = null
            },
            onDismiss = { deleteRevenueTarget = null }
        )
    }
}

@Composable
private fun AddFinanceDialog(
    onSaveExpense: (ExpenseType, String, String, Long, String, String) -> Unit,
    onSaveRevenue: (String, String, Long, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf("PENGELUARAN") } // MODAL, PENGELUARAN, PENDAPATAN
    val categories = when (mode) {
        "MODAL" -> listOf("Modal Awal", "Pembelian Instalasi", "Pembelian Perlengkapan", "Pembangunan Kebun")
        "PENDAPATAN" -> listOf("Penjualan Hasil Panen", "Penjualan Perlengkapan", "Jasa & Lainnya")
        else -> listOf("Benih", "Nutrisi", "Rockwool", "Listrik", "Air", "Tenaga Kerja", "Pengemasan", "Transportasi", "Lainnya")
    }
    var selectedCategory by remember(mode) { mutableStateOf(categories.first()) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var cycleLabel by remember { mutableStateOf("Periode November 2026") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Transaksi Keuangan") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PENGELUARAN" to "Pengeluaran", "MODAL" to "Modal", "PENDAPATAN" to "Pendapatan").forEach { (key, label) ->
                        FilterChip(selected = mode == key, onClick = { mode = key }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Text("Kategori:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Keterangan / Judul") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Nominal (Rp)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cycleLabel, onValueChange = { cycleLabel = it }, label = { Text("Periode Tanam") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toLongOrNull() ?: 0L
                when (mode) {
                    "MODAL" -> onSaveExpense(ExpenseType.MODAL, selectedCategory, title, amt, cycleLabel, notes)
                    "PENGELUARAN" -> onSaveExpense(ExpenseType.PENGELUARAN, selectedCategory, title, amt, cycleLabel, notes)
                    else -> onSaveRevenue(selectedCategory, title, amt, cycleLabel, notes)
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
