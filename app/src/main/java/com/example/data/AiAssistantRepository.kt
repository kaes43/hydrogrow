package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class AiAssistantRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "Anda adalah Asisten Ahli Hidroponik HydroGrow berbahasa Indonesia. " +
                        "Berikan panduan budidaya hidroponik (NFT, DFT, Wick, Dutch Bucket), " +
                        "analisis pH, EC/TDS, suhu air, pencegahan hama, estimasi modal, " +
                        "simulasi keuntungan, dan perencanaan jadwal tanam secara jelas dan praktis. " +
                        "ATURAN WAJIB: (1) Selalu jelaskan bahwa saran bersifat rekomendasi panduan, " +
                        "bukan diagnosis laboratorium pasti. (2) Jangan pernah mengarang hasil pengukuran " +
                        "sensor pH/EC/suhu yang tidak diberikan oleh pengguna."
                )
            }
        )
    }

    fun streamAdvice(prompt: String): Flow<Result<String>> = flow {
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(Result.success(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isBlank()) {
                emit(Result.failure(IllegalStateException("Asisten AI tidak mengembalikan respons.")))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI streaming timed out", e)
            emit(Result.failure(Exception("Waktu tunggu Asisten AI habis (60 detik). Silakan coba lagi.")))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            emit(Result.failure(Exception(e.localizedMessage ?: "Gagal menghubungi Asisten AI HydroGrow.")))
        }
    }

    private companion object {
        const val TAG = "HydroGrowAI"
    }
}
