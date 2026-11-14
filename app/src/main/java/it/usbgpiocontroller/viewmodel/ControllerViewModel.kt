package it.usbgpiocontroller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.usbgpiocontroller.data.usb.FtdiManager
import it.usbgpiocontroller.domain.ControllerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ControllerViewModel(app: Application) : AndroidViewModel(app) {
    private val ftdi = FtdiManager(app)
    private val _state = MutableStateFlow(ControllerState())
    val state = _state.asStateFlow()

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val present = ftdi.findSupportedDevice() != null
        if (!present && _state.value.connected) {
            ftdi.disconnect()
            _state.value = _state.value.copy(connected = false, rtsHigh = null, error = "Dispositivo USB scollegato.")
        } else if (present && !_state.value.connected) {
            try {
                ftdi.connect()
                // Start deasserted. RTS# is active low, so the physical pin is expected HIGH.
                ftdi.deassertRts()
                _state.value = _state.value.copy(connected = true, rtsHigh = true, lastCommand = "RTS deasserted · RTS# HIGH", error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(connected = false, error = e.message ?: "Errore USB/RTS.")
            }
        }
    }

    fun assertRts() = commandRts(asserted = true)

    fun deassertRts() = commandRts(asserted = false)

    private fun commandRts(asserted: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        try {
            if (asserted) ftdi.assertRts() else ftdi.deassertRts()
            val expectedPinHigh = !asserted
            _state.value = _state.value.copy(
                connected = true,
                rtsHigh = expectedPinHigh,
                lastCommand = if (asserted) "RTS asserted · RTS# LOW" else "RTS deasserted · RTS# HIGH",
                error = null,
            )
        } catch (e: Exception) {
            // Preserve the last successful output state and command on failure.
            _state.value = _state.value.copy(connected = ftdi.isConnected(), error = e.message ?: "Errore comando RTS.")
        }
    }

    fun disconnect() = viewModelScope.launch(Dispatchers.IO) {
        ftdi.disconnect()
        _state.value = _state.value.copy(connected = false, rtsHigh = null, lastCommand = "Disconnesso", error = null)
    }

    override fun onCleared() { ftdi.disconnect() }
}
