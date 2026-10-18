# Kharcha: Frontend Guidelines

Scope: Jetpack Compose UI for Android. Applies to every screen.

## 1. Principles

1. Accuracy over speed. The user must see what will be saved before it is saved.
2. Few taps for the common path. Add entry and scan receipt are reachable from the home screen.
3. Calm and readable. Money screens must be easy to scan.
4. Offline by default. No loading spinners for network, because there is none.

## 2. Project structure

```
ui/
  theme/        Color, Type, Shape, Theme
  components/   shared composables
  navigation/   NavHost and routes
  home/
  entries/
  scan/
  summary/
  settings/
```

Each feature package holds its screen composable, its ViewModel, and its UI state class.

## 3. State management

- One `UiState` data class per screen, exposed as `StateFlow` from the ViewModel.
- Collect with `collectAsStateWithLifecycle`.
- One-time events (snackbar, navigation) use a `Channel` or a consumed-event field. Never a boolean in `UiState` that is never reset.
- Composables are stateless where possible. State is hoisted to the caller.
- No business logic in composables.

## 4. Theming

- Material 3 with dynamic color enabled on Android 12+, and a fixed fallback palette.
- Support light and dark themes.
- Semantic colors for income and expense must not rely on color alone. Pair with a sign or label.

## 5. Money and date display

- Convert paisa to a display string in one shared formatter. No ad hoc formatting in screens.
- Show the currency prefix `Rs.` consistently.
- Use Nepali digit grouping if chosen later. Decide once and apply everywhere.
- Dates are shown in Gregorian in v1. Bikram Sambat arrives in v1.1 through one shared date formatter.

## 6. Core components

- `AmountText`: formats and colors an amount.
- `EntryRow`: category, note, date, amount.
- `CategoryPicker`: bottom sheet or dropdown.
- `FieldWithStatus`: text field with a validation state (ok, warning, error).
- `ConfirmScanCard`: shows parsed fields next to the source image.
- `EmptyState`: icon, one sentence, one action.

## 7. Confirm screen rules

- Show the source image and the parsed fields together.
- Every parsed field is editable.
- A field that failed validation is highlighted and shows the reason in plain words.
- The save button stays disabled while any field is in error state.
- A duplicate warning appears above the form and offers "View existing entry" and "Save anyway".

## 8. Forms

- Amount field uses a numeric keyboard and accepts at most 2 decimals.
- Validate on blur and on save. Show one error message per field.
- Preserve input across rotation and process death with `rememberSaveable` or `SavedStateHandle`.

## 9. Accessibility

- Touch targets of at least 48dp.
- Content descriptions on icon buttons and images.
- Text scales with system font size. Test at the largest size.
- Contrast meets WCAG AA.

## 10. Error and empty states

- Every list has an empty state with a clear next action.
- OCR failure shows a plain message and offers manual entry with the image still attached.
- Backup failure shows what failed and that no data was changed.

## 11. Naming and code style

- Composables use PascalCase. Previews end in `Preview`.
- Provide a `@Preview` for each shared component, in light and dark.
- Strings live in `strings.xml`. No hard-coded user text.
- Keep composables under roughly 100 lines. Extract when larger.
