# Amazon App Platform - Begriffsglossar

Dieses Glossar erklaert Begriffe, die im UML-Diagramm
[`app-platform-system-architecture.drawio`](../Diagrams/app-platform-system-architecture.drawio),
in der Erklaerung
[`app-platform-system-architecture-explanation.md`](app-platform-system-architecture-explanation.md)
oder allgemein in diesem Repository vorkommen koennen.

Die Begriffe sind bewusst etwas ausfuehrlicher gesammelt als unbedingt noetig.
Das Dokument soll beim Verstehen der Architektur helfen, auch wenn man mit
Kotlin Multiplatform, Dependency Injection oder der Modulstruktur noch nicht
vertraut ist.

## Architektur und allgemeine Begriffe

| Begriff | Bedeutung |
| --- | --- |
| App Platform | Das Framework in diesem Repository. Es hilft dabei, Kotlin Multiplatform Apps mit gemeinsamer Business-Logik, Dependency Injection, Scopes, Presentern und Renderern zu bauen. |
| Architektur | Die grundlegende Struktur eines Systems: welche Teile es gibt, welche Aufgaben sie haben und wie sie miteinander kommunizieren. |
| Systemarchitektur | Die grobe technische Gesamtsicht auf ein System. Im Diagramm meint das Zusammenspiel aus App-Start, DI, Scopes, Features, Presentern, Renderern und Tooling. |
| UML | Unified Modeling Language. Eine standardisierte Notation, um Softwarestrukturen und Abhaengigkeiten grafisch darzustellen. |
| UML Component Diagram | Ein UML-Diagrammtyp, der Komponenten und deren Beziehungen zeigt. Das Draw.io-Diagramm nutzt diesen Stil. |
| Draw.io / diagrams.net | Ein Diagrammwerkzeug. `.drawio`-Dateien koennen dort geoeffnet und bearbeitet werden. |
| Komponente | Ein abgegrenzter Baustein im System, zum Beispiel Presenter, RendererFactory oder Gradle Plugin. |
| Schicht / Layer | Eine logische Ebene im System. Das Diagramm zeigt unter anderem Tooling, Platform Entrypoints, Application Assembly, Feature Modules und Framework Core. |
| Laufzeit / Runtime | Der Zeitraum, in dem die App tatsaechlich ausgefuehrt wird. |
| Build-Zeit / Compile-Zeit | Der Zeitraum, in dem Quellcode uebersetzt, generiert, geprueft und paketiert wird. |
| API | Application Programming Interface. Eine oeffentliche Schnittstelle, gegen die anderer Code programmieren kann. |
| Contract | Ein Vertrag zwischen Komponenten. Er beschreibt, was eine Komponente anbietet oder erwartet, ohne die konkrete Implementierung offenzulegen. |
| Implementierung | Der konkrete Code, der eine API oder ein Interface tatsaechlich ausfuehrt. |
| Implementierungsdetail | Technische Einzelheit einer Implementierung, die andere Module moeglichst nicht kennen sollten. |
| Kopplung | Beschreibt, wie stark Komponenten voneinander abhaengen. Weniger Kopplung macht Code meist wartbarer und besser testbar. |
| Kohesion | Beschreibt, wie gut die Teile eines Moduls fachlich zusammenpassen. Hohe Kohesion ist gut, weil ein Modul dann eine klare Aufgabe hat. |
| Wiederverwendbarkeit | Die Faehigkeit, Code in mehreren Apps, Plattformen oder Features erneut zu verwenden. |
| Plattform | Ein Zielsystem, auf dem die App laufen kann, zum Beispiel Android, iOS, Desktop oder Web. |
| Plattformunabhaengig | Code, der nicht direkt von einer konkreten Plattform-API abhaengt. |
| Plattformspezifisch | Code, der bewusst nur fuer eine Plattform gilt, zum Beispiel Android-Code mit `Application` oder iOS-Code mit `UIApplication`. |
| Bootstrap | Der Startvorgang einer App. Dabei werden Plattform, Root Scope, DI-Graph und UI-Rendering initialisiert. |
| Entrypoint | Der Einstiegspunkt einer App oder Plattform, zum Beispiel `MainActivity` auf Android oder `Main.kt` auf Desktop. |
| App Shell | Die aeussere Struktur einer App, die den eigentlichen Inhalt einbettet. In diesem Projekt uebernehmen Templates oft diese Rolle. |
| Root | Die oberste Ebene einer Struktur. Ein Root Scope ist der oberste Scope, ein Root Model ist das oberste Model der UI-Struktur. |
| Lifecycle | Der Lebenszyklus eines Objekts oder Bereichs: erzeugen, verwenden, beenden/aufraeumen. |
| Lifecycle Boundary | Eine Grenze, innerhalb der Objekte gemeinsam leben und gemeinsam beendet werden. Scopes sind solche Grenzen. |

