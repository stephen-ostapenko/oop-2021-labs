package ru.senin.kotlin.net.registry

import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import kotlin.test.assertEquals

class DatabaseRegistryTest {
    private val testUserName = "pupkin"
    private val testHttpAddress = UserAddress(Protocol.HTTP, "127.0.0.1", 9999)
    private val userData = UserInfo(testUserName, testHttpAddress)

    private val anotherTestUserName = "getzlaf"
    private val testUdpAddress = UserAddress(Protocol.UDP, "127.0.0.2", 8088)
    private val anotherUserData = UserInfo(anotherTestUserName, testUdpAddress)

    @Test
    fun `add user test`() {
        val registry = DatabaseRegistry()
        assertEquals(mapOf(), registry.getUsersListAsMap())
        registry.addUser(userData)
        assertEquals(mapOf(testUserName to testHttpAddress), registry.getUsersListAsMap())
        registry.addUser(anotherUserData)
        assertEquals(mapOf(
            testUserName to testHttpAddress,
            anotherTestUserName to testUdpAddress
        ), registry.getUsersListAsMap())
    }

    @Test
    fun `update user test`() {
        val registry = DatabaseRegistry()
        registry.addUser(userData)
        registry.updateUser(testUserName, testUdpAddress)
        assertEquals(mapOf(testUserName to testUdpAddress), registry.getUsersListAsMap())
    }

    @Test
    fun `delete user test`() {
        val registry = DatabaseRegistry()
        registry.addUser(userData)
        registry.addUser(anotherUserData)
        registry.deleteUser(testUserName)
        assertEquals(mapOf(anotherTestUserName to testUdpAddress), registry.getUsersListAsMap())
        registry.addUser(userData)
        assertEquals(mapOf(
            testUserName to testHttpAddress,
            anotherTestUserName to testUdpAddress
        ), registry.getUsersListAsMap())
    }

    @Test
    fun `storing data in a file test`() {
        val pathToDB = "test.db"
        val registry = DatabaseRegistry(pathToDB)
        registry.addUser(userData)
        registry.addUser(anotherUserData)
        val anotherRegistry = DatabaseRegistry(pathToDB)
        assertEquals(mapOf(
                testUserName to testHttpAddress,
                anotherTestUserName to testUdpAddress
        ), anotherRegistry.getUsersListAsMap())
        registry.clearRegistry()
        assertEquals(mapOf(), anotherRegistry.getUsersListAsMap())
    }

    @Test
    fun `clear registry test`() {
        val registry = DatabaseRegistry()
        registry.addUser(userData)
        registry.addUser(anotherUserData)
        registry.clearRegistry()
        assertEquals(mapOf(), registry.getUsersListAsMap())
    }
}