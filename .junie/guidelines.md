# Greeting Plugin Project Overview

## Introduction
This project is a sample Gradle plugin written in Java. It demonstrates how to build and test a simple Gradle plugin that adds a custom task to a project.

## Plugin Functionality
The Greeting Plugin adds a task called `greet` to any project it's applied to. When executed, this task prints a greeting message to the console: "Hello from plugin 'com.example.plugin.greeting'".

## Project Structure
- `src/main/java`: Contains the main plugin implementation
  - `com.example.plugin.GreetingPlugin`: The main plugin class that implements Gradle's Plugin interface
- `src/test`: Contains unit tests for the plugin
- `src/functionalTest`: Contains functional tests that verify the plugin works correctly when applied to a project

## Build Configuration
The project uses the Gradle build system with the following configuration:
- Plugin ID: `com.example.plugin.greeting`
- Implementation Class: `com.example.plugin.GreetingPlugin`
- The build includes configuration for both unit tests and functional tests

## Usage
To use this plugin in another project:
1. Apply the plugin to your project
2. Run the `greet` task: `./gradlew greet`

## Development
This project follows standard Gradle plugin development practices. For more information on developing Gradle plugins, refer to the [Gradle Plugin Development Guide](https://gradle.org/guides/?q=Plugin%20Development).