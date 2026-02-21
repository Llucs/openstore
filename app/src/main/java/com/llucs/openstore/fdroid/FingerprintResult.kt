package com.llucs.openstore.fdroid

data class FingerprintResult(
    val baseUrl: String,
    val fingerprintSha256: String,
    val repoNameGuess: String
)
