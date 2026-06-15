# Amazon App Platform - Erklaerung des DI-/Scope-Diagramms

Dieses Dokument erklaert das Draw.io-Diagramm
[`app-platform-di-scope.drawio`](../Diagrams/app-platform-di-scope.drawio).
Das Diagramm zeigt, wie **Dependency Injection**, **RootScope**,
**plattformabhaengige Werte**, **AppGraph** und **generierte Bindings** in der
Amazon App Platform zusammenarbeiten.

## Zweck des Diagramms

Das DI-/Scope-Diagramm beantwortet im Kern diese Frage:

> Wie bekommt gemeinsamer Kotlin-Multiplatform-Code die richtigen konkreten
> Implementierungen, ohne direkt von Android, iOS, Desktop oder Wasm abzuhaengen?

Die Antwort besteht aus drei grossen Teilen:

1. Plattformen liefern ihre spezifischen Werte.
2. Der AppGraph setzt daraus und aus den Feature-Beitraegen den Objektgraphen
   zusammen.
3. Der RootScope haelt diesen Graphen als langlebigen Service fuer die App.

So kann gemeinsamer Code gegen Interfaces aus `:public`-Modulen arbeiten,
waehrend die konkrete Implementierung zur Laufzeit ueber den DI-Graphen
bereitgestellt wird.

## Die drei Hauptbereiche

Das Diagramm ist in drei grosse Bereiche aufgeteilt:

| Bereich | Bedeutung |
| --- | --- |
| Platform source set | Plattformcode, der Android-, iOS-, Desktop- oder Wasm-spezifische Werte bereitstellt. |
| Runtime scope boundary | Der RootScope als Lebenszyklusgrenze der App. |
| Dependency graph | Der DI-Graph, der APIs, Implementierungen und generierte Bindings verbindet. |

Diese Trennung ist wichtig, weil Kotlin-Multiplatform-Code nicht direkt von
Plattformdetails abhaengen soll. Android-spezifische Objekte wie `Application`
oder iOS-spezifische Objekte wie `UIApplication` bleiben am Rand des Systems.

## Platform source set

Links im Diagramm befindet sich der Bereich **Platform source set**.

Ein Source Set ist in Kotlin Multiplatform eine Quellcodegruppe fuer eine
bestimmte Plattform oder fuer gemeinsamen Code. Beispiele sind:

- `androidMain`
- `iosMain`
- `desktopMain`
- `wasmJsMain`
- `commonMain`

Im DI-/Scope-Diagramm geht es vor allem um die plattformspezifischen Source
Sets. Dort werden Werte bereitgestellt, die gemeinsamer Code nicht selbst kennen
sollte.

### Android platform values

Android kann zum Beispiel diese Werte in den Graphen geben:

- `Application`
- `Activity`
- Android Services
- Android-spezifische Implementierungen

Diese Objekte sind nur auf Android verfuegbar. Deshalb sollten sie nicht in
`commonMain` auftauchen.

### iOS platform values

iOS kann eigene Werte liefern, zum Beispiel:

- `UIApplication`
- `UIViewController`
- iOS-spezifische Services
- native iOS-Kontextobjekte

Auch diese Werte gehoeren an die Plattformgrenze und werden ueber DI in das
System eingebracht.

### Desktop / Wasm values

Desktop und Wasm koennen ebenfalls eigene Werte bereitstellen:

- Fenster- oder Browserkontext
- plattformspezifische Services
- `RootScopeProvider`
- Web- oder Desktop-spezifische Implementierungen

Der gemeinsame Code muss dabei nur die abstrakten APIs kennen.

## AppGraph.Factory

Im unteren Teil des linken Bereichs steht **AppGraph.Factory**.

Diese Factory erzeugt den plattformspezifischen AppGraph. In Metro sieht das
konzeptionell so aus:

```kotlin
@DependencyGraph(AppScope::class)
interface AndroidAppGraph {
  @DependencyGraph.Factory
  fun interface Factory {
    fun create(
      @Provides application: Application,
      @Provides rootScopeProvider: RootScopeProvider,
    ): AndroidAppGraph
  }
}
```

