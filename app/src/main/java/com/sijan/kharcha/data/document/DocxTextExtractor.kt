package com.sijan.kharcha.data.document

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class DocxContent(
    val fullText: String,
    val paragraphs: List<String>
)

object DocxTextExtractor {

    fun extractText(file: File): Result<DocxContent> {
        return runCatching {
            FileInputStream(file).use { input ->
                extractText(input).getOrThrow()
            }
        }
    }

    fun extractText(inputStream: InputStream): Result<DocxContent> {
        return runCatching {
            val zip = ZipInputStream(inputStream)
            var docEntry = zip.nextEntry
            var foundDocumentXml = false
            val paragraphs = mutableListOf<String>()
            val currentParagraph = StringBuilder()

            while (docEntry != null) {
                if (docEntry.name.equals("word/document.xml", ignoreCase = true)) {
                    foundDocumentXml = true
                    val parser = createParser()
                    parser.setInput(zip, "UTF-8")
                    var eventType = parser.eventType

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                val name = parser.name
                                if (name == "t" || name.endsWith(":t")) {
                                    val text = parser.nextText()
                                    currentParagraph.append(text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                val name = parser.name
                                if (name == "p" || name.endsWith(":p")) {
                                    val paragraphText = currentParagraph.toString().trim()
                                    if (paragraphText.isNotEmpty()) {
                                        paragraphs.add(paragraphText)
                                    }
                                    currentParagraph.clear()
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    val remaining = currentParagraph.toString().trim()
                    if (remaining.isNotEmpty()) {
                        paragraphs.add(remaining)
                    }
                    break
                }
                zip.closeEntry()
                docEntry = zip.nextEntry
            }

            if (!foundDocumentXml) {
                throw IllegalArgumentException("Not a valid DOCX file: missing word/document.xml")
            }

            DocxContent(
                fullText = paragraphs.joinToString("\n"),
                paragraphs = paragraphs
            )
        }
    }

    private fun createParser(): XmlPullParser {
        return try {
            Xml.newPullParser()
        } catch (_: Throwable) {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            factory.newPullParser()
        }
    }
}
