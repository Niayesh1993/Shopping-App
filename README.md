# ShoppingApp

An Android app for searching a product catalogue, with paging and error recovery.
Kotlin + Jetpack Compose, Clean Architecture + MVVM, tested behaviour-first (BDD).

## Architecture

Clean Architecture in three layers; the dependency arrow always points inwards.

```
presentation  ──▶  domain  ◀──  data
```

```
com.zozi.shoppingapp
├── domain/          pure Kotlin — no Android, no Retrofit, no Compose
│   ├── model/       Product, Price, Rating, Availability, Promotion, ProductPage
│   ├── error/       AppError — the error vocabulary of the whole app
│   ├── result/      Outcome — sealed Success/Failure with map / fold
│   ├── repository/  ProductRepository (interface, owned by the domain)
│   └── usecase/     SearchProductsUseCase
│
├── data/            everything that knows about the network
│   ├── remote/      Retrofit API, DTOs, ApiKeyInterceptor, ErrorMapper, safeApiCall
│   ├── mapper/      DTO → domain model
│   └── repository/  ProductRepositoryImpl
│
├── presentation/    MVVM
│   └── search/      SearchViewModel, SearchUiState, SearchScreen, components/
│
├── di/              NetworkModule, RepositoryModule, DispatcherModule, Qualifiers
└── ui/theme/        Material 3 theme
```

## How it is built

- **MVVM** — `SearchViewModel` exposes one immutable `SearchUiState` as a `StateFlow`, collected with
  `collectAsStateWithLifecycle`.
- **Stateless UI** — `SearchScreen` takes state in and sends callbacks out. `SearchRoute` is the only
  composable that touches the ViewModel.
- **Browse, then search** — opening the app requests page 1 with no `query` parameter at all, so there
  are products to scroll straight away and the search field narrows them down. If the endpoint refuses
  a query-less request, the screen falls back to prompting for a search term and stops retrying.
- **Debounced search** — keystrokes go into a `MutableStateFlow`, debounced 350 ms and de-duplicated,
  so typing `apple` issues one request.
- **Pagination** — a `snapshotFlow` over `LazyListState` requests the next page at the end of the
  list; pages are appended and `distinctBy(Product::id)`.
- **Errors as data** — transport failures are mapped once at the data boundary into an `AppError` and
  returned inside `Outcome<T>`, so no exception escapes the repository. Full-screen errors and
  next-page errors are separate fields, so a failed page 2 never discards page 1.
- **DI** — Hilt with KSP, all modules in `SingletonComponent`.

## Technology

| | Version |
|---|---|
| Android Gradle Plugin | 9.5.0 |
| Kotlin / KSP | 2.2.10 / 2.2.10-2.0.2 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |
| Java | 11 |

| Library | Version | Used for |
|---|---|---|
| Compose BOM | 2026.02.01 | UI toolkit, Material 3 |
| AndroidX Lifecycle | 2.11.0 | ViewModel, lifecycle-aware state collection |
| Activity Compose | 1.13.0 | Entry point |
| Hilt (+ Navigation Compose) | 2.60.1 / 1.4.0 | Dependency injection |
| Retrofit (+ serialization converter) | 3.0.0 | HTTP client |
| OkHttp | 4.12.0 | Transport and logging |
| kotlinx.serialization | 1.9.0 | JSON |
| kotlinx.coroutines | 1.11.0 | suspend, Flow, StateFlow |
| Coil | 3.2.0 | Async images |

**Test libraries:** JUnit 4 (4.13.2), MockK (1.14.11, `mockk-android` for instrumented),
Turbine (1.2.1), kotlinx-coroutines-test (1.11.0), OkHttp MockWebServer (4.12.0),
Compose UI Test, Hilt Android Testing.

Versions live in `gradle/libs.versions.toml`.

## Testing: BDD, not TDD

The production code was written first, and the tests were written afterwards from its **observable
behaviour**. That is BDD, not TDD — there was no red-green-refactor cycle, so the tests document
behaviour rather than having driven the design.

Every test is named as a sentence and its body is split by `// Given`, `// When`, `// Then`.

```kotlin
@Test
fun `given a blank query when searching then the catalogue is browsed with no query at all`() = runTest {
    // Given
    respondWith(Outcome.Success(productPage()))

    // When the user has typed nothing, or only whitespace
    searchProducts("")
    searchProducts("   ")

    // Then the endpoint is asked to browse rather than to match an empty string
    coVerify(exactly = 2) { repository.searchProducts(query = null, page = 1) }
}
```

Instrumented tests spell the same three clauses in `camelCase`
(`givenTheSearchFailed_whenRetryIsTapped_thenTheResultsAppear`), because `minSdk` is 24 and dex
rejects method names containing spaces below API 30.

`ProductRepository` is mocked with MockK in every test — it has a single function, so a hand-written
fake would be more code than the behaviour it stands in for. Instrumented tests swap it into the Hilt
graph with `@TestInstallIn`.

**67 unit tests** in `app/src/test` and **24 instrumented tests** in `app/src/androidTest`.

## Running

Add to `local.properties` (git-ignored) or export as environment variables:

```properties
SEARCH_BASE_URL=https://your-host/mobile-assignment/
SEARCH_API_KEY=your-key
```

`SEARCH_BASE_URL` falls back to the assignment endpoint; without `SEARCH_API_KEY` the live API
answers 403. The whole test suite runs without either value, because no test uses the real network.

```bash
./gradlew assembleDebug                # build the app
./gradlew testDebugUnitTest            # 67 unit tests — no device needed
./gradlew connectedDebugAndroidTest    # 24 instrumented tests — needs a device
```
