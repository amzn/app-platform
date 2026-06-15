# Amazon App Platform - Erklaerung des Presenter-/Renderer-Dataflow-Diagramms

Dieses Dokument erklaert das Draw.io-Diagramm
[`app-platform-presenter-renderer-dataflow.drawio`](../Diagrams/app-platform-presenter-renderer-dataflow.drawio).
Das Diagramm zeigt, wie Zustand und Events zwischen Presenter, Model, Renderer
und UI fliessen.

## Zweck des Diagramms

Das Diagramm beantwortet diese Frage:

> Wie trennt die App Platform Business-Logik von UI-Darstellung?

Die Antwort ist ein unidirektionaler Datenfluss:

1. Presenter berechnen Models.
2. Renderer zeigen Models an.
3. User-Aktionen werden als Events ueber Model-Lambdas zurueckgegeben.
4. Presenter verarbeiten Events und erzeugen neue Models.

## Die zwei Hauptbereiche

Das Diagramm trennt zwei Welten:

| Bereich | Aufgabe |
| --- | --- |
| Shared business logic | Presenter, StateFlow und Models. |
| Platform UI layer | RendererFactory, Renderer, sichtbare UI und User-Aktionen. |

Diese Trennung macht moeglich, dass Business-Logik gemeinsam genutzt werden
kann, waehrend jede Plattform ihre eigene UI-Darstellung haben darf.

## Presenter API

Die Presenter API liegt typischerweise in einem `:public`-Modul.

Sie beschreibt:

- welche Inputs ein Presenter erwartet
- welches Model er erzeugt
- welche Events moeglich sind
- welche Zustaende ein Screen oder Feature haben kann

Beispielhaft:

```kotlin
interface LoginPresenter : MoleculePresenter<Unit, LoginPresenter.Model> {
  sealed interface Event {
    data object Logout : Event
    data class ChangeName(val newName: String) : Event
  }

  sealed interface Model : BaseModel {
    data object LoggedOut : Model
    data class LoggedIn(
      val user: User,
      val onEvent: (Event) -> Unit,
    ) : Model
  }
}
```

Wichtig ist: Die API sagt, was moeglich ist, aber nicht, wie es konkret
umgesetzt wird.

## PresenterImpl

Die konkrete Presenter-Implementierung liegt in einem `:impl`-Modul.

Sie enthaelt:

- Business-Logik
- Zustandsuebergaenge
- Navigation
- Event-Verarbeitung
- Aufrufe zu Repositories oder Services

In der App Platform werden Presenter haeufig mit Molecule gebaut. Dadurch kann
die Compose Runtime genutzt werden, um Zustand und Model-Baeume zu berechnen,
ohne direkt Compose UI verwenden zu muessen.

## StateFlow

Ein Presenter stellt Zustand als Stream bereit.

`StateFlow` bedeutet:

- Es gibt immer einen aktuellen Wert.
- Neue Werte koennen beobachtet werden.
- UI oder Runtime koennen auf neue Models reagieren.

Das passt gut zum Model-Ansatz, weil jeder Zustand als neues immutable Model
ausgegeben werden kann.

## Model

Das Model beschreibt, was die UI anzeigen soll.

Ein Model kann enthalten:

- Text
- Listen
- Ladezustand
- Fehlerzustand
- Child Models
- Event-Lambdas

Ein Model sollte keine UI-Framework-Objekte enthalten. Es beschreibt den Zustand
abstrakt, nicht die konkrete Darstellung.

## RendererFactory

Die RendererFactory ist die Bruecke zwischen Model und Renderer.

Sie bekommt ein Model und fragt:

> Welcher Renderer kann diesen Model-Typ darstellen?

Renderer werden per DI und Codegenerierung registriert. Dadurch muss die
RendererFactory nicht manuell fuer jeden Screen angepasst werden.

## Renderer

Der Renderer uebersetzt ein Model in UI.

Beispiele:

- `ComposeRenderer`
- `ViewRenderer`
- Android-spezifische Renderer
- Compose-Multiplatform-Renderer

Der Renderer sollte moeglichst keine Business-Logik enthalten. Er liest das
Model und zeigt es an.

## Visible UI

Die sichtbare UI ist das, was der User wirklich sieht:

- Buttons
- Texte
- Listen
- Eingabefelder
- Ladeindikatoren
- Fehlermeldungen

Diese UI kann auf jeder Plattform anders aussehen, solange sie dasselbe Model
korrekt interpretiert.

## User action

Wenn der User klickt, tippt oder eine Geste ausfuehrt, entsteht ein Event.

Der Renderer ruft dann eine Event-Lambda aus dem Model auf:

```kotlin
model.onEvent(LoginPresenter.Event.ChangeName("New name"))
```

Dadurch geht die Aktion zurueck zum Presenter.

## Warum Events im Model liegen

Events ueber Model-Lambdas haben einen wichtigen Vorteil:

> Der Renderer muss nicht wissen, wie Business-Logik funktioniert.

Der Renderer weiss nur:

- welches Model er hat
- welche Event-Lambda er aufrufen darf
- welcher Event-Typ zur Aktion passt

Der Presenter entscheidet, was danach passiert.

## Der unidirektionale Kreis

Der Ablauf sieht so aus:

1. Presenter berechnet Model.
2. Model wird beobachtet.
3. RendererFactory sucht Renderer.
4. Renderer zeigt UI.
5. User interagiert.
6. Renderer ruft `model.onEvent(event)` auf.
7. Presenter verarbeitet Event.
8. Presenter berechnet neues Model.

Dieser Kreis ist kontrolliert und testbar.

## Testbarkeit

Diese Architektur macht Tests einfacher:

| Teil | Wie testbar? |
| --- | --- |
| Presenter | Mit Fakes und Flow/Turbine-Tests, ohne echte UI. |
| Model | Als reine Datenstruktur vergleichbar. |
| Renderer | Mit vorgegebenen Models isoliert testbar. |
| Events | Durch Aufruf der Event-Lambda pruefbar. |

## Kernaussage

Das Presenter-/Renderer-Dataflow-Diagramm zeigt:

> Presenter entscheiden, was der Zustand ist. Renderer entscheiden, wie dieser
> Zustand aussieht. Events laufen ueber Models zurueck.

Diese Trennung ist einer der wichtigsten Gruende, warum die App Platform fuer
Kotlin Multiplatform geeignet ist.
