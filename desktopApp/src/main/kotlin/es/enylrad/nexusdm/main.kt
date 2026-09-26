package es.enylrad.nexusdm

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import es.enylrad.nexusdm.ai.client.JvmLlmBackendGateway

fun main() {
    // Created once, outside composition, so recompositions never rebuild the app state.
    val gateway = JvmLlmBackendGateway()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "NexusDM",
            state = rememberWindowState(width = 1280.dp, height = 860.dp),
        ) {
            App(gateway)
        }
    }
}
