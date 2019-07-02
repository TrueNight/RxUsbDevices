package xyz.truenight.usbdevices

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Parcelable


/**
 * Copyright (C) 2019 Mikhail Frolov
 */
class DeviceConnectionActivity : Activity() {

    override fun onResume() {
        super.onResume()

        intent?.also {
            if (it.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val usbDevice = it.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE)

                // Create a new intent and put the usb device in as an extra
                val broadcastIntent = Intent(DeviceReceiver.ACTION_USB_DEVICE_ATTACHED)
                broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice)

                // Broadcast this event so we can receive it
                sendBroadcast(broadcastIntent)
            }
        }

        // Close the activity
        finish()
    }
}