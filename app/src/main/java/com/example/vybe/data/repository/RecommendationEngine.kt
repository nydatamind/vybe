package com.example.vybe.data.repository

import com.example.vybe.core.model.Track
import com.example.vybe.data.provider.CuratedAudioCatalog
import com.example.vybe.data.provider.OnboardingCatalog

/**
 * Supported song moods & vibes for context-aware playback and intelligent queue progression.
 */
/**
 * Supported song moods & vibes for context-aware playback and intelligent queue progression.
 */
enum class SongMood(val label: String, val searchKeywords: List<String>) {
    SAD_EMOTIONAL(
        label = "Sad & Emotional",
        searchKeywords = listOf("sad songs", "emotional songs", "heartbreak songs", "dard judai", "slowed reverb sad")
    ),
    ROMANTIC(
        label = "Romantic & Love",
        searchKeywords = listOf("romantic love songs", "love ballads", "romantic hits", "sweet melody")
    ),
    PARTY_DANCE(
        label = "Party & Dance",
        searchKeywords = listOf("party dance hits", "club dance songs", "high energy dance", "dhamaka hits")
    ),
    DEVOTIONAL(
        label = "Devotional & Bhakti",
        searchKeywords = listOf("bhajan aarti", "bhakti geet", "krishna bhajan", "devotional hits", "aarti")
    ),
    SUFI_SOULFUL(
        label = "Sufi & Soulful",
        searchKeywords = listOf("sufi songs", "qawwali hits", "soulful sufi melodies")
    ),
    PUNJABI_POP(
        label = "Punjabi Pop",
        searchKeywords = listOf("punjabi pop hits", "latest punjabi songs", "bhangra pop")
    ),
    HARYANVI(
        label = "Haryanvi Hits",
        searchKeywords = listOf("haryanvi superhit songs", "haryanvi dance hits")
    ),
    HIPHOP_RAP(
        label = "Hip-Hop & Rap",
        searchKeywords = listOf("desi hip hop rap", "trending rap songs", "drill trap hits")
    ),
    CHILL_LOFI(
        label = "Chill & Lo-Fi",
        searchKeywords = listOf("lofi acoustic songs", "chill vibes", "acoustic unplugged", "peaceful melodies")
    ),
    REGIONAL_FOLK(
        label = "Regional Folk",
        searchKeywords = listOf("traditional regional folk", "folk superhits")
    ),
    GENERAL_HIT(
        label = "Superhits",
        searchKeywords = listOf("superhit songs", "top trending hits")
    )
}

/**
 * Music ecosystems for strict category isolation and firewalling.
 * Prevents devotional (Aarti/Bhajan) from leaking into secular genres,
 * and prevents cross-regional genre contamination.
 */
enum class MusicEcosystem(val id: String, val label: String, val defaultSearchKeyword: String) {
    DEVOTIONAL("god", "Devotional & Bhakti", "bhajan aarti bhakti geet"),
    HARYANVI("haryanvi", "Haryanvi Hits", "haryanvi superhit songs"),
    PUNJABI("punjabi", "Punjabi Hits", "punjabi latest superhit songs"),
    BHOJPURI("bhojpuri", "Bhojpuri Hits", "bhojpuri superhit songs"),
    RAJASTHANI("rajasthani", "Rajasthani Hits", "rajasthani superhit songs"),
    BENGALI("bengali", "Bengali Hits", "bengali superhit songs"),
    MARATHI("marathi", "Marathi Hits", "marathi superhit songs"),
    GUJARATI("gujarati", "Gujarati Hits", "gujarati superhit songs"),
    SOUTH_INDIAN("south", "South Regional", "south indian superhit songs"),
    ENGLISH_GLOBAL("english", "International English", "english billboard pop hits"),
    HINDI_BOLLYWOOD("hindi", "Hindi Bollywood", "hindi bollywood superhit songs")
}

/**
 * Normalizes a song title by stripping movie tags, remix/lofi suffixes, and punctuation
 * to identify duplicate songs across different album releases or remixes.
 */
