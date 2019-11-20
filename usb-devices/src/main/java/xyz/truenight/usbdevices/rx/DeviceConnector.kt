package xyz.truenight.usbdevices.rx

import android.content.Context
import android.hardware.usb.UsbDevice

class DeviceConnector<M : Any>(
    context: Context,
    filterResId: Int,
    mapping: (UsbDevice?) -> M?
) : BaseDeviceConnector<M>(context, filterResId, mapping, false)