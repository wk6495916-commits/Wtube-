package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Screen states for switching between bottom navigation tabs or specialized views
enum class WtubeTab {
    HOME, DISCOVER, UPLOAD, INBOX, PROFILE
}

class WtubeViewModel(application: Application) : AndroidViewModel(application) {

    private val db: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "wtube-database"
    ).fallbackToDestructiveMigration().build()

    val repository = WtubeRepository(db)

    // UI States
    val allVideos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedVideos: StateFlow<List<VideoEntity>> = repository.likedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myProfile: StateFlow<UserProfileEntity?> = repository.myProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Search and tab switching state
    private val _currentTab = MutableStateFlow(WtubeTab.HOME)
    val currentTab: StateFlow<WtubeTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(listOf("cyberpunk", "travel", "@chloe_creates"))
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    val searchResults: StateFlow<List<VideoEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allVideos
            } else if (query.startsWith("#")) {
                repository.searchHashtag(query)
            } else {
                repository.searchVideos(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks which comments are shown. Contains active video ID.
    private val _activeCommentVideoId = MutableStateFlow<Long?>(null)
    val activeCommentVideoId: StateFlow<Long?> = _activeCommentVideoId.asStateFlow()

    // Tracks which video is currently being shared
    private val _activeShareSharedVideo = MutableStateFlow<VideoEntity?>(null)
    val activeShareSharedVideo: StateFlow<VideoEntity?> = _activeShareSharedVideo.asStateFlow()

    val activeComments: StateFlow<List<CommentEntity>> = _activeCommentVideoId
        .flatMapLatest { videoId ->
            if (videoId == null) flowOf(emptyList())
            else repository.getCommentsForVideo(videoId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile detail state for viewing other creators
    private val _selectedCreatorHandle = MutableStateFlow<String?>(null)
    val selectedCreatorHandle: StateFlow<String?> = _selectedCreatorHandle.asStateFlow()

    val creatorDetailVideos: StateFlow<List<VideoEntity>> = _selectedCreatorHandle
        .flatMapLatest { handle ->
            if (handle == null) flowOf(emptyList())
            else repository.getVideosByCreator(handle)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run database check and pre-population on high-performance dispatch context
        viewModelScope.launch {
            allVideos.first { true } // wait for initial load trigger
            prepopulateIfNeeded()
            // Check if profile exists to set _isLoggedIn
            val profile = repository.myProfile.first()
            _isLoggedIn.value = (profile != null)
        }
    }

    fun selectTab(tab: WtubeTab) {
        _currentTab.value = tab
        if (tab != WtubeTab.HOME) {
            // Pause any feed actions if needed
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            addRecentSearch(query)
        }
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        _recentSearches.value = if (current.size > 8) current.take(8) else current
    }

    fun removeRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        _recentSearches.value = current
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun selectCreator(handle: String?) {
        _selectedCreatorHandle.value = handle
    }

    fun showCommentsFor(videoId: Long?) {
        _activeCommentVideoId.value = videoId
    }

    fun showShareFor(video: VideoEntity?) {
        _activeShareSharedVideo.value = video
    }

    fun incrementShareCount(video: VideoEntity) {
        viewModelScope.launch {
            val updated = video.copy(sharesCount = video.sharesCount + 1)
            repository.updateVideo(updated)
            // Synchronize active sharing entity state
            if (_activeShareSharedVideo.value?.id == video.id) {
                _activeShareSharedVideo.value = updated
            }
        }
    }

    // Toggle video actions
    fun toggleLike(video: VideoEntity) {
        viewModelScope.launch {
            val updated = video.copy(
                isLiked = !video.isLiked,
                likesCount = if (video.isLiked) video.likesCount - 1 else video.likesCount + 1
            )
            repository.updateVideo(updated)
            
            // If it's my own database, synchronize total likes on my profile if creatorHandle matches me
            if (video.creatorHandle == "@aquib_wtube") {
                myProfile.value?.let { profile ->
                    repository.insertProfile(
                        profile.copy(
                            totalLikes = if (video.isLiked) profile.totalLikes - 1 else profile.totalLikes + 1
                        )
                    )
                }
            }
        }
    }

    fun toggleFollow(video: VideoEntity) {
        viewModelScope.launch {
            val updated = video.copy(isFollowed = !video.isFollowed)
            repository.updateVideo(updated)
            
            // Sync all videos of the same creator with follow status!
            allVideos.value.forEach { otherVideo ->
                if (otherVideo.creatorHandle == video.creatorHandle && otherVideo.id != video.id) {
                    repository.updateVideo(otherVideo.copy(isFollowed = updated.isFollowed))
                }
            }
        }
    }

    fun toggleBookmark(video: VideoEntity) {
        viewModelScope.launch {
            val updated = video.copy(
                isBookmarked = !video.isBookmarked,
                bookmarksCount = if (video.isBookmarked) video.bookmarksCount - 1 else video.bookmarksCount + 1
            )
            repository.updateVideo(updated)
        }
    }

    fun addComment(videoId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val profile = myProfile.value
            val comment = CommentEntity(
                videoId = videoId,
                authorName = profile?.displayName ?: "Aquib D.",
                authorHandle = profile?.username ?: "@aquib_wtube",
                authorAvatar = profile?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                text = text
            )
            repository.insertComment(comment)
            
            // Increment comment count on the video itself
            allVideos.value.find { it.id == videoId }?.let { video ->
                repository.updateVideo(video.copy(commentsCount = video.commentsCount + 1))
            }
        }
    }

    fun createNewVideo(
        videoUrl: String,
        description: String,
        musicName: String,
        musicArtist: String,
        tags: String,
        filterApplied: String = "Normal"
    ) {
        viewModelScope.launch {
            val profile = myProfile.value
            val newVid = VideoEntity(
                videoUrl = if (videoUrl.isNotBlank()) videoUrl else "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                creatorName = profile?.displayName ?: "Aquib D.",
                creatorHandle = profile?.username ?: "@aquib_wtube",
                creatorAvatar = profile?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                musicName = if (musicName.isNotBlank()) musicName else "Original SoundTrack",
                musicArtist = if (musicArtist.isNotBlank()) musicArtist else (profile?.displayName ?: "Aquib"),
                description = description,
                tags = if (tags.startsWith("#")) tags else tags.split(" ").joinToString(" ") { "#$it" },
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                bookmarksCount = 0,
                isLiked = false,
                isBookmarked = false,
                isFollowed = false,
                filterApplied = filterApplied
            )
            repository.insertVideo(newVid)
        }
    }

    fun updateMyProfile(displayName: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            myProfile.value?.let { profile ->
                val updated = profile.copy(
                    displayName = displayName,
                    bio = bio,
                    avatarUrl = avatarUrl.ifBlank { profile.avatarUrl }
                )
                repository.insertProfile(updated)
                
                // Update all items where I am the creator to show the updated name and avatar!
                allVideos.value.forEach { video ->
                    if (video.creatorHandle == profile.username) {
                        repository.updateVideo(
                            video.copy(
                                creatorName = updated.displayName,
                                creatorAvatar = updated.avatarUrl
                            )
                        )
                    }
                }
            }
        }
    }

    fun signUp(
        email: String,
        username: String,
        displayName: String,
        bio: String,
        passwordEntered: String,
        phoneNumber: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (email.isBlank() || username.isBlank() || displayName.isBlank() || passwordEntered.isBlank() || phoneNumber.isBlank()) {
                onResult(false, "Please fill in all layout fields, including phone number.")
                return@launch
            }
            val formattedUsername = if (username.startsWith("@")) username.trim() else "@${username.trim()}"

            val existingEmail = repository.getAccountByEmail(email.trim())
            if (existingEmail != null) {
                onResult(false, "An account with this email already exists.")
                return@launch
            }
            val existingUser = repository.getAccountByUsername(formattedUsername)
            if (existingUser != null) {
                onResult(false, "This username is already taken.")
                return@launch
            }

            val defaultAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            val newAccount = UserAccountEntity(
                email = email.trim(),
                username = formattedUsername,
                displayName = displayName.trim(),
                avatarUrl = defaultAvatar,
                bio = bio.ifBlank { "Excited to join Wtube! 🚀" },
                passwordHash = passwordEntered,
                phoneNumber = phoneNumber.trim()
            )
            repository.insertAccount(newAccount)

            val activeProfile = UserProfileEntity(
                username = newAccount.username,
                displayName = newAccount.displayName,
                avatarUrl = newAccount.avatarUrl,
                bio = newAccount.bio,
                followersCount = 0,
                followingCount = 0,
                totalLikes = 0
            )
            repository.insertProfile(activeProfile)
            _isLoggedIn.value = true
            onResult(true, "Successfully signed up! Welcome to Wtube 🎉")
        }
    }

    fun login(emailOrUsername: String, passwordEntered: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (emailOrUsername.isBlank() || passwordEntered.isBlank()) {
                onResult(false, "Please enter both credentials.")
                return@launch
            }

            val credential = emailOrUsername.trim()
            var account = repository.getAccountByEmail(credential)
            if (account == null) {
                val searchUser = if (credential.startsWith("@")) credential else "@$credential"
                account = repository.getAccountByUsername(searchUser)
            }

            if (account == null) {
                // If it is our preseeded default user, dynamically register them so they can login safely
                if (credential == "user@example.com" || credential == "@aquib_wtube" || credential == "aquib") {
                    val defaultAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                    val preseeded = UserAccountEntity(
                        email = "user@example.com",
                        username = "@aquib_wtube",
                        displayName = "Aquib D.",
                        avatarUrl = defaultAvatar,
                        bio = "🚀 Building Wtube — the final-frontier TikTok alternative on Android! Material 3 dynamic styling, Kotlin coroutines, and fluid video playbacks.",
                        passwordHash = "password123"
                    )
                    repository.insertAccount(preseeded)
                    account = preseeded
                }
            }

            if (account == null) {
                onResult(false, "No account found with these credentials.")
                return@launch
            }

            if (account.passwordHash != passwordEntered) {
                onResult(false, "Incorrect password. Please try again.")
                return@launch
            }

            val profile = UserProfileEntity(
                username = account.username,
                displayName = account.displayName,
                avatarUrl = account.avatarUrl,
                bio = account.bio,
                followersCount = 42800,
                followingCount = 382,
                totalLikes = 52710
            )
            repository.insertProfile(profile)
            _isLoggedIn.value = true
            onResult(true, "Welcome back, ${account.displayName}! 👋")
        }
    }

    fun loginWithGoogle(email: String, displayName: String, avatarUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                onResult(false, "Unable to resolve Google account.")
                return@launch
            }
            val cleanEmail = email.trim()
            val usernameFromEmail = "@" + cleanEmail.substringBefore("@").lowercase().replace(Regex("[^a-zA-Z0-9_]"), "")

            var account = repository.getAccountByEmail(cleanEmail)
            if (account == null) {
                account = UserAccountEntity(
                    email = cleanEmail,
                    username = usernameFromEmail,
                    displayName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") },
                    avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                    bio = "Logged in securely with Google Auth! 🎨",
                    passwordHash = "google_authenticated_oauth2"
                )
                repository.insertAccount(account)
            }

            val profile = UserProfileEntity(
                username = account.username,
                displayName = account.displayName,
                avatarUrl = account.avatarUrl,
                bio = account.bio,
                followersCount = 42800,
                followingCount = 382,
                totalLikes = 52710
            )
            repository.insertProfile(profile)
            _isLoggedIn.value = true
            onResult(true, "Successfully authenticated as $cleanEmail! Google identity verified. 🗝️")
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearMyProfile()
            _isLoggedIn.value = false
        }
    }

    private suspend fun prepopulateIfNeeded() {
        val currentList = db.videoDao().getAllVideos().first()
        if (currentList.isNotEmpty()) return

        // Populate User Profiling
        val defaultProfile = UserProfileEntity(
            username = "@aquib_wtube",
            displayName = "Aquib D.",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            bio = "🚀 Building Wtube — the final-frontier TikTok alternative on Android! Material 3 dynamic styling, Kotlin coroutines, and fluid video playbacks.",
            followersCount = 42800,
            followingCount = 382,
            totalLikes = 52710
        )
        repository.insertProfile(defaultProfile)

        // 5 Beautiful sample videos
        val v1Id = repository.insertVideo(
            VideoEntity(
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                creatorName = "Chloe Digital",
                creatorHandle = "@chloe_creates",
                creatorAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                musicName = "Neon Midnight (Lo-fi mix)",
                musicArtist = "Cyber Audio Works",
                description = "Chasing the ultimate cyberpunk neon aesthetic tonight in Tokyo! The lights hit differently in 4K 🌃✨",
                tags = "#wtube #tokyo #cyberpunk #aesthetic #foryou",
                likesCount = 28400,
                commentsCount = 3,
                sharesCount = 1420,
                bookmarksCount = 3890,
                isLiked = false
            )
        )

        val v2Id = repository.insertVideo(
            VideoEntity(
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                creatorName = "Alex Dancer",
                creatorHandle = "@alex_grooves",
                creatorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                musicName = "Beats from the Streets Vol 12",
                musicArtist = "Acoustic loops",
                description = "Street hip-hop improvisation! Practice daily, refine always. Let me know what you guys think of this fluid drop? 🔥🕺",
                tags = "#dance #hiphop #improvisation #streetstyle #wtube",
                likesCount = 59100,
                commentsCount = 3,
                sharesCount = 9430,
                bookmarksCount = 12700,
                isLiked = false
            )
        )

        val v3Id = repository.insertVideo(
            VideoEntity(
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                creatorName = "Sofia Wanderlust",
                creatorHandle = "@sofia_escapes",
                creatorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                musicName = "Golden Hour Serenade",
                musicArtist = "Acoustic loops",
                description = "Waking up inside an alpine glass cabin in Switzerland is a core memory unlocked. Absolutely breathtaking landscape! 🏔️🌲❤️",
                tags = "#travel #switzerland #nature #adventure #aesthetic",
                likesCount = 104200,
                commentsCount = 3,
                sharesCount = 21000,
                bookmarksCount = 32800,
                isLiked = false
            )
        )

        val v4Id = repository.insertVideo(
            VideoEntity(
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                creatorName = "Marcus Riffs",
                creatorHandle = "@marcus_guitars",
                creatorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                musicName = "Original Guitar Jam (Stuttered)",
                musicArtist = "Marcus Riffs",
                description = "New continuous acoustic sliding melody I wrote! Should I publish the full tab on my web portal? Let's talk in comments! 🎸🎶",
                tags = "#music #guitar #guitarsolo #originalsound #tutorial",
                likesCount = 42100,
                commentsCount = 3,
                sharesCount = 890,
                bookmarksCount = 3120,
                isLiked = false
            )
        )

        val v5Id = repository.insertVideo(
            VideoEntity(
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                creatorName = "Tech Horizon",
                creatorHandle = "@tech_horizon",
                creatorAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                musicName = "Synthwave Sunrise",
                musicArtist = "Electro Drone",
                description = "Unboxing the world's first liquid holographic wearable tracker! The build feels hyper premium and solid. Complete review coming soon! 🕶️📟",
                tags = "#tech #gadget #cyberwear #futuretech #wtube",
                likesCount = 12500,
                commentsCount = 3,
                sharesCount = 650,
                bookmarksCount = 980,
                isLiked = false
            )
        )

        // Seed 3 realistic comments for each video to make it vibrant and alive!
        val commentsMap = mapOf(
            v1Id to listOf(
                Triple("Zoe Bell", "@zoe_reads", "OMG, Tokyo looks absolutely pristine in this. I need to visit this autumn! 😍"),
                Triple("Tyler Chase", "@tyler_chase", "That grading is immaculate. Which profile did you shoot this on?"),
                Triple("Dan Tech", "@dan_tech", "Insane clarity! Wtube compression is handling this 4k marvel flawlessly.")
            ),
            v2Id to listOf(
                Triple("Emily Styles", "@emily_styles", "The footwork at 0:12 is wild!!! You're floating! 😱✨"),
                Triple("Vince Carter", "@vince_grooves", "Let's collab next week in Berlin man, keep hitting hard!"),
                Triple("Mia Wong", "@mia_wong", "Best dance clip of the day on my FYP easily. Underrated.")
            ),
            v3Id to listOf(
                Triple("Lukas Keller", "@lukas_wander", "I've been there! Literally feels like living inside a luxury crystal ball!"),
                Triple("Sarah Croft", "@sarah_adventures", "Adding this to my bucket list immediately. How far is it from Zurich?"),
                Triple("David King", "@david_k", "My jaw literally dropped. The sheer majesty is stunning.")
            ),
            v4Id to listOf(
                Triple("Leo Sterling", "@leo_strings", "Yes please release the tab! My fingers are itching to practice this. 🎵🎸"),
                Triple("Alice Cooper", "@alice_c", "The sliding harmonics on the intro were exceptionally clean."),
                Triple("Jack Harris", "@jack_music", "Brilliant tone. Are you using a dynamic mic or pickup output?")
            ),
            v5Id to listOf(
                Triple("Bruce Wayne", "@bruce_gadget", "Looks incredibly futuristic. How long does the charge hold?"),
                Triple("Tony S.", "@arc_reactor", "Interesting optic lenses, reminded me of early-stage HUD devices."),
                Triple("Lisa Vance", "@lisa_v", "This looks neat, but is it heavy to wear on a jog?")
            )
        )

        commentsMap.forEach { (videoId, commentsList) ->
            commentsList.forEach { (author, handle, text) ->
                repository.insertComment(
                    CommentEntity(
                        videoId = videoId,
                        authorName = author,
                        authorHandle = handle,
                        authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100", // standard avatar
                        text = text
                    )
                )
            }
        }
    }
}
