package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import org.slf4j.event.Level
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import ru.senin.kotlin.net.checkUserName
import java.lang.Thread.sleep
import kotlin.concurrent.thread

lateinit var registry: Registry

enum class Mode {
    Map, Database
}

fun String.toMode(): Mode {
    return enumValueOf(this)
}

fun main(args: Array<String>) {
    try {
        val config = HoconApplicationConfig(ConfigFactory.load())
        val mode = (config.propertyOrNull("ktor.deployment.mode")?.getString() ?: "Map").toMode()

        registry = when (mode) {
            Mode.Map -> HashMapRegistry()
            Mode.Database -> {
                val pathToDatabase = config.propertyOrNull("ktor.deployment.pathToDatabase")?.getString() ?: ""
                DatabaseRegistry(pathToDatabase)
            }
        }

        thread {
            while (true) {
                sleep(60 * 1000)
                registry.updateUsersList()
            }
        }

        EngineMain.main(args)
    }
    catch (e: Exception) {
        println(e)
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
            checkUserName(user.name) ?: throw IllegalUserNameException()

            registry.addUser(user)

            call.respond(mapOf("status" to "ok"))
        }

        get("/v1/users") {
            call.respond(registry.getUsersListAsMap())
        }

        put("/v1/users/{name}") {
            val name = call.parameters["name"] ?: throw IllegalUserNameException()
            checkUserName(name) ?: throw IllegalUserNameException()
            val address = call.receive<UserAddress>()

            registry.updateUser(name, address)

            call.respond(mapOf("status" to "ok"))
        }

        delete("/v1/users/{name}") {
            val name = (call.parameters["name"] ?: call.respond(mapOf("status" to "ok"))) as String
            checkUserName(name) ?: throw IllegalUserNameException()

            registry.deleteUser(name)

            call.respond(mapOf("status" to "ok"))
        }
    }
}

class UserAlreadyRegisteredException: RuntimeException("User already registered")
class IllegalUserNameException: RuntimeException("Illegal user name")