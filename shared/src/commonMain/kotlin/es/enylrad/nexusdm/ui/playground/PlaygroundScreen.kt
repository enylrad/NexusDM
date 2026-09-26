package es.enylrad.nexusdm.ui.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import es.enylrad.nexusdm.domain.model.GraphNode
import kotlin.math.roundToInt
import nexusdm.shared.generated.resources.Res
import nexusdm.shared.generated.resources.action_cancel
import nexusdm.shared.generated.resources.action_run
import nexusdm.shared.generated.resources.backend_claude
import nexusdm.shared.generated.resources.backend_ollama
import nexusdm.shared.generated.resources.claude_api_key
import nexusdm.shared.generated.resources.depth_label
import nexusdm.shared.generated.resources.error_message
import nexusdm.shared.generated.resources.focus_hint
import nexusdm.shared.generated.resources.hide_json
import nexusdm.shared.generated.resources.loading_models
import nexusdm.shared.generated.resources.model_label
import nexusdm.shared.generated.resources.models_error
import nexusdm.shared.generated.resources.no_models
import nexusdm.shared.generated.resources.no_operations
import nexusdm.shared.generated.resources.node_locked
import nexusdm.shared.generated.resources.ollama_url
import nexusdm.shared.generated.resources.op_add_edge
import nexusdm.shared.generated.resources.op_add_node
import nexusdm.shared.generated.resources.op_merge_nodes
import nexusdm.shared.generated.resources.op_remove_edge
import nexusdm.shared.generated.resources.op_remove_node
import nexusdm.shared.generated.resources.op_update_edge
import nexusdm.shared.generated.resources.op_update_node
import nexusdm.shared.generated.resources.operations_title
import nexusdm.shared.generated.resources.playground_subtitle
import nexusdm.shared.generated.resources.playground_title
import nexusdm.shared.generated.resources.rationale_title
import nexusdm.shared.generated.resources.refresh_models
import nexusdm.shared.generated.resources.request_placeholder
import nexusdm.shared.generated.resources.result_stats
import nexusdm.shared.generated.resources.running
import nexusdm.shared.generated.resources.section_backend
import nexusdm.shared.generated.resources.section_campaign
import nexusdm.shared.generated.resources.section_request
import nexusdm.shared.generated.resources.select_model
import nexusdm.shared.generated.resources.show_json
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Screen to try the AI backends on the sample campaign. */
@Composable
fun PlaygroundScreen(controller: PlaygroundController, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()

    Row(modifier = modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier.width(380.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackendCard(state, controller)
            CampaignCard(state, controller)
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.playground_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(Res.string.playground_subtitle), style = MaterialTheme.typography.bodyMedium)
            RequestCard(state, controller)
            state.error?.let { ErrorCard(stringResource(Res.string.error_message, it)) }
            state.result?.let { ResultCard(it) }
        }
    }
}

@Composable
private fun BackendCard(state: PlaygroundState, controller: PlaygroundController) {
    SectionCard(stringResource(Res.string.section_backend)) {
        BackendOption(stringResource(Res.string.backend_ollama), state.backend == BackendKind.OLLAMA) {
            controller.selectBackend(BackendKind.OLLAMA)
        }
        BackendOption(stringResource(Res.string.backend_claude), state.backend == BackendKind.CLAUDE) {
            controller.selectBackend(BackendKind.CLAUDE)
        }
        HorizontalDivider()
        when (state.backend) {
            BackendKind.OLLAMA -> OllamaSection(state, controller)
            BackendKind.CLAUDE -> ClaudeSection(state, controller)
        }
    }
}

@Composable
private fun BackendOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}

