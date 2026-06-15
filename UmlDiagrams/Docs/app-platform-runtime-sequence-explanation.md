# Amazon App Platform - Erklaerung des Runtime-Sequence-Diagramms

Dieses Dokument erklaert das Draw.io-Diagramm
[`app-platform-runtime-sequence.drawio`](../Diagrams/app-platform-runtime-sequence.drawio).
Das Diagramm zeigt den Laufzeitablauf einer App-Platform-Anwendung: vom
Plattformstart bis zur sichtbaren UI und wieder zurueck zum Presenter, wenn der
User interagiert.

## Zweck des Diagramms

Das Diagramm beantwortet diese Frage:

> Was passiert konkret, wenn eine App-Platform-App gestartet wird?

Die App Platform trennt Plattformstart, Lebenszyklus, Dependency Injection,
Business-Logik und UI-Rendering. Das Runtime-Sequence-Diagramm zeigt diese
Trennung als Schrittfolge.

## Leserichtung

Die Schritte sind bewusst als grosse Karten dargestellt:

1. Obere Reihe von links nach rechts.
2. Dann nach unten.
3. Untere Reihe von rechts nach links.
4. Der gestrichelte Pfeil zeigt den Event-Rueckweg.

Die Darstellung ist kein klassisches enges UML-Sequenzdiagramm mehr, sondern ein
lesbarer Ablaufplan. Das ist fuer dieses Thema hilfreicher, weil die einzelnen
Architekturrollen wichtiger sind als Millisekunden-genaue Methodenaufrufe.

## Schritt 1: Platform Entrypoint

Die App startet immer auf einer konkreten Plattform:

- Android
- iOS
- Desktop
- Web/Wasm

Jede Plattform hat eigene Einstiegspunkte:

| Plattform | Typische Einstiegspunkte |
| --- | --- |
| Android | `AndroidApplication`, `MainActivity` |
| iOS | `MainViewController`, Xcode Wrapper |
| Desktop | `Main.kt`, `DesktopApp` |
| Wasm | `wasmJsMain`, Browser-Bootstrap |

Diese Einstiegspunkte sollen moeglichst wenig Business-Logik enthalten. Ihre
Aufgabe ist vor allem, die gemeinsame App-Platform-Laufzeit zu starten.

## Schritt 2: Root Scope erzeugen

Nach dem Plattformstart wird der **RootScope** erzeugt.

Der RootScope ist die zentrale Lebenszyklusgrenze der App. Er beschreibt:

- welche appweiten Objekte gemeinsam leben
- wann diese Objekte aufgeraeumt werden
- welche Services zur Laufzeit verfuegbar sind
- welche Coroutines an die App-Lebensdauer gebunden sind

Ohne einen klaren RootScope wuerde die App leichter in globalen Zustand,
unklare Lebenszyklen oder Speicherlecks abrutschen.

## Schritt 3: Platform AppGraph bauen

Danach wird der **Platform AppGraph** erstellt.

Der AppGraph ist der Dependency-Injection-Graph der finalen App. Er verbindet:

- plattformspezifische Werte
- Public APIs
- konkrete Implementierungen
- generierte Bindings
- Root-Komponenten wie Presenter, TemplateProvider oder RendererFactory

Der Graph ist plattformspezifisch, weil jede Plattform andere Objekte
bereitstellen kann. Android liefert zum Beispiel eine `Application`, iOS kann
eine `UIApplication` liefern, Desktop und Wasm haben wiederum eigene
Startkontexte.

## Schritt 4: Root Presenter aufloesen

Sobald der Graph existiert, kann die App Root-Abhaengigkeiten aus dem Graphen
beziehen.

Dazu gehoeren typischerweise:

- Root Presenter
- TemplateProvider
- RendererFactory
- Services fuer Navigation oder App-Zustand
- Feature-Presenter

Die App arbeitet dabei idealerweise gegen APIs aus `:public`-Modulen. Welche
konkrete Implementierung genutzt wird, entscheidet der DI-Graph.

## Schritt 5: Model berechnen

