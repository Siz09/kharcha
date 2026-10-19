package com.sijan.kharcha.domain.parser.model

data class BoundingBox(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2

    fun isRightOf(other: BoundingBox, verticalTolerancePx: Int = 30): Boolean {
        val sameVerticalBand = (top <= other.bottom && bottom >= other.top) ||
            kotlin.math.abs(centerY - other.centerY) <= verticalTolerancePx
        val positionedToRight = left >= other.left + (other.width / 4)
        return sameVerticalBand && positionedToRight
    }

    fun isBelow(other: BoundingBox, horizontalTolerancePx: Int = 60): Boolean {
        val sameHorizontalBand = (left <= other.right && right >= other.left) ||
            kotlin.math.abs(centerX - other.centerX) <= horizontalTolerancePx
        val positionedBelow = top >= other.centerY
        return sameHorizontalBand && positionedBelow
    }

    fun horizontalDistance(other: BoundingBox): Int = left - other.right

    fun verticalDistance(other: BoundingBox): Int = top - other.bottom
}

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
