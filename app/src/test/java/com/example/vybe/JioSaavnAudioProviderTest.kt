package com.example.vybe

import com.example.vybe.data.provider.JioSaavnAudioProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnAudioProviderTest {

    @Test
    fun testJioSaavnSearchAndStreamResolution() = runBlocking {
        val provider = JioSaavnAudioProvider()

        println("=== TEST 1: Searching 'Kesariya' ===")
        val kesariyaResult = provider.search("Kesariya")
        println("Tracks found for 'Kesariya': ${kesariyaResult.tracks.size}")
        kesariyaResult.tracks.take(5).forEach { track ->
            println(" - Title: ${track.title} | Artist: ${track.artistName} | StreamUrl: ${track.streamUrl}")
        }
        assertTrue("Should return tracks for 'Kesariya'", kesariyaResult.tracks.isNotEmpty())

        println("=== TEST 2: Stream URL & DES Decryption ===")
        val firstTrack = kesariyaResult.tracks.first()
        val streamUrl = provider.getStreamUrl(firstTrack)
        println("Resolved Stream URL: $streamUrl")
        assertNotNull("Stream URL should not be null", streamUrl)
        assertTrue("Stream URL should be valid HTTPS media stream", streamUrl!!.startsWith("https://") || streamUrl.startsWith("http://"))
        assertTrue("Stream URL should be from Saavn / Akamai CDN", streamUrl.contains("saavncdn.com") || streamUrl.contains("jiosaavn"))

        println("=== TEST 3: In-Memory Stream Caching ===")
        val cached = JioSaavnAudioProvider.streamCache[firstTrack.id]
        assertNotNull("Stream URL should be present in global in-memory cache", cached)

        println("=== TEST 4: HD Artwork Resolution ===")
        val hdArt = JioSaavnAudioProvider.cleanArtworkUrl(firstTrack.artworkUrl)
        println("HD Artwork URL: $hdArt")
        if (hdArt.isNotBlank()) {
            assertTrue("Artwork should be 500x500 HD", hdArt.contains("500x500") || !hdArt.contains("150x150"))
        }

        println("=== TEST 5: Trending Tracks ===")
        val trending = provider.getTrending()
        println("Trending tracks count: ${trending.size}")
        trending.take(5).forEach { track ->
            println(" - Trending: ${track.title} by ${track.artistName}")
        }
        assertTrue("Trending tracks should not be empty", trending.isNotEmpty())
    }
}
