package com.sijan.kharcha.domain.model

enum class UdharStatus {
    PENDING,   // No settlement yet
    PARTIAL,   // Partially settled (settledPaisa < amountPaisa)
    SETTLED    // Fully settled (settledPaisa >= amountPaisa)
}