Die wichtigen Punkte:

- Der finale Graph wird in einem plattformspezifischen Source Set definiert.
- Plattformwerte werden ueber `@Provides` in den Graphen gegeben.
- Der Graph kennt dadurch sowohl gemeinsamen Code als auch Plattformobjekte.
- Gemeinsame Komponenten muessen trotzdem nicht direkt von Plattformklassen
  abhaengen.

## Runtime scope boundary

In der Mitte des Diagramms befindet sich **Runtime scope boundary**. Dieser
Bereich beschreibt den RootScope und seine Services.

### RootScope

Der **RootScope** ist die zentrale Lebenszyklusgrenze der App.

Er wird beim Start der App erzeugt und lebt normalerweise so lange wie die App
selbst. Wenn die App beendet wird, kann auch der RootScope abgebaut werden.

Der RootScope ist wichtig, weil er kontrolliert, welche Objekte gemeinsam leben
und gemeinsam aufgeraeumt werden.

Typische Aufgaben des RootScope:

- appweite Services halten
- den DI-Graphen verfuegbar machen
- CoroutineScopes verwalten
- Lifecycle-Callbacks ausloesen
- Ressourcen am Ende der App-Lebenszeit freigeben

### Scope services

Ein Scope kann Services enthalten. Im Diagramm sind Beispiele genannt:

- AppGraph service
- CoroutineScope
- Lifecycle callbacks

Der AppGraph wird als Service im Scope registriert. Dadurch koennen spaeter
andere Komponenten ueber den Scope auf den Graphen zugreifen.

Das ist besonders hilfreich fuer Stellen, an denen Constructor Injection nicht
direkt moeglich ist, zum Beispiel bei plattformverwalteten Objekten wie Android
`ViewModel`, `Activity` oder aehnlichen Einstiegspunkten.

### Graph lookup

**Graph lookup** beschreibt den Zugriff auf den registrierten DI-Graphen.

In Metro kann das zum Beispiel ueber eine Scope-Erweiterung wie diese passieren:

```kotlin
scope.metroDependencyGraph<AppGraph>()
```

Das bedeutet:

1. Eine Komponente hat Zugriff auf den Scope.
2. Sie fragt den passenden Dependency Graph ab.
3. Aus dem Graphen kann sie benoetigte Abhaengigkeiten beziehen.

Dieser Weg ist nicht der Normalfall fuer jede Klasse. Im Idealfall werden
Abhaengigkeiten per Constructor Injection uebergeben. Der Lookup ist aber
nuetzlich fuer Objekte, die von der Plattform erstellt werden und deren
Konstruktor man nicht frei kontrolliert.

## Dependency graph

Rechts im Diagramm befindet sich der Bereich **Dependency graph**.

Dieser Bereich zeigt, wie APIs, Implementierungen und generierte Bindings im
DI-System zusammenkommen.

### Platform AppGraph

Der **Platform AppGraph** ist der finale DI-Graph einer App fuer eine konkrete
Plattform.

Er weiss:

- welche Plattformwerte bereitgestellt wurden
- welche Feature-Implementierungen verfuegbar sind
- welche APIs durch welche Implementierungen erfuellt werden
- welche Renderer fuer welche Models existieren
- welche Root-Komponenten die App braucht

Wichtig ist: Der AppGraph lebt nicht irgendwo in einem beliebigen Feature,
sondern in der finalen App-Schicht. Das passt zur Modulregel der App Platform:
Nur `:app`-Module duerfen konkrete `:impl`-Module direkt zusammensetzen.

### Generated bindings

**Generated bindings** sind automatisch erzeugte DI-Verknuepfungen.

Sie entstehen durch Annotationen wie:

- `@ContributesBinding`
- `@ContributesTo`
- `@ContributesRenderer`

Diese Annotationen sagen dem Build-System sinngemaess:

- Diese Klasse implementiert eine bestimmte API.
- Diese Klasse soll zu einem bestimmten Scope beitragen.
- Dieser Renderer kann ein bestimmtes Model rendern.

