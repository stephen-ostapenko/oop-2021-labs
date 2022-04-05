package ru.senin.kotlin.net.registry

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.database.Database
import ru.senin.kotlin.net.database.Users
import ru.senin.kotlin.net.database.UsersQueries
import kotlin.test.assertEquals

class DatabaseTest {
    private val testUserName = "pupkin"
    private val anotherTestUserName = "getzlaf"
    private val testList1 =
        listOf(Users("pupkin", "HTTP", "127.0.0.1", 8080, 1, 0))
    private val testList2 =
        listOf(Users("getzlaf", "UDP", "127.0.0.2", 8088, 1, 0))
    private val testListOfAll = listOf(
        Users("pupkin", "HTTP", "127.0.0.1", 8080, 1, 0),
        Users("getzlaf", "UDP", "127.0.0.2", 8088, 1, 0)
    )

    private val driver: SqlDriver
    private val usersQueries: UsersQueries

    init {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        usersQueries = Database(driver).usersQueries
    }

    @Test
    fun `add user test`() {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        assertEquals(testList1, usersQueries.selectAll().executeAsList())
    }

    @Test
    fun `add several users test` () {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        assertEquals(testListOfAll, usersQueries.selectAll().executeAsList())
    }

    @Test
    fun `select by name test`() {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        assertEquals(testList1, usersQueries.selectByName(name = testUserName).executeAsList())
    }

    @Test
    fun `update user address test`() {
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        usersQueries.updateUserAddress(name = "getzlaf", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        assertEquals(
            listOf(Users("getzlaf", "HTTP", "127.0.0.1", 8080, 1, 0)),
            usersQueries.selectAll().executeAsList()
        )
    }

    @Test
    fun `delete user test`() {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        usersQueries.deleteUser(name = "getzlaf")
        assertEquals(testList1, usersQueries.selectAll().executeAsList())
        usersQueries.deleteUser(name = anotherTestUserName)
        assertEquals(testList1, usersQueries.selectAll().executeAsList())
    }

    @Test
    fun `update user existence test`() {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        usersQueries.deleteUser(name = anotherTestUserName)
        usersQueries.updateUserExistence(name = anotherTestUserName)
        assertEquals(testListOfAll, usersQueries.selectAll().executeAsList())
    }

    @Test
    fun `update existence of existing user`() {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        usersQueries.updateUserExistence(name = testUserName)
        assertEquals(testList1, usersQueries.selectAll().executeAsList())
    }

    @Test
    fun `increase failed checks test`() {
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        usersQueries.increaseFailedChecks(name = anotherTestUserName)
        usersQueries.increaseFailedChecks(name = anotherTestUserName)
        assertEquals(
            listOf(Users("getzlaf", "UDP", "127.0.0.2", 8088, 1, 2)),
            usersQueries.selectAll().executeAsList()
        )
    }

    @Test
    fun `reset failed checks test`() {
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        usersQueries.increaseFailedChecks(name = anotherTestUserName)
        usersQueries.increaseFailedChecks(name = anotherTestUserName)
        usersQueries.resetFailedChecks(name = anotherTestUserName)
        assertEquals(testList2, usersQueries.selectAll().executeAsList())
    }

    @Test
    fun `select all failed test`() {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        usersQueries.increaseFailedChecks(name = anotherTestUserName)
        usersQueries.increaseFailedChecks(name = testUserName)
        usersQueries.increaseFailedChecks(name = anotherTestUserName)
        assertEquals(
            listOf(Users("getzlaf", "UDP", "127.0.0.2", 8088, 1, 2)),
            usersQueries.selectAllFailed(2).executeAsList()
        )
    }

    @Test
    fun `has ever existed test`() {
        usersQueries.insert(name = "pupkin", protocol = "HTTP", host = "127.0.0.1", port = 8080)
        usersQueries.insert(name = "getzlaf", protocol = "UDP", host = "127.0.0.2", port = 8088)
        usersQueries.deleteUser(name = testUserName)
        assertEquals(
            listOf(Users("pupkin", "HTTP", "127.0.0.1", 8080, 0, 0)),
            usersQueries.hasEverExisted(name = testUserName).executeAsList()
        )
        assertEquals(testList2, usersQueries.hasEverExisted(name = anotherTestUserName).executeAsList())
    }
}