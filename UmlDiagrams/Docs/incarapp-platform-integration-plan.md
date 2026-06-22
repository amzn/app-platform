# inCarApp - Integrationsplan fuer eine App-Platform-Architektur

Dieses Dokument beschreibt, wie die beispielhafte `inCarApp` in eine
App-Platform-Architektur ueberfuehrt werden kann.

Ziel ist nicht, die vorhandene App 1:1 zu migrieren. Die App dient als internes
Beispiel, um eine Plattform fuer folgende Zieloberflaechen zu entwerfen:

- Android Automotive
- Android Phone
- iOS

Die UI soll moeglichst stark ueber Compose Multiplatform geteilt werden. Die
Mercedes-Benz Widgets sollen weiterverwendet werden und werden perspektivisch
ebenfalls Multiplatform-faehig sein.

## Aktueller Stand der Beispiel-App

Die hochgeladene `inCarApp` ist aktuell eine Android-App mit Automotive-Bezug.

Wichtige Eigenschaften:

| Bereich | Aktueller Stand |
| --- | --- |
| Plattform | Android / Android Automotive |
| UI | Jetpack Compose mit Mercedes-Benz Widgets |
| DI | Hilt |
| App-Entrypoints | `MainApplication`, `MainActivity` |
| Car-Zugriff | Android `android.car.Car`, `CarPropertyManager`, `VehiclePropertyIds` |
| Domain | `CarInfo`, `Result`, `CarInfoRepository`, Use Cases |
| State | Android `ViewModel` + `StateFlow` / `LiveData` |
| Tests | Unit Tests, Android Tests, Screenshot Tests mit Roborazzi/MBTestKit |
| Build | Standalone Android Gradle Projekt mit eigenem Version Catalog |

Die App ist damit ein guter Vertical Slice: Sie zeigt Datenzugriff auf
Fahrzeuginformationen, Domain-Logik, UI-State, Compose UI, DI und Tests.

## Wichtige Build-Beobachtungen

Die `inCarApp` bringt eigene Build-Dateien mit:

- `inCarApp/settings.gradle.kts`
- `inCarApp/build.gradle.kts`
- `inCarApp/gradle.properties`
- `inCarApp/libs.versions.toml`

Dabei fallen ein paar Punkte auf:

| Punkt | Bedeutung fuer Integration |
| --- | --- |
| `settings.gradle.kts` inkludiert `:app` | Die hochgeladene Struktur wirkt teilweise standalone/flattened. Fuer direkte Integration muss die Modulstruktur angepasst werden. |
| `minSdk = 34` | Das passt zu Automotive, ist aber fuer Android Phone eventuell zu hoch. |
| `compileSdk = 36`, `targetSdk = 35` | Muss mit der Zielplattform abgestimmt werden. |
| Kotlin `2.2.20` in `inCarApp` vs. Kotlin `2.4.0` im App-Platform-Checkout | Fuer echte Integration muss eine Version fuehrend sein. |
| AGP `8.10.0` in `inCarApp` vs. AGP `8.13.2` im App-Platform-Checkout | Gradle/AGP-Versionen muessen konsolidiert werden. |
| Hilt wird genutzt | Hilt ist Android-spezifisch und sollte nicht in den shared KMP-Core wandern. |
| MB Artifactory Repositories | Fuer echte Builds braucht CI/Dev-Umgebung Credentials und Repository-Zugriff. |

Fuer eine Architekturplanung reicht der Stand aus. Fuer eine echte
Build-Integration muessten Versionskatalog, Repository-Zugriff und Modulpfade
bereinigt werden.

## Zielarchitektur

Die Zielarchitektur sollte so aussehen:

```text
Shared KMP Layer
  Public APIs
  Domain Models
  Use Cases
  Presenter
  UI Models
  Events
  Capability Interfaces

Platform / Surface Implementations
  Android Automotive Vehicle Providers
  Android Phone Providers
  iOS Providers
  Automotive Renderer
  Phone Renderer
  iOS Renderer

Platform Apps
  app-automotive
  app-android-phone
  app-ios
```

