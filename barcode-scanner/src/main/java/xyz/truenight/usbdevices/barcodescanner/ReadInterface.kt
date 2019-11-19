package xyz.truenight.usbdevices.barcodescanner

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

class ReadInterface(val device: UsbDevice, val usbInterface: UsbInterface, val usbEndpoint: UsbEndpoint) {

    companion object {
        fun create(device: UsbDevice?): ReadInterface? {
            val usbInterface = findInterface(device)
            val usbEndpoint = findEndpoint(usbInterface)
            return if (device != null && usbInterface != null && usbEndpoint != null) ReadInterface(
                device,
                usbInterface,
                usbEndpoint
            ) else null
        }

        private fun findInterface(usbDevice: UsbDevice?): UsbInterface? {
            if (usbDevice == null) return null

            for (nIf in 0 until usbDevice.interfaceCount) {
                val usbInterface = usbDevice.getInterface(nIf)
                if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                    return usbInterface
                }
            }
            return null
        }

        private fun findEndpoint(usbInterface: UsbInterface?): UsbEndpoint? {
            if (usbInterface == null) return null

            for (i in 0 until usbInterface.endpointCount) {
                val endpoint = usbInterface.getEndpoint(i)

                if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                    return endpoint
                }
            }
            return null
        }
    }
}