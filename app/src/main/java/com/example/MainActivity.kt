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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

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
                    color = MaterialTheme.colorScheme.background
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
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf("discover") }

    // Collect variables
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

    // Custom deep background brush
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F1B29),
            Color(0xFF131F2E),
            Color(0xFF0B111A)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Persistent Player bar at the bottom
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
                    containerColor = Color(0xFF080D14),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                        label = { Text("Discover") },
                        selected = currentTab == "discover",
                        onClick = { currentTab = "discover" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00ADB5),
                            selectedTextColor = Color(0xFF00ADB5),
                            indicatorColor = Color(0xFF132A33),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        selected = currentTab == "favorites",
                        onClick = { currentTab = "favorites" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00ADB5),
                            selectedTextColor = Color(0xFF00ADB5),
                            indicatorColor = Color(0xFF132A33),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
                        label = { Text("Recents") },
                        selected = currentTab == "recents",
                        onClick = { currentTab = "recents" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00ADB5),
                            selectedTextColor = Color(0xFF00ADB5),
                            indicatorColor = Color(0xFF132A33),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
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

    // Modal Bottom Sheets for countries and languages selection
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

    // Full screen detailed Player Dialog
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
        // Styled Cosmic Header
        Text(
            text = "Global Waves",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Explore live streams from all around the globe",
            fontSize = 13.sp,
            color = Color(0xFFA1B0C4)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Premium Search Card
        TextField(
            value = query,
            onValueChange = {
                viewModel.updateSearchQuery(it)
                viewModel.performSearch()
            },
            placeholder = { Text("Search by station name...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.updateSearchQuery("")
                        viewModel.performSearch()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1B2838),
                unfocusedContainerColor = Color(0xFF1B2838),
                disabledContainerColor = Color(0xFF1B2838),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("station_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal filter selections
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location Select Chip
            FilterChip(
                selected = selectedCountry != null,
                onClick = onOpenLocationSheet,
                label = { 
                    Text(
                        text = selectedCountry?.name ?: "Any Location",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF14202F),
                    labelColor = Color.LightGray,
                    selectedContainerColor = Color(0xFF00ADB5).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF00ADB5)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCountry != null,
                    borderColor = Color(0xFF26394F),
                    selectedBorderColor = Color(0xFF00ADB5),
                    borderWidth = 1.dp
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            // Language Select Chip
            FilterChip(
                selected = selectedLanguage != null,
                onClick = onOpenLanguageSheet,
                label = { 
                    Text(
                        text = selectedLanguage?.name?.replaceFirstChar { it.uppercase() } ?: "Any Language",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF14202F),
                    labelColor = Color.LightGray,
                    selectedContainerColor = Color(0xFF00ADB5).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF00ADB5)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedLanguage != null,
                    borderColor = Color(0xFF26394F),
                    selectedBorderColor = Color(0xFF00ADB5),
                    borderWidth = 1.dp
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            // Reset chip
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
                        .background(Color(0xFF231C24), shape = CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Filters", tint = Color(0xFFE94560))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Central States rendering
        when (searchResults) {
            is StationsUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00ADB5))
                }
            }
            is StationsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00ADB5))
                }
            }
            is StationsUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = searchResults.message, color = Color.LightGray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.fetchFeatured() }) {
                        Text("Retry Connection")
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
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No radio stations match your search filters.", color = Color.Gray, textAlign = TextAlign.Center)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14202F).copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favicon image loader
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1F2E42)),
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
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Metadata
            Column(
                modifier = Modifier
                    .weight(1.0f)
            ) {
                Text(
                    text = station.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
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
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (station.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val firstTags = station.tags.split(",").take(2).joinToString(" | ")
                    Text(
                        text = firstTags.uppercase(),
                        color = Color(0xFF00ADB5),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Interactive Controls
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (isFav) Color(0xFFE94560) else Color.LightGray
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
            text = "Your Favorites",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Your primary curated internet radio channels",
            fontSize = 13.sp,
            color = Color(0xFFA1B0C4)
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No saved channels yet.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Stars added while playing or exploring will appear organized here.",
                    color = Color.Gray,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14202F).copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
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
                        .size(36.dp)
                        .background(Color(0xFF0F1B29).copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove Favorite", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            // Rounded Square Logo Container
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F2E42)),
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
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = station.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = station.country.ifEmpty { "Global Stream" },
                color = Color.Gray,
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
            text = "Recently Played",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Your stream history is kept safe locally on your device",
            fontSize = 13.sp,
            color = Color(0xFFA1B0C4)
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
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No history recorded.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Once you connect and tune into a station, it will instantly appear in this history log.",
                    color = Color.Gray,
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
    
    // Rotating Album Cover Animation Setup
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val isPlayingMode = playbackState is RadioPlaybackState.Playing

    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() }
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rotating image
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        if (isPlayingMode) {
                            rotationZ = rotation
                        }
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF1F2E42)),
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
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Station details
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = station.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusText = when (playbackState) {
                        is RadioPlaybackState.Playing -> "Tuned In"
                        is RadioPlaybackState.Buffering -> "Buffering..."
                        is RadioPlaybackState.Paused -> "Paused"
                        is RadioPlaybackState.Error -> "Stream Error"
                        else -> "Connecting"
                    }
                    val statusColor = when (playbackState) {
                        is RadioPlaybackState.Playing -> Color(0xFF00ADB5)
                        is RadioPlaybackState.Buffering -> Color.Yellow
                        is RadioPlaybackState.Error -> Color.Red
                        else -> Color.Gray
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controls
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like Button",
                    tint = if (isFav) Color(0xFFE94560) else Color.LightGray
                )
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(48.dp)
            ) {
                when (playbackState) {
                    is RadioPlaybackState.Buffering -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF00ADB5),
                            strokeWidth = 2.dp
                        )
                    }
                    is RadioPlaybackState.Playing -> {
                        Icon(Icons.Default.Pause, contentDescription = "Pause button", tint = Color.White)
                    }
                    else -> {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play button", tint = Color.White)
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

    // Rotating Animation for vinyl inside Full screen Player Dialogue
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val isPlayingMode = playbackState is RadioPlaybackState.Playing

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A1017)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF14243B),
                                Color(0xFF0A1017)
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
                    // Header control row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = Color.White)
                        }

                        Text(
                            text = "Now Playing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Fav button",
                                tint = if (isFav) Color(0xFFE94560) else Color.White
                            )
                        }
                    }

                    // Rotating visual artwork
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .border(8.dp, Color.White.copy(alpha = 0.04f), CirclesShapeStyle)
                                .padding(12.dp)
                                .graphicsLayer {
                                    if (isPlayingMode) {
                                        rotationZ = rotation
                                    }
                                }
                                .clip(CirclesShapeStyle)
                                .background(Color(0xFF111E2E)),
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
                                Icon(Icons.Default.Radio, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            }
                        }
                        
                        // Active mini bouncing pulse waves mockup
                        if (isPlayingMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.height(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                repeat(5) { i ->
                                    val pulseAnim = rememberInfiniteTransition()
                                    val h by pulseAnim.animateFloat(
                                        initialValue = 4f,
                                        targetValue = 18f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(400 + (i * 120), easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(h.dp)
                                            .background(Color(0xFF00ADB5), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(34.dp)) // keeps spacing even
                        }
                    }

                    // Metadata display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = station.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
                            color = Color(0xFFA1B0C4),
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
                                            .background(Color(0xFF00ADB5).copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag.uppercase(), fontSize = 10.sp, color = Color(0xFF00ADB5), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Player buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(82.dp)
                                .background(Color(0xFF00ADB5), CircleShape)
                        ) {
                            when (playbackState) {
                                is RadioPlaybackState.Buffering -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                }
                                is RadioPlaybackState.Playing -> {
                                    Icon(Icons.Default.Pause, contentDescription = "Play/Pause control", tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                                else -> {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause control", tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }

                    // Volume controller bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeMute, contentDescription = null, tint = Color.Gray)
                            Slider(
                                value = scaleVolume,
                                onValueChange = {
                                    scaleVolume = it
                                    onVolumeChange(it)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00ADB5),
                                    activeTrackColor = Color(0xFF00ADB5),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF00ADB5))
                        }
                    }
                }
            }
        }
    }
}