Der wichtigste Grundsatz:

> Gemeinsame Logik fragt nicht: "Bin ich Android Automotive?"
> Gemeinsame Logik fragt: "Welche Capability ist verfuegbar?"

Dadurch kann ein Feature auf verschiedenen Plattformen unterschiedlich viel
koennen, ohne dass der gemeinsame Code plattformspezifische Klassen importiert.

## Empfohlene Modulstruktur

Da die Plattform nicht generisch sein soll, sondern konkret auf `inCarApp`
bezogen ist, sollte die Modulstruktur ebenfalls konkret bleiben.

```text
inCarApp:public
inCarApp:impl
inCarApp:impl-android-automotive
inCarApp:impl-android-phone
inCarApp:impl-ios
inCarApp:renderer-automotive
inCarApp:renderer-android-phone
inCarApp:renderer-ios
inCarApp:testing
inCarApp:impl-robots
inCarApp:app-automotive
inCarApp:app-android-phone
inCarApp:app-ios
```

Diese Struktur trennt:

- stabile APIs
- gemeinsame Implementierungslogik
- plattformspezifische Hardware-/Service-Anbindung
- plattformspezifische Renderer
- finale App-Entrypoints
- Testhilfen

## Modulverantwortung im Detail

### `inCarApp:public`

Dieses Modul enthaelt die stabilen Contracts.

Geeignete Inhalte:

- `VehicleInfo`
- `VehicleInfoProvider`
- `VehicleInfoResult`
- `VehicleCapabilities`
- `InCarPresenter`
- `InCarModel`
- `InCarEvent`
- `InCarSurface`
- `Capability`

Beispiel:

```kotlin
interface VehicleInfoProvider {
    suspend fun getVehicleInfo(): VehicleInfoResult
}

sealed interface VehicleInfoResult {
    data class Available(val info: VehicleInfo) : VehicleInfoResult
    data object NotSupported : VehicleInfoResult
    data object PermissionMissing : VehicleInfoResult
    data class Error(val message: String, val cause: Throwable? = null) : VehicleInfoResult
}

data class VehicleInfo(
    val manufacturer: String?,
    val model: String?,
    val vin: String?,
    val isConnected: Boolean,
)
```

Wichtig: Dieses Modul darf keine Android-Car-Klassen importieren.

### `inCarApp:impl`

Dieses Modul enthaelt gemeinsame Business- und Presenter-Logik.

Geeignete Inhalte:

- `InCarPresenterImpl`
- `GetVehicleInfoUseCase`
- `DisconnectVehicleServiceUseCase`
- Mapping von `VehicleInfoResult` nach `InCarModel`
- Regeln fuer Loading, Error, Unsupported und Permission States

Hier kann sehr viel geteilt werden, auch wenn der konkrete Fahrzeugzugriff
hardwareabhaengig ist.

Beispiel:

```kotlin
class InCarPresenterImpl(
    private val vehicleInfoProvider: VehicleInfoProvider,
) : InCarPresenter {
    // erzeugt InCarModel aus VehicleInfoResult
}
```

### `inCarApp:impl-android-automotive`

Dieses Modul kapselt den echten Automotive-Hardwarezugriff.

Aktuelle Klassen, die hierhin gehoeren:

- `CarInfoProviderImpl`
- Android `Car`
- `CarPropertyManager`
- `VehiclePropertyIds`
- Automotive Permissions

Zielname:

```text
AndroidAutomotiveVehicleInfoProvider
```

Dieses Modul darf Android Automotive APIs importieren. Shared Code darf das
nicht.

### `inCarApp:impl-android-phone`

Dieses Modul definiert, was Android Phone tun kann, wenn keine direkte
Fahrzeughardware verfuegbar ist.

Moegliche Strategien:

- `VehicleInfoResult.NotSupported`
- Remote-/Backend-Daten, falls vorhanden
- Companion-App-Verbindung
- Demo-/Mock-Daten fuer interne Tests
- eingeschraenkter Funktionsumfang

Das ist kein Fehlerfall. Es ist eine Capability-Entscheidung.

