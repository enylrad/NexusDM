# NexusDM

Dungeons & Dragons campaign manager built with Kotlin and Compose Multiplatform (Desktop).

NexusDM keeps every campaign as a **local knowledge graph** that acts as the Single Source of
Truth. Instead of feeding the whole campaign documentation to an LLM each time (and overflowing
its context), only the relevant subgraph is sent, and the LLM answers with structured graph
mutations instead of free text.

## Architecture flow

1. **Ingestion** – initial documentation (and, later, Foundry VTT world exports) is turned into a
   graph of entities (NPCs, factions, locations, events, …) and their relationships.
2. **Context extraction** – when the DM proposes a new idea, only the affected subgraph
   (`SubgraphQuery` → `Subgraph`) is loaded from the local database and sent to the LLM.
3. **Structured output** – the LLM returns a `MutationBatch`: an ordered list of
   `GraphMutation`s (`add_node`, `update_node`, `remove_node`, `merge_nodes`, `add_edge`,
   `update_edge`, `remove_edge`).
4. **Apply & render** – the batch is validated and applied to the local graph, and the campaign
   documents (DM and player views) are regenerated from it.

## Domain overview

- **Campaign** – container of one graph. The app supports many campaigns.
- **GraphNode** – generic entity with a `NodeType`, a short `summary` (LLM context), a long
  `description` (documents), free-form `properties`, `visibility` (DM only / revealed),
  Foundry links (`ExternalRef`) and attached files (`AssetRef`).
- **NodeType** – `PLAYER_CHARACTER`, `NPC`, `CREATURE`, `FACTION`, `LOCATION`, `MAP`, `EVENT`,
  `SESSION`, `ENCOUNTER`, `QUEST`, `ITEM`, `DEITY`, `LORE`.
- **NodeTypeSchema** – well-known property keys per node type (e.g. `level` for player
  characters, `session_number` for sessions, `challenge_rating` for creatures).
- **GraphEdge / EdgeType** – directed, typed relationships (`MEMBER_OF`, `LOCATED_IN`,
  `INVOLVES`, `FEATURED_IN`, `DEPICTS`, …) with optional source/target type constraints.
- **Foundry VTT** – nodes can point to Foundry documents (Actor, Scene, Item, JournalEntry, …)
  through their UUID. Foundry links and assets are never sent to or modified by the LLM.

## Project structure

- `desktopApp` – desktop entry point (`main()`).
- `shared` – all application code, organized by layer under `es.enylrad.nexusdm`:

```
es.enylrad.nexusdm
├── domain
│   ├── model        // Campaign, GraphNode, GraphEdge, NodeType, EdgeType, Visibility, ExternalRef, AssetRef
│   ├── schema       // PropertySpec, NodeTypeSchema
│   ├── mutation     // MutationBatch, GraphMutation, NodePatch, EdgePatch, NodeRef (LLM output contract)
│   ├── graph        // SubgraphQuery, Subgraph (traversal, validation and mutation applier to come)
│   └── repository   // Repository interfaces (planned)
├── data
│   ├── local        // Room database, entities, DAOs (planned)
│   ├── mapper       // Entity <-> domain mappers (planned)
│   └── repository   // Repository implementations (planned)
├── ai
│   ├── client       // LLM client (planned)
│   ├── context      // Subgraph -> prompt context (planned)
│   └── schema       // JSON/tool schema for MutationBatch (planned)
├── ingestion        // Documents -> graph extraction (planned)
├── integration
│   └── foundry      // Foundry VTT import/sync (planned)
├── rendering        // Graph -> campaign documents (planned)
├── ui               // Compose screens (planned)
└── di               // Dependency wiring (planned)
```

## Conventions

- All code, identifiers and comments are written in English.
- New work goes to `develop`; `master` only receives final releases.

## Running

- Desktop app: `./gradlew :desktopApp:run` (hot reload: `./gradlew :desktopApp:hotRun --auto`)
- Tests: `./gradlew :shared:jvmTest`