Der Vorteil: Der AppGraph muss nicht jede einzelne Implementierung manuell
verdrahten. Stattdessen werden die passenden Bindings zur Build-Zeit erzeugt.

### Public APIs

**Public APIs** sind die stabilen Schnittstellen aus `:public`-Modulen.

Beispiele:

- Presenter-Interfaces
- Repository-Interfaces
- Model-Contracts
- Provider-Interfaces

Andere Module sollen gegen diese APIs programmieren, nicht gegen konkrete
Implementierungen.

Das ist ein Kernpunkt von Dependency Inversion:

> Konsumenten haengen von Abstraktionen ab, nicht von konkreten Klassen.

### Implementations

**Implementations** sind konkrete Klassen aus `:impl`-Modulen.

Beispiele:

- `LoginPresenterImpl`
- `UserPagePresenterImpl`
- `LoginRenderer`
- `UserPageListRenderer`
- Plattformprovider
- Repository-Implementierungen

Diese Klassen implementieren Public APIs oder tragen Renderer/Bindings zum DI
Graphen bei.

Die Implementierungen sind absichtlich getrennt von den Public APIs, damit
andere Module nicht unkontrolliert konkrete Details importieren.

### Consumers

**Consumers** sind Komponenten, die Abhaengigkeiten aus dem Graphen verwenden.

Im Diagramm werden Beispiele genannt:

- `TemplateProvider`
- Presenter-Baum
- `RendererFactory`
- ViewModel-Fallback-Zugriff

Consumers bekommen ihre Abhaengigkeiten idealerweise per Constructor Injection.
Falls das nicht moeglich ist, koennen sie ueber den Scope den Graphen abfragen.

## Die Pfeile im Diagramm

Die Pfeile zeigen, wie die Teile zusammenarbeiten.

### Plattformwerte gehen in die AppGraph.Factory

Android, iOS, Desktop oder Wasm liefern plattformspezifische Werte an die
`AppGraph.Factory`.

Das ist im Diagramm mit `@Provides` beschriftet.

Diese Werte sind explizite Eingaben in den Graphen. Dadurch kann gemeinsamer
Code spaeter indirekt auf Plattformfaehigkeiten zugreifen, ohne direkt
Plattformklassen zu importieren.

### AppGraph.Factory erzeugt Platform AppGraph

Die Factory erstellt den konkreten AppGraph fuer die Plattform.

Dieser Graph ist der zentrale Ort, an dem sich Folgendes trifft:

- Plattformwerte
- Public APIs
- Implementierungen
- generierte Bindings
- appweite Root-Abhaengigkeiten

### AppGraph wird im RootScope registriert

Der fertige AppGraph wird als Service im RootScope registriert.

Dadurch hat die App eine klare Lebenszyklusregel:

- Solange der RootScope lebt, ist der AppGraph verfuegbar.
- Wenn der RootScope beendet wird, endet auch die Lebensdauer der appweiten
  Abhaengigkeiten.

### Implementierungen erzeugen Bindings

Implementierungen tragen ueber Annotationen Bindings zum Graphen bei.

Zum Beispiel:

```kotlin
@ContributesBinding(AppScope::class)
class LoginPresenterImpl : LoginPresenter
```

Oder bei Renderern:

```kotlin
@ContributesRenderer
class LoginRenderer : ComposeRenderer<LoginPresenter.Model>()
```

Die Annotationen fuehren dazu, dass zur Build-Zeit Glue-Code erzeugt wird.

### Implementierungen implementieren Public APIs

Der Pfeil von **Implementations** zu **Public APIs** bedeutet:

> Konkrete Klassen erfuellen die stabilen Schnittstellen.

Zum Beispiel:

- `LoginPresenterImpl` implementiert `LoginPresenter`
- `FakeUserRepository` kann `UserRepository` implementieren
- ein Plattformprovider implementiert eine gemeinsame Provider-API

### AppGraph stellt API-Typen bereit

Der AppGraph stellt Konsumenten normalerweise API-Typen bereit, nicht konkrete
Implementierungstypen.

Ein Consumer fragt also idealerweise nach:

```kotlin
LoginPresenter
```

nicht nach:

