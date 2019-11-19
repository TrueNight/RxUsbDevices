package xyz.truenight.usbdevices.rx

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import xyz.truenight.usbdevices.DeviceReceiver
import xyz.truenight.utils.optional.ifPresent

class RootDeviceConnector<M : Any>(
    context: Context,
    filterResId: Int,
    private val mapping: (UsbDevice?) -> M?
) : DeviceConnector<M>(context, filterResId, mapping) {

    private val appUid =
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .uid

    init {
        enablePrivateApi()
    }

    override fun registerReceiver(context: Context) {
        val appContext = context.applicationContext
        DeviceReceiver(this, deviceFilter).register(appContext, true)
    }

    // todo add support of multi-connections
    override fun findDevice(): Pair<UsbDevice?, M?> {
        deviceFilter.devices.forEach { device ->
            mapping(device).also {
                it.ifPresent {
                    if (!usbManager.hasPermission(device)) grantAutomaticUsbPermissionRoot(
                        appUid,
                        device
                    )
                }
                it?.apply { return@findDevice device to this }
            }
        }
        return EMPTY
    }

    companion object {

        fun enablePrivateApi() {
            runCommand("settings put global hidden_api_policy_pre_p_apps 1")
            runCommand("settings put global hidden_api_policy_p_apps 1")
        }

        @SuppressLint("SoonBlockedPrivateApi", "PrivateApi", "DiscouragedPrivateApi")
        fun grantAutomaticUsbPermissionRoot(appUid: Int, usbDevice: UsbDevice): Boolean {

            try {
                val serviceManagerClass = Class.forName("android.os.ServiceManager")
                val getServiceMethod =
                    serviceManagerClass.getDeclaredMethod("getService", String::class.java)
                getServiceMethod.isAccessible = true
                val binder =
                    getServiceMethod.invoke(null, Context.USB_SERVICE) as android.os.IBinder

                val iUsbManagerClass = Class.forName("android.hardware.usb.IUsbManager")
                val stubClass = Class.forName("android.hardware.usb.IUsbManager\$Stub")
                val asInterfaceMethod =
                    stubClass.getDeclaredMethod("asInterface", android.os.IBinder::class.java)
                asInterfaceMethod.isAccessible = true
                val iUsbManager = asInterfaceMethod.invoke(null, binder)

                val grantDevicePermissionMethod = iUsbManagerClass.getDeclaredMethod(
                    "grantDevicePermission",
                    UsbDevice::class.java,
                    Int::class.javaPrimitiveType
                )
                grantDevicePermissionMethod.isAccessible = true
                grantDevicePermissionMethod.invoke(iUsbManager, usbDevice, appUid)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }
}