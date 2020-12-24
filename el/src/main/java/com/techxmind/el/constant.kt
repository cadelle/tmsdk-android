package com.techxmind.el

// Default event types
const val PV = "PV"
const val PD = "PD"
const val AS = "AS"
const val AQ = "AQ"

enum class SerializationType {
    JSON, PROTOBUF
}

enum class Env(val env: String) {
    PRODUCT(""),
    TEST("test"),
    DEVELOPMENT("dev"),
}