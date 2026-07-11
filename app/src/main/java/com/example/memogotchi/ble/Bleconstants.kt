package com.example.memogotchi.ble

import android.os.ParcelUuid
import java.util.UUID

object BleConstants {
    // Custom 128-bit Service UUID unique to this app. Generated once —
    // do NOT change this after devices are in the wild, or old and new
    // app versions will silently stop finding each other.
    val SERVICE_UUID: UUID = UUID.fromString("7d2b1a4c-9e3f-4a1b-8c2d-5f6e7a8b9c0d")
    val SERVICE_PARCEL_UUID: ParcelUuid = ParcelUuid(SERVICE_UUID)

    // Must match TOKEN_BYTE_LENGTH in the backend's presence.ts. BLE legacy
    // advertising has a 31-byte total packet budget; carrying the token in
    // a Service Data (128-bit UUID) AD structure costs 18+N bytes, plus
    // ~3 bytes for the mandatory Flags AD structure = 21+N, leaving N <= 10
    // bytes of headroom. 8 bytes keeps a safety margin.
    const val TOKEN_BYTE_LENGTH = 8
}

fun hexStringToBytes(hex: String): ByteArray {
    val clean = hex.trim()
    require(clean.length % 2 == 0) { "Hex string must have even length: $clean" }
    return ByteArray(clean.length / 2) { i ->
        val index = i * 2
        clean.substring(index, index + 2).toInt(16).toByte()
    }
}

fun bytesToHexString(bytes: ByteArray): String =
    bytes.joinToString("") { "%02x".format(it) }