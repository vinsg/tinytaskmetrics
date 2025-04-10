package ca.vinsg.tinytaskmetrics

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

/**
 * Gradle plugin that records and reports build times using Configuration Cache compatible Build Services.
 */
abstract class TinyTaskMetricsPlugin @Inject constructor(
    // Inject the listener registry service provided by Gradle
    private val listenerRegistry: BuildEventsListenerRegistry
) : Plugin<Project> {

    override fun apply(project: Project) {
        // This plugin registers build-scoped services, so it should only be applied once,
        // ideally to the root project. Applying it multiple times won't hurt due to
        // registerIfAbsent, but it's cleaner to apply it only at the root.
        if (project.parent == null) { // Check if it's the root project
            configurePluginForBuild(project)
        } else {
            project.logger.warn(
                "[TinyTaskMetrics] Plugin should ideally be applied only to the root project " +
                        "in settings.gradle.kts or the root build.gradle.kts."
            )
        }
    }

    private fun configurePluginForBuild(rootProject: Project) {
        // Create the extension on the root project
        val extension = rootProject.extensions.create("tinyTaskMetrics", TinyTaskMetricsExtension::class.java)

        // Register SystemInfoService once for the build
        val systemInfoServiceProvider: Provider<SystemInfoService> =
            rootProject.gradle.sharedServices.registerIfAbsent(
                "tinyTaskMetricsSystemInfoService", // Unique name
                SystemInfoService::class.java
            ) {}

        // Provider to collect Android project paths safely during configuration
        val androidProjectPathsProvider: Provider<Set<String>> = rootProject.provider {
            rootProject.allprojects // Iterate over all projects in the build
                .filter { p ->
                    // Check for common Android plugin IDs
                    p.pluginManager.hasPlugin("com.android.application") ||
                            p.pluginManager.hasPlugin("com.android.library") ||
                            p.pluginManager.hasPlugin("com.android.dynamic-feature") ||
                            p.pluginManager.hasPlugin("com.android.test") ||
                            p.pluginManager.hasPlugin("com.android.asset-pack")
                }
                .map { it.path }
                .toSet()
        }

        val defaultOutputDirProvider = rootProject.layout.buildDirectory.dir("reports")

        // Register TinyTaskMetricsTrackerService once for the build
        val tinyTaskMetricsTrackerProvider: Provider<TinyTaskMetricsTrackerService> =
            rootProject.gradle.sharedServices.registerIfAbsent(
                "tinyTaskMetricsTracker",
                TinyTaskMetricsTrackerService::class.java
            ) { spec ->
                spec.parameters.systemInfoService.set(systemInfoServiceProvider)
                spec.parameters.androidProjectPaths.set(androidProjectPathsProvider)
                spec.parameters.outputDirectory.set(extension.outputDir.orElse(defaultOutputDirProvider))
                spec.parameters.exportTxt.set(extension.exportTxt)
                spec.parameters.csvFileName.set(extension.csvFileName)
                spec.parameters.txtFileName.set(extension.txtFileName)
            }

        listenerRegistry.onTaskCompletion(tinyTaskMetricsTrackerProvider)

        rootProject.logger.lifecycle("[TinyTaskMetrics] Plugin applied and services registered.")
    }
}