package ru.senin.kotlin.wiki

import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestStatHandler : StatHandler(1, 1) {
    val articleList = mutableListOf<Article>()

    override fun submitArticle(article: Article): Future<*> {
        articleList.add(article)
        return super.submitArticle(article)
    }
}

class FileParserTest {
    companion object {
        private const val TEST_DATA_PATH = "./src/test/resources/testData/"
    }

    fun invokeTest(testName: String) {
        val handler = TestStatHandler()

        handler.submitFile(File("$TEST_DATA_PATH$testName.xml.bz2"))
        handler.join()

        val expected = File("$TEST_DATA_PATH$testName.expected.txt").readLines()
        val actual = handler.articleList.map {
            listOf(
                it.title.toString(),
                it.text.toString(),
                it.date.toString(),
                it.bytes
            )
        }.flatten()

        assertTrue { expected == actual }
    }

    @Test
    fun `test xml parsing 1`() {
        invokeTest("test-xml-1")
    }

    @Test
    fun `test xml parsing 2`() {
        invokeTest("test-xml-2")
    }

    @Test
    fun `test xml parsing 3`() {
        invokeTest("test-xml-3")
    }
}