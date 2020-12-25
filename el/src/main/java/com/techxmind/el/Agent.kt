package com.techxmind.el

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.protobuf.util.JsonFormat
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object Agent {
    private const val REFERER = "el_referer"

    private var session: Session = Session()

    private var buffer = mutableListOf<Message.EventLog>()
    private var submitBuffer = mutableListOf<List<Message.EventLog>>()

    private var eventCommonBuilder = Message.EventLogCommon.newBuilder()
    private var eventCommon = eventCommonBuilder.build()

    private var bufferLock = java.lang.Object()
    private var submitBufferLock = java.lang.Object()

    private var inited = false

    init {
        val syncDataFromConfigure = {
            if (eventCommonBuilder.appType != Configure.appType) {
                eventCommonBuilder.appType = Configure.appType
            }
            if (eventCommonBuilder.appChannel != Configure.appChannel) {
                eventCommonBuilder.appChannel = Configure.appChannel
            }
            if (eventCommonBuilder.env != Configure.env.env) {
                eventCommonBuilder.env = Configure.env.env
            }
            if (eventCommonBuilder.mid != Configure.mid) {
                eventCommonBuilder.mid = Configure.mid
            }
            if (eventCommonBuilder.oaid != Configure.oaid) {
                eventCommonBuilder.oaid = Configure.oaid
            }
            eventCommon = eventCommonBuilder.build()
        }
        Configure.registerObserver(syncDataFromConfigure)
        syncDataFromConfigure()
    }

    @Synchronized
    internal fun currentSession(): Session {
        if (System.currentTimeMillis() - session.startTime > Configure.eventSessionSecondLimit * 1000 ||
            session.eventCount > Configure.eventSessionCountLimit
        ) {
            session = Session()
        }

        return session
    }

    fun init(ctx: Context) {
        if (inited) return

        inited = true

        Analyst.init(ctx)

        Helper.app?.let {
            // carrier
            eventCommonBuilder.carrier = Helper.getCarrier()

            // network
            eventCommonBuilder.network = Helper.getNetWorkType()


            // os/platform info
            eventCommonBuilder.platform = "android"
            eventCommonBuilder.os = "android"
            eventCommonBuilder.osVersion = android.os.Build.VERSION.RELEASE ?: ""
            eventCommonBuilder.deviceModel = android.os.Build.MODEL ?: ""
            eventCommonBuilder.deviceBrand = android.os.Build.BRAND ?: ""
            eventCommonBuilder.deviceVendor = android.os.Build.MANUFACTURER ?: ""

            // app version
            eventCommonBuilder.appVersion = Helper.getAppVersion()

            if (ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                eventCommonBuilder.imei = Helper.getImei()
            }

            // android id
            eventCommonBuilder.androidId = Helper.getAndroidId()

            val ids = Helper.getTkIdAndUdid(eventCommonBuilder.imei, eventCommonBuilder.oaid, eventCommonBuilder.androidId)
            eventCommonBuilder.tkid = ids[0]
            eventCommonBuilder.udid = ids[1]

            eventCommon = eventCommonBuilder.build()
        }

        startCommitWorker()
    }

    internal fun initAfterVisual(ctx: Context) {
        // screen properties
        val sp = Helper.getScreenProperties(ctx)
        eventCommonBuilder.screenWidth = sp.width
        eventCommonBuilder.screenHeight = sp.height
        eventCommonBuilder.screenResolution = sp.resolution
        eventCommonBuilder.screenSize = sp.size
        eventCommon = eventCommonBuilder.build()
    }

    fun setPageInfo(
        activity: Activity,
        pageId: String,
        pageKey: String = "",
        layoutId: String = ""
    ): Agent {
        return setPageInfo(
            activity.hashCode(), PageInfo(
                pageId,
                pageKey,
                layoutId,
                activity.intent.getParcelableExtra<Referer>(REFERER)
            )
        )
    }

    fun getUdid(): String {
        return eventCommon.udid
    }

    fun getTkid(): String {
        return eventCommon.tkid
    }

    fun setReferer(intent: Intent, activity: Activity, refModuleId: String = ""): Agent {
        intent.putExtra(REFERER, Referer.create(lastPvEvent(activity.hashCode()), refModuleId))
        return this
    }

    private fun setPageInfo(objHash: Int, info: PageInfo): Agent {
        Analyst.setPageInfo(objHash, info)
        return this
    }

    fun lastPvEvent(objHash: Int): Event? {
        return Analyst.lastPvEvent(objHash)
    }

    fun getPageInfo(objHash: Int): PageInfo? {
        return Analyst.getPageInfo(objHash)
    }

    fun event(
        type: String,
        activity: Activity? = null,
        extendInfo: MutableMap<String, String>? = null
    ): Agent {
        return event(type, activity.hashCode(), extendInfo)
    }

    fun event(type: String, objHash: Int? = null, extendInfo: MutableMap<String, String>? = null): Agent {
        val evt = Event(type = type)

        if (extendInfo != null) {
            if (extendInfo.containsKey("moduleId")) {
                evt.moduleId = extendInfo.remove("moduleId") ?: ""
            }
            if (extendInfo.containsKey("duration")) {
                val dur = extendInfo.remove("duration") ?: "0"
                evt.duration = dur.toLongOrNull() ?: 0
            }
            if (extendInfo.containsKey("time")) {
                val t = extendInfo.remove("time") ?: "0"
                evt.time = t.toLongOrNull() ?: 0
            }
            if (extendInfo.isNotEmpty()) {
                evt.extendInfo = extendInfo
            }
        }

        if (objHash != null) {
            Analyst.getPageInfo(objHash)?.let {
                evt.pageId = it.pageId
                evt.pageKey = it.pageKey
                evt.layoutId = it.layoutId
                it.ref?.let { ref ->
                    evt.refPvId = ref.refPvId
                    evt.refPageId = ref.refPageId
                    evt.refPageKey = ref.refPageKey
                    evt.refLayoutId = ref.refLayoutId
                    evt.refModuleId = ref.refModuleId
                }
            }

            Analyst.lastPvEvent(objHash)?.let {
                evt.pvId = it.pvId
            }
        }

        return event(evt)
    }

    fun event(evt: Event): Agent {

        if (evt.type == "") {
            logger.log("ignore event cause event type is missing!")
            return this
        }

        val s = currentSession()
        val b = Message.EventLog.newBuilder()
            .setSessionId(s.sessionID)
            .setEventId(s.newEventID())

        if (evt.time > 0) {
            b.eventTime = evt.time
        } else {
            b.eventTime = System.currentTimeMillis()
        }
        b.event = evt.type
        b.pageId = evt.pageId
        b.layoutId = evt.layoutId
        b.pvId = evt.pvId
        b.pageKey = evt.pageKey
        b.moduleId = evt.moduleId
        b.actionType = evt.actionType
        b.refPageId = evt.refPageId
        b.refPvId = evt.refPvId
        b.refPageKey = evt.refPageKey
        b.refLayoutId = evt.refLayoutId
        b.duration = evt.duration
        b.refModuleId = evt.refModuleId
        evt.extendInfo?.let {
            b.putAllExtendInfo(it)
        }

        val msg = b.build()
        if (DEBUG) {
            //logger.log("new event:" + JsonFormat.printer().print(msg))
        }

        var needSubmit = false
        synchronized(bufferLock) {
            buffer.add(msg)
            needSubmit = buffer.size >= Configure.submitEventCounts
        }

        if (needSubmit) {
            logger.log("submit cause event count reached the limitation")
            submit()
        }

        return this
    }

    fun submit() = synchronized(bufferLock) {
        if (buffer.size == 0) return

        logger.log("submit events: count=${buffer.size}")

        synchronized(submitBufferLock) {
            submitBuffer.add(buffer)
            buffer = mutableListOf<Message.EventLog>()
            submitBufferLock.notify()
        }
    }

    private fun startCommitWorker() {
        thread(start = true) {
            val maxBatchSize = 50
            while(true) {
                var batchList = mutableListOf<Message.EventLog>()
                synchronized(submitBufferLock) {
                    if (submitBuffer.size == 0) {
                        submitBufferLock.wait()
                    }

                    while (true) {
                        val list = submitBuffer.removeFirstOrNull() ?: break
                        for (item in list) {
                            batchList.add(item)
                        }
                        if (batchList.size >= maxBatchSize) break
                    }
                }
                send(batchList)
            }
        }

        // 定时提交
        thread(start = true) {
            while(true) {
                Thread.sleep((Configure.submitIntervalSeconds * 1000).coerceAtLeast(3000))
                logger.log("submit cause time schedule")
                submit()
            }
        }
    }

    private fun send(list: List<Message.EventLog>) {
        if (list.isEmpty()) return

        val eventsBuilder = Message.EventLogs.newBuilder()
        eventsBuilder.common = eventCommon

        for (event in list) {
            eventsBuilder.addEvents(event)
        }

        send(eventsBuilder.build())
    }

    private fun send(events: Message.EventLogs) {
        val url = URL(Configure.logserver)

        if (DEBUG) {
            logger.log("send mul events:" + JsonFormat.printer().printingEnumsAsInts().print(events))
        }

        try {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/protobuf")
                // 用于服务端时间校准
                setRequestProperty("X-Client-Time", System.currentTimeMillis().toString())
                outputStream.write(events.toByteArray())
                outputStream.flush()

                val inputStream = DataInputStream(getInputStream())
                val reader = BufferedReader(InputStreamReader(inputStream))
                val output = reader.readText()

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    logger.log("sent successfully with response $output")
                    return
                }

                logger.log("sent error with code = $responseCode and response $output")
            }
        } catch (e: Exception) {
            logger.log("send exception:$e")
        }

    }
}
