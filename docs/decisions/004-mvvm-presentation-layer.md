# ADR-004 — MVVM presentation layer

- **Status:** accepted
- **Date:** 2026-10-21

## Context

The 1.x app uses MVVM with LiveData, Data Binding and a `NetworkBoundResource` / `Resource`
wrapper. The new screens are written in Compose Multiplatform with ViewModels in shared code
(ADR-003), so the same presentation code must later run on iOS. The app has four screens and one
maintainer. MVI with intents and reducers is a popular alternative, but its ceremony does not pay
for itself at this size. MVVM is also the pattern most readers of this repository already know.
A name alone does not keep a pattern consistent, so the rules are written down here.

## Decision

1. **One immutable state per screen.** Each screen has one `XxxUiState` data class with read-only
   collections. Derived values are computed properties, not stored fields.
2. **One state stream and one event stream.** A ViewModel exposes `val uiState: StateFlow<XxxUiState>`
   and, for one-time effects such as navigation, a snackbar or a share sheet, `val events: Flow<XxxEvent>`
   backed by a buffered `Channel`.
3. **Plain methods, not intents.** The UI calls ordinary ViewModel methods (`onRefresh()`,
   `onAmountChanged(value)`); there is no sealed intent hierarchy and no reducer. This is the
   deliberate difference from MVI.
4. **Pure functions for non-trivial logic.** Portfolio totals, freshness and "refresh over existing
   data" live in pure functions with plain unit tests and no coroutines.
5. **ViewModels do not navigate** and do not touch platform APIs. They emit events; the platform
   shell handles them.
6. **Room is the single source of truth.** Screens observe a `Flow` from the database; the network
   is only a `refresh()` command, the one operation that can fail. A newer refresh cancels an
   older one, so a stale response never overwrites newer data.
7. **Stateless screens.** A screen composable takes `(state, callbacks, modifier)`; a thin stateful
   wrapper obtains the ViewModel from Koin (ADR-005) and collects the flows.
8. ViewModels use the multiplatform `androidx.lifecycle.ViewModel` and live in shared code, so the
   iOS app in 2.0.0 reuses them unchanged.

## What we do not do

- No LiveData, Data Binding, `NetworkBoundResource` or `Resource`/`Status` wrapper in new code.
- No business logic in composables.
- No `Dispatchers.*` referenced directly in ViewModels; dispatchers are injected where needed.

## Consequences

- A small shared base class may provide `updateState {}` and `sendEvent()`, with its own contract
  test; it is optional, not required.
- ViewModel tests use fakes of the repositories and need no Android framework.
- Reviews check new screens against these eight rules (`config/review-rules.md`).

## Reopen when

A screen's state logic outgrows plain methods and pure functions, or a fifth and sixth screen make
a reducer pattern cheaper than the rules above.