## Kotlin Multiplatform und Plattformen

| Begriff | Bedeutung |
| --- | --- |
| Kotlin Multiplatform | Kotlin-Technologie, mit der gemeinsamer Code fuer mehrere Plattformen geschrieben werden kann. |
| KMP | Abkuerzung fuer Kotlin Multiplatform. |
| commonMain | Source Set fuer gemeinsamen Code, der auf mehreren Plattformen genutzt wird. |
| commonTest | Source Set fuer gemeinsame Tests. |
| androidMain | Source Set fuer Android-spezifischen Code. |
| iosMain | Source Set fuer iOS-spezifischen Code. |
| desktopMain | Source Set fuer Desktop-spezifischen Code. |
| wasmJsMain | Source Set fuer Web/Wasm-spezifischen Code. |
| Source Set | Eine Quellcodegruppe in Kotlin Multiplatform, zum Beispiel `commonMain` oder `androidMain`. |
| Android | Mobile Plattform von Google. In diesem Projekt eine der Zielplattformen der Beispielapps. |
| iOS | Mobile Plattform von Apple. In diesem Projekt ueber Kotlin/Native und Xcode Wrapper angebunden. |
| Desktop | JVM-basierte Desktop-Zielplattform, typischerweise mit Compose Multiplatform UI. |
| JVM | Java Virtual Machine. Laufzeitumgebung fuer Java/Kotlin auf Desktop/Server. |
| Native | Plattformziel ohne JVM, zum Beispiel iOS ueber Kotlin/Native. |
| Web | Browser-Zielplattform. In diesem Projekt ueber Kotlin/Wasm. |
| Wasm | WebAssembly. Ein binaeres Format, das im Browser ausgefuehrt werden kann. |
| WasmJs | Kotlin-Ziel fuer WebAssembly mit JavaScript-Integration. |
| Browser Distribution | Fertiger Web-Build, der im Browser ausgeliefert werden kann. |
| Xcode Wrapper | Ein iOS-Projekt, das die Kotlin-Multiplatform-App einbindet und ueber Xcode baut/startet. |
| Android Application | Android-Klasse, die beim Start der App initialisiert wird. |
| MainActivity | Typischer Android-Einstiegspunkt fuer eine UI. |
| MainViewController | iOS-Einstiegspunkt, der eine Kotlin/Compose-basierte View bereitstellt. |
| Main.kt | Hauefiger Einstiegspunkt fuer Desktop- oder Wasm-Anwendungen in Kotlin. |
| expect / actual | Kotlin-Multiplatform-Mechanismus, um gemeinsame Deklarationen mit plattformspezifischen Implementierungen zu verbinden. Dieses Projekt empfiehlt fuer viele Faelle DI statt `expect / actual`. |

## Dependency Injection und Dependency Inversion