### `inCarApp:impl-ios`

Dieses Modul definiert dieselbe Frage fuer iOS.

Moegliche Strategien:

- `VehicleInfoResult.NotSupported`
- Remote-/Backend-Daten
- spaetere Fahrzeug-/Account-Integration
- Demo-Daten fuer interne Entwicklung

iOS sollte nicht versuchen, Android-Car-APIs nachzubilden. Es sollte dieselben
Public APIs implementieren und sauber melden, was verfuegbar ist.

### `inCarApp:renderer-automotive`

Dieses Modul enthaelt den Automotive Renderer.

Da MB Widgets weiterverwendet werden sollen, gehoeren hierhin:

- `MBTheme`
- `MBBackground`
- `MBAppLayoutStatic`
- `MBAppLayoutHeader`
- `MBButton`
- `MBText`
- `MBCheckbox`
- Automotive-spezifische Layoutregeln

Wenn MB Widgets in Zukunft Multiplatform werden, kann ein Teil dieses Renderers
spaeter in ein gemeinsames Compose-Multiplatform-Renderer-Modul wandern.

### `inCarApp:renderer-android-phone`

Dieses Modul enthaelt den Android-Phone-Renderer.

Da ihr moeglichst viel UI sharen wollt, sollte dieser Renderer moeglichst
Compose-Multiplatform-kompatibel geschrieben werden.

Unterschiede zum Automotive Renderer:

- andere Navigation
- andere Screen-Groessen
- andere Interaktionsdichte
- eventuell andere erlaubte Feature-Zustaende

### `inCarApp:renderer-ios`

Dieses Modul enthaelt den iOS-Renderer mit Compose Multiplatform.

Da iOS ebenfalls Compose Multiplatform nutzen soll, kann die UI perspektivisch
staerker geteilt werden.

Empfohlener Ansatz:

```text
shared renderer foundation
  gemeinsame Composables mit MB Widgets

renderer-android-phone
  Android-spezifische Host-/Interop-Anteile

renderer-ios
  iOS-spezifische Host-/Interop-Anteile

renderer-automotive
  Automotive-spezifische Layout-/Safety-Variante
```

### `inCarApp:testing`

Dieses Modul enthaelt Testhilfen.

Geeignete Inhalte:

- `FakeVehicleInfoProvider`
- Fixture-Daten
- Fake Presenter
- Test Models
- Capability-Testmatrix

Beispiel:

```kotlin
class FakeVehicleInfoProvider(
    private var result: VehicleInfoResult,
) : VehicleInfoProvider {
    override suspend fun getVehicleInfo(): VehicleInfoResult = result
}
```

### `inCarApp:impl-robots`

Dieses Modul enthaelt UI-Test-Robots.

Da Automotive, Phone und iOS unterschiedliche Oberflaechen haben koennen, sollte
man zwei Ebenen unterscheiden:

```text
InCarRobotContract
  assertVehicleInfoVisible()
  tapRefresh()
  assertUnsupportedState()

AutomotiveInCarRobot
AndroidPhoneInCarRobot
IosInCarRobot
```

### `inCarApp:app-automotive`

Dieses Modul enthaelt die Automotive-App.

Aktuelle Klassen, die hierhin gehoeren:

- `MainApplication`
- `MainActivity`
- Android Manifest mit Automotive Permissions
- DI Setup fuer Automotive
- MBActivity/MBApplication Host

### `inCarApp:app-android-phone`

Dieses Modul enthaelt die Android-Phone-App.

Es kann Hilt oder eine andere Android-DI-Schicht behalten, sollte aber shared
Code nur ueber Public APIs konsumieren.

### `inCarApp:app-ios`

Dieses Modul enthaelt den iOS-Wrapper.

Es startet die KMP-Logik, verbindet iOS Lifecycle mit dem RootScope und hostet
die Compose-Multiplatform-UI.

## Was aus der aktuellen App wohin wandert

