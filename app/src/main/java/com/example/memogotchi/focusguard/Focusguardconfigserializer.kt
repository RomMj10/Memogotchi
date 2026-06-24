package com.example.memogotchi.focusguard

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream

/**
 * Required by DataStore's proto variant — tells DataStore how to read/write
 * FocusGuardConfig bytes on disk. This is boilerplate every proto DataStore
 * needs; there's no built-in default the way there is for Preferences
 * DataStore.
 */
object FocusGuardConfigSerializer : Serializer<FocusGuardConfig> {

    override val defaultValue: FocusGuardConfig = FocusGuardConfig.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FocusGuardConfig {
        try {
            return FocusGuardConfig.parseFrom(input)
        } catch (exception: Exception) {
            // CorruptionException tells DataStore to fall back to
            // defaultValue rather than crash the app if the file on disk
            // is unreadable (e.g. corrupted during a write, or from an
            // incompatible older schema version).
            throw CorruptionException("Cannot read FocusGuardConfig proto.", exception)
        }
    }

    override suspend fun writeTo(t: FocusGuardConfig, output: OutputStream) {
        t.writeTo(output)
    }
}