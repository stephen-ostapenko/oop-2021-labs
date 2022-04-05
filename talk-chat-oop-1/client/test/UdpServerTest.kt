package ru.senin.kotlin.net

import ru.senin.kotlin.net.client.UdpChatClient
import ru.senin.kotlin.net.server.UdpChatServer

class UdpServerTest : ServerTest() {
    override val port = 3000
    override val server = UdpChatServer(host, port)
    override val client = UdpChatClient(host, port)
}