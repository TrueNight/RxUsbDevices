package xyz.truenight.usbdevices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED
import timber.log.Timber
import xyz.truenight.utils.optional.safe


class DeviceReceiver(
    private val listener: DeviceConnectionListener,
    private val deviceFilter: UsbDeviceFilter
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        val device = intent?.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

        if (device == null || !deviceFilter.matchesHostDevice(device)) return

        when (intent.action) {
            ACTION_USB_DEVICE_ATTACHED, ACTION_ACCEPTED_USB_DEVICE_ATTACHED -> {
                Timber.d("Device attached, extras = ${intent.extras?.let { bundle ->
                    bundle.keySet().joinToString { "$it=${bundle[it]}" }
                }.safe { "null" }}")
                listener.onDeviceConnected(device)
            }
            ACTION_USB_DEVICE_DETACHED -> {
                Timber.d("Device detached, extras = ${intent.extras?.let { bundle ->
                    bundle.keySet().joinToString { "$it=${bundle[it]}" }
                }.safe { "null" }}")
                listener.onDeviceDisconnected(device)
            }
        }
    }

    fun register(appContext: Context, root: Boolean = false) {
        appContext.registerReceiver(
            this,
            IntentFilter(if (root) ACTION_USB_DEVICE_ATTACHED else ACTION_ACCEPTED_USB_DEVICE_ATTACHED)
        )
        appContext.registerReceiver(
            this,
            IntentFilter(ACTION_USB_DEVICE_DETACHED)
        )
    }

    fun unregister(appContext: Context) {
        appContext.unregisterReceiver(this)
    }

    companion object {
        const val ACTION_ACCEPTED_USB_DEVICE_ATTACHED =
            "xyz.truenight.usbdevices.USB_DEVICE_ATTACHED"
    }
}