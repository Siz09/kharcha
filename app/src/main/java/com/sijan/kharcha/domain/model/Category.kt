package com.sijan.kharcha.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val type: EntryType
)
