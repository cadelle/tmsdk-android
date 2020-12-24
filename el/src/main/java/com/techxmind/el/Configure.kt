package com.techxmind.el

import kotlin.properties.Delegates

object Configure {
    var logserver = "https://l.benshar.cn/mul"

    // 事件Session窗口阈值定义
    var eventSessionSecondLimit: Long = 300
    var eventSessionCountLimit: Long = 1000

    // 发送策略
    // 定时发送
    var submitIntervalSeconds: Long = 60
    // 事件数满多少条发送
    var submitEventCounts: Long = 50

    // App Session定义
    // App失去焦点后（退至后台，黑屏，锁屏)时间超过该值，定义为一次新的Session
    // 算做一次新的启动
    var appSessionHeartBeatSeconds: Long = 60

    // 用户Session的定义
    // App离开主屏超过该值，则认为是一个新的Session，即打开App算一次启动
    var sessionHearbeatSeconds: Long = 30

    // 数据上报序列化类型： protobuf/json
    //var serializationType: SerializationType = SerializationType.PROTOBUF

    // 环境
    var env: Env by Delegates.observable(Env.PRODUCT) { _, _, _ -> observer() }

    // app标识
    var appType: String by Delegates.observable("default") { _, _, _ -> observer() }

    // app来源渠道
    var appChannel: String by Delegates.observable("") { _, _, _ -> observer() }

    // member id/user id
    var mid: String by Delegates.observable("") { _, _, _ -> observer() }

    // oaid
    // TODO 自动获取
    var oaid: String by Delegates.observable("") { _, _, _ -> observer() }

    internal var observer: () -> Unit = {}

    internal fun registerObserver(observer: () -> Unit) {
        this.observer = observer
    }

    fun setDebug(flag: Boolean) {
        DEBUG = flag
    }
}
