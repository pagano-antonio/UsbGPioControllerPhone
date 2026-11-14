package it.usbgpiocontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import it.usbgpiocontroller.viewmodel.ControllerViewModel

class MainActivity : ComponentActivity() {
    private val vm by viewModels<ControllerViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF176B5B))) { ControllerScreen(vm) } }
    }
}

@Composable
private fun ControllerScreen(vm: ControllerViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) { vm.refresh(); delay(1_000) }
    }
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF5F7F6)) {
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("USB GPIO Controller", style = MaterialTheme.typography.headlineSmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CONNESSIONE USB", style = MaterialTheme.typography.labelLarge)
                    Text(if (state.connected) "● Connected" else "● Disconnected", color = if (state.connected) Color(0xFF16734D) else Color(0xFF707873))
                    Text("${state.deviceName}\nVID 0x%04X · PID 0x%04X".format(state.vid, state.pid), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("DIAGNOSTICA RTS#", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(18.dp).background(if (state.rtsHigh == true) Color(0xFFE53935) else Color(0xFF9AA39F), RoundedCornerShape(50)))
                        Text("RTS# ${when (state.rtsHigh) { true -> "HIGH · deasserted"; false -> "LOW · asserted"; null -> "—" }}", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { vm.assertRts() }, enabled = state.connected, modifier = Modifier.weight(1f)) { Text("ASSERT · LOW") }
                        OutlinedButton(onClick = { vm.deassertRts() }, enabled = state.connected, modifier = Modifier.weight(1f)) { Text("DEASSERT · HIGH") }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DIAGNOSTICA", style = MaterialTheme.typography.labelLarge)
                    Text("Ultimo comando: ${state.lastCommand}")
                    state.error?.let { Text("Errore: $it", color = MaterialTheme.colorScheme.error) }
                    Text("USB request accepted non misura il pin. RTS asserted → RTS# atteso LOW; deasserted → RTS# atteso HIGH. Verificare entrambi con multimetro sul dispositivo reale.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { vm.disconnect() }, enabled = state.connected) { Text("Disconnect") }
                }
            }
        }
    }
}
