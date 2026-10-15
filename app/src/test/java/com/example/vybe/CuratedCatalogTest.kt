package com.example.vybe

import com.example.vybe.data.provider.CuratedAudioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedCatalogTest {

    @Test
    fun testCatalog_hasTracksArtistsAndAlbums() {
        assertTrue(CuratedAudioCatalog.tracks.isNotEmpty())
        assertTrue(CuratedAudioCatalog.artists.isNotEmpty())
        assertTrue(CuratedAudioCatalog.albums.isNotEmpty())
    }

    @Test
    fun testTracks_haveValidMetadataAndLicense() {
        CuratedAudioCatalog.tracks.forEach { track ->
            assertFalse(track.id.isBlank())
            assertFalse(track.title.isBlank())
            assertFalse(track.artistName.isBlank())
            assertFalse(track.license.isBlank())
            assertTrue(track.durationMs > 0)
        }
    }

    @Test
    fun testTrackDurationFormatted() {
        val track = CuratedAudioCatalog.tracks.first()
        val formatted = track.durationFormatted
        assertTrue(formatted.contains(":"))
    }
}
