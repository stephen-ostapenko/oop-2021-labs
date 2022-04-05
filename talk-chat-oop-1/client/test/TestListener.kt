package ru.senin.kotlin.net

import ru.senin.kotlin.net.server.ChatMessageListener

class TestListener : ChatMessageListener {
    val receivedMessages : MutableList<Message> = mutableListOf()

    override fun messageReceived(userName: String, text: String) {
        receivedMessages.add(Message(userName, text))
    }
}