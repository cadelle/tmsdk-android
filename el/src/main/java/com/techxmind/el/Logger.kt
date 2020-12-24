package com.techxmind.el

interface Logger {
    fun log(message: String)
}

class StdLogger: Logger {
    override fun log(message: String) {
        if (DEBUG) {
            println(message)
        }
    }
}

var DEBUG = true

var logger: Logger = StdLogger()
