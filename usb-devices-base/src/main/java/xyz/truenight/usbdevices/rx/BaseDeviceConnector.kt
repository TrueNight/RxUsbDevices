package xyz.truenight.usbdevices.rx

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import io.reactivex.processors.BehaviorProcessor
import xyz.truenight.usbdevices.DeviceConnectionListener
import xyz.truenight.usbdevices.DeviceReceiver
import xyz.truenight.usbdevices.UsbDeviceFilter
import xyz.truenight.utils.optional.toOptional

abstract class BaseDeviceConnector<M : Any>(
    context: Context,
    filterResId: Int,
    private val mapping: (UsbDevice?) -> M?,
    root: Boolean = false
) : DeviceConnectionListener {

    protected val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    protected val deviceFilter: UsbDeviceFilter = UsbDeviceFilter(context, filterResId)

    // todo add support of multi-connections
    protected open fun findDevice(): Pair<UsbDevice?, M?> {
        deviceFilter.devices.forEach { device ->
            if (usbManager.hasPermission(device)) {
                mapping(device)?.apply { return@findDevice device to this }
            }
        }
        return EMPTY
    }

    private val pairSubject by lazy { BehaviorProcessor.createDefault(findDevice()) }

    /**
     * Returns Flowable which emmit not null items from mapping
     */
    fun observe() = pairSubject.map { it.second.toOptional() }

    init {
        // todo maybe subscribe only if observed
        val appContext = context.applicationContext
        DeviceReceiver(this, deviceFilter).register(appContext, root)
    }

    private fun onDeviceChanged() {
        val currentDevice = pairSubject.value?.first

        val pair = findDevice()

        if (currentDevice != pair.first) {
            pairSubject.onNext(pair)
        }
    }

    companion object {
        @JvmStatic
        protected val EMPTY = null to null
    }

    final override fun onDeviceConnected(device: UsbDevice) {
        onDeviceChanged()
    }

    final override fun onDeviceDisconnected(device: UsbDevice) {
        onDeviceChanged()
    }
}