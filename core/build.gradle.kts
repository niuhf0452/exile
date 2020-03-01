dependencies {
    api(project(":${rootProject.name}-inject"))
    implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:${Vers.kotlinx_io}")
    implementation("com.google.guava:guava:${Vers.guava}")
}
