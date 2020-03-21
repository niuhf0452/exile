import com.github.niuhf0452.exile.bom

plugins {
    id("exile.kotlin")
    id("exile.kotlin.allopen")
    id("exile.kotlin.test")
    id("exile.maven.publish")
}

dependencies {
    api(bom.`kotlin-stdlib-jdk8`)
    api(bom.`kotlin-reflect`)
    implementation(bom.classgraph)
    implementation(bom.`byte-buddy`)
    testImplementation(bom.`log4j-slf4j-impl`)
}
