package com.sijan.kharcha.ui.udhar

import com.sijan.kharcha.domain.model.UdharRecord
import com.sijan.kharcha.domain.model.UdharSettlement
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharSummary

enum class UdharTabFilter {
    ALL,
    LENT,
    BORROWED
}

data class UdharUiState(
    val tabFilter: UdharTabFilter = UdharTabFilter.ALL,
    val statusFilter: UdharStatus? = null,
    val summary: UdharSummary = UdharSummary(),
    val items: List<UdharRecord> = emptyList(),
    val selectedUdharForSettlement: UdharRecord? = null,
    val selectedUdharForHistory: UdharRecord? = null,
    val settlementsForHistory: List<UdharSettlement> = emptyList(),
    val showAddDialog: Boolean = false,
    val udharToDelete: UdharRecord? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
