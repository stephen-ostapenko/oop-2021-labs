package ru.senin.kotlin.net.registry

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import ru.senin.kotlin.net.database.Database
import ru.senin.kotlin.net.database.UsersQueries
import ru.senin.kotlin.net.toProtocol
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class Registry {
    protected val healthCheckers = ConcurrentHashMap<String, UserHealthChecker>()

    fun insertHealthChecker(name: String, address: UserAddress) {
        healthCheckers[name] = when (address.protocol) {
            Protocol.HTTP -> HttpUserHealthChecker(address)
            Protocol.WEBSOCKET -> WsUserHealthChecker(address)
            Protocol.UDP -> UdpUserHealthChecker(address)
        }
    }

    abstract fun addUser(user: UserInfo)
    abstract fun updateUser(name: String, address: UserAddress)
    abstract fun deleteUser(name: String)
    abstract fun getUsersListAsMap(): Map<String, UserAddress>
    abstract fun updateUsersList()
    abstract fun clearRegistry()
}

class HashMapRegistry: Registry() {
    private val users = ConcurrentHashMap<String, UserAddress>()

    override fun addUser(user: UserInfo) {
        val name = user.name
        val address = user.address
        if (users.containsKey(name)) {
            throw UserAlreadyRegisteredException()
        }

        users[name] = user.address
        insertHealthChecker(name, address)
    }

    override fun updateUser(name: String, address: UserAddress) {
        users[name] = address
        insertHealthChecker(name, address)
    }

    override fun deleteUser(name: String) {
        if (users.containsKey(name)) {
            users.remove(name)
            healthCheckers.remove(name)
        }
    }

    override fun getUsersListAsMap(): Map<String, UserAddress> {
        return users.toMap()
    }

    override fun updateUsersList() {
        healthCheckers.forEach {
            it.value.updateFailedChecksCounter()
        }

        healthCheckers.entries.retainAll { it.value.checkHealth() }
        users.entries.retainAll { it.key in healthCheckers.keys }
    }

    override fun clearRegistry() {
        users.clear()
        healthCheckers.clear()
    }
}

class DatabaseRegistry(file: String = "") : Registry() {
    private val pathToDatabase: String = file
    private val driver: SqlDriver
    private val usersQueries: UsersQueries

    init {
        val databaseFileAlreadyExists = File(pathToDatabase).exists()
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + pathToDatabase)
        if (!databaseFileAlreadyExists) {
            Database.Schema.create(driver)
        }
        usersQueries = Database(driver).usersQueries
    }

    private fun hasEverExisted(name: String): Boolean {
        return usersQueries.hasEverExisted(name = name).executeAsOneOrNull() != null
    }

    private fun checkUserExistence(name: String): Boolean {
        return usersQueries.selectByName(name = name).executeAsOneOrNull() != null
    }

    override fun addUser(user: UserInfo) {
        val name = user.name
        val address = user.address
        if (checkUserExistence(name)) {
            throw UserAlreadyRegisteredException()
        }
        if (hasEverExisted(name)) {
            usersQueries.updateUserExistence(name = name)
            updateUser(name, address)
        } else {
            usersQueries.insert(
                name = name,
                protocol = address.protocol.toString(),
                host = address.host,
                port = address.port.toLong()
            )
            insertHealthChecker(name, address)
        }
    }

    override fun updateUser(name: String, address: UserAddress) {
        usersQueries.updateUserAddress(
            name = name,
            protocol = address.protocol.toString(),
            host = address.host,
            port = address.port.toLong()
        )
        insertHealthChecker(name, address)
    }

    override fun deleteUser(name: String) {
        if (checkUserExistence(name)) {
            usersQueries.deleteUser(name = name)
            healthCheckers.remove(name)
        }
    }

    override fun getUsersListAsMap(): Map<String, UserAddress> {
        val listFromDatabase = usersQueries.selectUsersForMap().executeAsList().map {
            UserInfo(it.name, UserAddress(it.protocol.toProtocol(), it.host, it.port.toInt()))
        }
        return listFromDatabase.associate { it.name to it.address }
    }

    override fun updateUsersList() {
        healthCheckers.forEach {
            it.value.updateFailedChecksCounter()
        }

        val listToDelete = healthCheckers.entries.filter { !it.value.checkHealth() }
        listToDelete.forEach {
            deleteUser(it.key)
        }
    }

    override fun clearRegistry() {
        usersQueries.clearDatabase()
        healthCheckers.clear()
    }
}