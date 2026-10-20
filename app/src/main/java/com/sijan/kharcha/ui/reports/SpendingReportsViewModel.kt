package com.sijan.kharcha.ui.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.AnalyticsRepository
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SpendingReportsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val ledgerRepository: LedgerRepository,
    private val pdfStatementGenerator: PdfStatementGenerator
) : ViewModel() {

    private val now = LocalDate.now()
    val selectedYear = MutableStateFlow(now.year)
    val selectedMonth = MutableStateFlow(now.monthValue)

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    val analyticsSummary: StateFlow<SpendingAnalyticsSummary?> = combine(
        selectedYear,
        selectedMonth
    ) { y, m -> Pair(y, m) }.flatMapLatest { (y, m) ->
        analyticsRepository.getMonthlyAnalytics(y, m)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun onPreviousMonth() {
        if (selectedMonth.value == 1) {
            selectedYear.value -= 1
            selectedMonth.value = 12
        } else {
            selectedMonth.value -= 1
        }
    }

    fun onNextMonth() {
        if (selectedMonth.value == 12) {
            selectedYear.value += 1
            selectedMonth.value = 1
        } else {
            selectedMonth.value += 1
        }
    }

    fun exportPdf(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val year = selectedYear.value
                val month = selectedMonth.value
                val summary = ledgerRepository.getMonthlySummary(year, month).first()
                val categories = ledgerRepository.getAllCategories().first()

                val startEpoch = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val endEpoch = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val entries = ledgerRepository.getFilteredEntries(
                    startDate = startEpoch,
                    endDate = endEpoch
                ).first()

                val file = withContext(Dispatchers.IO) {
                    pdfStatementGenerator.generateMonthlyStatement(
                        year = year,
                        month = month,
                        summary = summary,
                        entries = entries,
                        categories = categories
                    )
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "Kharcha Monthly Statement").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } finally {
                _isGeneratingPdf.value = false
                onComplete()
            }
        }
    }
}