| Begriff | Bedeutung |
| --- | --- |
| Dependency | Eine Abhaengigkeit, die eine Klasse oder Komponente benoetigt, zum Beispiel ein Repository, Presenter oder Service. |
| Dependency Injection | Muster, bei dem Abhaengigkeiten von aussen bereitgestellt werden, statt sie in einer Klasse selbst zu erzeugen. |
| DI | Abkuerzung fuer Dependency Injection. |
| Dependency Inversion | Prinzip, nach dem hoherwertige Logik von Abstraktionen abhaengt und nicht direkt von konkreten Implementierungen. |
| Inversion of Control | Allgemeines Prinzip, bei dem eine Komponente nicht alles selbst steuert, sondern Kontrolle an Frameworks oder externe Konfiguration abgibt. DI ist eine Form davon. |
| Object Graph | Netzwerk aller Objekte und Abhaengigkeiten, die durch Dependency Injection zusammengesetzt werden. |
| Dependency Graph | Synonym oder sehr nah verwandt mit Object Graph. Zeigt, welche Komponenten voneinander abhaengen. |
| AppGraph | Der DI-Graph der App. Er kennt die relevanten Abhaengigkeiten und Implementierungen fuer eine konkrete App und Plattform. |
| Platform AppGraph | Ein AppGraph, der pro Plattform definiert wird, damit Android, iOS, Desktop oder Wasm eigene Objekte bereitstellen koennen. |
| Component | In DI-Kontexten oft ein Objekt oder Interface, das einen DI-Graphen repraesentiert. |
| Binding | Verknuepfung zwischen einer API und ihrer konkreten Implementierung. |
| Contributed Binding | Binding, das automatisch per Annotation in den DI-Graph eingebracht wird. |
| Constructor Injection | Abhaengigkeiten werden ueber den Konstruktor einer Klasse uebergeben. Das ist meist gut testbar und wenig magisch. |
| Provider | Objekt oder Funktion, die eine Abhaengigkeit bereitstellt. |
| Factory | Objekt oder Funktion, die neue Instanzen erzeugt. |
| Singleton | Objekt, von dem innerhalb eines Scopes nur eine Instanz existiert. |
| Scope Annotation | Annotation, die beschreibt, in welchem Scope eine DI-Instanz lebt. |
| Metro | Compile-time Dependency-Injection-Framework. In diesem Projekt die empfohlene DI-Loesung. |
| kotlin-inject | Alternatives compile-time DI-Framework, das ebenfalls unterstuetzt wird. |
| kotlin-inject-anvil | Erweiterung rund um kotlin-inject, historisch und fuer bestehende Codebasen relevant. |
| Dagger | Bekannter DI-Ansatz aus der Android/JVM-Welt. In der Doku als historischer Kontext erwaehnt. |
| Anvil | Framework von Square fuer DI-Codegenerierung, historisch verwandt mit den spaeteren Ansaetzen. |
| `@DependencyGraph` | Metro-Annotation, die einen Dependency Graph definiert. |
| `@DependencyGraph.Factory` | Metro-Annotation fuer eine Factory, die einen Dependency Graph erzeugt. |
| `@Provides` | Annotation, um Werte explizit in den DI-Graph einzubringen. |
| `@ContributesBinding` | Annotation, mit der eine Klasse als Implementierung einer API in den Graph eingebracht wird. |
| `@ContributesTo` | Annotation, mit der eine Schnittstelle oder ein Binding zu einem bestimmten Graph/Scope beigetragen wird. |
| `@SingleIn` | Annotation, die eine Instanz innerhalb eines Scopes singleton-artig macht. |
| `@Inject` | DI-Annotation, die bei einigen DI-Frameworks Konstruktor- oder Feldinjektion markiert. |
| Generated Binding | Binding-Code, der von einem Tool generiert wird und nicht manuell geschrieben ist. |
| Multibinding | DI-Technik, bei der mehrere Implementierungen in eine Sammlung eingebracht werden. Renderer werden so auffindbar gemacht. |

## Scopes und Lebenszyklus

| Begriff | Bedeutung |
| --- | --- |
| Scope | Ein Bereich mit eigenem Lebenszyklus, in dem Services, DI-Graphen und CoroutineScopes gehalten werden koennen. |
| Root Scope | Oberster Scope der App. Wird beim App-Start erzeugt und lebt in der Regel so lange wie die App. |
| AppScope | Scope fuer appweite Abhaengigkeiten. |
| RendererScope | Spezieller Scope fuer Renderer-Bindings und Renderer-Erzeugung. |
| RootScopeProvider | Schnittstelle oder Objekt, das Zugriff auf den Root Scope bereitstellt. |
| Service | Wiederverwendbare Funktionalitaet, die in einem Scope registriert sein kann. |
| CoroutineScope | Kotlin-Konstrukt, das die Lebensdauer von Coroutines steuert. |
| Coroutine | Leichtgewichtiger nebenlaeufiger Ablauf in Kotlin. |
| State holder | Objekt, das Zustand verwaltet oder bereitstellt. Presenter koennen eine solche Rolle einnehmen. |
| Cleanup | Aufraeumen von Ressourcen am Ende eines Lebenszyklus. |
| Tear down | Beenden oder Abbauen eines Scopes oder Testaufbaus. |

## Presenter, Models und Datenfluss

