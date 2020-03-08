@file:Suppress("SpellCheckingInspection")

package com.github.niuhf0452.exile

import org.gradle.api.Project

object Vers {
    const val kotlin = "1.3.70"
    const val kotlinx_io = "0.1.16"
    const val kotlinx_serialization = "0.20.0"
    const val guava = "28.2-jre"
    const val kotlin_test = "3.3.0"
    const val classgraph = "4.8.65"
    const val bytebuddy = "1.10.8"
}

object Bom {
    const val `kotlin-stdlib-jdk8` = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    const val `kotlin-reflect` = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"
    const val `kotlinx-serialization` = "org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Vers.kotlinx_serialization}"
    const val `kotlinx-io` = "org.jetbrains.kotlinx:kotlinx-io-jvm:${Vers.kotlinx_io}"
    const val `kotlintest-runner-junit5` = "io.kotlintest:kotlintest-runner-junit5:${Vers.kotlin_test}"
    const val classgraph = "io.github.classgraph:classgraph:${Vers.classgraph}"
    const val `byte-buddy` = "net.bytebuddy:byte-buddy:${Vers.bytebuddy}"
    const val guava = "com.google.guava:guava:${Vers.guava}"
}

val Project.bom: Bom
    get() = Bom

fun Project.submodule(name: String): Project {
    return project(":${rootProject.name}-$name")
}