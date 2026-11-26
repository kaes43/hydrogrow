package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.model.CropPreset
import com.example.model.ExpenseType
import com.example.model.FinancialExpense
import com.example.model.FinancialRevenue
import com.example.model.HarvestPrediction
import com.example.model.HealthStatus
import com.example.model.HydroPlant
import com.example.model.PlantMonitoringLog
import com.example.model.PlantStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Formatters {
    private val indonesiaLocale = Locale("in", "ID")
    private val rupiahFormat = NumberFormat.getNumberInstance(indonesiaLocale)
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", indonesiaLocale)
    private val dateShortFormat = SimpleDateFormat("dd/MM", indonesiaLocale)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesiaLocale)

    fun rupiah(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        return "${sign}Rp${rupiahFormat.format(kotlin.math.abs(amount))}"
    }

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatShortDate(millis: Long): String = dateShortFormat.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun formatDistanceKm(km: Double): String {
        return if (km < 1.0) {
            "${(km * 1000).roundToInt()} m"
        } else {
            String.format(indonesiaLocale, "%.1f km", km)
        }
    }
}

object HydroPhysics {
    val cropPresets = listOf(
        CropPreset("Selada Hijau", 5.5, 6.5, 0.8, 1.4, 35, 0.16),
        CropPreset("Selada Merah", 5.5, 6.5, 0.8, 1.4, 38, 0.15),
        CropPreset("Pakcoy", 6.0, 7.0, 1.5, 2.0, 32, 0.18),
        CropPreset("Kangkung", 5.5, 6.5, 1.2, 2.0, 24, 0.14),
        CropPreset("Bayam", 6.0, 7.0, 1.5, 2.2, 28, 0.13),
        CropPreset("Kale", 5.5, 6.5, 1.4, 2.2, 50, 0.20),
        CropPreset("Seledri", 6.0, 6.8, 1.8, 2.4, 65, 0.16),
        CropPreset("Sawi", 6.0, 7.0, 1.5, 2.2, 30, 0.17),
        CropPreset("Tomat Ceri", 5.5, 6.5, 2.0, 3.5, 75, 0.85),
        CropPreset("Cabai", 5.8, 6.5, 2.0, 3.2, 85, 0.45)
    )

    fun presetFor(cropType: String): CropPreset {
        return cropPresets.firstOrNull { it.cropType.equals(cropType, ignoreCase = true) }
            ?: cropPresets.first()
    }

    fun evaluateHealth(plant: HydroPlant?, ph: Double, ec: Double, waterTemp: Double): HealthStatus {
        val phMin = plant?.phMin ?: 5.5
        val phMax = plant?.phMax ?: 6.5
        val ecMin = plant?.ecMin ?: 0.8
        val ecMax = plant?.ecMax ?: 2.0

        val phOutSevere = ph < phMin - 0.6 || ph > phMax + 0.6
        val ecOutSevere = ec < ecMin - 0.5 || ec > ecMax + 0.6
        val tempSevere = waterTemp < 16.0 || waterTemp > 30.0
        if (phOutSevere || ecOutSevere || tempSevere) return HealthStatus.PERINGATAN

        val phWarn = ph < phMin || ph > phMax
        val ecWarn = ec < ecMin || ec > ecMax
        val tempWarn = waterTemp < 19.0 || waterTemp > 27.5
        if (phWarn || ecWarn || tempWarn) return HealthStatus.PERLU_DIPERIKSA

        return HealthStatus.NORMAL
    }

