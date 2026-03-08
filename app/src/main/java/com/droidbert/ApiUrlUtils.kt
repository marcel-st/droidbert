package com.droidbert

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ApiUrlUtils {
    fun normalizeApiBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val parsed = trimmed.toHttpUrlOrNull() ?: return trimmed

        val normalized = if (parsed.encodedPath == "/" || parsed.encodedPath.isBlank()) {
            parsed.newBuilder()
                .encodedPath("/api/current.php")
                .build()
        } else {
            parsed
        }

        return normalized.newBuilder()
            .query(null)
            .build()
            .toString()
    }
}