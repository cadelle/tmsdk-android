package com.techxmind.el

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Point
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import com.techxmind.el.Message.EventLog.Carrier
import com.techxmind.el.Message.EventLog.Network
import com.tencent.mmkv.MMKV
import java.io.UnsupportedEncodingException
import java.lang.reflect.InvocationTargetException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.math.pow
import kotlin.math.sqrt

internal object Helper {
    // must be setted in Agent.init
    internal var app: Application? = null

    @RequiresPermission(permission.ACCESS_NETWORK_STATE)
    fun getNetWorkType(): Network {
        val cm = app!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = cm.activeNetworkInfo ?: return Network.NETWORK_UNKNOWN

        if (!info.isAvailable) {
            return Network.NETWORK_UNKNOWN
        }

        if (info.type == ConnectivityManager.TYPE_WIFI) {
            return Network.NETWORK_WIFI
        }

        return when (info.subtype) {
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> Network.NETWORK_2G

            TelephonyManager.NETWORK_TYPE_TD_SCDMA,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP -> Network.NETWORK_3G

            TelephonyManager.NETWORK_TYPE_IWLAN,
            TelephonyManager.NETWORK_TYPE_LTE -> Network.NETWORK_4G


            TelephonyManager.NETWORK_TYPE_NR -> Network.NETWORK_5G

            else -> {
                val subtypeName = info.subtypeName
                if (subtypeName.equals("TD-SCDMA", ignoreCase = true)
                    || subtypeName.equals("WCDMA", ignoreCase = true)
                    || subtypeName.equals("CDMA2000", ignoreCase = true)
                ) {
                    Network.NETWORK_3G
                } else {
                    Network.NETWORK_UNKNOWN
                }
            }
        }
    }

    fun getAndroidId(): String {
        try {
            return Settings.System.getString(
                app!!.contentResolver,
                Settings.System.ANDROID_ID
            )
        } catch (_: Exception) {}

        return ""
    }

    fun getCarrier(): Carrier {
        val tm = app!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val opNum = tm.simOperator ?: return Carrier.CARRIER_UNKNOWN

        return when (opNum) {
            "46000", "46002", "46007", "46020" -> Carrier.CARRIER_CM
            "46001", "46006", "46009" -> Carrier.CARRIER_CU
            "46003", "46005", "46011" -> Carrier.CARRIER_CT
            else -> Carrier.CARRIER_UNKNOWN
        }
    }

    fun getAppVersion(): String {
        try {
            val pInfo = app!!.packageManager.getPackageInfo(app!!.packageName, 0)
            return pInfo.versionName
        } catch (_: java.lang.Exception) {}

        return ""
    }

    fun getScreenProperties(ctx: Context): ScreenProperties{
        val sp = ScreenProperties()
        val point = Point()
        val dm = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ctx.display?.let {
                it.getRealSize(point)
                it.getRealMetrics(dm)
            }
        } else {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.let {
                val point = Point()
                wm.defaultDisplay.getRealSize(point)
                wm.defaultDisplay.getRealMetrics(dm)
            }
        }
        sp.width = point.x
        sp.height = point.y
        sp.resolution = "${point.x}x${point.y}"
        if (dm.widthPixels > 0 && dm.density > 0) {
            sp.size = "%.1f".format(
                (sqrt(
                    dm.widthPixels.toDouble().pow(2.0) + dm.heightPixels.toDouble().pow(2.0)
                ) / (160 * dm.density))
            )
        }

