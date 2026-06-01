package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.VideoEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Floating heart metadata for double-tap anims
data class TempHeart(
    val id: Long,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WtubeVideoPlayer(
    video: VideoEntity,
    isActive: Boolean,
    isAutoScrollEnabled: Boolean = false,
    onVideoCompleted: () -> Unit = {},
    onNextScrollClick: (() -> Unit)? = null,
    onLikeToggle: () -> Unit,
    onFollowToggle: () -> Unit,
    onCommentClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onShareClick: () -> Unit,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Video States
    var isVideoPreparing by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isPausedInternal by remember { mutableStateOf(false) }
    val isPlaying = isActive && !isPausedInternal

    // Cache states to avoid heavy redundant native calls during rapid recompositions
    var lastIsPlaying by remember { mutableStateOf<Boolean?>(null) }
    var lastIsMuted by remember { mutableStateOf<Boolean?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    // Tap Feedbacks
    var showPlayPauseOverlay by remember { mutableStateOf(false) }
    var playOverlayIcon by remember { mutableStateOf(Icons.Filled.PlayArrow) }
    val floatingHearts = remember { mutableStateListOf<TempHeart>() }
    var nextHeartId by remember { mutableLongStateOf(0L) }

    // Floating Vinyl Disc Rotation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_spin_angle"
    )

    // Pulse action button animation for follows
    val scaleFollow = remember { Animatable(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container_${video.id}")
    ) {
        // Immersive Full Frame Video Render
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    tag = null
                }
            },
            update = { view ->
                if (!isActive) {
                    if (view.tag != null) {
                        try {
                            if (view.isPlaying) {
                                view.pause()
                            }
                            view.stopPlayback()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        view.tag = null
                        isVideoPreparing = false
                        lastIsPlaying = null
                        lastIsMuted = null
                        mediaPlayerRef = null
                    }
                } else {
                    val currentTag = view.tag as? String
                    if (currentTag != video.videoUrl) {
                        view.tag = video.videoUrl
                        isVideoPreparing = true
                        lastIsPlaying = null
                        lastIsMuted = null
                        mediaPlayerRef = null
                        try {
                            view.setVideoURI(Uri.parse(video.videoUrl))
                            view.setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                mp.isLooping = !isAutoScrollEnabled
                                mp.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                                isVideoPreparing = false
                                if (isPlaying) {
                                    try {
                                        view.start()
                                        lastIsPlaying = true
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                } else {
                                    lastIsPlaying = false
                                }
                                lastIsMuted = isMuted
                            }
                            view.setOnErrorListener { _, _, _ ->
                                isVideoPreparing = false
                                true
                            }
                            view.setOnCompletionListener {
                                if (isAutoScrollEnabled) {
                                    onVideoCompleted()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        // Dynamically update looping property for Auto-Scroll support
                        mediaPlayerRef?.let { mp ->
                            try {
                                val expectedLooping = !isAutoScrollEnabled
                                if (mp.isLooping != expectedLooping) {
                                    mp.isLooping = expectedLooping
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        try {
                            view.setOnCompletionListener {
                                if (isAutoScrollEnabled) {
                                    onVideoCompleted()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Dynamically update play/pause state only if it actually changed
                        if (lastIsPlaying != isPlaying) {
                            try {
                                if (isPlaying) {
                                    if (!view.isPlaying) {
                                        view.start()
                                    }
                                } else {
                                    if (view.isPlaying) {
                                        view.pause()
                                    }
                                }
                                lastIsPlaying = isPlaying
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        // Dynamically update volume/muting only if it actually changed
                        if (lastIsMuted != isMuted) {
                            try {
                                val vol = if (isMuted) 0f else 1f
                                mediaPlayerRef?.setVolume(vol, vol)
                                lastIsMuted = isMuted
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
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
                mediaPlayerRef = null
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(video.id) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            // Popping hearts!
                            val id = nextHeartId++
                            val rotation = Random.nextFloat() * 40f - 20f
                            floatingHearts.add(
                                TempHeart(
                                    id = id,
                                    x = offset.x,
                                    y = offset.y,
                                    rotation = rotation,
                                    scale = 1f
                                )
                            )
                            if (!video.isLiked) {
                                onLikeToggle()
                            }
                            coroutineScope.launch {
                                delay(800)
                                floatingHearts.removeAll { it.id == id }
                            }
                        },
                        onTap = {
                            isPausedInternal = !isPausedInternal
                            playOverlayIcon =
                                if (isPausedInternal) Icons.Filled.Pause else Icons.Filled.PlayArrow
                            showPlayPauseOverlay = true
                            coroutineScope.launch {
                                delay(600)
                                showPlayPauseOverlay = false
                            }
                        }
                    )
                }
        )

        // Applied Aesthetic Filter Overlay (Grayscale, Sepia, or Vibrant)
        VideoFilterOverlay(filterName = video.filterApplied)

        // Bottom gradient overlay for readability of video text details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Top gradient overlay for header readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // Buffering Feedback Spinner
        if (isVideoPreparing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = TikTokNeonPink,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(45.dp)
                )
            }
        }

        // Tap Play/Pause Indicator Overlay
        AnimatedVisibility(
            visible = showPlayPauseOverlay,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = playOverlayIcon,
                    contentDescription = "Playback HUD State",
                    tint = Color.White,
                    modifier = Modifier.size(45.dp)
                )
            }
        }

        // Floating Hearts Canvas (Double tap feedback)
        floatingHearts.forEach { tempHeart ->
            var scaleAnimState by remember { mutableStateOf(0.4f) }
            var yOffsetState by remember { mutableStateOf(0f) }
            var alphaState by remember { mutableStateOf(1f) }
            
            LaunchedEffect(tempHeart.id) {
                animate(
                    initialValue = 0.4f,
                    targetValue = 1.3f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) { value, _ ->
                    scaleAnimState = value
                }
                launch {
                    animate(
                        initialValue = 0f,
                        targetValue = -150f,
                        animationSpec = tween(700, easing = FastOutSlowInEasing)
                    ) { value, _ ->
                        yOffsetState = value
                    }
                }
                launch {
                    delay(300)
                    animate(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = tween(450)
                    ) { value, _ ->
                        alphaState = value
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = (tempHeart.x / LocalContext.current.resources.displayMetrics.density).dp - 35.dp,
                        y = (tempHeart.y / LocalContext.current.resources.displayMetrics.density).dp - 35.dp + yOffsetState.dp
                    )
                    .size(70.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Popping Heart",
                    tint = TikTokNeonPink.copy(alpha = alphaState),
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(tempHeart.rotation)
                        .scale(scaleAnimState)
                )
            }
        }

        // Side Action Deck Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 12.dp)
        ) {
            // 1. Creator Profile Avatar with Follow Circle action
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clickable { onCreatorClick(video.creatorHandle) }
            ) {
                AsyncImage(
                    model = video.creatorAvatar,
                    contentDescription = "Creator Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TikTokGray)
                        .padding(1.5.dp)
                )
                
                // Pulsating Follow Toggle Button pill in Cyber Neon shades
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (video.isFollowed) TikTokCyan else TikTokNeonPink)
                        .align(Alignment.BottomCenter)
                        .clickable {
                            coroutineScope.launch {
                                scaleFollow.animateTo(1.3f, animationSpec = tween(150))
                                onFollowToggle()
                                scaleFollow.animateTo(1.0f, animationSpec = tween(150))
                            }
                        }
                        .scale(scaleFollow.value),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (video.isFollowed) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = "Follow Action Toggle",
                        tint = if (video.isFollowed) Color.Black else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // 2. Heart/Like Activation Button with popping feedback
            val heartScale = remember { Animatable(1f) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            heartScale.animateTo(1.4f, animationSpec = tween(130))
                            onLikeToggle()
                            heartScale.animateTo(1f, animationSpec = tween(130))
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .scale(heartScale.value)
                        .testTag("like_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Like Interaction",
                        tint = if (video.isLiked) TikTokNeonPink else Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatSocialCount(video.likesCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 3. Comments Dialog Sheets Drawer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("comment_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ModeComment,
                        contentDescription = "Comments Overlay",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatSocialCount(video.commentsCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 4. Bookmaking Pin Toggle
            val bookmarkScale = remember { Animatable(1f) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            bookmarkScale.animateTo(1.3f, animationSpec = tween(130))
                            onBookmarkToggle()
                            bookmarkScale.animateTo(1f, animationSpec = tween(130))
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .scale(bookmarkScale.value)
                        .testTag("bookmark_button_${video.id}")
                ) {
                    Icon(
                        imageVector = if (video.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                        contentDescription = "Bookmark",
                        tint = if (video.isBookmarked) TikTokGold else Color.White,
                        modifier = Modifier.size(35.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatSocialCount(video.bookmarksCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 5. Sharing Link Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share Profile",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatSocialCount(video.sharesCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 5b. Next Scroll Arrow Action
            if (onNextScrollClick != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onNextScrollClick,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("scroll_next_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TikTokCyan.copy(alpha = 0.25f))
                                .border(1.dp, TikTokCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = "Scroll Next Video",
                                tint = TikTokCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Next",
                        color = TikTokCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 6. Spinning Lo-fi Vinyl Soundtrack Record
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(2.dp, TikTokGray, CircleShape)
                    .padding(4.dp)
                    .rotate(if (isPlaying) discRotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = video.creatorAvatar,
                    contentDescription = "Disc Audio Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            }
        }

        // Bottom Details & Description Overlay Block (Aligned Left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(bottom = 78.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Creator Handle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onCreatorClick(video.creatorHandle) }
            ) {
                Text(
                    text = video.creatorName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = video.creatorHandle,
                    color = TikTokSubText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Description, tags
            Text(
                text = "${video.description} ${video.tags}",
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Scrolling Audio Track Title (Marquee-Style Vibe)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Sound Track Playing",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${video.musicArtist} - ${video.musicName}",
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Extremely slim progress timeline scrubber at the absolute bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(Color.White.copy(alpha = 0.25f))
                .align(Alignment.BottomCenter)
        ) {
            var progress by remember { mutableStateOf(0f) }
            if (isPlaying) {
                LaunchedEffect(video.id) {
                    while (true) {
                        progress += 0.012f
                        if (progress >= 1f) progress = 0f
                        delay(200)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Brush.horizontalGradient(listOf(TikTokCyan, TikTokNeonPink)))
            )
        }
    }
}

// Utility to nicely format statistics (e.g. 104200 -> 104.2K, 1250000 -> 1.2M)
fun formatSocialCount(count: Long): String {
    return when {
         count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
         count >= 1_000 -> String.format("%.1fK", count / 1_000f)
         else -> count.toString()
    }
}

@Composable
fun VideoFilterOverlay(
    filterName: String,
    modifier: Modifier = Modifier
) {
    if (filterName == "Normal" || filterName.isBlank()) return
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("video_filter_overlay_${filterName.lowercase()}")
    ) {
        when (filterName) {
            "Grayscale" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color(0xFF808080),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Color
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x304E4E4E))
                )
            }
            "Sepia" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color(0x3A704212),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Color
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x22704212))
                )
            }
            "Vibrant" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color(0x28FF007F),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Overlay
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x1500FFCC))
                )
            }
        }
    }
}
