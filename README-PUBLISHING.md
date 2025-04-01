# Publishing to the Gradle Plugin Portal

This document provides instructions on how to publish the TinyTaskMetrics plugin to the Gradle Plugin Portal.

## Prerequisites

1. You need to have a Gradle Plugin Portal account. If you don't have one, sign up at [https://plugins.gradle.org/](https://plugins.gradle.org/).

2. After signing up, you need to generate API keys from your profile page.

## Setting up API Keys

1. Log in to the Gradle Plugin Portal at [https://plugins.gradle.org/](https://plugins.gradle.org/).

2. Navigate to your profile page by clicking on your username in the top-right corner.

3. Click on the "API Keys" tab.

4. Generate a new API key if you don't have one already.

5. Create a `gradle.properties` file in your `~/.gradle/` directory (or add to it if it already exists) with the following content:

```properties
gradle.publish.key=<your API key>
gradle.publish.secret=<your API secret>
```

## Publishing the Plugin

1. Make sure you have updated the plugin metadata in the `build.gradle.kts` file:
   - Group: `ca.vinsg`
   - Version: `1.0.0` (update this for each new release)
   - Website and VCS URL: Update these to point to your actual repository

2. Run the following command from the project root directory:

```bash
./gradlew :tinytaskmetrics-plugin:publishPlugins
```

3. The plugin will be published to the Gradle Plugin Portal and will be available at:
   `https://plugins.gradle.org/plugin/ca.vinsg.tinytaskmetrics`

## Updating the Plugin

To publish a new version of the plugin:

1. Update the `version` in the `tinytaskmetrics-plugin/build.gradle.kts` file.

2. Make your changes to the plugin code.

3. Run the publishing command again:

```bash
./gradlew :tinytaskmetrics-plugin:publishPlugins
```

## Verifying the Publication

After publishing, you can verify that your plugin is available by:

1. Visiting `https://plugins.gradle.org/plugin/ca.vinsg.tinytaskmetrics`

2. Testing it in a new project by adding the following to your `build.gradle` or `build.gradle.kts`:

```kotlin
plugins {
    id("ca.vinsg.tinytaskmetrics") version "1.0.0"
}
```

## Troubleshooting

If you encounter issues during the publishing process:

1. Make sure your API keys are correctly set up in the `~/.gradle/gradle.properties` file.

2. Check that the plugin ID is unique and follows the recommended format (typically a reverse domain name).

3. Ensure that all required metadata (website, vcsUrl, tags) is provided.

4. Review the error messages in the Gradle output for specific issues.