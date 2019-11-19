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

    private lateinit var service: BarcodeScannerService

    private var deviceAttachedDisposable: Disposable? = null

    fun init(
        context: Context,
        deviceConnector: DeviceConnector<ReadInterface> =
            DeviceConnector(context, R.xml.scanner_filter) { ReadInterface.create(it) }
    ) {
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
