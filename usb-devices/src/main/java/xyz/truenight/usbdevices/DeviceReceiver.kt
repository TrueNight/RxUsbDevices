package xyz.truenight.usbdevices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED
import timber.log.Timber
import xyz.truenight.usbdevices.rx.DeviceConnector


class DeviceReceiver(private val listener: DeviceConnector<*>, private val deviceFilter: UsbDeviceFilter) :
    BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        val device = intent?.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

        if (device == null || !deviceFilter.matchesHostDevice(device)) return

        if (intent.action == ACTION_USB_DEVICE_ATTACHED) {
            Timber.d("Device attached, extras = ${intent.extras?.let { bundle ->
                bundle.keySet().joinToString { "$it=${bundle[it]}" }
            }
                ?: "null"}")
            listener.onDeviceConnected(true, device)
        }

        if (intent.action == ACTION_USB_DEVICE_DETACHED) {
            Timber.d("Device detached, extras = ${intent.extras?.let { bundle ->
                bundle.keySet().joinToString { "$it=${bundle[it]}" }
            }
                ?: "null"}")
            listener.onDeviceConnected(false, device)
        }
    }

    fun register(appContext: Context) {
        appContext.registerReceiver(
            this,
            IntentFilter(ACTION_USB_DEVICE_ATTACHED)
        )
        appContext.registerReceiver(
            this,
            IntentFilter(ACTION_USB_DEVICE_DETACHED)
        )
    }

    companion object {
        const val ACTION_USB_DEVICE_ATTACHED = "xyz.truenight.usbdevices.USB_DEVICE_ATTACHED"
    }
}