Der Presenter berechnet den aktuellen Zustand der App.

In der App Platform werden Presenter haeufig als `MoleculePresenter`
implementiert. Das bedeutet:

- Business-Logik wird in einer `@Composable` `present(...)`-Funktion berechnet.
- Das Ergebnis ist ein Model.
- Das Model beschreibt, was die UI anzeigen soll.
- Das Model kann Event-Lambdas enthalten, ueber die die UI Aktionen zurueck
  melden kann.

Ein Model sollte moeglichst immutable sein. Wenn sich Zustand aendert, wird ein
neues Model erzeugt.

## Schritt 6: Template verpackt Root Model

Das **Template** legt die aeussere Struktur um den Root-Model-Baum.

Man kann das Template als App-Shell verstehen. Es kann zum Beispiel festlegen:

- welche Root-Navigation aktiv ist
- wie Screens in eine gemeinsame Struktur eingebettet werden
- welche globalen UI-Bereiche existieren
- wie der oberste Model-Baum aussieht

Templates sind app-spezifisch. Sie verbinden den allgemeinen Presenter-Zustand
mit der konkreten App-Struktur.

## Schritt 7: Renderer auswaehlen

Die **RendererFactory** bekommt ein Model und sucht den passenden Renderer.

Das passiert anhand des Model-Typs. Ein Model wie `LoginPresenter.Model` kann
zum Beispiel einen `LoginRenderer` bekommen.

Renderer werden in der App Platform typischerweise ueber Annotationen
registriert:

```kotlin
@ContributesRenderer
class LoginRenderer : ComposeRenderer<LoginPresenter.Model>()
```

Der passende Binding-Code wird zur Build-Zeit erzeugt. Zur Laufzeit kann die
RendererFactory dadurch den passenden Renderer finden.

## Schritt 8: UI rendern und Events senden

Der Renderer stellt das Model auf der Plattform dar.

Moegliche Renderer-Arten:

- `ComposeRenderer` fuer Compose Multiplatform
- `ViewRenderer` fuer Android Views
- Android-spezifische Compose/View-Interop-Renderer

Der Renderer sollte keine Business-Logik enthalten. Er liest das Model und zeigt
es an.

Wenn der User interagiert, ruft der Renderer eine Event-Lambda aus dem Model
auf:

```kotlin
model.onEvent(LoginPresenter.Event.Logout)
```

Dadurch geht die Aktion kontrolliert zurueck zum Presenter.

## Der Event-Rueckweg

Der gestrichelte Pfeil im Diagramm zeigt den wichtigsten Rueckkanal:

1. User klickt, tippt oder fuehrt eine Geste aus.
2. Der Renderer reagiert auf die UI-Aktion.
3. Der Renderer ruft `model.onEvent(event)` auf.
4. Der Presenter verarbeitet das Event.
5. Der Presenter erzeugt ein neues Model.
6. Die UI rendert erneut.

Dieser Rueckweg verhindert, dass UI-Code beliebig globale Services veraendert.
Die Zustandsaenderung laeuft ueber den Presenter.

## Warum das wichtig ist

Der Ablauf schuetzt drei Grenzen:

| Grenze | Nutzen |
| --- | --- |
| Plattformstart vs. Business-Logik | Einstiegspunkte bleiben duenn und austauschbar. |
| Presenter vs. Renderer | Logik und Darstellung bleiben getrennt. |
| Model vs. Event | Zustand und Aktionen sind explizit modelliert. |

Dadurch wird die App besser testbar. Presenter koennen ohne echte UI getestet
werden. Renderer koennen isoliert gegen Models getestet werden. Plattformcode
bleibt auf Bootstrap und Integration fokussiert.

## Kernaussage

Die Runtime Sequence zeigt:

> Plattformcode startet die App, Scopes und DI setzen die Laufzeit zusammen,
> Presenter erzeugen Models, Renderer zeigen Models an, und Events laufen
> kontrolliert zurueck zum Presenter.

Das ist der Kern der App-Platform-Laufzeitarchitektur.
