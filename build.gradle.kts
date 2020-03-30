import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm")
    id("org.sonarqube") version "2.8"
}

repositories {
    mavenCentral()
}

group = "com.github.niuhf0452"
version = "0.1-SNAPSHOT"

idea {
    project {
        jdkName = "11"
        languageLevel = IdeaLanguageLevel("11")
    }
    module {
        isDownloadSources = true
    }
}

sonarqube {
    properties {
        property("sonar.projectName", "exile")
        property("sonar.projectKey", "com.github.niuhf0452.exile")
        property("sonar.organization", "niuhf0452")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

subprojects {
    if (name.contains("-examples-")) {
        sonarqube {
            isSkipProject = true
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xjvm-default=enable")
    }
}