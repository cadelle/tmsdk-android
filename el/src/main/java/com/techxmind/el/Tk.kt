package com.techxmind.el

import java.lang.Exception

object Tk {
    private const val VERSION = 1
    private const val HEADER_BYTE_SIZE = 2
    private const val CRC_BYTE_SIZE = 2

    fun generateTkId(udid: ByteArray): String {
        val crcBs = crc16(udid)  //2 bytes
        val headerBs = byteArrayOf(
            VERSION.toByte(),
            0xff.toByte() // 预留位
        ) // 2 bytes

        val bs = ByteArray(udid.size + crcBs.size + headerBs.size)
        var offset = 0
        headerBs.copyInto(bs, offset)
        offset += headerBs.size
        crcBs.copyInto(bs, offset)
        offset += crcBs.size
        udid.copyInto(bs, offset)

        return Helper.urlSafeBase64Encode(bs)
    }

    fun generateTkId(udid: String): String {
        try {
            return generateTkId(Helper.urlSafeBase64Decode(udid))
        } catch (_: Exception) {}

        return ""
    }

    fun getUdidFromTk(tk: String): String? {
        try {
            val bs = Helper.urlSafeBase64Decode(tk)
            if (bs.size <= HEADER_BYTE_SIZE + CRC_BYTE_SIZE) {
                return null
            }
            val crcBs = ByteArray(CRC_BYTE_SIZE)
            val udidBs = ByteArray(bs.size - (HEADER_BYTE_SIZE + CRC_BYTE_SIZE))
            bs.copyInto(crcBs, 0, HEADER_BYTE_SIZE, HEADER_BYTE_SIZE + CRC_BYTE_SIZE)
            bs.copyInto(udidBs, 0, HEADER_BYTE_SIZE + CRC_BYTE_SIZE)
            val crc = crc16(udidBs)

            if (!(crc[0] == crcBs[0] && crc[1] == crcBs[1])) {
                return null
            }

            return Helper.urlSafeBase64Encode(udidBs)
        } catch (_: Exception) {}

        return null
    }

    private fun crc16(bs: ByteArray): ByteArray {
        var crc = 0xFFFF
        for (i in bs.indices) {
            crc = crc.xor(bs[i].toInt().and(0xFF))
            for (j in 0 until 8) {
                crc = if (crc.and(0x0001) == 1) {
                    crc.shr(1).xor(0xA001)
                } else {
                    crc.shr(1)
                }
            }
        }

        return byteArrayOf(
            crc.shr(8).and(0xFF).toByte(),
            crc.and(0xFF).toByte()
        )
    }
}