rootProject.name = "exile"

fun defineModule(path: String, name: String = path.replace('/', '-')) {
    include(path)
    project(":$path").name = "${rootProject.name}-$name"
}

defineModule("inject")
defineModule("config")
defineModule("web")
defineModule("common")
defineModule("core")
defineModule("examples/inject")