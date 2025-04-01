package ca.vinsg.tinytaskmetrics

import java.io.IOException
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import org.gradle.api.file.DirectoryProperty
import java.io.File
import java.util.concurrent.TimeUnit
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parameters for the TinyTaskMetricsTrackerService.
 * Passed from the TinyTaskMetrics Plugin during registration.
 */
interface TinyTaskMetricsTrackerParameters : BuildServiceParameters {
    val systemInfoService: Property<SystemInfoService>
    val outputDirectory: DirectoryProperty
    val exportTxt: Property<Boolean>
    val csvFileName: Property<String>
    val txtFileName: Property<String>
    val androidProjectPaths: SetProperty<String> // Use SetProperty for collections
}

/**
 * TinyTaskMetricsTracker Service that tracks task execution times and generates reports at the end of the build.
 */
abstract class TinyTaskMetricsTrackerService : BuildService<TinyTaskMetricsTrackerParameters>,
    OperationCompletionListener, AutoCloseable {

    private val taskTimings = ConcurrentHashMap<String, Long>()
    private val taskSkipped = ConcurrentHashMap<String, Boolean>()
    private val isAndroidTaskMap = ConcurrentHashMap<String, Boolean>()

    private val buildStartTime = System.currentTimeMillis()
    private val buildId = UUID.randomUUID().toString()

    // Listener method called by Gradle for task finish events
    override fun onFinish(event: FinishEvent) {
        if (event is TaskFinishEvent) {
            val taskPath = event.descriptor.taskPath
            val duration = event.result.endTime - event.result.startTime // Duration from event
            val skipped = event.result is TaskSkippedResult

            taskTimings[taskPath] = duration
            taskSkipped[taskPath] = skipped

            // Check if the task belongs to a registered Android project path
            val androidProjects = parameters.androidProjectPaths.getOrElse(emptySet())
            val isAndroid = androidProjects.any { projPath ->
                taskPath.startsWith("$projPath:") || taskPath == projPath
            }
            isAndroidTaskMap[taskPath] = isAndroid

            // Optional: Log for debugging during development
            // if (isAndroid) {
            //     val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(duration)
            //     val skippedStatus = if (skipped) "SKIPPED" else "EXECUTED"
            //     println("Android build task $taskPath took $durationSeconds seconds (Status: $skippedStatus)")
            // }
        }
    }

    override fun close() {
        println("TinyTaskMetricsTrackerService: Build finished, generating reports...")
        generateAndSaveReports()
        println("TinyTaskMetricsTrackerService: Reports generated.")
    }

    private fun generateAndSaveReports() {
        val totalBuildTime = System.currentTimeMillis() - buildStartTime
        val systemInfo = parameters.systemInfoService.get() // Access system info lazily HERE

        // Convert collected data
        val taskData = convertToTaskData()

        // Get configuration from parameters
        val outputDir = parameters.outputDirectory.get().asFile
        val doExportTxt = parameters.exportTxt.getOrElse(false)
        val csvFile = outputDir.resolve(parameters.csvFileName.get())
        val txtFile = outputDir.resolve(parameters.txtFileName.get())

        outputDir.mkdirs() // Ensure directory exists

        // --- Reporting Logic (moved from ReportGenerator to simplify) ---

        // Print Summary to Console
        printSummaryInternal(taskData, totalBuildTime, systemInfo)

        // Save Summary to CSV
        saveSummaryToCsvInternal(csvFile, taskData, systemInfo)

        // Save Summary to TXT (if enabled)
        if (doExportTxt) {
            saveSummaryToTxtInternal(txtFile, taskData, totalBuildTime, systemInfo)
        }
    }

    /** Converts internal maps to the final TaskData structure */
    private fun convertToTaskData(): Map<String, TaskData> {
        return taskTimings.mapValues { (taskPath, duration) ->
            TaskData(
                durationMs = duration,
                skipped = taskSkipped[taskPath] ?: false,
                isAndroidTask = isAndroidTaskMap[taskPath] ?: false
            )
        }
    }

    // --- Internal Reporting Methods ---

    private fun printSummaryInternal(
        taskData: Map<String, TaskData>,
        totalBuildTimeMs: Long,
        systemInfo: SystemInfoService // Receive SystemInfoService instance
    ) {
        val totalBuildTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(totalBuildTimeMs)

        println("\n=== TinyTaskMetrics Build Time Summary ===")
        println("Build ID: $buildId")
        println("Total build time: $totalBuildTimeSeconds seconds")

        println("\n=== System Information ===")
        println("OS: ${systemInfo.osNameAndVersion}")
        println("Processor: ${systemInfo.processorName}") // Access lazy property
        println("Total RAM: ${systemInfo.totalPhysicalRam} MB") // Access lazy property
        println("JVM Max Memory: ${systemInfo.jvmMaxMemory} MB") // Access lazy property

        val androidTasks = taskData.filter { it.value.isAndroidTask }
        if (androidTasks.isNotEmpty()) {
            println("\nTop Android Build Tasks (by duration):")
            androidTasks.entries
                .sortedByDescending { it.value.durationMs }
                .take(15) // Show top N tasks
                .forEach { (taskPath, data) ->
                    val durationSec = data.durationMs / 1000.0
                    val skippedTag = if (data.skipped) " [SKIPPED]" else ""
                    println(String.format("  %.3f s - %s%s", durationSec, taskPath, skippedTag))
                }
        } else {
            println("\nNo Android-specific tasks recorded.")
        }
        println("=========================================\n")
    }

    private fun saveSummaryToCsvInternal(
        csvFile: File,
        taskData: Map<String, TaskData>,
        systemInfo: SystemInfoService // Receive SystemInfoService instance
    ) {
        try {
            val fileExists = csvFile.exists()
            FileWriter(csvFile, true).use { writer -> // Append mode
                if (!fileExists) {
                    writer.append("BuildId,Timestamp,Task,Duration_ms,Skipped,OS,Processor,Total_RAM_MB,JVM_Max_Memory_MB\n")
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                val timestamp = dateFormat.format(Date())

                // Fetch lazy system info once per build report
                val osInfo = systemInfo.osNameAndVersion
                val procInfo = systemInfo.processorName
                val ramInfo = systemInfo.totalPhysicalRam
                val jvmMemInfo = systemInfo.jvmMaxMemory

                taskData.entries
                    .sortedBy { it.key } // Sort by task path for consistent order
                    .forEach { (taskPath, data) ->
                        // Escape commas in task path if necessary
                        val safeTaskPath = taskPath.replace(",", ";")
                        writer.append("$buildId,$timestamp,$safeTaskPath,${data.durationMs},${data.skipped},$osInfo,$procInfo,$ramInfo,$jvmMemInfo\n")
                    }
            }
            println("Build time summary CSV appended to: ${csvFile.absolutePath}")
        } catch (e: IOException) {
            System.err.println("Error writing CSV report to ${csvFile.absolutePath}: ${e.message}")
        }
    }

    private fun saveSummaryToTxtInternal(
        txtFile: File,
        taskData: Map<String, TaskData>,
        totalBuildTimeMs: Long,
        systemInfo: SystemInfoService // Receive SystemInfoService instance
    ) {
        try {
            txtFile.writeText("=== TinyTaskMetrics Build Time Summary ===\n")
            txtFile.appendText("Build ID: $buildId\n")
            val totalBuildTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(totalBuildTimeMs)
            txtFile.appendText("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
            txtFile.appendText("Total build time: $totalBuildTimeSeconds seconds\n")

            // Fetch lazy system info once
            val osInfo = systemInfo.osNameAndVersion
            val procInfo = systemInfo.processorName
            val ramInfo = systemInfo.totalPhysicalRam
            val jvmMemInfo = systemInfo.jvmMaxMemory

            txtFile.appendText("\n=== System Information ===\n")
            txtFile.appendText("OS: $osInfo\n")
            txtFile.appendText("Processor: $procInfo\n")
            txtFile.appendText("Total RAM: $ramInfo MB\n")
            txtFile.appendText("JVM Max Memory: $jvmMemInfo MB\n")

            val androidTasks = taskData.filter { it.value.isAndroidTask }
            if (androidTasks.isNotEmpty()) {
                txtFile.appendText("\nTop Android Build Tasks (by duration):\n")
                androidTasks.entries
                    .sortedByDescending { it.value.durationMs }
                    .take(25) // Show more in text file maybe
                    .forEach { (taskPath, data) ->
                        val durationSec = data.durationMs / 1000.0
                        val skippedTag = if (data.skipped) " [SKIPPED]" else ""
                        txtFile.appendText(String.format("  %.3f s - %s%s\n", durationSec, taskPath, skippedTag))
                    }
            } else {
                txtFile.appendText("\nNo Android-specific tasks recorded.\n")
            }
            txtFile.appendText("=========================================\n")
            println("Build time summary TXT saved to: ${txtFile.absolutePath}")
        } catch (e: IOException) {
            System.err.println("Error writing TXT report to ${txtFile.absolutePath}: ${e.message}")
        }
    }
}