| Aktuell | Ziel |
| --- | --- |
| `CarInfo` | `inCarApp:public` als `VehicleInfo` oder `CarInfo` |
| `Result` | Plattform-Core oder `inCarApp:public`; langfristig besser allgemeines Core-Modul |
| `CarInfoRepository` | `inCarApp:public` oder durch `VehicleInfoProvider` ersetzen |
| `GetCarInfoUseCase` | `inCarApp:impl` |
| `DisconnectCarServiceUseCase` | `inCarApp:impl`, aber Lifecycle kritisch pruefen |
| `CarInfoProvider` | `inCarApp:public` als Capability Interface |
| `CarInfoProviderImpl` | `inCarApp:impl-android-automotive` |
| `CarInfoRepositoryImpl` | `inCarApp:impl`, wenn nur Provider orchestriert; sonst platform-spezifisch |
| `CarInfoViewModel` | `InCarPresenterImpl` oder Surface-ViewModel-Adapter |
| `CarInfoUiState` | `InCarModel` |
| `CarInfoScreen` | `renderer-automotive`, spaeter gemeinsamer CMP Renderer moeglich |
| `MainApplication` | `app-automotive` |
| `MainActivity` | `app-automotive` |
| Hilt Module | Android/Automotive DI Adapter |
| Screenshot Tests | Surface-spezifische Renderer-/Visual-Tests |

## Wie man Hardware-Abhaengigkeit regelt

Die wichtigste Regel:

> Hardwarezugriff wird nicht geteilt. Hardwarefaehigkeiten werden abstrahiert.

Shared Code sollte nicht wissen:

```text
android.car.Car
CarPropertyManager
VehiclePropertyIds
Context
Permission APIs
```

Shared Code sollte wissen:

```text
VehicleInfoProvider
VehicleCapabilities
VehicleInfoResult
DrivingStatePolicy
FeatureAvailability
```

### Capability-Modell

Empfohlenes Modell:

```kotlin
enum class CapabilityState {
    Available,
    NotSupported,
    PermissionMissing,
    TemporarilyUnavailable,
}

data class VehicleCapabilities(
    val vehicleInfo: CapabilityState,
    val drivingState: CapabilityState,
    val connectivity: CapabilityState,
)
```

Der Presenter kann damit shared bleiben:

- Wenn `Available`: Daten anzeigen.
- Wenn `PermissionMissing`: Permission-/Hinweis-Model erzeugen.
- Wenn `NotSupported`: Feature nicht anzeigen oder Unsupported-State zeigen.
- Wenn `TemporarilyUnavailable`: Retry/Loading/Error-State zeigen.

### Ergebnis statt Exceptions als App-Zustand

Die aktuelle App nutzt bereits ein `Result`-Modell. Das ist ein guter Start.

Fuer Plattformfaehigkeiten sollte man aber noch expliziter werden:

```kotlin
sealed interface VehicleDataState {
    data object Loading : VehicleDataState
    data class Content(val info: VehicleInfo) : VehicleDataState
    data object UnsupportedOnSurface : VehicleDataState
    data object PermissionRequired : VehicleDataState
    data class Error(val message: String) : VehicleDataState
}
```

Damit kann UI pro Surface anders reagieren, ohne die Business-Logik zu
duplizieren.

## UI-Sharing-Strategie

Da MB Widgets langfristig Multiplatform werden und iOS Compose Multiplatform
nutzen soll, ist eine hohe UI-Sharing-Quote realistisch.

Trotzdem sollten drei Ebenen getrennt werden:

```text
Shared UI Model
  InCarModel, InCarEvent

Shared Composable Building Blocks
  MB widget based composables
  common layout parts

Surface Renderer
  Automotive layout variant
  Phone layout variant
  iOS host/adaptation
```

### Was gut shared werden kann

- Textformatierung
- State Rendering
- Loading/Error/Unsupported-Komponenten
- einfache Info-Karten
- Button-Semantik
- Event-Mapping
- Presenter-to-Model Mapping

### Was eher surface-spezifisch bleibt

- Automotive Layoutdichte
- Driving/Parked Mode UX
- Rotary/Touch/Voice Input
- Phone Navigation
- iOS Hosting und Lifecycle
- Automotive Permissions
- Screen-Klassen und Head-Unit-Formfaktoren

