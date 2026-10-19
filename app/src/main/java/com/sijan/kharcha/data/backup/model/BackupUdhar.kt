package com.sijan.kharcha.data.backup.model

import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharType
import kotlinx.serialization.Serializable

@Serializable
data class BackupUdhar(
    val id: Long,
    val personName: String,
    val phoneNumber: String? = null,
    val type: UdharType,
    val amountPaisa: Long,
    val settledPaisa: Long = 0L,
    val dueDate: Long? = null,
    val notes: String = "",
    val status: UdharStatus = UdharStatus.PENDING,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
