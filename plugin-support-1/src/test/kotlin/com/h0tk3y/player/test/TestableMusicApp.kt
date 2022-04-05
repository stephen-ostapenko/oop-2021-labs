package com.h0tk3y.player.test

import com.h0tk3y.player.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TestableMusicApp(
    pluginPaths: List<MusicPluginPath>,
) : MusicApp(
    pluginPaths
) {
    override val player: MockPlayer by lazy {
        MockPlayer(
            playbackListeners
        )
    }
}

internal class AppTests {
    lateinit var app: TestableMusicApp

    @Before
    fun init() {
        app = TestableMusicApp(emptyList()).apply { init() }
    }

    @Test
    fun testNone() {
        val track1 = Track(mutableMapOf(), File("sounds/beep-1.mp3"))
        val track2 = Track(mutableMapOf(), File("sounds/beep-2.mp3"))
        val playlist = Playlist("my", mutableListOf(track1, track2))
        app.player.playbackState = PlaybackState.Playing(PlaylistPosition(playlist, 0), isResumedFromPause = false)
        app.player.finishedTrack()
        assertTrue(app.player.playbackState.let { it is PlaybackState.Playing && it.playlistPosition.position == 1 })
        app.player.finishedTrack()
        assertEquals(app.player.playbackState, PlaybackState.Stopped)
    }
}