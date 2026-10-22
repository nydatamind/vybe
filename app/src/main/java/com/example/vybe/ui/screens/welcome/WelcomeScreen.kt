package com.example.vybe.ui.screens.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.VybeEmeraldBright
import com.example.vybe.core.designsystem.vybeNeonGlow
import com.example.vybe.data.provider.MusicLanguage
import com.example.vybe.data.provider.OnboardingArtist
import com.example.vybe.data.provider.OnboardingCatalog
import kotlinx.coroutines.delay

enum class OnboardingStep {
    LANGUAGES,
    ARTISTS,
    GREAT_PICKS
}

@Composable
fun WelcomeScreen(
    onComplete: (selectedLanguages: Set<String>, selectedArtists: Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.LANGUAGES) }
    val selectedLanguageIds = remember { mutableStateListOf("hindi", "punjabi") } // Default popular choices
    val selectedArtistNames = remember { mutableStateListOf<String>() }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        if (currentStep == OnboardingStep.GREAT_PICKS) {
            // "Great picks!" Celebration Transition Screen
            val pickedArtists = remember(selectedArtistNames.toList()) {
                val allArtists = OnboardingCatalog.artists
                selectedArtistNames.mapNotNull { name ->
                    allArtists.find { it.name.equals(name, ignoreCase = true) }
                }.take(3).ifEmpty {
                    allArtists.take(3)
                }
            }

            GreatPicksStep(
                artists = pickedArtists,
                onFinish = {
                    onComplete(selectedLanguageIds.toSet(), selectedArtistNames.toSet())
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header with Progress & Skip Button
                OnboardingHeader(
                    currentStep = currentStep,
                    onBackClick = {
                        if (currentStep == OnboardingStep.ARTISTS) {
                            currentStep = OnboardingStep.LANGUAGES
                        }
                    },
                    onSkipClick = {
                        onComplete(selectedLanguageIds.toSet(), selectedArtistNames.toSet())
                    }
                )

                // Step Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (currentStep == OnboardingStep.LANGUAGES) {
                        LanguageSelectionStep(
                            languages = OnboardingCatalog.languages,
                            selectedIds = selectedLanguageIds,
                            onToggleLanguage = { langId ->
                                if (selectedLanguageIds.contains(langId)) {
                                    if (selectedLanguageIds.size > 1) {
                                        selectedLanguageIds.remove(langId)
                                    }
                                } else {
                                    selectedLanguageIds.add(langId)
                                }
                            }
                        )
                    } else {
                        val availableArtists = remember(selectedLanguageIds.toList()) {
                            OnboardingCatalog.getArtistsForLanguages(selectedLanguageIds.toSet())
                        }
                        ArtistSelectionStep(
                            artists = availableArtists,
                            selectedLanguages = OnboardingCatalog.languages,
                            selectedLanguageIds = selectedLanguageIds,
                            selectedArtistNames = selectedArtistNames,
                            onToggleArtist = { name ->
                                if (selectedArtistNames.contains(name)) {
                                    selectedArtistNames.remove(name)
                                } else {
                                    selectedArtistNames.add(name)
                                }
                            }
                        )
                    }
                }

                // Bottom Navigation Action Bar
                OnboardingBottomBar(
                    currentStep = currentStep,
                    selectedLanguagesCount = selectedLanguageIds.size,
                    selectedArtistsCount = selectedArtistNames.size,
                    onNext = {
                        if (currentStep == OnboardingStep.LANGUAGES) {
                            currentStep = OnboardingStep.ARTISTS
                        } else {
                            currentStep = OnboardingStep.GREAT_PICKS
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GreatPicksStep(
    artists: List<OnboardingArtist>,
    onFinish: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        delay(1800) // 1.8 seconds celebration display
        onFinish()
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.8f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "greatPicksScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 400),
        label = "greatPicksAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFinish() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .padding(horizontal = 24.dp)
        ) {
            // Overlapping Artist Circular Avatars (100% Symmetrically Centered)
            val displayArtists = artists.take(3)
            val avatarSize = 108.dp
            val stepOffset = 78.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(avatarSize),
                contentAlignment = Alignment.Center
            ) {
                displayArtists.forEachIndexed { index, artist ->
                    val xOffset = when (displayArtists.size) {
                        1 -> 0.dp
                        2 -> if (index == 0) (-stepOffset / 2) else (stepOffset / 2)
                        else -> when (index) {
                            0 -> -stepOffset
                            1 -> 0.dp
                            else -> stepOffset
                        }
                    }
                    val zIndexVal = when (displayArtists.size) {
                        1 -> 1f
                        2 -> if (index == 0) 2f else 1f
                        else -> if (index == 1) 3f else 1f
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset)
                            .zIndex(zIndexVal)
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(3.5.dp, Color(0xFF121212), CircleShape)
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = artist.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        AsyncImage(
                            model = artist.artworkUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Bold "Great picks!" text
            Text(
                text = "Great picks!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color.White,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: OnboardingStep,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentStep == OnboardingStep.ARTISTS) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Step 1: Subtle Step Indicator Pill
            Surface(
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "STEP 1 OF 2",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = VybeEmeraldBright,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Stepper Progress Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VybeEmeraldBright)
            )
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (currentStep == OnboardingStep.ARTISTS) VybeEmeraldBright else Color(0x33FFFFFF))
            )
        }

        // Skip Button
        androidx.compose.material3.TextButton(
            onClick = onSkipClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun LanguageSelectionStep(
    languages: List<MusicLanguage>,
    selectedIds: List<String>,
    onToggleLanguage: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
                Text(
                    text = "What music do you like?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose your preferred languages to tune your recommendations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    fontSize = 14.sp
                )
            }
        }

        items(languages, key = { it.id }) { lang ->
            val isSelected = selectedIds.contains(lang.id)
            LanguageCard(
                language = lang,
                isSelected = isSelected,
                onClick = { onToggleLanguage(lang.id) }
            )
        }
    }
}

private fun getCategoryDrawableRes(langId: String): Int {
    return when (langId.lowercase()) {
        "hindi" -> com.example.vybe.R.drawable.cat_hindi
        "english" -> com.example.vybe.R.drawable.cat_english
        "rajasthani" -> com.example.vybe.R.drawable.cat_rajasthani
        "haryanvi" -> com.example.vybe.R.drawable.cat_haryanvi
        "punjabi" -> com.example.vybe.R.drawable.cat_punjabi
        "tamil" -> com.example.vybe.R.drawable.cat_tamil
        "telugu" -> com.example.vybe.R.drawable.cat_telugu
        "malayalam" -> com.example.vybe.R.drawable.cat_malayalam
        "marathi" -> com.example.vybe.R.drawable.cat_marathi
        "gujarati" -> com.example.vybe.R.drawable.cat_gujarati
        "bengali" -> com.example.vybe.R.drawable.cat_bengali
        "kannada" -> com.example.vybe.R.drawable.cat_kannada
        "bhojpuri" -> com.example.vybe.R.drawable.cat_bhojpuri
        "god" -> com.example.vybe.R.drawable.cat_god
        else -> com.example.vybe.R.drawable.cat_hindi
    }
}

@Composable
private fun LanguageCard(
    language: MusicLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Ultra-smooth spring bouncy press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isSelected) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "languageCardScale"
    )

    val cardColor = remember(language.accentColor) { Color(language.accentColor) }
    val drawableRes = remember(language.id) { getCategoryDrawableRes(language.id) }

    Surface(
        color = cardColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        3.dp,
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.White, VybeEmeraldBright)
                        ),
                        RoundedCornerShape(18.dp)
                    )
                } else {
                    Modifier.border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        ) {
            // Cutout artist photo card
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = drawableRes),
                contentDescription = language.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle dark vignette gradient for perfect text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0x55000000), Color(0x22000000), Color(0x88000000))
                        )
                    )
            )

            // Language Name text overlay on top-left
            Text(
                text = language.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 17.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 12.dp)
            )

            // Selected Checkmark Badge (Top-Right indicator with glow)
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSelectionStep(
    artists: List<OnboardingArtist>,
    selectedLanguages: List<MusicLanguage>,
    selectedLanguageIds: List<String>,
    selectedArtistNames: List<String>,
    onToggleArtist: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilterId by remember { mutableStateOf("for_you") }

    // Filter by search query or selected language chip
    val filteredArtists = remember(artists, searchQuery, activeFilterId) {
        val queryClean = searchQuery.trim().lowercase()
        var list = if (activeFilterId == "for_you") {
            artists
        } else {
            artists.filter { it.languageId.equals(activeFilterId, ignoreCase = true) }
        }
        if (queryClean.isNotEmpty()) {
            list = list.filter {
                it.name.lowercase().contains(queryClean) ||
                it.languageName.lowercase().contains(queryClean) ||
                it.hitSongTitle.lowercase().contains(queryClean)
            }
        }
        list
    }

    // Available language chips based on user's selected languages
    val filterChips = remember(selectedLanguageIds.toList(), selectedLanguages) {
        selectedLanguages.filter { selectedLanguageIds.contains(it.id) }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Title & Subtitle
        item(span = { GridItemSpan(3) }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Choose 3 or more artists you like.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap artists to personalize your mixes & discover daily favorites",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        }

        // 2. Search Bar (Modern Dark Glass container)
        item(span = { GridItemSpan(3) }) {
            Surface(
                color = Color(0xFF1E2232),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (searchQuery.isNotEmpty()) VybeEmerald else Color(0x33FFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) VybeEmeraldBright else TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search artists, singers or tracks...",
                                    color = TextMutedDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Filter Chips (Pills: For You, Hindi, Haryanvi, Bhojpuri, etc.)
        item(span = { GridItemSpan(3) }) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "For You" Pill (shows all selected languages sorted by popularity)
                item {
                    val isForYouSelected = activeFilterId == "for_you"
                    Surface(
                        color = if (isForYouSelected) VybeEmerald else Color(0xFF1B1E2B),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = if (isForYouSelected) VybeEmerald else Color(0xFF33384A),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { activeFilterId = "for_you" }
                    ) {
                        Text(
                            text = "For You",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isForYouSelected) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }

                // Selected Language Chips
                filterChips.forEach { lang ->
                    item(key = lang.id) {
                        val isChipSelected = activeFilterId == lang.id
                        Surface(
                            color = if (isChipSelected) VybeEmerald else Color(0xFF1B1E2B),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isChipSelected) VybeEmerald else Color(0xFF33384A),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { activeFilterId = lang.id }
                        ) {
                            Text(
                                text = lang.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChipSelected) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Circular Artist Grid Items (3 Columns)
        if (filteredArtists.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                ) {
                    Text(
                        text = "No artists found for \"$searchQuery\"",
                        color = TextMutedDark,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(filteredArtists, key = { it.id }) { artist ->
                val isSelected = selectedArtistNames.contains(artist.name)
                ArtistOnboardingItem(
                    artist = artist,
                    isSelected = isSelected,
                    onClick = { onToggleArtist(artist.name) }
                )
            }
        }
    }
}

@Composable
private fun ArtistOnboardingItem(
    artist: OnboardingArtist,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "artistCardScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0xFF2C3040), Color(0xFF161822))
                    )
                )
                .border(
                    width = if (isSelected) 3.5.dp else 1.dp,
                    color = if (isSelected) Color.White else Color(0x22FFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 28.sp
            )

            val context = androidx.compose.ui.platform.LocalContext.current
            AsyncImage(
                model = remember(artist.artworkUrl) {
                    coil.request.ImageRequest.Builder(context)
                        .data(artist.artworkUrl)
                        .crossfade(true)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                },
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                // Dimming & Glowing Tick Overlay (Spotify style)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontSize = 13.5.sp,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = artist.languageName,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontSize = 11.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OnboardingBottomBar(
    currentStep: OnboardingStep,
    selectedLanguagesCount: Int,
    selectedArtistsCount: Int,
    onNext: () -> Unit
) {
    Surface(
        color = DarkBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        val buttonText: String
        val isEnabled: Boolean

        if (currentStep == OnboardingStep.LANGUAGES) {
            isEnabled = selectedLanguagesCount > 0
            buttonText = if (isEnabled) "Next: Choose Artists ($selectedLanguagesCount selected)" else "Select at least 1 language"
        } else {
            isEnabled = true
            buttonText = if (selectedArtistsCount > 0) {
                "Finish & Start Listening ($selectedArtistsCount selected)"
            } else {
                "Start Listening"
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(28.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isEnabled) com.example.vybe.core.designsystem.VybeGradientEmerald
                        else androidx.compose.ui.graphics.Brush.linearGradient(listOf(DarkCard, DarkCard))
                    )
                    .clickable(enabled = isEnabled) { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color.Black else TextMutedDark,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (currentStep == OnboardingStep.LANGUAGES) Icons.Default.ArrowForward else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isEnabled) Color.Black else TextMutedDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


