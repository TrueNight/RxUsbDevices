package xyz.truenight.usbdevices.barcodescanner

import android.content.Context
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import xyz.truenight.usbdevices.rx.BaseDeviceConnector


/**
 * Copyright (C) 2019 Mikhail Frolov
 */
object BarcodeScanner {

    private lateinit var connector: BaseDeviceConnector<ReadInterface>

    private lateinit var service: BarcodeScannerService

    private var deviceAttachedDisposable: Disposable? = null

    fun init(context: Context, deviceConnector: BaseDeviceConnector<ReadInterface>) {
        connector = deviceConnector

        service = BarcodeScannerService(context)

        deviceAttachedDisposable?.dispose()
        deviceAttachedDisposable = deviceConnector.observe()
            .observeOn(Schedulers.io())
            .subscribe {
                Timber.d("Device ${if (it.present) "connected" else "not connected"}")
                if (it.present) {
                    service.connect(it.get())
                } else {
                    service.close()
                }
            }
    }

    fun observe() = service.observe()

}
