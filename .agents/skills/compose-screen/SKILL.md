---
name: compose-screen
description: Use this skill when implementing or modifying a Jetpack Compose screen in ui/, creating ViewModels, or ensuring compliance with FRONTEND_GUIDELINES.md.
---

# Jetpack Compose Screen Development Workflow

Follow this procedure for building or modifying any screen in `ui/`.

## Screen Package Structure
Every feature screen lives under `ui/<feature>/`:
```text
ui/<feature>/
├── <Feature>Screen.kt      # Stateful wrapper + Stateless composables + Previews
├── <Feature>ViewModel.kt   # Hilt ViewModel exposing StateFlow<UiState>
└── <Feature>UiState.kt     # Immutable data class representing UI state
```

## Step-by-Step Implementation

1. **Define Immutable UI State**:
   - Single data class per screen:
     ```kotlin
     data class FeatureUiState(
         val isLoading: Boolean = false,
         val items: List<Item> = emptyList(),
         val errorMessage: String? = null
     )
     ```

2. **Build ViewModel**:
   - Annotate with `@HiltViewModel`.
   - Expose state through `StateFlow<FeatureUiState>`:
     ```kotlin
     @HiltViewModel
     class FeatureViewModel @Inject constructor(
         private val repository: LedgerRepository
     ) : ViewModel() {
         val uiState: StateFlow<FeatureUiState> = ...
     }
     ```
   - One-time events (snackbars, navigation) must use a `Channel` or consumed events, never an unreset boolean in `UiState`.

3. **Build Composables**:
   - Root composable collects state with `collectAsStateWithLifecycle()` and forwards state & event lambdas to stateless child composables.
   - Keep composables under 100 lines; extract smaller sub-composables.
   - Zero business logic inside composables.

4. **Strings & Formatting**:
   - Extract every user-facing string to `res/values/strings.xml` and call `stringResource(R.string.<id>)`.
   - Money display must use `AmountText` (converting paisa to `Rs. X,XX,XXX.XX`).

5. **Accessibility & Quality Checklist**:
   - Minimum 48dp touch targets on interactive elements.
   - Explicit `contentDescription` on all icons and images.
   - Support light and dark themes using `KharchaTheme`.
   - Include `@Preview(name = "Light Mode")` and `@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)`.

6. **Verify**:
   - Run strings guard: `bash scripts/guard-strings.sh`.
   - Run compilation & lint: `./gradlew lintDebug`.
