package com.sijan.kharcha.domain.model

enum class TaxCategory(val annualCeilingPaisa: Long?) {
    LIFE_INSURANCE(4000000L),    // Rs. 40,000 annual ceiling
    HEALTH_INSURANCE(2000000L),  // Rs. 20,000 annual ceiling
    MEDICAL(null),               // Medical expenses credit
    RETIREMENT(null),            // CIT / SSF / EPF contributions
    DONATION(null),              // Tax-exempt donations
    OTHER(null)
}
