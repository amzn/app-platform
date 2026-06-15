# Amazon App Platform - Erklaerung des Module-Dependency-Diagramms

Dieses Dokument erklaert das Draw.io-Diagramm
[`app-platform-module-dependencies.drawio`](../Diagrams/app-platform-module-dependencies.drawio).
Das Diagramm zeigt die Modulstruktur der App Platform und die wichtigste
Abhaengigkeitsregel im Repository.

## Zweck des Diagramms

Das Diagramm beantwortet diese Frage:

> Warum trennt die App Platform Features in `:public`, `:impl`, `:testing`,
> `:*-robots` und `:app`?

Die kurze Antwort lautet:

> APIs sollen wiederverwendbar bleiben, konkrete Implementierungen sollen nicht
> unkontrolliert in andere Module leaken.

## Die zentrale Regel

Die wichtigste Regel lautet:

> Nur finale `:app`-Module duerfen konkrete `:impl`-Module direkt importieren.

Alle anderen Module sollen gegen `:public` APIs arbeiten.

Das verhindert, dass wiederverwendbarer Code von konkreten Implementierungen,
Plattformdetails oder grossen transitive Dependency Graphen abhaengt.

## Allowed production dependency path

Links im Diagramm ist der erlaubte Produktionspfad dargestellt.

### `:feature:public`

Das `:public`-Modul enthaelt stabile Vertrage.

Typische Inhalte:

- Interfaces
- Presenter APIs
- Model-Typen
- Event-Typen
- Repository-Contracts
- kleine gemeinsame Domain-Typen

Ein Public-Modul sollte keine konkreten Storage-, Netzwerk-, UI- oder
Plattformdetails verraten.

### `:feature:impl`

Das `:impl`-Modul enthaelt konkrete Produktionsimplementierungen.

Typische Inhalte:

- `PresenterImpl`
- Renderer
- Provider
- Repository-Implementierungen
- DI-Beitraege
- plattformspezifische Implementierungen

Das `:impl`-Modul implementiert die APIs aus `:public`.

### `:sample:app` oder `:recipes:app`

Das finale `:app`-Modul setzt die konkrete App zusammen.

Es darf:

- Public APIs verwenden
- konkrete Impl-Module importieren
- den finalen AppGraph definieren
- Plattform-Entrypoints enthalten
- Features fuer eine konkrete App zusammensetzen

Das ist absichtlich erlaubt, weil eine finale App entscheiden muss, welche
Implementierungen sie wirklich verwendet.

## Testing and automation modules

Rechts oben zeigt das Diagramm Test- und Automatisierungsmodule.

### `:feature:testing`

Ein `:testing`-Modul enthaelt wiederverwendbare Testhilfen.

Typische Inhalte:

- Fakes
- Test-Fixtures
- Fake Provider
- Scope-Testhilfen
- vorgefertigte Testdaten

Diese Testhilfen implementieren normalerweise APIs aus `:public`. Dadurch kann
ein Test echte Implementierungen ersetzen, ohne Produktionsdetails importieren
zu muessen.

### `:feature:impl-robots`

Ein Robot-Modul enthaelt wiederverwendbare UI-Testaktionen.

Typische Inhalte:

- Screen Robots
- Klick-Helfer
- Assertions
- UI-Test-Vokabular

Robots machen Tests lesbarer. Statt in jedem Test einzelne UI-Knoten manuell zu
suchen, kapselt ein Robot die wiederkehrenden Aktionen.

## Forbidden shortcut

Rechts unten zeigt das Diagramm den verbotenen Shortcut:

> Ein Non-app-Modul importiert direkt ein `:impl`-Modul.

Das ist problematisch, weil dadurch konkrete Implementierungsdetails in
wiederverwendbaren Code gelangen.

Beispiel:

```text
:sample:navigation:impl -> :sample:user:impl
```

So eine Abhaengigkeit kann ungewollt dazu fuehren, dass Navigation nicht mehr
gegen die User-API arbeitet, sondern gegen konkrete User-Implementierungen.

Der bessere Weg:

```text
:sample:navigation:impl -> :sample:user:public
```

Die finale App darf dann beide Impl-Module zusammenfuehren:

```text
:sample:app -> :sample:navigation:impl
:sample:app -> :sample:user:impl
```

## Warum diese Trennung wichtig ist

### Kleinere Abhaengigkeitsgraphen

Wenn Module nur von Public APIs abhaengen, muessen sie nicht alle
Implementierungsdependencies mitziehen.

Das kann Build-Zeiten verbessern und reduziert geistige Komplexitaet.

### Weniger API Leakage

API Leakage bedeutet, dass Implementierungsdetails versehentlich Teil der
oeffentlichen API werden.

Beispiel: Ein Public-API-Typ sollte nicht verraten, dass intern SQLite,
Android-Kontext oder ein bestimmter Netzwerkclient verwendet wird.

### Einfachere Tests

Wenn Konsumenten gegen Interfaces arbeiten, koennen Tests leicht Fakes
verwenden.

Das ist deutlich einfacher, als echte Implementierungen mit echter Datenbank,
echtem Netzwerk oder echter Plattformumgebung starten zu muessen.

### Bessere Austauschbarkeit

Eine App kann eine andere Implementierung verwenden, solange sie dieselbe API
erfuellt.

Das ist besonders wertvoll fuer Kotlin Multiplatform, weil Android, iOS,
Desktop und Wasm unterschiedliche Plattformdetails haben koennen.

## Build Enforcement

Die Modulregel ist nicht nur eine Empfehlung. Sie wird im Build geprueft.

Der relevante Task heisst:

```bash
./gradlew checkModuleStructureDependencies
```

Dieser Task verhindert, dass verbotene Impl-Abhaengigkeiten unbemerkt ins
Projekt gelangen.

## Typische Entscheidungsfragen

| Frage | Antwort |
| --- | --- |
| Braucht anderer Code diesen Vertrag? | In `:public`. |
| Ist es eine konkrete Produktionsklasse? | In `:impl`. |
| Wird es nur in Tests gebraucht, aber von mehreren Tests geteilt? | In `:testing`. |
| Kapselt es UI-Testaktionen? | In `:*-robots`. |
| Muss die finale App konkrete Implementierungen verbinden? | In `:app`. |

## Kernaussage

Das Module-Dependency-Diagramm zeigt:

> Wiederverwendbarer Code spricht mit APIs. Konkrete Implementierungen werden
> erst in der finalen App zusammengesetzt.

Diese Regel ist eine der wichtigsten Grundlagen der App Platform.