| Begriff | Bedeutung |
| --- | --- |
| Presenter | Komponente, die Business-Logik, Navigation und Zustand berechnet und als Model bereitstellt. |
| MoleculePresenter | Presenter-Variante, die Compose/Molecule nutzt, um Models zu berechnen. |
| Molecule | Library von Cash App, die Compose Runtime nutzt, um aus Composable-Funktionen reaktive StateFlows zu erzeugen. |
| Compose Runtime | Kern von Compose, der Zustandsaenderungen und Recomposition steuert, auch ohne UI. |
| `@Composable` | Annotation aus Compose. Markiert Funktionen, die am Compose-Zustandsmodell teilnehmen. |
| Model | Datenstruktur, die den aktuellen Zustand beschreibt, den die UI anzeigen soll. |
| BaseModel | Gemeinsames Basisinterface fuer Models in der App Platform. |
| Immutable Model | Model, das nach dem Erzeugen nicht veraendert wird. Aenderungen werden durch neue Models dargestellt. |
| State | Zustand der App oder eines Features zu einem bestimmten Zeitpunkt. |
| StateFlow | Kotlin Flow-Typ, der immer einen aktuellen Zustand hat und neue Zustaende reaktiv ausliefert. |
| Flow | Kotlin-API fuer asynchrone Datenstroeme. |
| Event | Aktion oder Signal aus der UI, zum Beispiel Button-Klick oder Texteingabe. |
| Event Lambda | Funktion im Model, ueber die Renderer Nutzeraktionen zurueck an den Presenter schicken. |
| Unidirectional Dataflow | Datenfluss in eine Haupt-Richtung: Presenter erzeugt Model, Renderer zeigt Model, Events gehen kontrolliert zurueck. |
| Model Tree | Baumstruktur aus Models. Ein Root Model kann weitere Child Models enthalten. |
| Root Model | Oberstes Model, das die gesamte App- oder Screen-Struktur beschreibt. |
| Navigation | Wechsel zwischen Screens oder Zustandsbereichen der App. In diesem Projekt kann Navigation modellgetrieben ueber Presenter erfolgen. |
| Model-driven navigation | Navigation wird als Teil des Models oder Presenter-Zustands ausgedrueckt, statt direkt in der UI hart verdrahtet zu sein. |
| Parent Presenter | Presenter, der andere Presenter aufruft oder ihre Models kombiniert. |
| Child Presenter | Presenter, dessen Model in ein groesseres Model eingebettet wird. |
| Recomposition | Compose-Vorgang, bei dem Funktionen wegen geaendertem Zustand erneut ausgefuehrt werden. |
| Remember | Compose-Funktion, um Werte ueber Recompositions hinweg zu behalten. |
| Sealed Interface | Kotlin-Sprachmittel, um eine geschlossene Menge von Typvarianten zu definieren. Nuetzlich fuer Model-Zustaende. |

## Renderer und UI

| Begriff | Bedeutung |
| --- | --- |
| Renderer | Komponente, die ein Model in sichtbare UI uebersetzt. |
| RendererFactory | Komponente, die anhand eines Models den passenden Renderer findet oder erzeugt. |
| ComposeRenderer | Renderer fuer Compose Multiplatform UI. |
| ViewRenderer | Renderer fuer klassische Android Views. |
| Android Views | Traditionelles Android-UI-System mit `View`, `ViewGroup`, `TextView` usw. |
| Compose Multiplatform | UI-Framework, mit dem Compose-basierte UIs auf mehreren Plattformen laufen koennen. |
| Compose UI | UI-Schicht von Compose, im Gegensatz zur reinen Compose Runtime. |
| SwiftUI | Apple-UI-Framework. In der Doku als moegliche, aber nicht von der App Platform bereitgestellte UI-Richtung erwaehnt. |
| UIKit | Klassisches iOS-UI-Framework. |
| `render(model)` | Allgemeine Renderer-Methode, die ein Model rendert. Bei ComposeRenderern wird stattdessen eine composable Render-Methode genutzt. |
| `renderCompose(model)` | Compose-spezifische Render-Methode, die im Composable-Kontext ausgefuehrt wird. |
| `Modifier` | Compose-Konzept zur Beschreibung von Layout, Verhalten, Styling und Interaktion an UI-Elementen. |
| UI Layer | Schicht, die fuer Darstellung und Interaktion verantwortlich ist. |
| Renderer Hierarchy | Renderer koennen andere Renderer aufrufen und so eine UI-Baumstruktur aufbauen. |
| Parent Renderer | Renderer, der Child Models an Child Renderer weitergibt. |
| Child Renderer | Renderer, der einen Teilbereich eines groesseren Models rendert. |
| `@ContributesRenderer` | Annotation, mit der ein Renderer automatisch fuer den passenden Model-Typ registriert wird. |
| Renderer Binding | DI-/Codegen-Verknuepfung zwischen Model-Typ und Renderer. |
| ComposeAndroidRendererFactory | Android-spezifische Factory, die ComposeRenderer und ViewRenderer kombinieren kann. |
| AndroidRendererFactory | Android-spezifische Factory fuer ViewRenderer. |
| ComposeRendererFactory | RendererFactory fuer Compose Multiplatform. |

