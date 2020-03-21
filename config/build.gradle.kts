import com.github.niuhf0452.exile.bom
import com.github.niuhf0452.exile.submodule

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
    compileOnly(submodule("inject"))
    testImplementation(bom.`log4j-slf4j-impl`)
    testImplementation(bom.`wiremock-jre8`)
}
