package ru.senin.kotlin.wiki

import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.bzip2.BZip2Utils
import org.xml.sax.helpers.DefaultHandler
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Future
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

fun StatHandler.parseFile(file: File) {
    val sentArticles = mutableListOf<Future<*>>()

    try {
        val pageTag = "page"
        val titleTag = "title"
        val textTag = "text"
        val revisionTag = "revision"
        val timestampTag = "timestamp"

        var article = Article()

        val parserFactory: SAXParserFactory = SAXParserFactory.newInstance()
        val saxParser: SAXParser = parserFactory.newSAXParser()
        val defaultHandler = object : DefaultHandler() {
            var activeTags: Int = 0
            var numberOfWellFormedXML: Int = 0
            var numberOfIncorrectTags: Int = 0

            var isPageActive: Boolean = false
            var isRevisionActive: Boolean = false
            var isCorrectNesting: Boolean = true
            var currentValue = ""
            var currentElement = false

            override fun startElement(uri: String, localName: String, qName: String, attributes: org.xml.sax.Attributes) {
                activeTags++
                if (numberOfWellFormedXML != 0) throw WrongNumberOfXMLFilesException()

                currentElement = true
                currentValue = qName

                when (qName) {
                    pageTag -> {
                        article = Article()
                        isPageActive = true
                    }
                    revisionTag -> {
                        if (isPageActive and isCorrectNesting)
                            isRevisionActive = true
                    }
                    textTag -> {
                        if (isRevisionActive and isCorrectNesting)
                            article.bytes = attributes.getValue("", "bytes")
                    }
                    else -> {
                        if (isPageActive and (qName != titleTag) and (qName != timestampTag)) {
                            isCorrectNesting = false
                            numberOfIncorrectTags++
                        }
                    }
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                activeTags--
                if (activeTags == 0) numberOfWellFormedXML++

                currentElement = false
                currentValue = ""
                when (qName) {
                    pageTag -> {
                        if (article.check() and isCorrectNesting) {
                            sentArticles.add(submitArticle(article))
                        }
                        isPageActive = false
                    }
                    revisionTag -> {
                        if (isPageActive and isCorrectNesting)
                            isRevisionActive = false
                    }
                    else -> {
                        if (isPageActive and (qName != titleTag) and (qName != textTag) and (qName != timestampTag)) {
                            numberOfIncorrectTags--
                            if (numberOfIncorrectTags == 0)
                                isCorrectNesting = true
                        }
                    }
                }

                if (!isPageActive and isRevisionActive) throw NotWellFormedXMLException()
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (currentElement) {
                    when (currentValue) {
                        titleTag -> {
                            if (isPageActive and isCorrectNesting)
                                article.title.append(ch, start, length)
                        }
                        textTag -> {
                            if (isRevisionActive and isCorrectNesting) {
                                article.text.append(ch, start, length)
                            }
                        }
                        timestampTag -> {
                            if (isRevisionActive and isCorrectNesting)
                                article.date.append(ch, start, length)
                        }
                    }
                }
            }
        }
        if (!BZip2Utils.isCompressedFilename(file.name)) throw NotBzip2FileException()

        val fin = FileInputStream(file)
        val bfin = BufferedInputStream(fin)
        val decompressedInputStream = CompressorStreamFactory()
            .createCompressorInputStream(CompressorStreamFactory.BZIP2, bfin)
        saxParser.parse(decompressedInputStream, defaultHandler)

    } catch (e : WrongNumberOfXMLFilesException) {
        println("Error! ${e.message}")
        throw e
    } catch (e : NotWellFormedXMLException) {
        println("Error! ${e.message}")
        throw e
    } catch (e : NotBzip2FileException) {
        println("Error! ${e.message}")
        throw e
    } finally {
        sentArticles.forEach {
            it.get()
        }

        latch.countDown()
        println("File ${file.name} done")
    }
}

class WrongNumberOfXMLFilesException : IllegalArgumentException("Too much xml files in one file")
class NotWellFormedXMLException : IllegalArgumentException("Not well-formed xml file")
class NotBzip2FileException : IllegalArgumentException("Not bzip2 file")