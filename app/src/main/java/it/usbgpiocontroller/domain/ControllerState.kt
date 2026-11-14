package it.usbgpiocontroller.domain

data class ControllerState(
    val connected: Boolean = false,
    val deviceName: String = "FT232R USB UART",
    val vid: Int = 0x0403,
    val pid: Int = 0x6001,
    val rtsHigh: Boolean? = null,
    val lastCommand: String = "Nessun comando",
    val error: String? = null,
)
