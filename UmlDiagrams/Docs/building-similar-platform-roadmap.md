# Roadmap: Eine aehnliche App Platform in einer Firma bauen

Dieses Dokument beschreibt, was eine Firma erklaeren und implementieren sollte,
wenn sie eine aehnliche Kotlin-Multiplatform-App-Platform bauen moechte.

Es ist als Roadmap gedacht: erst das Zielbild, dann ein MVP, danach Ausbau,
Governance und Skalierung.

## Zielbild

Eine App Platform soll Teams helfen, Apps mit klaren Architekturgrenzen zu
bauen.

Das Ziel ist nicht nur Code-Sharing. Das Ziel ist ein wiederholbarer Weg, Apps
zu strukturieren:

- gemeinsame Business-Logik
- klare Modulgrenzen
- plattformspezifische UI
- kontrollierte Dependency Injection
- explizite Lebenszyklen
- gute Testbarkeit
- einheitlicher App-Start
- wiederverwendbare Feature-Patterns

## Nicht-Ziele

Eine Firma sollte auch klar sagen, was die Plattform nicht sein soll.

Moegliche Nicht-Ziele:

- kein komplettes Designsystem
- kein Backend-Framework
- kein Ersatz fuer Produktarchitektur
- kein Zwang, jede UI identisch aussehen zu lassen
- kein Framework, das alle Plattformdetails versteckt
- kein Freifahrtschein fuer globale Singletons

Diese Grenzen sind wichtig, damit Teams die Plattform richtig einsetzen.

## Was erklaert werden muss

### Architekturprinzipien

Teams muessen verstehen:

- Warum gibt es Public APIs?
- Warum sind Implementierungen getrennt?
- Warum darf nur die finale App konkrete Impl-Module zusammensetzen?
- Warum sind Presenter und Renderer getrennt?
- Warum ist der Scope eine Lebenszyklusgrenze?

Ohne diese Prinzipien wirkt die Plattform schnell wie zusaetzliche Komplexitaet.

### Modulstruktur

Die Modulregeln muessen sehr klar dokumentiert sein.

Mindestens erklaeren:

| Modulart | Zweck |
| --- | --- |
| `:public` | APIs, Models, Contracts |
| `:impl` | Produktionsimplementierungen |
| `:testing` | Fakes und Testhilfen |
| `:*-robots` | UI-Test-Robots |
| `:app` | finale App-Zusammensetzung |

Wichtig ist auch, verbotene Abhaengigkeiten mit Beispielen zu zeigen.

### Dependency Injection

Die Firma muss entscheiden und dokumentieren:

- Welches DI-System wird empfohlen?
- Wie werden Plattformwerte bereitgestellt?
- Wo leben AppGraph oder Component?
- Wie werden Bindings beigetragen?
- Wie sehen Fehlermeldungen und Debugging aus?

### Scope- und Lifecycle-Modell

Ein Scope-Modell braucht klare Regeln:

- Wann entsteht ein Scope?
- Wer beendet ihn?
- Welche Services duerfen darin leben?
- Wie werden Coroutines gebunden?
- Gibt es Child Scopes?
- Wie verhindert man Leaks?

### Presenter-Modell

Teams muessen wissen:

- Was gehoert in Presenter?
- Was gehoert nicht in Presenter?
- Wie sehen Models aus?
- Wie werden Events modelliert?
- Wie wird Navigation abgebildet?
- Wie testet man Presenter?

### Renderer-Modell

Teams muessen wissen:

- Wie wird ein Model gerendert?
- Wie findet die RendererFactory den passenden Renderer?
- Welche Renderer-Arten gibt es?
- Wie geht man mit Plattformunterschieden um?
- Was passiert, wenn kein Renderer existiert?

### Testing-Strategie

Eine Plattform ohne Teststrategie bleibt unvollstaendig.

Dokumentiert werden sollten:

- Unit Tests
- Presenter Tests
- Renderer Tests
- Android Instrumented Tests
- Desktop UI Tests
- iOS Tests
- Wasm Tests
- Fakes
- Robots
- Test-Fixtures

### Migration

Viele Firmen starten nicht auf der gruenen Wiese. Deshalb braucht es eine
Migrationsstrategie:

- bestehende App analysieren
- ein Feature isolieren
- Public API einfuehren
- Impl-Modul herausziehen
- Presenter/Renderer trennen
- Fakes ergaenzen
- AppGraph integrieren
- altes Muster schrittweise entfernen

## Was implementiert werden muss

### Core APIs

Ein MVP braucht stabile Kern-APIs:

