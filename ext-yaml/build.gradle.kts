import com.github.niuhf0452.exile.bom

plugins {
    id("exile.kotlin")
    id("exile.kotlin.test")
    id("exile.kotlin.serialization")
    id("exile.maven.publish")
}

dependencies {
    api(bom.`kotlin-stdlib-jdk8`)
    api(bom.`kotlinx-coroutines-jdk8`)
    api(bom.`slf4j-api`)
    api(bom.`serial-yaml`)
    compileOnly(project(":${rootProject.name}-config"))
    compileOnly(project(":${rootProject.name}-web"))
    testImplementation(bom.`log4j-slf4j-impl`)
    testImplementation(bom.`log4j-core`)
    testCompileOnly(project(":${rootProject.name}-config"))
    testCompileOnly(project(":${rootProject.name}-web"))
}
