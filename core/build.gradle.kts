import com.github.niuhf0452.exile.bom
import com.github.niuhf0452.exile.submodule

plugins {
    id("exile.kotlin")
    id("exile.kotlin.test")
}

dependencies {
    api(submodule("inject"))
    implementation(bom.`kotlinx-io`)
    implementation(bom.guava)
}
