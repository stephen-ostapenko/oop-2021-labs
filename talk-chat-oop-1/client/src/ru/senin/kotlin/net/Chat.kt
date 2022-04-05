package ru.senin.kotlin.net

import ru.senin.kotlin.net.client.ChatClient
import ru.senin.kotlin.net.client.HttpChatClient
import ru.senin.kotlin.net.client.UdpChatClient
import ru.senin.kotlin.net.client.WsChatClient
import ru.senin.kotlin.net.server.ChatMessageListener
import java.lang.Thread.sleep
import java.net.ConnectException
import kotlin.concurrent.thread

class Chat(
    private val name: String,
    private val registry: RegistryApi
) : ChatMessageListener {

    private var exit = false
    private var selectedUser: String? = null
    private val clients = mutableMapOf<String, ChatClient>()
    private var users =  mutableMapOf<String, UserAddress>()
    private var currentClient: ChatClient? = null

    private fun getPromptString(): String {
        return "  to [${selectedUser ?: "<not selected>"}] <<< "
    }

    private fun prompt(): String {
        print(getPromptString())
        var value: String? = readLine()
        while (value.isNullOrBlank()) {
            print(getPromptString())
            value = readLine()
        }
        return value.trimStart()
    }

    private fun updateUsersList(silent: Boolean = false) {
        val registeredUsers = registry.list().execute().getOrNull()
        if (registeredUsers == null) {
            messageReceived("<System notifications>", "Error! Cannot get users from registry")
            return
        }
        val aliveUserNames = registeredUsers.keys
        if (selectedUser != null && selectedUser !in aliveUserNames) {
            selectedUser = null
            messageReceived("<System notifications>", "Selected user was removed from registry")
        }
        users.clear()
        users.putAll(registeredUsers)
        clients.entries.retainAll { it.key in aliveUserNames }
        if (!silent) {
            users.forEach { (name, address) ->
                println("$name ==> $address")
            }
        }
    }

    private fun selectUser(userName: String) {
        val userAddress = users[userName]
        if (userAddress == null) {
            println("Unknown user '$userName'")
            return
        }
        selectedUser = userName
        currentClient = when (userAddress.protocol) {
            Protocol.HTTP -> HttpChatClient(userAddress.host, userAddress.port)
            Protocol.WEBSOCKET -> WsChatClient(userAddress.host, userAddress.port)
            Protocol.UDP -> UdpChatClient(userAddress.host, userAddress.port)
        }
        clients.getOrPut(userName) {
            val clientToPut = currentClient
            clientToPut ?: throw MissingChatClientException(userAddress)
        }
    }

    private fun exit() {
        exit = true
    }

    private fun message(text: String) {
        val currentUser = selectedUser
        if (currentUser == null) {
            println("User not selected. Use :user command")
            return
        }
        val address = users[currentUser]
        if (address == null) {
            println("Cannot send message, because user disappeared")
            return
        }
        try {
            val client: ChatClient = currentClient ?: throw MissingChatClientException(address)
            client.sendMessage(Message(name, text))
        }
        catch (e: ConnectException) {
            println("Error! Cannot access user $currentUser on address $address")
        }
        catch (e: Exception) {
            println("Error! $e")
        }
    }

    fun commandLoop() {
        // User list updater.
        thread {
            while (!exit) {
                // Checking exit every second.
                // 3 minutes overall.
                for (exitChecksCount in 1..180) {
                    if (exit) {
                        break
                    }
                    sleep(1000)
                }

                if (!exit) {
                    updateUsersList(silent = true)
                }
            }
        }

        var input: String
        printWelcome()
        updateUsersList()
        while (!exit) {
            input = prompt()
            when (input.substringBefore(" ")) {
                ":update" -> updateUsersList()
                ":exit" -> exit()
                ":user" -> {
                    val userName = input.split("""\s+""".toRegex()).drop(1).joinToString(" ")
                    selectUser(userName)
                }
                "" -> {}
                else -> message(input)
            }
        }
    }

    private fun printWelcome() {
        println(
            """
                          Был бы                      
             _______     _       _       _   __   
            |__   __|   / \     | |     | | / /   
               | |     / ^ \    | |     | |/ /    
               | |    / /_\ \   | |     |    \     
               | |   / _____ \  | |___  | |\  \    
               |_|  /_/     \_\ |_____| |_| \__\ () () ()   
                                     
                    \ | /
                ^  -  O  -  
               / \^ / | \   
              /  / \        Hi, $name
             /  /   \     Welcome to Chat!
            """.trimIndent()
        )
    }

    override fun messageReceived(userName: String, text: String) {
        println("\nfrom [$userName] >>> $text")
        print(getPromptString())
    }
}

class MissingChatClientException(address: UserAddress): RuntimeException("Missing chat client for address $address")