@file:JvmName("DeviceUtil")

package xyz.truenight.usbdevices.rx

import timber.log.Timber
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Created by true
 * date: 21/09/2017
 * time: 00:12
 *
 *
 * Copyright © Mikhail Frolov
 */

private val EXECUTOR = Executors.newSingleThreadExecutor()

val isX64 get() = System.getProperty("os.arch") == "aarch64"

fun runCommand(command: String, complete: ((String?, Throwable?) -> Unit)? = null) =
    EXECUTOR.execute {
        try {
            val result = runCommandInternal(command)
            complete?.invoke(result, null)
        } catch (e: Exception) {
            complete?.invoke(null, e)
            Timber.e(e)
            Timber.d("<-- %s", command)
        }
    }

@Throws(IOException::class, InterruptedException::class)
fun runCommandInternal(command: String): String {
    Timber.d("--> %s", command)
    when {
        isX64 -> Runtime.getRuntime().exec("su -c $command")
        else -> Runtime.getRuntime().exec(arrayOf("su", "-c", "system/bin/sh")).apply {
            DataOutputStream(outputStream).apply {
                writeBytes("$command\n")
                flush()
            }
        }
    }.apply {
        val result = String(inputStream.readBytes())
        Timber.d(result)
        val error = String(errorStream.readBytes())
        Timber.d(error)
        if (error.isNotEmpty()) {
            throw TerminalException(error.trimEnd('\n'))
        }
        waitFor()
        Timber.d("<-- %s", command)
        return result.trimEnd('\n')
    }
}

class TerminalException(message: String) : RuntimeException(message)
