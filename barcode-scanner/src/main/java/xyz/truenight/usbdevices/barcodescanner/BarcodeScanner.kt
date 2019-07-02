package xyz.truenight.usbdevices.barcodescanner

import android.content.Context
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.butik.barcodescanner.R
import timber.log.Timber
import xyz.truenight.usbdevices.rx.DeviceConnector


/**
 * Copyright (C) 2019 Mikhail Frolov
 */
object BarcodeScanner {

    private lateinit var connector: DeviceConnector<ReadInterface>

    lateinit var service: BarcodeScannerService

    private var deviceAttachedDisposable: Disposable? = null

    fun init(context: Context) {
        connector = DeviceConnector(
            context,
            R.xml.scanner_filter
        ) { ReadInterface.create(it) }
        service =
            BarcodeScannerService(context)

        deviceAttachedDisposable?.dispose()
        deviceAttachedDisposable = connector.observe()
            .observeOn(Schedulers.io())
            .subscribe {
                Timber.d("Device ${if (it.present) "connected" else "not connected"}")
                if (it.present) {
                    service.connect(it.get)
                } else {
                    service.close()
                }
            }
    }
}