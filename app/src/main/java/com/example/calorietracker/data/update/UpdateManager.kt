package com.example.calorietracker.data.update

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("body") val body: String,
    @SerializedName("published_at") val publishedAt: String
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String) : UpdateStatus()
    data class NoUpdate(val version: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class UpdateManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // GitHub API URL
    private val REPO_OWNER = "Nephelium"
    private val REPO_NAME = "MeowFit"
    private val RELEASE_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(RELEASE_URL)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext UpdateStatus.Error("API请求失败: ${response.code} ${response.message}")
                    }

                    val responseBody = response.body?.string() ?: return@withContext UpdateStatus.Error("响应内容为空")
                    
                    val release = try {
                        gson.fromJson(responseBody, ReleaseInfo::class.java)
                    } catch (e: Exception) {
                        return@withContext UpdateStatus.Error("解析响应失败: ${e.localizedMessage}")
                    }

                    if (release == null || release.tagName.isBlank()) {
                         return@withContext UpdateStatus.Error("未能获取到版本信息")
                    }

                    // Compare versions
                    val remoteVersion = release.tagName.removePrefix("v")
                    val localVersion = currentVersion.removePrefix("v")
                    
                    if (isNewerVersion(remoteVersion, localVersion)) {
                        UpdateStatus.UpdateAvailable(release, currentVersion)
                    } else {
                        UpdateStatus.NoUpdate(currentVersion)
                    }
                }
            } catch (e: Exception) {
                UpdateStatus.Error("网络错误: ${e.localizedMessage}")
            }
        }
    }
    
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(remoteParts.size, localParts.size)
        
        for (i in 0 until length) {
            val r = if (i < remoteParts.size) remoteParts[i] else 0
            val l = if (i < localParts.size) localParts[i] else 0
            
            if (r > l) return true
            if (r < l) return false
        }
        
        return false
    }
}
