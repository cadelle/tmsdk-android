package com.techxmind.tmsdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.techxmind.el.Agent
import com.techxmind.el.Configure

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        // init event log sdk
        with(Configure) {
            setDebug(true)
            logserver = "https://l.techxmind.com/mul"
            //appSessionHeartBeatSeconds = 30
            //submitIntervalSeconds = 10
            mid = "5555"
            appChannel = "txm"
        }

        Agent.init(this)
    }
}