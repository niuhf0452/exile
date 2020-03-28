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
    api(bom.`kotlinx-coroutines-jdk8`)
    api(bom.`slf4j-api`)
    api(bom.`netty-all`)
    api(submodule("serialization"))
    compileOnly(submodule("inject"))
    compileOnly(submodule("config"))
    compileOnly(bom.`servlet-api`)
    compileOnly(bom.`jetty-server`)
    compileOnly(bom.`jetty-servlet`)
    testImplementation(bom.`log4j-slf4j-impl`)
    testImplementation(bom.`servlet-api`)
    testImplementation(bom.`jetty-server`)
    testImplementation(bom.`jetty-servlet`)
}
