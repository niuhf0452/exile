plugins {
    id("org.jetbrains.kotlin.plugin.allopen") version Vers.kotlin
    `exile-publish`
}

allOpen {
    annotation("com.github.niuhf0452.exile.inject.Inject")
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
    implementation("io.github.classgraph:classgraph:${Vers.classgraph}")
    implementation("net.bytebuddy:byte-buddy:${Vers.bytebuddy}")
}
