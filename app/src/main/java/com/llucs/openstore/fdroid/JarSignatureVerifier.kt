package com.llucs.openstore.fdroid

import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

object JarSignatureVerifier {

    fun fingerprintSha256ForEntry(jarFile: File, entryName: String): String {
        JarFile(jarFile, true).use { jar ->
            val entry: JarEntry = jar.getJarEntry(entryName)
                ?: throw IllegalStateException("Entrada ausente no JAR: $entryName")

            jar.getInputStream(entry).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val r = input.read(buffer)
                    if (r <= 0) break
                }
            }

            val certs = entry.certificates?.toList().orEmpty()
            val x509 = certs.firstOrNull { it is X509Certificate } as? X509Certificate
                ?: throw IllegalStateException("JAR sem assinatura válida (sem certificado).")

            val digest = MessageDigest.getInstance("SHA-256").digest(x509.encoded)
            return digest.joinToString("") { "%02X".format(it) }
        }
    }
}
