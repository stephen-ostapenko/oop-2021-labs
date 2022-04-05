package ru.senin.kotlin.net

import org.junit.jupiter.api.*
import ru.senin.kotlin.net.client.ChatClient
import ru.senin.kotlin.net.server.ChatServer
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.test.Ignore
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ServerTest {
    val host = "127.0.0.1"
    abstract val port : Int
    private val listener = TestListener()
    abstract val server : ChatServer
    abstract val client : ChatClient

    private val message1 = Message("pupkin", "Hello, World!")
    private val message2 = Message("pupkin", "Bye, everybody!")
    private val message3 = Message("tester", "Psh, psh")
    private val message4 = Message("tester", "Can you hear me?")
    private val message5 = Message("tester", "Roger")
    private val message6 = Message("tester", "")

    @BeforeAll
    fun beforeAll() {
        server.setMessageListener(listener)
        thread {
            server.start()
        }
        sleep(10000L)
    }

    @BeforeEach
    fun deleteReceivedMessages() {
        listener.receivedMessages.clear()
    }

    @Ignore
    @Test
    fun `send one message`() {
        client.sendMessage(message1)
        sleep(1000L)
        assertEquals(listOf(message1), listener.receivedMessages)
    }

    @Ignore
    @Test
    fun `send several messages`() {
        client.sendMessage(message1)
        sleep(1000L)
        client.sendMessage(message2)
        sleep(1000L)
        client.sendMessage(message3)
        sleep(1000L)
        client.sendMessage(message4)
        sleep(1000L)
        client.sendMessage(message5)
        sleep(1000L)
        client.sendMessage(message6)
        sleep(1000L)
        assertEquals(listOf(message1, message2, message3, message4, message5, message6), listener.receivedMessages)
    }

    @AfterAll
    fun afterAll() {
        server.stop()
    }
}