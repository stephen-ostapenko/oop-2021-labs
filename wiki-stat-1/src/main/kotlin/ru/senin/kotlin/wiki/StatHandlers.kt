package ru.senin.kotlin.wiki

import java.io.File
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

open class StatHandler(threadsCount: Int, filesCount: Int) {
    private val threadPool = Executors.newFixedThreadPool(threadsCount * filesCount * 3)
    val latch = CountDownLatch(filesCount)

    private val titleStatHandler = TitleStatHandler()
    private val textStatHandler = TextStatHandler()
    private val sizeStatHandler = SizeStatHandler()
    private val dateStatHandler = DateStatHandler()

    fun join(waitForAllFiles: Boolean = true) {
        if (waitForAllFiles) {
            latch.await()
        }
        threadPool.shutdown()

        while (!threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
            // do nothing
        }
    }

    class CallablePoolTask(private val action: () -> List<String>) : Callable<List<String>> {
        override fun call(): List<String> {
            return action()
        }
    }

    fun submitFile(file: File) {
        threadPool.submit { parseFile(file) }
    }

    open fun submitArticle(article: Article): Future<*> {
        return threadPool.submit { addArticle(article) }
    }

    private fun addArticle(article: Article) {
        val futureTitle = threadPool.submit(CallablePoolTask { article.getParsedTitle() })
        val futureText = threadPool.submit(CallablePoolTask { article.getParsedText() })

        threadPool.submit { sizeStatHandler.updateCounter(article.getBytes()) }
        threadPool.submit { dateStatHandler.updateCounter(article.getYear()) }

        threadPool.submit { titleStatHandler.updateCounter(futureTitle.get()) }
        threadPool.submit { textStatHandler.updateCounter(futureText.get()) }
    }

    fun getReport(): List<String> {
        val result = mutableListOf("Топ-${MOST_FREQ_TITLE_WORDS_COUNT} слов в заголовках статей:")
        titleStatHandler.getMostFrequentWords().forEach {
            result.add("${it.count} ${it.word}")
        }
        result.add("")

        result.add("Топ-${MOST_FREQ_TEXT_WORDS_COUNT} слов в статьях:")
        textStatHandler.getMostFrequentWords().forEach {
            result.add("${it.count} ${it.word}")
        }
        result.add("")

        result.add("Распределение статей по размеру:")
        sizeStatHandler.getPagesCountBySize().forEach {
            result.add("${it.first} ${it.second}")
        }
        result.add("")

        result.add("Распределение статей по времени:")
        dateStatHandler.getPagesCountByYear().forEach {
            val year = it.first.toString()
            result.add("${year.padStart(4, '0')} ${it.second}")
        }
        result.add("")

        return result
    }

    private data class WordStat(val word: String, val count: Int)

    private abstract class TitleAndTextStatHandlerImplementation {
        private val wordStatComparator = Comparator { a: WordStat, b: WordStat ->
            compareValuesBy(a, b, { -it.count }, { it.word })
                .takeIf { it != 0 } ?: error("similar words in ConcurrentHashMap")
        }

        val occurrenceCounter = ConcurrentHashMap<String, Int>()

        fun updateCounter(words: List<String>) {
            words.forEach {
                occurrenceCounter.computeIfAbsent(it) { 0 }
                occurrenceCounter.computeIfPresent(it) { _: String, count: Int -> count.inc() }
            }
        }

        fun getMostFrequentWords(nWord: Int): List<WordStat> {
            val queue = PriorityQueue(Collections.reverseOrder(wordStatComparator))

            occurrenceCounter.entries.forEach {
                queue.add(WordStat(it.key, it.value))
                if (queue.size > nWord) {
                    queue.remove()
                }
            }

            val result = mutableListOf<WordStat>()
            while (queue.isNotEmpty()) {
                result.add(queue.remove())
            }

            return result.reversed()
        }
    }

    private class TitleStatHandler : TitleAndTextStatHandlerImplementation() {
        fun getMostFrequentWords(): List<WordStat> {
            return getMostFrequentWords(MOST_FREQ_TITLE_WORDS_COUNT)
        }
    }

    private class TextStatHandler : TitleAndTextStatHandlerImplementation() {
        fun getMostFrequentWords(): List<WordStat> {
            return getMostFrequentWords(MOST_FREQ_TEXT_WORDS_COUNT)
        }
    }

    private class SizeStatHandler {
        val sizeCounter = List(LOG_MAX_PAGE_SIZE) { AtomicInteger(0) }

        fun updateCounter(bytes: Int) {
            var currentSize = 10
            var logCurrentSize = 0
            while (currentSize <= bytes) {
                currentSize *= 10
                logCurrentSize++
            }

            sizeCounter[logCurrentSize].incrementAndGet()
        }

        fun getPagesCountBySize(): List<Pair<Int, Int>> {
            return sizeCounter.mapIndexed { index, count -> index to count.get() }
                .dropLastWhile { it.second == 0 }.dropWhile { it.second == 0 }
        }
    }

    private class DateStatHandler {
        val yearCounter = List(MAX_YEAR) { AtomicInteger(0) }

        fun updateCounter(year: Int) {
            yearCounter[year].incrementAndGet()
        }

        fun getPagesCountByYear(): List<Pair<Int, Int>> {
            return yearCounter.mapIndexed { index, count -> index to count.get() }
                .dropLastWhile { it.second == 0 }.dropWhile { it.second == 0 }
        }
    }
}