package ru.senin.kotlin.wiki

class Article {
    var title = StringBuilder()
    var text = StringBuilder()
    var date = StringBuilder()
    var bytes: String? = null

    fun check(): Boolean = title.isNotEmpty() && text.isNotEmpty() && date.isNotEmpty() && (bytes != null)

    fun getParsedTitle(): List<String> {
        val reg = Regex("[^а-яА-Я]")
        return title.split(reg).filter { it.length >= 3 }.map { it.toLowerCase() }
    }

    fun getParsedText(): List<String> {
        val reg = Regex("[^а-яА-Я]")
        return text.split(reg).filter { it.length >= 3 }.map { it.toLowerCase() }
    }

    fun getYear(): Int {
        val yearInString: String = date.substring(0, 4) ?: throw NullDateException()
        return yearInString.toInt()
    }

    fun getBytes(): Int = bytes?.toInt() ?: throw NullBytesException()
}

class NullDateException : IllegalStateException("Trying to parse null date")
class NullBytesException : IllegalStateException("Trying to parse null bytes")