val CirclesShapeStyle = CircleShape

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
        containerColor = Color(0xFF131D2A),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .padding(16.dp)
        ) {
            Text(
                text = "Select Country",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sheet Search field
            TextField(
                value = sheetQuery,
                onValueChange = { sheetQuery = it },
                placeholder = { Text("Filter countries...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1B2838),
                    unfocusedContainerColor = Color(0xFF1B2838),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // "Any country" fallback action
            ListItem(
                headlineContent = { Text("Any Location (Worldwide)", color = Color(0xFF00ADB5), fontWeight = FontWeight.Bold) },
                leadingContent = { Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF00ADB5)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCountrySelected(null) }
                    .padding(vertical = 4.dp)
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            when (countriesState) {
                is CountriesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00ADB5))
                    }
                }
                is CountriesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Failed to load countries list.", color = Color.Red)
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
                                headlineContent = { Text(country.name, color = Color.White) },
                                trailingContent = { 
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("${country.stationcount} stations", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                },
                                leadingContent = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray) },
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
        containerColor = Color(0xFF131D2A),
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
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sheet Search field
            TextField(
                value = sheetQuery,
                onValueChange = { sheetQuery = it },
                placeholder = { Text("Filter languages...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1B2838),
                    unfocusedContainerColor = Color(0xFF1B2838),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // "Any Language" standard option
            ListItem(
                headlineContent = { Text("Any Language", color = Color(0xFF00ADB5), fontWeight = FontWeight.Bold) },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF00ADB5)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLanguageSelected(null) }
                    .padding(vertical = 4.dp)
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            when (languagesState) {
                is LanguagesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00ADB5))
                    }
                }
                is LanguagesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Failed to load languages list.", color = Color.Red)
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
                                headlineContent = { Text(language.name.replaceFirstChar { it.uppercase() }, color = Color.White) },
                                trailingContent = { 
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("${language.stationcount} stations", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                },
                                leadingContent = { Icon(Icons.Default.Translate, contentDescription = null, tint = Color.Gray) },
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
