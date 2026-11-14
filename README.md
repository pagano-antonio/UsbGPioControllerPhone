# UsbGpioController

App Android Kotlin/Jetpack Compose per comandare direttamente RTS di un FT232RL tramite USB Host. Minimum SDK 23.

## Controllo USB e diagnostica RTS#

L'app **non usa D2XX**: non c'è una libreria D2XX né una sua versione configurata nel progetto. Apre il dispositivo con Android USB Host e invia la richiesta FTDI `SIO_SET_MODEM_CTRL` al dispositivo aperto. Non attiva BitBang.

La richiesta RTS usa il bit 9 per abilitare RTS e il bit 1 per il suo stato. La precedente maschera `0x0100` abilitava DTR; perciò cambiava DTR lasciando RTS invariato. La diagnostica invia:

| Pulsante | Stato logico RTS | `wValue` | Livello atteso sul pin fisico RTS# |
| --- | --- | --- | --- |
| ASSERT · LOW | Asserted | `0x0202` | LOW, circa 0 V |
| DEASSERT · HIGH | Deasserted | `0x0200` | HIGH, livello I/O del dispositivo (tipicamente circa 3,3 V) |

RTS# è un'uscita attiva bassa. L'accettazione della richiesta USB non è una misura elettrica: verificare i due stati sul pin RTS# rispetto a GND con il dispositivo reale. Le chiamate avvengono su `Dispatchers.IO`; l'interfaccia aggiorna l'ultimo comando e il livello atteso solo dopo una risposta USB non negativa. I dettagli della richiesta e del risultato sono registrati con tag `UsbGpioController`.

## Build e installazione

Il progetto usa Android Gradle Plugin 9.5.0 (compatibile con Gradle 9.6+) e richiede Android SDK 37 per compilare. Aprire la cartella in Android Studio con questi componenti installati. Eseguire `./gradlew assembleDebug` e installare `app/build/outputs/apk/debug/app-debug.apk` (oppure `adb install -r app/build/outputs/apk/debug/app-debug.apk`). Se non esiste un wrapper, generarlo con `gradle wrapper` da un'installazione Gradle compatibile. Il Galaxy M32 deve supportare USB host/OTG e concedere il permesso USB quando richiesto.

## Uso e limiti

Collegare l'adattatore FT232RL tramite OTG e concedere il permesso USB quando richiesto. L'app riconosce VID `0403` / PID `6001`. Usare alternativamente ASSERT · LOW e DEASSERT · HIGH per la diagnosi, quindi Disconnect. Non invia dati seriali e non abilita BitBang. Il comportamento elettrico va misurato sul dispositivo reale. Per pilotare un LED, usare una resistenza in serie e GND comune, rispettando i limiti elettrici del datasheet FT232RL; non collegare carichi elevati.
