package xyz.truenight.usbdevices.barcodescanner

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbRequest
import android.os.Looper
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Copyright (C) 2019 Mikhail Frolov
 */
class BarcodeScannerService(context: Context) {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var disposable: Disposable? = null

    private val subject = PublishProcessor.create<String>()

    fun observe(): Flowable<String> = subject

    internal fun connect(readInterface: ReadInterface) {
        close()
        Timber.d("connect")
        val connection = usbManager.openDevice(readInterface.device)
        Timber.d("connected")
        val claimed = connection?.claimInterface(readInterface.usbInterface, true)
        Timber.d("claimed = $claimed")

        disposable = Flowable.create<String>({

            it.setCancellable {
                Timber.d("release interface")
                connection?.releaseInterface(readInterface.usbInterface)
                Timber.d("close connection")
                connection?.close()
            }

            Timber.d("read")
            Timber.d("main thread = ${Looper.getMainLooper() == Looper.myLooper()}")
            val bufferSize = readInterface.usbEndpoint.maxPacketSize
//            val buffer = ByteArray(bufferSize)
            val byteBuffer = ByteBuffer.allocate(bufferSize)

            val usbRequest = UsbRequest()
            usbRequest.initialize(connection, readInterface.usbEndpoint)
            Timber.d("initialized")

            while (!it.isCancelled) {
                Timber.d("waiting for scan")
                @Suppress("DEPRECATION")
                usbRequest.queue(byteBuffer, bufferSize)
                Timber.d("queued")

                val request = connection.requestWait()

                if (request != null && request.endpoint.direction == UsbConstants.USB_DIR_IN) {
                    val buffer = byteBuffer.array()
                    Timber.d("result = ${buffer.joinToString()}")

                    val result = newlandParse(buffer)
                    byteBuffer.clear()
                    it.onNext(result)
                } else {
                    Timber.d("error")
                }
            }
        }, BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io()).subscribe(subject::onNext) { Timber.d(it) }
        Timber.d("subscribed")
    }

    private fun newlandParse(data: ByteArray): String =
        String(data.copyOfRange(2, 2 + data[1]))
            .replace(Regex("[\n\r]+"), "")

    internal fun close() {
        Timber.d("dispose")
        disposable?.dispose()
        disposable = null
    }
}