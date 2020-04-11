import com.github.niuhf0452.exile.bom

plugins {
    id("exile.kotlin")
    id("exile.kotlin.serialization")
    id("exile.kotlin.test")
}

dependencies {
    api(project(":${rootProject.name}-inject"))
    api(project(":${rootProject.name}-config"))
    api(project(":${rootProject.name}-web"))
    api(bom.`slf4j-api`)
    implementation(project(":${rootProject.name}-ext-netty"))
    implementation(project(":${rootProject.name}-ext-yaml"))
    implementation(bom.`log4j-slf4j-impl`)
    implementation(bom.`log4j-1-2-api`)
    implementation(bom.`log4j-jul`)
    implementation(bom.`kotlinx-coroutines-slf4j`)
}
