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
    api(bom.`opentracing-api`)
    api(project(":${rootProject.name}-common"))
    implementation(bom.`kotlinx-coroutines-slf4j`)
    testImplementation(bom.`log4j-slf4j-impl`)
    testImplementation(bom.`log4j-core`)
}

configurations.create("testlib")

val testlibJar = tasks.create("testJar", Jar::class) {
    archiveClassifier.set("testlib")
    from(sourceSets.test.get().output)
}

artifacts {
    add("testlib", testlibJar)
}
