package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.UserAddress

abstract class UserHealthChecker {
    private var failedChecks: Int = 0

    abstract fun needToUpdateFailedChecksCounter(): Boolean

    fun updateFailedChecksCounter() {
        if (needToUpdateFailedChecksCounter()) {
            failedChecks++
        }
    }

    fun checkHealth(): Boolean {
        return failedChecks <= 2
    }
}

class HttpUserHealthChecker(address: UserAddress) : UserHealthChecker() {
    private val objectMapper = jacksonObjectMapper()
    private val userHealthCheckerApi: HttpUserHealthCheckerApi = Retrofit.Builder()
        .baseUrl("http://${address.host}:${address.port}/")
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .build().create(HttpUserHealthCheckerApi::class.java)

    override fun needToUpdateFailedChecksCounter(): Boolean {
        return try {
            val response = userHealthCheckerApi.checkHealth().execute()
            if (!(response.isSuccessful && response.message() == "OK")) {
                throw Exception("User not available")
            }
            false
        } catch (e: Exception) {
            true
        }
    }
}

class WsUserHealthChecker(private val address: UserAddress) : UserHealthChecker() {
    private val client = HttpClient {
        install(WebSockets)
    }

    override fun needToUpdateFailedChecksCounter(): Boolean {
        return runBlocking {
            return@runBlocking try {
                var result = false

                client.ws(host = address.host, port = address.port, path = "/v1/ws/health") {
                    val next = incoming.receive()
                    if (!(next is Frame.Text && next.readText() == "OK")) {
                        result = true
                    }
                }

                result
            }
            catch (e: Exception) {
                true
            }
        }
    }
}

// UDP users are not so cool to be healthChecked.
class UdpUserHealthChecker(private val address: UserAddress) : UserHealthChecker() {
    override fun needToUpdateFailedChecksCounter(): Boolean {
        address // = (void)address;
        return false
    }
}