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
    api(bom.`serial-protobuf`)
    api(bom.`serial-cbor`)
    api(bom.`serial-avro`)
    api(bom.`serial-xml`)
    api(bom.`serial-yaml`)
    api(bom.`serial-bson`)
    testImplementation(bom.`log4j-slf4j-impl`)
}
