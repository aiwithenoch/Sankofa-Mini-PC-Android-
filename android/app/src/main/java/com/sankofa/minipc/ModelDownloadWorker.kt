package com.sankofa.minipc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext failure("missing_url")
        val requestedName = inputData.getString(KEY_FILE_NAME) ?: return@withContext failure("missing_name")
        val expectedSha256 = inputData.getString(KEY_SHA256).orEmpty().lowercase()
        val expectedSize = inputData.getLong(KEY_SIZE, -1L)
        val safeName = sanitizeFileName(requestedName)

        setForeground(createForegroundInfo(safeName, 0))

        val modelsDir = File(applicationContext.filesDir, "models").apply { mkdirs() }
        val finalFile = File(modelsDir, safeName)
        val partFile = File(modelsDir, "$safeName.part")
        val etagFile = File(modelsDir, "$safeName.etag")

        try {
            val existingBytes = partFile.takeIf(File::exists)?.length() ?: 0L
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("Accept-Encoding", "identity")
                if (existingBytes > 0L) {
                    setRequestProperty("Range", "bytes=$existingBytes-")
                    etagFile.takeIf(File::exists)?.readText()?.trim()?.takeIf(String::isNotEmpty)?.let {
                        setRequestProperty("If-Range", it)
                    }
                }
            }

            try {
                val status = connection.responseCode
                if (status !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) {
                    throw IOException("Download server returned HTTP $status")
                }

                val canResume = status == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0L
                val writeOffset = if (canResume) existingBytes else 0L
                if (!canResume && partFile.exists()) {
                    partFile.delete()
                }

                connection.getHeaderField("ETag")?.takeIf(String::isNotBlank)?.let(etagFile::writeText)

                val responseBytes = connection.contentLengthLong.coerceAtLeast(0L)
                val totalBytes = when {
                    expectedSize > 0L -> expectedSize
                    canResume -> writeOffset + responseBytes
                    else -> responseBytes
                }

                RandomAccessFile(partFile, "rw").use { output ->
                    output.seek(writeOffset)
                    connection.inputStream.buffered(1024 * 1024).use { input ->
                        val buffer = ByteArray(1024 * 1024)
                        var downloaded = writeOffset
                        while (true) {
                            if (isStopped) return@withContext Result.retry()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            val progress = if (totalBytes > 0L) {
                                ((downloaded * 100L) / totalBytes).coerceIn(0L, 100L).toInt()
                            } else {
                                0
                            }
                            setProgress(workDataOf(KEY_PROGRESS to progress, KEY_DOWNLOADED to downloaded))
                            setForeground(createForegroundInfo(safeName, progress))
                        }
                    }
                    output.fd.sync()
                }
            } finally {
                connection.disconnect()
            }

            if (expectedSize > 0L && partFile.length() != expectedSize) {
                return@withContext failure("size_mismatch", partFile.length())
            }

            if (expectedSha256.isNotBlank()) {
                val actual = sha256(partFile)
                if (!actual.equals(expectedSha256, ignoreCase = true)) {
                    partFile.delete()
                    etagFile.delete()
                    return@withContext failure("sha256_mismatch")
                }
            }

            moveAtomically(partFile, finalFile)
            etagFile.delete()
            Result.success(
                workDataOf(
                    KEY_FILE_PATH to finalFile.absolutePath,
                    KEY_PROGRESS to 100,
                ),
            )
        } catch (error: IOException) {
            Result.retry()
        } catch (error: SecurityException) {
            failure("security_error")
        }
    }

    private fun createForegroundInfo(fileName: String, progress: Int): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model downloads",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading AI model")
            .setContentText(fileName)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun failure(reason: String, actualSize: Long = -1L): Result = Result.failure(
        workDataOf(
            KEY_ERROR to reason,
            KEY_DOWNLOADED to actualSize,
        ),
    )

    private fun sanitizeFileName(name: String): String {
        val base = File(name).name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        require(base.isNotBlank() && base != "." && base != "..") { "Invalid model filename" }
        return base
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun moveAtomically(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        runCatching {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            if (destination.exists() && !destination.delete()) {
                throw IOException("Could not replace existing model")
            }
            if (!source.renameTo(destination)) {
                throw IOException("Could not activate downloaded model")
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "sankofa_model_downloads"
        private const val NOTIFICATION_ID = 2201
        const val KEY_URL = "url"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_SHA256 = "sha256"
        const val KEY_SIZE = "size"
        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED = "downloaded"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR = "error"

        fun enqueue(
            context: Context,
            url: String,
            fileName: String,
            sha256: String = "",
            expectedSize: Long = -1L,
        ) {
            require(url.startsWith("https://")) { "Model downloads must use HTTPS" }
            val data = Data.Builder()
                .putString(KEY_URL, url)
                .putString(KEY_FILE_NAME, fileName)
                .putString(KEY_SHA256, sha256)
                .putLong(KEY_SIZE, expectedSize)
                .build()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val work = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .addTag("model-download:$fileName")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "model-download:$fileName",
                ExistingWorkPolicy.KEEP,
                work,
            )
        }
    }
}
