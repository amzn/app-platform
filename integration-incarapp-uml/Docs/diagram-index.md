# inCarApp Integration UML Diagrams

This folder contains readable Draw.io diagrams for designing an inCarApp-oriented multiplatform app platform.

## Diagrams

| # | File | Purpose |
| --- | --- | --- |
| 01 | [System Context](../Diagrams/01-system-context.drawio) | Actors, apps, vehicle APIs, backend, MB Widgets and the platform. |
| 02 | [Platform Layers](../Diagrams/02-platform-layers.drawio) | Shared KMP core, renderers, platform apps and tooling. |
| 03 | [Module Dependencies](../Diagrams/03-module-dependencies.drawio) | Concrete public/impl/provider/renderer/app module rules. |
| 04 | [Vehicle Capability Abstraction](../Diagrams/04-vehicle-capability-abstraction.drawio) | Hardware abstraction through capabilities and explicit result states. |
| 05 | [Presenter Renderer Dataflow](../Diagrams/05-presenter-renderer-dataflow.drawio) | State and event flow between presenter, model, renderer and UI. |
| 06 | [Surface Renderer Strategy](../Diagrams/06-surface-renderer-strategy.drawio) | How MB Widgets and Compose Multiplatform can maximize UI sharing. |
| 07 | [DI Scope Architecture](../Diagrams/07-di-scope-architecture.drawio) | Hilt today, KMP DI later, RootScope and app graph boundaries. |
| 08 | [Runtime Sequence](../Diagrams/08-runtime-sequence.drawio) | Startup and event loop sequence across all target surfaces. |
| 09 | [Migration from current inCarApp](../Diagrams/09-migration-from-current-incarapp.drawio) | Mapping current classes into target platform modules. |
| 10 | [Testing Strategy](../Diagrams/10-testing-strategy.drawio) | Unit, presenter, provider, renderer, screenshot and integration test layers. |
| 11 | [Roadmap](../Diagrams/11-roadmap.drawio) | MVP and rollout phases for the internal platform. |

## Reading Order

Start with 01 and 02 for the high-level picture. Use 03 and 04 for the key architectural constraints. Use 05 through 08 for runtime behavior. Use 09 through 11 for implementation planning.