fun normalizeSongTitle(title: String): String {
    return title
        // Remove parenthesized/bracketed details: (From ...), (feat ...), (Remix), [Slowed + Reverb], etc.
        .replace(Regex("""(?i)\s*[\(\[\{](?:from\s+[^)\]\}]+|feat\.[^)\]\}]+|ft\.[^)\]\}]+|with\.[^)\]\}]+|remix|lofi|slowed[^)\]\}]*|reverb|dance mix|acoustic[^)\]\}]*|film version|original motion picture[^)\]\}]*|ost|reprise|version|audio|video|full song|lyric\s*video|official\s*video|dj\s*remix|club\s*mix)[\)\]\}]"""), "")
        // Remove trailing suffixes like "- From Brahmastra", "- Remix", "- Slowed + Reverb", "ft. XYZ", etc.
        .replace(Regex("""(?i)\s*[-–—]\s*(?:from\s+.*|remix|lofi|slowed.*|reverb|acoustic.*|film version|ost|dj\s*remix)"""), "")
        .replace(Regex("""[“”"‘’'–—−‐_/\\|:;]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .lowercase()
}

/**
 * Deduplicates tracks based on clean normalized song title and artist name,
 * completely eliminating identical songs repeated 5-10 times.
 */
fun List<Track>.distinctSongs(): List<Track> {
    val seen = HashSet<String>()
    val result = ArrayList<Track>()
    for (track in this) {
        val norm = normalizeSongTitle(track.title)
        val cleanArtist = track.artistName.lowercase().trim()
        val key = if (norm.length >= 3) "${norm}_${cleanArtist.take(12)}" else track.id.ifBlank { track.title.lowercase().trim() }
        if (seen.add(key)) {
            result.add(track)
        }
    }
    return result
}

class RecommendationEngine {

    companion object {
        fun normalizeSongTitle(title: String): String = com.example.vybe.data.repository.normalizeSongTitle(title)

        /**
         * Detects the music ecosystem of a track by analyzing title, artist, album, and genre tags.
         */
        fun detectEcosystem(track: Track): MusicEcosystem {
            val combined = "${track.title} ${track.artistName} ${track.albumName} ${track.genre}".lowercase().trim()
            val artist = track.artistName.lowercase().trim()
            val genre = track.genre.lowercase().trim()

            // 1. Devotional / Bhakti Check (Highest priority isolation)
            if (listOf(
                    "krishna", "shiva", "mahadev", "ram ", "shri ram", "hanuman", "bhajan", "aarti",
                    "chalisa", "govind", "radha", "bhakti", "god", "mandir", "mata ", "durga", "ganesh",
                    "ganpati", "sai ", "om namah", "gayatri", "stuti", "shloka", "vrindavan", "shyam",
                    "hare rama", "har har shambhu", "namo namo", "shiv tandav", "shankara", "radha rani",
                    "murli", "kunj bihari", "shri krishna", "vaishnav", "katha", "jai shree", "jagannath",
                    "amritwani", "kavach", "bajrang baan", "sunderkand", "ramayan"
                ).any { combined.contains(it) } || listOf(
                    "anuradha paudwal", "anup jalota", "hansraj raghuwanshi", "lakhbir singh lakkha",
                    "sachet tandon", "kanhaiya mittal", "jaya kishori", "maanya arora", "gulshan kumar",
                    "suresh wadkar"
                ).any { artist.contains(it) } || genre.contains("god") || genre.contains("devotional") || genre.contains("bhakti")
            ) {
                return MusicEcosystem.DEVOTIONAL
            }

            // 2. Haryanvi Check
            if (genre.contains("haryanvi") || listOf("haryanvi", "haryana", "ragni").any { combined.contains(it) } ||
                listOf(
                    "masoom sharma", "renuka panwar", "gulzaar chhaniwala", "khasa aala chahar", "kd desirock",
                    "amit saini rohtakiya", "diler kharkiya", "aman jaji", "raju punjabi", "ruchika jangid",
                    "sapna choudhary", "vicky kajla", "pranjal dahiya", "sumit goswami", "bintu pabra",
                    "ajay hooda", "surender romio", "manisha sharma", "devender ahlawat", "kavita shobu",
                    "annu kadyan", "binder danoda", "sonika singh", "shiva choudhary", "mohit sharma",
                    "raj mawar", "khasa aala", "gulzaar"
                ).any { artist.contains(it) }
            ) {
                return MusicEcosystem.HARYANVI
            }

            // 3. Punjabi Check
            if (genre.contains("punjabi") || listOf("punjabi", "bhangra", "pind", "jatt").any { combined.contains(it) } ||
                listOf(
                    "diljit dosanjh", "karan aujla", "ap dhillon", "sidhu moosewala", "shubh", "arjan dhillon",
                    "ammy virk", "harrdy sandhu", "guru randhawa", "jordan sandhu", "gurnam bhullar",
                    "jassie gill", "sunanda sharma", "nimrat khaira", "kulwinder billa", "b praak", "jaani",
                    "mankirt aulakh", "parmish verma", "prem dhillon", "tegi pannu", "wazir patar",
                    "cheetah", "chani nattan", "kaka", "r nait", "korala maan", "garry sandhu", "sidhu moose", "diljit"
                ).any { artist.contains(it) }
            ) {
                return MusicEcosystem.PUNJABI
            }

            // 4. Bhojpuri Check
            if (genre.contains("bhojpuri") || combined.contains("bhojpuri") ||
                listOf(
                    "pawan singh", "khesari lal yadav", "shilpi raj", "ritesh pandey", "arvind akela kallu",
                    "neelkamal singh", "akshara singh", "pramod premi yadav", "gunjan singh",
                    "antra singh priyanka", "ankush raja", "chandan chanchal", "samar singh", "khesari"
                ).any { artist.contains(it) }
            ) {
                return MusicEcosystem.BHOJPURI
            }

            // 5. Rajasthani Check
            if (genre.contains("rajasthani") || listOf("rajasthani", "marwadi", "marwari").any { combined.contains(it) } ||
                listOf("mame khan", "seema mishra", "prakash mali", "rani rangili", "ila arun", "chhotu singh rawna").any { artist.contains(it) }
            ) {
                return MusicEcosystem.RAJASTHANI
            }

            // 6. Bengali Check
            if (genre.contains("bengali") || listOf("bengali", "bangla").any { combined.contains(it) } ||
                listOf("anupam roy", "nachiketa chakraborty", "rupam islam", "jeet gannguli", "nachiketa", "rupam").any { artist.contains(it) }
            ) {
                return MusicEcosystem.BENGALI
            }

            // 7. Marathi Check
            if (genre.contains("marathi") || combined.contains("marathi") ||
                listOf("ajay-atul", "bela shende", "swapnil bandodkar", "avdhoot gupte", "aadarsh shinde", "mahesh kale", "ajay atul").any { artist.contains(it) }
            ) {
                return MusicEcosystem.MARATHI
            }

            // 8. Gujarati Check
            if (genre.contains("gujarati") || listOf("gujarati", "garba", "dandiya").any { combined.contains(it) } ||
                listOf("kinjal dave", "geeta rabari", "jignesh kaviraj", "aditya gadhvi", "osman mir").any { artist.contains(it) }
            ) {
                return MusicEcosystem.GUJARATI
            }

            // 9. South Indian Check
            if (listOf("tamil", "telugu", "kannada", "malayalam").any { genre.contains(it) || combined.contains(it) } ||
                listOf("anirudh ravichander", "anirudh", "sid sriram", "devi sri prasad", "thaman", "sanjith hegde", "vineeth sreenivasan", "sushin shyam").any { artist.contains(it) }
            ) {
                return MusicEcosystem.SOUTH_INDIAN
            }

            // 10. English / Global Check
            if (genre.contains("english") || genre.contains("international") ||
                listOf("justin bieber", "taylor swift", "the weeknd", "ed sheeran", "drake", "eminem", "billie eilish", "dua lipa", "post malone", "ariana grande", "bruno mars").any { artist.contains(it) }
            ) {
                return MusicEcosystem.ENGLISH_GLOBAL
            }

            // 11. Default: Hindi Bollywood
            return MusicEcosystem.HINDI_BOLLYWOOD
        }

        /**
         * Category Firewall: Strictly checks if two ecosystems can share recommendation space.
         * Devotional is 100% isolated. Regional languages never randomly jump across borders.
         */
        fun areEcosystemsCompatible(seed: MusicEcosystem, candidate: MusicEcosystem): Boolean {
            if (seed == MusicEcosystem.DEVOTIONAL) {
                return candidate == MusicEcosystem.DEVOTIONAL
            }
            if (candidate == MusicEcosystem.DEVOTIONAL) {
                return false // Non-devotional track strictly CANNOT recommend devotional content
            }
            return seed == candidate
        }

        /**
         * Detects the mood/vibe of a track by analyzing title, artist, album, and genre tags.
         */
        fun detectMood(track: Track): SongMood {
            val combined = "${track.title} ${track.artistName} ${track.albumName} ${track.genre}".lowercase()

            // 1. Devotional / Bhakti Check (Highest priority)
            if (listOf(
                    "krishna", "shiva", "mahadev", "ram ", "shri ram", "hanuman", "bhajan", "aarti",
                    "chalisa", "govind", "radha", "bhakti", "god", "mandir", "mata ", "durga", "ganesh",
                    "ganpati", "sai", "om namah", "gayatri", "stuti", "shloka", "vrindavan", "shyam",
                    "hare rama", "har har shambhu", "namo namo", "shiv tandav", "shankara", "radha rani",
                    "murli", "kunj bihari", "shri krishna", "vaishnav", "katha", "jai shree", "jagannath"
                ).any { combined.contains(it) }
            ) {
                return SongMood.DEVOTIONAL
            }

            // 2. Sad / Emotional / Heartbreak Check
            if (listOf(
                    "sad", "emotional", "heartbreak", "breakup", "dard", "judai", "bewafa", "tanhai",
                    "rona", "roye", "channa mereya", "agar tum saath ho", "tujhe bhula diya", "hamari adhuri",
                    "khairiyat", "lonely", "broken", "crying", "tears", "alvida", "tujhe sochta hoon",
                    "ae dil hai mushkil", "bekhayali", "dua ", "phir bhi tumko chaahunga", "tera ban jaunga",
                    "tadap", "virah", "sitam", "faasle", "gham", "ashq", "dard dilo ke", "lut gaye",
                    "mann bharryaa", "sakhiyan", "qismat", "filhall", "dil tod ke", "wafa", "kasam",
                    "musafir", "mile ho tum", "humnava", "tera chehra", "soch na sake"
                ).any { combined.contains(it) }
            ) {
                return SongMood.SAD_EMOTIONAL
            }

            // 3. Sufi & Qawwali Check
            if (listOf(
                    "sufi", "qawwali", "khwaja", "kun faya", "arziyan", "maula", "bulleya", "ali maula",
                    "duma dum", "mast qalandar", "chhap tilak", "afreen", "rashke qamar", "tajdar e haram",
                    "sajda", "chaap tilak", "damadam"
                ).any { combined.contains(it) }
            ) {
                return SongMood.SUFI_SOULFUL
            }

            // 4. Punjabi Pop & Swag
            if (listOf(
                    "punjabi", "jatt", "pind", "soorme", "gabru", "mutiyaar", "diljit", "karan aujla",
                    "sidhu moose", "ap dhillon", "shubh", "arjan dhillon", "ammy virk", "bhangra",
                    "softly", "cheques", "goat", "born to shine", "lover", "wavy", "winning speech"
                ).any { combined.contains(it) }
            ) {
                return SongMood.PUNJABI_POP
            }

            // 5. Haryanvi Check
            if (listOf(
                    "haryanvi", "haryana", "ragni", "masoom sharma", "renuka panwar", "gulzaar",
                    "khasa aala", "kd desirock", "bahu", "kale je libaas", "system", "desi desi",
                    "aman jaji", "raju punjabi"
                ).any { combined.contains(it) }
            ) {
                return SongMood.HARYANVI
            }

            // 6. Hip-Hop / Rap
            if (listOf(
                    "rap", "hip hop", "hip-hop", "trap", "drill", "gully", "hustle", "295",
                    "same beef", "krsna", "divine", "raftaar", "mc stan", "seedhe maut", "emiway"
                ).any { combined.contains(it) }
            ) {
                return SongMood.HIPHOP_RAP
            }

            // 7. Party & High Energy
            if (listOf(
                    "party", "dance", "club", "daru", "nach", "hookah", "garmi", "kala chashma",
                    "masti", "dhamaka", "dj remix", "dhol", "chogada", "kamariya", "sauda khara",
                    "dilbar", "proper patola", "saturday saturday", "kar gayi chull", "sheila ki jawani",
                    "chittiyaan kalaiyaan", "bom diggy", "swag se swagat", "badshah", "honey singh", "neha kakkar"
                ).any { combined.contains(it) }
            ) {
                return SongMood.PARTY_DANCE
            }

            // 8. Romantic / Melodic
            if (listOf(
                    "love", "romantic", "pyaar", "ishq", "mohabbat", "dil", "sanam", "humsafar",
                    "tum hi ho", "raataan lambiyan", "kesariya", "shiddat", "jaan", "hawayein",
                    "tere hawaale", "apna bana le", "deva deva", "satranga", "pehla nasha",
                    "sunn raha hai", "bol do na zara", "kaun tujhe", "tum mile", "kinna sona", "sukoon"
                ).any { combined.contains(it) }
            ) {
                return SongMood.ROMANTIC
            }

            // 9. Chill & Lo-Fi
            if (listOf(
                    "lofi", "lo-fi", "chill", "acoustic", "unplugged", "ambient", "peaceful",
                    "prateek kuhad", "anuv jain", "jasleen royal", "cold/mess", "husn", "baarishein"
                ).any { combined.contains(it) }
            ) {
                return SongMood.CHILL_LOFI
            }

            // 10. Regional Folk
            if (listOf("bhojpuri", "rajasthani", "marathi", "gujarati", "tamil", "telugu", "malayalam", "kannada", "bengali").any { combined.contains(it) }) {
                return SongMood.REGIONAL_FOLK
            }

            return SongMood.GENERAL_HIT
        }

        /**
         * Returns curated peer singer clusters matching the primary artist, mood, and music ecosystem.
         */
        fun getRelatedPeerArtists(
            artistName: String,
            mood: SongMood,
            ecosystem: MusicEcosystem? = null
        ): List<String> {
            val cleanArtist = artistName.lowercase().trim()
            val effectiveEco = ecosystem ?: when {
                mood == SongMood.DEVOTIONAL || listOf("anuradha", "anup jalota", "hansraj", "sachet").any { cleanArtist.contains(it) } -> MusicEcosystem.DEVOTIONAL
                mood == SongMood.HARYANVI || listOf("masoom", "renuka", "gulzaar", "khasa aala", "aman jaji", "raju punjabi").any { cleanArtist.contains(it) } -> MusicEcosystem.HARYANVI
                mood == SongMood.PUNJABI_POP || listOf("diljit", "karan aujla", "sidhu", "ap dhillon", "shubh").any { cleanArtist.contains(it) } -> MusicEcosystem.PUNJABI
                listOf("pawan singh", "khesari", "shilpi raj", "ritesh pandey").any { cleanArtist.contains(it) } -> MusicEcosystem.BHOJPURI
                listOf("mame khan", "seema mishra", "prakash mali").any { cleanArtist.contains(it) } -> MusicEcosystem.RAJASTHANI
                listOf("anupam roy", "nachiketa", "rupam").any { cleanArtist.contains(it) } -> MusicEcosystem.BENGALI
                listOf("ajay", "atul", "bela shende").any { cleanArtist.contains(it) } -> MusicEcosystem.MARATHI
                listOf("kinjal dave", "geeta rabari", "aditya gadhvi").any { cleanArtist.contains(it) } -> MusicEcosystem.GUJARATI
                listOf("anirudh", "sid sriram", "devi sri", "thaman").any { cleanArtist.contains(it) } -> MusicEcosystem.SOUTH_INDIAN
                listOf("justin bieber", "taylor swift", "the weeknd").any { cleanArtist.contains(it) } -> MusicEcosystem.ENGLISH_GLOBAL
                else -> MusicEcosystem.HINDI_BOLLYWOOD
            }

            return when (effectiveEco) {
                // 1. Devotional / Bhakti Singers
                MusicEcosystem.DEVOTIONAL -> listOf(
                    "Jubin Nautiyal", "Hansraj Raghuwanshi", "Anuradha Paudwal", "Anup Jalota",
                    "Sachet Tandon", "Parampara Tandon", "Lakhbir Singh Lakkha", "Suresh Wadkar",
                    "Kanhaiya Mittal", "Pujya Jaya Kishori", "Maanya Arora"
                )

                // 2. Haryanvi Peers
                MusicEcosystem.HARYANVI -> listOf(
                    "Masoom Sharma", "Renuka Panwar", "Gulzaar Chhaniwala", "Khasa Aala Chahar",
                    "KD Desirock", "Aman Jaji", "Raju Punjabi", "Amit Saini Rohtakiya",
                    "Diler Kharkiya", "Ruchika Jangid", "Sapna Choudhary", "Vicky Kajla", "Pranjal Dahiya"
                )

                // 3. Punjabi Pop Peers
                MusicEcosystem.PUNJABI -> listOf(
                    "Diljit Dosanjh", "Karan Aujla", "AP Dhillon", "Sidhu Moosewala",
                    "Shubh", "Arjan Dhillon", "Ammy Virk", "Harrdy Sandhu", "Guru Randhawa", "Jordan Sandhu"
                )

                // 4. Bhojpuri Peers
                MusicEcosystem.BHOJPURI -> listOf(
                    "Pawan Singh", "Khesari Lal Yadav", "Shilpi Raj", "Ritesh Pandey",
                    "Arvind Akela Kallu", "Neelkamal Singh", "Akshara Singh", "Pramod Premi Yadav"
                )

                // 5. Rajasthani Peers
                MusicEcosystem.RAJASTHANI -> listOf(
                    "Mame Khan", "Seema Mishra", "Prakash Mali", "Choudhary", "Rani Rangili", "Ila Arun", "Chhotu Singh Rawna"
                )

                // 6. Bengali Peers
                MusicEcosystem.BENGALI -> listOf(
                    "Shreya Ghoshal", "Arijit Singh", "Anupam Roy", "Monali Thakur",
                    "Nachiketa Chakraborty", "Rupam Islam", "Jeet Gannguli"
                )

                // 7. Marathi Peers
                MusicEcosystem.MARATHI -> listOf(
                    "Ajay-Atul", "Bela Shende", "Swapnil Bandodkar", "Avdhoot Gupte",
                    "Aadarsh Shinde", "Shreya Ghoshal", "Mahesh Kale"
                )

                // 8. Gujarati Peers
                MusicEcosystem.GUJARATI -> listOf(
                    "Darshan Raval", "Kinjal Dave", "Geeta Rabari", "Jignesh Kaviraj",
                    "Aditya Gadhvi", "Osman Mir"
                )

                // 9. South Indian Peers
                MusicEcosystem.SOUTH_INDIAN -> listOf(
                    "Anirudh Ravichander", "Sid Sriram", "Devi Sri Prasad", "Thaman S",
                    "Sanjith Hegde", "Vineeth Sreenivasan", "Sushin Shyam", "A.R. Rahman"
                )

                // 10. International Peers
                MusicEcosystem.ENGLISH_GLOBAL -> listOf(
                    "Justin Bieber", "Taylor Swift", "The Weeknd", "Ed Sheeran",
                    "Drake", "Eminem", "Billie Eilish", "Dua Lipa", "Post Malone"
                )

                // 11. Hindi Bollywood (Fine-grained by mood)
                MusicEcosystem.HINDI_BOLLYWOOD -> {
                    when (mood) {
                        SongMood.SAD_EMOTIONAL -> listOf("Arijit Singh", "Atif Aslam", "KK", "Mohit Chauhan", "Jubin Nautiyal", "Vishal Mishra", "Mustafa Zahid", "Stebin Ben", "B Praak", "Pritam")
                        SongMood.PARTY_DANCE -> listOf("Badshah", "Yo Yo Honey Singh", "Neha Kakkar", "Mika Singh", "Guru Randhawa", "Diljit Dosanjh", "Sukhwinder Singh")
                        SongMood.SUFI_SOULFUL -> listOf("Rahat Fateh Ali Khan", "Nusrat Fateh Ali Khan", "Kailash Kher", "Javed Ali", "Wadali Brothers", "A.R. Rahman")
                        SongMood.CHILL_LOFI -> listOf("Prateek Kuhad", "Anuv Jain", "Jasleen Royal", "Darshan Raval", "Armaan Malik", "Arijit Singh")
                        else -> listOf("Arijit Singh", "Pritam", "Shreya Ghoshal", "Atif Aslam", "KK", "Jubin Nautiyal", "Armaan Malik", "Sonu Nigam", "Mohit Chauhan")
                    }
                }
            }
        }
    }

    /**
     * Scores, filters, and sequences candidate tracks based on mood consistency,
     * singer continuity, and genre harmony.
     */
    fun scoreAndRankRelatedTracks(
        seedTrack: Track,
        candidates: List<Track>,
        limit: Int = 20
    ): List<Track> {
        val seedEcosystem = detectEcosystem(seedTrack)
        val seedMood = detectMood(seedTrack)
        val seedArtist = seedTrack.artistName.lowercase().trim()
        val seedLanguage = seedTrack.genre.lowercase().trim()
        val peerArtists = getRelatedPeerArtists(seedTrack.artistName, seedMood, seedEcosystem).map { it.lowercase().trim() }

        val cleanCandidates = candidates
            .filter { it.id != seedTrack.id && !it.title.equals(seedTrack.title, ignoreCase = true) }
            .distinctSongs()

        val scored = cleanCandidates.mapNotNull { track ->
            val trackEcosystem = detectEcosystem(track)
            if (!areEcosystemsCompatible(seedEcosystem, trackEcosystem)) {
                return@mapNotNull null
            }

            var score = 0
            val trackMood = detectMood(track)
            val trackArtist = track.artistName.lowercase().trim()
            val trackGenre = track.genre.lowercase().trim()

            // 1. Same Primary Artist Bonus (+45 points)
            if (seedArtist.isNotBlank() && (trackArtist.contains(seedArtist) || seedArtist.contains(trackArtist))) {
                score += 45
            }

            // 2. Peer Artist in Same Mood Cluster (+25 points)
            if (peerArtists.any { trackArtist.contains(it) || it.contains(trackArtist) }) {
                score += 25
            }

            // 3. Exact Same Mood Bonus (+35 points)
            if (trackMood == seedMood) {
                score += 35
            } else if (
                (seedMood == SongMood.SAD_EMOTIONAL && trackMood == SongMood.ROMANTIC) ||
                (seedMood == SongMood.ROMANTIC && trackMood == SongMood.SAD_EMOTIONAL) ||
                (seedMood == SongMood.ROMANTIC && trackMood == SongMood.CHILL_LOFI) ||
                (seedMood == SongMood.SUFI_SOULFUL && trackMood == SongMood.SAD_EMOTIONAL) ||
                (seedMood == SongMood.PARTY_DANCE && trackMood == SongMood.PUNJABI_POP)
            ) {
                // Harmonic mood compatibility (+15 points)
                score += 15
            } else if (
                // Harsh mood clash penalties (-50 points)
                (seedMood == SongMood.DEVOTIONAL && trackMood == SongMood.PARTY_DANCE) ||
                (seedMood == SongMood.DEVOTIONAL && trackMood == SongMood.HIPHOP_RAP) ||
                (seedMood == SongMood.SAD_EMOTIONAL && trackMood == SongMood.PARTY_DANCE) ||
                (seedMood == SongMood.PARTY_DANCE && trackMood == SongMood.SAD_EMOTIONAL)
            ) {
                score -= 50
            }

            // 4. Same Language / Region Bonus (+15 points)
            if (seedLanguage.isNotBlank() && (trackGenre.contains(seedLanguage) || seedLanguage.contains(trackGenre))) {
                score += 15
            }

            Pair(track, score)
        }

        return scored
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctSongs()
            .take(limit)
    }

    /**
     * Intelligent Next Song Recommendation + Dynamic Autoplay Engine.
     * Evaluates multi-dimensional signals:
     * - Strict category isolation & firewall: NEVER cross incompatible genres (e.g. Aarti/Bhajan vs Haryanvi/Bollywood)
     * - Seed primary artist continuity & peer mood cluster
     * - Mood congruence and language harmony
     * - User's selected "What Do You Like" onboarding preferences within compatible ecosystem
     * - User's frequently & daily played songs (Room DB history) within compatible ecosystem
     * - Fast-skip penalty / negative feedback
     * - Serendipitous fresh artist discovery within same ecosystem
     * - Strict deduplication against active queue, skipped tracks, and session played tracks
     */
    fun generateAutoplayQueue(
        seedTrack: Track,
        historyTracks: List<Track> = emptyList(),
        mostPlayedTracks: List<Track> = emptyList(),
        likedTracks: List<Track> = emptyList(),
        preferredLanguages: Set<String> = emptySet(),
        preferredArtists: Set<String> = emptySet(),
        skippedTrackIds: Set<String> = emptySet(),
        skippedTitles: Set<String> = emptySet(),
        sessionPlayedTrackIds: Set<String> = emptySet(),
        sessionPlayedTitles: Set<String> = emptySet(),
        currentQueueIds: Set<String> = emptySet(),
        candidatePool: List<Track> = emptyList(),
        limit: Int = 20
    ): List<Track> {
        val seedNormTitle = normalizeSongTitle(seedTrack.title)
        val seedArtist = seedTrack.artistName.lowercase().trim()
        val seedMood = detectMood(seedTrack)
        val seedEcosystem = detectEcosystem(seedTrack)
        val seedGenre = seedTrack.genre.lowercase().trim()
        val peerArtists = getRelatedPeerArtists(seedTrack.artistName, seedMood, seedEcosystem).map { it.lowercase().trim() }

        val cleanPrefLangs = preferredLanguages.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()
        val cleanPrefArtists = preferredArtists.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()

        val cleanSkippedIds = skippedTrackIds.map { it.trim() }.toSet()
        val cleanSkippedTitles = skippedTitles.map { normalizeSongTitle(it) }.toSet()
        val cleanSessionPlayedIds = sessionPlayedTrackIds.map { it.trim() }.toSet()
        val cleanSessionPlayedTitles = sessionPlayedTitles.map { normalizeSongTitle(it) }.toSet()
        val cleanCurrentQueueIds = currentQueueIds.map { it.trim() }.toSet()

        // Gather candidate track pool and deduplicate
        val allCandidates = candidatePool.distinctSongs()

        val scoredList = mutableListOf<Pair<Track, Int>>()
        val seenNormTitles = mutableSetOf<String>()
        seenNormTitles.add(seedNormTitle)

        for (candidate in allCandidates) {
            val candNormTitle = normalizeSongTitle(candidate.title)
            val candId = candidate.id.trim()
            val candArtist = candidate.artistName.lowercase().trim()
            val candGenre = candidate.genre.lowercase().trim()
            val candMood = detectMood(candidate)
            val candEcosystem = detectEcosystem(candidate)

            // HARD FIREWALL: Incompatible ecosystems are discarded immediately (Zero leakage of Aarti/Bhajan into secular music)
            if (!areEcosystemsCompatible(seedEcosystem, candEcosystem)) {
                continue
            }

            // Strict deduplication: Don't repeat seed song, already queued tracks, or session played tracks
            if (candId == seedTrack.id.trim() || candNormTitle == seedNormTitle) continue
            if (cleanCurrentQueueIds.contains(candId)) continue
            if (cleanSessionPlayedIds.contains(candId) || cleanSessionPlayedTitles.contains(candNormTitle)) continue
            if (!seenNormTitles.add(candNormTitle)) continue

            var score = 15 // Baseline validity score

            // 1. Negative Feedback: Fast-skip penalty (-80 points)
            if (cleanSkippedIds.contains(candId) || cleanSkippedTitles.contains(candNormTitle)) {
                score -= 80
            }

            // 2. Same Primary Artist Priority (+55 points)
            if (seedArtist.isNotBlank() && (candArtist.contains(seedArtist) || seedArtist.contains(candArtist))) {
                score += 55
            }

            // 3. Peer Artist in Mood/Ecosystem Cluster (+35 points)
            if (peerArtists.any { candArtist.contains(it) || it.contains(candArtist) }) {
                score += 35
            }

            // 4. Mood Harmony (+35 for exact mood, +15 for harmonic, -60 for clash)
            if (candMood == seedMood) {
                score += 35
            } else if (
                (seedMood == SongMood.SAD_EMOTIONAL && candMood == SongMood.ROMANTIC) ||
                (seedMood == SongMood.ROMANTIC && candMood == SongMood.SAD_EMOTIONAL) ||
                (seedMood == SongMood.ROMANTIC && candMood == SongMood.CHILL_LOFI) ||
                (seedMood == SongMood.SUFI_SOULFUL && candMood == SongMood.SAD_EMOTIONAL) ||
                (seedMood == SongMood.PARTY_DANCE && candMood == SongMood.PUNJABI_POP)
            ) {
                score += 15
            } else if (
                (seedMood == SongMood.DEVOTIONAL && candMood != SongMood.DEVOTIONAL) ||
                (seedMood != SongMood.DEVOTIONAL && candMood == SongMood.DEVOTIONAL) ||
                (seedMood == SongMood.SAD_EMOTIONAL && candMood == SongMood.PARTY_DANCE) ||
                (seedMood == SongMood.PARTY_DANCE && candMood == SongMood.SAD_EMOTIONAL)
            ) {
                score -= 60
            }

            // 5. Language / Regional Continuity (+25 points)
            if (seedGenre.isNotBlank() && (candGenre.contains(seedGenre) || seedGenre.contains(candGenre))) {
                score += 25
            }

            // 6. User's "What Do You Like" Preferences (+30 for artist, +15 for language)
            if (cleanPrefArtists.any { candArtist.contains(it) || it.contains(candArtist) }) {
                score += 30
            }
            if (cleanPrefLangs.any { candGenre.contains(it) || it.contains(candGenre) }) {
                score += 15
            }

            // 7. Frequently & Daily Listened Songs (+25 for most played, +20 for liked, +12 for history)
            if (mostPlayedTracks.any { it.id == candId || normalizeSongTitle(it.title) == candNormTitle }) {
                score += 25
            }
            if (likedTracks.any { it.id == candId || normalizeSongTitle(it.title) == candNormTitle }) {
                score += 20
            }
            if (historyTracks.take(20).any { it.id == candId || normalizeSongTitle(it.title) == candNormTitle }) {
                score += 12
            }

            // 8. Serendipitous Discovery: Fresh related artist discovery within same ecosystem (+18 points)
            val isHeardBefore = historyTracks.any { it.artistName.equals(candidate.artistName, ignoreCase = true) } ||
                    mostPlayedTracks.any { it.artistName.equals(candidate.artistName, ignoreCase = true) }
            if (!isHeardBefore && (peerArtists.any { candArtist.contains(it) } || candMood == seedMood)) {
                score += 18
            }

            if (score > 0) {
                scoredList.add(Pair(candidate, score))
            }
        }

        // Sort by calculated score descending
        val sorted = scoredList.sortedByDescending { it.second }.map { it.first }

        // Smart Artist Pacing & Diversity:
        // Prioritize the best matching tracks while ensuring no more than 2 consecutive songs
        // come from the exact same lead artist, creating a premium radio/station flow.
        val finalQueue = mutableListOf<Track>()
        var consecutiveArtistCount = 0
        var lastLeadArtist = ""

        val remainingPool = sorted.toMutableList()
        while (finalQueue.size < limit && remainingPool.isNotEmpty()) {
            val candidateIndex = remainingPool.indexOfFirst { cand ->
                val candLead = cand.artistName.lowercase().trim().split(Regex("[,&]")).firstOrNull()?.trim() ?: cand.artistName.lowercase().trim()
                if (candLead == lastLeadArtist && consecutiveArtistCount >= 2) {
                    false
                } else {
                    true
                }
            }

            val chosen = if (candidateIndex != -1) {
                remainingPool.removeAt(candidateIndex)
            } else {
                remainingPool.removeAt(0)
            }

            val chosenLead = chosen.artistName.lowercase().trim().split(Regex("[,&]")).firstOrNull()?.trim() ?: chosen.artistName.lowercase().trim()
            if (chosenLead == lastLeadArtist) {
                consecutiveArtistCount++
            } else {
                consecutiveArtistCount = 1
                lastLeadArtist = chosenLead
            }
            finalQueue.add(chosen)
        }

        return finalQueue.distinctSongs().take(limit)
    }

    /**
     * Instantly and synchronously generates 5-10 high-affinity tracks matching the seed track's ecosystem and mood.
     * Guaranteed to execute in <1ms without any network calls, used for immediate zero-lag queue priming.
     */
    fun getInstantFallbackQueue(
        seedTrack: Track,
        availablePool: List<Track> = emptyList(),
        limit: Int = 10
    ): List<Track> {
        val seedEcosystem = detectEcosystem(seedTrack)
        val seedMood = detectMood(seedTrack)
        val seedNormTitle = normalizeSongTitle(seedTrack.title)
        val seedArtist = seedTrack.artistName.lowercase().trim()
        val peerArtists = getRelatedPeerArtists(seedTrack.artistName, seedMood, seedEcosystem).map { it.lowercase().trim() }

        val pool = (availablePool + CuratedAudioCatalog.tracks + OnboardingCatalog.defaultHitTracks).distinctSongs()

        return pool
            .filter { candidate ->
                if (candidate.id == seedTrack.id || normalizeSongTitle(candidate.title) == seedNormTitle) return@filter false
                val candEco = detectEcosystem(candidate)
                areEcosystemsCompatible(seedEcosystem, candEco)
            }
            .sortedByDescending { candidate ->
                var score = 10
                val candMood = detectMood(candidate)
                val candArtist = candidate.artistName.lowercase().trim()
                if (seedArtist.isNotBlank() && (candArtist.contains(seedArtist) || seedArtist.contains(candArtist))) score += 40
                if (peerArtists.any { candArtist.contains(it) || it.contains(candArtist) }) score += 25
                if (candMood == seedMood) score += 30
                score
            }
            .take(limit)
    }

    fun generateRecommendations(
        likedTracks: List<Track>,
        recentlyPlayed: List<Track>,
        preferredLanguages: Set<String> = emptySet(),
        preferredArtists: Set<String> = emptySet(),
        allCandidates: List<Track> = emptyList(),
        limit: Int = 15
    ): List<Track> {
        val cleanLangs = preferredLanguages.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()
        val cleanArtists = preferredArtists.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()

        val preferenceHits = if (cleanLangs.isNotEmpty() || cleanArtists.isNotEmpty()) {
            OnboardingCatalog.getHitTracksForPreferences(preferredLanguages, preferredArtists)
        } else {
            emptyList()
        }

        val combinedCandidates = (allCandidates + preferenceHits).distinctSongs()
        val uniqueCandidates = if (combinedCandidates.isNotEmpty()) {
            combinedCandidates
        } else {
            (CuratedAudioCatalog.tracks + OnboardingCatalog.defaultHitTracks).distinctSongs()
        }

        val genreScores = mutableMapOf<String, Int>()
        likedTracks.forEach { track ->
            val g = track.genre.lowercase()
            genreScores[g] = (genreScores[g] ?: 0) + 4
        }
        recentlyPlayed.take(15).forEach { track ->
            val g = track.genre.lowercase()
            genreScores[g] = (genreScores[g] ?: 0) + 2
        }

        val artistScores = mutableMapOf<String, Int>()
        val artistIdScores = mutableMapOf<String, Int>()
        likedTracks.forEach { track ->
            if (track.artistId.isNotBlank()) {
                artistIdScores[track.artistId] = (artistIdScores[track.artistId] ?: 0) + 5
            }
            val a = track.artistName.lowercase()
            artistScores[a] = (artistScores[a] ?: 0) + 5
        }
        recentlyPlayed.take(15).forEach { track ->
            if (track.artistId.isNotBlank()) {
                artistIdScores[track.artistId] = (artistIdScores[track.artistId] ?: 0) + 3
            }
            val a = track.artistName.lowercase()
            artistScores[a] = (artistScores[a] ?: 0) + 3
        }

        val scoredTracks = uniqueCandidates.map { candidate ->
            var score = 0
            val candidateGenre = candidate.genre.lowercase()
            val candidateArtist = candidate.artistName.lowercase()
            val candidateTitle = candidate.title.lowercase()

            if (cleanArtists.any { a -> candidateArtist.contains(a) || a.contains(candidateArtist) || candidateTitle.contains(a) }) {
                score += 35
            }

            if (cleanLangs.any { l -> candidateGenre.contains(l) || l.contains(candidateGenre) || candidateTitle.contains(l) || candidateArtist.contains(l) }) {
                score += 15
            }

            score += genreScores[candidateGenre] ?: 0
            score += artistScores[candidateArtist] ?: 0
            if (candidate.artistId.isNotBlank()) {
                score += artistIdScores[candidate.artistId] ?: 0
            }

            if (likedTracks.none { it.id == candidate.id } && (genreScores[candidateGenre] ?: 0) > 0) {
                score += 3
            }

            Pair(candidate, score)
        }

        val sorted = scoredTracks.sortedByDescending { it.second }.map { it.first }.distinctSongs()

        val selectedSingersSongs = mutableListOf<Track>()
        if (cleanArtists.isNotEmpty()) {
            val usedTrackKeys = mutableSetOf<String>()
            for (artistName in cleanArtists) {
                val artistSongs = uniqueCandidates.filter { track ->
                    val a = track.artistName.lowercase()
                    val t = track.title.lowercase()
                    a.contains(artistName) || artistName.contains(a) || t.contains(artistName)
                }.distinctSongs()

                var count = 0
                for (song in artistSongs) {
                    val key = normalizeSongTitle(song.title)
                    if (usedTrackKeys.add(key)) {
                        selectedSingersSongs.add(song)
                        count++
                        if (count >= 3) break
                    }
                }
            }
        }

        val remainingRecommendations = sorted.filter { track ->
            val key = normalizeSongTitle(track.title)
            selectedSingersSongs.none { normalizeSongTitle(it.title) == key }
        }

        val combinedResult = (selectedSingersSongs + remainingRecommendations).distinctSongs().take(limit)
        return combinedResult.ifEmpty { sorted.take(limit) }
    }

    fun generateMadeForYouMixes(
        likedTracks: List<Track>,
        recentlyPlayed: List<Track>
    ): Map<String, List<Track>> {
        val mixes = mutableMapOf<String, List<Track>>()

        val chillTracks = CuratedAudioCatalog.tracks.filter {
            it.genre in listOf("Lo-Fi", "Lofi", "Ambient", "Chill", "Acoustic", "Bollywood", "Melody")
        }.distinctSongs()
        if (chillTracks.isNotEmpty()) {
            mixes["Chill & Focus Mix"] = chillTracks
        }

        val energyTracks = CuratedAudioCatalog.tracks.filter {
            it.genre in listOf("Synthwave", "EDM", "Electronic", "Dance", "Punjabi", "Desi Hip-Hop", "Pop", "Bollywood", "Lofi")
        }.distinctSongs()
        if (energyTracks.isNotEmpty()) {
            mixes["High Energy & Cyber Wave"] = energyTracks
        }

        val cinematicTracks = CuratedAudioCatalog.tracks.filter {
            it.genre in listOf("Cinematic", "Classical", "Pop", "Bollywood")
        }.distinctSongs()
        if (cinematicTracks.isNotEmpty()) {
            mixes["Deep Horizon & Melodies"] = cinematicTracks
        }

        return mixes
    }
}