## DI-Strategie einfach erklaert

Die vorherige Frage nach DI meinte:

> Wer entscheidet, welche konkrete Implementierung hinter einem Interface steckt?

Aktuell macht das Hilt:

```text
CarInfoProvider -> CarInfoProviderImpl
CarInfoRepository -> CarInfoRepositoryImpl
```

Fuer die neue Plattform gibt es zwei Phasen.

### Phase 1: Hilt bleibt Android-spezifisch

Das ist der pragmatische Start.

```text
app-automotive
  Hilt Module:
    VehicleInfoProvider -> AndroidAutomotiveVehicleInfoProvider

app-android-phone
  Hilt Module:
    VehicleInfoProvider -> AndroidPhoneVehicleInfoProvider
```

Shared KMP-Code enthaelt keine Hilt-Annotationen.

### Phase 2: Plattform-DI fuer KMP

Wenn die Plattform reifer wird, kann man Metro oder kotlin-inject nutzen.

Dann liegt DI naeher an der App Platform:

```text
inCarApp:public
  Interfaces

inCarApp:impl
  contributed shared bindings

inCarApp:impl-android-automotive
  contributed automotive bindings

app-automotive
  final AppGraph
```

Empfehlung: Erst Hilt-Adapter fuer Android/Automotive, spaeter KMP-DI
einfuehren, wenn die Modulgrenzen stabil sind.

## Automotive UX und Safety

Die konkrete Safety-/UX-Policy wird spaeter entschieden. Deshalb sollte die
Architektur sie schon vorbereiten.

Empfohlene Public API:

```kotlin
interface DrivingStatePolicy {
    fun availabilityFor(feature: InCarFeature): FeatureAvailability
}

sealed interface FeatureAvailability {
    data object Available : FeatureAvailability
    data object ParkedOnly : FeatureAvailability
    data object DisabledWhileDriving : FeatureAvailability
    data class Unavailable(val reason: String) : FeatureAvailability
}
```

Dann kann spaeter entschieden werden:

- Feature ist waehrend Fahrt erlaubt.
- Feature ist nur im Stand erlaubt.
- Feature braucht vereinfachte UI.
- Feature ist auf Phone/iOS anders verfuegbar.

## Migrationsphasen

### Phase 0: Beispiel stabilisieren

Ziel: Die vorhandene App als Referenz verstehen.

Aufgaben:

- Build-Layout klaeren (`settings.gradle.kts` inkludiert aktuell `:app`)
- Version Catalog mit Zielplattform abgleichen
- CarInfo Vertical Slice dokumentieren
- Tests als Baseline behalten

### Phase 1: Public API extrahieren

Ziel: Android-Car-Abhaengigkeiten aus den Contracts entfernen.

Aufgaben:

- `VehicleInfo` definieren
- `VehicleInfoProvider` definieren
- `VehicleInfoResult` definieren
- `InCarModel` und `InCarEvent` definieren
- Fakes in `testing` anlegen

### Phase 2: Shared Presenter einfuehren

Ziel: ViewModel-Logik in plattformunabhaengige Presenter-Logik ueberfuehren.

Aufgaben:

- `CarInfoViewModel`-State nach `InCarPresenterImpl` migrieren
- `CarInfoUiState` nach `InCarModel` migrieren
- Event-Lambdas modellieren
- Tests fuer Presenter mit Fake Provider schreiben

### Phase 3: Automotive Provider isolieren

Ziel: Android Automotive APIs in eigenes Impl-Modul verschieben.

Aufgaben:

- `CarInfoProviderImpl` nach `impl-android-automotive`
- Permissions im Automotive App-Modul behalten
- `CarPropertyManager` nur dort verwenden
- Disconnect/Lifecycle pruefen

### Phase 4: Renderer trennen

Ziel: UI moeglichst shared machen, aber Surface-Varianten erhalten.

Aufgaben:

