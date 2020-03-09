import com.github.niuhf0452.exile.submodule

plugins {
    id("exile.kotlin")
    id("exile.kotlin.serialization")
    id("exile.kotlin.allopen")
}

dependencies {
    implementation(submodule("inject"))
}