package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdFlow(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id != :currentUserId")
    suspend fun getAllOtherUsers(currentUserId: Int): List<UserEntity>

    @Query("SELECT * FROM users WHERE username LIKE :query OR email LIKE :query")
    suspend fun searchUsers(query: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE groupId IS NULL ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getGroupPostsFlow(groupId: Int): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE (content LIKE :query OR category LIKE :query) AND groupId IS NULL ORDER BY timestamp DESC")
    suspend fun searchPosts(query: String): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Query("UPDATE posts SET smileCount = smileCount + 1 WHERE id = :postId")
    suspend fun incrementSmileCount(postId: Int)

    @Query("UPDATE posts SET smileCount = smileCount - 1 WHERE id = :postId")
    suspend fun decrementSmileCount(postId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: PostLikeEntity)

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteLike(postId: Int, userId: Int)

    @Query("SELECT COUNT(*) FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun isPostLikedByUser(postId: Int, userId: Int): Int

    @Query("SELECT * FROM post_likes WHERE userId = :userId")
    fun getLikedPostIdsFlow(userId: Int): Flow<List<PostLikeEntity>>
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPostFlow(postId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderId = :userId AND receiverId = :otherUserId) OR (senderId = :otherUserId AND receiverId = :userId) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsersFlow(userId: Int, otherUserId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET isRead = 1 WHERE senderId = :otherUserId AND receiverId = :userId")
    suspend fun markMessagesAsRead(userId: Int, otherUserId: Int)

    @Query("SELECT DISTINCT CASE WHEN senderId = :userId THEN receiverId ELSE senderId END FROM messages WHERE senderId = :userId OR receiverId = :userId")
    fun getConversationUserIdsFlow(userId: Int): Flow<List<Int>>
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT g.* FROM groups g INNER JOIN group_members gm ON g.id = gm.groupId WHERE gm.userId = :userId")
    fun getGroupsJoinedByUserFlow(userId: Int): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE name LIKE :query OR topic LIKE :query OR description LIKE :query")
    suspend fun searchGroups(query: String): List<GroupEntity>

    @Query("SELECT u.* FROM users u INNER JOIN group_members gm ON u.id = gm.userId WHERE gm.groupId = :groupId")
    fun getGroupMembersFlow(groupId: Int): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun isUserGroupMember(groupId: Int, userId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteGroupMember(groupId: Int, userId: Int)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: Int): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: Int)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)
}
