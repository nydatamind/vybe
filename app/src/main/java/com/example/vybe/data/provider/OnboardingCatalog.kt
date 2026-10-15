package com.example.vybe.data.provider

import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.Track

data class MusicLanguage(
    val id: String,
    val name: String,
    val nativeName: String,
    val accentColor: Long,
    val artworkUrl: String = ""
)

data class OnboardingArtist(
    val id: String,
    val name: String,
    val languageId: String,
    val languageName: String,
    val artworkUrl: String,
    val isPopular: Boolean = true,
    val hitSongTitle: String = "",
    val popularityRank: Int = 80,
    val monthlyListeners: Long = 10000000L
)

object OnboardingCatalog {

    val languages: List<MusicLanguage> = listOf(
        // Row 1: Hindi | International
        MusicLanguage("hindi", "Hindi", "हिंदी", 0xFFE22630, "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg"),
        MusicLanguage("english", "International", "English", 0xFFE58E26, "https://c.saavncdn.com/artists/Justin_Bieber_005_20201127112218_500x500.jpg"),

        // Row 2: Rajasthani | Haryanvi (as requested: 2nd row)
        MusicLanguage("rajasthani", "Rajasthani", "राजस्थानी", 0xFFF57C00, "https://c.saavncdn.com/artists/Mame_Khan_20191129073056_500x500.jpg"),
        MusicLanguage("haryanvi", "Haryanvi", "हरियाणवी", 0xFF7CB342, "https://c.saavncdn.com/artists/Masoom_Sharma_003_20250619064935_500x500.jpg"),

        // Row 3: Punjabi | God (swapped with Tamil)
        MusicLanguage("punjabi", "Punjabi", "ਪੰਜਾਬੀ", 0xFF8E24AA, "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073054_500x500.jpg"),
        MusicLanguage("god", "God", "भक्ति", 0xFFE5A93C, "https://c.saavncdn.com/301/Shri-Krishna-Govind-Hare-Murari-Hindi-2023-20230128142805-500x500.jpg"),

        // Row 4: Telugu | Malayalam
        MusicLanguage("telugu", "Telugu", "తెలుగు", 0xFF00C853, "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg"),
        MusicLanguage("malayalam", "Malayalam", "മലയാളം", 0xFF5FA777, "https://c.saavncdn.com/artists/Vineeth_Sreenivasan_003_20240508103358_500x500.jpg"),

        // Row 5: Marathi | Gujarati
        MusicLanguage("marathi", "Marathi", "मराठी", 0xFFB86B43, "https://c.saavncdn.com/artists/Bela_Shende_003_20240129070549_500x500.jpg"),
        MusicLanguage("gujarati", "Gujarati", "ગુજરાતી", 0xFFE84378, "https://c.saavncdn.com/artists/Darshan_Raval_006_20250807060352_500x500.jpg"),

        // Row 6: Bengali | Kannada
        MusicLanguage("bengali", "Bengali", "বাংলা", 0xFF1E88E5, "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg"),
        MusicLanguage("kannada", "Kannada", "ಕನ್ನಡ", 0xFFD32F2F, "https://c.saavncdn.com/artists/Sanjith_Hegde_002_20230413072406_500x500.jpg"),

        // Row 7: Bhojpuri | Tamil (swapped with God)
        MusicLanguage("bhojpuri", "Bhojpuri", "भोजपुरी", 0xFFFF5722, "https://c.saavncdn.com/artists/Pawan_Singh_003_20241119074737_500x500.jpg"),
        MusicLanguage("tamil", "Tamil", "தமிழ்", 0xFFDF9E3C, "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg")
    )

    private fun generateArtists(langId: String, langName: String, artistData: List<Triple<String, String, String>>): List<OnboardingArtist> {
        return artistData.mapIndexed { index, (name, song, art) ->
            val rank = (100 - index).coerceAtLeast(10)
            val listeners = (25000000L - index * 400000L).coerceAtLeast(1000000L)
            OnboardingArtist(
                id = "art_${langId}_${name.lowercase().replace(" ", "_").replace("-", "_").replace(".", "")}",
                name = name,
                languageId = langId,
                languageName = langName,
                artworkUrl = art,
                isPopular = index < 15,
                hitSongTitle = song,
                popularityRank = rank,
                monthlyListeners = listeners
            )
        }
    }

    // --- 50 POPULAR HINDI ARTISTS ---
    private val hindiArtists = generateArtists("hindi", "Hindi", listOf(
        Triple("Arijit Singh", "Kesariya", "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg"),
        Triple("Pritam", "Channa Mereya", "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg"),
        Triple("Shreya Ghoshal", "Sunn Raha Hai", "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg"),
        Triple("Atif Aslam", "Jeena Jeena", "https://c.saavncdn.com/953/club-ambition-Unknown-2024-20240628202752-500x500.jpg"),
        Triple("Vishal-Shekhar", "Besharam Rang", "https://c.saavncdn.com/artists/Vishal-Shekhar_20191130071357_500x500.jpg"),
        Triple("Sachin-Jigar", "Apna Bana Le", "https://c.saavncdn.com/artists/Sachin_Jigar_003_20251222093820_500x500.jpg"),
        Triple("Sonu Nigam", "Kal Ho Naa Ho", "https://c.saavncdn.com/artists/Sonu_Nigam_003_20260813182013_500x500.jpg"),
        Triple("KK", "Zara Sa", "https://c.saavncdn.com/artists/KK_500x500.jpg"),
        Triple("Neha Kakkar", "Dilbar", "https://c.saavncdn.com/artists/Neha_Kakkar_007_20241212115832_500x500.jpg"),
        Triple("Jubin Nautiyal", "Raataan Lambiyan", "https://c.saavncdn.com/artists/Jubin_Nautiyal_003_20231130204020_500x500.jpg"),
        Triple("Udit Narayan", "Tip Tip Barsa", "https://c.saavncdn.com/artists/Udit_Narayan_004_20241029065120_500x500.jpg"),
        Triple("Kumar Sanu", "Aankh Marey", "https://c.saavncdn.com/artists/Kumar_Sanu_500x500.jpg"),
        Triple("Alka Yagnik", "Agar Tum Saath Ho", "https://c.saavncdn.com/artists/Alka_Yagnik_002_20220314192930_500x500.jpg"),
        Triple("Anuv Jain", "Husn", "https://c.saavncdn.com/artists/Anuv_Jain_001_20231206073013_500x500.jpg"),
        Triple("Darshan Raval", "Chogada", "https://c.saavncdn.com/artists/Darshan_Raval_006_20250807060352_500x500.jpg"),
        Triple("Mohit Chauhan", "Tum Se Hi", "https://c.saavncdn.com/artists/Mohit_Chauhan_500x500.jpg"),
        Triple("Jasleen Royal", "Heeriye", "https://c.saavncdn.com/artists/Jasleen_Royal_002_20230615091108_500x500.jpg"),
        Triple("Prateek Kuhad", "cold/mess", "https://c.saavncdn.com/artists/Prateek_Kuhad_006_20260515064251_500x500.jpg"),
        Triple("Stebin Ben", "Rula Ke Gaya Ishq", "https://c.saavncdn.com/artists/Stebin_Ben_005_20240124093234_500x500.jpg"),
        Triple("B Praak", "Mann Bharryaa", "https://c.saavncdn.com/artists/B_Praak_001_20191118112005_500x500.jpg"),
        Triple("Sunidhi Chauhan", "Sami Sami", "https://c.saavncdn.com/artists/Sunidhi_Chauhan_005_20250515061617_500x500.jpg"),
        Triple("Shankar Mahadevan", "Breathless", "https://c.saavncdn.com/artists/Shankar_Mahadevan_500x500.jpg"),
        Triple("Shaan", "Chand Sifarish", "https://c.saavncdn.com/artists/Shaan_004_20250422120221_500x500.jpg"),
        Triple("Armaan Malik", "Bol Do Na Zara", "https://c.saavncdn.com/artists/Armaan_Malik_006_20260813132832_500x500.jpg"),
        Triple("Amaal Mallik", "Kaun Tujhe", "https://c.saavncdn.com/artists/Amaal_Mallik_004_20260224065851_500x500.jpg"),
        Triple("Mika Singh", "Subha Hone Na De", "https://c.saavncdn.com/artists/Mika_Singh_003_20250321072715_500x500.jpg"),
        Triple("Rahat Fateh Ali Khan", "O Re Piya", "https://c.saavncdn.com/artists/Rahat_Fateh_Ali_Khan_500x500.jpg"),
        Triple("Kishore Kumar", "Pal Pal Dil Ke Paas", "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg"),
        Triple("Lata Mangeshkar", "Lag Ja Gale", "https://c.saavncdn.com/artists/Lata_Mangeshkar_004_20230623105323_500x500.jpg"),
        Triple("Mohammed Rafi", "Gulabi Aankhen", "https://c.saavncdn.com/artists/Mohammed_Rafi_500x500.jpg"),
        Triple("Mukesh", "Jeena Yahan Marna Yahan", "https://c.saavncdn.com/artists/Mukesh_500x500.jpg"),
        Triple("Asha Bhosle", "Dum Maro Dum", "https://c.saavncdn.com/artists/Asha_Bhosle_002_20200212082318_500x500.jpg"),
        Triple("Amit Trivedi", "Namo Namo", "https://c.saavncdn.com/artists/Amit_Trivedi_007_20241118063149_500x500.jpg"),
        Triple("Monali Thakur", "Moh Moh Ke Dhaage", "https://c.saavncdn.com/artists/Monali_Thakur_500x500.jpg"),
        Triple("Shashwat Sachdev", "Uri Anthem", "https://c.saavncdn.com/artists/Shashwat_Sachdev_000_20221011114409_500x500.jpg"),
        Triple("Himesh Reshammiya", "Aashiq Banaya Aapne", "https://c.saavncdn.com/artists/Himesh_Reshammiya_500x500.jpg"),
        Triple("Kailash Kher", "Teri Deewani", "https://c.saavncdn.com/artists/Kailash_Kher_003_20200114120508_500x500.jpg"),
        Triple("Vishal Mishra", "Pehle Bhi Main", "https://c.saavncdn.com/artists/Vishal_Mishra_005_20251120085316_500x500.jpg"),
        Triple("Sachet Tandon", "Bekhayali", "https://c.saavncdn.com/artists/Sachet_Tandon_20191129105330_500x500.jpg"),
        Triple("Parampara Tandon", "Mere Sohneya", "https://c.saavncdn.com/artists/Parampara_Thakur_20191130070743_500x500.jpg"),
        Triple("Tulsi Kumar", "Soch Na Sake", "https://c.saavncdn.com/artists/Tulsi_Kumar_006_20251123185308_500x500.jpg"),
        Triple("Palak Muchhal", "Kaun Tujhe", "https://c.saavncdn.com/artists/Palak_Muchhal_004_20250422120342_500x500.jpg"),
        Triple("Asees Kaur", "Raataan Lambiyan", "https://c.saavncdn.com/artists/Asees_Kaur_007_20250303070610_500x500.jpg"),
        Triple("Dhvani Bhanushali", "Vaaste", "https://c.saavncdn.com/artists/Dhvani_Bhanushali_001_20241015090250_500x500.jpg"),
        Triple("King", "Maan Meri Jaan", "https://c.saavncdn.com/artists/Ash_King_002_20241104095521_500x500.jpg"),
        Triple("Tanishk Bagchi", "Raataan Lambiyan", "https://c.saavncdn.com/artists/Tanishk_Bagchi_003_20260106115039_500x500.jpg"),
        Triple("Salim-Sulaiman", "Chak De India", "https://c.saavncdn.com/artists/Salim_Sulaiman_000_20211011142008_500x500.jpg"),
        Triple("Sukhwinder Singh", "Chaiyya Chaiyya", "https://c.saavncdn.com/artists/Sukhwinder_Singh_003_20240715112657_500x500.jpg"),
        Triple("Lucky Ali", "O Sanam", "https://c.saavncdn.com/artists/Lucky_Ali_004_20240227102228_500x500.jpg"),
        Triple("Pankaj Udhas", "Chitthi Aayi Hai", "https://c.saavncdn.com/artists/Pankaj_Udhas_002_20191215150014_500x500.jpg")
    ))

