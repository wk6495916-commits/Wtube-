package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.net.Uri
import com.example.data.CommentEntity
import com.example.data.VideoEntity
import com.example.ui.components.WtubeVideoPlayer
import com.example.ui.components.VideoFilterOverlay
import com.example.ui.components.formatSocialCount
import com.example.ui.theme.*
import com.example.viewmodel.WtubeTab
import com.example.viewmodel.WtubeViewModel
import kotlinx.coroutines.launch

// 1. THE MAIN FEED SCREEN (TikTok vertical swipe)
@Composable
fun FeedScreen(
    viewModel: WtubeViewModel,
    videos: List<VideoEntity>,
    onCreatorClick: (String) -> Unit,
    onCommentClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = TikTokNeonPink)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Prepopulating stunning Wtube reels...", color = Color.White)
            }
        }
        return
    }

    // Horizontal category selection overlay at top ("Following" vs "For You")
    var selectedFeedCategory by remember { mutableStateOf("For You") }
    var isAutoScrollEnabled by remember { mutableStateOf(false) }

    // Standard Jetpack Compose Vertical Pager
    val pagerState = rememberPagerState(pageCount = { videos.size })

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            // Make sure only the active visible index plays audio and loops!
            val isActive = pageIndex == pagerState.currentPage
            val video = videos[pageIndex]

            WtubeVideoPlayer(
                video = video,
                isActive = isActive,
                isAutoScrollEnabled = isAutoScrollEnabled,
                onVideoCompleted = {
                    coroutineScope.launch {
                        val nextIndex = pagerState.currentPage + 1
                        if (nextIndex < videos.size) {
                            try {
                                pagerState.animateScrollToPage(nextIndex)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                onNextScrollClick = {
                    coroutineScope.launch {
                        val nextIndex = pagerState.currentPage + 1
                        if (nextIndex < videos.size) {
                            try {
                                pagerState.animateScrollToPage(nextIndex)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                onLikeToggle = { viewModel.toggleLike(video) },
                onFollowToggle = { viewModel.toggleFollow(video) },
                onCommentClick = { onCommentClick(video.id) },
                onBookmarkToggle = { viewModel.toggleBookmark(video) },
                onShareClick = {
                    viewModel.showShareFor(video)
                },
                onCreatorClick = onCreatorClick
            )
        }

        // Header Category Overlay (Following | For You)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Following",
                color = if (selectedFeedCategory == "Following") Color.White else Color.White.copy(alpha = 0.5f),
                fontWeight = if (selectedFeedCategory == "Following") FontWeight.Bold else FontWeight.Normal,
                fontSize = 17.sp,
                modifier = Modifier
                    .clickable { selectedFeedCategory = "Following" }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Text(
                text = "For You",
                color = if (selectedFeedCategory == "For You") Color.White else Color.White.copy(alpha = 0.5f),
                fontWeight = if (selectedFeedCategory == "For You") FontWeight.Bold else FontWeight.Normal,
                fontSize = 17.sp,
                modifier = Modifier
                    .clickable { selectedFeedCategory = "For You" }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Interactive Auto-Scroll Mode floating pill (Aesthetics)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isAutoScrollEnabled) TikTokNeonPink.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = if (isAutoScrollEnabled) TikTokNeonPink else Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { isAutoScrollEnabled = !isAutoScrollEnabled }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("auto_scroll_toggle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Autorenew,
                    contentDescription = "Auto Scroll",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isAutoScrollEnabled) "Auto ON" else "Auto OFF",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 2. DISCOVER SCREEN (Hashtags, Search grid, Interactive searches)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: WtubeViewModel,
    modifier: Modifier = Modifier,
    onVideoSelect: (VideoEntity) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedSearchTab by remember { mutableStateOf("All") }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var voiceSimulatedText by remember { mutableStateOf("Listening...") }

    // Trending static list helper
    val trendingHashtags = listOf("#cyberpunk", "#dance", "#travel", "#music", "#tech")

    // Dynamic creator lookup based on all videos database
    val matchedCreators = remember(allVideos, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            allVideos
                .filter {
                    it.creatorHandle.contains(searchQuery, ignoreCase = true) ||
                    it.creatorName.contains(searchQuery, ignoreCase = true)
                }
                .distinctBy { it.creatorHandle }
        }
    }

    // Dynamic hashtag extraction matched with search query
    val matchedHashtags = remember(allVideos, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            allVideos
                .flatMap { it.tags.split(Regex("[\\s,#]+")) }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { "#$it" }
                .filter { it.contains(searchQuery.replace("#", ""), ignoreCase = true) }
                .distinct()
        }
    }

    // Simulated Speech Recognition Voice Search Logic
    if (showVoiceDialog) {
        LaunchedEffect(Unit) {
            voiceSimulatedText = "Listening..."
            kotlinx.coroutines.delay(1500)
            val sampleVoiceQueries = listOf("cyberpunk", "travel", "alex dancer", "tutorial")
            val picked = sampleVoiceQueries.random()
            voiceSimulatedText = "Heard: \"$picked\""
            kotlinx.coroutines.delay(800)
            viewModel.updateSearchQuery(picked)
            showVoiceDialog = false
        }

        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Cancel", color = TikTokNeonPink)
                }
            },
            containerColor = TikTokCardBackground,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Microphone",
                        tint = TikTokNeonPink,
                        modifier = Modifier.size(28.dp)
                    )
                    Text("Voice Search", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cute animated soundwave bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(40.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "soundwave")
                        val h1Float by infiniteTransition.animateFloat(
                            initialValue = 10f,
                            targetValue = 35f,
                            animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                            label = "h1"
                        )
                        val h2Float by infiniteTransition.animateFloat(
                            initialValue = 25f,
                            targetValue = 10f,
                            animationSpec = infiniteRepeatable(animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                            label = "h2"
                        )
                        val h3Float by infiniteTransition.animateFloat(
                            initialValue = 12f,
                            targetValue = 38f,
                            animationSpec = infiniteRepeatable(animation = tween(350, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                            label = "h3"
                        )
                        val h4Float by infiniteTransition.animateFloat(
                            initialValue = 30f,
                            targetValue = 15f,
                            animationSpec = infiniteRepeatable(animation = tween(450, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                            label = "h4"
                        )
                        val h5Float by infiniteTransition.animateFloat(
                            initialValue = 15f,
                            targetValue = 32f,
                            animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                            label = "h5"
                        )

                        val h1 = h1Float.dp
                        val h2 = h2Float.dp
                        val h3 = h3Float.dp
                        val h4 = h4Float.dp
                        val h5 = h5Float.dp

                        Box(modifier = Modifier.width(6.dp).height(h1).clip(RoundedCornerShape(3.dp)).background(TikTokCyan))
                        Box(modifier = Modifier.width(6.dp).height(h2).clip(RoundedCornerShape(3.dp)).background(TikTokCyan))
                        Box(modifier = Modifier.width(6.dp).height(h3).clip(RoundedCornerShape(3.dp)).background(TikTokCyan))
                        Box(modifier = Modifier.width(6.dp).height(h4).clip(RoundedCornerShape(3.dp)).background(TikTokCyan))
                        Box(modifier = Modifier.width(6.dp).height(h5).clip(RoundedCornerShape(3.dp)).background(TikTokCyan))
                    }

                    Text(
                        text = voiceSimulatedText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Say something like \"cyberpunk\" or \"travel\"...",
                        color = TikTokSubText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TikTokDarkBackground)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Futuristic search head with Voice Search Integrated
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search creators, sounds, or hashtags...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TikTokNeonPink) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { showVoiceDialog = true }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice Search", tint = TikTokCyan)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = TikTokCyan,
                unfocusedBorderColor = TikTokGray,
                focusedContainerColor = TikTokCardBackground,
                unfocusedContainerColor = TikTokCardBackground
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().testTag("search_input")
        )

        if (searchQuery.isBlank()) {
            // Recent Searches (History) Section
            if (recentSearches.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clear All",
                            color = TikTokNeonPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.clearRecentSearches() }
                                .padding(4.dp)
                                .testTag("clear_recent_searches")
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentSearches) { search ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(TikTokGray)
                                    .clickable { viewModel.updateSearchQuery(search) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = search,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.removeRecentSearch(search) }
                                )
                            }
                        }
                    }
                }
            }

            // Display standard trending tags list with scrollable previews
            Text(
                text = "Trending Hashtags",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(trendingHashtags) { hashtag ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clickable { viewModel.updateSearchQuery(hashtag) }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(TikTokNeonPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                             ) {
                                Text("#", color = TikTokNeonPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text(hashtag, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Popular soundtrack & creative reels", color = TikTokSubText, fontSize = 12.sp)
                            }
                        }

                        // Horizontal clip list related to hashtag
                        val playlist = searchResults.filter { it.tags.contains(hashtag, ignoreCase = true) }
                        if (playlist.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Fallback empty tags preview container
                                repeat(3) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TikTokGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.VideoFile, contentDescription = "Video", tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                playlist.forEach { video ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TikTokCardBackground)
                                            .clickable { onVideoSelect(video) }
                                    ) {
                                        AsyncImage(
                                            model = video.creatorAvatar,
                                            contentDescription = "Thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                    )
                                                )
                                        )
                                        Text(
                                            text = formatSocialCount(video.likesCount) + " likes",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Searched results activated with Category Filtering Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val searchTabs = listOf("All", "Videos", "Creators", "Hashtags")
                searchTabs.forEach { tabName ->
                    val isSelected = selectedSearchTab == tabName
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) TikTokCyan else TikTokCardBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) TikTokCyan else TikTokGray,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedSearchTab = tabName }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tabName,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main result content according to selected category tab
            when (selectedSearchTab) {
                "All" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. MATCHED CREATORS SUBSECTION
                        if (matchedCreators.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Creators",
                                        color = Color.LightGray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    matchedCreators.take(3).forEach { video ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(TikTokCardBackground)
                                                .clickable {
                                                    viewModel.selectCreator(video.creatorHandle)
                                                    viewModel.selectTab(WtubeTab.PROFILE)
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = video.creatorAvatar,
                                                contentDescription = "Avatar",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = video.creatorName,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = video.creatorHandle,
                                                    color = TikTokCyan,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.selectCreator(video.creatorHandle)
                                                    viewModel.selectTab(WtubeTab.PROFILE)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = TikTokNeonPink),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Profile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. MATCHED HASHTAGS SUBSECTION
                        if (matchedHashtags.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Related Tags",
                                        color = Color.LightGray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(matchedHashtags) { hashtag ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(TikTokNeonPink.copy(alpha = 0.15f))
                                                    .clickable { viewModel.updateSearchQuery(hashtag) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(text = hashtag, color = TikTokNeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. VIDEOS HEADER
                        item {
                            Text(
                                text = "Videos for \"$searchQuery\"",
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 4. MATCHED VIDEOS GRID IN ALL TAB
                        if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No matching videos found.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            gridItems(searchResults, 3) { video ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TikTokCardBackground)
                                        .clickable { onVideoSelect(video) }
                                ) {
                                    AsyncImage(
                                        model = video.creatorAvatar,
                                        contentDescription = "Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = video.creatorHandle,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatSocialCount(video.likesCount) + " ♥",
                                            color = TikTokNeonPink,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Videos" -> {
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.SentimentDissatisfied, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(50.dp))
                                Text("No matching clips found", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(searchResults) { video ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TikTokCardBackground)
                                        .clickable { onVideoSelect(video) }
                                ) {
                                    AsyncImage(
                                        model = video.creatorAvatar,
                                        contentDescription = "Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = video.creatorHandle,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatSocialCount(video.likesCount) + " ♥",
                                            color = TikTokNeonPink,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Creators" -> {
                    if (matchedCreators.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Person, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(50.dp))
                                Text("No creators matching \"$searchQuery\"", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchedCreators) { video ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TikTokCardBackground)
                                        .clickable {
                                            viewModel.selectCreator(video.creatorHandle)
                                            viewModel.selectTab(WtubeTab.PROFILE)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    AsyncImage(
                                        model = video.creatorAvatar,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.creatorName,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = video.creatorHandle,
                                            color = TikTokCyan,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${formatSocialCount(video.likesCount * 3 + 1500)} followers • Popular Reel Creator",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.selectCreator(video.creatorHandle)
                                            viewModel.selectTab(WtubeTab.PROFILE)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TikTokNeonPink),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text("View", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                "Hashtags" -> {
                    if (matchedHashtags.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Tag, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(50.dp))
                                Text("No matching tags found", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchedHashtags) { hashtag ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TikTokCardBackground)
                                        .clickable { viewModel.updateSearchQuery(hashtag) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(TikTokNeonPink.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("#", color = TikTokNeonPink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(hashtag, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Explore creative recordings & sound clips", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Help divide elements for 3-column layouts inside classic lazy lists safely!
fun <T> LazyListScope.gridItems(
    items: List<T>,
    columnCount: Int,
    itemContent: @Composable (T) -> Unit
) {
    val rows = items.chunked(columnCount)
    rows.forEach { row ->
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(item)
                    }
                }
                // Fill up empty space in the last row to maintain grid sizes
                val emptySlots = columnCount - row.size
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

enum class VideoFilter(val displayName: String) {
    NORMAL("Normal"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    VIBRANT("Vibrant")
}

// 3. UPLOAD SCREEN (Adding a custom mp4 video or Preset choice)
@Composable
fun UploadScreen(
    viewModel: WtubeViewModel,
    modifier: Modifier = Modifier
) {
    var videoUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var musicName by remember { mutableStateOf("") }
    var musicArtist by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("#dance") }
    var selectedFilter by remember { mutableStateOf(VideoFilter.NORMAL) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploadingFromGallery by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingFromGallery = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = "wtube_gallery_upload_${System.currentTimeMillis()}.mp4"
                        val destFile = File(context.filesDir, fileName)
                        destFile.outputStream().use { outputStream ->
                            inputStream.use { it.copyTo(outputStream) }
                        }
                        withContext(Dispatchers.Main) {
                            videoUrl = Uri.fromFile(destFile).toString()
                            if (description.isBlank()) {
                                description = "Freshly uploaded custom video from gallery! 📲✨"
                            }
                            isUploadingFromGallery = false
                            android.widget.Toast.makeText(context, "Loaded video from gallery successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isUploadingFromGallery = false
                            android.widget.Toast.makeText(context, "Could not load selected video data.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isUploadingFromGallery = false
                        android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Preset MP4 links list for speed-testing the player!
    val presets = listOf(
        Pair("Alpine Winter Cabin Switzerland", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
        Pair("Cyberpunk Tokyo Lights Drive", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
        Pair("Forest Sunset Camp Experience", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TikTokDarkBackground)
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Wtube Reel",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Upload online MP4 short video link to instantly add to the vertical swiping feed. Persistent inside local DB!",
            color = TikTokSubText,
            fontSize = 13.sp
        )

        // Preset helpers
        Card(
            colors = CardDefaults.cardColors(containerColor = TikTokCardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Sample Video Template", color = TikTokCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                presets.forEach { (label, url) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (videoUrl == url) TikTokGray else Color.Transparent)
                            .clickable {
                                videoUrl = url
                                if (description.isBlank()) description = "Watching this spectacular $label! 🎬🌟"
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = Color.White, fontSize = 12.sp)
                        Icon(
                            imageVector = if (videoUrl == url) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = "Select",
                            tint = if (videoUrl == url) TikTokNeonPink else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ----------------- GALLERY VIDEO PICKER -----------------
        Card(
            colors = CardDefaults.cardColors(containerColor = TikTokCardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Upload via Local Gallery",
                    color = TikTokCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    text = "Select any .mp4 file directly from your Android device gallery. It is automatically imported into local storage for seamless caching and persistent playback!",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                if (videoUrl.startsWith("file:///")) {
                    // Show loaded video success info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1100F2FE))
                            .border(1.dp, TikTokCyan, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TikTokCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Success",
                                    tint = TikTokCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ready to Publish!",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Video successfully cached in internal storage.",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        galleryLauncher.launch("video/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (videoUrl.startsWith("file:///")) TikTokCyan else TikTokNeonPink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gallery_upload_btn")
                ) {
                    if (isUploadingFromGallery) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VideoLibrary,
                                contentDescription = "Gallery Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (videoUrl.startsWith("file:///")) "Change Gallery Video" else "Pick Video from Gallery",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // ----------------- VIDEO PREVIEW CONTAINER -----------------
        Text("Video Preview", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TikTokCardBackground)
                .border(1.dp, TikTokGray, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (videoUrl.isNotBlank()) {
                // Real Live Video Preview playing the current URL!
                var isPreparing by remember(videoUrl) { mutableStateOf(true) }
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView<VideoView>(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                tag = null
                            }
                        },
                        update = { view ->
                            val currentTag = view.tag as? String
                            if (currentTag != videoUrl) {
                                view.tag = videoUrl
                                isPreparing = true
                                try {
                                    view.setVideoURI(Uri.parse(videoUrl))
                                    view.setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        mp.setVolume(0f, 0f)
                                        isPreparing = false
                                        try {
                                            view.start()
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        }
                                    }
                                    view.setOnErrorListener { _, _, _ ->
                                        isPreparing = false
                                        true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onRelease = { view ->
                            try {
                                view.setOnPreparedListener(null)
                                view.setOnErrorListener(null)
                                view.setOnCompletionListener(null)
                                view.stopPlayback()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Apply Selected Filter Overlay on Top
                    VideoFilterOverlay(filterName = selectedFilter.displayName)

                    if (isPreparing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = TikTokNeonPink,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Watermark / Indicator badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Live Preview", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Placeholder UX
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = "Waiting for video",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Select a template above to load visual preview",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ----------------- FILTER SELECTOR -----------------
        Text("Apply Aesthetic Filter", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) TikTokNeonPink.copy(alpha = 0.15f) else TikTokCardBackground)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TikTokNeonPink else TikTokGray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .testTag("filter_chip_${filter.displayName.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tiny circle showing filter type preview
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when (filter) {
                                        VideoFilter.NORMAL -> Color.White
                                        VideoFilter.GRAYSCALE -> Color(0xFF888888)
                                        VideoFilter.SEPIA -> Color(0xFFB58A3F)
                                        VideoFilter.VIBRANT -> Color(0xFFFF007F)
                                    }
                                )
                        )
                        Text(
                            text = filter.displayName,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // URL Field
        Text("Active Video Link (.mp4)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = videoUrl,
            onValueChange = { videoUrl = it },
            placeholder = { Text("https://example.com/stream.mp4", color = Color.Gray) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = TikTokNeonPink,
                unfocusedBorderColor = TikTokGray,
                focusedContainerColor = TikTokCardBackground,
                unfocusedContainerColor = TikTokCardBackground
            ),
            modifier = Modifier.fillMaxWidth().testTag("upload_url_input")
        )

        // Description Field
        Text("Caption / Description", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Write something catchy...", color = Color.Gray) },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = TikTokNeonPink,
                unfocusedBorderColor = TikTokGray,
                focusedContainerColor = TikTokCardBackground,
                unfocusedContainerColor = TikTokCardBackground
            ),
            modifier = Modifier.fillMaxWidth().testTag("upload_desc_input")
        )

        // Music Fields
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Soundtrack Handle", color = Color.White, fontSize = 13.sp)
                OutlinedTextField(
                    value = musicName,
                    onValueChange = { musicName = it },
                    placeholder = { Text("Beat Track", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokNeonPink,
                        focusedContainerColor = TikTokCardBackground
                    )
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Music Artist", color = Color.White, fontSize = 13.sp)
                OutlinedTextField(
                    value = musicArtist,
                    onValueChange = { musicArtist = it },
                    placeholder = { Text("Independent DJ", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokNeonPink,
                        focusedContainerColor = TikTokCardBackground
                    )
                )
            }
        }

        // Hashtags Field
        Text("Hashtags", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            placeholder = { Text("#wtube #foryou #fyp", color = Color.Gray) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = TikTokNeonPink,
                unfocusedContainerColor = TikTokCardBackground,
                unfocusedBorderColor = TikTokGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Submit button
        Button(
            onClick = {
                if (videoUrl.isBlank()) {
                    android.widget.Toast.makeText(context, "Please configure or select a Video Link!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.createNewVideo(
                        videoUrl = videoUrl,
                        description = description.ifBlank { "Exciting shorts trending now! 🔥" },
                        musicName = musicName.ifBlank { "Original Audio Core" },
                        musicArtist = musicArtist.ifBlank { "Aquib" },
                        tags = tags,
                        filterApplied = selectedFilter.displayName
                    )
                    android.widget.Toast.makeText(context, "Video added to active feed successfully! 🎉 Swipe to watch!", android.widget.Toast.LENGTH_LONG).show()
                    viewModel.selectTab(WtubeTab.HOME)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TikTokNeonPink),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("upload_submit_btn")
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = "Add", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish to Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// 4. INBOX SCREEN (Activity feeds & messages)
@Composable
fun InboxScreen(
    modifier: Modifier = Modifier
) {
    val activities = listOf(
        Triple("Chloe Digital liked your post.", "12m ago", Icons.Filled.Favorite),
        Triple("Sofia Wanderlust started following you.", "1h ago", Icons.Filled.PersonAdd),
        Triple("Tyler Chase commented on your Switzerland vlog.", "3h ago", Icons.Filled.Comment),
        Triple("Support Wtube team pinned your creative track.", "1d ago", Icons.Filled.PushPin),
        Triple("Dan Tech sent you a direct sound link.", "2d ago", Icons.Filled.Send)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TikTokDarkBackground)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All Activity",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Message, contentDescription = "DMs", tint = TikTokCyan)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Activities List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(activities) { (text, time, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TikTokCardBackground)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TikTokGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = "Icon", tint = TikTokNeonPink, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = time, color = TikTokSubText, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokGray),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reply", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 5. USER PROFILE SCREEN (Stats, personal bios, created videos, liked videos galleries)
@Composable
fun ProfileScreen(
    viewModel: WtubeViewModel,
    creatorHandle: String?, // Null indicates ME, otherwise views specific creator
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onVideoSelect: (VideoEntity) -> Unit
) {
    val myProfile by viewModel.myProfile.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val likedVideos by viewModel.likedVideos.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (creatorHandle == null && !isLoggedIn) {
        AuthScreen(viewModel = viewModel, modifier = modifier)
        return
    }

    var showEditDialog by remember { mutableStateOf(false) }

    val isMe = creatorHandle == null || (myProfile != null && creatorHandle == myProfile?.username)

    // Pull creator details
    val creatorProfile = if (isMe) {
        myProfile
    } else {
        // Collect credentials from matching video creator
        allVideos.find { it.creatorHandle == creatorHandle }?.let {
            com.example.data.UserProfileEntity(
                username = it.creatorHandle,
                displayName = it.creatorName,
                avatarUrl = it.creatorAvatar,
                bio = "Co-creator checking Wtube! Let's build a stunning vertical landscape of loop loops.",
                followersCount = 189000,
                followingCount = 422,
                totalLikes = 899000
            )
        }
    }

    // Tab categories inside profiles (My reels vs Liked reels)
    var selectedProfileTab by remember { mutableStateOf(0) }

    if (creatorProfile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TikTokNeonPink)
        }
        return
    }

    // Filter relevant galleries
    val myCreatedVideos = allVideos.filter { it.creatorHandle == creatorProfile.username }
    val displayGalleries = if (selectedProfileTab == 0) {
        myCreatedVideos
    } else {
        if (isMe) likedVideos else allVideos.filter { it.isLiked && it.creatorHandle == creatorProfile.username }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxSize()
            .background(TikTokDarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Top App bar (Spans all 3 columns)
        item(span = { GridItemSpan(3) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Text(
                    text = creatorProfile.username,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                var showSettingsMenu by remember { mutableStateOf(false) }

                Box {
                    IconButton(onClick = { if (isMe) showSettingsMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Actions", tint = Color.White)
                    }
                    if (isMe) {
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false },
                            modifier = Modifier.background(TikTokCardBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Log Out 🚪", color = Color.White, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showSettingsMenu = false
                                    viewModel.logout()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Profile details, controls, bio, and tabs (Spans all 3 columns)
        item(span = { GridItemSpan(3) }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                // Profile image
                AsyncImage(
                    model = creatorProfile.avatarUrl,
                    contentDescription = "Profile Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, TikTokNeonPink, CircleShape)
                        .background(TikTokGray)
                )

                // Display Name
                Text(
                    text = creatorProfile.displayName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Statistics Row ( Following | Followers | Likes )
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStatColumn(count = formatSocialCount(creatorProfile.followingCount), label = "Following")
                    ProfileStatColumn(count = formatSocialCount(creatorProfile.followersCount), label = "Followers")
                    ProfileStatColumn(count = formatSocialCount(creatorProfile.totalLikes), label = "Likes")
                }

                // Edit Profile / Message / Follow controls
                if (isMe) {
                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokGray),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(180.dp).testTag("edit_profile_btn")
                    ) {
                        Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = TikTokNeonPink),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Follow", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = TikTokGray),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Message", color = Color.White)
                        }
                    }
                }

                // Bio
                Text(
                    text = creatorProfile.bio,
                    color = TikTokWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tab selects
                TabRow(
                    selectedTabIndex = selectedProfileTab,
                    containerColor = Color.Transparent,
                    contentColor = TikTokNeonPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedProfileTab]),
                            color = TikTokNeonPink,
                            height = 2.5.dp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedProfileTab == 0,
                        onClick = { selectedProfileTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.GridOn, contentDescription = "My Reels", tint = if (selectedProfileTab == 0) TikTokNeonPink else Color.Gray, modifier = Modifier.size(16.dp))
                                Text("Reels", color = if (selectedProfileTab == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedProfileTab == 1,
                        onClick = { selectedProfileTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Liked Reels", tint = if (selectedProfileTab == 1) TikTokNeonPink else Color.Gray, modifier = Modifier.size(16.dp))
                                Text("Liked", color = if (selectedProfileTab == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }

        // 3. Gallery Grid items (or Empty placeholder - Spans all columns)
        if (displayGalleries.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (selectedProfileTab == 0) Icons.Filled.VideoCameraBack else Icons.Filled.FavoriteBorder,
                            contentDescription = "Empty",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = if (selectedProfileTab == 0) "No uploads yet" else "No liked reels yet",
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(displayGalleries) { video ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TikTokCardBackground)
                        .clickable { onVideoSelect(video) }
                ) {
                    AsyncImage(
                        model = video.creatorAvatar,
                        contentDescription = "Clip preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Likes overlay at lower-left
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Views", tint = Color.White, modifier = Modifier.size(10.dp))
                        Text(
                            text = formatSocialCount(video.likesCount),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal Edit Profile bio Dialog
    if (showEditDialog && isMe) {
        var tempName by remember { mutableStateOf(creatorProfile.displayName) }
        var tempBio by remember { mutableStateOf(creatorProfile.bio) }
        var tempAvatar by remember { mutableStateOf(creatorProfile.avatarUrl) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile Details", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            containerColor = TikTokCardBackground,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display Name", color = TikTokCyan) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true,
                        modifier = Modifier.testTag("name_edit_input")
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Custom Bio", color = TikTokCyan) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        maxLines = 3,
                        modifier = Modifier.testTag("bio_edit_input")
                    )
                    OutlinedTextField(
                        value = tempAvatar,
                        onValueChange = { tempAvatar = it },
                        label = { Text("Avatar Pic URL", color = TikTokCyan) },
                        placeholder = { Text("https://example.com/avatar.jpg") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMyProfile(tempName, tempBio, tempAvatar)
                        showEditDialog = false
                    },
                    modifier = Modifier.testTag("confirm_profile_edit")
                ) {
                    Text("Save Changes", color = TikTokNeonPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ProfileStatColumn(count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = count, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TikTokSubText, fontSize = 12.sp)
    }
}

// 6. COMMENTS BOTTOM DRAWER OVERLAY (Beautiful, highly usable slide-up comment sections)
@Composable
fun CommentsBottomSheet(
    comments: List<CommentEntity>,
    onAddComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f) // Opens up to cover 65% of screen beautifully
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(TikTokCardBackground)
            .padding(16.dp)
            .testTag("comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Comment count and close handle
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${comments.size} comments",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(bottom = 8.dp)
                )
            }

            // Scrollable List of comments
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Be the first to say something! 💬🚀", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(comments) { comment ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = comment.authorAvatar,
                                contentDescription = "Commenter Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TikTokGray)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = comment.authorName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = comment.authorHandle,
                                        color = TikTokSubText,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Heart", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Footer Input Bar to write comments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Add comment...", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokCyan,
                        unfocusedBorderColor = TikTokGray,
                        focusedContainerColor = TikTokGray,
                        unfocusedContainerColor = TikTokGray
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).testTag("comment_input_text")
                )

                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddComment(newCommentText)
                            newCommentText = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TikTokNeonPink)
                        .testTag("submit_comment_btn")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// 7. SHARING BOTTOM DRAWER OVERLAY (With beautiful high fidelity animations)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShareBottomSheet(
    video: VideoEntity,
    onShareDestClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Interactive Animation States
    var copiedCheckmarkState by remember { mutableStateOf(false) }
    var repostStatusState by remember { mutableStateOf(false) }
    var savingProgressState by remember { mutableStateOf<Float?>(null) } // null = idle, 0.0f to 1.0f = progress
    var showWhatsAppPulse by remember { mutableStateOf(false) }
    var showInstagramPulse by remember { mutableStateOf(false) }
    var showDuetSplitCover by remember { mutableStateOf(false) }

    // Sent-to friends state
    val friends = listOf(
        Pair("@alex_dancer", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&fit=crop"),
        Pair("@sara_cooks", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&fit=crop"),
        Pair("@tech_guru", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&fit=crop"),
        Pair("@comedy_king", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&fit=crop")
    )
    val sentFriends = remember { mutableStateListOf<String>() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(TikTokCardBackground)
            .padding(16.dp)
            .testTag("share_bottom_sheet")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag handle or top indicator
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share with",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Share", tint = Color.White)
                }
            }

            // SECTION 1: Send to Friends (TikTok Quick-Share Icons)
            Text(
                text = "Send to Friends",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                friends.forEach { friend ->
                    val isSent = sentFriends.contains(friend.first)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                if (!isSent) {
                                    sentFriends.add(friend.first)
                                    android.widget.Toast.makeText(context, "Sent video to ${friend.first}! ✈️", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("friend_share_${friend.first.removePrefix("@")}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(54.dp)
                        ) {
                            AsyncImage(
                                model = friend.second,
                                contentDescription = friend.first,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(TikTokGray)
                            )
                            if (isSent) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(TikTokCyan.copy(alpha = 0.75f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Sent",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSent) "Sent" else friend.first,
                            color = if (isSent) TikTokCyan else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            // SECTION 2: Sharing Channels Row
            Text(
                text = "Channels",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Feature 1: Copy Link (Beautiful local animated copy feedback)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            copiedCheckmarkState = true
                            clipboardManager.setText(AnnotatedString(video.videoUrl))
                            onShareDestClick("Copy Link")
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1800)
                                copiedCheckmarkState = false
                            }
                        }
                        .weight(1f)
                        .testTag("share_channel_copy_link")
                ) {
                    val scaleFactor by animateFloatAsState(
                        targetValue = if (copiedCheckmarkState) 1.25f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .scale(scaleFactor)
                            .clip(CircleShape)
                            .background(if (copiedCheckmarkState) Color(0xFF4CAF50) else Color(0x1FADF5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (copiedCheckmarkState) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Copied",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = "Copy Link",
                                tint = TikTokCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (copiedCheckmarkState) "Copied!" else "Copy Link",
                        color = if (copiedCheckmarkState) Color(0xFF4CAF50) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Feature 2: Repost (Increments Share count, changes status dynamically)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (!repostStatusState) {
                                repostStatusState = true
                                onShareDestClick("Repost")
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    repostStatusState = false
                                }
                            }
                        }
                        .weight(1f)
                        .testTag("share_channel_repost")
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (repostStatusState) TikTokNeonPink.copy(alpha = 0.3f) else Color(0x1FFF6188)),
                        contentAlignment = Alignment.Center
                    ) {
                        val rotationAnim by animateFloatAsState(
                            targetValue = if (repostStatusState) 360f else 0f,
                            animationSpec = tween(1200)
                        )
                        Icon(
                            imageVector = Icons.Filled.Autorenew,
                            contentDescription = "Repost",
                            tint = TikTokNeonPink,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotationAnim)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (repostStatusState) "Reposted!" else "Repost",
                        color = if (repostStatusState) TikTokNeonPink else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Feature 3: WhatsApp Simulated Share (Circular pulse zoom)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            showWhatsAppPulse = true
                            onShareDestClick("WhatsApp")
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000)
                                android.widget.Toast.makeText(context, "Opened WhatsApp! Sharing video...", android.widget.Toast.LENGTH_SHORT).show()
                                showWhatsAppPulse = false
                            }
                        }
                        .weight(1f)
                        .testTag("share_channel_whatsapp")
                ) {
                    val whatsappScale by animateFloatAsState(
                        targetValue = if (showWhatsAppPulse) 1.25f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .scale(whatsappScale)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "WhatsApp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                // Feature 4: Instagram Simulated Share (Circular pulse zoom)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            showInstagramPulse = true
                            onShareDestClick("Instagram")
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000)
                                android.widget.Toast.makeText(context, "Redirecting to Instagram Stories...", android.widget.Toast.LENGTH_SHORT).show()
                                showInstagramPulse = false
                            }
                        }
                        .weight(1f)
                        .testTag("share_channel_instagram")
                ) {
                    val instagramScale by animateFloatAsState(
                        targetValue = if (showInstagramPulse) 1.25f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .scale(instagramScale)
                            .clip(CircleShape)
                            .background(Color(0xFFE1306C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Instagram",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Instagram", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            // SECTION 3: Utility / Creators Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Save Video (Simulate full 0 to 100% beautiful progress ring animation!)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (savingProgressState == null) {
                                onShareDestClick("Save Video")
                                coroutineScope.launch {
                                    var current = 0.0f
                                    while (current <= 1.0f) {
                                        savingProgressState = current
                                        kotlinx.coroutines.delay(100)
                                        current += 0.1f
                                    }
                                    android.widget.Toast.makeText(context, "Video saved directly to gallery! 📥", android.widget.Toast.LENGTH_LONG).show()
                                    savingProgressState = null
                                }
                            }
                        }
                        .weight(1f)
                        .testTag("share_channel_save_video")
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = savingProgressState
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = progress,
                                color = TikTokCyan,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = TikTokCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Save Video",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (savingProgressState != null) "Saving..." else "Save Video",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Duet Mode (Simulate horizontal dynamic split screens with overlay slide)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            showDuetSplitCover = true
                            onShareDestClick("Duet")
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2600)
                                showDuetSplitCover = false
                            }
                        }
                        .weight(1f)
                        .testTag("share_channel_duet")
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (showDuetSplitCover) TikTokCyan.copy(alpha = 0.3f) else Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = "Duet",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Duet Mode",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(2f)) // keep spacing proportional
            }
        }
    }

    // Full screen overlay for Duet Simulation animation
    if (showDuetSplitCover) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { /* Block taps */ }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DUET SPLIT SCREEN MODE",
                    color = TikTokCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Split display animation comparison
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, TikTokCyan, RoundedCornerShape(16.dp))
                ) {
                    // Left screen (original video thumbnail preview with scale)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = video.creatorAvatar,
                            contentDescription = "Main video",
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f))
                        )
                        Text(
                            text = "Original Video",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                        )
                    }

                    // Right screen (Simulated selfie/recorder split screen pulsing)
                    val infiniteTransition = rememberInfiniteTransition()
                    val selfiePulse by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Gray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.scale(selfiePulse)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = "Recording",
                                tint = TikTokNeonPink,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Recording...",
                                color = TikTokNeonPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = TikTokCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aligning video tracks & mixing audio...", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

// 8. AUTHENTICATION & REGISTRATION SCREENS (Sign up, Log In, and Google Auth flows)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: com.example.viewmodel.WtubeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(false) }

    // Text field controllers
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showGoogleOneTap by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TikTokDarkBackground)
            .statusBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Wtube branded stylized neon text logo headers
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "wtube",
                    color = TikTokCyan,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(x = (-3).dp, y = (-2).dp)
                )
                Text(
                    text = "wtube",
                    color = TikTokNeonPink,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(x = 3.dp, y = 2.dp)
                )
                Text(
                    text = "wtube",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "Discover loops, sync with creators, share stories.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Switch Tabs (Log In vs Create Account)
            TabRow(
                selectedTabIndex = if (isSignUpMode) 1 else 0,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (isSignUpMode) 1 else 0]),
                        color = TikTokNeonPink
                    )
                },
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Tab(
                    selected = !isSignUpMode,
                    onClick = { isSignUpMode = false },
                    text = { Text("Log In", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
                Tab(
                    selected = isSignUpMode,
                    onClick = { isSignUpMode = true },
                    text = { Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // TextFields Column
            if (isSignUpMode) {
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = "Email", tint = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                )

                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (e.g. @aquib)", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Username", tint = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("auth_username_input")
                )

                // Display Name Field
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name / Full Name", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Display Name", tint = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("auth_display_name_input")
                )

                // Phone Number Field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone Number", tint = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("auth_phone_input")
                )

                // Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (Optional status)", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Bio", tint = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Email / Username Field (for login)
                OutlinedTextField(
                    value = email, // repurposed for login identifier
                    onValueChange = { email = it },
                    label = { Text("Email or Username", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Credential", tint = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TikTokCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("auth_login_credential_input")
                )
            }

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password", tint = Color.LightGray) },
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "HIDE" else "SHOW",
                        color = TikTokCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { passwordVisible = !passwordVisible }
                            .padding(end = 12.dp)
                    )
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TikTokCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Submit Button
            Button(
                onClick = {
                    isLoading = true
                    if (isSignUpMode) {
                        viewModel.signUp(
                            email = email,
                            username = username,
                            displayName = displayName,
                            bio = bio,
                            passwordEntered = password,
                            phoneNumber = phoneNumber
                        ) { success, msg ->
                            isLoading = false
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    } else {
                        viewModel.login(
                            emailOrUsername = email,
                            passwordEntered = password
                        ) { success, msg ->
                            isLoading = false
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TikTokNeonPink),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isSignUpMode) "Register Account" else "Log In",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Demo Credentials helper tip
            if (!isSignUpMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "💡 Quick Tryout Demo Credentials:",
                            color = TikTokCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Email/Username: user@example.com OR @aquib_wtube\nPassword: password123",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Decorator divider options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                Text("or", color = Color.Gray, fontSize = 12.sp)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            }

            // GOOGLE CTA PILL
            Button(
                onClick = { showGoogleOneTap = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .testTag("auth_google_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    com.example.ui.screens.GoogleGLogoIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        color = Color(0xFF1F1F1F),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated overlay for secure Google One Tap simulation
        AnimatedVisibility(
            visible = showGoogleOneTap,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showGoogleOneTap = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .clickable(enabled = false, onClick = {}) // block background click
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(24.dp)
                        .testTag("google_one_tap_sheet"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.ui.screens.GoogleGLogoIcon()
                            Text(
                                text = "Sign in with Google",
                                color = Color(0xFF1F1F1F),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showGoogleOneTap = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Text(
                        text = "To authorize log-in securely, Google will share your email address and default name with Wtube.",
                        color = Color.DarkGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // User primary account option (Prepopulates with user metadata for supreme custom detail!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aquib D. (Verified Account)",
                                color = Color(0xFF1F1F1F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "aqibdih339@gmail.com",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }

                    // Confirm identity Button
                    Button(
                        onClick = {
                            viewModel.loginWithGoogle(
                                email = "aqibdih339@gmail.com",
                                displayName = "Aquib D.",
                                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                            ) { success, msg ->
                                showGoogleOneTap = false
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_continue_button")
                    ) {
                        Text("Continue as Aquib", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "Secured Identity by Google Account Services",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

// Custom canvas vector Google colorful G brand
@Composable
fun GoogleGLogoIcon() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val width = size.width
        val height = size.height

        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = true
        )
        // Yellow left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = true
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 315f,
            sweepAngle = 90f,
            useCenter = true
        )
        // Blue right arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = true
        )

        // Inner white solid cutout circle
        drawCircle(
            color = Color.White,
            radius = width * 0.35f
        )

        // Draw Google blue horizontal stem bar
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.38f),
            size = androidx.compose.ui.geometry.Size(width * 0.46f, height * 0.24f)
        )
    }
}

