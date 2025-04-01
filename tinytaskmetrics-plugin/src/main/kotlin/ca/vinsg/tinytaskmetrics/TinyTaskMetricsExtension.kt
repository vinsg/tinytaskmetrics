package ca.vinsg.tinytaskmetrics

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring the TinyTaskMetrics plugin.
 *
 * Allows customization of the tracking and reporting behavior.
 */
abstract class TinyTaskMetricsExtension @Inject constructor(objects: ObjectFactory) { // Make abstract for property injection

    /**
     * Whether to export build metrics summary to a text file.
     * When true, a human-readable text report will be generated.
     * Default is false (only CSV output is generated).
     */
    val exportTxt: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    /**
     * Custom output directory for build metrics summary files.
     * If not set, files will be saved in the project's build directory (`build/reports`).
     */
    val outputDir: DirectoryProperty = objects.directoryProperty() // No default here, set in plugin apply

    /**
     * Custom file path for the CSV output file relative to the outputDir.
     * If not set, the file will be named "build-metrics.csv".
     */
    val csvFileName: Property<String> = objects.property(String::class.java).convention("build-metrics.csv")

    /**
     * Custom file path for the TXT output file relative to the outputDir.
     * If not set, the file will be named "build-metrics.txt".
     */
    val txtFileName: Property<String> = objects.property(String::class.java).convention("build-metrics.txt")
}