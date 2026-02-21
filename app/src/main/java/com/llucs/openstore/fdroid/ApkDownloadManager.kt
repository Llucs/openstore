package com.llucs.openstore.fdroid

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

sealed class ApkDownloadState {
    data class Progress(val readBytes: Long, val totalBytes: Long) : ApkDownloadState()
    data class Done(val file: File) : ApkDownloadState()
    data class Error(val message: String) : ApkDownloadState()
}

class ApkDownloadManager(private val context: Context) {

    fun downloadApk(
        url: String,
        expectedSha256Hex: String,
        targetName: String
    ): Flow<ApkDownloadState> = callbackFlow {
        val call = HttpClient.client.newCall(Request.Builder().url(url).build())
        val targetDir = File(context.cacheDir, "apks").apply { mkdirs() }
        val target = File(targetDir, targetName)

        val thread = Thread {
            try {
                call.execute().use { resp ->
                    if (!resp.isSuccessful) {
                        trySend(ApkDownloadState.Error("HTTP ${resp.code} ao baixar APK"))
                        return@use
                    }
                    val body = resp.body ?: run {
                        trySend(ApkDownloadState.Error("Resposta sem corpo ao baixar APK"))
                        return@use
                    }
                    val total = body.contentLength().takeIf { it > 0 } ?: -1L
                    val digest = MessageDigest.getInstance("SHA-256")

                    FileOutputStream(target).use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var readTotal = 0L
                            while (true) {
                                val r = input.read(buffer)
                                if (r <= 0) break
                                out.write(buffer, 0, r)
                                digest.update(buffer, 0, r)
                                readTotal += r
                                trySend(ApkDownloadState.Progress(readTotal, total))
                            }
                            out.flush()
                        }
                    }
                    val got = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!got.equals(expectedSha256Hex.trim(), ignoreCase = true)) {
                        target.delete()
                        trySend(ApkDownloadState.Error("SHA-256 não confere. Esperado: $expectedSha256Hex / Recebido: $got"))
                    } else {
                        trySend(ApkDownloadState.Done(target))
                    }
                }
            } catch (e: Exception) {
                target.delete()
                trySend(ApkDownloadState.Error(e.message ?: e.javaClass.simpleName))
            } finally {
                close()
            }
        }
        thread.start()

        awaitClose {
            call.cancel()
        }
    }
}
