import com.github.niuhf0452.exile.bom
import com.github.niuhf0452.exile.submodule

plugins {
    id("exile.kotlin")
    id("exile.kotlin.test")
}

dependencies {
    api(submodule("inject"))
    api(submodule("config"))
    implementation(bom.`kotlinx-io`)
    implementation(bom.`kotlinx-coroutines-core`)
    implementation(bom.guava)
    // logging bridge
    api(bom.`slf4j-api`)
    implementation(bom.`log4j-slf4j-impl`)
    implementation(bom.`log4j-1-2-api`)
    implementation(bom.`log4j-jul`)
    implementation(bom.`kotlinx-coroutines-slf4j`)
}
