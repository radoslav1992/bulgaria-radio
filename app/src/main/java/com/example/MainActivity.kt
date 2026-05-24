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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.database.AppDatabase
import com.example.data.model.StationEntity
import com.example.data.network.RetrofitInstance
import com.example.data.repository.RadioRepository
import com.example.player.RadioPlaybackState
import com.example.player.RadioPlayerManager
import com.example.ui.RadioViewModel
import com.example.ui.RadioViewModelFactory
import com.example.ui.StationsUiState
import com.example.ui.theme.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this)
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

                // Tricolor accent strip above nav bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(BulgariaWhite.copy(alpha = 0.7f)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(BulgariaGreen.copy(alpha = 0.7f)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(BulgariaRed.copy(alpha = 0.7f)))
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
                        icon = { Icon(Icons.Default.Radio, contentDescription = "Слушай") },
                        label = { Text("Слушай", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
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
                        icon = { Icon(Icons.Default.Star, contentDescription = "Любими") },
                        label = { Text("Любими", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
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
                        icon = { Icon(Icons.Default.History, contentDescription = "История") },
                        label = { Text("История", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
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
                    favorites = favorites
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
    favorites: List<StationEntity>
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Bulgarian tricolor accent bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(BulgariaWhite))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(BulgariaGreen))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(BulgariaRed))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalFlorist,
                contentDescription = null,
                tint = BulgariaRed.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Радио България",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = AmberGlow
                )
                Text(
                    text = "Слушай български радио станции",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    color = FadedLabel
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = query,
            onValueChange = {
                viewModel.updateSearchQuery(it)
                viewModel.performSearch()
            },
            placeholder = { Text("Търси станции...", color = FadedLabel, fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AmberGlow.copy(alpha = 0.7f)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.updateSearchQuery("")
                        viewModel.performSearch()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Изчисти", tint = FadedLabel)
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

        BannerAd(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(12.dp))

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
                        Text("Опитай отново", fontFamily = FontFamily.Serif, color = AmberGlow)
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
                        Text(text = "Няма намерени станции.", color = FadedLabel, textAlign = TextAlign.Center, fontFamily = FontFamily.Serif)
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
fun BannerAd(modifier: Modifier = Modifier) {
    val adUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111" // Google test banner
    } else {
        "ca-app-pub-4599369851069924/4120029820"
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
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
                        contentDescription = "Лого на станция",
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
                    text = station.language.ifEmpty { "България" },
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
                    contentDescription = "Любими",
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
            text = "Любими станции",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = AmberGlow
        )
        Text(
            text = "Запазените ви радио станции",
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
                    text = "Все още няма любими.",
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Маркирайте любимите си станции, за да ги запазите за бърз достъп.",
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
                    Icon(Icons.Default.Clear, contentDescription = "Премахни", tint = FadedLabel, modifier = Modifier.size(14.dp))
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
                        contentDescription = "Лого на станция",
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
                text = "България",
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
            text = "Дневник",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = AmberGlow
        )
        Text(
            text = "Последно слушани станции",
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
                    text = "Все още няма записи.",
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Когато пуснете станция, тя ще бъде записана тук.",
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
    val isPlayingMode = playbackState is RadioPlaybackState.Playing

    val infiniteTransition = rememberInfiniteTransition()
    val pilotGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3A2518),
                        Color(0xFF2C1810),
                        Color(0xFF1E1109)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(BrassGold.copy(alpha = 0.5f), BrassGold.copy(alpha = 0.15f))
                ),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .clickable { onExpand() }
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        Brush.radialGradient(
                            colors = if (isPlayingMode)
                                listOf(PilotLampGreen.copy(alpha = pilotGlow), PilotLampGreen.copy(alpha = 0.3f))
                            else
                                listOf(PilotLampOff, PilotLampOff.copy(alpha = 0.5f))
                        ),
                        CircleShape
                    )
                    .border(1.dp, BrassGold.copy(alpha = 0.5f), CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DialFaceGreen, DialFaceLight, DialFaceGreen)
                        )
                    )
                    .border(1.dp, BrassGold.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(VinylBlack.copy(alpha = 0.4f))
                            .border(1.dp, AmberGlow.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (station.favicon.isNotBlank()) {
                            AsyncImage(
                                model = station.favicon,
                                contentDescription = "Лого на станция",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                            )
                        } else {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = AmberGlow.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = station.name,
                            color = AmberBright,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (playbackState) {
                                is RadioPlaybackState.Playing -> "В ЕФИР"
                                is RadioPlaybackState.Buffering -> "НАСТРОЙКА..."
                                is RadioPlaybackState.Paused -> "ПАУЗА"
                                is RadioPlaybackState.Error -> "НЯМА СИГНАЛ"
                                else -> "..."
                            },
                            color = if (isPlayingMode) PilotLampGreen.copy(alpha = 0.8f) else FadedLabel,
                            fontFamily = FontFamily.Serif,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isFav) AmberBright else ChromeDark,
                                if (isFav) BrassGold else Color(0xFF4A4A4A)
                            )
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, BrassGold.copy(alpha = 0.6f), CircleShape)
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Любими",
                    tint = if (isFav) VinylBlack else CreamWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MahoganyPanel, Color(0xFF2A1508))
                        ),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            colors = listOf(BrassLight, BrassGold, BrassLight)
                        ),
                        CircleShape
                    )
                    .clickable { onTogglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                when (playbackState) {
                    is RadioPlaybackState.Buffering -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AmberGlow,
                            strokeWidth = 2.dp
                        )
                    }
                    is RadioPlaybackState.Playing -> {
                        Icon(Icons.Default.Pause, contentDescription = "Пауза", tint = AmberGlow, modifier = Modifier.size(18.dp))
                    }
                    else -> {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Пусни", tint = AmberGlow, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SpeakerGrille(modifier: Modifier = Modifier) {
    val fabricColor = GrilleFabric
    val slatColor = GrilleSlat
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fabricColor)
            .drawBehind {
                val slatSpacing = 6.dp.toPx()
                val slatHeight = 2.dp.toPx()
                var y = slatSpacing
                while (y < size.height) {
                    drawRoundRect(
                        color = slatColor,
                        topLeft = Offset(4.dp.toPx(), y),
                        size = Size(size.width - 8.dp.toPx(), slatHeight),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                    y += slatSpacing
                }
            }
            .border(2.dp, BrassGold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
    )
}

