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

## 4. Theming & Complete Color System

Kharcha uses a **Forest Green + Warm Amber + Soft Neutral** palette. Green establishes trust and financial clarity; amber accents key actions; neutral surfaces keep numbers readable.

### 4.1 Palette Tokens
| Token Role | Light Theme | Dark Theme | Usage |
|---|---|---|---|
| **Primary** | `#176B52` | `#78D5B0` | Main CTA, active bottom navigation, primary actions |
| **Primary Container** | `#D5F2E4` | `#204D3D` | Selected cards, active chips |
| **On-Primary Container** | `#104B38` | `#C5F2DC` | Text/icons on primary containers |
| **Accent Amber** | `#F2B544` | `#F2C66D` | Highlights, badges, secondary emphasis |
| **Secondary** | `#53665C` | `#B8C5BB` | Supporting controls, metadata |
| **Tertiary** | `#527A85` | `#A5D1DE` | Informational chips, non-financial accents |
| **Background** | `#F7F8F5` | `#121714` | Main canvas (dark theme uses green-tinted neutral) |
| **Surface** | `#FFFFFF` | `#1A211D` | Cards, forms, modal sheets |
| **Surface Variant** | `#EDF1EC` | `#29342D` | Secondary surfaces, input fills |
| **Outline** | `#737D76` | `#89958C` | Input borders, dividers |

### 4.2 Financial & Semantic Status Tokens
These remain stable across both themes (immune to dynamic color shifts):
- **Income**: `#18794E` (Light) / `#70D6A3` (Dark) paired with `+` sign and label.
- **Expense**: `#B54735` (Light) / `#FFB4A8` (Dark) paired with `-` sign and label.
- **Warning**: `#835500` (Light) / `#F2C66D` (Dark) for duplicate IDs or review required.
- **Error**: `#BA1A1A` (Light) / `#FFB4AB` (Dark) for invalid amounts or storage errors.
- **Information**: `#245DB8` (Light) / `#A9C7FF` (Dark) for guidance notices.
- **Neutral Status**: `#536159` (Light) / `#B8C5BB` (Dark).

### 4.3 Receipt Confidence & Validation States
- **Valid Field**: Pale green background, green check icon, normal text contrast.
- **Needs Review**: Warm amber background, warning icon, plain-language reason.
- **Invalid Field**: Pale red background, error icon, blocks saving until corrected.
- **Possible Duplicate**: Pale blue information card with "View existing" vs "Save anyway" actions.

### 4.4 Backup Status States
- **Backed up**: Green status with timestamp of last successful write.
- **Never backed up**: Neutral gray with prompt to configure.
- **Backup running**: Blue progress indicator during active export.
- **Backup failed**: Red status explaining failure with retry option.
- **Folder access lost**: Amber warning prompting SAF directory re-selection.

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

## 12. Animotion MCP Design System Integration

All icons, animations, and micro-interactions in Kharcha frontend use the Animotion MCP catalog:
- **Vector Icons**: Real SVGs sourced from Lucide, Tabler, or Heroicons via `animotion.search_icons` and converted to Android vector drawables.
- **Fintech & Dashboard Animations**:
  - `FN07` (Bar Grow, 800ms `FastOutSlowInEasing` / `cubic-bezier(0.4, 0.0, 0.2, 1)`): Category spend percentage bars.
  - `FN08` (Value Counter, 1000ms ease-out): Metric and balance number interpolations.
  - `FN10` (Transaction Slide, 300ms ease-out): Staggered entry row entrance.
- **Attention Animations**: Pulse/shake transitions for duplicate transaction IDs and invalid inputs.
- **Button Micro-Interactions**: Spring physics (`spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)`) on FAB and action button taps.
