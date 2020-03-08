rootProject.name = "exile"

fun defineModule(path: String, name: String = path.replace('/', '-')) {
    include(path)
    project(":$path").name = "${rootProject.name}-$name"
}

defineModule("inject")
defineModule("core")
defineModule("examples/inject")