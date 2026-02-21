package com.llucs.openstore.fdroid

import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream

class HttpDownloader {

    data class Result(
        val changed: Boolean,
        val etag: String,
        val lastModified: String,
        val code: Int
    )

    fun downloadToFile(
        url: String,
        target: File,
        etag: String,
        lastModified: String
    ): Result {
        val builder = Request.Builder().url(url)
        if (etag.isNotBlank()) builder.header("If-None-Match", etag)
        if (lastModified.isNotBlank()) builder.header("If-Modified-Since", lastModified)

        HttpClient.client.newCall(builder.build()).execute().use { resp ->
            if (resp.code == 304) {
                return Result(changed = false, etag = etag, lastModified = lastModified, code = resp.code)
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code} ao baixar: $url")
            }
            writeBody(resp, target)
            val newEtag = resp.header("ETag").orEmpty()
            val newLastMod = resp.header("Last-Modified").orEmpty()
            return Result(changed = true, etag = newEtag, lastModified = newLastMod, code = resp.code)
        }
    }

    private fun writeBody(resp: Response, target: File) {
        val body = resp.body ?: throw IllegalStateException("Resposta vazia.")
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { out ->
            body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val r = input.read(buffer)
                    if (r <= 0) break
                    out.write(buffer, 0, r)
                }
                out.flush()
            }
        }
    }
}
