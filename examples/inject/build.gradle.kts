plugins {
    id("exile.kotlin")
    id("exile.kotlin.serialization")
    id("exile.kotlin.allopen")
}

dependencies {
    implementation(project(":${rootProject.name}-inject"))
}