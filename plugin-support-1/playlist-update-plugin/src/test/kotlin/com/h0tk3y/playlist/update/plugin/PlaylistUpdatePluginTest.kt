package com.h0tk3y.playlist.update.plugin

import com.h0tk3y.player.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PlaylistUpdatePluginTest {
    private var playlist: Playlist

    init {
        val beepTracks = (1..4).map {
            Track(
                mapOf(
                    TrackMetadataKeys.ARTIST to "beep${it}Artist",
                    TrackMetadataKeys.NAME to "beep-$it"
                ),
                File("sounds/beep-$it.mp3")
            )
        }

        playlist = Playlist("beeps", beepTracks.toMutableList())
    }

    @Test
    fun `test add new playlist`() {
        val pluginPath = listOf(
            MusicPluginPath(
                listOf("com.h0tk3y.playlist.update.plugin.PlaylistUpdatePlugin"),
                listOf(File(pluginsPath).resolve("playlist-update-plugin.jar"))
            )
        )
        val app = MusicApp(pluginPath)
        val plugin = PlaylistUpdatePlugin(app)

        plugin.contribute(listOf("new", "test"))
        assertEquals(listOf(Playlist("test", mutableListOf())), app.musicLibrary.playlists)
        plugin.contribute(listOf("delete", "another"))
        assertEquals(listOf(Playlist("test", mutableListOf())), app.musicLibrary.playlists)
        plugin.contribute(listOf("delete", "test"))
        assertEquals(listOf<Playlist>(), app.musicLibrary.playlists)
    }

    @Test
    fun `test add new track`() {
        val pluginPath = listOf(
            MusicPluginPath(
                listOf("com.h0tk3y.playlist.update.plugin.PlaylistUpdatePlugin"),
                listOf(File(pluginsPath).resolve("playlist-update-plugin.jar"))
            )
        )
        val app = MusicApp(pluginPath)
        val plugin = PlaylistUpdatePlugin(app)

        app.musicLibrary.playlists.add(playlist)
        plugin.contribute(listOf("new", "test"))
        plugin.contribute(listOf("add", "test", "beeps", "0"))
        assertEquals(1, app.musicLibrary.playlists.find { it.name == "test" }?.tracks?.size)
        plugin.contribute(listOf("add", "test", "beeps", "1"))
        assertEquals(2, app.musicLibrary.playlists.find { it.name == "test" }?.tracks?.size)
        plugin.contribute(listOf("remove", "test", "0"))
        assertEquals(1, app.musicLibrary.playlists.find { it.name == "test" }?.tracks?.size)
        plugin.contribute(listOf("delete", "test"))
        app.musicLibrary.playlists.remove(playlist)
    }
}