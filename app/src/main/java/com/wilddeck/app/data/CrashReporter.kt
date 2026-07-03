package com.wilddeck.app.data

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

object CrashReporter {
    private const val TAG = "WildDeckCrashReporter"
    private const val REPORT_DIR = "crash-reports"
    private const val LATEST_REPORT = "latest-crash.txt"
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val report = buildReport(appContext, thread, throwable)
                writeReport(appContext, report)
                Log.e(TAG, report)
            }.onFailure {
                Log.e(TAG, "Failed to write crash report.", it)
            }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(10)
            }
        }
    }

    fun latestReport(context: Context): String? {
        val latest = File(reportDirectory(context), LATEST_REPORT)
        return latest.takeIf { it.exists() }?.readText()
    }

    fun clearReports(context: Context) {
        reportDirectory(context).listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun writeReport(context: Context, report: String) {
        val directory = reportDirectory(context).apply { mkdirs() }
        File(directory, LATEST_REPORT).writeText(report)
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(Date())
        File(directory, "crash-$timestamp.txt").writeText(report)
    }

    private fun reportDirectory(context: Context): File =
        File(context.filesDir, REPORT_DIR)

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionName = packageInfo?.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString()
        } ?: "unknown"
        return buildString {
            appendLine("WildDecks crash report")
            appendLine("Time: ${Date()}")
            appendLine("Package: ${context.packageName}")
            appendLine("Version: $versionName ($versionCode)")
            appendLine("Thread: ${thread.name}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}")
            appendLine()
            appendLine(Log.getStackTraceString(throwable))
        }
    }
}
