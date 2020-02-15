plugins {
    antlr
}

tasks.compileKotlin.configure {
    dependsOn(tasks.generateGrammarSource)
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:${Vers.kotlinx_io}")
    implementation("com.google.guava:guava:${Vers.guava}")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit"))
}
