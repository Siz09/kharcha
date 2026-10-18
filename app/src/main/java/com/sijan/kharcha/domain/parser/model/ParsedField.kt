package com.sijan.kharcha.domain.parser.model

data class ParsedField<T>(
    val value: T?,
    val raw: String = "",
    val status: ValidationStatus = ValidationStatus.VALID,
    val errorMessage: String? = null
)
