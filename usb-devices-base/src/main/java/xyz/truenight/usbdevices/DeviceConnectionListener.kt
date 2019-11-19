package xyz.truenight.usbdevices

import android.hardware.usb.UsbDevice

interface DeviceConnectionListener {

    fun onDeviceConnected(device: UsbDevice)

    fun onDeviceDisconnected(device: UsbDevice)
}