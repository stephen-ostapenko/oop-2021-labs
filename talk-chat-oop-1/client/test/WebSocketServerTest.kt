package ru.senin.kotlin.net

import ru.senin.kotlin.net.client.WsChatClient
import ru.senin.kotlin.net.server.WsChatServer

class WebSocketServerTest : ServerTest() {
    override val port = 8082
    override val server = WsChatServer(host, port)
    override val client = WsChatClient(host, port)
}