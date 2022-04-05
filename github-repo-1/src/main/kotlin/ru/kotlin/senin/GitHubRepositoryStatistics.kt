package ru.kotlin.senin

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.kotlin.senin.GitHubRepositoryStatistics.LoadingStatus.*
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.SocketException
import java.time.LocalDateTime
import java.util.concurrent.CancellationException
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

val log: Logger = LoggerFactory.getLogger("AppUI")
private val defaultInsets = Insets(3, 10, 3, 10)

@FlowPreview
fun main() {
    setDefaultFontSize(18f)
    GitHubRepositoryStatistics().apply {
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}

@FlowPreview
class GitHubRepositoryStatistics : JFrame("GitHub Repository Statistics"), CoroutineScope {

    enum class LoadingStatus { COMPLETED, CANCELED, IN_PROGRESS, FAILED }

    companion object {
        private val columns = arrayOf("Author", "Commits", "Files", "Changes")
        const val textFieldWidth = 30
    }

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    @FlowPreview
    private fun init() {
        // Start a new loading on 'load' click
        addLoadListener {
            saveParameters()
            loadResults()
        }

        // Save preferences and exit on closing the window
        addOnWindowClosingListener {
            job.cancel()
            saveParameters()
            exitProcess(0)
        }

        // Load stored params (user & password values)
        loadInitialParameters()
    }

    @FlowPreview
    private fun loadResults() {
        val (username, password, repositoryUrl) = getParameters()
        val (owner, repository) = parseRepositoryUrl(repositoryUrl)
        val req = RequestData(username, password, owner, repository)

        clearResults()
        val service = createGitHubService(req.username, req.password)

        val startTime = System.currentTimeMillis()
        launch(Dispatchers.Default) {
            log.info("loading results from $owner/$repository")
            log.info("rep url: $repositoryUrl")

            loadResults(service, req) { users, completed, failed ->
                withContext(Dispatchers.Main) {
                    updateResults(users, startTime, completed, failed)
                }
            }
        }.setUpCancellation()
    }

    private fun parseRepositoryUrl(repositoryUrl: String): Pair<String, String> {
        val owner = repositoryUrl.substring(
            Regex("(?<=^https://github\\.com/)(\\w[\\-\\w]*\\w)")
                .find(repositoryUrl)?.range ?: error("Repository URL doesn't match regex")
        )

        val repository = repositoryUrl.substring(
            Regex("(?<=^https://github\\.com/$owner/)(\\w[._\\-\\w]*\\w)")
                .find(repositoryUrl)?.range ?: error("Repository URL doesn't match regex")
        )

        return owner to repository
    }

    private fun clearResults() {
        updateResults(emptyMap())
        updateLoadingStatus(IN_PROGRESS)
        setActionsStatus(newLoadingEnabled = false)
    }

    private fun updateResults(results: Map<String, UserStatistics>, startTime: Long, completed: Boolean, failed: Boolean) {
        updateResults(results)
        updateLoadingStatus(
            if (failed) FAILED else if (completed) COMPLETED else IN_PROGRESS, startTime
        )
        if (completed) {
            setActionsStatus(newLoadingEnabled = true)
            removeCancelListener(cancelListener ?: error("null cancelListener"))
        }
    }

    private fun updateLoadingStatus(status: LoadingStatus, startTime: Long? = null) {
        val time = if (startTime != null) {
            val time = System.currentTimeMillis() - startTime
            "${(time / 1000)}.${time % 1000 / 100} sec"
        } else ""

        val text = "Loading status: " +
                when (status) {
                    COMPLETED -> "completed in $time"
                    IN_PROGRESS -> "in progress $time"
                    FAILED -> "failed in $time"
                    CANCELED -> "canceled"
                }
        setLoadingStatus(text, status == IN_PROGRESS)
    }

    private fun getCancelListener(j: Job) = ActionListener {
        j.cancel()
        updateLoadingStatus(CANCELED)
        setActionsStatus(newLoadingEnabled = true)
    }

    private var cancelListener: ActionListener? = null

    private fun Job.setUpCancellation() {
        if (cancelListener != null) {
            removeCancelListener(cancelListener ?: error("null cancelListener"))
        }
        cancelListener = getCancelListener(this)
        setActionsStatus(newLoadingEnabled = false, cancellationEnabled = true)
        addCancelListener(cancelListener ?: error("null cancelListener"))
    }

    private fun loadInitialParameters() {
        setParameters(loadParameters())
    }

    private fun saveParameters() {
        val parameters = getParameters()
        if (parameters.username.isEmpty() && parameters.password.isEmpty()) {
            removeStoredParameters()
        } else {
            saveParameters(parameters)
        }
    }

    private val username = JTextField(textFieldWidth)
    private val password = JPasswordField(textFieldWidth)
    private val repositoryUrl = JTextField(textFieldWidth)
    private val load = JButton("Load statistics")
    private val cancel = JButton("Cancel").apply { isEnabled = false }

    private val resultsModel = DefaultTableModel(columns, 0)
    private val results = JTable(resultsModel)
    private val resultsScroll = JScrollPane(results).apply {
        preferredSize = Dimension(200, 600)
    }

    private val loadingIcon = ImageIcon(javaClass.classLoader.getResource("ajax-loader.gif"))
    private val loadingStatus = JLabel("Start new loading", loadingIcon, SwingConstants.CENTER)

    init {
        // Create UI
        rootPane.contentPane = JPanel(GridBagLayout()).apply {
            addLabeled("GitHub Username", username)
            addLabeled("Password/Token", password)
            addWideSeparator()
            addLabeled("Repository url", repositoryUrl)
            addWideSeparator()
            addWide(JPanel().apply {
                add(load)
                add(cancel)
            })
            addWide(resultsScroll) {
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
            addWide(loadingStatus)
        }

        // Initialize actions
        init()
    }

    private fun updateResults(results: Map<String, UserStatistics>) {
        val sortedResults = results.toList().sortedWith(compareByDescending { it.second.commits }).toMap()
        resultsModel.setDataVector(sortedResults.map { (login, stat) ->
            arrayOf(login, stat.commits, stat.files.size, stat.changes)
        }.toTypedArray(), columns)
    }

    private fun setLoadingStatus(text: String, iconRunning: Boolean) {
        loadingStatus.text = text
        loadingStatus.icon = if (iconRunning) loadingIcon else null
    }

    private fun addCancelListener(listener: ActionListener) {
        cancel.addActionListener(listener)
    }

    private fun removeCancelListener(listener: ActionListener) {
        cancel.removeActionListener(listener)
    }

    private fun addLoadListener(listener: () -> Unit) {
        load.addActionListener { listener() }
    }

    private fun addOnWindowClosingListener(listener: () -> Unit) {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                listener()
            }
        })
    }

    private fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean = false) {
        load.isEnabled = newLoadingEnabled
        cancel.isEnabled = cancellationEnabled
    }

    private fun setParameters(storedParameters: StoredParameters) {
        username.text = storedParameters.username
        password.text = storedParameters.password
        repositoryUrl.text = storedParameters.repositoryUrl
    }

    private fun getParameters(): StoredParameters {
        return StoredParameters(username.text, password.password.joinToString(""), repositoryUrl.text)
    }
}

