package com.example.myfit.data.update

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val RELEASES_URL =
    "https://api.github.com/repos/vctr13/myfit-android/releases/latest"

object UpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        val body = client.newCall(request).execute().use { it.body?.string() }
            ?: return@withContext null
        val json = JsonParser.parseString(body).asJsonObject
        val tagName = json.get("tag_name")?.asString ?: return@withContext null
        val changelog = json.get("body")?.asString?.trim() ?: ""
        val assets = json.getAsJsonArray("assets") ?: return@withContext null
        val apkUrl = assets
            .firstOrNull { it.asJsonObject.get("name")?.asString?.endsWith(".apk") == true }
            ?.asJsonObject?.get("browser_download_url")?.asString
            ?: return@withContext null
        ReleaseInfo(
            tagName = tagName,
            versionName = tagName.trimStart('v'),
            changelog = changelog,
            apkUrl = apkUrl
        )
    }

    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
        val l = parts(latest)
        val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val lp = l.getOrElse(i) { 0 }
            val cp = c.getOrElse(i) { 0 }
            if (lp > cp) return true
            if (lp < cp) return false
        }
        return false
    }

    suspend fun downloadApk(url: String, destFile: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body ?: error("Пустой ответ сервера")
            FileOutputStream(destFile).use { out ->
                body.byteStream().copyTo(out)
            }
        }
    }
}
