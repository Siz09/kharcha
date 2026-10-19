package com.sijan.kharcha.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand Colors (Forest Green + Warm Amber)
val PrimaryForestGreen = Color(0xFF176B52)
val PrimaryForestGreenDark = Color(0xFF10513F)
val PrimaryForestGreenLight = Color(0xFFD5F2E4)
val AccentAmber = Color(0xFFF2B544)
val AccentAmberDark = Color(0xFF835500)
val SecondarySlate = Color(0xFF53665C)
val TertiaryBlue = Color(0xFF527A85)

// Financial & Semantic Status Tokens (Light Theme)
val IncomeGreenLight = Color(0xFF18794E)
val ExpenseRedLight = Color(0xFFB54735)
val WarningAmberLight = Color(0xFF835500)
val ErrorRedLight = Color(0xFFBA1A1A)
val InfoBlueLight = Color(0xFF245DB8)
val NeutralStatusLight = Color(0xFF536159)

// Financial & Semantic Status Tokens (Dark Theme)
val IncomeGreenDark = Color(0xFF70D6A3)
val ExpenseRedDark = Color(0xFFFFB4A8)
val WarningAmberDark = Color(0xFFF2C66D)
val ErrorRedDark = Color(0xFFFFB4AB)
val InfoBlueDark = Color(0xFFA9C7FF)
val NeutralStatusDark = Color(0xFFB8C5BB)

// Backward-compatible aliases for existing composable references
val IncomeGreen = IncomeGreenLight
val ExpenseRed = ExpenseRedLight
val NeutralGray = NeutralStatusLight
val GreenPrimary = PrimaryForestGreen
val GreenSecondary = SecondarySlate
val GreenTertiary = TertiaryBlue
val DarkGreenPrimary = Color(0xFF78D5B0)
val DarkGreenSecondary = Color(0xFFB8C5BB)
val DarkBackground = Color(0xFF121714)
val DarkSurface = Color(0xFF1A211D)

@Immutable
data class KharchaSemanticColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val subtleBorder: Color,
    val accentAmber: Color
)

val LightKharchaSemanticColors = KharchaSemanticColors(
    income = IncomeGreenLight,
    onIncome = Color.White,
    incomeContainer = Color(0xFFD5F2E4),
    expense = ExpenseRedLight,
    onExpense = Color.White,
    expenseContainer = Color(0xFFFFDAD6),
    warning = WarningAmberLight,
    onWarning = Color.White,
    warningContainer = Color(0xFFFFE082),
    error = ErrorRedLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    info = InfoBlueLight,
    onInfo = Color.White,
    infoContainer = Color(0xFFD7E3FF),
    subtleBorder = Color(0xFFE1E6E1),
    accentAmber = AccentAmber
)

val DarkKharchaSemanticColors = KharchaSemanticColors(
    income = IncomeGreenDark,
    onIncome = Color(0xFF003822),
    incomeContainer = Color(0xFF005234),
    expense = ExpenseRedDark,
    onExpense = Color(0xFF680003),
    expenseContainer = Color(0xFF93000A),
    warning = WarningAmberDark,
    onWarning = Color(0xFF452B00),
    warningContainer = Color(0xFF633F00),
    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    info = InfoBlueDark,
    onInfo = Color(0xFF002F65),
    infoContainer = Color(0xFF00458E),
    subtleBorder = Color(0xFF354239),
    accentAmber = WarningAmberDark
)