- `Scope`
- `RootScopeProvider`
- `Presenter`
- `BaseModel`
- `Renderer`
- `RendererFactory`
- Lifecycle-Hooks

Diese APIs sollten klein starten. Zu grosse Kern-APIs werden schwer zu aendern.

### DI-Integration

Die Plattform braucht eine klare DI-Integration.

Moegliche Optionen:

- Metro
- Dagger
- kotlin-inject
- Koin
- firmeneigenes DI

Fuer eine KMP-Plattform ist Compile-time DI oft attraktiv, weil Fehler frueh im
Build sichtbar werden.

### Scope Runtime

Die Scope Runtime sollte mindestens koennen:

- Services registrieren
- Services abrufen
- Cleanup ausfuehren
- CoroutineScopes verwalten
- Parent/Child-Beziehungen optional ermoeglichen

### Presenter Runtime

Die Plattform braucht ein Presenter-Modell:

- einheitliche Presenter-Interfaces
- Model-Erzeugung
- Event-Verarbeitung
- Testhilfen
- optional Molecule-Unterstuetzung

### Renderer Runtime

Die Renderer Runtime braucht:

- Renderer-Interface
- RendererFactory
- Lookup nach Model-Typ
- Caching-Regeln
- Fehler fuer fehlende Renderer
- Plattformadapter

### Platform Bootstrap

Fuer jede Zielplattform braucht es einen Startpfad:

| Plattform | Mindest-Implementierung |
| --- | --- |
| Android | Application, Activity, RootScope-Erzeugung |
| iOS | ViewController, Framework-Einbindung |
| Desktop | Main-Funktion, Window Setup |
| Web/Wasm | Browser-Bootstrap, Distribution Setup |

Ein MVP kann mit Android und Desktop starten. iOS und Wasm koennen spaeter
folgen.

### Gradle Plugin

Ein Gradle Plugin ist fuer Skalierung entscheidend.

Es sollte automatisieren:

- Kotlin Multiplatform Targets
- Compose-Konfiguration
- DI-Konfiguration
- KSP oder Compiler-Plugin-Konfiguration
- Testtasks
- Modulstrukturregeln
- Android Namespace
- Publishing-Metadaten

Ohne Plugin muessen Teams zu viel Build-Logik kopieren.

### Modulstruktur-Check

Die Modulregeln muessen technisch erzwungen werden.

Ein Build-Check sollte verhindern:

- Non-app Modul haengt von `:impl` ab
- Impl-Details leaken in Public APIs
- falsche Modulnamen
- fehlende Public/Impl-Paare bei Features

Die wichtigste Regel ist:

```text
Only :app modules may depend on :impl modules.
```

### Codegenerierung

Codegen ist nicht fuer den ersten Tag zwingend, wird aber schnell wichtig.

Moegliche Codegen-Funktionen:

- DI-Bindings
- Renderer-Bindings
- Feature-Beitraege
- Graph-Erweiterungen
- Test-Runner
- API-Metadaten

Wichtig: Codegen braucht gute Fehlermeldungen.

### Starter Template

Ein Starter Template ist Pflicht, wenn andere Teams die Plattform nutzen sollen.

Es sollte enthalten:

- minimale App
- ein Beispiel-Feature
- Public/Impl/Testing-Struktur
- AppGraph
- RootScope
- Presenter
- Renderer
- Tests
- README mit Run-Befehlen

### Sample App

Eine groessere Sample App zeigt echte Patterns.

Sie sollte enthalten:

- mehrere Features
- Navigation
- Fakes
- Robots
- Plattform-Entrypoints
- Presenter-Komposition
- Renderer-Komposition
- Tests fuer mehrere Plattformen

## MVP-Roadmap

### Phase 1: Architektur-MVP

Ziel: Eine kleine App laeuft mit klaren Kernabstraktionen.

Lieferumfang:

- `Scope`
- `Presenter`
- `BaseModel`
- `Renderer`
- `RendererFactory`
- ein DI-Graph
- Android-Startpfad
- Desktop-Startpfad
- ein Beispiel-Feature
- einfache Tests

Noch nicht notwendig:

- Compiler Plugin
- komplexe Codegenerierung
- iOS
- Wasm
- umfangreiche Robots

### Phase 2: Modulstruktur und Build-Plugin

Ziel: Teams koennen neue Features nach festen Regeln bauen.

Lieferumfang:

- Gradle Plugin
- Modulstruktur-Check
- `:public` / `:impl` / `:testing` Muster
- Beispielmodule
- CI-Tasks
- klare Fehlermeldungen

### Phase 3: Multi-Platform-Ausbau

