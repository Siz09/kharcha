package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.parser.model.OcrResult
import com.sijan.kharcha.domain.parser.model.ParseResult

interface ReceiptParser {
    val sourceName: String
    fun canParse(ocr: OcrResult): Boolean
    fun parse(ocr: OcrResult): ParseResult
}
