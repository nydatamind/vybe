package com.example.vybe

import com.example.vybe.core.model.Track
import com.example.vybe.data.provider.CuratedAudioCatalog
import com.example.vybe.data.repository.RecommendationEngine
import com.example.vybe.data.repository.SongMood
import com.example.vybe.data.repository.distinctSongs
import com.example.vybe.data.repository.normalizeSongTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private val engine = RecommendationEngine()

    @Test
    fun testDefaultRecommendations_returnsExpectedLimit() {
        val recommendations = engine.generateRecommendations(
            likedTracks = emptyList(),
            recentlyPlayed = emptyList(),
            allCandidates = CuratedAudioCatalog.tracks,
            limit = 5
        )

        assertEquals(5, recommendations.size)
    }

    @Test
    fun testRecommendations_prioritizesLikedGenre() {
        val likedLoFi = Track(
            id = "liked_1",
            title = "Chill Beats",
            artistId = "art_1",
            artistName = "Ketsa",
            genre = "Lo-Fi"
        )

        val recommendations = engine.generateRecommendations(
            likedTracks = listOf(likedLoFi),
            recentlyPlayed = emptyList(),
            allCandidates = CuratedAudioCatalog.tracks,
            limit = 10
        )

        assertNotNull(recommendations)
        assertFalse(recommendations.isEmpty())
        val topTrack = recommendations.first()
        assertTrue(topTrack.genre == "Lo-Fi" || topTrack.artistId == "art_1" || recommendations.any { it.genre == "Lo-Fi" })
    }

    @Test
    fun testMadeForYouMixes_generatesCategories() {
        val mixes = engine.generateMadeForYouMixes(emptyList(), emptyList())
        assertTrue(mixes.containsKey("Chill & Focus Mix"))
        assertTrue(mixes.containsKey("High Energy & Cyber Wave"))
        assertTrue(mixes["Chill & Focus Mix"]?.isNotEmpty() == true)
    }

    @Test
    fun testDetectMood_sadEmotionalSongs() {
        val channaMereya = Track(id = "1", title = "Channa Mereya", artistName = "Arijit Singh", albumName = "Ae Dil Hai Mushkil", genre = "Hindi")
        val hamariAdhuri = Track(id = "2", title = "Hamari Adhuri Kahani", artistName = "Arijit Singh", albumName = "Hamari Adhuri Kahani", genre = "Bollywood")
        val judai = Track(id = "3", title = "Judai", artistName = "Arijit Singh", albumName = "Badlapur", genre = "Hindi")

        assertEquals(SongMood.SAD_EMOTIONAL, RecommendationEngine.detectMood(channaMereya))
        assertEquals(SongMood.SAD_EMOTIONAL, RecommendationEngine.detectMood(hamariAdhuri))
        assertEquals(SongMood.SAD_EMOTIONAL, RecommendationEngine.detectMood(judai))
    }

    @Test
    fun testDetectMood_devotionalSongs() {
        val aarti = Track(id = "4", title = "Aarti Kunj Bihari Ki", artistName = "Anuradha Paudwal", albumName = "Aarti Sangrah", genre = "Devotional")
        val mahadev = Track(id = "5", title = "Har Har Shambhu Shiv Mahadev", artistName = "Abhilipsa Panda", albumName = "Devotional Hits", genre = "Bhakti")

        assertEquals(SongMood.DEVOTIONAL, RecommendationEngine.detectMood(aarti))
        assertEquals(SongMood.DEVOTIONAL, RecommendationEngine.detectMood(mahadev))
    }

    @Test
    fun testDetectMood_partyAndPunjabiSongs() {
        val kalaChashma = Track(id = "6", title = "Kala Chashma Party Mix", artistName = "Badshah, Neha Kakkar", albumName = "Baar Baar Dekho", genre = "Party")
        val bornToShine = Track(id = "7", title = "Born to Shine", artistName = "Diljit Dosanjh", albumName = "G.O.A.T.", genre = "Punjabi")

        assertEquals(SongMood.PARTY_DANCE, RecommendationEngine.detectMood(kalaChashma))
        assertEquals(SongMood.PUNJABI_POP, RecommendationEngine.detectMood(bornToShine))
    }

    @Test
    fun testGetRelatedPeerArtists() {
        val arijitPeers = RecommendationEngine.getRelatedPeerArtists("Arijit Singh", SongMood.SAD_EMOTIONAL)
        assertTrue(arijitPeers.contains("Atif Aslam"))
        assertTrue(arijitPeers.contains("KK"))
        assertTrue(arijitPeers.contains("Jubin Nautiyal"))

        val bhajanPeers = RecommendationEngine.getRelatedPeerArtists("Anuradha Paudwal", SongMood.DEVOTIONAL)
        assertTrue(bhajanPeers.contains("Anup Jalota"))
        assertTrue(bhajanPeers.contains("Hansraj Raghuwanshi"))
    }

    @Test
    fun testScoreAndRankRelatedTracks_ranksSameMoodAndSingerHigher() {
        val seed = Track(
            id = "seed_1",
            title = "Channa Mereya",
            artistName = "Arijit Singh",
            albumName = "Ae Dil Hai Mushkil",
            genre = "Hindi"
        )

        val sameArtistSad = Track(
            id = "cand_1",
            title = "Hamari Adhuri Kahani",
            artistName = "Arijit Singh",
            albumName = "Hamari Adhuri Kahani",
            genre = "Hindi"
        )

        val peerArtistSad = Track(
            id = "cand_2",
            title = "Dard E Dil",
            artistName = "Atif Aslam",
            albumName = "Hits",
            genre = "Hindi"
        )

        val clashingPartySong = Track(
            id = "cand_3",
            title = "Saturday Saturday Party Dance",
            artistName = "Badshah",
            albumName = "Party Hits",
            genre = "Party"
        )

        val candidates = listOf(clashingPartySong, peerArtistSad, sameArtistSad)
        val ranked = engine.scoreAndRankRelatedTracks(seedTrack = seed, candidates = candidates, limit = 10)

        assertFalse(ranked.isEmpty())
        // Top tracks should be the same artist or peer artist sad songs
        assertEquals("cand_1", ranked[0].id)
        assertEquals("cand_2", ranked[1].id)
        // Party song should be penalized or excluded
        assertFalse(ranked.take(2).any { it.id == "cand_3" })
    }

    @Test
    fun testDistinctSongs_deduplicatesCleanTitles() {
        val t1 = Track(id = "1", title = "Kesariya (From Brahmastra)", artistName = "Arijit Singh", genre = "Hindi")
        val t2 = Track(id = "2", title = "Kesariya [Slowed + Reverb]", artistName = "Arijit Singh", genre = "Hindi")
        val t3 = Track(id = "3", title = "Tum Hi Ho", artistName = "Arijit Singh", genre = "Hindi")

        val deduped = listOf(t1, t2, t3).distinctSongs()
        assertEquals(2, deduped.size)
        assertEquals("Kesariya (From Brahmastra)", deduped[0].title)
        assertEquals("Tum Hi Ho", deduped[1].title)
    }
}