    fun predictHarvest(
        plant: HydroPlant,
        logs: List<PlantMonitoringLog>,
        nowMillis: Long = System.currentTimeMillis()
    ): HarvestPrediction {
        val dayMs = 86_400_000L
        val estimatedMillis = plant.plantedDateMillis + plant.harvestAgeDays * dayMs
        val daysRemaining = max(0L, ((estimatedMillis - nowMillis) + dayMs - 1) / dayMs)
        val latestLog = logs.filter { it.plantId == plant.id }.maxByOrNull { it.recordedAtMillis }
        val healthFactor = when (latestLog?.healthStatus) {
            HealthStatus.PERINGATAN.name -> 0.80
            HealthStatus.PERLU_DIPERIKSA.name -> 0.92
            else -> 0.97
        }
        val readyHoles = max(1L, (plant.holeCount * healthFactor).roundToInt().toLong())
        val preset = presetFor(plant.cropType)
        val estWeightKg = readyHoles * preset.estimatedKgPerHole
        val readiness = when {
            plant.status == PlantStatus.PANEN_SELESAI.name -> "Sudah Dipanen"
            daysRemaining <= 0L || plant.status == PlantStatus.SIAP_PANEN.name -> "Siap Panen Sekarang"
            daysRemaining <= 5L -> "Mendekati Panen ($daysRemaining hari lagi)"
            else -> "Masa Pertumbuhan ($daysRemaining hari lagi)"
        }
        return HarvestPrediction(
            estimatedHarvestMillis = estimatedMillis,
            daysRemaining = daysRemaining,
            estimatedReadyHoles = readyHoles,
            estimatedWeightKg = estWeightKg,
            readinessLabel = readiness
        )
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}

object FinancialPdfExporter {
    fun exportToUri(
        context: Context,
        uri: Uri,
        periodLabel: String,
        expenses: List<FinancialExpense>,
        revenues: List<FinancialRevenue>
    ): Result<Unit> {
        return try {
            val totalRevenue = revenues.sumOf { it.amount }
            val totalOpsExpense = expenses.filter { it.entryType == ExpenseType.PENGELUARAN.name }.sumOf { it.amount }
            val totalCapital = expenses.filter { it.entryType == ExpenseType.MODAL.name }.sumOf { it.amount }
            val profit = totalRevenue - totalOpsExpense

            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(21, 128, 61)
                textSize = 20f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(17, 27, 21)
                textSize = 13f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(55, 65, 81)
                textSize = 11f
            }

            var y = 52f
            canvas.drawText("HydroGrow - Laporan Keuangan Usaha Hidroponik", 40f, y, titlePaint)
            y += 22f
            canvas.drawText("Periode: $periodLabel  |  Dicetak: ${Formatters.formatDateTime(System.currentTimeMillis())}", 40f, y, bodyPaint)
            y += 28f

            canvas.drawText("Ringkasan Keuangan", 40f, y, headerPaint)
            y += 18f
            canvas.drawText("Total Pendapatan        : ${Formatters.rupiah(totalRevenue)}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Total Pengeluaran Ops   : ${Formatters.rupiah(totalOpsExpense)}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Total Modal Kebun       : ${Formatters.rupiah(totalCapital)}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Estimasi Laba / Rugi    : ${Formatters.rupiah(profit)}", 40f, y, headerPaint)
            y += 28f

            canvas.drawText("Rincian Pendapatan (${revenues.size} catatan)", 40f, y, headerPaint)
            y += 18f
            revenues.take(12).forEach { rev ->
                canvas.drawText("• ${Formatters.formatDate(rev.dateMillis)} | ${rev.title} (${rev.category}) : ${Formatters.rupiah(rev.amount)}", 44f, y, bodyPaint)
                y += 15f
            }
            y += 14f
            canvas.drawText("Rincian Modal & Pengeluaran (${expenses.size} catatan)", 40f, y, headerPaint)
            y += 18f
            expenses.take(16).forEach { exp ->
                if (y < 800f) {
                    canvas.drawText("• ${Formatters.formatDate(exp.dateMillis)} | [${exp.entryType}] ${exp.title} : ${Formatters.rupiah(exp.amount)}", 44f, y, bodyPaint)
                    y += 15f
                }
            }

            pdf.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            } ?: error("Gagal membuka aliran berkas PDF.")
            pdf.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
