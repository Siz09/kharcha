package com.sijan.kharcha.domain.parser.template

import com.sijan.kharcha.domain.parser.model.OcrResult
import java.util.Locale

enum class TemplateSource(val templateId: String, val displayName: String) {
    ESEWA("esewa_slip_v1", "eSewa"),
    KHALTI("khalti_receipt_v1", "Khalti"),
    FONEPAY("fonepay_qr_v1", "Fonepay"),
    GENERIC("generic_receipt_v1", "Receipt")
}

interface ReceiptTemplate {
    val template: TemplateSource
    fun matches(ocr: OcrResult): Boolean
}

object EsewaTemplate : ReceiptTemplate {
    override val template: TemplateSource = TemplateSource.ESEWA

    override fun matches(ocr: OcrResult): Boolean {
        val lower = ocr.fullText.lowercase(Locale.ROOT)
        return lower.contains("esewa") ||
            (lower.contains("transaction id") && lower.contains("amount") && lower.contains("paid to"))
    }
}

object KhaltiTemplate : ReceiptTemplate {
    override val template: TemplateSource = TemplateSource.KHALTI

    override fun matches(ocr: OcrResult): Boolean {
        val lower = ocr.fullText.lowercase(Locale.ROOT)
        return lower.contains("khalti") ||
            (lower.contains("transaction id") && lower.contains("amount") && lower.contains("cashback"))
    }
}

object FonepayTemplate : ReceiptTemplate {
    override val template: TemplateSource = TemplateSource.FONEPAY

    override fun matches(ocr: OcrResult): Boolean {
        val lower = ocr.fullText.lowercase(Locale.ROOT)
        return lower.contains("fonepay") ||
            (lower.contains("trace id") || lower.contains("retrieval reference"))
    }
}

object GenericReceiptTemplate : ReceiptTemplate {
    override val template: TemplateSource = TemplateSource.GENERIC

    override fun matches(ocr: OcrResult): Boolean {
        val lower = ocr.fullText.lowercase(Locale.ROOT)
        return lower.contains("total") || lower.contains("bill") || lower.contains("invoice") || lower.contains("pan")
    }
}

object TemplateRegistry {
    val templates: List<ReceiptTemplate> = listOf(
        EsewaTemplate,
        KhaltiTemplate,
        FonepayTemplate,
        GenericReceiptTemplate
    )

    fun detect(ocr: OcrResult): ReceiptTemplate? {
        return templates.firstOrNull { it.matches(ocr) }
    }
}
