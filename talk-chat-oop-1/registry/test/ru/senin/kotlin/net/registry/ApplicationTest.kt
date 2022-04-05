package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import ru.senin.kotlin.net.checkUserName
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

fun Application.testModule() {

    (environment.config as MapApplicationConfig).apply {
        // define test environment here
    }
    module(testing = true)
}

class ApplicationTest {
    private val objectMapper = jacksonObjectMapper()
    private val testUserName = "pupkin"
    private val testHttpAddress = UserAddress(Protocol.HTTP, "127.0.0.1", 9999)
    private val userData = UserInfo(testUserName, testHttpAddress)

    private val anotherTestUserName = "abracadabra"
    private val anotherTestHttpAddress = UserAddress(Protocol.HTTP, "0.0.0.1", 8080)
    private val anotherUserData = UserInfo(anotherTestUserName, anotherTestHttpAddress)

    private val testWebSocketAddress = UserAddress(Protocol.WEBSOCKET, "127.0.0.1", 8083)
    private val userDataWebSocket = UserInfo(anotherTestUserName, testWebSocketAddress)
    private val testUdpAddress = UserAddress(Protocol.UDP, "127.0.0.1", 3002)
    private val userDataUdp = UserInfo(anotherTestUserName, testUdpAddress)

    private val badUserName = "It's a bad name!"
    private val badUserData = UserInfo(badUserName, testHttpAddress)

    @BeforeEach
    fun clearRegistry() {
        Registry.users.clear()
    }

    @Ignore
    @Test
    fun `test checkUserName`() {
        assertNotNull(checkUserName(testUserName))
    }

    @Ignore
    @Test
    fun `bad test checkUserName`() {
        assertEquals(checkUserName(badUserName), null)
    }

    @Ignore
    @Test
    fun `empty user name`() {
        assertEquals(checkUserName(""), null)
    }

    @Ignore
    @Test
    fun `health endpoint`() {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }

    @Ignore
    @Test
    fun `register user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(anotherUserData))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(mapOf(testUserName to testHttpAddress, anotherTestUserName to anotherTestHttpAddress),
                        Registry.users.toMap())
            }
        }
    }

    @Ignore
    @Test
    fun `register WebSocket user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(userDataWebSocket))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(mapOf(testUserName to testHttpAddress, anotherTestUserName to testWebSocketAddress),
                        Registry.users.toMap())
            }
        }
    }

    @Ignore
    @Test
    fun `register Udp user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(userDataUdp))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(mapOf(testUserName to testHttpAddress, anotherTestUserName to testUdpAddress),
                        Registry.users.toMap())
            }
        }
    }

    @Ignore
    @Test
    fun `register already registered user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(userData))
            }.apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }

    @Ignore
    @Test
    fun `register a bad name user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(badUserData))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Ignore
    @Test
    fun `list users`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Get
                uri = "/v1/users"
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String, UserAddress>>(content)
                assertNotNull(info["pupkin"])
                assertEquals(info["pupkin"], testHttpAddress)
            }
        }
    }

    @Ignore
    @Test
    fun `update user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Put
                uri = "/v1/users/pupkin"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(testHttpAddress))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(mapOf(testUserName to testHttpAddress), Registry.users.toMap())
            }
        }
    }

    @Ignore
    @Test
    fun `update bad name user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Put
                uri = "/v1/users/That's_bad!"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(testHttpAddress))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Ignore
    @Test
    fun `update user info`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Put
                uri = "/v1/users/pupkin"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(anotherTestHttpAddress))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(mapOf(testUserName to anotherTestHttpAddress), Registry.users.toMap())
            }
        }
    }

    @Ignore
    @Test
    fun `update non-existing user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Put
                uri = "/v1/users/abracadabra"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(anotherTestHttpAddress))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(mapOf(testUserName to testHttpAddress, anotherTestUserName to anotherTestHttpAddress),
                        Registry.users.toMap())
            }
        }
    }

    @Ignore
    @Test
    fun `delete user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Delete
                uri = "/v1/users/pupkin"
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(emptyMap(), Registry.users.toMap())
            }
        }
    }

    @Ignore
    @Test
    fun `delete non-existing user`() = withRegisteredTestUser {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Delete
                uri = "/v1/users/abracadabra"
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                assertEquals(mapOf(testUserName to testHttpAddress), Registry.users.toMap())
            }
        }
    }

    private fun withRegisteredTestUser(block: TestApplicationEngine.() -> Unit) {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(userData))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                this@withTestApplication.block()
            }
        }
    }
}
