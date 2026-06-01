package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUrl: String,
    val creatorName: String,
    val creatorHandle: String,
    val creatorAvatar: String,
    val musicName: String,
    val musicArtist: String,
    val description: String,
    val tags: String, // Comma or space separated hashtags, e.g. "#tiktok #dance #wtube"
    val likesCount: Long,
    val commentsCount: Long,
    val sharesCount: Long,
    val bookmarksCount: Long,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isFollowed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val filterApplied: String = "Normal"
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: Long,
    val authorName: String,
    val authorHandle: String,
    val authorAvatar: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val followersCount: Long,
    val followingCount: Long,
    val totalLikes: Long
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY timestamp DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: Long): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("SELECT * FROM videos WHERE isLiked = 1 ORDER BY timestamp DESC")
    fun getLikedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE creatorHandle = :handle ORDER BY timestamp DESC")
    fun getVideosByCreator(handle: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE tags LIKE :hashtag ORDER BY timestamp DESC")
    fun searchVideosByHashtag(hashtag: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE description LIKE :query OR creatorHandle LIKE :query OR musicName LIKE :query OR tags LIKE :query ORDER BY timestamp DESC")
    fun searchVideos(query: String): Flow<List<VideoEntity>>
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY timestamp DESC")
    fun getCommentsForVideo(videoId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Long)
}

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val passwordHash: String,
    val phoneNumber: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'me'")
    fun getMyProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile WHERE id = 'me'")
    suspend fun clearMyProfile()
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getAccountByUsername(username: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity)
}

@Database(entities = [VideoEntity::class, CommentEntity::class, UserProfileEntity::class, UserAccountEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun commentDao(): CommentDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userAccountDao(): UserAccountDao
}

class WtubeRepository(private val db: AppDatabase) {
    val videoDao = db.videoDao()
    val commentDao = db.commentDao()
    val userProfileDao = db.userProfileDao()
    val userAccountDao = db.userAccountDao()

    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val likedVideos: Flow<List<VideoEntity>> = videoDao.getLikedVideos()
    val myProfile: Flow<UserProfileEntity?> = userProfileDao.getMyProfile()

    fun getVideosByCreator(handle: String): Flow<List<VideoEntity>> = videoDao.getVideosByCreator(handle)
    fun getCommentsForVideo(videoId: Long): Flow<List<CommentEntity>> = commentDao.getCommentsForVideo(videoId)
    fun searchVideos(query: String): Flow<List<VideoEntity>> = videoDao.searchVideos("%$query%")
    fun searchHashtag(tag: String): Flow<List<VideoEntity>> = videoDao.searchVideosByHashtag("%$tag%")

    suspend fun insertVideo(video: VideoEntity): Long = videoDao.insertVideo(video)
    suspend fun updateVideo(video: VideoEntity) = videoDao.updateVideo(video)
    suspend fun insertComment(comment: CommentEntity) = commentDao.insertComment(comment)
    suspend fun insertProfile(profile: UserProfileEntity) = userProfileDao.insertProfile(profile)
    suspend fun clearMyProfile() = userProfileDao.clearMyProfile()

    suspend fun getAccountByEmail(email: String): UserAccountEntity? = userAccountDao.getAccountByEmail(email)
    suspend fun getAccountByUsername(username: String): UserAccountEntity? = userAccountDao.getAccountByUsername(username)
    suspend fun insertAccount(account: UserAccountEntity) = userAccountDao.insertAccount(account)
}
