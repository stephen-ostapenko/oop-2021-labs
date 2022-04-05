package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import org.slf4j.event.Level
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import ru.senin.kotlin.net.checkUserName
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    thread {
        while (true) {
            sleep(60 * 1000)
            Registry.updateUsersList()
        }
    }
    EngineMain.main(args)
}

object Registry {
    val users = ConcurrentHashMap<String, UserAddress>()
    val healthCheckers = ConcurrentHashMap<String, UserHealthChecker>()

    fun updateUsersList() {
        healthCheckers.forEach {
            it.value.updateFailedChecksCounter()
        }

        healthCheckers.entries.retainAll { it.value.checkHealth() }

        users.entries.retainAll { it.key in healthCheckers.keys }
    }
}

@Suppress("UNUSED_PARAMETER")
@JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "invalid argument")
        }
        exception<UserAlreadyRegisteredException> { cause ->
            call.respond(HttpStatusCode.Conflict, cause.message ?: "user already registered")
        }
        exception<IllegalUserNameException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "illegal user name")
        }
    }
    routing {
        get("/v1/health") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }

        post("/v1/users") {
            val user = call.receive<UserInfo>()
            val name = user.name
            checkUserName(name) ?: throw IllegalUserNameException()
            if (Registry.users.containsKey(name)) {
                throw UserAlreadyRegisteredException()
            }
            Registry.users[name] = user.address

            Registry.healthCheckers[name] = when (user.address.protocol) {
                Protocol.HTTP -> HttpUserHealthChecker(user.address)
                Protocol.WEBSOCKET -> WsUserHealthChecker(user.address)
                Protocol.UDP -> UdpUserHealthChecker(user.address)
            }

            call.respond(mapOf("status" to "ok"))
        }

        get("/v1/users") {
            call.respond(Registry.users.toMap())
        }

        put("/v1/users/{name}") {
            val address = call.receive<UserAddress>()
            val name = call.parameters["name"] ?: throw IllegalUserNameException()
            checkUserName(name) ?: throw IllegalUserNameException()
            Registry.users[name] = address

            Registry.healthCheckers[name] = when (address.protocol) {
                Protocol.HTTP -> HttpUserHealthChecker(address)
                Protocol.WEBSOCKET -> WsUserHealthChecker(address)
                Protocol.UDP -> UdpUserHealthChecker(address)
            }

            call.respond(mapOf("status" to "ok"))
        }

        delete("/v1/users/{name}") {
            val name = call.parameters["name"] ?: call.respond(mapOf("status" to "ok"))
            if (Registry.users.containsKey(name)) {
                Registry.users.remove(name)
                Registry.healthCheckers.remove(name)
            }
            call.respond(mapOf("status" to "ok"))
        }
    }
}

class UserAlreadyRegisteredException: RuntimeException("User already registered")
class IllegalUserNameException: RuntimeException("Illegal user name")