        return sp
    }

    @SuppressLint("HardwareIds")
    @RequiresPermission(permission.READ_PHONE_STATE)
    fun getImei(): String {
        return getImeiOrMeid(true)
    }

    @SuppressLint("HardwareIds")
    @RequiresPermission(permission.READ_PHONE_STATE)
    fun getImeiOrMeid(isImei: Boolean): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ""
        }

        val tm = app!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return if (isImei) {
                getMinOne(tm.getImei(0), tm.getImei(1))
            } else {
                getMinOne(tm.getMeid(0), tm.getMeid(1))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val ids: String = getSystemPropertyByReflect(if (isImei) "ril.gsm.imei" else "ril.cdma.meid")
            if (!TextUtils.isEmpty(ids)) {
                val idArr = ids.split(",").toTypedArray()
                return if (idArr.size == 2) {
                    getMinOne(idArr[0], idArr[1])
                } else {
                    idArr[0]
                }
            }
            var id0 = tm.deviceId
            var id1 = ""
            try {
                val method = tm.javaClass.getMethod("getDeviceId", Int::class.javaPrimitiveType)
                id1 = method.invoke(
                    tm,
                    if (isImei) TelephonyManager.PHONE_TYPE_GSM else TelephonyManager.PHONE_TYPE_CDMA
                ) as String
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
            if (isImei) {
                if (id0.length < 15) {
                    id0 = ""
                }
                if (id1.length < 15) {
                    id1 = ""
                }
            } else {
                if (id0.length == 14) {
                    id0 = ""
                }
                if (id1.length == 14) {
                    id1 = ""
                }
            }
            return getMinOne(id0, id1)
        } else {
            val deviceId = tm.deviceId
            if (isImei) {
                if (deviceId != null && deviceId.length >= 15) {
                    return deviceId
                }
            } else {
                if (deviceId != null && deviceId.length == 14) {
                    return deviceId
                }
            }
        }
        return ""
    }

    private fun getSystemPropertyByReflect(key: String): String {
        try {
            @SuppressLint("PrivateApi") val clz = Class.forName("android.os.SystemProperties")
            val getMethod = clz.getMethod(
                "get",
                String::class.java,
                String::class.java
            )
            return getMethod.invoke(clz, key, "") as String
        } catch (e: java.lang.Exception) { /**/
        }
        return ""
    }

    private fun getMinOne(s0: String, s1: String): String {
        val empty0 = TextUtils.isEmpty(s0)
        val empty1 = TextUtils.isEmpty(s1)
        if (empty0 && empty1) return ""
        if (!empty0 && !empty1) {
            return if (s0 <= s1) {
                s0
            } else {
                s1
            }
        }
        return if (!empty0) s0 else s1
    }

    fun randomString(len: Int): String {
        return RandomString.generate(len)
    }

    fun getTkIdAndUdid(imei: String = "", oaid: String = "", androidId: String = ""): Array<String> {
        val tkidKey = "tkid"
        var udid = ""
        var tkid = ""

        val mmkv = try {
            MMKV.defaultMMKV()
        } catch (_: IllegalStateException) {
            MMKV.initialize(app!!)
            MMKV.defaultMMKV()
        }

        val tkidCipher = mmkv.decodeString(tkidKey) ?: ""

        if (tkidCipher != "") {
            tkid = Encryption.decrypt(tkidCipher) ?: ""
        }

        if (tkid != "") {
            udid = Tk.getUdidFromTk(tkid) ?: ""
        }

        if (udid != "") {
            logger.log("Get tkid=${tkid} udid=${udid} from mmkv")
            return arrayOf(tkid, udid)
        }

        // 兼容天气旧版本
        udid = mmkv.decodeString("udid") ?: ""

        if (udid != "") {
            tkid = Tk.generateTkId(udid)

            if (tkid != "") {
                mmkv.encode(tkidKey, Encryption.encrypt(tkid))
            }
            logger.log("Get old version udid=${udid} from mmkv")
            return arrayOf(tkid, udid)
        }

        val udidBs = if (imei != "" && !"""^0+$""".toRegex().matches(imei)) {
            logger.log("Generate udid by imei")
            generateUdid(imei)
        } else if (oaid != "") {
            logger.log("Generate udid by oaid")
            generateUdid(oaid)
        } else if (androidId != "") {
            logger.log("Generate udid by android")
            generateUdid(androidId)
        } else {
            logger.log("Generate udid by uuid")
            generateUdid(randomString(16))
        }

        tkid = Tk.generateTkId(udidBs)
        udid = urlSafeBase64Encode(udidBs)

        if (tkid != "") {
            mmkv.encode(tkidKey, Encryption.encrypt(tkid))
        }

        logger.log("new tkid=${tkid} udid=${udid}")

        return arrayOf(tkid, udid)
    }

    private fun generateUdid(uuid: String): ByteArray {
        return getDigest("MD5").digest(getRawBytes(uuid)).slice(4..11).toByteArray()
    }

    fun urlSafeBase64Encode(bs: ByteArray): String {
        return Base64.encodeToString(bs, Base64.NO_WRAP.or(Base64.NO_PADDING).or(Base64.URL_SAFE))
    }

    fun urlSafeBase64Decode(str: String): ByteArray {
        return Base64.decode(str, Base64.NO_WRAP.or(Base64.NO_PADDING).or(Base64.URL_SAFE))
    }

    private fun getRawBytes(text: String): ByteArray {
        return try {
            text.toByteArray(Charsets.UTF_8)
        } catch (e: UnsupportedEncodingException) {
            text.toByteArray()
        }
    }

    private fun getDigest(algorithm: String): MessageDigest {
        try {
            return MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException(e)
        }
    }
}

private object RandomString {
    private val chars: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun generate(len: Int): String {
        if (len <= 0) return ""
        return (1..len).map { kotlin.random.Random.nextInt(0, chars.size)}
            .map(chars::get)
            .joinToString("")
    }
}

class ScreenProperties(
    var width: Int = 0,
    var height: Int = 0,
    var resolution: String = "",
    var size: String = ""
)