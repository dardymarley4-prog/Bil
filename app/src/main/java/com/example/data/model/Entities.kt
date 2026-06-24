package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val username: String,
    val bio: String = "Heureux sur SMILIFE ! ☀️",
    val avatarEmoji: String = "😊",
    val dailySmileGoal: Int = 3,
    val smilesShared: Int = 0,
    val smilesReceived: Int = 0
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val username: String,
    val avatarEmoji: String,
    val content: String,
    val category: String, // Gratitude, Joie, Gentillesse, Humour, Défi
    val timestamp: Long = System.currentTimeMillis(),
    val smileCount: Int = 0,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "IMAGE" or "VIDEO"
    val groupId: Int? = null // Null if posted in general feed, otherwise groupId
)

@Entity(tableName = "post_likes", primaryKeys = ["postId", "userId"])
data class PostLikeEntity(
    val postId: Int,
    val userId: Int
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val userId: Int,
    val username: String,
    val avatarEmoji: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val topic: String,
    val creatorId: Int,
    val avatarEmoji: String = "👥",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberEntity(
    val groupId: Int,
    val userId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int, // Recipient
    val type: String, // "FRIEND_REQUEST", "MESSAGE", "MENTION", "GROUP_POST"
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val referenceId: Int = 0, // Id of the post, message, sender or group
    val senderName: String = "",
    val senderEmoji: String = ""
)
