plugins {
    `kotlin-dsl`
}

val kotlinVersion = "1.3.70"

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    implementation("com.github.jk1:gradle-license-report:1.13")
    implementation(gradleApi())
}