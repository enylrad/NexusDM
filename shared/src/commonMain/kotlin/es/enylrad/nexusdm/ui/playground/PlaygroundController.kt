package es.enylrad.nexusdm.ui.playground

import es.enylrad.nexusdm.ai.MutationProposalService
import es.enylrad.nexusdm.ai.client.LlmBackendGateway
import es.enylrad.nexusdm.ai.client.LlmBackendSettings
import es.enylrad.nexusdm.domain.graph.SubgraphQuery
import es.enylrad.nexusdm.domain.graph.extractSubgraph
import es.enylrad.nexusdm.domain.model.Campaign
import es.enylrad.nexusdm.domain.model.GraphEdge
import es.enylrad.nexusdm.domain.model.GraphNode
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.mutation.MutationBatch
import es.enylrad.nexusdm.domain.mutation.MutationJson
import es.enylrad.nexusdm.sample.SampleCampaign
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class BackendKind { OLLAMA, CLAUDE }

/** Formats milliseconds as seconds with one decimal, e.g. 12345 -> "12.3". */
fun formatSeconds(millis: Long): String = "${millis / 1000}.${(millis % 1000) / 100}"

/** Outcome of a successful proposal request. */
data class ProposalResult(
    val rationale: String,
    val operations: List<FormattedOperation>,
    val rawJson: String,
    val elapsedMillis: Long,
    val contextNodeCount: Int,
    val contextEdgeCount: Int,
)

data class PlaygroundState(
    val campaign: Campaign,
    val nodes: List<GraphNode>,
    val backend: BackendKind = BackendKind.OLLAMA,
    val ollamaUrl: String = LlmBackendSettings.DEFAULT_OLLAMA_URL,
    val ollamaModels: List<String> = emptyList(),
    val selectedOllamaModel: String? = null,
    val loadingModels: Boolean = false,
    val modelsError: String? = null,
    val claudeApiKey: String = "",
    val claudeModel: String = LlmBackendSettings.DEFAULT_CLAUDE_MODEL,
    val focusNodeIds: Set<NodeId> = emptySet(),
    val depth: Int = 1,
    val request: String = "",
    val running: Boolean = false,
    val result: ProposalResult? = null,
    val error: String? = null,
) {
    val backendReady: Boolean
        get() = when (backend) {
            BackendKind.OLLAMA -> !selectedOllamaModel.isNullOrBlank() && ollamaUrl.isNotBlank()
            BackendKind.CLAUDE -> claudeModel.isNotBlank()
        }

    val canRun: Boolean
        get() = !running && backendReady && request.isNotBlank() && focusNodeIds.isNotEmpty()
}

/**
 * State holder of the AI playground: sends a DM request about the selected nodes of a
 * campaign to the chosen backend and exposes the proposed mutations. It has no UI
 * dependencies so it can be tested on its own.
 */
class PlaygroundController(
    private val gateway: LlmBackendGateway,
    private val scope: CoroutineScope,
    campaign: Campaign = SampleCampaign.campaign,
    nodes: List<GraphNode> = SampleCampaign.nodes,
    private val edges: List<GraphEdge> = SampleCampaign.edges,
    initialFocus: Set<NodeId> = setOf(NodeId("npc_varis")),
) {
    private val _state = MutableStateFlow(
        PlaygroundState(
            campaign = campaign,
            nodes = nodes.sortedWith(compareBy({ it.type.ordinal }, { it.name })),
            focusNodeIds = initialFocus.filter { id -> nodes.any { it.id == id } }.toSet(),
        ),
    )
    val state: StateFlow<PlaygroundState> = _state.asStateFlow()

    private val formatter = OperationFormatter(nodes, edges)
    private val prettyJson = Json(from = MutationJson.json) { prettyPrint = true }
    private var runJob: Job? = null

    fun selectBackend(backend: BackendKind) = _state.update { it.copy(backend = backend, error = null) }

    fun setOllamaUrl(url: String) = _state.update { it.copy(ollamaUrl = url.trim()) }

    fun selectOllamaModel(model: String) = _state.update { it.copy(selectedOllamaModel = model) }

    fun setClaudeApiKey(key: String) = _state.update { it.copy(claudeApiKey = key.trim()) }

    fun setClaudeModel(model: String) = _state.update { it.copy(claudeModel = model.trim()) }

    fun setRequest(request: String) = _state.update { it.copy(request = request) }

    fun setDepth(depth: Int) = _state.update { it.copy(depth = depth.coerceIn(0, MAX_DEPTH)) }

    fun toggleFocus(id: NodeId) = _state.update { current ->
        val focus = if (id in current.focusNodeIds) current.focusNodeIds - id else current.focusNodeIds + id
        current.copy(focusNodeIds = focus)
    }

    /** Loads the models installed in Ollama and keeps the selection when still available. */
    fun refreshModels() {
        val url = _state.value.ollamaUrl
        _state.update { it.copy(loadingModels = true, modelsError = null) }
        scope.launch {
            try {
                val models = gateway.listOllamaModels(url).sorted()
                _state.update { current ->
                    val selected = current.selectedOllamaModel?.takeIf { it in models } ?: models.firstOrNull()
                    current.copy(ollamaModels = models, selectedOllamaModel = selected, loadingModels = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(ollamaModels = emptyList(), loadingModels = false, modelsError = e.describe()) }
            }
        }
    }

    /** Sends the request to the selected backend. Does nothing when [PlaygroundState.canRun] is false. */
    fun run() {
        val snapshot = _state.value
        if (!snapshot.canRun) return
        _state.update { it.copy(running = true, error = null, result = null) }
        runJob = scope.launch {
            try {
                val subgraph = extractSubgraph(
                    nodes = snapshot.nodes,
                    edges = edges,
                    query = SubgraphQuery(
                        campaignId = snapshot.campaign.id,
                        seedIds = snapshot.focusNodeIds,
                        maxDepth = snapshot.depth,
                    ),
                )
                val service = MutationProposalService(gateway.createClient(snapshot.backendSettings()))
                val started = TimeSource.Monotonic.markNow()
                val batch = service.propose(snapshot.campaign, subgraph, snapshot.request)
                val result = ProposalResult(
                    rationale = batch.rationale,
                    operations = formatter.format(batch),
                    rawJson = prettyJson.encodeToString(MutationBatch.serializer(), batch),
                    elapsedMillis = started.elapsedNow().inWholeMilliseconds,
                    contextNodeCount = subgraph.nodes.size,
                    contextEdgeCount = subgraph.edges.size,
                )
                _state.update { it.copy(running = false, result = result) }
            } catch (e: CancellationException) {
                _state.update { it.copy(running = false) }
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(running = false, error = e.describe()) }
            }
        }
    }

    fun cancel() {
        runJob?.cancel()
        runJob = null
        _state.update { it.copy(running = false) }
    }

    private fun PlaygroundState.backendSettings(): LlmBackendSettings = when (backend) {
        BackendKind.OLLAMA -> LlmBackendSettings.Ollama(model = selectedOllamaModel.orEmpty(), baseUrl = ollamaUrl)
        BackendKind.CLAUDE -> LlmBackendSettings.Claude(apiKey = claudeApiKey.ifBlank { null }, model = claudeModel)
    }

    private fun Exception.describe(): String = message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "Unknown error"

    private companion object {
        const val MAX_DEPTH = 3
    }
}
