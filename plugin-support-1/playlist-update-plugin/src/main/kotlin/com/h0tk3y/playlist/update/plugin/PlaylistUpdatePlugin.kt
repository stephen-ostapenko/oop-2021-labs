package com.h0tk3y.playlist.update.plugin

import com.h0tk3y.player.ConsoleHandlerPlugin
import com.h0tk3y.player.MusicApp
import com.h0tk3y.player.Playlist
import java.io.InputStream
import java.io.OutputStream

class PlaylistUpdatePlugin(override val musicAppInstance: MusicApp) : ConsoleHandlerPlugin {
    override val preferredOrder: Int
        get() = 127
    override val helpSectionName: String
        get() = "plugin to work with playlists"

    override fun printHelp() {
        println(
                """
                    new playlist_name: add new playlist with playlist_name
                    add dest_playlist_name src_playlist_name n: add the track from src_playlist_name in position n to dest_playlist_name
                    delete playlist_name: delete playlist with playlist_name
                    remove playlist_name n: delete the track from playlist_name in position n from playlist_name
                """.trimIndent()
        )
    }

    override fun contribute(current: List<String>?): List<String>? {
        if (current == null)
            return null

        val handledValue = when (current[0]) {
            "new" -> run {
                val playlistName = current.getOrNull(1) ?: return@run false
                val playlists = musicAppInstance.musicLibrary.playlists
                if (playlists.find { it.name == playlistName } != null) {
                    println("playlist $playlistName already exists")
                    return@run true
                }
                playlists.add(Playlist(playlistName, mutableListOf()))
                true
            }
            "add" -> run {
                val destPlaylistName = current.getOrNull(1) ?: return@run false
                val srcPlaylistName = current.getOrNull(2) ?: return@run false
                val position = current.getOrNull(3)?.toIntOrNull() ?: return@run false
                val playlists = musicAppInstance.musicLibrary.playlists
                val destPlaylist = playlists.find { it.name == destPlaylistName }
                        ?: let { println("destination playlist $srcPlaylistName not found"); return@run false }
                val srcPlaylist = playlists.find { it.name == srcPlaylistName }
                        ?: let { println("source playlist $srcPlaylistName not found"); return@run false }
                if (position !in srcPlaylist.tracks.indices) {
                    println("position $position out of bounds for playlist $srcPlaylistName")
                    return@run true
                }
                if (srcPlaylist.tracks[position] in destPlaylist.tracks) {
                    println("this track has already been added to $destPlaylistName")
                    return@run true
                }
                destPlaylist.tracks.add(srcPlaylist.tracks[position])
                true
            }
            "delete" -> run {
                val playlistName = current.getOrNull(1) ?: return@run false
                val playlists = musicAppInstance.musicLibrary.playlists
                val playlist = musicAppInstance.musicLibrary.playlists.find { it.name == playlistName }
                        ?: let { println("playlist $playlistName not found"); return@run false }
                playlists.remove(playlist)
                true
            }
            "remove" -> run {
                val playlistName = current.getOrNull(1) ?: return@run false
                val position = current.getOrNull(2)?.toIntOrNull() ?: return@run false
                val playlist = musicAppInstance.musicLibrary.playlists.find { it.name == playlistName }
                        ?: let { println("playlist $playlistName not found"); return@run false }
                if (position !in playlist.tracks.indices) {
                    println("position $position out of bounds for playlist $playlistName")
                    return@run true
                }
                playlist.tracks.remove(playlist.tracks[position])
                true
            }
            else -> false
        }

        return if (handledValue) null else current
    }

    override fun init(persistedState: InputStream?) = Unit

    override fun persist(stateStream: OutputStream) = Unit
}