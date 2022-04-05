package ru.kotlin.senin

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import kotlin.test.junit5.JUnit5Asserter.assertEquals

class Test {
    @FlowPreview
    @Test
    fun testLoadResults() = runBlockingTest {
        val startTime = currentTime
        var index = 0
        loadResults(MockGithubService, testRequestData) { results, _, _ ->
            val expected = progressResults[index++]
            val time = currentTime - startTime

            assertEquals("Wrong result on step $index after time $time:",
                expected.login2Stat, results)
        }
    }
}