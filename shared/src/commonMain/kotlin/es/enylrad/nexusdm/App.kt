package es.enylrad.nexusdm

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import es.enylrad.nexusdm.ai.client.LlmBackendGateway
import es.enylrad.nexusdm.ui.playground.PlaygroundController
import es.enylrad.nexusdm.ui.playground.PlaygroundScreen

/** Root composable. The platform provides the [gateway] to the available LLM backends. */
@Composable
fun App(gateway: LlmBackendGateway) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val scope = rememberCoroutineScope()
            val controller = remember(gateway) { PlaygroundController(gateway, scope) }
            // Load the installed Ollama models on start so the selector is ready.
            LaunchedEffect(controller) { controller.refreshModels() }
            PlaygroundScreen(controller)
        }
    }
}
