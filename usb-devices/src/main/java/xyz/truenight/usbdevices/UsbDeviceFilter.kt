package xyz.truenight.usbdevices

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*

class UsbDeviceFilter @Throws(XmlPullParserException::class, IOException::class)
constructor(ctx: Context, resourceId: Int) {

    private val hostDeviceFilters = ArrayList<DeviceFilter>()
    private val usbManager: UsbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager

    /**
     * Returns a list of connected USB Host devices matching the devices filter.
     */
    val devices: List<UsbDevice>
        get() {
            val matchedDevices = ArrayList<UsbDevice>()
            for (device in usbManager.deviceList.values) {
                if (matchesHostDevice(device)) {
                    matchedDevices.add(device)
                }
            }
            return matchedDevices
        }

    init {

        ctx.resources.getXml(resourceId).use { parser ->
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                if ("usb-device" == tagName && parser.eventType == XmlPullParser.START_TAG) {
                    hostDeviceFilters.add(DeviceFilter.read(parser))
                }
                eventType = parser.next()
            }
        }
    }

    fun matchesHostDevice(device: UsbDevice): Boolean {
        for (filter in hostDeviceFilters) {
            if (filter.matches(device)) {
                return true
            }
        }
        return false
    }

    class DeviceFilter private constructor(
        // USB Vendor ID (or -1 for unspecified)
        val vendorId: Int,
        // USB Product ID (or -1 for unspecified)
        val productId: Int,
        // USB device or interface class (or -1 for unspecified)
        val cls: Int,
        // USB device subclass (or -1 for unspecified)
        val subclass: Int,
        // USB device protocol (or -1 for unspecified)
        val protocol: Int
    ) {

        private fun matches(clasz: Int, subclass: Int, protocol: Int): Boolean {
            return ((cls == -1 || clasz == cls)
                    && (this.subclass == -1 || subclass == this.subclass)
                    && (this.protocol == -1 || protocol == this.protocol))
        }

        fun matches(device: UsbDevice): Boolean {
            if (vendorId != -1 && device.vendorId != vendorId)
                return false
            if (productId != -1 && device.productId != productId)
                return false

            // check device class/subclass/protocol
            if (matches(
                    device.deviceClass, device.deviceSubclass,
                    device.deviceProtocol
                )
            )
                return true

            // if device doesn't match, check the interfaces
            val count = device.interfaceCount
            for (i in 0 until count) {
                val intf = device.getInterface(i)
                if (matches(
                        intf.interfaceClass,
                        intf.interfaceSubclass,
                        intf.interfaceProtocol
                    )
                )
                    return true
            }

            return false
        }

        companion object {
            fun read(parser: XmlPullParser): DeviceFilter {
                var vendorId = -1
                var productId = -1
                var deviceClass = -1
                var deviceSubclass = -1
                var deviceProtocol = -1

                val count = parser.attributeCount
                for (i in 0 until count) {
                    val name = parser.getAttributeName(i)
                    // All attribute values are ints
                    val value = Integer.parseInt(parser.getAttributeValue(i))

                    if ("vendor-id" == name) {
                        vendorId = value
                    } else if ("product-id" == name) {
                        productId = value
                    } else if ("class" == name) {
                        deviceClass = value
                    } else if ("subclass" == name) {
                        deviceSubclass = value
                    } else if ("protocol" == name) {
                        deviceProtocol = value
                    }
                }

                return DeviceFilter(
                    vendorId, productId, deviceClass,
                    deviceSubclass, deviceProtocol
                )
            }
        }
    }
}