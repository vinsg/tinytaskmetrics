# tinyTaskMetrics

A tiny Gradle plugin to record task execution times and basic system metrics for your builds, compatible with the Configuration Cache.

## Quick Start

Apply the plugin to the project(s) where you want reports generated.

### With Kotlin DSL (build.gradle.kts):

```kotlin
plugins {
    id("ca.vinsg.tinytaskmetrics") version "0.0.1" 
}
```

### With Groovy DSL (build.gradle):

```groovy
plugins {
    id 'ca.vinsg.tinytaskmetrics' version '0.0.1'
}
```

Now, simply run your regular build tasks. The report generation task is automatically triggered after common tasks like `assemble`, `check`, or `build` complete for the project where the plugin is applied.

```bash
# Example: Running assembleDebug will generate the report afterwards
./gradlew :app:assembleDebug 
```

Look for the report files in the `build/reports/` directory of the project where the plugin was applied (e.g., `app/build/reports/`).

## Features

- ⏱️ Automatically records the execution time of tasks within the build.
- 💻 Captures basic system information (OS, CPU, RAM, JVM Memory) for context.
- ✅ **Configuration Cache Compatible:** Designed to work efficiently with Gradle's configuration cache.
- 📄 Generates per-project reports in CSV format (`tinytaskmetrics-summary.csv`) for detailed analysis.
- 📝 Optionally generates a human-readable TXT report (`tinytaskmetrics-summary.txt`).
- ⚙️ Report generation runs automatically after common build tasks finish for the project.

## Configuration

Configure the plugin within the project's build script using the `tinyTaskMetrics` extension:

```kotlin
// Kotlin DSL (e.g., app/build.gradle.kts)
tinyTaskMetrics {
    // Enable text report generation (CSV is always generated)
    exportTxt.set(true) 

    // Specify a custom output directory (defaults to <project>/build/reports)
    // outputDir.set(layout.projectDirectory.dir("custom/metrics/output"))

    // Custom file names for reports (placed within outputDir)
    // csvFileName.set("my-project-metrics.csv")
    // txtFileName.set("my-project-metrics.txt")
}
```

```groovy
// Groovy DSL (e.g., app/build.gradle)
tinyTaskMetrics {
    exportTxt = true

    // outputDir = layout.projectDirectory.dir("custom/metrics/output")
    // csvFileName = "my-project-metrics.csv"
    // txtFileName = "my-project-metrics.txt"
}
```

### Configuration Options

| Option        | Type                | Default                   | Description                                                                                             |
|---------------|---------------------|---------------------------|---------------------------------------------------------------------------------------------------------|
| `exportTxt`   | `Property<Boolean>` | `false`                   | Whether to generate the human-readable text report (`.txt`). The CSV report is always generated.        |
| `outputDir`   | `DirectoryProperty` | `<project>/build/reports` | The directory where report files will be saved within the applying project.                             |
| `csvFileName` | `Property<String>`  | `"build-metrics.csv"`     | The name of the CSV output file, placed within the `outputDir`.                                         |
| `txtFileName` | `Property<String>`  | `"build-metrics.txt"`     | The name of the TXT output file (if `exportTxt` is true), placed within the `outputDir`.                |

## Build Report Files

The plugin generates report files in the `build/reports/` directory of **each project where it is applied**:

1.  **CSV File** (`build-metrics.csv` by default): Contains detailed task timing data, skipped status, and system information in CSV format. This file is appended to on subsequent builds within the same directory, allowing for historical tracking via the `BuildId` column.
2.  **TXT File** (`build-metrics.txt` by default): Optional (enable with `exportTxt = true`), provides a human-readable summary including system info and top tasks by duration for the specific project. This file is overwritten on each build.

## Roadmap

- [ ] Create CI for build and release.
- [ ] Automate Gradle Portal publishing.
- [ ] Add a minimal HTML report option
- [ ] Add the ability to pass the task we want to watch.
- [ ] Add more comprehensive functional tests