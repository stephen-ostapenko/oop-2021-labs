package ru.senin.kotlin.net

enum class Protocol {
    HTTP, WEBSOCKET, UDP
}

fun Protocol.getUrlPrefix(): String {
    return when (this) {
        Protocol.HTTP -> "http"
        Protocol.WEBSOCKET -> "ws"
        Protocol.UDP -> "udp"
    }
}

fun String.toProtocol(): Protocol {
    return enumValueOf(this)
}

data class UserAddress(
    val protocol: Protocol,
    val host: String,
    val port: Int
) {
    override fun toString(): String {
        return "${protocol.getUrlPrefix()}://${host}:${port}"
    }
}

data class UserInfo(val name: String, val address: UserAddress)

data class Message(val user: String, val text: String)

fun checkUserName(name: String) = """^[a-zA-Z0-9-_.]+$""".toRegex().find(name)