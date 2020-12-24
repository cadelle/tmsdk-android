package com.techxmind.el

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.common.cache.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

internal object Analyst {
    private var isInited: Boolean = false

    private var appStartTime: Long = System.currentTimeMillis()
    private var appLastActiveTime: Long = System.currentTimeMillis()
    private var appIsActive: Boolean = true

    private var pages: ConcurrentMap<Int, Page> = ConcurrentHashMap()
    private var pvRefs: Cache<Int, Event> = CacheBuilder.newBuilder()
        .concurrencyLevel(8)
        .expireAfterWrite(600, TimeUnit.SECONDS)
        .initialCapacity(10)
        .maximumSize(1000)
        .build()

    private var actCount = 0
    private var visualInited = false

    fun init(ctx: Context) {
        if (isInited) return

        if (ctx is Activity) {
            Helper.app = ctx.application
        } else if (ctx is Application) {
            Helper.app = ctx
        }

        Helper.app?.let {
            registerActivityCallbacks(it)
        }


        asEvent()
        Agent.submit()

        isInited = true
    }

    fun setPageInfo(objHash: Int, info: PageInfo, initPvId: String? = null) {
        pages[objHash] = Page(info, initPvId)
    }

    fun getPageInfo(objHash: Int): PageInfo? {
        return pages[objHash]?.info
    }

    fun lastPvEvent(objHash: Int): Event? {
        val evt = pvRefs.getIfPresent(objHash)

        if (evt != null) {
            return evt.copy()
        }

        return null
    }

    fun newPvID(): String {
        return Agent.currentSession().newPvID()
    }

    // 页面浏览事件
    fun pvEvent(pageInfo: PageInfo, objHash: Int = 0, initPvId: String? = null) {
        val evt = Event(
            type = PV,
            pageId = pageInfo.pageId,
            pageKey = pageInfo.pageKey,
            layoutId = pageInfo.layoutId,
            pvId = initPvId ?: newPvID()
        )

        pageInfo.ref?.let {
            evt.refPageId =  it.refPageId
            evt.refModuleId = it.refModuleId
            evt.refPageKey = it.refPageKey
            evt.refLayoutId = it.refLayoutId
            evt.refPvId = it.refPvId
        }

        if (objHash != 0) {
            pvRefs.put(objHash, evt.copy())
        }

        Agent.event(evt)
    }

    // 页面退出事件
    fun pdEvent(refEvent: Event, duration: Long) {
        val evt = refEvent.copy()
        evt.type = PD
        evt.duration = duration
        evt.time = System.currentTimeMillis()
        Agent.event(evt)
    }

    // App启动事件
    fun asEvent() {
        Agent.event(Event(
            type = AS
        ))
    }

    // App退出事件
    fun aqEvent(objHash: Int? = null) {
        val duration = appLastActiveTime - appStartTime
        Agent.event(
            type = AQ,
            objHash = objHash,
            extendInfo = mutableMapOf(
                "time" to appLastActiveTime.toString(),
                "duration" to duration.toString()
            )
        )
    }

    private fun checkAppSession()
    {
        val now = System.currentTimeMillis()
        if (!appIsActive && now - appLastActiveTime > Configure.appSessionHeartBeatSeconds*1000) {
            logger.log("app new session")
            aqEvent()
            appStartTime = now
            appLastActiveTime = now
            asEvent()
            Agent.submit()
        }
    }

    private fun registerActivityCallbacks(app: Application)
    {
        app.registerActivityLifecycleCallbacks(object: Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val hashCode = activity.hashCode()
            }

            override fun onActivityStarted(activity: Activity) {
                val hashCode = activity.hashCode()
                //pvEvent(pageId, pageKey, hashCode)
                var pageInfo = getPageInfo(hashCode)

                if (pageInfo == null) {
                    // create default page info
                    pageInfo = PageInfo(activity.toString())
                    setPageInfo(hashCode, pageInfo)
                }

                actCount++

                logger.log("Activity#$hashCode#${activity.toString()} started, activity count = $actCount")
            }

            override fun onActivityResumed(activity: Activity) {
                val hashCode = activity.hashCode()

                if (!visualInited) {
                    visualInited = true
                    Agent.initAfterVisual(activity)
                }

                pages[hashCode]?.let {
                    it.active = true
                    if (!it.isPvSubmited) {
                        it.isPvSubmited = true
                        pvEvent(it.info, hashCode)
                    }
                }

                checkAppSession()
                appLastActiveTime = System.currentTimeMillis()
                appIsActive = true

                logger.log("Activity#$hashCode#${activity.toString()} resumed")
            }

            override fun onActivityPaused(activity: Activity) {
                val hashCode = activity.hashCode()

                pages[hashCode]?.let {
                    it.active = false
                }

                appLastActiveTime = System.currentTimeMillis()

                logger.log("Activity#$hashCode#${activity.toString()} paused")
            }

            override fun onActivityStopped(activity: Activity) {
                val hashCode = activity.hashCode()

                pages[hashCode]?.let {
                    it.active = false
                }

                appLastActiveTime = System.currentTimeMillis()
                appIsActive = false

                actCount--

                if (actCount == 0 && activity.isTaskRoot) {
                    logger.log("aqq quit?")
                    pages[hashCode]?.let {
                        pages.remove(hashCode)
                        val duration = it.duration()
                        lastPvEvent(hashCode)?.let {
                                refEvt ->
                            pdEvent(refEvt, duration)
                        }
                    }
                    aqEvent(hashCode)
                    Agent.submit()
                }

                logger.log("Activity#$hashCode#${activity.toString()} stopped activity count = $actCount")
            }

            override fun onActivityDestroyed(activity: Activity) {
                val hashCode = activity.hashCode()

                pages[hashCode]?.let {
                    pages.remove(hashCode)
                    val duration = it.duration()
                    lastPvEvent(hashCode)?.let {
                        refEvt ->
                        pdEvent(refEvt, duration)
                    }
                }

                /*
                if (activity.isTaskRoot) {
                    aqEvent(hashCode)
                }
                 */

                logger.log("Activity#$hashCode#${activity.toString()} destoryed")
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }
        })
    }
}