Ziel: Die Plattform funktioniert auf allen gewuenschten Targets.

Lieferumfang:

- iOS Bootstrap
- Wasm Bootstrap
- plattformspezifische AppGraphs
- Plattformprovider
- Compose Multiplatform Integration
- Plattformtests

### Phase 4: Codegen und Developer Experience

Ziel: Weniger Boilerplate, bessere Fehlermeldungen.

Lieferumfang:

- Annotationen fuer Bindings
- Renderer-Registrierung
- Graph-Beitraege
- Diagnosemeldungen
- Testdaten fuer Codegen
- Troubleshooting Docs

### Phase 5: Governance und Skalierung

Ziel: Die Plattform bleibt ueber viele Teams hinweg konsistent.

Lieferumfang:

- API Review-Prozess
- Versionierung
- Changelog
- Release Pipeline
- Deprecation Policy
- Migration Guides
- Architekturentscheidungen als ADRs
- Ownership-Modell

## Kritische Designentscheidungen

### DI-System

Die Wahl des DI-Systems beeinflusst Build-Zeit, Fehlermeldungen,
Multiplatform-Unterstuetzung und Codegen-Aufwand.

Eine Firma sollte frueh entscheiden:

- Compile-time oder Runtime DI?
- Annotationen oder manuelle Module?
- Wie gut ist KMP-Support?
- Wie gut sind Fehlermeldungen?

### Navigation

Navigation muss bewusst modelliert werden.

Optionen:

- model-driven navigation
- Router
- Plattformnavigation
- hybride Loesung

Die Entscheidung beeinflusst Presenter, Models, Tests und Renderer.

### UI-Strategie

Die Firma muss klaeren:

- Wird Compose Multiplatform genutzt?
- Gibt es Android View Interop?
- Gibt es SwiftUI/UIKit Renderer?
- Muss Web/Wasm dieselben Renderer verwenden?
- Wie viel Plattform-Look-and-Feel ist erlaubt?

### Scope-Hierarchie

Ein RootScope reicht fuer ein MVP. Spaeter koennen Child Scopes sinnvoll sein:

- User Session Scope
- Feature Scope
- Screen Scope
- Flow Scope

Aber: Mehr Scope-Ebenen bedeuten mehr Lifecycle-Komplexitaet.

## Risiken

| Risiko | Gegenmassnahme |
| --- | --- |
| Plattform wird zu abstrakt | Mit echter Sample App validieren. |
| Modulregeln werden ignoriert | Build-Checks erzwingen. |
| DI-Fehler sind unverstaendlich | Diagnosen und Troubleshooting schreiben. |
| Zu viel Boilerplate | Codegen schrittweise einfuehren. |
| Teams umgehen Presenter/Renderer | Beispiele, Reviews und Tests bereitstellen. |
| Scope-Leaks | Lifecycle-Regeln und Leak-Tests definieren. |
| Migration wird zu gross | Featureweise migrieren. |

## Erfolgskriterien

Eine aehnliche Plattform ist erfolgreich, wenn:

- neue Features nach einem klaren Template entstehen
- Teams Public APIs statt Impl-Klassen verwenden
- Presenter ohne echte UI testbar sind
- Renderer isoliert getestet werden koennen
- Plattformstartpunkte duenn bleiben
- DI-Fehler frueh im Build auffallen
- neue Apps aus einem Starter Template entstehen koennen
- Dokumentation und Beispiele aktuell bleiben

## Empfohlener Start

Der beste erste Schritt ist ein kleiner, echter Vertical Slice:

1. Eine Mini-App.
2. Zwei Plattformen, zum Beispiel Android und Desktop.
3. Ein Feature mit `:public` und `:impl`.
4. Ein Presenter mit Model und Event.
5. Ein Renderer.
6. Ein RootScope.
7. Ein DI-Graph.
8. Ein Test mit Fake.
9. Eine kurze Doku.

Erst wenn dieser Slice gut funktioniert, sollte man in Codegen,
umfangreichere Plattformen und Governance investieren.

## Kernaussage

Eine Firma sollte eine solche Plattform nicht als Sammlung von Libraries bauen,
sondern als **Produkt fuer interne Entwicklerinnen und Entwickler**.

Das bedeutet:

- klare Konzepte
- gute Fehlermeldungen
- Templates
- Tests
- Dokumentation
- Migration
- Governance

Die technischen Bausteine sind wichtig. Aber die Plattform wird erst wertvoll,
wenn Teams sie leicht verstehen, korrekt anwenden und schrittweise in echte Apps
uebernehmen koennen.