    // --- 50 POPULAR PUNJABI ARTISTS ---
    private val punjabiArtists = generateArtists("punjabi", "Punjabi", listOf(
        Triple("Diljit Dosanjh", "Lover", "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073054_500x500.jpg"),
        Triple("Karan Aujla", "Softly", "https://c.saavncdn.com/artists/Karan_Aujla_004_20260810121947_500x500.jpg"),
        Triple("Sidhu Moosewala", "295", "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_004_20250617183705_500x500.jpg"),
        Triple("AP Dhillon", "Excuses", "https://c.saavncdn.com/artists/AP_Dhillon_004_20251023102150_500x500.jpg"),
        Triple("Yo Yo Honey Singh", "Brown Rang", "https://c.saavncdn.com/artists/Yo_Yo_Honey_Singh_004_20260811095253_500x500.jpg"),
        Triple("Shubh", "Cheques", "https://c.saavncdn.com/artists/Shubh_000_20220921112507_500x500.jpg"),
        Triple("Badshah", "Jugnu", "https://c.saavncdn.com/artists/Badshah_006_20241118064015_500x500.jpg"),
        Triple("Guru Randhawa", "High Rated Gabru", "https://c.saavncdn.com/artists/Guru_Randhawa_004_20250701125845_500x500.jpg"),
        Triple("Arjan Dhillon", "Glorious", "https://c.saavncdn.com/artists/Arjan_Dhillon_001_20260525181943_500x500.jpg"),
        Triple("Ammy Virk", "Qismat", "https://c.saavncdn.com/artists/Ammy_Virk_005_20241101070506_500x500.jpg"),
        Triple("Harrdy Sandhu", "Bijlee Bijlee", "https://c.saavncdn.com/artists/Hardy_Sandhu_001_20190913112018_500x500.jpg"),
        Triple("Jassi Gill", "Nikle Currant", "https://c.saavncdn.com/artists/Jassie_Gill_007_20240124121355_500x500.jpg"),
        Triple("Amrinder Gill", "Dildarian", "https://c.saavncdn.com/artists/Amrinder_Gill_500x500.jpg"),
        Triple("Jordan Sandhu", "Teeje Week", "https://c.saavncdn.com/artists/Jordan_Sandhu_003_20230503073730_500x500.jpg"),
        Triple("Gurdas Maan", "Challa", "https://c.saavncdn.com/artists/Gurdas_Maan_005_20250114134730_500x500.jpg"),
        Triple("Garry Sandhu", "Illegal Weapon", "https://c.saavncdn.com/artists/Garry_Sandhu_500x500.jpg"),
        Triple("Mankirt Aulakh", "Gangland", "https://c.saavncdn.com/artists/Mankirt_Aulakh_007_20260522100331_500x500.jpg"),
        Triple("Parmish Verma", "Gaal Ni Kadni", "https://c.saavncdn.com/artists/Parmish_Verma_001_20240220103750_500x500.jpg"),
        Triple("Babbu Maan", "Mitran Di Chatri", "https://c.saavncdn.com/artists/Babbu_Maan_002_20241101070625_500x500.jpg"),
        Triple("Gippy Grewal", "Angreji Beat", "https://c.saavncdn.com/artists/Gippy_Grewal_004_20191118143844_500x500.jpg"),
        Triple("Nimrat Khaira", "Bhalwani Gedi", "https://c.saavncdn.com/925/Above-All-Punjabi-2021-20210320153531-500x500.jpg"),
        Triple("Sunanda Sharma", "Jaani Tera Naa", "https://c.saavncdn.com/artists/Sunanda_Sharma_001_20251110174316_500x500.jpg"),
        Triple("Jas Manak", "Prada", "https://c.saavncdn.com/artists/Jass_Manak_001_20240119064204_500x500.jpg"),
        Triple("Balkar Sidhu", "Majhe Diye Mombatiye", "https://c.saavncdn.com/artists/Balkar_Sidhu_500x500.jpg"),
        Triple("Kuldeep Manak", "Tere Tille Ton", "https://c.saavncdn.com/artists/Kuldeep_Manak_005_20230608123601_500x500.jpg"),
        Triple("Surjit Bindrakhia", "Tera Yaar Bolda", "https://c.saavncdn.com/artists/Surjit_Bindrakhia_500x500.jpg"),
        Triple("Chamkila", "Pehle Lalkare Naal", "https://c.saavncdn.com/artists/Amar_Singh_Chamkila_500x500.jpg"),
        Triple("Maninder Buttar", "Sakhiyaan", "https://c.saavncdn.com/artists/Maninder_Buttar_002_20190424105759_500x500.jpg"),
        Triple("Gurnam Bhullar", "Diamond", "https://c.saavncdn.com/artists/Gurnam_Bhullar_007_20250728101411_500x500.jpg"),
        Triple("Ranjit Bawa", "Jatt Di Akal", "https://c.saavncdn.com/artists/Ranjit_Bawa_003_20231025064301_500x500.jpg"),
        Triple("Kaka", "Libaas", "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg"),
        Triple("Tarsem Jassar", "Galwakdi", "https://c.saavncdn.com/artists/Tarsem_Jassar_003_20260822174821_500x500.jpg"),
        Triple("Kulwinder Billa", "Time Table", "https://c.saavncdn.com/artists/Kulwinder_Billa_20171106103541_500x500.jpg"),
        Triple("Rajvir Jawanda", "Kangani", "https://c.saavncdn.com/artists/Rajvir_Jawanda_003_20250728092420_500x500.jpg"),
        Triple("Prem Dhillon", "Old Skool", "https://c.saavncdn.com/artists/Prem_Dhillon_005_20260624073306_500x500.jpg"),
        Triple("Tegi Pannu", "Into You", "https://c.saavncdn.com/artists/Tegi_Pannu_20240418054856_500x500.jpg"),
        Triple("Wazir Patar", "BP Praan", "https://c.saavncdn.com/artists/Wazir_Patar_001_20250527090047_500x500.jpg"),
        Triple("Navaan Sandhu", "Radio", "https://c.saavncdn.com/artists/Navaan_Sandhu_000_20240422092254_500x500.jpg"),
        Triple("Gur Sidhu", "Dekhi Jau", "https://c.saavncdn.com/artists/Gur_Sidhu_001_20230314123602_500x500.jpg"),
        Triple("Cheetah", "Mexican", "https://c.saavncdn.com/900/Cheetah-English-2019-20190502212858-500x500.jpg"),
        Triple("Kidd", "Moosetape", "https://c.saavncdn.com/artists/The_Kidd_000_20260211105201_500x500.jpg"),
        Triple("Snappy", "High Rated", "https://c.saavncdn.com/artists/Snappy_001_20210518093726_500x500.jpg"),
        Triple("Ikky", "Making Memories", "https://c.saavncdn.com/artists/IKKY_000_20210818053348_500x500.jpg"),
        Triple("Manni Sandhu", "Special Edition", "https://c.saavncdn.com/139/Karde-Haan-English-2019-20190125224905-500x500.jpg"),
        Triple("Sukshinder Shinda", "Balle Balle", "https://c.saavncdn.com/artists/Sukhshinder_Shinda_003_20250623103236_500x500.jpg"),
        Triple("Jazzy B", "Naag", "https://c.saavncdn.com/artists/Jazzy_B_500x500.jpg"),
        Triple("Harbhajan Mann", "Gallan Goriyan", "https://c.saavncdn.com/artists/Harbhajan_Mann_500x500.jpg"),
        Triple("Bally Sagoo", "Gur Nalon Ishq", "https://c.saavncdn.com/artists/Bally_Sagoo_500x500.jpg"),
        Triple("Panjabi MC", "Mundian To Bach Ke", "https://c.saavncdn.com/artists/Panjabi_MC_500x500.jpg"),
        Triple("Dr Zeus", "Kangna", "https://c.saavncdn.com/artists/Dr_Zeus_500x500.jpg")
    ))

