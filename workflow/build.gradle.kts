import com.github.niuhf0452.exile.submodule

plugins {
    id("exile.kotlin")
    id("exile.kotlin.test")
    id("exile.kotlin.serialization")
}

dependencies {
    implementation(submodule("core"))
}
