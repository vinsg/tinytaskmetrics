plugins {
    kotlin("jvm") version "2.1.0"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "ca.vinsg"
version = "0.0.1"


dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    testImplementation("junit:junit:4.13.2")
}

gradlePlugin {
    // Define the plugins
    website.set("https://github.com/vinsg/tinytaskmetrics")
    vcsUrl.set("https://github.com/vinsg/tinytaskmetrics.git")

    plugins {
        create("tinyTaskMetrics") {
            id = "ca.vinsg.tinytaskmetrics"
            implementationClass = "ca.vinsg.tinytaskmetrics.TinyTaskMetricsPlugin"
            displayName = "TinyTaskMetrics Plugin"
            description = "A tiny plugin that records task times and system metrics."
            tags.set(listOf("android", "build", "metrics", "performance"))
        }
    }
}

repositories {
    mavenCentral()
    google()
}
