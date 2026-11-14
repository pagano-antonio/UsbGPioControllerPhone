package it.usbgpiocontroller.data.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

/** FT232R USB host wrapper using the chip's documented vendor control request for RTS. */
class FtdiManager(context: Context) {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var openedDevice: UsbDevice? = null
    private var permissionPending = false

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) permissionPending = false
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) {
            appContext.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(permissionReceiver, filter)
        }
    }

    fun findSupportedDevice(): UsbDevice? = usbManager.deviceList.values.firstOrNull {
        it.vendorId == VID && it.productId == PID
    }

    @Synchronized
    fun connect(): Boolean {
        connection?.let { return true }
        val usb = findSupportedDevice() ?: throw IllegalStateException("Dispositivo FTDI VID 0403 / PID 6001 non trovato.")
        if (!usbManager.hasPermission(usb)) {
            if (!permissionPending) {
                permissionPending = true
                val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                usbManager.requestPermission(usb, PendingIntent.getBroadcast(appContext, 0, intent, flags))
            }
            throw IllegalStateException("Concedi il permesso USB nella finestra di sistema.")
        }
        connection = usbManager.openDevice(usb)
            ?: throw IllegalStateException("Apertura USB non riuscita.")
        openedDevice = usb
        Log.i(TAG, "Aperto dispositivo ${usb.deviceName}, VID=%04X PID=%04X".format(usb.vendorId, usb.productId))
        return true
    }

    @Synchronized
    fun assertRts() = setRts(asserted = true)

    @Synchronized
    fun deassertRts() = setRts(asserted = false)

    private fun setRts(asserted: Boolean) {
        val current = connection ?: throw IllegalStateException("Dispositivo non connesso.")
        val device = openedDevice ?: throw IllegalStateException("Nessun dispositivo FTDI aperto.")
        // FTDI SIO_SET_MODEM_CTRL uses bit 9 to enable RTS and bit 1 for its logical state.
        // The previous 0x0100 mask enabled DTR; it left RTS unchanged (and changed bit 0 of DTR).
        val value = RTS_ENABLE or if (asserted) RTS_ASSERTED else 0
        val expectedPinLevel = if (asserted) "LOW (RTS# asserted)" else "HIGH (RTS# deasserted)"
        Log.i(TAG, "Invio RTS ${if (asserted) "ASSERT" else "DEASSERT"}: device=${device.deviceName} " +
            "VID=%04X PID=%04X requestType=0x%02X request=0x%02X value=0x%04X index=0 length=0 expectedPin=$expectedPinLevel"
            .format(device.vendorId, device.productId, REQUEST_TYPE_VENDOR_OUT, SIO_SET_MODEM_CTRL, value))
        val result = current.controlTransfer(REQUEST_TYPE_VENDOR_OUT, SIO_SET_MODEM_CTRL, value, 0, null, 0, CONTROL_TIMEOUT_MS)
        Log.i(TAG, "Esito RTS ${if (asserted) "ASSERT" else "DEASSERT"}: result=$result " +
            "(${if (result >= 0) "USB request accepted" else "USB request failed"}); expectedPin=$expectedPinLevel")
        check(result >= 0) { "Il dispositivo ha rifiutato il comando RTS (controlTransfer=$result)." }
    }

    @Synchronized
    fun isConnected(): Boolean = connection != null && findSupportedDevice()?.let(usbManager::hasPermission) == true

    @Synchronized
    fun disconnect() {
        connection?.close()
        connection = null
        openedDevice = null
    }

    companion object {
        const val VID = 0x0403
        const val PID = 0x6001
        private const val TAG = "UsbGpioController"
        private const val ACTION_USB_PERMISSION = "it.usbgpiocontroller.USB_PERMISSION"
        private const val REQUEST_TYPE_VENDOR_OUT = 0x40
        private const val SIO_SET_MODEM_CTRL = 1
        private const val RTS_ENABLE = 0x0200
        private const val RTS_ASSERTED = 0x0002
        private const val CONTROL_TIMEOUT_MS = 1_000
    }
}