## Modulstruktur

| Begriff | Bedeutung |
| --- | --- |
| Gradle Module | Eigenstaendige Build-Einheit in Gradle, zum Beispiel `:sample:login:public`. |
| Module Structure | Die App-Platform-Regel, Code in `:public`, `:impl`, `:testing`, `:*-robots` und `:app` zu trennen. |
| `:public` module | Modul fuer oeffentliche APIs, Interfaces, Models und Contracts. |
| `:impl` module | Modul fuer konkrete Implementierungen. Sollte nur von `:app`-Modulen direkt importiert werden. |
| `:testing` module | Modul fuer Fakes, Testhilfen und gemeinsame Test-Infrastruktur. |
| `:*-robots` module | Modul fuer UI-Robots und wiederverwendbare Testaktionen. |
| `:app` module | Finale Anwendung, die APIs und Implementierungen zusammenfuehrt. |
| Non-app module | Jedes Modul, das kein finales App-Modul ist. Solche Module sollen nicht direkt von `:impl` abhaengen. |
| Transitive Dependency | Abhaengigkeit, die indirekt ueber ein anderes Modul ins Projekt kommt. |
| Dependency Graph im Build | Struktur der Gradle-Modulabhaengigkeiten. Nicht zu verwechseln mit dem DI-Object-Graph zur Laufzeit. |
| Public API | Oeffentliche Schnittstelle eines Moduls, die von anderen Modulen verwendet werden darf. |
| API Leakage | Problem, bei dem Implementierungsdetails versehentlich Teil der oeffentlichen API werden. |
| Build Graph | Gesamtheit der Gradle-Module und ihrer Abhaengigkeiten. |
| Artifact ID | Eindeutiger Name eines veroeffentlichten Build-Artefakts. |
| Android Namespace | Paket-/Namespace-Konfiguration fuer Android-Module. |

## Build, Gradle und Codegenerierung

| Begriff | Bedeutung |
| --- | --- |
| Gradle | Build-System, das dieses Projekt baut, testet und paketiert. |
| Gradle Plugin | Erweiterung fuer Gradle. Das App-Platform-Plugin automatisiert viele KMP- und DI-Konfigurationen. |
| Convention Plugin | Gradle-Plugin, das wiederkehrende Projektkonfiguration zentralisiert. |
| `settings.gradle` | Datei, die den Root-Build und alle enthaltenen Gradle-Module definiert. |
| `build.gradle` | Gradle-Builddatei fuer Projekt- oder Modulkonfiguration. |
| `libs.versions.toml` | Version Catalog fuer Dependency- und Plugin-Versionen. |
| buildSrc | Spezieller Gradle-Bereich fuer eigene Build-Logik im Repository. |
| Included Build | Gradle-Mechanismus, bei dem ein anderer Build eingebunden wird. Hier wird das lokale `gradle-plugin` eingebunden. |
| Dependency Substitution | Gradle-Technik, um eine Dependency durch ein lokales Projekt zu ersetzen. |
| KSP | Kotlin Symbol Processing. Tool fuer Codegenerierung auf Basis von Kotlin-Symbolen. |
| KSP common | Gemeinsame KSP-Hilfen in diesem Repository. |
| Compiler Plugin | Erweiterung des Kotlin-Compilers. In diesem Repo gibt es Metro-Extensions fuer bestimmte DI-Features. |
| FIR | Frontend Intermediate Representation im Kotlin-Compiler. Relevant fuer Compiler-Plugin-Tests. |
| IR | Intermediate Representation des Kotlin-Compilers. Ebenfalls relevant fuer Compiler-Plugin-Tests. |
| Codegen | Kurzform fuer Code Generation. |
| Generated Test Runner | Testklasse, die aus Testdaten generiert wird. |
| Golden File | Erwartete Ausgabedatei in Tests. Wenn sich Compiler-Ausgaben absichtlich aendern, muessen Goldens aktualisiert werden. |
| Diagnostics Test | Test, der erwartete Compilerdiagnosen prueft. |
| Box Test | Compiler-Test, der kompiliert und ausfuehrt. Die Funktion `box()` soll typischerweise `"OK"` liefern. |
| Dump Test | Test, der Compiler-Zwischenausgaben mit Golden Files vergleicht. |

