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
    api(bom.`kotlinx-coroutines-jdk8`)
    api(bom.`slf4j-api`)
    api(bom.`netty-all`)
    api(bom.`serial-protobuf`)
    api(bom.`serial-cbor`)
    api(bom.`serial-avro`)
    api(bom.`serial-xml`)
    api(bom.`serial-yaml`)
    api(bom.`serial-bson`)
    api(bom.`opentracing-api`)
    api(submodule("common"))
    implementation(bom.`kotlinx-coroutines-slf4j`)
    compileOnly(bom.`servlet-api`)
    compileOnly(bom.`jetty-server`)
    compileOnly(bom.`jetty-servlet`)
    testImplementation(bom.`log4j-slf4j-impl`)
    testImplementation(bom.`servlet-api`)
    testImplementation(bom.`jetty-server`)
    testImplementation(bom.`jetty-servlet`)
    testImplementation(bom.`log4j-test`)
}
