# Amazon App Platform - Diagrammuebersicht

Dieser Ordner enthaelt Draw.io-Diagramme und begleitende Markdown-Dateien zur
Architektur der Amazon App Platform.

## Empfohlene Lesereihenfolge

| Reihenfolge | Datei | Frage, die das Diagramm beantwortet |
| --- | --- | --- |
| 1 | [`app-platform-system-architecture.drawio`](app-platform-system-architecture.drawio) | Welche grossen Schichten und Komponenten hat die App Platform? |
| 2 | [`app-platform-runtime-sequence.drawio`](app-platform-runtime-sequence.drawio) | Was passiert Schritt fuer Schritt beim App-Start bis zur gerenderten UI? |
| 3 | [`app-platform-module-dependencies.drawio`](app-platform-module-dependencies.drawio) | Warum gibt es `:public`, `:impl`, `:testing`, `:*-robots` und `:app`? |
| 4 | [`app-platform-presenter-renderer-dataflow.drawio`](app-platform-presenter-renderer-dataflow.drawio) | Wie fliessen Models und Events zwischen Presenter, Renderer und UI? |
| 5 | [`app-platform-di-scope.drawio`](app-platform-di-scope.drawio) | Wie arbeiten RootScope, AppGraph, DI und generierte Bindings zusammen? |
| 6 | [`app-platform-feature-module-template.drawio`](app-platform-feature-module-template.drawio) | Wie sieht ein typisches Feature-Modul in dieser Architektur aus? |

## Begleitende Dokumente

| Datei | Inhalt |
| --- | --- |
| [`app-platform-system-architecture-explanation.md`](app-platform-system-architecture-explanation.md) | Ausfuehrliche Erklaerung des Systemarchitekturdiagramms. |
| [`app-platform-dictionary.md`](app-platform-dictionary.md) | Glossar mit Begriffen aus Architektur, KMP, DI, Scopes, Presenter/Renderer, Gradle und Testing. |

## Kurzbeschreibung der Diagramme

### System Architecture

Das Hauptdiagramm zeigt die App Platform als Gesamtbild. Es trennt Build/Tooling,
Platform Entrypoints, Application Assembly, Feature Modules, Framework Core und
Runtime Flow.

### Runtime Sequence

Dieses Diagramm zeigt den zeitlichen Ablauf beim Start der App:

1. Plattform startet App.
2. RootScope wird erzeugt.
3. AppGraph wird aufgebaut.
4. Presenter berechnen Models.
5. Templates verpacken den Root-Model-Tree.
6. RendererFactory sucht Renderer.
7. Renderer zeigt UI.
8. Events gehen ueber Models zurueck.

### Module Dependencies

Dieses Diagramm erklaert die wichtigste Modulregel:

> Nur finale `:app`-Module duerfen konkrete `:impl`-Module direkt importieren.

Wiederverwendbare Module sollen gegen `:public` APIs arbeiten. Tests koennen
`:testing`-Module und Robots verwenden.

### Presenter / Renderer Dataflow

Dieses Diagramm zeigt den unidirektionalen Datenfluss:

- Presenter erzeugen immutable Models.
- Renderer zeigen Models an.
- UI-Aktionen werden als Events ueber Model-Lambdas zurueck an Presenter
  gegeben.

### DI / Scope Architecture

Dieses Diagramm zeigt, wie plattformspezifische Werte in den AppGraph gelangen,
wie der Graph im RootScope registriert wird und wie generierte Bindings
Implementierungen mit Public APIs verbinden.

### Feature Module Template

Dieses Diagramm ist ein Bauplan fuer neue oder bestehende Features. Es zeigt,
welche Dateien und Konzepte typischerweise in `:public`, `:impl`, `:testing`,
`:impl-robots` und `:app` gehoeren.