```kotlin
LoginPresenterImpl
```

Das haelt den Consumer entkoppelt.

### Consumers werden injiziert oder holen den Graphen ueber Scope

Der Normalfall ist Constructor Injection:

```kotlin
class SomePresenter(
  private val userRepository: UserRepository,
)
```

Der Spezialfall ist Graph Lookup ueber Scope, wenn die Plattform das Objekt
erzeugt und Constructor Injection nicht direkt passt.

## Warum diese Architektur wichtig ist

Diese Architektur loest mehrere typische Probleme in Kotlin-Multiplatform-Apps.

### Plattformdetails bleiben am Rand

Android-, iOS-, Desktop- und Wasm-spezifische Objekte werden im jeweiligen
Source Set bereitgestellt. Gemeinsamer Code muss sie nicht importieren.

Das macht den gemeinsamen Code portabler.

### Business-Logik bleibt testbar

Presenter, Repositories und andere Konsumenten koennen gegen Interfaces
programmiert werden. In Tests lassen sich echte Implementierungen durch Fakes
ersetzen.

### Der Objektgraph wird zur Build-Zeit geprueft

Metro und kotlin-inject sind Compile-time-DI-Frameworks. Das bedeutet:

- fehlende Bindings werden frueh erkannt
- falsche Graphen fuehren zu Build-Fehlern
- viele Runtime-Crashes durch fehlende Dependencies werden vermieden

### Lebenszyklen sind explizit

Der RootScope macht klar, welche Objekte appweit leben. Dadurch ist das System
besser kontrollierbar als ein frei verteilter globaler Zustand.

### Module bleiben sauber getrennt

Der AppGraph lebt in der finalen App-Schicht und darf Implementierungen
zusammenziehen. Wiederverwendbare Feature-Module koennen bei Public APIs
bleiben.

Das passt zur Modulstruktur:

- `:public` enthaelt APIs
- `:impl` enthaelt Implementierungen
- `:app` setzt konkrete Implementierungen zusammen
- `:testing` liefert Fakes und Testhilfen

## Typischer Ablauf

Ein typischer Ablauf sieht so aus:

1. Android, iOS, Desktop oder Wasm startet die App.
2. Die App erstellt den RootScope.
3. Die Plattform erstellt den passenden AppGraph ueber eine Factory.
4. Plattformwerte werden per `@Provides` in den Graphen gegeben.
5. Generierte Bindings verbinden Public APIs mit Implementierungen.
6. Der fertige AppGraph wird im RootScope registriert.
7. Root-Komponenten wie Presenter, TemplateProvider oder RendererFactory werden
   aus dem Graphen bezogen.
8. Die App kann laufen, ohne dass gemeinsamer Code direkt Plattformdetails
   kennen muss.

## Beispielhafte mentale Zusammenfassung

Man kann sich das System so merken:

| Teil | Aufgabe |
| --- | --- |
| Platform source set | Liefert konkrete Plattformwerte. |
| AppGraph.Factory | Baut den DI-Graphen fuer eine Plattform. |
| AppGraph | Kennt die konkreten Bindings der finalen App. |
| RootScope | Haelt den Graphen und bestimmt seine Lebensdauer. |
| Public APIs | Beschreiben, was gebraucht wird. |
| Implementations | Beschreiben, wie es konkret gemacht wird. |
| Generated bindings | Verbinden APIs und Implementierungen automatisch. |
| Consumers | Verwenden APIs, ohne konkrete Implementierungen kennen zu muessen. |

## Kernaussage

Das DI-/Scope-Diagramm zeigt, wie die App Platform zwei Dinge gleichzeitig
erreicht:

1. **Gemeinsamer Code bleibt sauber und plattformunabhaengig.**
2. **Die finale App kann trotzdem konkrete Plattform- und Feature-
   Implementierungen verwenden.**

Der RootScope gibt dem Ganzen eine klare Lebensdauer. Der AppGraph setzt die
Objekte zusammen. Generierte Bindings reduzieren manuelle Verdrahtung. Public
APIs schuetzen den gemeinsamen Code vor zu starker Kopplung an konkrete
Implementierungen.
