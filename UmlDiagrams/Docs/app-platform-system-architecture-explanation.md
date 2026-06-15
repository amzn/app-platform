# Amazon App Platform - Erklaerung des UML-Diagramms

Dieses Dokument erklaert das Draw.io-Diagramm
[`app-platform-system-architecture.drawio`](../Diagrams/app-platform-system-architecture.drawio).
Das Diagramm zeigt die Systemarchitektur der Amazon App Platform als
UML-Komponentenuebersicht.

Die App Platform ist ein Framework fuer Kotlin Multiplatform Apps. Eine App soll
moeglichst viel Logik gemeinsam nutzen koennen, aber trotzdem auf Android, iOS,
Desktop und Web/Wasm jeweils passend gerendert werden. Deshalb trennt die
Architektur sehr deutlich:

- App-Start pro Plattform
- Dependency Injection und Lebenszyklusverwaltung
- Feature-APIs und Feature-Implementierungen
- Presenter fuer Zustand und Business-Logik
- Renderer fuer die tatsaechliche UI
- Tests, Robots und Fakes

## Grundidee

Die wichtigste Idee ist die Trennung zwischen Business-Logik und UI.

Presenter erzeugen den Zustand der Anwendung als Models. Renderer konsumieren
diese Models und stellen sie auf der jeweiligen Plattform dar. Dadurch muss die
Business-Logik nicht wissen, ob sie spaeter auf Android, iOS, Desktop oder im
Browser angezeigt wird.

Der Datenfluss ist weitgehend unidirektional:

1. Die Plattform startet die App.
2. Ein Root Scope wird erzeugt.
3. Der Dependency-Injection-Graph wird aufgebaut.
4. Presenter berechnen immutable Models.
5. Templates verpacken den Root-Model-Tree.
6. Die RendererFactory sucht passende Renderer.
7. Die Plattform rendert die UI.
8. Nutzeraktionen gehen ueber Event-Lambdas zurueck zum Presenter.

## Build, Tooling & Docs

Links im Diagramm befindet sich der Bereich **Build, Tooling & Docs**. Diese
Komponenten gehoeren nicht direkt zur laufenden App, strukturieren aber das
Projekt und automatisieren wichtige Aufgaben.

### Gradle Plugin

Das Gradle Plugin `software.amazon.app.platform` konfiguriert Kotlin
Multiplatform, Compose, KSP, Dependency Injection und App-Platform-spezifische
Regeln. Es ist der zentrale Integrationspunkt, ueber den Module Funktionen der
App Platform aktivieren.

### buildSrc conventions

`buildSrc` enthaelt repo-interne Gradle-Konventionen. Dazu gehoeren unter
anderem Plattformtargets, Android-Emulator-Konfiguration, Desktop-Packaging und
Wasm-Defaults.

### Module structure check

Diese Komponente erzwingt die wichtigste Architekturregel der App Platform:

> Nur `:app`-Module duerfen direkt von `:impl`-Modulen abhaengen.

Dadurch bleiben konkrete Implementierungen von wiederverwendbaren API-Modulen
getrennt. Das reduziert Kopplung, verbessert Testbarkeit und verhindert, dass
Implementierungsdetails versehentlich in andere Module auslaufen.

### Code generation

KSP, Metro Extensions und kotlin-inject Extensions erzeugen Code fuer
Dependency Injection, Renderer-Bindings und weitere App-Platform-Integrationen.
Viele Verknuepfungen im System werden also nicht manuell geschrieben, sondern
aus Annotationen und Modulkonfigurationen generiert.

### Docs site

Die Dokumentation unter `docs/` beschreibt die Architektur, die Konzepte und die
empfohlenen Muster. Sie ist eine wichtige Quelle, um das Projekt und seine
Designentscheidungen zu verstehen.

### Starter blueprint

`blueprints/starter` ist eine eigenstaendige Vorlage fuer neue Apps. Sie zeigt,
wie man mit App Platform eine KMP-App aufsetzt und die wichtigsten Muster direkt
verwendet.

## Platform Entrypoints

Oben mittig liegt der Bereich **Platform Entrypoints**. Das sind die konkreten
Startpunkte der Anwendung pro Plattform.

- **Android** startet ueber `AndroidApplication` und `MainActivity`.
- **iOS** startet ueber `MainViewController` und einen Xcode Wrapper.
- **Desktop** startet ueber `Main.kt` und `DesktopApp`.
- **Wasm Web** startet ueber `wasmJsMain` und den Browser-Build.

Diese Einstiegspunkte enthalten moeglichst wenig Business-Logik. Ihre Aufgabe
ist vor allem, die Plattform zu initialisieren, den Root Scope aufzubauen, den
AppGraph zu erstellen und das Rendering zu starten.

## Application Assembly

Darunter befindet sich **Application Assembly**. In dieser Schicht wird die App
zusammengesetzt.

### :app modules

Beispiele sind `:sample:app`, `:recipes:app` oder das Starter-`:app`. Diese
Module sind die finalen Anwendungen. Sie duerfen konkrete Implementierungen
importieren und verbinden dadurch die einzelnen Features mit der Plattform.

### Platform AppGraph

Der AppGraph ist der Dependency-Injection-Graph der Anwendung. Er wird
plattformspezifisch erzeugt, weil jede Plattform eigene Objekte bereitstellen
kann, zum Beispiel Android `Application`, iOS `UIApplication` oder
Desktop-spezifische Services.

### Root Scope

