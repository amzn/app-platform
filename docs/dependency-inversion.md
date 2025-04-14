# Dependency Inversion

Dependency inversion means that high-level APIs don’t depend on low-level details and low-level details
only import other high-level APIs. It significantly reduces coupling between components. Dependency
inversion can be implemented on different levels, e.g. in code and in the module structure.

## Kotlin code

Dependency inversion implemented in Kotlin code refers to having abstractions in place instead of
relying on concrete implementations. Imagine this example:

```kotlin
class AccountProvider(
  private val database: SqliteDatabase,
  ...
) {
  val currentAccount: StateFlow<Account> = ...

  fun updateCurrentAccount(account: Account) {
    ...
  }
}

class ChangeAccountHandler(
  private val accountProvider: AccountProvider
) {

  private fun onAccountChanged(account: Account) {
    accountProvider.updateCurrentAccount(account)
    ...
  }
}
```

`ChangeAccountHandler` has a strong dependency on `AccountProvider`. This is problematic in multiple ways.
Evolving `AccountProvider` is challenging, because implementation details are easily leaked and become
part of the public API. Every dependency from `AccountProvider` is exposed to consumers, e.g. `ChangeAccountHandler`
knows that `AccountProvider` uses Sqlite for its implementation, a detail which should be hidden and makes
dependency graphs unnecessarily large. `ChangeAccountHandler` is hard to test. One has to spin up a Sqlite database
in a unit test environment in order to instantiate `AccountProvider` and pass it as argument to
`ChangeAccountHandler`.

A much better approach is introducing abstract APIs:

```kotlin
interface AccountProvider {
  val currentAccount: StateFlow<Account>

  fun updateCurrentAccount(account: Account)
}

class SqliteAccountProvider(
  private val database: SqliteDatabase
  ...
) : AccountProvider {

  @VisibleForTesting
  val allAccounts: List<Account> = ...

  ...
}
```

The interface `AccountProvider` solves the mentioned shortcomings. `SqliteAccountProvider` can change and
for example expose more fields (`allAccounts` in this sample) for verifications in unit tests without anyone
knowing as the interface doesn’t need to be updated. Sqlite is a pure implementation detail and no consumer
of `AccountProvider` has to know about it. This allows us to easily swap the implementation for a fake
`AccountProvider` together with fake data in a unit test for `ChangeAccountHandler`.

Breaking the dependency serves an additional purpose especially in Kotlin Multiplatform when
implementations have platform dependencies:

```kotlin
// commonMain
interface SqlDriver

// androidMain
class AndroidSqlDriver(context: Context) : SqlDriver

// iosMain
class NativeSqlDriver() : SqlDriver
```

Notice how the Android implementation has a strong dependency on the Android runtime through the `Context`
class. Relying on interfaces / abstract classes together with dependency injection is the
[preferred way](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-connect-to-apis.html#dependency-injection-framework) (1)
over `expect / actual` functions to implement dependency inversion as this approach allows platform specific changes.
{ .annotate }

1.  When you use a DI framework, you inject all of the dependencies through this framework. The same logic applies to handling platform dependencies. We recommend continuing to use DI if you already have it in your project, rather than using the expected and actual functions manually. This way, you can avoid mixing two different ways of injecting dependencies.
