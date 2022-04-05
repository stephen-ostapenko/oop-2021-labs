package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.HttpApi
import ru.senin.kotlin.net.Message
import java.net.InetSocketAddress

abstract class ChatClient(curHost: String, curPort: Int) {
    protected val host: String = curHost
    protected val port: Int = curPort
    protected val objectMapper = jacksonObjectMapper()

    abstract fun sendMessage(message: Message)
}

class HttpChatClient(host: String, port: Int) : ChatClient(host, port) {
    private val chatApi: HttpApi = Retrofit.Builder()
            .baseUrl("http://$host:$port/")
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build().create(HttpApi::class.java)

    override fun sendMessage(message: Message) {
        val response = chatApi.sendMessage(message).execute()
        if (!response.isSuccessful) {
            println("${response.code()} ${response.message()}")
        }
    }
}

class WsChatClient(host: String, port: Int) : ChatClient(host, port) {
    private val client = HttpClient {
        install(WebSockets)
    }

    override fun sendMessage(message: Message) {
        @Suppress("BlockingMethodInNonBlockingContext")
        runBlocking {
            client.ws(host = host, port = port, path = "/v1/ws/message") {
                val jsonString = objectMapper.writeValueAsString(message)
                send(jsonString)
            }
        }
    }
}

class UdpChatClient(host: String, port: Int) : ChatClient(host, port) {
    private val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).udp()

    override fun sendMessage(message: Message) {
        @Suppress("BlockingMethodInNonBlockingContext")
        runBlocking {
            val jsonString = objectMapper.writeValueAsString(message)
            val packet = BytePacketBuilder().append(jsonString).build()
            socketBuilder.connect(InetSocketAddress(host, port))
                    .outgoing.send(Datagram(packet, InetSocketAddress(host, port)))
        }
    }
}