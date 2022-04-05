package ru.senin.kotlin.net

import ru.senin.kotlin.net.client.HttpChatClient
import ru.senin.kotlin.net.server.HttpChatServer

class HttpServerTest : ServerTest() {
    override val port = 8080
    override val server = HttpChatServer(host, port)
    override val client = HttpChatClient(host, port)
}