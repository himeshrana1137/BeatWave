package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReport(
    val id: String,
    val timestamp: Long,
    val formattedTime: String,
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
    val exceptionType: String,
    val exceptionMessage: String,
    val stackTrace: String,
    val fullContent: String
)

object CrashReportManager {

    private const val PREFS_NAME = "beatwave_crash_reporting"
    private const val KEY_HAS_UNSEEN_CRASH = "has_unseen_crash"
    private const val KEY_LATEST_CRASH_TIME = "latest_crash_time"
    private const val MAX_REPORTS = 10
    private const val REPORTS_DIR = "crash_reports"

    private var isInstalled = false

    fun install(context: Context) {
        if (isInstalled) return
        isInstalled = true

        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashReport(appContext, thread, throwable)
            } catch (e: Exception) {
                Log.e("CrashReportManager", "Error saving crash report: ${e.message}")
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashReport(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val formattedTime = sdf.format(Date(timestamp))

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }

        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"
        val exceptionType = throwable::class.java.name
        val exceptionMessage = throwable.message ?: "No message"
        val stackTrace = Log.getStackTraceString(throwable)

        val reportContent = buildString {
            appendLine("=== BEATWAVE CRASH REPORT ===")
            appendLine("Time: $formattedTime")
            appendLine("Timestamp: $timestamp")
            appendLine("App Version: $appVersion")
            appendLine("Android Version: $androidVersion")
            appendLine("Device Model: $deviceModel")
            appendLine("Thread: ${thread.name} (id: ${thread.id})")
            appendLine("Exception: $exceptionType")
            appendLine("Message: $exceptionMessage")
            appendLine()
            appendLine("--- STACK TRACE ---")
            appendLine(stackTrace)
            appendLine("=== END OF REPORT ===")
        }

        val dir = File(context.filesDir, REPORTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val reportFile = File(dir, "crash_${timestamp}.txt")
        reportFile.writeText(reportContent)

        pruneOldReports(dir)

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_HAS_UNSEEN_CRASH, true)
            .putLong(KEY_LATEST_CRASH_TIME, timestamp)
            .apply()
    }

    private fun pruneOldReports(dir: File) {
        try {
            val files = dir.listFiles { _, name -> name.startsWith("crash_") && name.endsWith(".txt") }
                ?: return
            if (files.size > MAX_REPORTS) {
                files.sortByDescending { it.lastModified() }
                for (i in MAX_REPORTS until files.size) {
                    files[i].delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun getCrashReports(context: Context): List<CrashReport> {
        val list = mutableListOf<CrashReport>()
        try {
            val dir = File(context.filesDir, REPORTS_DIR)
            if (!dir.exists()) return emptyList()

            val files = dir.listFiles { _, name -> name.startsWith("crash_") && name.endsWith(".txt") }
                ?: return emptyList()

            files.sortByDescending { it.lastModified() }

            for (file in files) {
                try {
                    val content = file.readText()
                    val report = parseReport(file.name, content, file.lastModified())
                    list.add(report)
                } catch (e: Exception) {
                    Log.w("CrashReportManager", "Could not parse crash file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("CrashReportManager", "Error reading crash reports: ${e.message}")
        }
        return list
    }

    private fun parseReport(fileName: String, content: String, fileModified: Long): CrashReport {
        var timeStr = ""
        var timestamp = fileModified
        var appVersion = "Unknown"
        var androidVersion = "Unknown"
        var deviceModel = "Unknown"
        var exceptionType = "Exception"
        var exceptionMessage = ""
        val stackTraceBuilder = StringBuilder()
        var inStackTrace = false

        content.lineSequence().forEach { line ->
            when {
                line.startsWith("Time: ") -> timeStr = line.removePrefix("Time: ").trim()
                line.startsWith("Timestamp: ") -> timestamp = line.removePrefix("Timestamp: ").trim().toLongOrNull() ?: timestamp
                line.startsWith("App Version: ") -> appVersion = line.removePrefix("App Version: ").trim()
                line.startsWith("Android Version: ") -> androidVersion = line.removePrefix("Android Version: ").trim()
                line.startsWith("Device Model: ") -> deviceModel = line.removePrefix("Device Model: ").trim()
                line.startsWith("Exception: ") -> exceptionType = line.removePrefix("Exception: ").trim()
                line.startsWith("Message: ") -> exceptionMessage = line.removePrefix("Message: ").trim()
                line.startsWith("--- STACK TRACE ---") -> inStackTrace = true
                line.startsWith("=== END OF REPORT ===") -> inStackTrace = false
                inStackTrace -> stackTraceBuilder.appendLine(line)
            }
        }

        return CrashReport(
            id = fileName,
            timestamp = timestamp,
            formattedTime = if (timeStr.isNotBlank()) timeStr else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp)),
            appVersion = appVersion,
            androidVersion = androidVersion,
            deviceModel = deviceModel,
            exceptionType = exceptionType,
            exceptionMessage = exceptionMessage,
            stackTrace = stackTraceBuilder.toString().trim(),
            fullContent = content
        )
    }

    fun clearCrashReports(context: Context) {
        try {
            val dir = File(context.filesDir, REPORTS_DIR)
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e("CrashReportManager", "Error clearing reports: ${e.message}")
        }
    }

    fun checkAndConsumePendingCrash(context: Context): CrashReport? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_UNSEEN_CRASH, false)) return null

        prefs.edit().putBoolean(KEY_HAS_UNSEEN_CRASH, false).apply()
        val reports = getCrashReports(context)
        return reports.firstOrNull()
    }

    fun shareCrashReport(context: Context, report: CrashReport) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "BeatWave Crash Report - ${report.formattedTime}")
                putExtra(Intent.EXTRA_TEXT, report.fullContent)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Crash Report").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Log.e("CrashReportManager", "Error sharing report: ${e.message}")
        }
    }
}
