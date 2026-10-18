package com.sijan.kharcha.domain.parser.model

data class BoundingBox(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
)

data class OcrLine(
    val text: String,
    val boundingBox: BoundingBox? = null
)

data class OcrBlock(
    val text: String,
    val lines: List<OcrLine> = emptyList(),
    val boundingBox: BoundingBox? = null
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock> = emptyList()
) {
    val lines: List<String> by lazy {
        val extracted = blocks.flatMap { block -> block.lines.map { it.text } }
        if (extracted.isNotEmpty()) extracted else fullText.lines()
    }
}
