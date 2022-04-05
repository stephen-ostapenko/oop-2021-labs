package ru.senin.kotlin.wiki

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.argument
import com.apurebase.arkenv.parse
import java.io.File
import kotlin.time.measureTime

const val LOG_MAX_PAGE_SIZE = 9
const val MAX_YEAR = 3021
const val MOST_FREQ_TITLE_WORDS_COUNT = 300
const val MOST_FREQ_TEXT_WORDS_COUNT = 300

class Parameters : Arkenv() {
    val inputs: List<File> by argument("--inputs") {
        description = "Path(s) to bzip2 archived XML file(s) with WikiMedia dump. Comma separated."
        mapping = {
            it.split(",").map{ name -> File(name) }
        }
        validate("File does not exist or cannot be read") {
            it.all { file -> file.exists() && file.isFile && file.canRead() }
        }
    }

    val output: String by argument("--output") {
        description = "Report output file"
        defaultValue = { "statistics.txt" }
    }

    val threads: Int by argument("--threads") {
        description = "Number of threads"
        defaultValue = { 4 }
        validate("Number of threads must be in 1..32") {
            it in 1..32
        }
    }
}

fun runAllFiles(inputs: List<File>, outputFileName: String, threadsCount: Int) {
    val handler = StatHandler(threadsCount, inputs.size)

    inputs.forEach {
        handler.submitFile(it)
    }
    handler.join()

    File(outputFileName).writeText(handler.getReport().joinToString("\n"))
}

fun main(args: Array<String>) {
    try {
        val parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }

        val duration = measureTime {
            runAllFiles(parameters.inputs, parameters.output, parameters.threads)
        }
        println("Time: ${duration.inMilliseconds} ms")
    }
    catch (e: Exception) {
        println("Error! ${e.message}")
        throw e
    }
}
