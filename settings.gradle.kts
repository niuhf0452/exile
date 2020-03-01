rootProject.name = "exile"

fun defineModule(name: String) {
    include(name)
    project(":$name").name = "${rootProject.name}-$name"
}

defineModule("inject")
defineModule("core")