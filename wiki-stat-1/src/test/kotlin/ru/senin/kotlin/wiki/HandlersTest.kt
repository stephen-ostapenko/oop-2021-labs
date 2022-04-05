package ru.senin.kotlin.wiki

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class HandlersTest {

    var testArticle = Article()
    var anotherTestArticle = Article()
    private val path = "src/test/resources/testData/"

    init {
        testArticle.title.append(
            "Заголовок, чтобы протестировать работу подсчета статистики в работе по подсчету статистики"
        )
        testArticle.text.append(
            "Здесь должен быть какой-то текст текст с повторяющимися словами словами словами, " +
                    "чтобы проверить подсчет статистики"
        )
        testArticle.bytes = "913"
        testArticle.date.append("2004-08-09T04:36:57Z")

        anotherTestArticle.title.append("Простой заголовок про простой заголовок заголовок")
        anotherTestArticle.text.append("#REDIRECT [[Заглавная страница]]")
        anotherTestArticle.bytes = "49"
        anotherTestArticle.date.append("2004-08-09T04:36:57Z")
    }

    @Test
    fun `test Handlers on easy article`() {
        val handler = StatHandler(4, 1)
        handler.submitArticle(anotherTestArticle).get()
        handler.join(false)
        val expected = File(path + "HandlerTestData1.txt").readLines()
        val actual = handler.getReport()
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun `test Handlers on not so easy article`() {
        val handler = StatHandler(4, 1)
        handler.submitArticle(testArticle).get()
        handler.join(false)
        val expected = File(path + "HandlerTestData2.txt").readLines()
        val actual = handler.getReport()
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun `test several articles statistics`() {
        val handler = StatHandler(4, 1)
        handler.submitArticle(testArticle).get()
        handler.submitArticle(anotherTestArticle).get()
        handler.join(false)
        val expected = File(path + "HandlerTestData3.txt").readLines()
        val actual = handler.getReport()
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}