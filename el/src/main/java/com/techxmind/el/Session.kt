package com.techxmind.el

// 日志的Session定义，一定时间内或一定数量的日志为一个Session批次
class Session {
    private val uniq: String = Helper.randomString(10)
    val sessionID: String = "s:$uniq"
    val startTime: Long = System.currentTimeMillis()
    var eventCount: Long = 0
    var pvCount: Long = 0

    override fun toString() = "Session(id:$sessionID, start:$startTime, events:$eventCount)"

    fun newEventID(): String {
        return "e:%s:%03x".format(uniq, ++eventCount)
    }

    fun newPvID(): String {
        return "p:%s:%03x".format(uniq, ++pvCount)
    }
}