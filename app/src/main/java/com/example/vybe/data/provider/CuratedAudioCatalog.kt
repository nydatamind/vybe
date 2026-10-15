package com.example.vybe.data.provider

import com.example.vybe.core.model.Album
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.Track

object CuratedAudioCatalog {

    val artists: List<Artist> = listOf(
        Artist(
            id = "art_1",
            name = "A.R. Rahman",
            artworkUrl = "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg",
            bio = "Oscar & Grammy-winning composer, record producer, singer and songwriter known as the 'Mozart of Madras'.",
            genres = listOf("Bollywood", "Classical", "Film Score"),
            monthlyListeners = 14500000
        ),
        Artist(
            id = "art_2",
            name = "Arijit Singh",
            artworkUrl = "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg",
            bio = "Indian playback singer and music composer, widely regarded as the voice of soulful Indian romantic melodies.",
            genres = listOf("Bollywood", "Romantic", "Pop"),
            monthlyListeners = 18900000
        ),
        Artist(
            id = "art_3",
            name = "Shreya Ghoshal",
            artworkUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg",
            bio = "4-time National Award-winning Indian playback singer celebrated for her extraordinary vocal dexterity and hit songs.",
            genres = listOf("Bollywood", "Melody", "Classical"),
            monthlyListeners = 13800000
        ),
        Artist(
            id = "art_4",
            name = "Anirudh Ravichander",
            artworkUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg",
            bio = "Sensational Indian composer and singer ruling South Indian cinema and pan-Indian chartbusters with high-energy beats.",
            genres = listOf("South", "Rock", "Pop"),
            monthlyListeners = 15200000
        ),
        Artist(
            id = "art_5",
            name = "Pritam",
            artworkUrl = "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg",
            bio = "Prolific Indian music director and composer behind Bollywood's biggest blockbusters for over two decades.",
            genres = listOf("Bollywood", "Pop", "Melody"),
            monthlyListeners = 16800000
        ),
        Artist(
            id = "art_6",
            name = "Diljit Dosanjh",
            artworkUrl = "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073054_500x500.jpg",
            bio = "Global Punjabi icon, singer, and actor taking Punjabi pop and bhangra to international stages.",
            genres = listOf("Punjabi", "Pop", "Bhangra"),
            monthlyListeners = 13200000
        ),
        Artist(
            id = "art_7",
            name = "Badshah",
            artworkUrl = "https://c.saavncdn.com/artists/Badshah_006_20241118064015_500x500.jpg",
            bio = "Indian rapper, singer, and producer known for chart-topping party anthems and Desi Hip-Hop.",
            genres = listOf("Desi Hip-Hop", "Punjabi", "Pop"),
            monthlyListeners = 11400000
        ),
        Artist(
            id = "art_8",
            name = "Sid Sriram",
            artworkUrl = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg",
            bio = "Indian-American Carnatic musician, playback singer, and songwriter known for soul-stirring vocal range.",
            genres = listOf("Carnatic", "Melody", "Soul"),
            monthlyListeners = 9500000
        )
    )

    val albums: List<Album> = listOf(
        Album(
            id = "alb_1",
            title = "Rockstar",
            artistId = "art_1",
            artistName = "A.R. Rahman",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1080&auto=format&fit=crop&q=85",
            releaseDate = "2011",
            genre = "Bollywood"
        ),
        Album(
            id = "alb_2",
            title = "Brahmastra",
            artistId = "art_5",
            artistName = "Pritam & Arijit Singh",
            artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1080&auto=format&fit=crop&q=85",
            releaseDate = "2022",
            genre = "Bollywood"
        ),
        Album(
            id = "alb_3",
            title = "Jawan",
            artistId = "art_4",
            artistName = "Anirudh Ravichander",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/1e/ff/32/1eff3216-190d-6fd9-8f68-acbba846e6ee/8903431956026_cover.jpg/1200x1200bb.jpg",
            releaseDate = "2023",
            genre = "Bollywood"
        ),
        Album(
            id = "alb_4",
            title = "Guru",
            artistId = "art_1",
            artistName = "A.R. Rahman",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/8d/8c/69/8d8c697e-f108-9e69-0cb1-475b45ab26a8/886970389020.jpg/1200x1200bb.jpg",
            releaseDate = "2006",
            genre = "Bollywood"
        ),
        Album(
            id = "alb_5",
            title = "Ghost",
            artistId = "art_6",
            artistName = "Diljit Dosanjh",
            artworkUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=1080&auto=format&fit=crop&q=85",
            releaseDate = "2023",
            genre = "Punjabi"
        )
    )

