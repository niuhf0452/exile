package com.github.niuhf0452.exile.core

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.seconds
import io.kotlintest.specs.FunSpec

class ApplicationTest : FunSpec({
    test("an Application should start").config(timeout = 10.seconds) {
        val app = Application()
        var start = false
        var stop = false
        app.addListener(object : Application.Listener<ApplicationEvent> {
            override fun onEvent(event: ApplicationEvent) {
                if (event == AfterComponentStart(app)) {
                    start = true
                    Thread { app.stop() }.start()
                } else if (event == BeforeComponentStop(app)) {
                    stop = true
                }
            }
        })
        app.run()
        start.shouldBeTrue()
        stop.shouldBeTrue()
    }
})