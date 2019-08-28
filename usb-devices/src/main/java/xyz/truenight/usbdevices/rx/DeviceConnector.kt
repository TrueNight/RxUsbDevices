package xyz.truenight.usbdevices.rx

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import io.reactivex.processors.BehaviorProcessor
import xyz.truenight.usbdevices.DeviceReceiver
import xyz.truenight.usbdevices.UsbDeviceFilter
import xyz.truenight.utils.optional.toOptional

class DeviceConnector<M : Any>(
    context: Context,
    filterResId: Int,
    private val mapping: (UsbDevice?) -> M?
) {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val deviceFilter: UsbDeviceFilter =
        UsbDeviceFilter(context, filterResId)

    // todo add support of multi-connections
    private fun findDevice(): Pair<UsbDevice?, M?> {
        deviceFilter.devices.forEach { device ->
            mapping(device)
                ?.takeIf { usbManager.hasPermission(device) }
                ?.apply { return@findDevice device to this }
        }
        return EMPTY
    }

    private val pairSubject by lazy { BehaviorProcessor.createDefault(findDevice()) }

    /**
     * Returns Flowable which emmit not null items from mapping
     */
    fun observe() = pairSubject.map { it.second.toOptional() }

    init {
        val appContext = context.applicationContext
        DeviceReceiver(this, deviceFilter).register(appContext)
    }

    internal fun onDeviceConnected(connected: Boolean, device: UsbDevice) {
        val currentDevice = pairSubject.value?.first

        val pair = findDevice()

        if (currentDevice != pair.first) {
            pairSubject.onNext(pair)
        }
    }

    companion object {
        private val EMPTY = null to null
    }
}