    val tracks: List<Track> = listOf(
        Track(
            id = "trk_1",
            title = "Kesariya (From \"Brahmastra\")",
            artistId = "art_2",
            artistName = "Pritam, Arijit Singh & Amitabh Bhattacharya",
            albumId = "alb_2",
            albumName = "Brahmastra",
            artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1080&auto=format&fit=crop&q=85",
            durationMs = 268000,
            streamUrl = "",
            genre = "Bollywood",
            provider = "VYBECatalog",
            license = "Full Track Streaming",
            releaseDate = "2022"
        ),
        Track(
            id = "trk_2",
            title = "Kun Faya Kun (From \"Rockstar\")",
            artistId = "art_1",
            artistName = "A.R. Rahman, Javed Ali & Mohit Chauhan",
            albumId = "alb_1",
            albumName = "Rockstar",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1080&auto=format&fit=crop&q=85",
            durationMs = 470000,
            streamUrl = "",
            genre = "Bollywood",
            provider = "VYBECatalog",
            license = "Full Track Streaming",
            releaseDate = "2011"
        ),
        Track(
            id = "trk_3",
            title = "Chaleya (From \"Jawan\")",
            artistId = "art_4",
            artistName = "Anirudh Ravichander, Arijit Singh & Shilpa Rao",
            albumId = "alb_3",
            albumName = "Jawan",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/1e/ff/32/1eff3216-190d-6fd9-8f68-acbba846e6ee/8903431956026_cover.jpg/1200x1200bb.jpg",
            durationMs = 200000,
            streamUrl = "",
            genre = "Bollywood",
            provider = "VYBECatalog",
            license = "Full Track Streaming",
            releaseDate = "2023"
        ),
        Track(
            id = "trk_4",
            title = "Tere Bina",
            artistId = "art_1",
            artistName = "A.R. Rahman & Chinmayi Sripada",
            albumId = "alb_4",
            albumName = "Guru",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/8d/8c/69/8d8c697e-f108-9e69-0cb1-475b45ab26a8/886970389020.jpg/1200x1200bb.jpg",
            durationMs = 309000,
            streamUrl = "",
            genre = "Bollywood",
            provider = "VYBECatalog",
            license = "Full Track Streaming",
            releaseDate = "2006"
        ),
        Track(
            id = "trk_5",
            title = "Dil Hi Toh Hai",
            artistId = "art_2",
            artistName = "Pritam, Arijit Singh & Antara Mitra",
            albumId = "alb_2",
            albumName = "Hindi Unwind",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/b9/a9/35/b9a935d5-b41a-a468-ecd5-02f38bde6bc6/8909024121673.png/1200x1200bb.jpg",
            durationMs = 280000,
            streamUrl = "",
            genre = "Bollywood",
            provider = "VYBECatalog",
            license = "Full Track Streaming",
            releaseDate = "2019"
        ),
        Track(
            id = "trk_6",
            title = "Raanjhana - Lofi Mix",
            artistId = "art_2",
            artistName = "Arijit Singh, Asad Khan & Raqueeb Alam",
            albumId = "alb_2",
            albumName = "Lofi Love Stories",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/fb/91/ea/fb91ea08-e1ad-4372-c8a6-da114267df73/8909024121703.png/1200x1200bb.jpg",
            durationMs = 304000,
            streamUrl = "",
            genre = "Lofi",
            provider = "VYBECatalog",
            license = "Full Track Streaming",
            releaseDate = "2024"
        ),
        Track(
            id = "trk_7",
            title = "Sitaare (From \"Ikkis\")",
            artistId = "art_2",
            artistName = "Arijit Singh & Amitabh Bhattacharya",
            albumId = "alb_2",
            albumName = "Sitaare",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/5d/65/89/5d6589ac-c640-3515-0327-efa185104d83/196873834856.jpg/1200x1200bb.jpg",
            durationMs = 242000,
            streamUrl = "",
            genre = "Bollywood",
            provider = "VYBECatalog",
            license = "Full Track Streaming",
            releaseDate = "2025"
        )
    )

    fun getTracksForAlbum(albumId: String): List<Track> = tracks.filter { it.albumId == albumId }

    fun getTracksForArtist(artistId: String): List<Track> = tracks.filter { it.artistId == artistId }
}
