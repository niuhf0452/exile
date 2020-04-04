@file:Suppress("SpellCheckingInspection")

package com.github.niuhf0452.exile

import org.gradle.api.Project

object Vers {
    const val kotlin = "1.3.71"
    const val kotlinx_io = "0.1.16"
    const val kotlinx_serialization = "0.20.0"
    const val kotlinx_coroutines = "1.3.4"
    const val guava = "28.2-jre"
    const val kotlin_test = "3.3.0"
    const val classgraph = "4.8.65"
    const val bytebuddy = "1.10.8"
    const val antlr = "4.7.2"
    const val slf4j = "1.7.30"
    const val log4j = "2.13.1"
    const val wiremock = "2.26.3"
    const val netty = "4.1.48.Final"
    const val servlet = "4.0.1"
    const val jetty = "9.4.27.v20200227"
}

object Bom {
    const val `kotlin-stdlib-jdk8` = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    const val `kotlin-reflect` = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"
    const val `kotlinx-serialization` = "org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Vers.kotlinx_serialization}"
    const val `kotlinx-io` = "org.jetbrains.kotlinx:kotlinx-io-jvm:${Vers.kotlinx_io}"
    const val `kotlinx-coroutines-slf4j` = "org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Vers.kotlinx_coroutines}"
    const val `kotlinx-coroutines-jdk8` = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Vers.kotlinx_coroutines}"
    const val `kotlintest-runner-junit5` = "io.kotlintest:kotlintest-runner-junit5:${Vers.kotlin_test}"
    const val classgraph = "io.github.classgraph:classgraph:${Vers.classgraph}"
    const val `byte-buddy` = "net.bytebuddy:byte-buddy:${Vers.bytebuddy}"
    const val antlr = "org.antlr:antlr4:${Vers.antlr}"
    const val guava = "com.google.guava:guava:${Vers.guava}"
    const val `slf4j-api` = "org.slf4j:slf4j-api:${Vers.slf4j}"
    const val `log4j-slf4j-impl` = "org.apache.logging.log4j:log4j-slf4j-impl:${Vers.log4j}"
    const val `log4j-1-2-api` = "org.apache.logging.log4j:log4j-1.2-api:${Vers.log4j}"
    const val `log4j-jul` = "org.apache.logging.log4j:log4j-jul:${Vers.log4j}"
    const val `log4j-test` = "org.apache.logging.log4j:log4j-core:${Vers.log4j}:tests"
    const val `wiremock-jre8` = "com.github.tomakehurst:wiremock-jre8:${Vers.wiremock}"
    const val `netty-all` = "io.netty:netty-all:${Vers.netty}"
    const val `servlet-api` = "javax.servlet:javax.servlet-api:${Vers.servlet}"
    const val `jetty-server` = "org.eclipse.jetty:jetty-server:${Vers.jetty}"
    const val `jetty-servlet` = "org.eclipse.jetty:jetty-servlet:${Vers.jetty}"
    const val `serial-protobuf` = "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${Vers.kotlinx_serialization}"
    const val `serial-cbor` = "org.jetbrains.kotlinx:kotlinx-serialization-cbor:${Vers.kotlinx_serialization}"
    const val `serial-yaml` = "com.charleskorn.kaml:kaml:0.17.0"
    const val `serial-xml` = "net.devrieze:xmlutil-serialization-jvm:0.20.0.0"
    const val `serial-bson` = "com.github.jershell:kbson:0.2.1"
    const val `serial-avro` = "com.sksamuel.avro4k:avro4k-core:0.30.0"
}

val Project.bom: Bom
    get() = Bom

fun Project.submodule(name: String): Project {
    return project(":${rootProject.name}-$name")
}