package ru.senin.kotlin.net

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.argument
import com.apurebase.arkenv.parse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.server.ChatServer
import ru.senin.kotlin.net.server.HttpChatServer
import ru.senin.kotlin.net.server.UdpChatServer
import ru.senin.kotlin.net.server.WsChatServer
import java.net.URL
import kotlin.concurrent.thread

val supportedProtocolsAndDefaultPorts = mapOf("HTTP" to 8080, "WEBSOCKET" to 8082, "UDP" to 3000)

class Parameters : Arkenv() {
    val name : String by argument("--name") {
        description = "Name of user"
    }

    val registryBaseUrl : String by argument("--registry") {
        description = "Base URL of User Registry"
        defaultValue = { "http://localhost:8088" }
    }

    val protocol : String by argument("--protocol") {
        description = "Protocol to access client (String representation)"
        defaultValue = { "HTTP" }
    }

    val host : String by argument("--host") {
        description = "Hostname or IP to listen on"
        defaultValue = { "0.0.0.0" } // 0.0.0.0 - listen on all network interfaces
    }

    val port : Int by argument("--port") {
        description = "Port to listen for on"
        defaultValue = { supportedProtocolsAndDefaultPorts[protocol] ?: throw IllegalProtocolException(protocol) }
    }

    val publicUrl : String? by argument("--public-url") {
        description = "Public URL"
    }
}

val log: Logger = LoggerFactory.getLogger("main")
lateinit var parameters : Parameters

fun validateProtocol(protocol: Protocol) {
    if (!supportedProtocolsAndDefaultPorts.containsKey(protocol.toString())) {
        throw IllegalProtocolException(protocol.toString())
    }
}

fun validateHost(host: String) {
    if (!"""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)${'$'}""".toRegex().matches(host)) {
        throw IllegalHostException(host)
    }
}

fun validatePort(port: Int) {
    if (port !in 1..49150) {
        throw IllegalPortException(port)
    }
}

fun main(args: Array<String>) {
    try {
        parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }
        val protocol = parameters.protocol.toProtocol()
        val host = parameters.host
        val port = parameters.port

        validateProtocol(protocol)
        validateHost(host)
        validatePort(port)

        val name = parameters.name
        checkUserName(name) ?: throw IllegalArgumentException("Illegal user name '$name'")

        // initialize registry interface
        val objectMapper = jacksonObjectMapper()
        val registry = Retrofit.Builder()
            .baseUrl(parameters.registryBaseUrl)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build().create(RegistryApi::class.java)

        // create server engine
        val server: ChatServer = when (protocol) {
            Protocol.HTTP -> HttpChatServer(host, port)
            Protocol.WEBSOCKET -> WsChatServer(host, port)
            Protocol.UDP -> UdpChatServer(host, port)
        }

        val chat = Chat(name, registry)
        server.setMessageListener(chat)

        // start server as separate job
        val serverJob = thread {
            server.start()
        }
        try {
            // register our client
            val userAddress  = when {
                parameters.publicUrl != null -> {
                    val url = URL(parameters.publicUrl)
                    UserAddress(url.protocol.toProtocol(), url.host, url.port)
                }
                else -> UserAddress(protocol, host, port)
            }
            registry.register(UserInfo(name, userAddress)).execute()

            // start
            chat.commandLoop()
        }
        finally {
            registry.unregister(name).execute()
            server.stop()
            serverJob.join()
        }
    }
    catch (e: Exception) {
        log.error("Error! ${e.message}", e)
        println("Error! $e")
    }
}

class IllegalProtocolException(protocol: String): IllegalArgumentException("Illegal protocol ($protocol)")
class IllegalHostException(host: String): IllegalArgumentException("Illegal host address ($host)")
class IllegalPortException(port: Int): IllegalArgumentException("Illegal port ($port)")