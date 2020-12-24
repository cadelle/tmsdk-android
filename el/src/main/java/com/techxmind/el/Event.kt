package com.techxmind.el

import android.os.Parcelable
import android.text.Layout
import kotlinx.parcelize.Parcelize
import kotlin.properties.Delegates

data class Event(
    var type: String = "",
    var time: Long = 0,
    var pageId: String = "",
    var layoutId: String = "",
    var pageKey: String = "",
    var moduleId: String = "",
    var pvId: String = "",
    var actionType: String = "",
    var refPageId: String = "",
    var refLayoutId: String = "",
    var refPvId: String = "",
    var refPageKey: String = "",
    var refModuleId: String = "",
    var duration: Long = 0,
    var extendInfo: Map<String, String>? = null
)

@Parcelize
data class Referer(
    val refPageId: String,
    val refPageKey: String,
    val refLayoutId: String = "",
    val refPvId: String = "",
    val refModuleId: String = ""
): Parcelable {
    companion object {
        fun create(refPv: Event?, refModuleId: String = ""): Referer? {
            if (refPv == null) {
                return null
            }
            return Referer(
                refPageId = refPv.pageId,
                refPageKey =  refPv.pageKey,
                refLayoutId = refPv.layoutId,
                refPvId = refPv.pvId,
                refModuleId = refModuleId
            )
        }
    }
}

@Parcelize
class PageInfo(val pageId:String, val pageKey: String = "", val layoutId: String = "", val ref: Referer? = null): Parcelable {

}

internal class Page(val info: PageInfo, var initPvId: String? = null) {
    var activeStartTime: Long = System.currentTimeMillis()
    var activeTimeMills: Long = 0
    var isPvSubmited: Boolean = false

    var active: Boolean by Delegates.observable(true) {
            _, old, new ->
        if (!old && new) {
            activeStartTime = System.currentTimeMillis()
        } else if (old && !new) {
            activeTimeMills += System.currentTimeMillis() - activeStartTime
        }
    }

    fun duration(): Long {
        var d = activeTimeMills
        if (active) {
            d += System.currentTimeMillis() - activeTimeMills
        }
        return d
    }
}