Der Root Scope ist die zentrale Lebenszyklusgrenze der App. Er haelt Services,
DI-Graphen und CoroutineScopes. Wenn die App startet, wird dieser Scope
aufgebaut. Wenn die App endet, kann er sauber abgebaut werden.

Der AppGraph wird im Root Scope registriert. Danach koennen andere Komponenten
ueber den Scope auf den passenden Graphen zugreifen.

## Feature Modules

Der Bereich **Feature Modules** beschreibt die Modulstruktur fuer Feature-Code.
Diese Trennung ist eine der zentralen Architekturregeln des Projekts.

### :public modules

`:public`-Module enthalten stabile APIs. Dazu gehoeren Interfaces, Models und
Contracts. Andere Module sollen gegen diese APIs programmieren und nicht gegen
konkrete Implementierungen.

### :impl modules

`:impl`-Module enthalten konkrete Implementierungen. Dazu gehoeren Presenter,
Renderer, Provider und Feature-Logik. Diese Module implementieren die APIs aus
den `:public`-Modulen.

### :testing modules

`:testing`-Module enthalten Fakes und Testhilfen fuer die oeffentlichen APIs.
So koennen Tests mit austauschbaren Implementierungen arbeiten, ohne echte
Produktionskomponenten starten zu muessen.

### :*-robots modules

`:*-robots`-Module enthalten UI-Robots. Robots kapseln wiederverwendbare
Testaktionen und machen UI-Tests lesbarer und wartbarer.

## Framework Core

Unten mittig liegt der Bereich **Framework Core**. Er enthaelt die zentralen
Bausteine, aus denen App-Platform-Anwendungen bestehen.

### Scope

Ein Scope definiert eine Lebenszyklusgrenze. Komponenten innerhalb eines Scopes
leben gemeinsam und werden gemeinsam beendet. Scopes koennen Services und
CoroutineScopes halten.

### DI

Dependency Injection wird ueber Metro oder alternativ kotlin-inject realisiert.
Metro ist im Projekt die empfohlene Variante. Der DI-Graph setzt die konkreten
Implementierungen aus den Feature-Modulen zusammen.

### Presenter

Presenter erzeugen den Zustand der App. In diesem Projekt werden haeufig
`MoleculePresenter` verwendet. Sie berechnen Models, die von der UI beobachtet
werden koennen.

Presenter enthalten Business-Logik, Navigation und Zustandsuebergaenge. Sie
sollen selbst nicht direkt von UI-Technologie abhaengen.

### Template

Templates verpacken den Root-Model-Tree. Man kann sie als app-spezifische
Huelle um die eigentlichen Feature-Models verstehen.

### RendererFactory

Die RendererFactory findet fuer ein Model den passenden Renderer. Sie ist damit
die Bruecke zwischen abstraktem App-Zustand und konkreter UI-Darstellung.

### Renderers

Renderer konsumieren Models und stellen sie auf dem Bildschirm dar. Unterstuetzt
werden vor allem Compose Multiplatform und Android Views Interop.

Ein Renderer sendet Nutzeraktionen nicht direkt an globale Services, sondern
ueblicherweise ueber Event-Lambdas im Model zurueck zum Presenter.

### Robot & Testing

Dieser Bereich umfasst Testhilfen, Fakes, Robots und Patterns fuer UI- und
Presenter-Tests. Die Architektur ist so aufgebaut, dass Business-Logik und UI
moeglichst isoliert testbar bleiben.

## Runtime Flow

Rechts im Diagramm steht **Runtime Flow**. Dieser Bereich beschreibt den Ablauf,
wenn die App tatsaechlich laeuft.

### 1. Platform bootstraps app

Android, iOS, Desktop oder Web startet die App ueber den jeweiligen
Platform-Entrypoint.

### 2. Create Root Scope

Die App erzeugt den Root Scope. Dieser Scope ist der zentrale Ort fuer
Lebenszyklus, Services und den DI-Graphen.

### 3. Assemble DI graph

Der plattformspezifische DI-Graph wird aufgebaut. Er kennt alle Feature-Beitraege
und Plattformobjekte, die fuer diese App relevant sind.

### 4. Presenters compute immutable models

Presenter berechnen den aktuellen Zustand als immutable Models. Diese Models
beschreiben, was angezeigt werden soll und welche Events moeglich sind.

### 5. Template wraps root model tree

Das Root-Model wird in ein Template eingebettet. Dadurch kann die App eine
einheitliche Struktur fuer den obersten Model-Baum bereitstellen.

### 6. RendererFactory selects renderer

Die RendererFactory sucht anhand des Model-Typs den passenden Renderer.

### 7. Platform UI renders model

Der Renderer stellt das Model auf der jeweiligen Plattform dar, zum Beispiel mit
Compose Multiplatform oder Android Views.

### 8. Events travel back through model lambdas

Nutzeraktionen gehen ueber Event-Lambdas im Model zurueck zum Presenter. Der
Presenter verarbeitet das Event und erzeugt danach ein neues Model.

## Wichtigste Aussage des Diagramms

Die Architektur trennt bewusst **was die App tut** von **wie sie auf einer
Plattform aussieht**.

Presenter wissen nicht, ob sie auf Android, iOS, Desktop oder Web laufen. Sie
erzeugen nur Models. Renderer wissen, wie diese Models dargestellt werden. Der
DI-Graph und der Scope sorgen dafuer, dass zur richtigen Zeit die richtigen
Implementierungen verfuegbar sind.

Das macht die App Platform besonders nuetzlich fuer Kotlin Multiplatform:
Gemeinsame Logik bleibt wiederverwendbar, waehrend jede Plattform trotzdem
eigene UI- und Plattformdetails haben kann.
