import com.github.niuhf0452.exile.bom

plugins {
    id("exile.kotlin")
    id("exile.kotlin.test")
    id("exile.kotlin.serialization")
    id("exile.maven.publish")
}

dependencies {
    api(bom.`kotlin-stdlib-jdk8`)
    api(bom.`kotlin-reflect`)
    api(bom.`slf4j-api`)
    api(project(":${rootProject.name}-common"))
    compileOnly(project(":${rootProject.name}-inject"))
    testImplementation(bom.`log4j-slf4j-impl`)
    testImplementation(bom.`wiremock-jre8`)
}
