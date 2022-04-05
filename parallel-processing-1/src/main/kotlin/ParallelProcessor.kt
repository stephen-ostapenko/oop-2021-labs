package com.h0tk3y.spbsu.parallel

/**
 * Return your implementation of [ParallelProcessor]. To enroll for the advanced task, make it implement
 * [AdvancedParallelProcessor] as well.
 */
fun solution(numberOfThreads: Int): ParallelProcessor = OurParallelProcessor(numberOfThreads)

class OurParallelProcessor(n: Int) : AdvancedParallelProcessor {
    override val numberOfThreads: Int = n

    private class Index {
        private var cur = 0

        @Synchronized
        fun getAndIncrement(): Int {
            return cur++
        }
    }

    private fun startAllThreads(threads: List<Thread>) {
        for (thr in threads) {
            thr.start()
        }
    }

    private fun joinAllThreads(threads: List<Thread>) {
        for (thr in threads) {
            thr.join()
        }
    }

    private fun schedule(action: () -> Unit) {
        val threads = List(numberOfThreads) { Thread(action) }
        startAllThreads(threads)
        joinAllThreads(threads)
    }

    override fun <T : Any> minWithOrNull(list: List<T>, comparator: Comparator<T>): T? {
        if (list.isEmpty()) return null
        val index = Index()
        var result: T = list[0]

        schedule {
            var resultInThread: T = list[0]
            while (true) {
                val currentIndex = index.getAndIncrement()
                if (currentIndex >= list.size) {
                    synchronized(list) {
                        if (comparator.compare(result, resultInThread) > 0) {
                            result = resultInThread
                        }
                    }
                    return@schedule
                }
                if (comparator.compare(resultInThread, list[currentIndex]) > 0) {
                    resultInThread = list[currentIndex]
                }
            }
        }

        return result
    }

    override fun <T> all(list: List<T>, predicate: (T) -> Boolean): Boolean {
        val index = Index()
        var result = true

        schedule {
            while (result) {
                val currentIndex = index.getAndIncrement()
                if (currentIndex >= list.size) {
                    return@schedule
                }
                if (!predicate(list[currentIndex])) {
                    result = false
                }
            }
        }

        return result
    }

    override fun <T> filter(list: List<T>, predicate: (T) -> Boolean): List<T> {
        val index = Index()
        val filtered: MutableList<Boolean> = MutableList(list.size) { false }

        schedule {
            while (true) {
                val currentIndex = index.getAndIncrement()
                if (currentIndex >= list.size) {
                    return@schedule
                }
                filtered[currentIndex] = predicate(list[currentIndex])
            }
        }

        return list.filterIndexed { idx, _ ->  filtered[idx] }
    }

    override fun <T, R> map(list: List<T>, function: (T) -> R): List<R> {
        val index = Index()
        val resultOrNull = MutableList<R?>(list.size) { null }

        schedule {
            while (true) {
                val currentIndex = index.getAndIncrement()
                if (currentIndex >= list.size) {
                    return@schedule
                }
                resultOrNull[currentIndex] = function(list[currentIndex])
            }
        }

        return List(list.size) { resultOrNull[it] ?: error("Element was not mapped") }
    }

    override fun <T> joinToString(list: List<T>, separator: String): String {
        val index = Index()
        val stringed: MutableList<String> = MutableList(list.size) { "" }

        schedule {
            while (true) {
                val currentIndex = index.getAndIncrement()
                if (currentIndex >= list.size) {
                    return@schedule
                }
                stringed[currentIndex] = list[currentIndex].toString()
            }
        }

        var result = ""
        for (idx in list.indices) {
            if (idx > 0) result += separator
            result += stringed[idx]
        }

        return result
    }
}

/**
 * Basic task: implement parallel processing of a list of elements, with the same semantics of the operations as the
 * standard library's extensions.
 *
 * The implementation should use [numberOfThreads] threads when processing the list in order to reduce the latency.
 *
 * It is also safe to assume that the list is not modified concurrently when the operations are running.
 *
 * You should not assume that the comparators and functions passed to the operations work fast.
 *
 * In this task, you are not allowed to use any declarations from the `java.util.concurrent` package nor any other
 * high-level concurrency utilities from any libraries. The goal is to play with low-level primitives.
 */
interface ParallelProcessor {
    val numberOfThreads: Int

    fun <T : Any> minWithOrNull(list: List<T>, comparator: Comparator<T>): T?
    fun <T : Any> maxWithOrNull(list: List<T>, comparator: Comparator<T>): T? =
        minWithOrNull(list, comparator.reversed())

    fun <T> all(list: List<T>, predicate: (T) -> Boolean): Boolean
    fun <T> any(list: List<T>, predicate: (T) -> Boolean): Boolean =
        !all(list) { !predicate(it) }
}

fun <T : Comparable<T>> ParallelProcessor.minWithOrNull(list: List<T>) = this.minWithOrNull(list, naturalOrder())
fun <T : Comparable<T>> ParallelProcessor.maxWithOrNull(list: List<T>) = this.maxWithOrNull(list, naturalOrder())

/**
 * Advanced task: implement additional operations.
 */
interface AdvancedParallelProcessor : ParallelProcessor {
    fun <T> filter(list: List<T>, predicate: (T) -> Boolean): List<T>
    fun <T, R> map(list: List<T>, function: (T) -> R): List<R>
    fun <T> joinToString(list: List<T>, separator: String): String
}