@Composable
fun SignalWaveDisplay(isPlaying: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            val barCount = 9
            repeat(barCount) { i ->
                val pulseAnim = rememberInfiniteTransition()
                val barHeight by pulseAnim.animateFloat(
                    initialValue = if (isPlaying) 6f else 3f,
                    targetValue = if (isPlaying) 44f else 3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = if (isPlaying) 400 + (i * 80) else 1000,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val barColor = when {
                    i < 3 -> DialGreen
                    i < 6 -> AmberGlow
                    else -> TubeOrange
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(4.dp)
                        .height(barHeight.dp)
                        .background(
                            if (isPlaying) barColor else barColor.copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun RadioKnob(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4A3020),
                        Color(0xFF2A1508),
                        Color(0xFF1A0A02)
                    )
                ),
                CircleShape
            )
            .border(
                3.dp,
                Brush.sweepGradient(
                    colors = listOf(
                        BrassLight,
                        BrassGold.copy(alpha = 0.6f),
                        BrassLight,
                        BrassGold.copy(alpha = 0.4f),
                        BrassLight
                    )
                ),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cx = size.width / 2f
                    val notchRad = Math.toRadians((rotation - 90.0))
                    val innerR = size.width * 0.25f
                    val outerR = size.width * 0.42f
                    drawLine(
                        color = BrassLight,
                        start = Offset(
                            cx + (innerR * Math.cos(notchRad)).toFloat(),
                            cx + (innerR * Math.sin(notchRad)).toFloat()
                        ),
                        end = Offset(
                            cx + (outerR * Math.cos(notchRad)).toFloat(),
                            cx + (outerR * Math.sin(notchRad)).toFloat()
                        ),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
        )
        content()
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
    val isPlayingMode = playbackState is RadioPlaybackState.Playing

    val infiniteTransition = rememberInfiniteTransition()
    val pilotGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0604)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A0F07),
                                Color(0xFF0D0704)
                            )
                        )
                    )
                    .safeDrawingPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4A2C1A),
                                    Color(0xFF3A2010),
                                    Color(0xFF2C1810),
                                    Color(0xFF3A2010),
                                    Color(0xFF4A2C1A)
                                )
                            )
                        )
                        .border(
                            3.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    BrassGold.copy(alpha = 0.5f),
                                    BrassGold.copy(alpha = 0.2f),
                                    BrassGold.copy(alpha = 0.5f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Затвори",
                                tint = FadedLabel,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFlorist,
                                contentDescription = null,
                                tint = BulgariaRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "РАДИО БЪЛГАРИЯ",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrassGold,
                                letterSpacing = 3.sp
                            )
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Любими",
                                tint = if (isFav) AmberBright else FadedLabel,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SpeakerGrille(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(WalnutLight, VinylBlack)
                                )
                            )
                            .border(2.dp, BrassGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (station.favicon.isNotBlank()) {
                            AsyncImage(
                                model = station.favicon,
                                contentDescription = "Лого на станция",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = AmberGlow.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DialFaceGreen.copy(alpha = 0.9f),
                                        DialFaceLight,
                                        DialFaceGreen.copy(alpha = 0.9f)
                                    )
                                )
                            )
                            .border(2.dp, BrassGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .drawBehind {
                                val tickCount = 20
                                val startX = 16.dp.toPx()
                                val endX = size.width - 16.dp.toPx()
                                val tickY = size.height * 0.2f
                                val tickWidth = (endX - startX) / tickCount

                                for (i in 0..tickCount) {
                                    val x = startX + (i * tickWidth)
                                    val isMajor = i % 5 == 0
                                    drawLine(
                                        color = AmberGlow.copy(alpha = if (isMajor) 0.6f else 0.3f),
                                        start = Offset(x, tickY),
                                        end = Offset(x, tickY + if (isMajor) 12.dp.toPx() else 6.dp.toPx()),
                                        strokeWidth = if (isMajor) 1.5f else 1f
                                    )
                                }

                                val needleX = size.width * 0.5f
                                drawLine(
                                    color = NeedleRed,
                                    start = Offset(needleX, tickY - 4.dp.toPx()),
                                    end = Offset(needleX, size.height * 0.85f),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 20.dp)
                        ) {
                            Text(
                                text = station.name,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = AmberBright,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = station.language.ifEmpty { "България" },
                                fontFamily = FontFamily.Serif,
                                fontSize = 12.sp,
                                color = CreamWhite.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (isPlayingMode)
                                            listOf(
                                                PilotLampGreen.copy(alpha = pilotGlow),
                                                PilotLampGreen.copy(alpha = 0.4f),
                                                Color.Transparent
                                            )
                                        else
                                            listOf(PilotLampOff, PilotLampOff.copy(alpha = 0.5f), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                                .border(1.dp, BrassGold.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (playbackState) {
                                is RadioPlaybackState.Playing -> "В ЕФИР"
                                is RadioPlaybackState.Buffering -> "НАСТРОЙКА..."
                                is RadioPlaybackState.Paused -> "ПАУЗА"
                                is RadioPlaybackState.Error -> "НЯМА СИГНАЛ"
                                else -> "ГОТОВО"
                            },
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isPlayingMode) PilotLampGreen else FadedLabel,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(VinylBlack)
                            .border(1.dp, BrassGold.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        SignalWaveDisplay(
                            isPlaying = isPlayingMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (station.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val cleanTags = station.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            cleanTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .background(BrassGold.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                                        .border(1.dp, BrassGold.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        tag.uppercase(),
                                        fontSize = 9.sp,
                                        color = BrassGold,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RadioKnob(
                                modifier = Modifier.size(56.dp),
                                rotation = scaleVolume * 270f,
                                onClick = {}
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "ЗВУК",
                                fontFamily = FontFamily.Serif,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrassGold.copy(alpha = 0.7f),
                                letterSpacing = 1.5.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RadioKnob(
                                modifier = Modifier.size(72.dp),
                                rotation = 0f,
                                onClick = onTogglePlayPause
                            ) {
                                when (playbackState) {
                                    is RadioPlaybackState.Buffering -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = AmberGlow,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    is RadioPlaybackState.Playing -> {
                                        Icon(Icons.Default.Pause, contentDescription = "Пауза", tint = AmberGlow, modifier = Modifier.size(28.dp))
                                    }
                                    else -> {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Пусни", tint = AmberGlow, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isPlayingMode) "ПАУЗА" else "ПУСНИ",
                                fontFamily = FontFamily.Serif,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrassGold.copy(alpha = 0.7f),
                                letterSpacing = 1.5.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RadioKnob(
                                modifier = Modifier.size(56.dp),
                                rotation = 135f,
                                onClick = onToggleFavorite
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Любими",
                                    tint = if (isFav) AmberBright else FadedLabel,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "ЛЮБИМИ",
                                fontFamily = FontFamily.Serif,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrassGold.copy(alpha = 0.7f),
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeMute, contentDescription = null, tint = FadedLabel, modifier = Modifier.size(16.dp))
                        Slider(
                            value = scaleVolume,
                            onValueChange = {
                                scaleVolume = it
                                onVolumeChange(it)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = BrassGold,
                                activeTrackColor = AmberGlow,
                                inactiveTrackColor = WalnutDark
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
