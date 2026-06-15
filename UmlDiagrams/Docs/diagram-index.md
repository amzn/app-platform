# Amazon App Platform - Diagrammuebersicht

Dieser Ordner enthaelt die begleitenden Markdown-Dokumente zur Architektur der
Amazon App Platform. Die editierbaren Draw.io-Dateien liegen getrennt davon im
Ordner [`../Diagrams`](../Diagrams).

## Ordnerstruktur

| Ordner | Inhalt |
| --- | --- |
| [`../Diagrams`](../Diagrams) | Editierbare `.drawio`-Diagramme. |
| [`../Docs`](.) | Erklaerungen, Glossar, Roadmap und Lesereihenfolge. |

## Empfohlene Lesereihenfolge

| Reihenfolge | Diagramm | Erklaerung | Frage, die beantwortet wird |
| --- | --- | --- | --- |
| 1 | [`app-platform-system-architecture.drawio`](../Diagrams/app-platform-system-architecture.drawio) | [`app-platform-system-architecture-explanation.md`](app-platform-system-architecture-explanation.md) | Welche grossen Schichten und Komponenten hat die App Platform? |
| 2 | [`app-platform-runtime-sequence.drawio`](../Diagrams/app-platform-runtime-sequence.drawio) | [`app-platform-runtime-sequence-explanation.md`](app-platform-runtime-sequence-explanation.md) | Was passiert Schritt fuer Schritt beim App-Start bis zur gerenderten UI? |
| 3 | [`app-platform-module-dependencies.drawio`](../Diagrams/app-platform-module-dependencies.drawio) | [`app-platform-module-dependencies-explanation.md`](app-platform-module-dependencies-explanation.md) | Warum gibt es `:public`, `:impl`, `:testing`, `:*-robots` und `:app`? |
| 4 | [`app-platform-presenter-renderer-dataflow.drawio`](../Diagrams/app-platform-presenter-renderer-dataflow.drawio) | [`app-platform-presenter-renderer-dataflow-explanation.md`](app-platform-presenter-renderer-dataflow-explanation.md) | Wie fliessen Models und Events zwischen Presenter, Renderer und UI? |
| 5 | [`app-platform-di-scope.drawio`](../Diagrams/app-platform-di-scope.drawio) | [`app-platform-di-scope-explanation.md`](app-platform-di-scope-explanation.md) | Wie arbeiten RootScope, AppGraph, DI und generierte Bindings zusammen? |
| 6 | [`app-platform-feature-module-template.drawio`](../Diagrams/app-platform-feature-module-template.drawio) | [`app-platform-feature-module-template-explanation.md`](app-platform-feature-module-template-explanation.md) | Wie sieht ein typisches Feature-Modul in dieser Architektur aus? |

## Weitere Dokumente

| Datei | Inhalt |
| --- | --- |
| [`app-platform-dictionary.md`](app-platform-dictionary.md) | Glossar mit Begriffen aus Architektur, KMP, DI, Scopes, Presenter/Renderer, Gradle und Testing. |
| [`building-similar-platform-roadmap.md`](building-similar-platform-roadmap.md) | Roadmap fuer Firmen, die eine aehnliche App Platform bauen moechten. |

## Kurzbeschreibung der Diagramme

### System Architecture

Das Hauptdiagramm zeigt die App Platform als Gesamtbild. Es trennt Build/Tooling,
Platform Entrypoints, Application Assembly, Feature Modules, Framework Core und
Runtime Flow.

### Runtime Sequence

Dieses Diagramm zeigt den Ablauf beim Start der App: Plattformstart, RootScope,
DI-Graph, Presenter, Template, RendererFactory, Renderer und Event-Rueckweg.

### Module Dependencies

Dieses Diagramm erklaert die wichtigste Modulregel:

> Nur finale `:app`-Module duerfen konkrete `:impl`-Module direkt importieren.

Wiederverwendbare Module sollen gegen `:public` APIs arbeiten. Tests koennen
`:testing`-Module und Robots verwenden.

### Presenter / Renderer Dataflow

Dieses Diagramm zeigt den unidirektionalen Datenfluss: Presenter erzeugen
immutable Models, Renderer zeigen diese Models an, UI-Aktionen werden ueber
Model-Lambdas zurueck an Presenter gegeben.

### DI / Scope Architecture

Dieses Diagramm zeigt, wie plattformspezifische Werte in den AppGraph gelangen,
wie der Graph im RootScope registriert wird und wie generierte Bindings
Implementierungen mit Public APIs verbinden.

### Feature Module Template

Dieses Diagramm ist ein Bauplan fuer neue oder bestehende Features. Es zeigt,
welche Dateien und Konzepte typischerweise in `:public`, `:impl`, `:testing`,
`:impl-robots` und `:app` gehoeren.
