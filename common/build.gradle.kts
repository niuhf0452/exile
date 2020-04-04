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
    api(bom.`kotlinx-coroutines-jdk8`)
    testImplementation(bom.`log4j-slf4j-impl`)
}
