rootProject.name = "exile"

fun defineModule(path: String, name: String = path.replace('/', '-')) {
    include(path)
    project(":$path").name = "${rootProject.name}-$name"
}

defineModule("inject")
defineModule("config")
defineModule("web")
defineModule("serialization")
defineModule("core")
defineModule("examples/inject")