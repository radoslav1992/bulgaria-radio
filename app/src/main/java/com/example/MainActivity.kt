package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.database.AppDatabase
import com.example.data.model.NetworkCountry
import com.example.data.model.NetworkLanguage
import com.example.data.model.StationEntity
import com.example.data.network.RetrofitInstance
import com.example.data.repository.RadioRepository
import com.example.player.RadioPlaybackState
import com.example.player.RadioPlayerManager
import com.example.ui.CountriesUiState
import com.example.ui.LanguagesUiState
import com.example.ui.RadioViewModel
import com.example.ui.RadioViewModelFactory
import com.example.ui.StationsUiState
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val database = remember { AppDatabase.getDatabase(context) }
                val repository = remember { RadioRepository(database.stationDao(), RetrofitInstance.api) }
                val playerManager = remember { RadioPlayerManager(context.applicationContext) }
                val viewModel: RadioViewModel = viewModel(
                    factory = RadioViewModelFactory(
                        context.applicationContext as Application,
                        repository,
                        playerManager
                    )
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WalnutDark
                ) {
                    RadioAppMainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioAppMainScreen(viewModel: RadioViewModel) {
    var currentTab by remember { mutableStateOf("discover") }

    val searchResults by viewModel.stationsState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    var showLocationSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showFullscreenPlayer by remember { mutableStateOf(false) }

    val woodGrainBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A0F07),
            Color(0xFF231509),
            Color(0xFF1A0F07),
            Color(0xFF150B05),
            Color(0xFF1E1109)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                currentStation?.let { station ->
                    MiniPlayerBar(
                        station = station,
                        playbackState = playbackState,
                        favorites = favorites,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onToggleFavorite = { viewModel.toggleFavorite(station) },
                        onExpand = { showFullscreenPlayer = true }
                    )
                }

                NavigationBar(
                    containerColor = Color(0xFF0D0704),
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = AmberGlow.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                    )
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Radio, contentDescription = "Tune In") },
                        label = { Text("Tune In", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                        selected = currentTab == "discover",
                        onClick = { currentTab = "discover" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmberBright,
                            selectedTextColor = AmberGlow,
                            indicatorColor = MahoganyPanel.copy(alpha = 0.6f),
                            unselectedIconColor = FadedLabel,
                            unselectedTextColor = FadedLabel
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = "Presets") },
                        label = { Text("Presets", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                        selected = currentTab == "favorites",
                        onClick = { currentTab = "favorites" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmberBright,
                            selectedTextColor = AmberGlow,
                            indicatorColor = MahoganyPanel.copy(alpha = 0.6f),
                            unselectedIconColor = FadedLabel,
                            unselectedTextColor = FadedLabel
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                        selected = currentTab == "recents",
                        onClick = { currentTab = "recents" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmberBright,
                            selectedTextColor = AmberGlow,
                            indicatorColor = MahoganyPanel.copy(alpha = 0.6f),
                            unselectedIconColor = FadedLabel,
                            unselectedTextColor = FadedLabel
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(woodGrainBackground)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "discover" -> DiscoverTabScreen(
                    viewModel = viewModel,
                    searchResults = searchResults,
                    favorites = favorites,
                    selectedCountry = selectedCountry,
                    selectedLanguage = selectedLanguage,
                    onOpenLocationSheet = { showLocationSheet = true },
                    onOpenLanguageSheet = { showLanguageSheet = true }
                )
                "favorites" -> FavoritesTabScreen(
                    favorites = favorites,
                    onPlay = { viewModel.playStation(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
                "recents" -> RecentsTabScreen(
                    recentlyPlayed = recentlyPlayed,
                    favorites = favorites,
                    onPlay = { viewModel.playStation(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
            }
        }
    }

    if (showLocationSheet) {
        CountrySelectionSheet(
            viewModel = viewModel,
            onDismiss = { showLocationSheet = false },
            onCountrySelected = {
                viewModel.selectCountry(it)
                showLocationSheet = false
            }
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            viewModel = viewModel,
            onDismiss = { showLanguageSheet = false },
            onLanguageSelected = {
                viewModel.selectLanguage(it)
                showLanguageSheet = false
            }
        )
    }

    if (showFullscreenPlayer && currentStation != null) {
        val activeStation = currentStation!!
        FullscreenPlayerDialog(
            station = activeStation,
            playbackState = playbackState,
            favorites = favorites,
            onDismiss = { showFullscreenPlayer = false },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onToggleFavorite = { viewModel.toggleFavorite(activeStation) },
            onVolumeChange = { viewModel.setVolume(it) }
        )
    }
}

@Composable
fun DiscoverTabScreen(
    viewModel: RadioViewModel,
    searchResults: StationsUiState,
    favorites: List<StationEntity>,
    selectedCountry: NetworkCountry?,
    selectedLanguage: NetworkLanguage?,
    onOpenLocationSheet: () -> Unit,
    onOpenLanguageSheet: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "World Radio",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = AmberGlow
        )
        Text(
            text = "Tune into stations from across the globe",
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            color = FadedLabel
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Vintage-styled search field
        TextField(
            value = query,
            onValueChange = {
                viewModel.updateSearchQuery(it)
                viewModel.performSearch()
            },
            placeholder = { Text("Search stations...", color = FadedLabel, fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AmberGlow.copy(alpha = 0.7f)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.updateSearchQuery("")
                        viewModel.performSearch()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = FadedLabel)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = WalnutMedium,
                unfocusedContainerColor = WalnutMedium,
                disabledContainerColor = WalnutMedium,
                focusedTextColor = CreamWhite,
                unfocusedTextColor = CreamWhite,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmberGlow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .testTag("station_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vintage filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedCountry != null,
                onClick = onOpenLocationSheet,
                label = {
                    Text(
                        text = selectedCountry?.name ?: "Any Region",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Serif
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = WalnutLight,
                    labelColor = ParchmentText,
                    selectedContainerColor = AmberGlow.copy(alpha = 0.15f),
                    selectedLabelColor = AmberBright,
                    iconColor = FadedLabel,
                    selectedLeadingIconColor = AmberBright
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCountry != null,
                    borderColor = LeatherBrown.copy(alpha = 0.5f),
                    selectedBorderColor = AmberGlow.copy(alpha = 0.6f),
                    borderWidth = 1.dp
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            FilterChip(
                selected = selectedLanguage != null,
                onClick = onOpenLanguageSheet,
                label = {
                    Text(
                        text = selectedLanguage?.name?.replaceFirstChar { it.uppercase() } ?: "Any Language",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Serif
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = WalnutLight,
                    labelColor = ParchmentText,
                    selectedContainerColor = AmberGlow.copy(alpha = 0.15f),
                    selectedLabelColor = AmberBright,
                    iconColor = FadedLabel,
                    selectedLeadingIconColor = AmberBright
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedLanguage != null,
                    borderColor = LeatherBrown.copy(alpha = 0.5f),
                    selectedBorderColor = AmberGlow.copy(alpha = 0.6f),
                    borderWidth = 1.dp
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            if (selectedCountry != null || selectedLanguage != null || query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        viewModel.selectCountry(null)
                        viewModel.selectLanguage(null)
                        viewModel.updateSearchQuery("")
                        viewModel.performSearch()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(RadioRed.copy(alpha = 0.15f), shape = CircleShape)
                        .border(1.dp, RadioRed.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Filters", tint = RadioRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (searchResults) {
            is StationsUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AmberGlow)
                }
            }
            is StationsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AmberGlow)
                }
            }
            is StationsUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TubeOrange, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = searchResults.message, color = ParchmentText, textAlign = TextAlign.Center, fontFamily = FontFamily.Serif)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchFeatured() },
                        colors = ButtonDefaults.buttonColors(containerColor = MahoganyPanel)
                    ) {
                        Text("Retry Connection", fontFamily = FontFamily.Serif, color = AmberGlow)
                    }
                }
            }
            is StationsUiState.Success -> {
                val stationsList = searchResults.stations
                if (stationsList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = FadedLabel, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No stations found on this frequency.", color = FadedLabel, textAlign = TextAlign.Center, fontFamily = FontFamily.Serif)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(stationsList, key = { it.stationuuid }) { station ->
                            val isFav = favorites.any { it.stationuuid == station.stationuuid }
                            StationRowItem(
                                station = station,
                                isFav = isFav,
                                onPlay = { viewModel.playStation(station) },
                                onToggleFavorite = { viewModel.toggleFavorite(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationRowItem(
    station: StationEntity,
    isFav: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = WalnutMedium.copy(alpha = 0.9f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmberGlow.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station icon with vintage dial look
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(WalnutLight, VinylBlack)
                        )
                    )
                    .border(1.dp, BrassGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (station.favicon.isNotBlank()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = "Station Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = AmberGlow.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Text(
                    text = station.name,
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        station.country.ifEmpty { null },
                        station.language.ifEmpty { null }
                    ).joinToString(" • "),
                    color = FadedLabel,
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (station.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val firstTags = station.tags.split(",").take(2).joinToString(" | ")
                    Text(
                        text = firstTags.uppercase(),
                        color = BrassGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (isFav) AmberBright else FadedLabel
                )
            }
        }
    }
}

@Composable
fun FavoritesTabScreen(
    favorites: List<StationEntity>,
    onPlay: (StationEntity) -> Unit,
    onToggleFavorite: (StationEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Preset Stations",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = AmberGlow
        )
        Text(
            text = "Your saved radio presets",
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            color = FadedLabel
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = FadedLabel,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No presets saved yet.",
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Star your favorite stations to save them as presets for quick tuning.",
                    color = FadedLabel,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites, key = { it.stationuuid }) { station ->
                    FavoriteGridCard(
                        station = station,
                        onPlay = { onPlay(station) },
                        onRemove = { onToggleFavorite(station) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteGridCard(
    station: StationEntity,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WalnutMedium),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmberGlow.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable { onPlay() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .background(WalnutDark.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove Preset", tint = FadedLabel, modifier = Modifier.size(14.dp))
                }
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MahoganyPanel, VinylBlack)
                        )
                    )
                    .border(2.dp, BrassGold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (station.favicon.isNotBlank()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = "Station Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = AmberGlow.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = station.name,
                color = CreamWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = station.country.ifEmpty { "Worldwide" },
                color = FadedLabel,
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RecentsTabScreen(
    recentlyPlayed: List<StationEntity>,
    favorites: List<StationEntity>,
    onPlay: (StationEntity) -> Unit,
    onToggleFavorite: (StationEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Listening Log",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = AmberGlow
        )
        Text(
            text = "Stations you've tuned into recently",
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            color = FadedLabel
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (recentlyPlayed.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = FadedLabel,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nothing in the log yet.",
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Once you tune into a station, it will be recorded here.",
                    color = FadedLabel,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recentlyPlayed, key = { it.stationuuid }) { station ->
                    val isFav = favorites.any { it.stationuuid == station.stationuuid }
                    StationRowItem(
                        station = station,
                        isFav = isFav,
                        onPlay = { onPlay(station) },
                        onToggleFavorite = { onToggleFavorite(station) }
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    station: StationEntity,
    playbackState: RadioPlaybackState,
    favorites: List<StationEntity>,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onExpand: () -> Unit
) {
    val isFav = favorites.any { it.stationuuid == station.stationuuid }

    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val isPlayingMode = playbackState is RadioPlaybackState.Playing

    Card(
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1109)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AmberGlow.copy(alpha = 0.2f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .clickable { onExpand() }
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing indicator light
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isPlayingMode) DialGreen.copy(alpha = glowAlpha) else FadedLabel.copy(alpha = 0.3f),
                                VinylBlack
                            )
                        )
                    )
                    .border(2.dp, BrassGold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (station.favicon.isNotBlank()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = "Station Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusText = when (playbackState) {
                        is RadioPlaybackState.Playing -> "On Air"
                        is RadioPlaybackState.Buffering -> "Tuning..."
                        is RadioPlaybackState.Paused -> "Standby"
                        is RadioPlaybackState.Error -> "No Signal"
                        else -> "Connecting"
                    }
                    val statusColor = when (playbackState) {
                        is RadioPlaybackState.Playing -> DialGreen
                        is RadioPlaybackState.Buffering -> AmberBright
                        is RadioPlaybackState.Error -> RadioRed
                        else -> FadedLabel
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        color = FadedLabel,
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Preset Button",
                    tint = if (isFav) AmberBright else FadedLabel
                )
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(44.dp)
                    .background(MahoganyPanel, CircleShape)
                    .border(1.dp, BrassGold.copy(alpha = 0.4f), CircleShape)
            ) {
                when (playbackState) {
                    is RadioPlaybackState.Buffering -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AmberGlow,
                            strokeWidth = 2.dp
                        )
                    }
                    is RadioPlaybackState.Playing -> {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AmberGlow)
                    }
                    else -> {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = AmberGlow)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPlayerDialog(
    station: StationEntity,
    playbackState: RadioPlaybackState,
    favorites: List<StationEntity>,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val isFav = favorites.any { it.stationuuid == station.stationuuid }
    var scaleVolume by remember { mutableStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition()
    val dialGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val isPlayingMode = playbackState is RadioPlaybackState.Playing

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VinylBlack
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2C1810),
                                Color(0xFF1A0F07),
                                Color(0xFF0D0704)
                            )
                        )
                    )
                    .safeDrawingPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .background(WalnutMedium, CircleShape)
                                .border(1.dp, BrassGold.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = CreamWhite)
                        }

                        Text(
                            text = "Now Playing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = AmberGlow
                        )

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(44.dp)
                                .background(WalnutMedium, CircleShape)
                                .border(1.dp, BrassGold.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Preset button",
                                tint = if (isFav) AmberBright else CreamWhite
                            )
                        }
                    }

                    // Vintage dial display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .border(
                                    6.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            BrassGold.copy(alpha = 0.6f),
                                            BrassLight.copy(alpha = 0.8f),
                                            BrassGold.copy(alpha = 0.4f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .padding(6.dp)
                                .border(2.dp, AmberGlow.copy(alpha = if (isPlayingMode) dialGlow else 0.2f), CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            WalnutLight,
                                            VinylBlack
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (station.favicon.isNotBlank()) {
                                AsyncImage(
                                    model = station.favicon,
                                    contentDescription = "Station Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Radio, contentDescription = null, tint = AmberGlow.copy(alpha = 0.8f), modifier = Modifier.size(64.dp))
                            }
                        }

                        // Signal indicator bars
                        if (isPlayingMode) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.height(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                repeat(7) { i ->
                                    val pulseAnim = rememberInfiniteTransition()
                                    val h by pulseAnim.animateFloat(
                                        initialValue = 4f,
                                        targetValue = 20f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(500 + (i * 100), easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h.dp)
                                            .background(
                                                if (i < 3) DialGreen
                                                else if (i < 5) AmberGlow
                                                else TubeOrange,
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    // Station info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = station.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = CreamWhite,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = listOfNotNull(
                                station.country.ifEmpty { null },
                                station.language.ifEmpty { null }
                            ).joinToString("  •  "),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif,
                            color = FadedLabel,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (station.tags.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val cleanTags = station.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                cleanTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .background(BrassGold.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                                            .border(1.dp, BrassGold.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag.uppercase(), fontSize = 10.sp, color = BrassGold, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                                    }
                                }
                            }
                        }
                    }

                    // Play/Pause control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(MahoganyPanel, WalnutDark)
                                    ),
                                    CircleShape
                                )
                                .border(
                                    3.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            BrassGold.copy(alpha = 0.7f),
                                            BrassLight.copy(alpha = 0.9f),
                                            BrassGold.copy(alpha = 0.5f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            when (playbackState) {
                                is RadioPlaybackState.Buffering -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = AmberGlow,
                                        strokeWidth = 3.dp
                                    )
                                }
                                is RadioPlaybackState.Playing -> {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AmberGlow, modifier = Modifier.size(36.dp))
                                }
                                else -> {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = AmberGlow, modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }

                    // Volume knob (slider)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "VOLUME",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = FadedLabel,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeMute, contentDescription = null, tint = FadedLabel)
                            Slider(
                                value = scaleVolume,
                                onValueChange = {
                                    scaleVolume = it
                                    onVolumeChange(it)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = BrassGold,
                                    activeTrackColor = AmberGlow,
                                    inactiveTrackColor = WalnutLight
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AmberGlow)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionSheet(
    viewModel: RadioViewModel,
    onDismiss: () -> Unit,
    onCountrySelected: (NetworkCountry?) -> Unit
) {
    val countriesState by viewModel.countriesState.collectAsStateWithLifecycle()
    var sheetQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1109),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .padding(16.dp)
        ) {
            Text(
                text = "Select Region",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = AmberGlow,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TextField(
                value = sheetQuery,
                onValueChange = { sheetQuery = it },
                placeholder = { Text("Filter regions...", color = FadedLabel, fontFamily = FontFamily.Serif) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AmberGlow.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = WalnutMedium,
                    unfocusedContainerColor = WalnutMedium,
                    focusedTextColor = CreamWhite,
                    unfocusedTextColor = CreamWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberGlow.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(bottom = 12.dp)
            )

            ListItem(
                headlineContent = { Text("All Regions (Worldwide)", color = AmberBright, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) },
                leadingContent = { Icon(Icons.Default.Public, contentDescription = null, tint = AmberBright) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCountrySelected(null) }
                    .padding(vertical = 4.dp)
            )
            HorizontalDivider(color = AmberGlow.copy(alpha = 0.1f))

            when (countriesState) {
                is CountriesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AmberGlow)
                    }
                }
                is CountriesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Failed to load regions.", color = RadioRed, fontFamily = FontFamily.Serif)
                    }
                }
                is CountriesUiState.Success -> {
                    val list = (countriesState as CountriesUiState.Success).countries
                    val filtered = list.filter {
                        it.name.contains(sheetQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.name }) { country ->
                            ListItem(
                                headlineContent = { Text(country.name, color = CreamWhite, fontFamily = FontFamily.Serif) },
                                trailingContent = {
                                    Box(
                                        modifier = Modifier
                                            .background(AmberGlow.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                            .border(1.dp, AmberGlow.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("${country.stationcount}", color = BrassGold, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                                    }
                                },
                                leadingContent = { Icon(Icons.Default.Place, contentDescription = null, tint = FadedLabel) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCountrySelected(country) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    viewModel: RadioViewModel,
    onDismiss: () -> Unit,
    onLanguageSelected: (NetworkLanguage?) -> Unit
) {
    val languagesState by viewModel.languagesState.collectAsStateWithLifecycle()
    var sheetQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1109),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .padding(16.dp)
        ) {
            Text(
                text = "Select Language",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = AmberGlow,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TextField(
                value = sheetQuery,
                onValueChange = { sheetQuery = it },
                placeholder = { Text("Filter languages...", color = FadedLabel, fontFamily = FontFamily.Serif) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AmberGlow.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = WalnutMedium,
                    unfocusedContainerColor = WalnutMedium,
                    focusedTextColor = CreamWhite,
                    unfocusedTextColor = CreamWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberGlow.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(bottom = 12.dp)
            )

            ListItem(
                headlineContent = { Text("Any Language", color = AmberBright, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null, tint = AmberBright) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLanguageSelected(null) }
                    .padding(vertical = 4.dp)
            )
            HorizontalDivider(color = AmberGlow.copy(alpha = 0.1f))

            when (languagesState) {
                is LanguagesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AmberGlow)
                    }
                }
                is LanguagesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Failed to load languages.", color = RadioRed, fontFamily = FontFamily.Serif)
                    }
                }
                is LanguagesUiState.Success -> {
                    val list = (languagesState as LanguagesUiState.Success).languages
                    val filtered = list.filter {
                        it.name.contains(sheetQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.name }) { language ->
                            ListItem(
                                headlineContent = { Text(language.name.replaceFirstChar { it.uppercase() }, color = CreamWhite, fontFamily = FontFamily.Serif) },
                                trailingContent = {
                                    Box(
                                        modifier = Modifier
                                            .background(AmberGlow.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                            .border(1.dp, AmberGlow.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("${language.stationcount}", color = BrassGold, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                                    }
                                },
                                leadingContent = { Icon(Icons.Default.Translate, contentDescription = null, tint = FadedLabel) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLanguageSelected(language) }
                            )
                        }
                    }
                }
            }
        }
    }
}
