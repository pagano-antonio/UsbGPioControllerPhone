package it.usbgpiocontroller

import org.junit.Assert.assertEquals
import org.junit.Test

class UsbIdentityTest {
    @Test fun ftdi232rIdsAreExpected() {
        assertEquals(0x0403, it.usbgpiocontroller.data.usb.FtdiManager.VID)
        assertEquals(0x6001, it.usbgpiocontroller.data.usb.FtdiManager.PID)
    }
}
