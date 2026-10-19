package com.sijan.kharcha.domain.parser.layout

import com.sijan.kharcha.domain.parser.model.OcrLine
import com.sijan.kharcha.domain.parser.model.OcrResult

class SpatialLayoutExtractor(private val ocr: OcrResult) {

    val linesWithBoxes: List<OcrLine> = ocr.blocks.flatMap { it.lines }
    val hasBoundingBoxes: Boolean = linesWithBoxes.any { it.boundingBox != null }

    fun findAnchor(anchorRegex: Regex): OcrLine? {
        if (hasBoundingBoxes) {
            val matched = linesWithBoxes.firstOrNull { anchorRegex.matches(it.text.trim()) }
            if (matched != null) return matched
        }
        val textMatch = ocr.lines.firstOrNull { anchorRegex.matches(it.trim()) } ?: return null
        return OcrLine(text = textMatch, boundingBox = null)
    }

    fun findValueRightOf(anchor: OcrLine, predicate: ((OcrLine) -> Boolean)? = null): OcrLine? {
        val anchorBox = anchor.boundingBox ?: return null
        return linesWithBoxes
            .asSequence()
            .filter { it != anchor && it.boundingBox != null && it.text.isNotBlank() }
            .filter { it.boundingBox!!.isRightOf(anchorBox) }
            .filter { predicate == null || predicate(it) }
            .minByOrNull { it.boundingBox!!.horizontalDistance(anchorBox) }
    }

    fun findValueBelow(anchor: OcrLine, predicate: ((OcrLine) -> Boolean)? = null): OcrLine? {
        val anchorBox = anchor.boundingBox ?: return null
        return linesWithBoxes
            .asSequence()
            .filter { it != anchor && it.boundingBox != null && it.text.isNotBlank() }
            .filter { it.boundingBox!!.isBelow(anchorBox) }
            .filter { predicate == null || predicate(it) }
            .minByOrNull { it.boundingBox!!.verticalDistance(anchorBox) }
    }

    fun <T> extractField(
        anchorRegex: Regex,
        valueExtractor: (String) -> T?
    ): Pair<T, String>? {
        if (hasBoundingBoxes) {
            val anchor = findAnchor(anchorRegex)
            if (anchor != null) {
                // 1. Try inline on anchor line
                val inlineCandidate = anchor.text.replace(anchorRegex, "").trim().removePrefix(":").removePrefix("-").trim()
                val inlineValue = valueExtractor(inlineCandidate)
                if (inlineValue != null) {
                    return inlineValue to anchor.text
                }

                // 2. Try spatially to the right
                val rightCandidate = findValueRightOf(anchor)
                if (rightCandidate != null) {
                    val rightValue = valueExtractor(rightCandidate.text.trim())
                    if (rightValue != null) {
                        return rightValue to rightCandidate.text
                    }
                }

                // 3. Try spatially below
                val belowCandidate = findValueBelow(anchor)
                if (belowCandidate != null) {
                    val belowValue = valueExtractor(belowCandidate.text.trim())
                    if (belowValue != null) {
                        return belowValue to belowCandidate.text
                    }
                }
            }
        }

        // Sequential fallback across ocr.lines
        val rawLines = ocr.lines.map { it.trim() }.filter { it.isNotEmpty() }
        for (i in rawLines.indices) {
            val line = rawLines[i]
            if (anchorRegex.matches(line)) {
                val inlineCandidate = line.replace(anchorRegex, "").trim().removePrefix(":").removePrefix("-").trim()
                val inlineValue = valueExtractor(inlineCandidate)
                if (inlineValue != null) {
                    return inlineValue to line
                }

                if (i + 1 < rawLines.size) {
                    val nextLine = rawLines[i + 1]
                    val nextValue = valueExtractor(nextLine)
                    if (nextValue != null) {
                        return nextValue to nextLine
                    }
                }
            }
        }

        return null
    }
}
