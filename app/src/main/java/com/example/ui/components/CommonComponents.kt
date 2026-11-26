package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.CityLocation
import com.example.model.MarketplaceProduct
import com.example.model.PlantMonitoringLog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LeafGreenDeep
import com.example.ui.theme.LeafGreenPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OrangeHighlight
import com.example.util.Formatters
import com.example.util.HydroPhysics
import kotlin.math.max

@Composable
fun HydroGrowLogoMark(size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(LeafGreenDeep, LeafGreenPrimary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Eco,
            contentDescription = "Logo HydroGrow",
            tint = MintLight,
            modifier = Modifier.size(size * 0.56f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(size * 0.24f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = LeafGreenPrimary,
                modifier = Modifier.size(size * 0.16f)
            )
        }
    }
}

@Composable
fun DemoModeBanner(
    onSwitchRoleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AmberContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = null,
                    tint = OnAmberContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "MODE DEMO — Data & transaksi simulasi (bukan uang nyata)",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnAmberContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onSwitchRoleClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Ganti Peran",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = OrangeHighlight
                )
            }
        }
    }
}

@Composable
fun MetricKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Inbox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Kesalahan",
                tint = ErrorRed
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun GrowthLineChart(
    logs: List<PlantMonitoringLog>,
    modifier: Modifier = Modifier
) {
    val sorted = logs.sortedBy { it.recordedAtMillis }.takeLast(8)
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = OrangeHighlight
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    if (sorted.isEmpty()) {
        EmptyStateCard(
            title = "Belum Ada Data Grafik Pertumbuhan",
            subtitle = "Catat monitoring pH, EC, dan tinggi tanaman untuk menampilkan grafik perkembangan."
        )
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Grafik Tinggi Tanaman & pH", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(primaryColor))
                    Spacer(Modifier.width(4.dp))
                    Text("Tinggi (cm)", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                    Spacer(Modifier.width(4.dp))
                    Text("pH", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val w = size.width
                val h = size.height
                val padX = 20f
                val padY = 16f
                val usableW = max(1f, w - padX * 2)
                val usableH = max(1f, h - padY * 2)

                for (i in 0..3) {
                    val y = padY + (usableH / 3f) * i
                    drawLine(gridColor, Offset(padX, y), Offset(w - padX, y), strokeWidth = 1.5f)
                }

                val maxHeight = max(20f, sorted.maxOf { it.heightCm }.toFloat() * 1.15f)
                val stepX = if (sorted.size > 1) usableW / (sorted.size - 1) else 0f

                val heightPath = Path()
                val phPath = Path()
                sorted.forEachIndexed { idx, log ->
                    val x = if (sorted.size == 1) w / 2f else padX + stepX * idx
                    val yH = padY + usableH * (1f - (log.heightCm.toFloat() / maxHeight).coerceIn(0f, 1f))
                    val yPh = padY + usableH * (1f - ((log.ph.toFloat() - 4f) / 5f).coerceIn(0f, 1f))

                    if (idx == 0) {
                        heightPath.moveTo(x, yH)
                        phPath.moveTo(x, yPh)
                    } else {
                        heightPath.lineTo(x, yH)
                        phPath.lineTo(x, yPh)
                    }
                    drawCircle(primaryColor, radius = 6f, center = Offset(x, yH))
                    drawCircle(accentColor, radius = 5f, center = Offset(x, yPh))
                }
                if (sorted.size > 1) {
                    drawPath(heightPath, color = primaryColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    drawPath(phPath, color = accentColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                sorted.forEach { log ->
                    Text(
                        text = Formatters.formatShortDate(log.recordedAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FinanceBarComparisonChart(
    revenueTotal: Long,
    expenseTotal: Long,
    capitalTotal: Long,
    modifier: Modifier = Modifier
) {
    val maxVal = max(1L, maxOf(revenueTotal, expenseTotal, capitalTotal)).toFloat()
    val items = listOf(
        Triple("Pendapatan", revenueTotal, LeafGreenPrimary),
        Triple("Pengeluaran", expenseTotal, OrangeHighlight),
        Triple("Modal", capitalTotal, AmberAccent)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Perbandingan Arus Keuangan", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val barWidth = size.width / 6f
                val spacing = size.width / 3f
                items.forEachIndexed { idx, (_, value, color) ->
                    val ratio = (value.toFloat() / maxVal).coerceIn(0.05f, 1f)
                    val barHeight = size.height * 0.85f * ratio
                    val x = spacing * idx + (spacing - barWidth) / 2f
                    val y = size.height - barHeight
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                items.forEach { (label, amount, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        Text(Formatters.rupiah(amount), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun NearbySellersMapCanvas(
    userLocation: CityLocation,
    radiusKm: Double,
    products: List<MarketplaceProduct>,
    selectedSellerId: String?,
    onSelectSeller: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uniqueSellers = products.distinctBy { it.sellerId }
    val primary = LeafGreenPrimary
    val userDot = Color(0xFF2563EB)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF7F1))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Peta Sekitar ${userLocation.name} (Radius ${radiusKm.toInt()} km)",
                        style = MaterialTheme.typography.titleSmall,
                        color = LeafGreenDeep
                    )
                }
                StatusBadge("DEMO LOKASI", AmberContainer, OnAmberContainer)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFDDF0E2))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxRadiusPx = size.minDimension * 0.44f

                    // Subtle grid lines simulating map streets
                    val roadColor = Color.White.copy(alpha = 0.65f)
                    for (i in 1..4) {
                        val y = size.height * (i / 5f)
                        val x = size.width * (i / 5f)
                        drawLine(roadColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                        drawLine(roadColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                    }

                    // Radius circles
                    drawCircle(
                        color = primary.copy(alpha = 0.12f),
                        radius = maxRadiusPx,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.45f),
                        radius = maxRadiusPx,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.25f),
                        radius = maxRadiusPx * 0.5f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    // User center pin
                    drawCircle(userDot.copy(alpha = 0.2f), radius = 18f, center = Offset(cx, cy))
                    drawCircle(Color.White, radius = 10f, center = Offset(cx, cy))
                    drawCircle(userDot, radius = 7f, center = Offset(cx, cy))

                    // Seller pins mapped proportionally from lat/lng delta
                    uniqueSellers.forEach { prod ->
                        val distKm = HydroPhysics.calculateDistanceKm(
                            userLocation.latitude,
                            userLocation.longitude,
                            prod.sellerLat,
                            prod.sellerLng
                        )
                        val dLat = prod.sellerLat - userLocation.latitude
                        val dLng = prod.sellerLng - userLocation.longitude
                        val scale = maxRadiusPx / (radiusKm.toFloat() / 111f).coerceAtLeast(0.05f)
                        val px = (cx + (dLng.toFloat() * scale)).coerceIn(24f, size.width - 24f)
                        val py = (cy - (dLat.toFloat() * scale)).coerceIn(24f, size.height - 24f)

                        val isSelected = prod.sellerId == selectedSellerId
                        val inRadius = distKm <= radiusKm
                        val pinColor = when {
                            isSelected -> OrangeHighlight
                            inRadius -> primary
                            else -> Color.Gray
                        }
                        drawCircle(Color.White, radius = if (isSelected) 13f else 10f, center = Offset(px, py))
                        drawCircle(pinColor, radius = if (isSelected) 10f else 7.5f, center = Offset(px, py))
                    }
                }
                Text(
                    text = "Titik Biru: Anda  •  Hijau: Penjual Dalam Radius  •  Ketuk toko di bawah untuk pilih",
                    style = MaterialTheme.typography.labelSmall,
                    color = LeafGreenDeep,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.85f))
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            if (uniqueSellers.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uniqueSellers.take(3).forEach { sellerProd ->
                        val selected = sellerProd.sellerId == selectedSellerId
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSeller(sellerProd.sellerId) },
                            label = {
                                Text(
                                    sellerProd.sellerName.substringBefore(" ("),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