## Testing und Qualitaet

| Begriff | Bedeutung |
| --- | --- |
| Test Helper | Hilfsfunktion oder Hilfsklasse, die Tests einfacher macht. |
| Fake | Einfache Testimplementierung einer API, die echtes Produktionsverhalten ersetzt. |
| Mock | Testobjekt, das Verhalten simuliert und oft Interaktionen verifiziert. |
| Robot | Testmuster fuer UI-Tests. Ein Robot kapselt Aktionen und Assertions auf einer UI. |
| UI Robot | Robot, der Benutzerinteraktionen auf der UI beschreibt. |
| Turbine | Kotlin-Testlibrary zum Pruefen von Flow-Emissionen. |
| Unit Test | Test einer kleinen Codeeinheit ohne echte Plattformumgebung. |
| Instrumented Test | Android-Test, der auf Emulator oder echtem Geraet laeuft. |
| Emulator | Virtuelles Android-Geraet fuer Tests. |
| `desktopTest` | Gradle-Testtask fuer Desktop-Ziel. |
| `iosSimulatorArm64Test` | Gradle-Testtask fuer iOS-Simulator auf ARM64. |
| `wasmJsTest` | Gradle-Testtask fuer Web/Wasm-Ziel. |
| `linuxX64Test` | Gradle-Testtask fuer Linux x64. |
| `testDebugUnitTest` | Android-Unit-Testtask fuer Debug-Variante. |
| `emulatorCheck` | Android Instrumented-Testtask mit verwaltetem Emulator. |
| `connectedDebugAndroidTest` | Android Instrumented-Testtask gegen ein verbundenes Geraet oder einen manuell gestarteten Emulator. |
| `apiCheck` | Prueft, ob die oeffentliche API mit den erwarteten API-Dateien uebereinstimmt. |
| `apiDump` | Aktualisiert API-Baselines, wenn sich die oeffentliche API absichtlich geaendert hat. |
| `ktfmtCheck` | Prueft Kotlin-Formatierung mit ktfmt. |
| `detekt` | Statische Analyse fuer Kotlin-Code. |
| `lint` | Android-/Codequalitaetspruefung. |
| `checkModuleStructureDependencies` | Gradle-Task, der die App-Platform-Modulregeln prueft. |
| CI | Continuous Integration. Automatisierte Pruefung des Projekts, typischerweise auf GitHub Actions. |
| GitHub Actions | CI/CD-System von GitHub. |
| Quality Gate | Pruefung, die bestanden werden muss, bevor Code als akzeptabel gilt. |

## Projektbereiche im Repository

