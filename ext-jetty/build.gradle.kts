import com.github.niuhf0452.exile.bom

plugins {
    id("exile.kotlin")
    id("exile.kotlin.test")
    id("exile.kotlin.serialization")
    id("exile.maven.publish")
}

dependencies {
    compileOnly(project(":${rootProject.name}-inject"))
    compileOnly(project(":${rootProject.name}-web"))
    implementation(bom.`kotlinx-coroutines-slf4j`)
    implementation(bom.`servlet-api`)
    implementation(bom.`jetty-server`)
    implementation(bom.`jetty-servlet`)
    testImplementation(bom.`log4j-slf4j-impl`)
    testImplementation(bom.`log4j-core`)
    testImplementation(project(":${rootProject.name}-inject"))
    testImplementation(project(":${rootProject.name}-web"))
    testImplementation(project(":${rootProject.name}-web", "testlib"))
}