fun JPanel.addLabeled(label: String, component: JComponent) {
    add(JLabel(label), GridBagConstraints().apply {
        gridx = 0
        insets = defaultInsets
    })
    add(component, GridBagConstraints().apply {
        gridx = 1
        insets = defaultInsets
        anchor = GridBagConstraints.WEST
        fill = GridBagConstraints.HORIZONTAL
        weightx = 1.0
    })
}

fun JPanel.addWide(component: JComponent, constraints: GridBagConstraints.() -> Unit = {}) {
    add(component, GridBagConstraints().apply {
        gridx = 0
        gridwidth = 2
        insets = defaultInsets
        constraints()
    })
}

fun JPanel.addWideSeparator() {
    addWide(JSeparator()) {
        fill = GridBagConstraints.HORIZONTAL
    }
}

fun setDefaultFontSize(size: Float) {
    for (key in UIManager.getLookAndFeelDefaults().keys.toTypedArray()) {
        if (key.toString().toLowerCase().contains("font")) {
            val font = UIManager.getDefaults().getFont(key) ?: continue
            val newFont = font.deriveFont(size)
            UIManager.put(key, newFont)
        }
    }
}

private fun preferencesNode(): Preferences = Preferences.userRoot().node("AppUI")

data class StoredParameters(val username: String, val password: String, val repositoryUrl: String)

fun loadParameters(): StoredParameters {
    return preferencesNode().run {
        StoredParameters(
            get("username", ""),
            get("password", ""),
            get("repositoryUrl", "https://github.com/Kotlin/kotlinx.coroutines")
        )
    }
}

fun removeStoredParameters() {
    preferencesNode().removeNode()
}

fun saveParameters(storedParameters: StoredParameters) {
    preferencesNode().apply {
        put("username", storedParameters.username)
        put("password", storedParameters.password)
        put("repositoryUrl", storedParameters.repositoryUrl)
        sync()
    }
}

@FlowPreview
suspend fun loadResults(
    service: GitHubService, req: RequestData,
    updateResults: suspend (Map<String, UserStatistics>, completed: Boolean, failed: Boolean) -> Unit
): Unit = coroutineScope {
    val mp: MutableMap<String, UserStatistics> = mutableMapOf()
    val yearAgoTime = LocalDateTime.now().minusYears(1).toString()
    log.info("getting commits since $yearAgoTime")

    try {
        flow {
            var page = 1
            while (true) {
                log.info("getting commits from ${req.owner}/${req.repository} on page $page")
                val res = service.getCommits(
                    owner = req.owner,
                    repository = req.repository,
                    since = yearAgoTime,
                    page = page++
                )
                emit(res)
            }

        }.takeWhile {
            val commits = it.body()
            if (!it.isSuccessful || commits == null) {
                throw SocketException("failed to load commits (response code is ${it.code()})")
            }

            commits.isNotEmpty()

        }.flatMapMerge { response ->
            flow {
                response.body()?.forEach { commit ->
                    emit(commit)
                }
            }

        }.flatMapMerge { commit ->
            flow {
                val response = service.getChanges(req.owner, req.repository, commit.sha)
                val commitWithChanges = response.body()
                if (!response.isSuccessful || commitWithChanges == null) {
                    throw SocketException(
                        "failed to load commit ${req.owner}/${req.repository}/${commit.sha} (response code is ${response.code()})"
                    )
                }

                emit(commitWithChanges)
            }

        }.collect { commit ->
            if (commit.author != null && commit.author.type != "Bot") {
                val stat = mp[commit.author.login]
                val totalCommits = stat?.commits ?: 0
                val files = if (stat == null) mutableSetOf() else stat.files as MutableSet
                var changes = stat?.changes ?: 0

                commit.files.forEach { change ->
                    files.add(change.filename)
                    changes += change.changes
                }
                mp[commit.author.login] = UserStatistics(totalCommits + 1, files, changes)

                updateResults(mp, false, false)
            }
        }

    } catch (e: CancellationException) {
        log.info("loading cancelled")

    } catch (e: Exception) {
        log.info(e.stackTraceToString())
        updateResults(mp, true, true)
        return@coroutineScope
    }

    updateResults(mp, true, false)
}