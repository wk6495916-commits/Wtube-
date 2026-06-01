package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.WtubeTab
import com.example.viewmodel.WtubeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WtubeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreenContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContainer(
    viewModel: WtubeViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // Overlay state managers
    val activeCommentVideoId by viewModel.activeCommentVideoId.collectAsState()
    val activeComments by viewModel.activeComments.collectAsState()
    val selectedCreatorHandle by viewModel.selectedCreatorHandle.collectAsState()
    val activeShareSharedVideo by viewModel.activeShareSharedVideo.collectAsState()

    // Focus state (when tapping a grid item, scroll feed to focus on that video)
    var focusVideoByThumbnailId by remember { mutableStateOf<Long?>(null) }

    // Map content flows
    val resolvedFeedVideos = remember(allVideos, focusVideoByThumbnailId) {
        if (focusVideoByThumbnailId == null) {
            allVideos
        } else {
            val matching = allVideos.find { it.id == focusVideoByThumbnailId }
            if (matching != null) {
                // Pin the target video as the absolute first slide
                val reordered = mutableListOf(matching)
                reordered.addAll(allVideos.filter { it.id != focusVideoByThumbnailId })
                reordered
            } else {
                allVideos
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars) // Safeguards bottom navigation gestures
    ) {
        // High fidelity Tab screens
        Scaffold(
            bottomBar = {
                // TikTok immersive bottom bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(if (currentTab == WtubeTab.HOME) Color.Black.copy(alpha = 0.95f) else TikTokDarkBackground)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TAB 1: HOME
                    BottomBarNavItem(
                        tab = WtubeTab.HOME,
                        selected = currentTab == WtubeTab.HOME,
                        label = "Home",
                        activeIcon = Icons.Filled.Home,
                        inactiveIcon = Icons.Outlined.Home,
                        onSelect = {
                            focusVideoByThumbnailId = null // clear focus to reset list
                            viewModel.selectTab(WtubeTab.HOME)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // TAB 2: DISCOVER
                    BottomBarNavItem(
                        tab = WtubeTab.DISCOVER,
                        selected = currentTab == WtubeTab.DISCOVER,
                        label = "Discover",
                        activeIcon = Icons.Filled.Explore,
                        inactiveIcon = Icons.Outlined.Explore,
                        onSelect = { viewModel.selectTab(WtubeTab.DISCOVER) },
                        modifier = Modifier.weight(1f)
                    )

                    // TAB 3: CENTRAL UPLOAD (TikTok wing action style button!)
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CentralUploadIcon(
                            onClick = { viewModel.selectTab(WtubeTab.UPLOAD) }
                        )
                    }

                    // TAB 4: INBOX
                    BottomBarNavItem(
                        tab = WtubeTab.INBOX,
                        selected = currentTab == WtubeTab.INBOX,
                        label = "Inbox",
                        activeIcon = Icons.Filled.Mail,
                        inactiveIcon = Icons.Outlined.Mail,
                        onSelect = { viewModel.selectTab(WtubeTab.INBOX) },
                        modifier = Modifier.weight(1f)
                    )

                    // TAB 5: PROFILE
                    BottomBarNavItem(
                        tab = WtubeTab.PROFILE,
                        selected = currentTab == WtubeTab.PROFILE,
                        label = "Profile",
                        activeIcon = Icons.Filled.Person,
                        inactiveIcon = Icons.Outlined.Person,
                        onSelect = {
                            viewModel.selectCreator(null) // Reset to ME
                            viewModel.selectTab(WtubeTab.PROFILE)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            containerColor = Color.Black
        ) { innerPadding ->
            // Active Tab Content Router
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (currentTab) {
                    WtubeTab.HOME -> {
                        FeedScreen(
                            viewModel = viewModel,
                            videos = resolvedFeedVideos,
                            onCreatorClick = { handle ->
                                viewModel.selectCreator(handle)
                                viewModel.selectTab(WtubeTab.PROFILE)
                            },
                            onCommentClick = { videoId ->
                                viewModel.showCommentsFor(videoId)
                            }
                        )
                    }

                    WtubeTab.DISCOVER -> {
                        DiscoverScreen(
                            viewModel = viewModel,
                            onVideoSelect = { video ->
                                focusVideoByThumbnailId = video.id
                                viewModel.selectTab(WtubeTab.HOME)
                            }
                        )
                    }

                    WtubeTab.UPLOAD -> {
                        UploadScreen(viewModel = viewModel)
                    }

                    WtubeTab.INBOX -> {
                        InboxScreen()
                    }

                    WtubeTab.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            creatorHandle = selectedCreatorHandle,
                            onBack = if (selectedCreatorHandle != null) {
                                { viewModel.selectCreator(null) }
                            } else null,
                            onVideoSelect = { video ->
                                focusVideoByThumbnailId = video.id
                                viewModel.selectTab(WtubeTab.HOME)
                            }
                        )
                    }
                }
            }
        }

        // Drawer comment section sheet slide-up overlay
        AnimatedVisibility(
            visible = activeCommentVideoId != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.showCommentsFor(null) },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Prevent tapping background from closing state
                Box(modifier = Modifier.clickable(enabled = false, onClick = {})) {
                    CommentsBottomSheet(
                        comments = activeComments,
                        onAddComment = { text ->
                            activeCommentVideoId?.let { videoId ->
                                viewModel.addComment(videoId, text)
                            }
                        },
                        onDismiss = { viewModel.showCommentsFor(null) }
                    )
                }
            }
        }

        // Drawer share section sheet slide-up overlay
        AnimatedVisibility(
            visible = activeShareSharedVideo != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.showShareFor(null) },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Prevent tapping background from closing state
                Box(modifier = Modifier.clickable(enabled = false, onClick = {})) {
                    activeShareSharedVideo?.let { sharedVid ->
                        ShareBottomSheet(
                            video = sharedVid,
                            onShareDestClick = { destName ->
                                viewModel.incrementShareCount(sharedVid)
                            },
                            onDismiss = { viewModel.showShareFor(null) }
                        )
                    }
                }
            }
        }
    }
}

// Bottom nav bar helper component
@Composable
fun BottomBarNavItem(
    tab: WtubeTab,
    selected: Boolean,
    label: String,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = label,
            tint = if (selected) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// Dual wing-border styled TikTok create upload icon (+)
@Composable
fun CentralUploadIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 30.dp)
            .clickable { onClick() }
            .testTag("central_upload_indicator"),
        contentAlignment = Alignment.Center
    ) {
        // Cyan background tab wing offset left
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 28.dp)
                .offset(x = (-3).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TikTokCyan)
        )
        // Neon pink background tab wing offset right
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 28.dp)
                .offset(x = 3.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TikTokNeonPink)
        )
        // High contrast central pure white body shape
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Upload Reel",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
