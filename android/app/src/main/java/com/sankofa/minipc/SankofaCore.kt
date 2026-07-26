package com.sankofa.minipc

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class PerformanceTier {
    LITE,
    STANDARD,
    PERFORMANCE,
    HUGE_MODEL_RESEARCH,
}

data class DeviceProfile(
    val androidVersion: String,
    val architecture: String,
    val cpuCores: Int,
    val totalRamMb: Long,
    val freeStorageMb: Long,
    val tier: PerformanceTier,
)

object DeviceProfiler {
    fun detect(context: Context): DeviceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val storage = StatFs(context.filesDir.absolutePath)
        val ramMb = memoryInfo.totalMem / (1024L * 1024L)
        val freeStorageMb = storage.availableBytes / (1024L * 1024L)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        val tier = when {
            ramMb >= 20_000 && freeStorageMb >= 100_000 -> PerformanceTier.HUGE_MODEL_RESEARCH
            ramMb >= 12_000 && cores >= 8 -> PerformanceTier.PERFORMANCE
            ramMb >= 6_000 -> PerformanceTier.STANDARD
            else -> PerformanceTier.LITE
        }

        return DeviceProfile(
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            architecture = architecture,
            cpuCores = cores,
            totalRamMb = ramMb,
            freeStorageMb = freeStorageMb,
            tier = tier,
        )
    }
}

object NativeRuntime {
    private val loaded: Boolean = runCatching {
        System.loadLibrary("sankofa_runtime")
        true
    }.getOrDefault(false)

    private external fun runtimeInfo(): String

    fun info(): String = if (loaded) {
        runCatching { runtimeInfo() }.getOrElse { "native=error:${it.javaClass.simpleName}" }
    } else {
        "native=unavailable"
    }
}

class RuntimeManager(
    private val healthUrl: String = "http://127.0.0.1:8787/health",
) {
    suspend fun localHealth(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(healthUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2_000
                readTimeout = 2_000
            }
            connection.useConnection {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                (connection.responseCode in 200..299) to body
            }
        }.getOrElse { false to (it.message ?: it.javaClass.simpleName) }
    }
}

enum class ToolRisk {
    READ_ONLY,
    DRAFT,
    EXTERNAL_WRITE,
    DESTRUCTIVE,
}

object AgentPolicy {
    private val readMarkers = listOf("GET_", "LIST_", "SEARCH_", "READ_", "FETCH_", "FIND_")
    private val destructiveMarkers = listOf("DELETE_", "REMOVE_", "TRASH_", "CANCEL_", "REVOKE_")

    fun classify(toolSlug: String): ToolRisk {
        val action = toolSlug.uppercase().substringAfter('_', toolSlug.uppercase())
        return when {
            destructiveMarkers.any(action::startsWith) -> ToolRisk.DESTRUCTIVE
            action.startsWith("DRAFT_") || action.contains("CREATE_DRAFT") -> ToolRisk.DRAFT
            readMarkers.any(action::startsWith) -> ToolRisk.READ_ONLY
            else -> ToolRisk.EXTERNAL_WRITE
        }
    }

    fun needsApproval(toolSlug: String): Boolean = classify(toolSlug) != ToolRisk.READ_ONLY

    fun isBlocked(toolSlug: String): Boolean = classify(toolSlug) == ToolRisk.DESTRUCTIVE
}

data class GatewayResult(
    val statusCode: Int,
    val body: String,
    val approvalRequired: Boolean,
)

class GatewayClient(
    private val baseUrl: String,
    private val bearerToken: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createConnectionLink(
        userId: String,
        authConfigId: String,
        callbackUrl: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("userId", userId)
            put("authConfigId", authConfigId)
            callbackUrl?.takeIf { it.isNotBlank() }?.let { put("callbackUrl", it) }
        }
        val (status, body) = postJson("/v1/connect", payload)
        require(status in 200..299) { "Gateway connection failed ($status): $body" }
        json.parseToJsonElement(body).jsonObject["redirect_url"]?.jsonPrimitive?.contentOrNull
            ?: error("Gateway response did not include redirect_url")
    }

    suspend fun executeTool(
        userId: String,
        toolSlug: String,
        arguments: JsonObject,
        connectedAccountId: String? = null,
        approved: Boolean = false,
        version: String = "latest",
    ): GatewayResult = withContext(Dispatchers.IO) {
        val encodedSlug = URLEncoder.encode(toolSlug, StandardCharsets.UTF_8.toString())
        val payload = buildJsonObject {
            put("userId", userId)
            put("toolSlug", toolSlug)
            put("version", version)
            put("approved", approved)
            put("arguments", arguments)
            connectedAccountId?.takeIf { it.isNotBlank() }?.let { put("connectedAccountId", it) }
        }
        val (status, body) = postJson("/v1/tools/execute/$encodedSlug", payload)
        GatewayResult(
            statusCode = status,
            body = body,
            approvalRequired = status == 409,
        )
    }

    private fun postJson(path: String, payload: JsonObject): Pair<Int, String> {
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://127.0.0.1")) {
            "Gateway must use HTTPS except for localhost development"
        }
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $bearerToken")
        }
        connection.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
        return connection.useConnection {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        }
    }
}

private inline fun <T> HttpURLConnection.useConnection(block: () -> T): T = try {
    block()
} finally {
    disconnect()
}