@Composable
private fun OllamaSection(state: PlaygroundState, controller: PlaygroundController) {
    OutlinedTextField(
        value = state.ollamaUrl,
        onValueChange = controller::setOllamaUrl,
        label = { Text(stringResource(Res.string.ollama_url)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    ModelSelector(state, controller)
    OutlinedButton(onClick = controller::refreshModels, enabled = !state.loadingModels) {
        Text(stringResource(Res.string.refresh_models))
    }
    when {
        state.loadingModels -> Text(stringResource(Res.string.loading_models), style = MaterialTheme.typography.bodySmall)
        state.modelsError != null -> Text(
            stringResource(Res.string.models_error, state.modelsError.orEmpty()),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        state.ollamaModels.isEmpty() -> Text(stringResource(Res.string.no_models), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModelSelector(state: PlaygroundState, controller: PlaygroundController) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = state.ollamaModels.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(state.selectedOllamaModel ?: stringResource(Res.string.select_model))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.ollamaModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        controller.selectOllamaModel(model)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ClaudeSection(state: PlaygroundState, controller: PlaygroundController) {
    OutlinedTextField(
        value = state.claudeApiKey,
        onValueChange = controller::setClaudeApiKey,
        label = { Text(stringResource(Res.string.claude_api_key)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.claudeModel,
        onValueChange = controller::setClaudeModel,
        label = { Text(stringResource(Res.string.model_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CampaignCard(state: PlaygroundState, controller: PlaygroundController) {
    SectionCard(stringResource(Res.string.section_campaign, state.campaign.name)) {
        Text(state.campaign.premise, style = MaterialTheme.typography.bodySmall)
        Text(stringResource(Res.string.focus_hint), style = MaterialTheme.typography.labelLarge)
        state.nodes.forEach { node ->
            NodeRow(node, checked = node.id in state.focusNodeIds) { controller.toggleFocus(node.id) }
        }
        HorizontalDivider()
        Text(stringResource(Res.string.depth_label, state.depth))
        Slider(
            value = state.depth.toFloat(),
            onValueChange = { controller.setDepth(it.roundToInt()) },
            valueRange = 0f..3f,
            steps = 2,
        )
    }
}

@Composable
private fun NodeRow(node: GraphNode, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column {
            Text(node.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            val locked = if (node.isLocked) " · " + stringResource(Res.string.node_locked) else ""
            Text(node.type.name + locked, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RequestCard(state: PlaygroundState, controller: PlaygroundController) {
    SectionCard(stringResource(Res.string.section_request)) {
        OutlinedTextField(
            value = state.request,
            onValueChange = controller::setRequest,
            placeholder = { Text(stringResource(Res.string.request_placeholder)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = controller::run, enabled = state.canRun) {
                Text(stringResource(Res.string.action_run))
            }
            if (state.running) {
                OutlinedButton(onClick = controller::cancel) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        }
        if (state.running) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(Res.string.running), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResultCard(result: ProposalResult) {
    var showJson by remember(result) { mutableStateOf(false) }
    SectionCard(stringResource(Res.string.rationale_title)) {
        SelectionContainer { Text(result.rationale) }
        Text(
            stringResource(Res.string.result_stats, result.contextNodeCount, result.contextEdgeCount, formatSeconds(result.elapsedMillis)),
            style = MaterialTheme.typography.labelSmall,
        )
        HorizontalDivider()
        Text(stringResource(Res.string.operations_title, result.operations.size), style = MaterialTheme.typography.titleMedium)
        if (result.operations.isEmpty()) {
            Text(stringResource(Res.string.no_operations))
        }
        result.operations.forEach { OperationItem(it) }
        TextButton(onClick = { showJson = !showJson }) {
            Text(stringResource(if (showJson) Res.string.hide_json else Res.string.show_json))
        }
        if (showJson) {
            SelectionContainer {
                Text(result.rawJson, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OperationItem(operation: FormattedOperation) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            stringResource(operation.kind.label()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SelectionContainer {
            Column {
                Text(operation.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                operation.details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

private fun OperationKind.label(): StringResource = when (this) {
    OperationKind.ADD_NODE -> Res.string.op_add_node
    OperationKind.UPDATE_NODE -> Res.string.op_update_node
    OperationKind.REMOVE_NODE -> Res.string.op_remove_node
    OperationKind.MERGE_NODES -> Res.string.op_merge_nodes
    OperationKind.ADD_EDGE -> Res.string.op_add_edge
    OperationKind.UPDATE_EDGE -> Res.string.op_update_edge
    OperationKind.REMOVE_EDGE -> Res.string.op_remove_edge
}
