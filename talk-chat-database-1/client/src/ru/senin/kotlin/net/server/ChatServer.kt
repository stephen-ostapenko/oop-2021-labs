package ru.senin.kotlin.net.server

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import ru.senin.kotlin.net.Message
import java.net.InetSocketAddress
import java.net.SocketException

interface ChatMessageListener {
    fun messageReceived(userName: String, text: String)
}

abstract class ChatServer(curHost: String, curPort: Int) {
    protected val host: String = curHost
    protected val port: Int = curPort
    protected val objectMapper = jacksonObjectMapper()
    protected var listener: ChatMessageListener? = null
    private val engine = createEngine()
    abstract val serverName: String

    fun setMessageListener(listener: ChatMessageListener) {
        this.listener = listener
    }

    private fun createEngine(): NettyApplicationEngine {
        val applicationEnvironment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger(serverName)
            classLoader = ApplicationEngineEnvironment::class.java.classLoader
            connector {
                this.host = this@ChatServer.host
                this.port = this@ChatServer.port
            }
            module (configureModule())
        }
        return NettyApplicationEngine(applicationEnvironment)
    }

    abstract fun configureModule(): Application.() -> Unit

    fun start() {
        engine.start(true)
    }

    fun stop() {
        engine.stop(1000, 2000)
    }
}

class HttpChatServer(host: String, port: Int) : ChatServer(host, port) {
    override val serverName: String
        get() = "http-chat-server"

    override fun configureModule(): Application.() -> Unit = {
        install(CallLogging) {
            level = Level.DEBUG
            filter { call -> call.request.path().startsWith("/") }
        }

        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }

        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }

        routing {
            get("/v1/health") {
                call.respond(mapOf("status" to "ok"))
            }

            post("/v1/message") {
                val message = call.receive<Message>()
                listener?.messageReceived(message.user, message.text) ?: throw MissingMessageListenerException()
                call.respond(mapOf("status" to "ok", "content" to "Message received successfully"))
            }

            install(StatusPages) {
                exception<IllegalArgumentException> {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}

class WsChatServer(host: String, port: Int) : ChatServer(host, port) {
    override val serverName: String
        get() = "ws-chat-server"

    override fun configureModule(): Application.() -> Unit = {
        install(CallLogging) {
            level = Level.DEBUG
            filter { call -> call.request.path().startsWith("/") }
        }

        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }

        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }

        install(WebSockets)

        routing {
            webSocket("/v1/ws/health") {
                send("OK")
            }

            webSocket("/v1/ws/message") {
                val next = incoming.receive()
                if (next is Frame.Text) {
                    val plot = next.readText()
                    val message: Message = objectMapper.readValue(plot)
                    listener?.messageReceived(message.user, message.text)
                } else {
                    throw RuntimeException("Wrong message format")
                }
            }
        }
    }
}

class UdpChatServer(host: String, port: Int) : ChatServer(host, port) {
    override val serverName: String
        get() = "udp-chat-server"

    override fun configureModule(): Application.() -> Unit = {
        install(CallLogging) {
            level = Level.DEBUG
            filter { call -> call.request.path().startsWith("/") }
        }

        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }

        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }

        routing {
            try {
                val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(host, port))

                launch {
                    while (true) {
                        val next = socket.incoming.receive()
                        val plot = next.packet.readText()
                        val message: Message = objectMapper.readValue(plot)
                        listener?.messageReceived(message.user, message.text)
                    }
                }
            }
            catch (e: SocketException) {
                log.error("Error! ${e.message}", e)
                println("Error! $e")
                throw PortIsAlreadyInUseException(port)
            }
            catch (e: Exception) {
                log.error("Error! ${e.message}", e)
                println("Error! $e")
            }
        }
    }
}

class MissingMessageListenerException: RuntimeException("Message listener is missing")
class PortIsAlreadyInUseException(port: Int): RuntimeException("Port $port is already in use")
