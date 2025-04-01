package ca.vinsg.tinytaskmetrics

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException // Import IOException

/**
 * Build Service responsible for providing system information lazily.
 * This avoids executing external processes during the configuration phase.
 */
abstract class SystemInfoService : BuildService<BuildServiceParameters.None> {

    // Use lazy delegate to defer execution until first access
    val osName: String by lazy { System.getProperty("os.name") ?: "Unknown OS" }
    val osVersion: String by lazy { System.getProperty("os.version") ?: "Unknown Version" }
    val osNameAndVersion: String by lazy { "$osName $osVersion" }
    val jvmMaxMemory: Long by lazy {
        try {
            Runtime.getRuntime().maxMemory() / (1024 * 1024) // in MB
        } catch (e: Exception) {
            0L // Default value on error
        }
    }

    val processorName: String by lazy { fetchProcessorName() }
    val totalPhysicalRam: Long by lazy { fetchTotalPhysicalRam() } // in MB

    /**
     * Lazily retrieves the processor/chip name of the system.
     */
    private fun fetchProcessorName(): String {
        // Add logging for debugging if needed: println("Fetching processor name...")
        return try {
            when {
                osName.startsWith("Windows", ignoreCase = true) -> {
                    // Getting env var is safe
                    System.getenv("PROCESSOR_IDENTIFIER") ?: executeCommand(listOf("wmic", "cpu", "get", "name"))?.lines()?.getOrNull(1)?.trim() ?: "Unknown Windows CPU"
                }
                osName.startsWith("Mac", ignoreCase = true) -> {
                    executeCommand(listOf("sysctl", "-n", "machdep.cpu.brand_string")) ?: "Unknown Mac CPU"
                }
                osName.startsWith("Linux", ignoreCase = true) -> {
                    val cpuInfo = executeCommand(listOf("cat", "/proc/cpuinfo"))
                    cpuInfo?.lines()
                        ?.find { it.trim().startsWith("model name") }
                        ?.substringAfter(":")?.trim() ?: "Unknown Linux CPU"
                }
                else -> "Unsupported OS for CPU Info"
            }
        } catch (e: Exception) {
            System.err.println("Error fetching processor name: ${e.message}")
            "Error Fetching CPU"
        }
    }

    /**
     * Lazily retrieves the total physical RAM of the system in MB.
     */
    private fun fetchTotalPhysicalRam(): Long {
        // Add logging for debugging if needed: println("Fetching total physical RAM...")
        return try {
            // Try using OperatingSystemMXBean first
            val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
            // Check if the method exists and the value is reasonable (some JVMs might return 0 or negative)
            val memFromBean = osBean.totalMemorySize // Changed from totalPhysicalMemorySize
            if (memFromBean > 0) {
                return memFromBean / (1024 * 1024) // Convert to MB
            }

            // Fallback methods if bean didn't work
            when {
                osName.startsWith("Windows", ignoreCase = true) -> {
                    val output = executeCommand(listOf("wmic", "ComputerSystem", "get", "TotalPhysicalMemory"))
                    output?.lines()?.getOrNull(1)?.trim()?.toLongOrNull()?.div(1024 * 1024) ?: 0L // Bytes to MB
                }
                osName.startsWith("Mac", ignoreCase = true) -> {
                    val output = executeCommand(listOf("sysctl", "hw.memsize"))
                    output?.substringAfter(":")?.trim()?.toLongOrNull()?.div(1024 * 1024) ?: 0L // Bytes to MB
                }
                osName.startsWith("Linux", ignoreCase = true) -> {
                    val memInfo = executeCommand(listOf("cat", "/proc/meminfo"))
                    memInfo?.lines()
                        ?.find { it.startsWith("MemTotal:") }
                        ?.split("\\s+".toRegex())?.getOrNull(1)?.toLongOrNull()?.div(1024) // KB to MB
                        ?: 0L
                }
                else -> 0L
            }
        } catch (e: Exception) {
            System.err.println("Error fetching total physical RAM: ${e.message}")
            0L // Default value on error
        }
    }

    /**
     * Helper function to execute external commands safely.
     * Returns null if the command fails or produces no output.
     */
    private fun executeCommand(command: List<String>): String? {
        return try {
            val process = ProcessBuilder(command).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText().trim() // Read all output
            val exitCode = process.waitFor()
            reader.close()
            if (exitCode == 0 && output.isNotEmpty()) output else null
        } catch (e: IOException) {
            // Log error if necessary, e.g., command not found
            System.err.println("Failed to execute command '${command.joinToString(" ")}': ${e.message}")
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            System.err.println("Command execution interrupted '${command.joinToString(" ")}': ${e.message}")
            null
        } catch (e: Exception) { // Catch unexpected errors
            System.err.println("Unexpected error executing command '${command.joinToString(" ")}': ${e.message}")
            null
        }
    }
}