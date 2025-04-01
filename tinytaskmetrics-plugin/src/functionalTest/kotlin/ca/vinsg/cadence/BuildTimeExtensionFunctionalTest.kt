package ca.vinsg.cadence

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Functional tests for the BuildTimeExtension.
 */
class BuildTimeExtensionFunctionalTest {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    private fun setupProject() {
        // Create settings.gradle
        settingsFile = testProjectDir.newFile("settings.gradle")
        settingsFile.writeText("""
            rootProject.name = 'extension-test'
        """.trimIndent())

        // Create build.gradle
        buildFile = testProjectDir.newFile("build.gradle")
        buildFile.writeText("""
            plugins {
                id 'ca.vinsg.cadence'
            }
            
            // Default configuration - no explicit extension settings
        """.trimIndent())
    }

    @Test
    fun `plugin applies with default extension values`() {
        // Setup the test project
        setupProject()

        // Run the build
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withArguments("buildTimeSummary", "--info")
            .build()

        // Verify the task executed successfully
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildTimeSummary")?.outcome)

        // Check that the default output files were created in the build directory
        val buildDir = File(testProjectDir.root, "build")
        val csvFile = File(buildDir, "build-time-summary.csv")
        assertTrue("CSV file should exist", csvFile.exists())
        
        // By default, exportTxt is false, so no TXT file should be created
        val txtFile = File(buildDir, "build-time-summary.txt")
        assertFalse("TXT file should not exist by default", txtFile.exists())
    }

    @Test
    fun `extension can configure exportTxt`() {
        // Setup the test project
        setupProject()
        
        // Update build.gradle to configure the extension
        buildFile.writeText("""
            plugins {
                id 'ca.vinsg.cadence'
            }
            
            buildTime {
                exportTxt = true
            }
        """.trimIndent())

        // Run the build
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withArguments("buildTimeSummary", "--info")
            .build()

        // Verify the task executed successfully
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildTimeSummary")?.outcome)

        // Check that both output files were created
        val buildDir = File(testProjectDir.root, "build")
        val csvFile = File(buildDir, "build-time-summary.csv")
        val txtFile = File(buildDir, "build-time-summary.txt")
        
        assertTrue("CSV file should exist", csvFile.exists())
        assertTrue("TXT file should exist when exportTxt=true", txtFile.exists())
    }

    @Test
    fun `extension can configure custom output directory`() {
        // Setup the test project
        setupProject()
        
        // Update build.gradle to configure the extension with a custom output directory
        buildFile.writeText("""
            plugins {
                id 'ca.vinsg.cadence'
            }
            
            buildTime {
                exportTxt = true
                outputDir = file('custom-output')
            }
        """.trimIndent())

        // Run the build
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withArguments("buildTimeSummary", "--info")
            .build()

        // Verify the task executed successfully
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildTimeSummary")?.outcome)

        // Check that the output files were created in the custom directory
        val customDir = File(testProjectDir.root, "custom-output")
        val csvFile = File(customDir, "build-time-summary.csv")
        val txtFile = File(customDir, "build-time-summary.txt")
        
        assertTrue("CSV file should exist in custom directory", csvFile.exists())
        assertTrue("TXT file should exist in custom directory", txtFile.exists())
        
        // Check that the files were NOT created in the build directory
        val buildDir = File(testProjectDir.root, "build")
        val csvFileInBuildDir = File(buildDir, "build-time-summary.csv")
        val txtFileInBuildDir = File(buildDir, "build-time-summary.txt")
        
        assertFalse("CSV file should not exist in build directory", csvFileInBuildDir.exists())
        assertFalse("TXT file should not exist in build directory", txtFileInBuildDir.exists())
    }
}