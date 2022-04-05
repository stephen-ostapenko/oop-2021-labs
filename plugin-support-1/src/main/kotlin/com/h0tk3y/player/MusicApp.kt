package com.h0tk3y.player

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.full.superclasses

open class MusicApp(
    private val pluginPaths: List<MusicPluginPath>
) : AutoCloseable {

    companion object {
        private const val pluginDataFilePath = "./pluginData/"
        private const val pluginDataFileSuffix = ".resources.data"
    }

    private fun getPluginDataFileName(plugin: MusicPlugin): String {
        return pluginDataFilePath + plugin.pluginId + pluginDataFileSuffix
    }

    fun init() {
        plugins.forEach {
            val dataFile = getPluginDataFileName(it)

            if (File(dataFile).exists()) {
                FileInputStream(dataFile).use { stateStream ->
                    it.init(stateStream)
                }
            } else {
                it.init(null)
            }
        }

        musicLibrary // access to initialize

        player.init()
    }

    fun wipePersistedPluginData() {
        plugins.forEach {
            Files.deleteIfExists(Paths.get(getPluginDataFileName(it)))
        }
    }

    private val plugins: List<MusicPlugin> by lazy<List<MusicPlugin>> {
        val usedPlugins: MutableMap<String, Boolean> = mutableMapOf()
        val result = mutableListOf<MusicPlugin>()

        pluginPaths.forEach { musicPluginPath ->
            val loader = URLClassLoader(musicPluginPath.pluginClasspath.map { it.toURI().toURL() }.toTypedArray())

            musicPluginPath.pluginClasses.forEach { pluginClass ->
                if (usedPlugins[pluginClass] == null) {
                    val constructors by lazy {
                        try {
                            loader.loadClass(pluginClass).constructors
                        } catch (e: ClassNotFoundException) {
                            throw PluginClassNotFoundException(pluginClass)
                        }
                    }
                    @Suppress("UNUSED_EXPRESSION")
                    constructors // access to initialize

                    val createInstance: () -> MusicPlugin = createInstance@ {
                        val musicAppConstructor = constructors.find {
                            (it.parameterCount == 1) && (it.parameterTypes[0] == MusicApp::class.java)
                        }

                        if (musicAppConstructor != null) {
                            return@createInstance musicAppConstructor.newInstance(this) as MusicPlugin

                        } else {
                            val emptyConstructor = constructors.find { it.parameterCount == 0 }

                            if (emptyConstructor != null) {
                                val instance = emptyConstructor.newInstance() as MusicPlugin
                                instance.javaClass
                                    .getMethod("setMusicAppInstance", MusicApp::class.java)
                                    .invoke(instance, this)

                                return@createInstance instance

                            } else {
                                throw IllegalPluginException(Class.forName(pluginClass, false, loader))
                            }
                        }
                    }

                    val instance by lazy {
                        try {
                            createInstance()
                        } catch (e: Exception) {
                            throw IllegalPluginException(Class.forName(pluginClass, false, loader))
                        }
                    }
                    @Suppress("UNUSED_EXPRESSION")
                    instance // access to initialize

                    try {
                        instance.musicAppInstance
                    } catch (e: Exception) {
                        throw IllegalPluginException(Class.forName(pluginClass, false, loader))
                    }

                    result.add(instance)
                    usedPlugins[pluginClass] = true
                }
            }
        }

        return@lazy result
    }

    fun findSinglePlugin(pluginClassName: String): MusicPlugin? {
        val exact = plugins.find { it.pluginId == pluginClassName }
        if (exact != null) {
            return exact
        }

        return try {
            plugins.singleOrNull { musicPlugin ->
                musicPlugin::class.superclasses.any { it.qualifiedName == pluginClassName }
            }
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    fun <T : MusicPlugin> getPlugins(pluginClass: Class<T>): List<T> =
        plugins.filterIsInstance(pluginClass)

    private val musicLibraryContributors: List<MusicLibraryContributorPlugin>
        get() = getPlugins(MusicLibraryContributorPlugin::class.java)

    protected val playbackListeners: List<PlaybackListenerPlugin>
        get() = getPlugins(PlaybackListenerPlugin::class.java)

    val musicLibrary: MusicLibrary by lazy {
        musicLibraryContributors
            .sortedWith(compareBy({ it.preferredOrder }, { it.pluginId }))
            .fold(MusicLibrary(mutableListOf())) { acc, it -> it.contribute(acc) }
    }

    open val player: MusicPlayer by lazy {
        JLayerMusicPlayer(playbackListeners)
    }

    fun startPlayback(playlist: Playlist, fromPosition: Int) {
        player.playbackState = PlaybackState.Playing(
            PlaylistPosition(
                playlist,
                fromPosition
            ), isResumedFromPause = false
        )
    }

    fun nextOrStop(): Boolean =
        player.playbackState.playlistPosition?.let {
            val nextPosition = it.position + 1
            val newState = if (nextPosition in it.playlist.tracks.indices)
                PlaybackState.Playing(
                    PlaylistPosition(
                        it.playlist,
                        nextPosition
                    ), isResumedFromPause = false
                )
            else
                PlaybackState.Stopped
            player.playbackState = newState
            newState is PlaybackState.Playing
        } ?: false

    @Volatile
    var isClosed = false
        private set

    override fun close() {
        if (isClosed) return
        isClosed = true

        Files.createDirectories(Paths.get(pluginDataFilePath))

        plugins.forEach {
            FileOutputStream(getPluginDataFileName(it)).use { stateStream ->
                it.persist(stateStream)
            }
        }

        player.close()
    }
}