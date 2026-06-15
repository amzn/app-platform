# Amazon App Platform - Erklaerung des Feature-Module-Template-Diagramms

Dieses Dokument erklaert das Draw.io-Diagramm
[`app-platform-feature-module-template.drawio`](../Diagrams/app-platform-feature-module-template.drawio).
Das Diagramm zeigt einen Bauplan fuer ein typisches Feature in einer
App-Platform-Anwendung.

## Zweck des Diagramms

Das Diagramm beantwortet diese Frage:

> Wie sollte ein Feature strukturiert werden, damit es zur App-Platform-
> Architektur passt?

Ein Feature besteht nicht einfach aus einem grossen Modul. Stattdessen wird es
in klar getrennte Module aufgeteilt:

- `:public`
- `:impl`
- `:testing`
- `:impl-robots`
- finale Einbindung in `:app`

## Beispiel: `:sample:user`

Das Diagramm nutzt sinngemaess ein User-Feature als Beispiel.

Die Struktur koennte so aussehen:

```text
:sample:user:public
:sample:user:impl
:sample:user:testing
:sample:user:impl-robots
:sample:app
```

Diese Struktur laesst sich auf andere Features uebertragen, zum Beispiel Login,
Navigation, Checkout, Settings oder Profile.

## `:sample:user:public`

Das Public-Modul ist der stabile Vertrag des Features.

Es enthaelt typischerweise:

- Presenter-Interfaces
- Model-Typen
- Event-Typen
- Repository-Interfaces
- kleine Domain-Typen
- Contracts fuer andere Features

Beispiel:

```kotlin
interface UserPagePresenter : MoleculePresenter<UserPagePresenter.Input, UserPagePresenter.Model>
```

Andere Module duerfen dieses Public-Modul verwenden, ohne konkrete
Implementierungsdetails zu kennen.

## `:sample:user:impl`

Das Impl-Modul enthaelt Produktionscode.

Typische Inhalte:

- `UserPagePresenterImpl`
- `UserPageListRenderer`
- `UserPageDetailRenderer`
- Repository-Implementierungen
- Provider
- DI-Beitraege
- Renderer-Beitraege

Das Impl-Modul implementiert die APIs aus `:public`.

Beispiel:

```kotlin
@ContributesBinding(AppScope::class)
class UserPagePresenterImpl(...) : UserPagePresenter
```

Oder:

```kotlin
@ContributesRenderer
class UserPageListRenderer : ComposeRenderer<UserPageListPresenter.Model>()
```

## `:sample:user:testing`

Das Testing-Modul enthaelt wiederverwendbare Testunterstuetzung.

Typische Inhalte:

- Fakes
- Testdaten
- Fake Provider
- Scope-Testhilfen
- Fixture Builder

Der Zweck ist, Tests in anderen Modulen einfacher zu machen.

Beispiel:

```kotlin
class FakeUserRepository : UserRepository {
  ...
}
```

Tests koennen dann gegen die Public API arbeiten, aber eine Fake-Implementierung
verwenden.

## `:sample:user:impl-robots`

Das Robot-Modul enthaelt UI-Test-Robots.

Ein Robot kapselt wiederkehrende UI-Testaktionen:

- auf einen Button klicken
- Text eingeben
- Listenelement auswaehlen
- sichtbaren Text pruefen
- Screen-Zustand verifizieren

Dadurch werden UI-Tests lesbarer.

Statt:

```kotlin
composeRule.onNodeWithText("Alice").performClick()
composeRule.onNodeWithText("Details").assertIsDisplayed()
```

koennte ein Test sinngemaess schreiben:

```kotlin
userPageRobot.openUser("Alice")
userPageRobot.assertDetailsVisible()
```

## Optional: nested feature modules

Manche Features werden gross genug, dass eine weitere Unterteilung sinnvoll
wird.

Beispiel:

```text
:sample:user:list:public
:sample:user:list:impl
:sample:user:detail:public
:sample:user:detail:impl
```

Das lohnt sich, wenn:

- Teilbereiche separat wiederverwendet werden
- Presenter zu gross werden
- Renderer sehr unterschiedliche UI-Bereiche abdecken
- Tests sonst zu schwer wartbar werden
- Build-Zeiten durch kleinere Module profitieren

Nicht jedes Feature braucht diese Tiefe. Kleine Features sollten einfach
bleiben.

## Finale App Assembly

Rechts im Diagramm steht die finale App-Schicht.

### `:sample:app`

Das App-Modul ist die finale Anwendung.

Es darf:

- `:public`-Module verwenden
- `:impl`-Module importieren
- Plattform-Entrypoints enthalten
- AppGraph definieren
- RootScope erzeugen
- Features zusammensetzen

Das App-Modul ist der Ort, an dem konkrete Entscheidungen getroffen werden.

### Platform AppGraph

Der AppGraph bindet die konkreten Implementierungen ein.

Er nutzt die generierten DI-Beitraege aus den Impl-Modulen, um APIs mit
Implementierungen zu verbinden.

### App tests

App-Tests koennen Testing-Module und Robot-Module verwenden.

Das erlaubt:

- Fake-Daten
- Fake-Services
- wiederverwendbare UI-Aktionen
- plattformnahe Tests
- End-to-End-artige Tests innerhalb der App

## Feature Checklist

Beim Anlegen eines neuen Features kann man diese Fragen nutzen:

| Frage | Zielmodul |
| --- | --- |
| Ist es ein Interface oder Model, das andere nutzen sollen? | `:public` |
| Ist es konkrete Produktionslogik? | `:impl` |
| Wird es nur fuer Tests gebraucht, aber wiederverwendet? | `:testing` |
| Kapselt es UI-Testaktionen? | `:impl-robots` |
| Setzt es konkrete Feature-Implementierungen zusammen? | `:app` |

## Typische Fehler

### Zu viel im App-Modul

Wenn zu viele Feature-Klassen direkt in `:app` liegen, wird das App-Modul gross
und schwer wartbar.

Besser: Feature-Code in `:impl`, App nur fuer Assembly.

### APIs in Impl verstecken

Wenn andere Module konkrete Impl-Klassen verwenden muessen, fehlt vermutlich
eine Public API.

Besser: Interface oder Contract in `:public` bereitstellen.

### Testfakes in Produktionsmodulen

Fakes sollten nicht in `:impl` oder `:public` landen, wenn sie nur fuer Tests
gedacht sind.

Besser: in `:testing`.

### Robots in Tests duplizieren

Wenn dieselben UI-Aktionen in vielen Tests wiederholt werden, fehlt
wahrscheinlich ein Robot.

Besser: Robot-Modul anlegen.

## Kernaussage

Das Feature-Module-Template-Diagramm zeigt:

> Ein gutes Feature trennt Vertrag, Implementierung, Testunterstuetzung und
> App-Zusammensetzung.

Diese Trennung sorgt dafuer, dass Features wiederverwendbar, testbar und
plattformfreundlich bleiben.