    // --- 50 POPULAR HARYANVI ARTISTS ---
    private val haryanviArtists = generateArtists("haryanvi", "Haryanvi", listOf(
        Triple("Masoom Sharma", "2 Numbari", "https://c.saavncdn.com/artists/Masoom_Sharma_003_20250619064935_500x500.jpg"),
        Triple("Dhanda Nyoliwala", "Russian Bandana", "https://c.saavncdn.com/artists/Dhanda_Nyoliwala_000_20240820133551_500x500.jpg"),
        Triple("Gulzaar Chhaniwala", "Yamaha", "https://c.saavncdn.com/artists/Gulzaar_Chhaniwala_003_20200822041826_500x500.jpg"),
        Triple("Renuka Panwar", "52 Gaj Ka Daman", "https://c.saavncdn.com/artists/Renuka_Panwar_004_20240715112921_500x500.jpg"),
        Triple("Khasa Aala Chahar", "Lootera", "https://c.saavncdn.com/artists/Khasa_Aala_Chahar_001_20230313111956_500x500.jpg"),
        Triple("Diler Kharkiya", "Moto", "https://c.saavncdn.com/artists/Diler_Kharkiya_002_20200716105505_500x500.jpg"),
        Triple("Sumit Goswami", "Feelings", "https://c.saavncdn.com/artists/Sumit_Goswami_20191130131014_500x500.jpg"),
        Triple("Ruchika Jangid", "Coco Cola", "https://c.saavncdn.com/artists/Ruchika_Jangid_002_20211018102226_500x500.jpg"),
        Triple("Amit Saini Rohtakiya", "Kharche", "https://c.saavncdn.com/artists/Amit_Saini_Rohtakiya_003_20260410063653_500x500.jpg"),
        Triple("Sapna Choudhary", "Solid Body", "https://c.saavncdn.com/artists/Sapna_Choudhary_000_20220916102820_500x500.jpg"),
        Triple("MD KD", "Desi Desi Na Bolya Kar", "https://c.saavncdn.com/artists/MD_KD_20191130131409_500x500.jpg"),
        Triple("Raju Punjabi", "Desi Desi", "https://c.saavncdn.com/artists/Raju_Punjabi_001_20191130065747_500x500.jpg"),
        Triple("Vicky Kajla", "Desi Da Recard", "https://c.saavncdn.com/467/Yaar-vs-Sarkar-English-2018-20181020075207-500x500.jpg"),
        Triple("Ajay Hooda", "Bahu Kale Ki", "https://c.saavncdn.com/artists/Ajay_Hooda_20191130140344_500x500.jpg"),
        Triple("Gajender Phogat", "Bahu Zamidar Ki", "https://c.saavncdn.com/artists/Gajender_Phogat_20191204070752_500x500.jpg"),
        Triple("Anu Kadyan", "Goli Chal Javegi", "https://c.saavncdn.com/artists/Annu_Kadyan_001_20191202065753_500x500.jpg"),
        Triple("Pardeep Boora", "Loot Liya", "https://c.saavncdn.com/artists/Pardeep_Boora_000_20240131071852_500x500.jpg"),
        Triple("Manisha Sharma", "Chunni Me", "https://c.saavncdn.com/artists/Manisha_Sharma_004_20251222094549_500x500.jpg"),
        Triple("Billa Sonipat Aala", "Laada Ka Lada", "https://c.saavncdn.com/artists/Billa_Sonipat_Ala_000_20230703174904_500x500.jpg"),
        Triple("Farista", "Kafan", "https://c.saavncdn.com/126/Lilo-Chaman-Haryanvi-2019-20190524124039-500x500.jpg"),
        Triple("Aman Jaji", "Gypsy", "https://c.saavncdn.com/artists/Aman_Jaji_20200825131309_500x500.jpg"),
        Triple("Pranjal Dahiya", "Gypsy Balam Thanedar", "https://c.saavncdn.com/artists/Pranjal_Dahiya_001_20250617180520_500x500.jpg"),
        Triple("GD Kaur", "Gaddi Kali", "https://c.saavncdn.com/885/Joban-Ka-Rang-Hindi-2019-20191207231538-500x500.jpg"),
        Triple("Shiva Choudhary", "Gaddi Me Baith", "https://c.saavncdn.com/artists/Shiva_Choudhary_000_20230826180424_500x500.jpg"),
        Triple("Raj Mawer", "Ghungroo", "https://c.saavncdn.com/artists/Raj_Mawer_003_20251222094654_500x500.jpg"),
        Triple("Ashu Twinkle", "Kala Daman", "https://c.saavncdn.com/artists/Ashu_Twinkle_003_20251113111630_500x500.jpg"),
        Triple("Sombir Sheoran", "Jalebi Juda", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg"),
        Triple("Akki Kalyan", "Pistol", "https://c.saavncdn.com/692/The-Haryanvi-Mashup-6-Single-Hindi-2019-20190220041447-500x500.jpg"),
        Triple("Devender Fagna", "Desi Bandook", "https://c.saavncdn.com/artists/Devender_Ahlawat_001_20230413103532_500x500.jpg"),
        Triple("Kavita Shobhu", "Chhori Bindass", "https://c.saavncdn.com/409/Chhori-Bindass-Haryanvi-2022-20221109101513-500x500.jpg"),
        Triple("Tarun Panchal", "Bahu Kale Ki", "https://c.saavncdn.com/artists/Tarun_Panchal_000_20251113101122_500x500.jpg"),
        Triple("Kanchan Sharma", "Madam Baith Bolero Me", "https://c.saavncdn.com/516/Baithungi-Piya-Bolero-Me-Haryanvi-2024-20250409044637-500x500.jpg"),
        Triple("Nonu Rana", "Halka Dupatta", "https://c.saavncdn.com/artists/Nonu_Rana_003_20260421062740_500x500.jpg"),
        Triple("Sandeep Surila", "Kehar", "https://c.saavncdn.com/artists/Dr__Sandeep_Surila_001_20251006054753_500x500.jpg"),
        Triple("Komila", "Sandook", "https://c.saavncdn.com/197/7-Senses-English-2017-20170921183043-500x500.jpg"),
        Triple("Vinu Gaur", "Chhora Haryane Ka", "https://c.saavncdn.com/019/Gundagardi-Kaale-Suit-Ki-Haryanvi-2020-20200212050402-500x500.jpg"),
        Triple("Narender Bhagana", "Balam Ka Pyar", "https://c.saavncdn.com/artists/Narender_Bhagana_000_20251007143437_500x500.jpg"),
        Triple("Sonika Singh", "Bhabi Ji", "https://c.saavncdn.com/151/Bairi-Haryanvi-2019-20190329120539-500x500.jpg"),
        Triple("Anjali Raghav", "Ghunghat Ki Oth Me", "https://c.saavncdn.com/artists/Anjali_Raghav_003_20220916175111_500x500.jpg"),
        Triple("Pooja Hooda", "Ghoonghat", "https://c.saavncdn.com/023/Jaan-Jatni-3-Haryanvi-2019-20190502170916-500x500.jpg"),
        Triple("Uttam", "Khet Badi", "https://c.saavncdn.com/artists/Uttam_Singh_000_20220916183212_500x500.jpg"),
        Triple("Mukesh Jaji", "Desi Yaar", "https://c.saavncdn.com/394/Jai-Bholenath-Haryanvi-2018-20181127-500x500.jpg"),
        Triple("Surender Romio", "Fukra Jaat", "https://c.saavncdn.com/artists/Surender_Romio_002_20230323063812_500x500.jpg"),
        Triple("Shanky Goswami", "Yaar Badnaam", "https://c.saavncdn.com/132/Bholenath-Single-Hindi-2019-20190123171802-500x500.jpg"),
        Triple("Chetak Studio", "Haryanvi Mashup", "https://c.saavncdn.com/106/Mashup-Haryanvi-Songs-2020-Vol-1-Hindi-2020-20200713114001-500x500.jpg"),
        Triple("Lokesh Gurjar", "Chhora Gurjar Ka", "https://c.saavncdn.com/artists/Lokesh_Gurjar_001_20240411090053_500x500.jpg"),
        Triple("Gurmeet Bhadana", "Gurjar Ka Rob", "https://c.saavncdn.com/artists/Gurmeet_Bhadana_000_20240411085932_500x500.jpg"),
        Triple("Totaram Sondhiya", "Haryanvi Ragni", "https://c.saavncdn.com/artists/Totaram_Sondhiya_000_20240411085621_500x500.jpg"),
        Triple("Rajbala Bahadurgad", "Ragni Hit", "https://c.saavncdn.com/907/Ragni-Dj-Remix-Haryanvi-2025-20251105104622-500x500.jpg"),
        Triple("Deepak Hooda", "Zimidar", "https://c.saavncdn.com/129/Kallo-Feat-Ajay-Hooda-Pardeep-Boora-Haryanvi-2023-20231025091456-500x500.jpg")
    ))

    // --- 50 POPULAR RAJASTHANI ARTISTS ---
    private val rajasthaniArtists = generateArtists("rajasthani", "Rajasthani", listOf(
        Triple("Mame Khan", "Chaudhary", "https://c.saavncdn.com/artists/Mame_Khan_20191129073056_500x500.jpg"),
        Triple("Swaroop Khan", "Ghoomar", "https://c.saavncdn.com/artists/Swaroop_Khan_500x500.jpg"),
        Triple("Ila Arun", "Morni Baga Ma", "https://c.saavncdn.com/artists/Ila_Arun_20200205103703_500x500.jpg"),
        Triple("Seema Mishra", "Banna Re", "https://c.saavncdn.com/artists/Seema_Mishra_500x500.jpg"),
        Triple("Chhotu Singh Rawna", "Dharmo Ka Dharm", "https://c.saavncdn.com/artists/Chotu_Singh_Rawna_20191129125954_500x500.jpg"),
        Triple("Prakash Mali", "Katha Jagdev Kankali", "https://c.saavncdn.com/artists/Prakash_Mali_20191130085439_500x500.jpg"),
        Triple("Shyam Paliwal", "Guru Vandana", "https://c.saavncdn.com/artists/Shyam_Paliwal_500x500.jpg"),
        Triple("Suman Chouhan", "Ghoomar Ramva", "https://c.saavncdn.com/artists/Suman_Chouhan_001_20240110074754_500x500.jpg"),
        Triple("Gajendra Ajmera", "Teja Ji Ro Byavlo", "https://c.saavncdn.com/artists/Gajendra_Ajmera_20191130084648_500x500.jpg"),
        Triple("Rani Rangili", "Ghoonghat Ro Pallo", "https://c.saavncdn.com/artists/Rani_Rangili_20191129073418_500x500.jpg"),
        Triple("Geeta Goswami", "Banna Jiyo Hajaaro Saal", "https://c.saavncdn.com/artists/Geeta_Goswami_20191130132353_500x500.jpg"),
        Triple("Mahendra Singh Rathore", "Banna Banni", "https://c.saavncdn.com/artists/Mahendra_Singh_Rathore_500x500.jpg"),
        Triple("Mangal Singh", "Rajasthani Dj Song", "https://c.saavncdn.com/artists/Mangal_Singh_20191130085923_500x500.jpg"),
        Triple("Satyanarayan Joshi", "Fagan Ke Geet", "https://c.saavncdn.com/artists/Prasoon_Joshi_50x50.png"),
        Triple("Gordhan Meena", "Meenawati Geet", "https://c.saavncdn.com/984/Meenawati-New-Song-2024-Rajasthani-2023-20231129223028-500x500.jpg"),
        Triple("Rameshwar Malani", "Desi Bhajan", "https://c.saavncdn.com/artists/Harshavardhan_Rameshwar_000_20230704071724_500x500.jpg"),
        Triple("Sarita Kharwal", "Runicha Ra Shyam", "https://c.saavncdn.com/artists/Sarita_Kharwal_500x500.jpg"),
        Triple("Asha Vaishnav", "Banna O Banni", "https://c.saavncdn.com/artists/Asha_Vaishnav_500x500.jpg"),
        Triple("Mamta Vajpayee", "Mharo Gorband Nakhralo", "https://c.saavncdn.com/304/Chakka-Jaam-Huwa-Maiya-Ke-2010-500x500.jpg"),
        Triple("Dinesh Dewasi", "Rabari Hit", "https://c.saavncdn.com/artists/Dinesh_Dewasi_20171123091336_500x500.jpg"),
        Triple("Kanaram Meena", "Meena Geet", "https://c.saavncdn.com/583/Dil-ki-Dhadkan-mein-Pankaj-Sattawan-dard-bhare-Meena-Geet-kanaram-thali-Meena-Geet-Hindi-2022-20220330233028-500x500.jpg"),
        Triple("Raju Rawal", "Gujjar Rasiya", "https://c.saavncdn.com/artists/Raju_Rawal_20191130131547_500x500.jpg"),
        Triple("Devendra Bhati", "Banna Thari Jodi", "https://c.saavncdn.com/859/Non-Stop-Garba-by-Falguni-Pathak-Gujarati-2022-20220922001218-500x500.jpg"),
        Triple("Rekha Rao", "Padharo Mhare Desh", "https://c.saavncdn.com/artists/Rekha_Rao_500x500.jpg"),
        Triple("Anwar Khan", "Kesariya Balam", "https://c.saavncdn.com/artists/Anwar_Hussain_500x500.jpg"),
        Triple("Banne Khan", "Gorband", "https://c.saavncdn.com/767/Ora-Dalal-Pt-01-Bengali-2018-20180310080929-500x500.jpg"),
        Triple("Kheta Khan", "Khartal Beats", "https://c.saavncdn.com/787/Kheta-Khan-Mashup-Rajasthani-2023-20231124140319-500x500.jpg"),
        Triple("Kavita Paudwal", "Rajasthan Folk", "https://c.saavncdn.com/artists/Kavita_Krishnamurthy_003_20241029065255_500x500.jpg"),
        Triple("Richhpal Dhaliwal", "Bishnoi Shabad", "https://c.saavncdn.com/artists/Richpal_Dhaliwal_500x500.jpg"),
        Triple("Shrawan Sendhav", "Katha Bhajan", "https://c.saavncdn.com/628/Life-Is-Beautiful-Marathi-2009-500x500.jpg"),
        Triple("Suresh Lohar", "Dj Fagan", "https://c.saavncdn.com/017/Le-Photo-Le-Rajasthani-2018-20190107-500x500.jpg"),
        Triple("Neeta Nayak", "Ramdevji Bhajan", "https://c.saavncdn.com/artists/Neeta_Nayak_003_20220411174944_500x500.jpg"),
        Triple("Kalu Ram Bamania", "Kabir Bhajan", "https://c.saavncdn.com/587/Kalu-Madari-Aaya-Hindi-2022-20220910034558-500x500.jpg"),
        Triple("Bhanwari Devi", "Kattey", "https://c.saavncdn.com/529/Coke-Studio-MTV-Season-3-Episode-2-Hindi-2013-20180409220125-500x500.jpg"),
        Triple("Gazi Khan", "Barna Manganiyar", "https://c.saavncdn.com/862/Manganiyar-Music-of-Sheo-VOL-1-Marwari-2020-20211101180714-500x500.jpg"),
        Triple("Lakha Khan", "Sindhi Sarangi", "https://c.saavncdn.com/185/Tunes-of-the-Dunes-Vol-1-English-1999-20171010131239-500x500.jpg"),
        Triple("Chugge Khan", "Rajasthan Express", "https://c.saavncdn.com/048/Rajasthan-Josh-Hindi-2012-20190514175500-500x500.jpg"),
        Triple("Barmer Boys", "Bole To Mitho Lage", "https://c.saavncdn.com/290/Lalitya-Munshaw-Ghazals-Romantic-Hindi-2022-20220524231438-500x500.jpg"),
        Triple("Kutle Khan", "Moomal", "https://c.saavncdn.com/artists/Kutle_Khan_000_20251016132018_500x500.jpg"),
        Triple("Jasu Khan", "Jaanu Daga Na Dena", "https://c.saavncdn.com/185/Golden-Era-Hindi-2026-20260813171634-500x500.jpg"),
        Triple("Sharvan Baghel", "Rasiya Dhamaka", "https://c.saavncdn.com/artists/Pratibha_Singh_Baghel_20191215150815_500x500.jpg"),
        Triple("Hemraj Goyal", "Gurjar Hit", "https://c.saavncdn.com/515/Jai-Guru-Marudhar-Kesri-Hindi-2008-500x500.jpg"),
        Triple("Suresh Choudhary", "Marwadi Geet", "https://c.saavncdn.com/018/Nach-Nach-Janu-Rajasthani-2018-20181025-500x500.jpg"),
        Triple("Mukesh Inana", "Teja Ji Bhajan", "https://c.saavncdn.com/artists/Mukesh_500x500.jpg"),
        Triple("Banshi Lal", "Langa Folk", "https://c.saavncdn.com/993/Padmaavat-Hindi-2018-20180122-500x500.jpg"),
        Triple("Devki Nandan", "Desi Katha", "https://c.saavncdn.com/493/Desi-Katta-Hindi-2019-20191108114619-500x500.jpg"),
        Triple("Koyal Bai", "Mhari Nakhrali", "https://c.saavncdn.com/856/Bhojpuri-Sona-Koilar-Ke-Kook-2014-500x500.jpg"),
        Triple("Sunder Bai", "Pallo Latke", "https://c.saavncdn.com/320/GRWM-Vibes-Hindi-2026-20260514171324-500x500.jpg"),
        Triple("Nathu Lal Solanki", "Nagada Beats", "https://c.saavncdn.com/368/Bhojpuri-Bahubali-Khesari-Lal-Yadav-Bhojpuri-2023-20230223103230-500x500.jpg"),
        Triple("Kohinoor Langa", "Nimuda", "https://c.saavncdn.com/447/Ro-Reha-Hai-Dil-Punjabi-2006-20240408222104-500x500.jpg")
    ))

    // --- 50 POPULAR INTERNATIONAL / ENGLISH ARTISTS ---
    private val internationalArtists = generateArtists("english", "International", listOf(
        Triple("The Weeknd", "Blinding Lights", "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg"),
        Triple("Taylor Swift", "Cruel Summer", "https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg"),
        Triple("Drake", "God's Plan", "https://c.saavncdn.com/artists/Drake_006_20260520062317_500x500.jpg"),
        Triple("Ed Sheeran", "Shape of You", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg"),
        Triple("Justin Bieber", "Peaches", "https://c.saavncdn.com/artists/Justin_Bieber_005_20201127112218_500x500.jpg"),
        Triple("Dua Lipa", "Levitating", "https://c.saavncdn.com/artists/Dua_Lipa_004_20231120090922_500x500.jpg"),
        Triple("Billie Eilish", "Bad Guy", "https://c.saavncdn.com/artists/Billie_Eilish_20190211151539_500x500.jpg"),
        Triple("Alan Walker", "Faded", "https://c.saavncdn.com/artists/Alan_Walker_003_20231116095443_500x500.jpg"),
        Triple("Post Malone", "Sunflower", "https://c.saavncdn.com/artists/Post_Malone_004_20190911070147_500x500.jpg"),
        Triple("Charlie Puth", "Attention", "https://c.saavncdn.com/artists/Charlie_Puth_004_20221012071125_500x500.jpg"),
        Triple("Shawn Mendes", "Señorita", "https://c.saavncdn.com/artists/Shawn_Mendes_500x500.jpg"),
        Triple("Ariana Grande", "7 rings", "https://c.saavncdn.com/artists/Ariana_Grande_007_20260616180049_500x500.jpg"),
        Triple("Bruno Mars", "Uptown Funk", "https://c.saavncdn.com/artists/Bruno_Mars_003_20260324060413_500x500.jpg"),
        Triple("Eminem", "Lose Yourself", "https://c.saavncdn.com/artists/Eminem_003_20240403152835_500x500.jpg"),
        Triple("Coldplay", "Yellow", "https://c.saavncdn.com/artists/Coldplay_002_20241003070447_500x500.jpg"),
        Triple("Imagine Dragons", "Believer", "https://c.saavncdn.com/artists/Imagine_Dragons_500x500.jpg"),
        Triple("Maroon 5", "Memories", "https://c.saavncdn.com/artists/Maroon_5_004_20260505094522_500x500.jpg"),
        Triple("Adele", "Someone Like You", "https://c.saavncdn.com/artists/Adele_500x500.jpg"),
        Triple("Sam Smith", "Unholy", "https://c.saavncdn.com/artists/Sam_Smith_006_20250102114440_500x500.jpg"),
        Triple("Rihanna", "Diamonds", "https://c.saavncdn.com/artists/Rihanna_002_20250102114144_500x500.jpg"),
        Triple("Beyonce", "Halo", "https://c.saavncdn.com/artists/Beyonce_500x500.jpg"),
        Triple("Katy Perry", "Roar", "https://c.saavncdn.com/artists/Katy_Perry_004_20200616105931_500x500.jpg"),
        Triple("Lady Gaga", "Shallow", "https://c.saavncdn.com/artists/Lady_Gaga_005_20250911070220_500x500.jpg"),
        Triple("Harry Styles", "As It Was", "https://c.saavncdn.com/artists/Harry_Styles_500x500.jpg"),
        Triple("Zayn", "Dusk Till Dawn", "https://c.saavncdn.com/artists/Zayn_Malik_004_20260619190432_500x500.jpg"),
        Triple("Sia", "Cheap Thrills", "https://c.saavncdn.com/artists/Sia_002_20200921152829_500x500.jpg"),
        Triple("Marshmello", "Happier", "https://c.saavncdn.com/artists/Marshmello_20190123065408_500x500.jpg"),
        Triple("DJ Snake", "Let Me Love You", "https://c.saavncdn.com/artists/Dj_Snake_005_20200822045249_500x500.jpg"),
        Triple("Calvin Harris", "Summer", "https://c.saavncdn.com/artists/Calvin_Harris_500x500.jpg"),
        Triple("David Guetta", "Titanium", "https://c.saavncdn.com/artists/David_Guetta_004_20260324060455_500x500.jpg"),
        Triple("Avicii", "Wake Me Up", "https://c.saavncdn.com/artists/Avicii_002_20231227075611_500x500.jpg"),
        Triple("Kygo", "Firestone", "https://c.saavncdn.com/artists/Kygo_004_20200226104030_500x500.jpg"),
        Triple("The Chainsmokers", "Closer", "https://c.saavncdn.com/artists/The_Chainsmokers_500x500.jpg"),
        Triple("OneRepublic", "Counting Stars", "https://c.saavncdn.com/artists/OneRepublic_003_20250102111533_500x500.jpg"),
        Triple("Twenty One Pilots", "Stressed Out", "https://c.saavncdn.com/artists/Twenty_One_Pilots_003_20250825082352_500x500.jpg"),
        Triple("Linkin Park", "In The End", "https://c.saavncdn.com/artists/Linkin_Park_500x500.jpg"),
        Triple("Queen", "Bohemian Rhapsody", "https://c.saavncdn.com/artists/Queen_500x500.jpg"),
        Triple("Michael Jackson", "Billie Jean", "https://c.saavncdn.com/artists/Michael_Jackson_500x500.jpg"),
        Triple("Selena Gomez", "Calm Down", "https://c.saavncdn.com/artists/Selena_Gomez_003_20231023065157_500x500.jpg"),
        Triple("Camila Cabello", "Havana", "https://c.saavncdn.com/artists/Camila_Cabello_001_20240530122029_500x500.jpg"),
        Triple("Halsey", "Without Me", "https://c.saavncdn.com/artists/Halsey_003_20240822130830_500x500.jpg"),
        Triple("Olivia Rodrigo", "drivers license", "https://c.saavncdn.com/artists/Olivia_Rodrigo_002_20260622191816_500x500.jpg"),
        Triple("Miley Cyrus", "Flowers", "https://c.saavncdn.com/artists/Miley_Cyrus_003_20260420101713_500x500.jpg"),
        Triple("Lana Del Rey", "Summertime Sadness", "https://c.saavncdn.com/artists/Lana_Del_Rey_003_20260420101058_500x500.jpg"),
        Triple("Travis Scott", "SICKO MODE", "https://c.saavncdn.com/artists/Travis_Scott_20190823072138_500x500.jpg"),
        Triple("Kendrick Lamar", "HUMBLE.", "https://c.saavncdn.com/artists/Kendrick_Lamar_500x500.jpg"),
        Triple("Kanye West", "Heartless", "https://c.saavncdn.com/artists/Kanye_West_002_20200822050858_500x500.jpg"),
        Triple("50 Cent", "In Da Club", "https://c.saavncdn.com/artists/50_Cent_500x500.jpg"),
        Triple("Snoop Dogg", "Drop It Like It's Hot", "https://c.saavncdn.com/artists/Snoop_Dogg_001_20240103100254_500x500.jpg"),
        Triple("Pitbull", "Give Me Everything", "https://c.saavncdn.com/artists/Pitbull_004_20200502102251_500x500.jpg")
    ))

    // --- 50 POPULAR GOD / DEVOTIONAL ARTISTS ---
    private val godArtists = generateArtists("god", "God", listOf(
        Triple("Lord Krishna", "Shri Krishna Govind", "https://c.saavncdn.com/301/Shri-Krishna-Govind-Hare-Murari-Hindi-2023-20230128142805-500x500.jpg"),
        Triple("Jaya Kishori", "Radhe Krishna", "https://c.saavncdn.com/artists/Jaya_Kishori_000_20250527084717_500x500.jpg"),
        Triple("Kanhaiya Mittal", "Hara Hoon Baba", "https://c.saavncdn.com/553/Sheesh-Ke-Dani-Ki-Amar-Kahani-Hindi-2023-20230313101606-500x500.jpg"),
        Triple("Anup Jalota", "Aisi Lagi Lagan", "https://c.saavncdn.com/artists/Anup_Jalota_500x500.jpg"),
        Triple("Anuradha Paudwal", "Aarti Kunj Bihari Ki", "https://c.saavncdn.com/artists/Anuradha_Paudwal_004_20240923125158_500x500.jpg"),
        Triple("Hansraj Raghuwanshi", "Mera Bhola Hai Bhandari", "https://c.saavncdn.com/artists/Hansraj_Raghuwanshi_001_20220916054832_500x500.jpg"),
        Triple("Maanya Arora", "Radha Rani Lage", "https://c.saavncdn.com/artists/Maanya_Arora_002_20201013101201_500x500.jpg"),
        Triple("Gulshan Kumar", "Shree Hanuman Chalisa", "https://c.saavncdn.com/118/Garje-Gajraj-Hamare-From-Junglee--Hindi-2019-20190325075034-500x500.jpg"),
        Triple("Lakhbir Singh Lakkha", "Are Dwarpalo", "https://c.saavncdn.com/435/Saj-Rahe-Bhole-Baba-Nirale-Dulhe-Mein-Hindi-2019-20190709224546-500x500.jpg"),
        Triple("Sadhana Sargam", "Shri Ram Chandra Kripalu", "https://c.saavncdn.com/artists/Sadhana_Sargam_003_20250619100252_500x500.jpg"),
        Triple("Suresh Wadkar", "Om Namah Shivaya", "https://c.saavncdn.com/artists/Suresh_Wadkar_005_20230825062730_500x500.jpg"),
        Triple("Jagjit Singh", "Hey Ram", "https://c.saavncdn.com/artists/Jagjit_Singh_500x500.jpg"),
        Triple("Hariharan", "Hanuman Chalisa", "https://c.saavncdn.com/artists/Hariharan_500x500.jpg"),
        Triple("Kavita Krishnamurthy", "Gayatri Mantra", "https://c.saavncdn.com/artists/Kavita_Krishnamurthy_500x500.jpg"),
        Triple("Shubha Mudgal", "Mahamrityunjaya Mantra", "https://c.saavncdn.com/artists/Shubha_Mudgal_500x500.jpg"),
        Triple("Ravindra Jain", "Mangal Bhavan Amangal Hari", "https://c.saavncdn.com/artists/Ravindra_Jain_500x500.jpg"),
        Triple("Richa Sharma", "Mata Ke Bhajan", "https://c.saavncdn.com/artists/Richa_Sharma_20180905071027_500x500.jpg"),
        Triple("Narendra Chanchal", "Chalo Bulawa Aaya Hai", "https://c.saavncdn.com/artists/Narendra_Chanchal_20181101065926_500x500.jpg"),
        Triple("Vinod Agarwal", "Govind Mero Hai", "https://c.saavncdn.com/934/Phoolon-Se-Saj-Rahe-Hai-2000-500x500.jpg"),
        Triple("Rajan Ji Maharaj", "Ram Katha Bhajan", "https://c.saavncdn.com/175/Sonaji-Maharaj-Ki-Katha-Rajasthani-2000-20230317184746-500x500.jpg"),
        Triple("Pujya Morari Bapu", "Ram Dhun", "https://c.saavncdn.com/598/Saiyaara-Hindi-2025-20250703061754-500x500.jpg"),
        Triple("Ramesh Bhai Oza", "Shrimad Bhagwat", "https://c.saavncdn.com/020/Om-Namah-Shivay-Hindi-2010-500x500.jpg"),
        Triple("Gaurav Krishna Goswami", "Brij Ke Bhajan", "https://c.saavncdn.com/artists/Gaurav_Dagaonkar_20191216113105_500x500.jpg"),
        Triple("Chitra Vichitra", "Radha Naam Dhun", "https://c.saavncdn.com/505/Dhoondta-Dolun-Hindi-2018-20180919-500x500.jpg"),
        Triple("Devi Chitralekha", "Bhakti Ras", "https://c.saavncdn.com/726/Radhe-Radhe-Bol-Hindi-2019-20190823135103-500x500.jpg"),
        Triple("Bhai Harjinder Singh", "Gurbani Kirtan", "https://c.saavncdn.com/358/Amrit-Baani-Punjabi-2019-20190924135023-500x500.jpg"),
        Triple("Bhai Manpreet Singh", "Waheguru Simran", "https://c.saavncdn.com/242/Nitnem-Punjabi-2015-20180625-500x500.jpg"),
        Triple("Bhai Nirmal Singh", "Gurbani", "https://c.saavncdn.com/053/Chaar-Sahibzaade-Rise-of-Banda-Singh-Bahadur-Punjabi-2016-500x500.jpg"),
        Triple("Devki Nandan Thakur", "Bhagwat Bhajan", "https://c.saavncdn.com/396/Pathar-Ki-Radha-Pyari-Pathar-Ke-Krishna-Murari-Hindi-2019-20190315204335-500x500.jpg"),
        Triple("Daler Mehndi", "Tunak Tunak Bhakti", "https://c.saavncdn.com/artists/Daler_Mehndi_500x500.jpg"),
        Triple("Nitin Mukesh", "Mata Bhajan", "https://c.saavncdn.com/artists/Nitin_Mukesh_002_20230605094117_500x500.jpg"),
        Triple("Sanjo Baghel", "Alha Bhakti", "https://c.saavncdn.com/648/Desh-Bhakti-Aalha-Hindi-2019-20190214125934-500x500.jpg"),
        Triple("Shahnaz Akhtar", "Mata Ke Jas", "https://c.saavncdn.com/217/Maiya-Paon-Paijaniya-2009-500x500.jpg"),
        Triple("Pawan Tiwari", "Shiv Bhajan", "https://c.saavncdn.com/artists/Pawan_Kalyan_500x500.jpg"),
        Triple("Sonu Sharma", "Khatu Shyam Bhajan", "https://c.saavncdn.com/artists/Konkona_Sen_Sharma_20210409185435_500x500.jpg"),
        Triple("Sanjay Mittal", "Khatu Wale Shyam", "https://c.saavncdn.com/007/Bhagat-Ke-Vash-Me-Hai-Bhagwan-Hindi-2018-20181218112602-500x500.jpg"),
        Triple("Uma Lahari", "Shyam Baba Dhun", "https://c.saavncdn.com/137/Shyam-Sanware-Hindi-2019-20190827153406-500x500.jpg"),
        Triple("Ginni Kaur", "Shyam Dhani", "https://c.saavncdn.com/800/Hits-Of-Kanhiya-Mittal-Vol-8-Hindi-2022-20220222003627-500x500.jpg"),
        Triple("Pramod Kumar", "Radha Ashtami", "https://c.saavncdn.com/artists/Pramod_Kumar_008_20240328071426_500x500.jpg"),
        Triple("Abhilash", "Itni Shakti Hame Dena Data", "https://c.saavncdn.com/785/Madhurashtakam-Adharam-Madhuram-Sanskrit-2019-20190828065111-500x500.jpg"),
        Triple("Pradeep", "Aao Bachho Tumhe Dikhaye", "https://c.saavncdn.com/artists/Pradeep_Kumar_002_20250807084559_500x500.jpg"),
        Triple("Vipin Sachdeva", "Bhakti Mala", "https://c.saavncdn.com/053/Kabir-Ke-Dohe-1-Hindi-2000-500x500.jpg"),
        Triple("Babla Mehta", "Mukesh Bhakti", "https://c.saavncdn.com/101/Top-Navratri-Bhajans-Vol-2-Hindi-2018-20180929104003-500x500.jpg"),
        Triple("Debashish Das Gupta", "Shiv Dhun", "https://c.saavncdn.com/452/Shiv-Upasana-Hindi-2009-20250217210122-500x500.jpg"),
        Triple("Anand Kumar C", "Ganesh Vandana", "https://c.saavncdn.com/489/Tulsidas-Krit-Ramayan-101-Choupaiyon-Mein-2001-500x500.jpg"),
        Triple("Sooraj Sharma", "Bholenath", "https://c.saavncdn.com/artists/Mani_Sharma_500x500.jpg"),
        Triple("Gaurav Sharma", "Salasar Balaji", "https://c.saavncdn.com/artists/Gaurav_Sharma_000_20260804045514_500x500.jpg"),
        Triple("Rakesh Kala", "Jai Hanuman", "https://c.saavncdn.com/artists/Rakesh_Kala_20191215152225_500x500.jpg"),
        Triple("Bijender Chauhan", "Shiv Shankar", "https://c.saavncdn.com/artists/Bijender_Chauhan_000_20240903083717_500x500.jpg"),
        Triple("Shital Pandey", "Khatu Shyam Bhakti", "https://c.saavncdn.com/065/Mai-Re-Mai-Hamara-Uhe-Laiki-Chahi-Bhojpuri-2018-20210525151442-500x500.jpg")
    ))

    // --- 50 POPULAR TAMIL ARTISTS ---
    private val tamilArtists = generateArtists("tamil", "Tamil", listOf(
        Triple("Anirudh Ravichander", "Hukum", "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg"),
        Triple("A.R. Rahman", "Jai Ho", "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg"),
        Triple("Santhosh Narayanan", "Enjoy Enjaami", "https://c.saavncdn.com/artists/Santhosh_Narayanan_002_20250527101718_500x500.jpg"),
        Triple("Yuvan Shankar Raja", "Rowdy Baby", "https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_002_20180802174245_500x500.jpg"),
        Triple("Harris Jayaraj", "Vaaranam Aayiram", "https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg"),
        Triple("Ilaiyaraaja", "Rakkamma", "https://c.saavncdn.com/artists/Ilaiyaraaja_001_20251020081419_500x500.jpg"),
        Triple("S.P. Balasubrahmanyam", "En Kadhale", "https://c.saavncdn.com/artists/SP_Balasubrahmanyam_500x500.jpg"),
        Triple("K.J. Yesudas", "Kanne Kalaimane", "https://c.saavncdn.com/artists/KJ_Yesudas_500x500.jpg"),
        Triple("Sid Sriram", "Maruvaarthai", "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg"),
        Triple("D. Imman", "Kannaana Kanney", "https://c.saavncdn.com/artists/D_Imman_003_20180821182708_500x500.jpg"),
        Triple("G.V. Prakash Kumar", "Pookkal Pookkum", "https://c.saavncdn.com/artists/G_V__Prakash_Kumar_003_20251113063655_500x500.jpg"),
        Triple("Dhanush", "Why This Kolaveri Di", "https://c.saavncdn.com/artists/Dhanush_500x500.jpg"),
        Triple("Vijay", "Arabic Kuthu", "https://c.saavncdn.com/artists/Vijay_003_20240103075444_500x500.jpg"),
        Triple("Jonita Gandhi", "Chellama", "https://c.saavncdn.com/artists/Jonita_Gandhi_003_20180507091741_500x500.jpg"),
        Triple("Chinmayi Sripaada", "Vaseegara", "https://c.saavncdn.com/artists/Chinmayi_Sripada_002_20241206081603_500x500.jpg"),
        Triple("Shweta Mohan", "Maya Nadhi", "https://c.saavncdn.com/artists/Shweta_Mohan_500x500.jpg"),
        Triple("Andrea Jeremiah", "Google Google", "https://c.saavncdn.com/artists/Andrea_Jeremiah_500x500.jpg"),
        Triple("Karthik", "Vizhi Moodi", "https://c.saavncdn.com/artists/Karthik_500x500.jpg"),
        Triple("Haricharan", "Pookkalae Sattru", "https://c.saavncdn.com/artists/Haricharan_500x500.jpg"),
        Triple("Vijay Prakash", "Hosanna", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg"),
        Triple("Naresh Iyer", "Munbe Vaa", "https://c.saavncdn.com/artists/Naresh_Iyer_500x500.jpg"),
        Triple("Benny Dayal", "Omana Penne", "https://c.saavncdn.com/artists/Benny_Dayal_500x500.jpg"),
        Triple("Deva", "Salomiya", "https://c.saavncdn.com/artists/Deva_20190801133857_500x500.jpg"),
        Triple("Hiphop Tamizha", "Vaadi Pulla Vaadi", "https://c.saavncdn.com/artists/Hiphop_Tamizha_002_20230315131424_500x500.jpg"),
        Triple("Sam C.S.", "Vikram Vedha", "https://c.saavncdn.com/artists/Sam_C_S__001_20260904065259_500x500.jpg"),
        Triple("Sean Roldan", "Mayakkama Kalakkama", "https://c.saavncdn.com/artists/Sean_Roldan_002_20240319071510_500x500.jpg"),
        Triple("Pradeep Kumar", "Aagayam Theepidicha", "https://c.saavncdn.com/artists/Pradeep_Kumar_002_20250807084559_500x500.jpg"),
        Triple("Dhee", "Enjoy Enjaami", "https://c.saavncdn.com/artists/Dhee_20180510121326_500x500.jpg"),
        Triple("Arivu", "Enjoy Enjaami", "https://c.saavncdn.com/artists/Arivu_000_20240109070048_500x500.jpg"),
        Triple("Shashaa Tirupati", "Vaan Varuvaan", "https://c.saavncdn.com/artists/Shashaa_Tirupati_500x500.jpg"),
        Triple("Saindhavi", "Vizhigalil Oru Vaanavil", "https://c.saavncdn.com/artists/Saindhavi_000_20210603093829_500x500.jpg"),
        Triple("Suchitra", "Ennamo Edho", "https://c.saavncdn.com/artists/Suchitra_500x500.jpg"),
        Triple("Tippu", "Kattipudi Kattipudida", "https://c.saavncdn.com/artists/Tippu_20190723074416_500x500.jpg"),
        Triple("Silambarasan TR", "Bullet Song", "https://c.saavncdn.com/artists/Simbu_500x500.jpg"),
        Triple("Kamal Haasan", "Pathala Pathala", "https://c.saavncdn.com/artists/Kamal_Haasan_002_20231106093704_500x500.jpg"),
        Triple("Rajinikanth", "Superstar Hits", "https://c.saavncdn.com/artists/Rajinikanth_500x500.jpg"),
        Triple("S. Janaki", "Singara Velane", "https://c.saavncdn.com/artists/S_Janaki_005_20191129094347_500x500.jpg"),
        Triple("P. Susheela", "Chithirame", "https://c.saavncdn.com/artists/P_Susheela_500x500.jpg"),
        Triple("T.M. Soundararajan", "Ullathil Nalla Ullam", "https://c.saavncdn.com/artists/TM_Soundararajan_20190904080450_500x500.jpg"),
        Triple("Sirkazhi Govindarajan", "Ullathil", "https://c.saavncdn.com/962/Bhakti-Gaanam-Bharathiyar-Tamil-2000-500x500.jpg"),
        Triple("Mano", "Shenbagame", "https://c.saavncdn.com/artists/Mano_500x500.jpg"),
        Triple("Unni Menon", "Pudhu Vellai Mazhai", "https://c.saavncdn.com/artists/Unni_Menon_500x500.jpg"),
        Triple("Sujatha Mohan", "Roja Janeman", "https://c.saavncdn.com/artists/Sujatha_Mohan_500x500.jpg"),
        Triple("Bamba Bakya", "Simtaangaran", "https://c.saavncdn.com/942/Simtaangaran-From-Sarkar-Tamil-Tamil-2018-20180927174932-500x500.jpg"),
        Triple("Anthony Daasan", "Kaasu Panam", "https://c.saavncdn.com/artists/Anthony_Daasan_002_20230104095846_500x500.jpg"),
        Triple("Gaana Bala", "Aadi Pona Aavani", "https://c.saavncdn.com/artists/Gana_Bala_20190801134651_500x500.jpg"),
        Triple("Vaikom Vijayalakshmi", "Kannaana Kanney", "https://c.saavncdn.com/artists/Vaikom_Vijayalakshmi_500x500.jpg"),
        Triple("Kharesma Ravichandran", "Kadhal Cricket", "https://c.saavncdn.com/181/Thani-Oruvan-Tamil-2015-20200806151645-500x500.jpg"),
        Triple("Shakthisree Gopalan", "Nenjukkulle", "https://c.saavncdn.com/artists/Shakthisree_Gopalan_500x500.jpg"),
        Triple("Nivas K. Prasanna", "Thegidi", "https://c.saavncdn.com/artists/Nivas_K_Prasanna_20180424065203_500x500.jpg")
    ))

    // --- 50 POPULAR TELUGU ARTISTS ---
    private val teluguArtists = generateArtists("telugu", "Telugu", listOf(
        Triple("Devi Sri Prasad", "Oo Antava", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg"),
        Triple("Thaman S", "Butta Bomma", "https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg"),
        Triple("Sid Sriram", "Srivalli", "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg"),
        Triple("Armaan Malik", "Butta Bomma", "https://c.saavncdn.com/artists/Armaan_Malik_006_20260813132832_500x500.jpg"),
        Triple("Ram Miriyala", "Chitti", "https://c.saavncdn.com/artists/Ram_Miriyala_001_20250911063558_500x500.jpg"),
        Triple("Anurag Kulkarni", "Ramuloo Ramulaa", "https://c.saavncdn.com/artists/Anurag_Kulkarni_004_20251029091123_500x500.jpg"),
        Triple("Mangli", "Saranga Dariya", "https://c.saavncdn.com/artists/Mangli_Satyavathi_001_20240701121028_500x500.jpg"),
        Triple("Geetha Madhuri", "Pakka Local", "https://c.saavncdn.com/artists/Geetha_Madhuri_500x500.jpg"),
        Triple("Sunitha", "Ee Vela Lo Naa", "https://c.saavncdn.com/artists/Sunitha_Upadrashta_003_20240424064243_500x500.jpg"),
        Triple("M.M. Keeravaani", "Naatu Naatu", "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg"),
        Triple("Rahul Sipligunj", "Naatu Naatu", "https://c.saavncdn.com/artists/Rahul_Sipligunj_500x500.jpg"),
        Triple("Mickey J. Meyer", "Inkem Inkem", "https://c.saavncdn.com/artists/Mickey_J_Meyer_500x500.jpg"),
        Triple("Gopi Sundar", "Geetha Govindam", "https://c.saavncdn.com/artists/Gopi_Sundar_001_20190827112215_500x500.jpg"),
        Triple("Karthik", "Areyrey", "https://c.saavncdn.com/artists/Karthik_500x500.jpg"),
        Triple("Haricharan", "Manohari", "https://c.saavncdn.com/artists/Haricharan_500x500.jpg"),
        Triple("Hemachandra", "Baahubali", "https://c.saavncdn.com/981/Baahubali-2-The-Conclusion-Telugu-2016-20251012021842-500x500.jpg"),
        Triple("Sreerama Chandra", "Crazy Feeling", "https://c.saavncdn.com/artists/Sreerama_Chandra_Mynampati_002_20260810123238_500x500.jpg"),
        Triple("Allu Arjun", "Pushpa Hits", "https://c.saavncdn.com/artists/Allu_Arjun_500x500.jpg"),
        Triple("Chiranjeevi", "Indra Hits", "https://c.saavncdn.com/artists/Chiranjeevi_500x500.jpg"),
        Triple("Prabhas", "Saaho", "https://c.saavncdn.com/artists/Prabhas_500x500.jpg"),
        Triple("Jr NTR", "Follow Follow", "https://c.saavncdn.com/304/Nannaku-Prematho-Telugu-2015-500x500.jpg"),
        Triple("Mahesh Babu", "Sarileru Neekevvaru", "https://c.saavncdn.com/405/Sarileru-Neekevvaru-Dialogue-Telugu-2019-20250818151314-500x500.jpg"),
        Triple("Chitra", "Nuvvostanante", "https://c.saavncdn.com/347/Nuvvostanante-Nenoddantana-Telugu-2014-20181121-500x500.jpg"),
        Triple("S.P. Sailaja", "Sagara Sangamam", "https://c.saavncdn.com/artists/S_P_Sailaja_20190904070649_500x500.jpg"),
        Triple("Mano", "Telugu Melodies", "https://c.saavncdn.com/artists/Mano_500x500.jpg"),
        Triple("S.P. Charan", "Aadabadi", "https://c.saavncdn.com/806/Global-Star-Ram-Charan-Superhit-Song-Collection-Telugu-2026-20260324181027-500x500.jpg"),
        Triple("Vandemataram Srinivas", "Osey Ramulamma", "https://c.saavncdn.com/artists/Vandemataram_Srinivas_000_20220913110808_500x500.jpg"),
        Triple("Koti", "Hello Brother", "https://c.saavncdn.com/artists/Koti_000_20220913111016_500x500.jpg"),
        Triple("Raj-Koti", "Khaidi No 786", "https://c.saavncdn.com/artists/Raj_Koti_000_20220913110528_500x500.jpg"),
        Triple("K.V. Mahadevan", "Sankarabharanam", "https://c.saavncdn.com/artists/K_V_Mahadevan_20190909081616_500x500.jpg"),
        Triple("Ghantasala", "Siva Sankari", "https://c.saavncdn.com/artists/Ghantasala_500x500.jpg"),
        Triple("Chakri", "Idiot", "https://c.saavncdn.com/artists/Chakri_500x500.jpg"),
        Triple("R.P. Patnaik", "Jayam", "https://c.saavncdn.com/artists/RP_Patnaik_500x500.jpg"),
        Triple("Anup Rubens", "Manam", "https://c.saavncdn.com/artists/Anup_Rubens_002_20190905131837_500x500.jpg"),
        Triple("Radhan", "Arjun Reddy", "https://c.saavncdn.com/498/Adithya-Varma-Tamil-2019-20191127090744-500x500.jpg"),
        Triple("Vivek Sagar", "Pelli Choopulu", "https://c.saavncdn.com/artists/Vivek_Sagar_001_20180706155214_500x500.jpg"),
        Triple("Chaitan Bharadwaj", "RX 100", "https://c.saavncdn.com/artists/Chaitan_Bharadwaj_001_20210120092127_500x500.jpg"),
        Triple("Roll Rida", "Arupu", "https://c.saavncdn.com/artists/Roll_Rida_500x500.jpg"),
        Triple("Lipsika", "Bullet Bandi", "https://c.saavncdn.com/artists/Lipsika_500x500.jpg"),
        Triple("Mohana Bhogaraju", "Bullet Bandi", "https://c.saavncdn.com/artists/Mohana_Bhogaraju_003_20200114121135_500x500.jpg"),
        Triple("Prudhvi Chandra", "Naatu Naatu", "https://c.saavncdn.com/artists/Prudhvi_Chandra_002_20220323202022_500x500.jpg"),
        Triple("Sameera Bharadwaj", "Telisiney Na Nuvvey", "https://c.saavncdn.com/artists/Sameera_Bharadwaj_500x500.jpg"),
        Triple("Harini", "Chitapata Chinukulu", "https://c.saavncdn.com/artists/Harini_500x500.jpg"),
        Triple("Kaala Bhairava", "Dosti", "https://c.saavncdn.com/artists/Kaala_Bhairava_20181003123414_500x500.jpg"),
        Triple("Hesham Abdul Wahab", "Kushi", "https://c.saavncdn.com/artists/Hesham_Abdul_Wahab_001_20220919094035_500x500.jpg"),
        Triple("Vijay Devarakonda", "I Hate You", "https://c.saavncdn.com/artists/Vijay_Deverakonda_500x500.jpg"),
        Triple("Bheems Ceciroleo", "Dhamaka", "https://c.saavncdn.com/artists/Bheems_Ceciroleo_000_20221222112703_500x500.jpg"),
        Triple("Mahathi Swara Sagar", "Bheeshma", "https://c.saavncdn.com/artists/Mahathi_Swara_Sagar_000_20220623173528_500x500.jpg"),
        Triple("Siri", "Telugu Pop", "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg"),
        Triple("Indravathi Chauhan", "Oo Antava", "https://c.saavncdn.com/artists/Indravathi_Chauhan_000_20220121132532_500x500.jpg")
    ))

    // --- 50 POPULAR BHOJPURI ARTISTS ---
    private val bhojpuriArtists = generateArtists("bhojpuri", "Bhojpuri", listOf(
        Triple("Pawan Singh", "Lollypop Lagelu", "https://c.saavncdn.com/artists/Pawan_Singh_003_20241119074737_500x500.jpg"),
        Triple("Khesari Lal Yadav", "Nathuniya", "https://c.saavncdn.com/artists/Khesari_Lal_Yadav_004_20241119074533_500x500.jpg"),
        Triple("Shilpi Raj", "Raja Ji", "https://c.saavncdn.com/artists/Shilpi_Raj_003_20250310065400_500x500.jpg"),
        Triple("Pramod Premi Yadav", "Majanua", "https://c.saavncdn.com/artists/Pramod_Premi_Yadav_005_20260606081051_500x500.jpg"),
        Triple("Arvind Akela Kallu", "Choliya", "https://c.saavncdn.com/artists/Arvind_Akela_004_20241230072800_500x500.jpg"),
        Triple("Ritesh Pandey", "Hello Koun", "https://c.saavncdn.com/artists/Ritesh_Pandey_003_20230922124324_500x500.jpg"),
        Triple("Akshara Singh", "Patli Kamariya", "https://c.saavncdn.com/artists/Akshara_Singh_004_20241119073750_500x500.jpg"),
        Triple("Samar Singh", "Dhobiniya", "https://c.saavncdn.com/artists/Samar_Singh_002_20241119104715_500x500.jpg"),
        Triple("Ankush Raja", "Tikuliya", "https://c.saavncdn.com/artists/Ankush_Raja_003_20250825092440_500x500.jpg"),
        Triple("Neelkamal Singh", "Pyaar Me Pagal", "https://c.saavncdn.com/artists/Neelkamal_Singh_002_20251006060110_500x500.jpg"),
        Triple("Manoj Tiwari", "Rinkiya Ke Papa", "https://c.saavncdn.com/artists/Manoj_Tiwari_500x500.jpg"),
        Triple("Dinesh Lal Yadav Nirahua", "Nirahua Satal Rahe", "https://c.saavncdn.com/artists/Dinesh_Lal_Yadav_500x500.jpg"),
        Triple("Ravi Kishan", "Bhojpuri King", "https://c.saavncdn.com/artists/Ravi_Kishan_500x500.jpg"),
        Triple("Monalisa", "Chumma De", "https://c.saavncdn.com/artists/Monalisa_500x500.jpg"),
        Triple("Sunil Chhaila Bihari", "Chhaila Bihari Hits", "https://c.saavncdn.com/artists/Sunil_Chhaila_Bihari_500x500.jpg"),
        Triple("Gunjan Singh", "Chumma Dehab", "https://c.saavncdn.com/artists/Gunjan_Singh_000_20241119074445_500x500.jpg"),
        Triple("Antra Singh Priyanka", "Chhalakata Hamro Jawaniya", "https://c.saavncdn.com/artists/Antra_Singh_Priyanka_004_20250227064120_500x500.jpg"),
        Triple("Kalpana Patowary", "Gandi Baat", "https://c.saavncdn.com/artists/Kalpana_Patowary_003_20260512134310_500x500.jpg"),
        Triple("Golu Gold", "Lover Se Shadi", "https://c.saavncdn.com/artists/Golu_Gold_001_20230809104419_500x500.jpg"),
        Triple("Guddu Rangeela", "Rangeela Hits", "https://c.saavncdn.com/artists/Guddu_Rangila_500x500.jpg"),
        Triple("Radheshyam Rasiya", "Bhojpuri Rasiya", "https://c.saavncdn.com/501/Hare-Ram-Hare-Ram-Hare-Krishna-Hare-Krishna-1997-500x500.jpg"),
        Triple("Bharat Sharma Vyas", "Nirgun Bhajan", "https://c.saavncdn.com/artists/Bharat_Sharma_Vyas_000_20240401184528_500x500.jpg"),
        Triple("Madan Rai", "Bhojpuri Nirgun", "https://c.saavncdn.com/472/Hare-Rama-Bhojpuri-2016-500x500.jpg"),
        Triple("Vishnu Ojha", "Lokgeet", "https://c.saavncdn.com/694/Om-Namo-Bhagavate-Vasudevaya-Most-Powerful-Lord-Vishnu-and-Krishna-Mantra-Hindi-2022-20220202140633-500x500.jpg"),
        Triple("Sharda Sinha", "Chhath Geet", "https://c.saavncdn.com/artists/Sharda_Sinha_20190327091253_500x500.jpg"),
        Triple("Anu Dubey", "Devigeet", "https://c.saavncdn.com/artists/Anu_Dubey_000_20210412122844_500x500.jpg"),
        Triple("Amrapali Dubey", "Nisha Me Chadh Gayi", "https://c.saavncdn.com/artists/Amrapali_Dubey_003_20250922124052_500x500.jpg"),
        Triple("Kajal Raghwani", "Aho Piya", "https://c.saavncdn.com/artists/Kajal_Raghwani_20190319095759_500x500.jpg"),
        Triple("Priyanka Singh", "Nathuniya", "https://c.saavncdn.com/artists/Priyanka_Singh_004_20241119104131_500x500.jpg"),
        Triple("Alka Jha", "Bhojpuri Hits", "https://c.saavncdn.com/artists/Alka_Jha_20200122130914_500x500.jpg"),
        Triple("Khushboo Tiwari KT", "Papa Ki Pari", "https://c.saavncdn.com/703/Holi-Me-Ham-Bol-Rahe-Hai-Bhojpuri-2020-20200123170840-500x500.jpg"),
        Triple("Neha Raj", "Aara Hile", "https://c.saavncdn.com/artists/Neha_Raj_001_20230613111027_500x500.jpg"),
        Triple("Shivani Singh", "Kamar Kamra", "https://c.saavncdn.com/artists/Shivani_Singh_002_20260716094427_500x500.jpg"),
        Triple("Anupama Yadav", "Le Le Aaiha Coca Cola", "https://c.saavncdn.com/artists/Anupama_Yadav_000_20230613110208_500x500.jpg"),
        Triple("Tuntun Yadav", "Lahanga Me Meter", "https://c.saavncdn.com/artists/Tuntun_Yadav_002_20241119105337_500x500.jpg"),
        Triple("Bullet Raja", "Hamaar Raja Ji", "https://c.saavncdn.com/artists/Bullet_Raja_001_20240422102545_500x500.jpg"),
        Triple("Kavita Yadav", "Birha Hits", "https://c.saavncdn.com/artists/Kavita_Yadav_001_20220916133035_500x500.jpg"),
        Triple("Vijay Lal Yadav", "Birha Samrat", "https://c.saavncdn.com/794/Saiya-Gaila-Kalkata-Me-Hindi-2019-20191118122020-500x500.jpg"),
        Triple("Haider Ali Jugnu", "Birha Geet", "https://c.saavncdn.com/718/Devi-Pachra-Geet-Haider-Ali-Jugnu-English-2025-20250710221529-500x500.jpg"),
        Triple("Ramesh Yadav", "Ahir Geet", "https://c.saavncdn.com/744/Dil-Ke-Darad-Bhojpuri-2019-20190226164753-500x500.jpg"),
        Triple("Devi", "Bhojpuri Devi", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg"),
        Triple("Chandan Chanchal", "Devra Dhodi Chatana Ba", "https://c.saavncdn.com/artists/Chandan_Chanchal_003_20250403140331_500x500.jpg"),
        Triple("Deepak Dildar", "Dildar Yaar", "https://c.saavncdn.com/142/Holi-Me-Tumko-Choose-Kiya-Bhojpuri-2019-20190213140210-500x500.jpg"),
        Triple("Alam Raj", "Bhojpuri Sad", "https://c.saavncdn.com/014/Kheera-Kare-Istemal-Bhojpuri-2020-20200214131623-500x500.jpg"),
        Triple("Ajit Anand", "Bhojpuri Melodies", "https://c.saavncdn.com/artists/Ajit_Anand_20191202065654_500x500.jpg"),
        Triple("Mohan Rathore", "Sur Sangram", "https://c.saavncdn.com/artists/Mohan_Rathore_002_20260213070428_500x500.jpg"),
        Triple("Alok Kumar", "Bhojpuri Cine Hits", "https://c.saavncdn.com/artists/Alok_Kumar_001_20191125072859_500x500.jpg"),
        Triple("Mamta Rawat", "Chhath Mahaparv", "https://c.saavncdn.com/515/Pandit-Ji-Ka-Beta-Hai-From-Nirahua-Chalal-London--Hindi-2019-20190615224100-500x500.jpg"),
        Triple("Nisha Dubey", "Umar Ho Gail 22", "https://c.saavncdn.com/350/Holi-Ke-Super-Hit-Gane-2017-Bhojpuri-2017-500x500.jpg"),
        Triple("Poonam Dubey", "Bhojpuri Glam", "https://c.saavncdn.com/980/Khosi-Kine-Ke-Bahane-Bhojpuri-2020-20200217122756-500x500.jpg")
    ))

    // --- 50 POPULAR MARATHI & GUJARATI ARTISTS ---
    private val marathiArtists = generateArtists("marathi", "Marathi", listOf(
        Triple("Ajay-Atul", "Zingaat", "https://c.saavncdn.com/artists/Ajay_Atul_003_20230228105414_500x500.jpg"),
        Triple("Bela Shende", "Apsara Aali", "https://c.saavncdn.com/artists/Bela_Shende_003_20240129070549_500x500.jpg"),
        Triple("Aadarsh Shinde", "Deva Tujha Nav G", "https://c.saavncdn.com/artists/Adarsh_Shinde_004_20240205115913_500x500.jpg"),
        Triple("Swapnil Bandodkar", "Gori Gori Paan", "https://c.saavncdn.com/artists/Swapnil_Bandodkar_500x500.jpg"),
        Triple("Avadhoot Gupte", "Jai Jai Maharashtra Maza", "https://c.saavncdn.com/artists/Avadhoot_Gupte_500x500.jpg"),
        Triple("Vaishali Samant", "Kombdi Palali", "https://c.saavncdn.com/artists/Vaishali_Samant_500x500.jpg"),
        Triple("Anand Shinde", "Navri Natali", "https://c.saavncdn.com/artists/Anand_Shinde_500x500.jpg"),
        Triple("Suresh Wadkar", "Sur Niragas Ho", "https://c.saavncdn.com/artists/Suresh_Wadkar_005_20230825062730_500x500.jpg"),
        Triple("Shreya Ghoshal", "Chimbh Bhijale", "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg"),
        Triple("Mangesh Borgaonkar", "Marathi Bhavgeet", "https://c.saavncdn.com/artists/Mangesh_Borgaonkar_500x500.jpg"),
        Triple("Ketaki Mategaonkar", "Phulpakharu", "https://c.saavncdn.com/artists/Ketaki_Mategaonkar_500x500.jpg"),
        Triple("Arya Ambekar", "Ti Sadhya Kay Karte", "https://c.saavncdn.com/artists/Aarya_Ambekar_500x500.jpg"),
        Triple("Sudhir Phadke", "Geet Ramayan", "https://c.saavncdn.com/artists/Sudhir_Phadke_500x500.jpg"),
        Triple("Kishori Amonkar", "Sahela Re", "https://c.saavncdn.com/artists/Kishori_Amonkar_500x500.jpg"),
        Triple("Uttara Kelkar", "Lavani Hits", "https://c.saavncdn.com/artists/Uttara_Kelkar_500x500.jpg"),
        Triple("Jasraj Joshi", "Muramba", "https://c.saavncdn.com/artists/Jasraj_Joshi_500x500.jpg"),
        Triple("Nilesh Moharir", "Radha Hi Bavari", "https://c.saavncdn.com/artists/Nilesh_Moharir_500x500.jpg"),
        Triple("Amitraj", "Tik Tik Vajate Dokyat", "https://c.saavncdn.com/artists/Amitraj_500x500.jpg"),
        Triple("Rohan-Rohan", "Sanngto Aika", "https://c.saavncdn.com/artists/Rohan_Rohan_500x500.jpg"),
        Triple("Vijay Narayan Gavande", "Fandry", "https://c.saavncdn.com/artists/Vijay_Narayan_Gavande_500x500.jpg"),
        Triple("Chinmayi Sripaada", "Sairat", "https://c.saavncdn.com/artists/Chinmayi_Sripada_002_20241206081603_500x500.jpg"),
        Triple("Kavita Seth", "Marathi Sufi", "https://c.saavncdn.com/artists/Kavita_Seth_500x500.jpg"),
        Triple("Hariharan", "Vitthal Vitthal", "https://c.saavncdn.com/artists/Hariharan_500x500.jpg"),
        Triple("Anup Jalota", "Pandharpur Bhajan", "https://c.saavncdn.com/artists/Anup_Jalota_500x500.jpg"),
        Triple("Pralhad Shinde", "Bhim Geete", "https://c.saavncdn.com/899/Mhorkya-Marathi-2020-20200114114517-500x500.jpg"),
        Triple("Mahesh Kale", "Katyar Kaljat Ghusali", "https://c.saavncdn.com/artists/Mahesh_Kale_000_20230809114831_500x500.jpg"),
        Triple("Rahul Deshpande", "Kanada Raja Pandharicha", "https://c.saavncdn.com/artists/Rahul_Deshpande_002_20230809115427_500x500.jpg"),
        Triple("Kavita Raaman", "Lagnageet", "https://c.saavncdn.com/artists/Kavita_Krishnamurthy_003_20241029065255_500x500.jpg"),
        Triple("Hrishikesh Ranade", "Maza Hoshil Kaa", "https://c.saavncdn.com/artists/Hrishikesh_Ranade_002_20240923131052_500x500.jpg"),
        Triple("Kartiki Gaikwad", "Abhang", "https://c.saavncdn.com/artists/Kartiki_Kalyanji_Gaikwad_000_20250114062112_500x500.jpg"),
        Triple("Prathamesh Laghate", "Natya Sangeet", "https://c.saavncdn.com/artists/Prathamesh_Laghate_000_20240521094821_500x500.jpg"),
        Triple("Mugdha Vaishampayan", "Bhakti Geet", "https://c.saavncdn.com/artists/Mugdha_Vaishampayan_002_20220511084205_500x500.jpg"),
        Triple("Hridaynath Mangeshkar", "Koli Geet", "https://c.saavncdn.com/artists/Hridaynath_Mangeshkar_20171122092252_500x500.jpg"),
        Triple("Vasantrao Deshpande", "Natya Sangeet Hits", "https://c.saavncdn.com/artists/Vasantrao_Deshpande_20171122112948_500x500.jpg"),
        Triple("Bhimsen Joshi", "Sant Vani", "https://c.saavncdn.com/artists/Pandit_Bhimsen_Joshi_002_20230809115305_500x500.jpg"),
        Triple("Jitendra Abhisheki", "Ghei Chhand", "https://c.saavncdn.com/artists/Pandit_Jitendra_Abhisheki_20171110071715_500x500.jpg"),
        Triple("Shaahir Sable", "Maharashtra Geet", "https://c.saavncdn.com/artists/Shahir_Sable_20191127085451_500x500.jpg"),
        Triple("Vitthal Umap", "Jambhul Aakhyan", "https://c.saavncdn.com/507/Lok-Ramayan-1-Marathi-2001-20181120-500x500.jpg"),
        Triple("Sulochana Chavan", "Tuzya Usala Lagal Kolha", "https://c.saavncdn.com/artists/Sulochana_Chavan_20191127091134_500x500.jpg"),
        Triple("Roshan Satarkar", "Koli Lavani", "https://c.saavncdn.com/685/Rukmini-Alias-Roshan-Satarkar-Marathi-1988-500x500.jpg"),
        Triple("Madhu Kambikar", "Lavani Queen", "https://c.saavncdn.com/474/Mala-Gheun-Chala-1989-500x500.jpg"),
        Triple("Surekha Punekar", "Karbhari Daman", "https://c.saavncdn.com/987/Chhatishi-Kavloon-Dhara-Nakhrel-Lawanya-Marathi-2009-500x500.jpg"),
        Triple("Amrut Subhash", "Dhakku Makku", "https://c.saavncdn.com/078/Welcome-Home-Marathi-2019-20190607172936-500x500.jpg"),
        Triple("Rohit Raut", "Duniyadari", "https://c.saavncdn.com/artists/Rohit_Shyam_Raut_003_20251111120019_500x500.jpg"),
        Triple("Savaniee Ravindrra", "Raanpetala", "https://c.saavncdn.com/artists/Savaniee_Ravindrra_000_20230516122451_500x500.jpg"),
        Triple("Juilee Joglekar", "Marathi Unplugged", "https://c.saavncdn.com/artists/Juilee_Joglekar_001_20240129071043_500x500.jpg"),
        Triple("Sayali Kamble", "Kolhapuri Tadka", "https://c.saavncdn.com/874/Lek-Vachava-Ho-English-2017-20171221192418-500x500.jpg"),
        Triple("Chinar-Mahesh", "Timepass", "https://c.saavncdn.com/777/Timepass-Marathi-2013-20190401111212-500x500.jpg"),
        Triple("Pravin Kunwar", "Dholki Beats", "https://c.saavncdn.com/artists/Pravin_Kuwar_002_20240424062746_500x500.jpg"),
        Triple("Sagar Lingam", "Sairat Jhala Ji", "https://c.saavncdn.com/842/God-Of-Gods-Telugu-2021-20210720062116-500x500.jpg")
    ))

    private val gujaratiArtists = generateArtists("gujarati", "Gujarati", listOf(
        Triple("Darshan Raval", "Chogada", "https://c.saavncdn.com/artists/Darshan_Raval_006_20250807060352_500x500.jpg"),
        Triple("Aditya Gadhvi", "Khalasi", "https://c.saavncdn.com/artists/Aditya_Gadhvi_004_20260902060949_500x500.jpg"),
        Triple("Kinjal Dave", "Char Bangadi", "https://c.saavncdn.com/artists/Kinjal_Dave_003_20241217095517_500x500.jpg"),
        Triple("Geeta Rabari", "Rona Ser Ma", "https://c.saavncdn.com/artists/Geeta_Rabari_20191129080213_500x500.jpg"),
        Triple("Jignesh Kaviraj", "Bewafa Sanam", "https://c.saavncdn.com/artists/Jignesh_Kaviraj_500x500.jpg"),
        Triple("Kirtidan Gadhvi", "Mogal Chedta Kalo Naag", "https://c.saavncdn.com/artists/Kirtidan_Gadhvi_500x500.jpg"),
        Triple("Osman Mir", "Mor Bani Thanghat Kare", "https://c.saavncdn.com/artists/Osman_Mir_500x500.jpg"),
        Triple("Hemant Chauhan", "Shree Ram Bhajan", "https://c.saavncdn.com/artists/Hemant_Chauhan_500x500.jpg"),
        Triple("Pankaj Udhas", "Gujarati Ghazal", "https://c.saavncdn.com/artists/Pankaj_Udhas_002_20191215150014_500x500.jpg"),
        Triple("Falguni Pathak", "Chudi Jo Khanki", "https://c.saavncdn.com/artists/Falguni_Pathak_500x500.jpg"),
        Triple("Sachin-Jigar", "Kehvu Ghanu Chhe", "https://c.saavncdn.com/artists/Sachin_Jigar_003_20251222093820_500x500.jpg"),
        Triple("Siddharth Bhavsar", "Gujarati Unplugged", "https://c.saavncdn.com/artists/Siddharth_Bhavsar_500x500.jpg"),
        Triple("Aishwarya Majmudar", "Vhalam Aavo Ne", "https://c.saavncdn.com/artists/Aishwarya_Majmudar_500x500.jpg"),
        Triple("Rakesh Barot", "Majburi", "https://c.saavncdn.com/artists/Rakesh_Barot_500x500.jpg"),
        Triple("Vikram Thakor", "Pardesi", "https://c.saavncdn.com/artists/Vikram_Thakor_500x500.jpg"),
        Triple("Gaman Santhal", "Gaman Santhal Hit", "https://c.saavncdn.com/artists/Gaman_Santhal_500x500.jpg"),
        Triple("Mayabhai Ahir", "Lok Dayro", "https://c.saavncdn.com/artists/Mayabhai_Ahir_500x500.jpg"),
        Triple("Sairam Dave", "Hasya Dayro", "https://c.saavncdn.com/artists/Sairam_Dave_500x500.jpg"),
        Triple("Pamela Jain", "Garba Dhol", "https://c.saavncdn.com/artists/Pamela_Jain_500x500.jpg"),
        Triple("Kavita Das", "Sant Vani", "https://c.saavncdn.com/artists/Kavita_Das_500x500.jpg"),
        Triple("Bhikhudan Gadhvi", "Rashtriya Shayar", "https://c.saavncdn.com/artists/Bhikhudan_Gadhvi_500x500.jpg"),
        Triple("Parthiv Gohil", "Yun Shabnami", "https://c.saavncdn.com/artists/Parthiv_Gohil_500x500.jpg"),
        Triple("Mehul Surti", "Hellaro", "https://c.saavncdn.com/artists/Mehul_Surti_500x500.jpg"),
        Triple("Alpa Patel", "Aavjo", "https://c.saavncdn.com/artists/Alpa_Patel_002_20240521101938_500x500.jpg"),
        Triple("Umesh Barot", "Dholida", "https://c.saavncdn.com/artists/Umesh_Barot_20190617101056_500x500.jpg"),
        Triple("Bhoomi Trivedi", "Ram Chahe Leela", "https://c.saavncdn.com/artists/Bhoomi_Trivedi_002_20250404083847_500x500.jpg"),
        Triple("Devang Patel", "Meri Marzi", "https://c.saavncdn.com/611/Karna-Padta-Hai-Hindi-2015-500x500.jpg"),
        Triple("Arvind Vegda", "Bhai Bhai", "https://c.saavncdn.com/artists/Arvind_Vegda_20180219090352_500x500.jpg"),
        Triple("Kajal Maheriya", "Tu Mane Bhuli Gayi", "https://c.saavncdn.com/artists/Kajal_Maheriya_20190709113326_500x500.jpg"),
        Triple("Vijay Suvada", "Mahadev", "https://c.saavncdn.com/artists/Vijay_Suvada_002_20220216121120_500x500.jpg"),
        Triple("Rohit Thakor", "Desi Dhol", "https://c.saavncdn.com/artists/Rohit_Thakor_20190709114720_500x500.jpg"),
        Triple("Mamta Soni", "Gujarati Queen", "https://c.saavncdn.com/artists/Mamta_Sharma_003_20220311172644_500x500.jpg"),
        Triple("Pravin Luni", "Dakla Beats", "https://c.saavncdn.com/artists/Pravin_Luni_000_20241028063214_500x500.jpg"),
        Triple("Ashok Thakor", "Gujarati Mashup", "https://c.saavncdn.com/artists/Ashok_Thakor_001_20190709112848_500x500.jpg"),
        Triple("Tejal Thakor", "Maa Tara Ashirwad", "https://c.saavncdn.com/artists/Tejal_Thakor_000_20250114065410_500x500.jpg"),
        Triple("Nayan Pancholi", "Garba King", "https://c.saavncdn.com/717/Aarti-Jay-Adhyashakti-Gujarati-2016-20190107071607-500x500.jpg"),
        Triple("Atul Purohit", "Tara Vina Shyam", "https://c.saavncdn.com/968/Tara-Vina-Shyam-Gujarati-2000-500x500.jpg"),
        Triple("Aanal Vasavada", "Dandiya Raas", "https://c.saavncdn.com/580/Kaipo-Che-Gujarati-2017-500x500.jpg"),
        Triple("Dharaben Patel", "Ranuja Na Raja", "https://c.saavncdn.com/artists/Ameesha_Patel_500x500.jpg"),
        Triple("Ghanshyam Zula", "Sonal Maa", "https://c.saavncdn.com/330/Aavo-Ghanshyam-Zulavunri-Hindorname-Gujarati-2026-20260317120003-500x500.jpg"),
        Triple("Farida Mir", "Dayro Sangat", "https://c.saavncdn.com/774/Bhajo-Shankar-Bhola-Gujarati-2008-500x500.jpg"),
        Triple("Laxman Barot", "Santwani Bhajan", "https://c.saavncdn.com/436/Japale-Harika-Naam-Gujarati-2015-500x500.jpg"),
        Triple("Narayan Swami", "Guru Mahima", "https://c.saavncdn.com/642/Kaanada-Kadalige-Kannada-2022-20220803092625-500x500.jpg"),
        Triple("Purushottam Upadhyay", "Kavya Sangeet", "https://c.saavncdn.com/987/Hari-Kirtan-Sanskrit-2001-500x500.jpg"),
        Triple("Solomon", "Gujarati Pop", "https://c.saavncdn.com/artists/Salman_Khan_002_20200519100153_500x500.jpg"),
        Triple("Sanjay Oza", "Sugam Sangeet", "https://c.saavncdn.com/080/Swaranjali-Sanjay-Oza-Gujarati-Songs-2005-500x500.jpg"),
        Triple("Shruti Pathak", "Mar Dala", "https://c.saavncdn.com/artists/Shruti_Pathak_001_20221105183154_500x500.jpg"),
        Triple("Gargi Vora", "Navratri Hit", "https://c.saavncdn.com/artists/Gargi_Vora_000_20250114065329_500x500.jpg"),
        Triple("Hardik Bhaladhare", "Garba Dhol Beats", "https://c.saavncdn.com/767/Buddha-Bar-Platinum-Collection-Unknown-2016-20251201044619-500x500.jpg"),
        Triple("Aarav Roy", "Gujarati Urban", "https://c.saavncdn.com/283/Tera-Intezar-Karun-Hindi-2026-20260717051533-500x500.jpg")
    ))

    // --- 50 POPULAR BENGALI, KANNADA & MALAYALAM ARTISTS ---
    private val bengaliArtists = generateArtists("bengali", "Bengali", listOf(
        Triple("Shreya Ghoshal", "Ami Shotti Bolchi", "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg"),
        Triple("Arijit Singh", "Mon Majhi Re", "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg"),
        Triple("Anupam Roy", "Amake Amar Moto", "https://c.saavncdn.com/artists/Anupam_Roy_007_20250623084828_500x500.jpg"),
        Triple("Jeet Gannguli", "Mon Mane Na", "https://c.saavncdn.com/artists/Jeet_Gannguli_500x500.jpg"),
        Triple("Shaan", "Ei To Jeebon", "https://c.saavncdn.com/artists/Shaan_004_20250422120221_500x500.jpg"),
        Triple("Rupam Islam", "Fossils Hasnuhana", "https://c.saavncdn.com/artists/Rupam_Islam_500x500.jpg"),
        Triple("Manna Dey", "Coffee Houser Sei Addata", "https://c.saavncdn.com/artists/Manna_Dey_500x500.jpg"),
        Triple("Kishore Kumar", "Chirodini Tumi Je Amar", "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg"),
        Triple("Lata Mangeshkar", "Akash Pradeep Jwole", "https://c.saavncdn.com/artists/Lata_Mangeshkar_004_20230623105323_500x500.jpg"),
        Triple("Asha Bhosle", "Jete Dao Amay", "https://c.saavncdn.com/artists/Asha_Bhosle_002_20200212082318_500x500.jpg"),
        Triple("Geeta Dutt", "Ei Sundar Swarnali", "https://c.saavncdn.com/artists/Geeta_Dutt_500x500.jpg"),
        Triple("Bappi Lahiri", "Chirodini Tumi", "https://c.saavncdn.com/artists/Bappi_Lahiri_500x500.jpg"),
        Triple("Kumar Sanu", "Priya Re Priya Re", "https://c.saavncdn.com/artists/Kumar_Sanu_500x500.jpg"),
        Triple("Alka Yagnik", "Tomar Chhondote", "https://c.saavncdn.com/artists/Alka_Yagnik_002_20220314192930_500x500.jpg"),
        Triple("Sonu Nigam", "Bolo Na Bolo Na", "https://c.saavncdn.com/artists/Sonu_Nigam_003_20260813182013_500x500.jpg"),
        Triple("Monali Thakur", "Sundori Komola", "https://c.saavncdn.com/artists/Monali_Thakur_500x500.jpg"),
        Triple("Iman Chakraborty", "Tumi Jake Bhalobasho", "https://c.saavncdn.com/artists/Iman_Chakraborty_500x500.jpg"),
        Triple("Rupankar Bagchi", "E Tumi Kemon Tumi", "https://c.saavncdn.com/artists/Rupankar_Bagchi_500x500.jpg"),
        Triple("Armaan Malik", "Tor Mon Parar", "https://c.saavncdn.com/artists/Armaan_Malik_006_20260813132832_500x500.jpg"),
        Triple("Kartik Das Baul", "Baul Sangeet", "https://c.saavncdn.com/artists/Kartik_Das_Baul_500x500.jpg"),
        Triple("Nachiketa Chakraborty", "Nilanjana", "https://c.saavncdn.com/artists/Nachiketa_001_20210107112106_500x500.jpg"),
        Triple("Kabir Suman", "Tomake Chai", "https://c.saavncdn.com/318/Janlar-Kanche-From-Winkle-Twinkle-Bengali-2026-20260912063903-500x500.jpg"),
        Triple("Hemanta Mukherjee", "Pather Klanti", "https://c.saavncdn.com/474/Chayanika-Salil-Chowdhury-Vol-3-Bengali-2000-20201014154022-500x500.jpg"),
        Triple("R.D. Burman", "Mone Pore Ruby Roy", "https://c.saavncdn.com/artists/Rahul_Dev_Burman_500x500.jpg"),
        Triple("Somlata Acharyya", "Mayabono Biharini", "https://c.saavncdn.com/artists/Somlata_001_20251205184530_500x500.jpg"),
        Triple("Lagnajita Chakraborty", "Preme Pora Baron", "https://c.saavncdn.com/artists/Lagnajita_Chakraborty_003_20251029092948_500x500.jpg"),
        Triple("Mimi Chakraborty", "Anjana", "https://c.saavncdn.com/artists/Mimi_Chakraborty_20191129081625_500x500.jpg"),
        Triple("Rathijit Bhattacharjee", "Bengali Pop", "https://c.saavncdn.com/artists/Rathijit_Bhattacharjee_000_20250919075540_500x500.jpg"),
        Triple("Srikanto Acharya", "Bristi Tomake", "https://c.saavncdn.com/artists/Srikanto_Acharya_20200517092839_500x500.jpg"),
        Triple("Lopamudra Mitra", "Benimadhab", "https://c.saavncdn.com/artists/Lopamudra_Mitra_001_20201224111431_500x500.jpg"),
        Triple("Subhamita Banerjee", "Jokhon Porbe Na", "https://c.saavncdn.com/artists/Subhamita_Banerjee_000_20231218064551_500x500.jpg"),
        Triple("Indrani Sen", "Rabindra Sangeet", "https://c.saavncdn.com/922/Sukher-Majhe-Tomai-Dekhhechi-Bengali-2014-500x500.jpg"),
        Triple("Swagatalakshmi Dasgupta", "Gitabitan", "https://c.saavncdn.com/816/Hey-Nutan-Rabindra-Sangeet-1998-500x500.jpg"),
        Triple("Siddhartha Ray Cactus", "Holud Pakhi", "https://c.saavncdn.com/041/Khoda-Jane-Na-Bengali-2023-20230101184511-500x500.jpg"),
        Triple("Anindya Chatterjee Chandrabindoo", "Bhindeshi Tara", "https://c.saavncdn.com/672/Gadha-Bengali-2012-20231107211824-500x500.jpg"),
        Triple("Upal Sengupta", "Bondhu Tomay", "https://c.saavncdn.com/artists/Upal_Sengupta_001_20240315073034_500x500.jpg"),
        Triple("Silajit Majumder", "X=Prem", "https://c.saavncdn.com/artists/Silajit_Majumder_000_20240711071144_500x500.jpg"),
        Triple("Krosswindz", "Prithibi Ta Naki", "https://c.saavncdn.com/291/Phire-Dekha-Krosswindz-Bengali-2011-500x500.jpg"),
        Triple("Bhoomi", "Barandaye Roddur", "https://c.saavncdn.com/artists/Bhoomi_Trivedi_002_20250404083847_500x500.jpg"),
        Triple("Sourendro-Soumyojit", "Dekha Hobe Ei Banglay", "https://c.saavncdn.com/114/Dekha-Hobe-Ei-Banglay-Bengali-2017-20170821142758-500x500.jpg"),
        Triple("Debojit Saha", "Bengali Folk", "https://c.saavncdn.com/artists/Debojit_Saha_000_20210921055026_500x500.jpg"),
        Triple("Aneek Dhar", "Bangla Dance", "https://c.saavncdn.com/artists/Aneek_Dhar_000_20230413071713_500x500.jpg"),
        Triple("Ujjaini Mukherjee", "Ei Bhalo Ei Kharap", "https://c.saavncdn.com/artists/Ujjaini_Mukherjee_004_20240916120519_500x500.jpg"),
        Triple("Prashmita Paul", "Honeymoon", "https://c.saavncdn.com/artists/Prashmita_Paul_20191129081907_500x500.jpg"),
        Triple("Madhuraa Bhattacharya", "Tomake Bhalobashi", "https://c.saavncdn.com/artists/Madhuraa_Bhattacharya_000_20220104070130_500x500.jpg"),
        Triple("Aakanksha Sharma", "Bengali Melodies", "https://c.saavncdn.com/artists/Aakanksha_Sharma_20190328111913_500x500.jpg"),
        Triple("Aditi Munshi", "Padabali Kirtan", "https://c.saavncdn.com/artists/Aditi_Munshi_000_20240423110627_500x500.jpg"),
        Triple("Poushali Banerjee", "Folk Fusion", "https://c.saavncdn.com/artists/Pousali_Banerjee_001_20260821184653_500x500.jpg"),
        Triple("Rik Basu", "Bangla Acoustic", "https://c.saavncdn.com/artists/Rik_Basu_000_20241008052704_500x500.jpg"),
        Triple("Dohar", "Lokosangeet", "https://c.saavncdn.com/541/Mon-Re-Krishi-Kaaj-Bengali-2019-20191025154241-500x500.jpg")
    ))

    private val kannadaArtists = generateArtists("kannada", "Kannada", listOf(
        Triple("Sanjith Hegde", "Kush Kush", "https://c.saavncdn.com/artists/Sanjith_Hegde_002_20230413072406_500x500.jpg"),
        Triple("Vijay Prakash", "Belageddu", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg"),
        Triple("Sonu Nigam", "Anisuthide", "https://c.saavncdn.com/artists/Sonu_Nigam_003_20260813182013_500x500.jpg"),
        Triple("Shreya Ghoshal", "Ello Jinugiro", "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg"),
        Triple("Armaan Malik", "Ninnila", "https://c.saavncdn.com/artists/Armaan_Malik_006_20260813132832_500x500.jpg"),
        Triple("Sid Sriram", "Helo Shiva", "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg"),
        Triple("Rajesh Krishnan", "Usire Usire", "https://c.saavncdn.com/artists/Rajesh_Krishnan_500x500.jpg"),
        Triple("Dr. Rajkumar", "Huttidare Kannada Nadalli", "https://c.saavncdn.com/artists/Dr_Rajkumar_500x500.jpg"),
        Triple("S.P. Balasubrahmanyam", "Jeeva Hoovagide", "https://c.saavncdn.com/artists/SP_Balasubrahmanyam_500x500.jpg"),
        Triple("Hamsalekha", "Premaloka", "https://c.saavncdn.com/artists/Hamsalekha_500x500.jpg"),
        Triple("V. Harikrishna", "Yajamana", "https://c.saavncdn.com/artists/V_Harikrishna_500x500.jpg"),
        Triple("Arjun Janya", "Chuttu Chuttu", "https://c.saavncdn.com/artists/Arjun_Janya_500x500.jpg"),
        Triple("Chandan Shetty", "3 Peg", "https://c.saavncdn.com/artists/Chandan_Shetty_500x500.jpg"),
        Triple("Anuradha Bhat", "Appa I Love You", "https://c.saavncdn.com/artists/Anuradha_Bhat_500x500.jpg"),
        Triple("Indu Nagaraj", "Pyarge Aagbittaite", "https://c.saavncdn.com/artists/Indu_Nagaraj_500x500.jpg"),
        Triple("Naveen Sajju", "Enne Namdu", "https://c.saavncdn.com/artists/Naveen_Sajju_500x500.jpg"),
        Triple("Aishwarya Rangarajan", "Love Mocktail", "https://c.saavncdn.com/artists/Aishwarya_Rangarajan_500x500.jpg"),
        Triple("Raghu Dixit", "Gudugudiya", "https://c.saavncdn.com/artists/Raghu_Dixit_500x500.jpg"),
        Triple("Vasuki Vaibhav", "Dadda Song", "https://c.saavncdn.com/artists/Vasuki_Vaibhav_500x500.jpg"),
        Triple("Shashank Sheshagiri", "Kirik Party", "https://c.saavncdn.com/artists/Shashank_Sheshagiri_500x500.jpg"),
        Triple("Punit Rajkumar", "Neene Neene", "https://c.saavncdn.com/artists/Puneeth_Rajkumar_500x500.jpg"),
        Triple("Ganesh", "Mungaru Male", "https://c.saavncdn.com/artists/Golden_Star_Ganesh_500x500.jpg"),
        Triple("Rakshit Shetty", "Sapta Sagaradaache Ello", "https://c.saavncdn.com/artists/Rakshit_Shetty_500x500.jpg"),
        Triple("Rishab Shetty", "Kantara", "https://c.saavncdn.com/artists/Rishab_Shetty_500x500.jpg"),
        Triple("Manjula Gururaj", "O Nanna Cheluve", "https://c.saavncdn.com/artists/Manjula_Gururaj_500x500.jpg"),
        Triple("B.R. Chaya", "Kogileye", "https://c.saavncdn.com/artists/BR_Chaya_500x500.jpg"),
        Triple("Mysore Ananthaswamy", "Kurigalu Saar Kurigalu", "https://c.saavncdn.com/artists/Mysore_Ananthaswamy_500x500.jpg"),
        Triple("K.S. Chithra", "Kannada Gana", "https://c.saavncdn.com/artists/KS_Chithra_500x500.jpg"),
        Triple("Vani Jairam", "Bhavasangama", "https://c.saavncdn.com/artists/Vani_Jairam_500x500.jpg"),
        Triple("Mano Murthy", "Mungaru Male", "https://c.saavncdn.com/artists/Mano_Murthy_500x500.jpg"),
        Triple("Sadhu Kokila", "Rakta Kanneeru", "https://c.saavncdn.com/artists/Sadhu_Kokila_500x500.jpg"),
        Triple("Kailash Kher", "Hale Patre", "https://c.saavncdn.com/artists/Kailash_Kher_003_20200114120508_500x500.jpg"),
        Triple("Shankar Mahadevan", "Geleya Geleya", "https://c.saavncdn.com/artists/Shankar_Mahadevan_500x500.jpg"),
        Triple("C. Ashwath", "Kanooru Heggadithi", "https://c.saavncdn.com/artists/C_Aswath_500x500.jpg"),
        Triple("Ravi Basrur", "KGF Theme", "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg"),
        Triple("Ajaneesh Loknath", "Kantara Singara Siriye", "https://c.saavncdn.com/artists/B__Ajaneesh_Loknath_002_20231106093827_500x500.jpg"),
        Triple("All OK", "Don't Worry", "https://c.saavncdn.com/artists/AllOk_20171103073214_500x500.jpg"),
        Triple("Pancham Jeeva", "Kannada Rap", "https://c.saavncdn.com/artists/Pancham_Jeeva_20200320074452_500x500.jpg"),
        Triple("Supriya Ram", "Kantara Varaha", "https://c.saavncdn.com/artists/Supriyaa_Ram_20200123114212_500x500.jpg"),
        Triple("Praveen D Rao", "Bhavageethe", "https://c.saavncdn.com/590/Nanage-Neenu-Ninage-Naanu-Kannada-2003-500x500.jpg"),
        Triple("Yash", "KGF Hits", "https://c.saavncdn.com/artists/Yash_500x500.jpg"),
        Triple("Darshan", "Kranti", "https://c.saavncdn.com/artists/Darshan_007_20231213125516_500x500.jpg"),
        Triple("Sudeep", "Eega", "https://c.saavncdn.com/artists/Sudeep_500x500.jpg"),
        Triple("Upendra", "A", "https://c.saavncdn.com/artists/Upendra_500x500.jpg"),
        Triple("Rathnamala Prakash", "Bhavagethegalu", "https://c.saavncdn.com/artists/Ratnamala_Prakash_000_20250703063928_500x500.jpg"),
        Triple("Shimoga Subbanna", "Kaadu Kudure", "https://c.saavncdn.com/artists/Shimogga_Subbanna_000_20220926102238_500x500.jpg"),
        Triple("Narasimha Nayak", "Dasara Padagalu", "https://c.saavncdn.com/artists/Puttur_Narasimha_Nayak_20200427100241_500x500.jpg"),
        Triple("P.B. Sreenivas", "Aadisi Nodu", "https://c.saavncdn.com/artists/P_B_Sreenivas_500x500.jpg"),
        Triple("L.N. Shastry", "A", "https://c.saavncdn.com/artists/LN_Shastry_20200406114907_500x500.jpg"),
        Triple("Gurukiran", "Jogi", "https://c.saavncdn.com/artists/Gurukiran_20171017084342_500x500.jpg")
    ))

    private val malayalamArtists = generateArtists("malayalam", "Malayalam", listOf(
        Triple("Vineeth Sreenivasan", "Darshana", "https://c.saavncdn.com/artists/Vineeth_Sreenivasan_003_20240508103358_500x500.jpg"),
        Triple("K.J. Yesudas", "Harivarasanam", "https://c.saavncdn.com/artists/KJ_Yesudas_500x500.jpg"),
        Triple("K.S. Chithra", "Malare", "https://c.saavncdn.com/artists/KS_Chithra_500x500.jpg"),
        Triple("Sushin Shyam", "Aavesham Theme", "https://c.saavncdn.com/artists/Sushin_Shyam_500x500.jpg"),
        Triple("Vijay Yesudas", "Malare Ninne", "https://c.saavncdn.com/artists/Vijay_Yesudas_500x500.jpg"),
        Triple("Shweta Mohan", "Aaradhike", "https://c.saavncdn.com/artists/Shweta_Mohan_500x500.jpg"),
        Triple("M.G. Sreekumar", "Kilukkam", "https://c.saavncdn.com/artists/MG_Sreekumar_500x500.jpg"),
        Triple("Sujatha Mohan", "Pranayavarnangal", "https://c.saavncdn.com/artists/Sujatha_Mohan_500x500.jpg"),
        Triple("Unni Menon", "Kanneer Poovinte", "https://c.saavncdn.com/artists/Unni_Menon_500x500.jpg"),
        Triple("G. Venugopal", "Unarumee Gaanam", "https://c.saavncdn.com/artists/G_Venugopal_500x500.jpg"),
        Triple("P. Jayachandran", "Rasikan", "https://c.saavncdn.com/artists/P_Jayachandran_500x500.jpg"),
        Triple("Haricharan", "Ennu Ninte Moideen", "https://c.saavncdn.com/artists/Haricharan_500x500.jpg"),
        Triple("Karthik", "Anuragathin Velayil", "https://c.saavncdn.com/artists/Karthik_500x500.jpg"),
        Triple("Sithara Krishnakumar", "Vaanam Thilathilakkanu", "https://c.saavncdn.com/artists/Sithara_Krishnakumar_500x500.jpg"),
        Triple("Shaan Rahman", "Jimikki Kammal", "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg"),
        Triple("Gopi Sundar", "Charlie", "https://c.saavncdn.com/artists/Gopi_Sundar_001_20190827112215_500x500.jpg"),
        Triple("Bijibal", "Maheshinte Prathikaaram", "https://c.saavncdn.com/artists/Bijibal_500x500.jpg"),
        Triple("Rahul Raj", "Chotta Mumbai", "https://c.saavncdn.com/artists/Rahul_Raj_500x500.jpg"),
        Triple("Prashant Pillai", "Amen", "https://c.saavncdn.com/artists/Prashant_Pillai_500x500.jpg"),
        Triple("Sooraj Santhosh", "Aanandam", "https://c.saavncdn.com/artists/Sooraj_Santhosh_500x500.jpg"),
        Triple("Neeraj Madhav", "Panipaali", "https://c.saavncdn.com/artists/Neeraj_Madhav_500x500.jpg"),
        Triple("Vaikom Vijayalakshmi", "Katte Katte", "https://c.saavncdn.com/artists/Vaikom_Vijayalakshmi_500x500.jpg"),
        Triple("Rimi Tomy", "Chingamasam", "https://c.saavncdn.com/artists/Rimi_Tomy_500x500.jpg"),
        Triple("Afsal", "En Karalil", "https://c.saavncdn.com/artists/Afsal_500x500.jpg"),
        Triple("Vidhu Prathap", "Sukhamani Nilavu", "https://c.saavncdn.com/artists/Vidhu_Prathap_500x500.jpg"),
        Triple("Madhu Balakrishnan", "Amme Mookambike", "https://c.saavncdn.com/artists/Madhu_Balakrishnan_500x500.jpg"),
        Triple("Biju Narayanan", "Marannittumenthino", "https://c.saavncdn.com/artists/Biju_Narayanan_500x500.jpg"),
        Triple("Sachin Warrier", "Muthuchippi", "https://c.saavncdn.com/artists/Sachin_Warrier_500x500.jpg"),
        Triple("Divya S. Menon", "Thudakkam Maangalyam", "https://c.saavncdn.com/artists/Divya_S_Menon_500x500.jpg"),
        Triple("Hesham Abdul Wahab", "Hridayam", "https://c.saavncdn.com/artists/Hesham_Abdul_Wahab_001_20220919094035_500x500.jpg"),
        Triple("Jassie Gift", "Lajjavathiye", "https://c.saavncdn.com/artists/Jassie_Gift_000_20210303130936_500x500.jpg"),
        Triple("Prarthana Indrajith", "Laletta", "https://c.saavncdn.com/050/Amar-Akbar-Anthony-Malayalam-2015-500x500.jpg"),
        Triple("Deepak Dev", "Urumi", "https://c.saavncdn.com/artists/Deepak_Dev_20190506095250_500x500.jpg"),
        Triple("Rex Vijayan", "Parava", "https://c.saavncdn.com/artists/Rex_Vijayan_001_20200321101744_500x500.jpg"),
        Triple("Job Kurian", "Padayottam", "https://c.saavncdn.com/artists/Job_Kurian_20191016115219_500x500.jpg"),
        Triple("Fejo", "Aparaatha", "https://c.saavncdn.com/artists/Fejo_002_20240319065322_500x500.jpg"),
        Triple("ThirumaLi", "Malayali Da", "https://c.saavncdn.com/artists/ThirumaLi_000_20240530053711_500x500.jpg"),
        Triple("Dabzee", "Illuminati Aavesham", "https://c.saavncdn.com/artists/Dabzee_000_20230516072112_500x500.jpg"),
        Triple("Saju Navodaya", "Pashanam Shaji", "https://c.saavncdn.com/685/Osan-Beeran-Ladan-Malayalam-2001-20200824120450-500x500.jpg"),
        Triple("Najeem Arshad", "Thottu Thottu", "https://c.saavncdn.com/artists/Najim_Arshad_500x500.jpg"),
        Triple("Anne Amie", "Aararo", "https://c.saavncdn.com/artists/Anne_Amie_20190327091507_500x500.jpg"),
        Triple("K.S. Harisankar", "Pavizha Mazha", "https://c.saavncdn.com/artists/Harisankar_K_S__002_20230518095003_500x500.jpg"),
        Triple("Gowry Lekshmi", "Aaro Nenjil", "https://c.saavncdn.com/artists/Gowry_Lekshmi_003_20230810074658_500x500.jpg"),
        Triple("Arya Dayal", "Kingini Pookkal", "https://c.saavncdn.com/artists/Arya_Dhayal_001_20250806080749_500x500.jpg"),
        Triple("Niranj Suresh", "Thallumaala", "https://c.saavncdn.com/artists/Niranj_Suresh_20191130082536_500x500.jpg"),
        Triple("Zia Ul Haq", "Aavesham", "https://c.saavncdn.com/artists/Ziya_Ul-Haq_20190906102516_500x500.jpg"),
        Triple("Jakes Bejoy", "Ranam", "https://c.saavncdn.com/artists/Jakes_Bejoy_002_20260813132706_500x500.jpg"),
        Triple("Ouseppachan", "Pranayavarnangal", "https://c.saavncdn.com/artists/Ouseppachan_000_20220925082623_500x500.jpg"),
        Triple("Johnson Master", "Devaanganangal", "https://c.saavncdn.com/528/Kissan-Malayalam-2005-500x500.jpg"),
        Triple("Raveendran Master", "Harimuraleeravam", "https://c.saavncdn.com/076/Amaram-Malayalam-1991-20211116020015-500x500.jpg")
    ))

    val artists: List<OnboardingArtist> = (
        hindiArtists +
        punjabiArtists +
        haryanviArtists +
        rajasthaniArtists +
        internationalArtists +
        godArtists +
        tamilArtists +
        teluguArtists +
        bhojpuriArtists +
        marathiArtists +
        gujaratiArtists +
        bengaliArtists +
        kannadaArtists +
        malayalamArtists
    ).distinctBy { it.name.lowercase().trim() }

    fun getArtistsForLanguages(languageIds: Set<String>): List<OnboardingArtist> {
        val cleanIds = languageIds.map { it.lowercase().trim() }.toSet()
        if (cleanIds.isEmpty()) return hindiArtists.take(50)

        val resultList = mutableListOf<OnboardingArtist>()
        val seenNames = mutableSetOf<String>()

        // Pick 50 top artists per selected language
        cleanIds.forEach { langId ->
            val langArtists = when (langId) {
                "hindi" -> hindiArtists
                "punjabi" -> punjabiArtists
                "haryanvi" -> haryanviArtists
                "rajasthani" -> rajasthaniArtists
                "english", "international" -> internationalArtists
                "god" -> godArtists
                "tamil" -> tamilArtists
                "telugu" -> teluguArtists
                "bhojpuri" -> bhojpuriArtists
                "marathi" -> marathiArtists
                "gujarati" -> gujaratiArtists
                "bengali" -> bengaliArtists
                "kannada" -> kannadaArtists
                "malayalam" -> malayalamArtists
                else -> artists.filter { it.languageId.equals(langId, ignoreCase = true) }
            }

            val sortedLangArtists = langArtists.sortedWith(
                compareByDescending<OnboardingArtist> { it.isPopular }
                    .thenByDescending { it.popularityRank }
                    .thenByDescending { it.monthlyListeners }
            )

            var count = 0
            for (artist in sortedLangArtists) {
                val key = artist.name.lowercase().trim()
                if (!seenNames.contains(key)) {
                    seenNames.add(key)
                    resultList.add(artist)
                    count++
                    if (count >= 50) break
                }
            }
        }

        // Return combined list sorted in descending order of popularity
        return resultList.sortedWith(
            compareByDescending<OnboardingArtist> { it.isPopular }
                .thenByDescending { it.popularityRank }
                .thenByDescending { it.monthlyListeners }
        )
    }

    val defaultHitTracks: List<Track> = listOf(
        Track(
            id = "hit_kesariya",
            title = "Kesariya",
            artistName = "Arijit Singh, Pritam",
            artistId = "art_arijit",
            albumName = "Brahmastra",
            albumId = "alb_brahmastra",
            durationMs = 268000,
            streamUrl = "",
            artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1080&auto=format&fit=crop&q=85",
            genre = "Hindi",
            releaseDate = "2022"
        ),
        Track(
            id = "hit_295",
            title = "295",
            artistName = "Sidhu Moosewala",
            artistId = "art_sidhu",
            albumName = "Moosetape",
            albumId = "alb_moosetape",
            durationMs = 270000,
            streamUrl = "",
            artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1080&auto=format&fit=crop&q=85",
            genre = "Punjabi",
            releaseDate = "2021"
        ),
        Track(
            id = "hit_masoom_2numbari",
            title = "2 Numbari",
            artistName = "Masoom Sharma",
            artistId = "art_masoom",
            albumName = "2 Numbari",
            albumId = "alb_2numbari",
            durationMs = 210000,
            streamUrl = "",
            artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1080&auto=format&fit=crop&q=85",
            genre = "Haryanvi",
            releaseDate = "2023"
        ),
        Track(
            id = "hit_chaudhary",
            title = "Chaudhary",
            artistName = "Mame Khan",
            artistId = "art_mame",
            albumName = "Coke Studio",
            albumId = "alb_chaudhary",
            durationMs = 278000,
            streamUrl = "",
            artworkUrl = "https://c.saavncdn.com/artists/Mame_Khan_20191129073056_500x500.jpg",
            genre = "Rajasthani",
            releaseDate = "2019"
        ),
        Track(
            id = "hit_lover",
            title = "Lover",
            artistName = "Diljit Dosanjh",
            artistId = "art_diljit",
            albumName = "MoonChild Era",
            albumId = "alb_moonchild",
            durationMs = 191000,
            streamUrl = "",
            artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=1080&auto=format&fit=crop&q=85",
            genre = "Punjabi",
            releaseDate = "2021"
        )
    )

    fun getHitTracksForPreferences(
        selectedLanguages: Set<String>,
        selectedArtists: Set<String>
    ): List<Track> {
        val cleanLangs = selectedLanguages.map { it.lowercase().trim() }.toSet()
        val cleanArtists = selectedArtists.map { it.lowercase().trim() }.toSet()

        val matchingTracks = defaultHitTracks.filter { track ->
            val langMatch = cleanLangs.isNotEmpty() && cleanLangs.any { l ->
                track.genre.lowercase().contains(l) || l.contains(track.genre.lowercase())
            }
            val artistMatch = cleanArtists.isNotEmpty() && cleanArtists.any { a ->
                track.artistName.lowercase().contains(a) || a.contains(track.artistName.lowercase())
            }
            langMatch || artistMatch
        }

        return if (matchingTracks.isNotEmpty()) matchingTracks else defaultHitTracks
    }
}