- `CarInfoScreen` in Renderer-Modul verschieben
- `InCarModel` statt ViewModel direkt rendern
- Automotive Renderer mit MB Widgets
- Android Phone Renderer mit CMP-faehigen Composables
- iOS Renderer mit CMP Host

### Phase 5: Android Phone und iOS Provider einfuehren

Ziel: App laeuft auch ohne Fahrzeughardware.

Aufgaben:

- Android Phone Provider implementieren
- iOS Provider implementieren
- `NotSupported` / `PermissionMissing` / Remote-Datenstrategie entscheiden
- Surface-spezifische UX fuer fehlende Vehicle Daten bauen

### Phase 6: Plattform-Tooling

Ziel: Wiederholbarer Weg fuer weitere Features.

Aufgaben:

- Gradle Convention Plugin
- Modulstruktur-Check
- Starter Template
- Sample App
- Test-Fixtures
- Renderer-Teststrategie
- CI-Matrix fuer Automotive, Phone und iOS

## Was noch entschieden werden muss

| Entscheidung | Warum sie wichtig ist |
| --- | --- |
| Bleibt Hilt dauerhaft Android-only oder kommt Metro/kotlin-inject? | Beeinflusst KMP-Graph und Codegen. |
| Wie sehen MB Widgets als Multiplatform API aus? | Entscheidet, wie viel UI wirklich shared werden kann. |
| Welche Vehicle-Daten sind auf Phone/iOS verfuegbar? | Bestimmt Provider-Strategie. |
| Gibt es Backend-/Companion-Daten fuer Phone/iOS? | Verhindert reine Unsupported-States. |
| Welche Automotive UX-Regeln gelten spaeter? | Beeinflusst Renderer und FeatureAvailability. |
| Soll `Result` app-spezifisch bleiben oder Core-API werden? | Beeinflusst Wiederverwendung ueber Features hinweg. |
| Wie werden Permissions modelliert? | Wichtig fuer Android Automotive und spaeter Phone. |
| Wie wird iOS Lifecycle mit RootScope verbunden? | Wichtig fuer stabile KMP-Laufzeit. |

## Mindestdateien fuer echte Build-Integration

Fuer Architekturplanung reichen die hochgeladenen Dateien aus. Fuer echte
Compilation/Integration in eine Plattform braucht man zusaetzlich:

- Zugriff auf interne Maven/Artifactory Repositories
- gueltige Credentials fuer MB-Artefakte
- Entscheidung, welche Version Catalog fuehrend ist
- klares Gradle-Layout fuer die Zielmodule
- MB Widgets Multiplatform Artefakte, sobald verfuegbar
- CI-Umgebung mit Android SDK 36 und passender JDK/Gradle-Version
- iOS Build-Setup fuer Compose Multiplatform

## Konkreter MVP-Vorschlag

Der erste sinnvolle MVP sollte nicht alle Plattformen perfekt koennen. Er sollte
den Architekturpfad beweisen.

MVP:

1. `inCarApp:public`
2. `inCarApp:impl`
3. `inCarApp:impl-android-automotive`
4. `inCarApp:renderer-automotive`
5. `inCarApp:testing`
6. `inCarApp:app-automotive`

Danach:

7. `inCarApp:impl-android-phone`
8. `inCarApp:renderer-android-phone`
9. `inCarApp:impl-ios`
10. `inCarApp:renderer-ios`
11. `inCarApp:app-ios`

So wird zuerst der schwierigste Hardware-Fall sauber gekapselt. Danach koennen
Phone und iOS dieselben Public APIs nutzen.

## Kernaussage

Die `inCarApp` sollte als Vertical Slice fuer eine Plattform dienen:

> Shared Presenter und Models beschreiben die App. Provider abstrahieren
> Fahrzeugfaehigkeiten. Renderer stellen dieselben Models pro Surface dar.
> Hardwarezugriff bleibt in plattformspezifischen Impl-Modulen.

Damit kann die Plattform spaeter Android Automotive, Android Phone und iOS
unterstuetzen, ohne die Business- und UI-State-Logik fuer jede Plattform neu zu
schreiben.