| Begriff | Bedeutung |
| --- | --- |
| `sample/` | Haupt-Beispielapp. Zeigt End-to-End-Nutzung von Scopes, DI, Presentern, Renderern, Templates, Fakes und Robots. |
| `recipes/` | Zweite Beispielapp mit wiederverwendbaren Rezepten und Patterns. |
| `blueprints/starter/` | Eigenstaendige Starter-Vorlage fuer neue Apps. |
| `gradle-plugin/` | Quellcode des veroeffentlichbaren App-Platform-Gradle-Plugins. |
| `buildSrc/` | Repo-interne Gradle-Konventionen. |
| `docs/` | Produkt- und Framework-Dokumentation. |
| `scope/` | Framework-Module rund um Scopes und Scope-Testing. |
| `di-common/` | Gemeinsame DI-nahe APIs oder Hilfen. |
| `presenter/` | Allgemeine Presenter-APIs. |
| `presenter-molecule/` | Molecule-basierte Presenter-Integration. |
| `renderer/` | Allgemeine Renderer-APIs. |
| `renderer-compose-multiplatform/` | Compose-Multiplatform-Renderer-Integration. |
| `renderer-android-view/` | Android-View-Renderer-Integration. |
| `robot/` | Robot-APIs und Testabstraktionen. |
| `metro/` | Metro-Integration. |
| `metro-extensions/` | Erweiterungen und Compiler-Plugin-Arbeit fuer Metro. |
| `kotlin-inject/` | kotlin-inject-Integration. |
| `kotlin-inject-extensions/` | Erweiterungen fuer kotlin-inject. |
| `ksp-common/` | Gemeinsame KSP-Hilfslogik. |
| `internal/` | Interne Hilfen, zum Beispiel Testinfrastruktur. |
| `mkdocs.yml` | Konfiguration fuer die Dokumentationsseite. |
| `kotlin-js-store/wasm/yarn.lock` | Committer Yarn-Lockfile fuer Wasm/npm-Abhaengigkeiten. |

## Dokumentation, Web und Deployment

| Begriff | Bedeutung |
| --- | --- |
| MkDocs | Tool zum Erzeugen einer Dokumentationswebsite aus Markdown. |
| mkdocs-material | Theme und Erweiterungspaket fuer MkDocs. |
| GitHub Pages | Hosting-Funktion von GitHub fuer statische Websites. |
| Pages Workflow | GitHub-Actions-Workflow, der Doku und Wasm-Beispiele baut und veroeffentlicht. |
| Wasm Artifact | Build-Ergebnis fuer WebAssembly, das im Browser ausgefuehrt werden kann. |
| Production Bundle | Optimierter Build fuer Auslieferung/Deployment. |
| Development Server | Lokaler Server fuer Entwicklung, zum Beispiel fuer Wasm-Apps. |
| Yarn Lockfile | Datei, die genaue npm/Yarn-Abhaengigkeitsversionen festhaelt. |
| Lockfile Drift | Zustand, wenn generierte Lockfiles nicht mehr zur committed Version passen. |
| Dependency Update | Absichtliches Aktualisieren von Abhaengigkeiten und Lockfiles. |

## Wichtige Architekturregeln

| Regel | Bedeutung |
| --- | --- |
| APIs in `:public` | Wiederverwendbare Vertrage gehoeren in `:public`-Module. |
| Implementierungen in `:impl` | Konkreter Produktionscode gehoert in `:impl`-Module. |
| Apps verbinden alles | Nur finale `:app`-Module sollen `:impl`-Module direkt importieren. |
| Tests nutzen Fakes | Tests sollen wenn moeglich APIs ueber Fakes oder Testhilfen ersetzen. |
| Presenter kennen keine UI | Presenter erzeugen Models und enthalten Logik, aber keine konkrete UI-Darstellung. |
| Renderer enthalten UI | Renderer wissen, wie ein Model auf einer Plattform dargestellt wird. |
| Events gehen ueber Models zurueck | UI-Interaktionen werden ueber Event-Lambdas im Model an Presenter gemeldet. |
| DI statt harter Abhaengigkeiten | Konkrete Implementierungen werden injiziert, statt direkt in Konsumenten gebaut zu werden. |
| Plattformdetails bleiben am Rand | Android-, iOS-, Desktop- und Wasm-spezifischer Code sollte an Plattformgrenzen oder in passenden Source Sets liegen. |
| Codegen ist Teil des Designs | Viele Bindings und Registrierungen werden generiert und sind kein Zufall oder Nebeneffekt. |

## Kurzform des Gesamtsystems

Die App Platform organisiert eine Kotlin-Multiplatform-App so:

1. Plattform-Entrypoints starten die App.
2. Ein Root Scope definiert die Lebensdauer der App.
3. Der DI-Graph setzt APIs und Implementierungen zusammen.
4. Presenter berechnen Models.
5. Templates strukturieren den Root-Model-Baum.
6. RendererFactories finden passende Renderer.
7. Renderer zeigen die UI pro Plattform an.
8. Events fliessen kontrolliert zurueck in die Presenter.

Dadurch bleibt Business-Logik gemeinsam nutzbar und testbar, waehrend die UI
pro Plattform angepasst werden kann.
