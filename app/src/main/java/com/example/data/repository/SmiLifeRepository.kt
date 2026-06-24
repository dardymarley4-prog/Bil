package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class SmiLifeRepository(
    private val userDao: UserDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val messageDao: MessageDao,
    private val groupDao: GroupDao,
    private val notificationDao: NotificationDao
) {

    // Helper: Secure password hashing
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- USER / AUTH ---

    suspend fun registerUser(email: String, password: String, username: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isEmpty() || password.isEmpty() || username.trim().isEmpty()) {
                return@withContext Result.failure(Exception("Veuillez remplir tous les champs."))
            }
            val existing = userDao.getUserByEmail(normalizedEmail)
            if (existing != null) {
                return@withContext Result.failure(Exception("Cet email est déjà utilisé."))
            }

            val newUser = UserEntity(
                email = normalizedEmail,
                passwordHash = hashPassword(password),
                username = username.trim(),
                avatarEmoji = getRandomAvatar()
            )
            val id = userDao.insertUser(newUser)
            val createdUser = newUser.copy(id = id.toInt())
            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(normalizedEmail)
                ?: return@withContext Result.failure(Exception("Identifiants incorrects."))

            if (user.passwordHash == hashPassword(password)) {
                Result.success(user)
            } else {
                Result.failure(Exception("Identifiants incorrects."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: Int): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    fun getUserByIdFlow(userId: Int): Flow<UserEntity?> {
        return userDao.getUserByIdFlow(userId).flowOn(Dispatchers.IO)
    }

    suspend fun updateUserProfile(userId: Int, username: String, bio: String, avatar: String, dailyGoal: Int): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId)
                ?: return@withContext Result.failure(Exception("Utilisateur non trouvé."))

            val updated = user.copy(
                username = username.trim(),
                bio = bio.trim(),
                avatarEmoji = avatar,
                dailySmileGoal = dailyGoal
            )
            userDao.updateUser(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOtherUsers(currentUserId: Int): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getAllOtherUsers(currentUserId)
    }

    // --- SEARCH METHODS ---

    suspend fun searchUsers(query: String): List<UserEntity> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) emptyList()
        else userDao.searchUsers("%${query.trim()}%")
    }

    suspend fun searchPosts(query: String): List<PostEntity> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) emptyList()
        else postDao.searchPosts("%${query.trim()}%")
    }

    suspend fun searchGroups(query: String): List<GroupEntity> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) emptyList()
        else groupDao.searchGroups("%${query.trim()}%")
    }

    // --- POSTS ---

    fun getPostsFlow(): Flow<List<PostEntity>> {
        return postDao.getAllPostsFlow().flowOn(Dispatchers.IO)
    }

    fun getLikedPostIdsFlow(userId: Int): Flow<Set<Int>> {
        return postDao.getLikedPostIdsFlow(userId)
            .map { list -> list.map { it.postId }.toSet() }
            .flowOn(Dispatchers.IO)
    }

    suspend fun createPost(
        userId: Int,
        content: String,
        category: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        groupId: Int? = null
    ): Result<PostEntity> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId)
                ?: return@withContext Result.failure(Exception("Utilisateur non trouvé."))

            if (content.trim().isEmpty()) {
                return@withContext Result.failure(Exception("Le contenu ne peut pas être vide."))
            }

            val post = PostEntity(
                userId = userId,
                username = user.username,
                avatarEmoji = user.avatarEmoji,
                content = content.trim(),
                category = category,
                mediaUri = mediaUri,
                mediaType = mediaType,
                groupId = groupId
            )
            val id = postDao.insertPost(post)
            
            // Increment user's shared smiles count
            userDao.updateUser(user.copy(smilesShared = user.smilesShared + 1))

            // If it's a group post, we trigger a notification for members of that group (other than the poster)
            if (groupId != null) {
                val group = groupDao.searchGroups("").firstOrNull { it.id == groupId }
                if (group != null) {
                    groupDao.getGroupMembersFlow(groupId).collect { members ->
                        members.forEach { member ->
                            if (member.id != userId) {
                                createNotification(
                                    userId = member.id,
                                    type = "GROUP_POST",
                                    title = "Nouveau post dans ${group.name}",
                                    content = "${user.username} a publié : \"${content.take(30)}...\"",
                                    referenceId = id.toInt(),
                                    senderName = user.username,
                                    senderEmoji = user.avatarEmoji
                                )
                            }
                        }
                    }
                }
            }

            // Detect @mentions and notify users
            detectAndNotifyMentions(user, content, id.toInt())

            Result.success(post.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun detectAndNotifyMentions(sender: UserEntity, content: String, postId: Int) {
        // Simple mention parsing, e.g. @username
        val regex = Regex("@(\\w+)")
        val matches = regex.findAll(content)
        for (match in matches) {
            val username = match.groupValues[1]
            val users = userDao.searchUsers(username)
            val mentionedUser = users.firstOrNull { it.username.equals(username, ignoreCase = true) || it.username.replace(" ", "").equals(username, ignoreCase = true) }
            if (mentionedUser != null && mentionedUser.id != sender.id) {
                createNotification(
                    userId = mentionedUser.id,
                    type = "MENTION",
                    title = "Mentionné(e) dans un post !",
                    content = "${sender.username} vous a mentionné(e) dans sa publication.",
                    referenceId = postId,
                    senderName = sender.username,
                    senderEmoji = sender.avatarEmoji
                )
            }
        }
    }

    suspend fun toggleLike(postId: Int, userId: Int) = withContext(Dispatchers.IO) {
        val alreadyLiked = postDao.isPostLikedByUser(postId, userId) > 0
        if (alreadyLiked) {
            postDao.deleteLike(postId, userId)
            postDao.decrementSmileCount(postId)
        } else {
            postDao.insertLike(PostLikeEntity(postId, userId))
            postDao.incrementSmileCount(postId)
        }
    }

    // --- COMMENTS ---

    fun getCommentsForPostFlow(postId: Int): Flow<List<CommentEntity>> {
        return commentDao.getCommentsForPostFlow(postId).flowOn(Dispatchers.IO)
    }

    suspend fun addComment(postId: Int, userId: Int, content: String): Result<CommentEntity> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId)
                ?: return@withContext Result.failure(Exception("Utilisateur non trouvé."))

            if (content.trim().isEmpty()) {
                return@withContext Result.failure(Exception("Le commentaire ne peut pas être vide."))
            }

            val comment = CommentEntity(
                postId = postId,
                userId = userId,
                username = user.username,
                avatarEmoji = user.avatarEmoji,
                content = content.trim()
            )
            val id = commentDao.insertComment(comment)
            Result.success(comment.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- MESSAGING ---

    fun getMessagesBetweenUsersFlow(userId: Int, otherUserId: Int): Flow<List<MessageEntity>> {
        return messageDao.getMessagesBetweenUsersFlow(userId, otherUserId).flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(senderId: Int, receiverId: Int, content: String): Result<MessageEntity> = withContext(Dispatchers.IO) {
        try {
            if (content.trim().isEmpty()) {
                return@withContext Result.failure(Exception("Le message ne peut pas être vide."))
            }

            val message = MessageEntity(
                senderId = senderId,
                receiverId = receiverId,
                content = content.trim()
            )
            val id = messageDao.insertMessage(message)

            // Trigger notification for recipient (unless it's SmiBot)
            if (receiverId != 999) {
                val sender = userDao.getUserById(senderId)
                if (sender != null) {
                    createNotification(
                        userId = receiverId,
                        type = "MESSAGE",
                        title = "Nouveau message de ${sender.username}",
                        content = content.take(50),
                        referenceId = senderId,
                        senderName = sender.username,
                        senderEmoji = sender.avatarEmoji
                    )
                }
            }

            Result.success(message.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(userId: Int, otherUserId: Int) = withContext(Dispatchers.IO) {
        messageDao.markMessagesAsRead(userId, otherUserId)
    }

    fun getConversationUsersFlow(userId: Int): Flow<List<UserEntity>> {
        return messageDao.getConversationUserIdsFlow(userId).map { ids ->
            ids.mapNotNull { userDao.getUserById(it) }
        }.flowOn(Dispatchers.IO)
    }

    // --- GROUPS & COMMUNITIES ---

    fun getAllGroupsFlow(): Flow<List<GroupEntity>> {
        return groupDao.getAllGroupsFlow().flowOn(Dispatchers.IO)
    }

    fun getGroupsJoinedByUserFlow(userId: Int): Flow<List<GroupEntity>> {
        return groupDao.getGroupsJoinedByUserFlow(userId).flowOn(Dispatchers.IO)
    }

    fun getGroupMembersFlow(groupId: Int): Flow<List<UserEntity>> {
        return groupDao.getGroupMembersFlow(groupId).flowOn(Dispatchers.IO)
    }

    fun getGroupPostsFlow(groupId: Int): Flow<List<PostEntity>> {
        return postDao.getGroupPostsFlow(groupId).flowOn(Dispatchers.IO)
    }

    suspend fun isUserGroupMember(groupId: Int, userId: Int): Boolean = withContext(Dispatchers.IO) {
        groupDao.isUserGroupMember(groupId, userId) > 0
    }

    suspend fun createGroup(creatorId: Int, name: String, description: String, topic: String, avatarEmoji: String): Result<GroupEntity> = withContext(Dispatchers.IO) {
        try {
            if (name.trim().isEmpty() || description.trim().isEmpty() || topic.trim().isEmpty()) {
                return@withContext Result.failure(Exception("Veuillez remplir tous les champs du groupe."))
            }

            val group = GroupEntity(
                name = name.trim(),
                description = description.trim(),
                topic = topic.trim(),
                creatorId = creatorId,
                avatarEmoji = avatarEmoji
            )
            val id = groupDao.insertGroup(group)
            
            // Auto join creator to group
            groupDao.insertGroupMember(GroupMemberEntity(groupId = id.toInt(), userId = creatorId))

            Result.success(group.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGroup(groupId: Int, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            groupDao.insertGroupMember(GroupMemberEntity(groupId, userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(groupId: Int, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            groupDao.deleteGroupMember(groupId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- NOTIFICATIONS ---

    fun getNotificationsForUserFlow(userId: Int): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsForUserFlow(userId).flowOn(Dispatchers.IO)
    }

    suspend fun createNotification(
        userId: Int,
        type: String,
        title: String,
        content: String,
        referenceId: Int,
        senderName: String,
        senderEmoji: String
    ): Result<NotificationEntity> = withContext(Dispatchers.IO) {
        try {
            val notification = NotificationEntity(
                userId = userId,
                type = type,
                title = title,
                content = content,
                referenceId = referenceId,
                senderName = senderName,
                senderEmoji = senderEmoji
            )
            val id = notificationDao.insertNotification(notification)
            Result.success(notification.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: Int) = withContext(Dispatchers.IO) {
        notificationDao.markAllNotificationsAsRead(userId)
    }

    suspend fun markNotificationAsRead(id: Int) = withContext(Dispatchers.IO) {
        notificationDao.markNotificationAsRead(id)
    }

    suspend fun deleteNotification(id: Int) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(id)
    }

    // --- INITIALIZATION / SEEDING DATA ---

    suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        // Check if SmiBot exists
        val bot = userDao.getUserById(999)
        if (bot == null) {
            // 1. Insert SmiBot
            val smibot = UserEntity(
                id = 999,
                email = "smibot@smilife.com",
                passwordHash = hashPassword("smibot123"),
                username = "SmiBot 🤖",
                bio = "Votre compagnon bienveillant pour cultiver le bonheur quotidien !",
                avatarEmoji = "🤖",
                smilesShared = 15,
                smilesReceived = 120
            )
            userDao.insertUser(smibot)

            // 2. Insert other system profiles
            val profiles = listOf(
                UserEntity(id = 1001, email = "lea@smilife.com", passwordHash = hashPassword("lea123"), username = "Léa Bourdon ✨", bio = "Amoureuse de la nature et adepte du yoga 🧘‍♀️ Partageons des ondes positives !", avatarEmoji = "🧘‍♀️", smilesShared = 12, smilesReceived = 45),
                UserEntity(id = 1002, email = "thomas@smilife.com", passwordHash = hashPassword("thomas123"), username = "Thomas Legrand 🏃‍♂️", bio = "La vie est un défi à relever avec le sourire ! Motivé et sportif.", avatarEmoji = "🏃‍♂️", smilesShared = 8, smilesReceived = 24),
                UserEntity(id = 1003, email = "clara@smilife.com", passwordHash = hashPassword("clara123"), username = "Clara Martin 🌸", bio = "Lectrice passionnée, amie fidèle. Reconnaissante pour chaque petit instant.", avatarEmoji = "🌸", smilesShared = 19, smilesReceived = 62)
            )
            profiles.forEach { userDao.insertUser(it) }

            // 3. Insert some welcome posts
            val posts = listOf(
                PostEntity(id = 1, userId = 999, username = "SmiBot 🤖", avatarEmoji = "🤖", content = "Bienvenue sur SMILIFE ! ☀️ Notre mission est simple : partager des ondes positives et cultiver le bonheur. Ici, on ne partage que ce qui fait sourire ! Qu'est-ce qui vous a fait sourire aujourd'hui ?", category = "Joie", smileCount = 15),
                PostEntity(id = 2, userId = 1001, username = "Léa Bourdon ✨", avatarEmoji = "🧘‍♀️", content = "Un simple sourire peut illuminer la journée de quelqu'un. Aujourd'hui, j'ai offert un café à un inconnu et son sourire a fait ma journée ! 🥰 Ne sous-estimez jamais la gentillesse. @ClaraMartin", category = "Gentillesse", smileCount = 28),
                PostEntity(id = 3, userId = 1002, username = "Thomas Legrand 🏃‍♂️", avatarEmoji = "🏃‍♂️", content = "Enfin réussi mon défi de courir 10km ce matin sous un beau soleil ! Quel accomplissement ! Le corps est capable de tout si l'esprit sourit. 🏃‍♂️🔥 @SmiBot", category = "Défi", smileCount = 14),
                PostEntity(id = 4, userId = 1003, username = "Clara Martin 🌸", avatarEmoji = "🌸", content = "Extrêmement reconnaissante pour ce café chaud pris en lisant un bon livre ce matin. La gratitude change notre perspective sur la vie ! Quels sont vos trois mercis d'aujourd'hui ? 🙏", category = "Gratitude", smileCount = 21)
            )
            posts.forEach { postDao.insertPost(it) }

            // 4. Seed some initial comments
            val comments = listOf(
                CommentEntity(postId = 1, userId = 1001, username = "Léa Bourdon ✨", avatarEmoji = "🧘‍♀️", content = "Superbe idée d'application, j'adore le concept !"),
                CommentEntity(postId = 1, userId = 1003, username = "Clara Martin 🌸", avatarEmoji = "🌸", content = "Hâte de partager tous mes petits bonheurs ici !"),
                CommentEntity(postId = 2, userId = 999, username = "SmiBot 🤖", avatarEmoji = "🤖", content = "Magnifique geste, Léa ! La bienveillance est contagieuse."),
                CommentEntity(postId = 3, userId = 1002, username = "Thomas Legrand 🏃‍♂️", avatarEmoji = "🏃‍♂️", content = "Merci à tous pour les encouragements ! 💪")
            )
            comments.forEach { commentDao.insertComment(it) }

            // 5. Seed some greeting messages from SmiBot
            val messages = listOf(
                MessageEntity(senderId = 999, receiverId = 1001, content = "Bonjour Léa ! Merci d'avoir rejoint SmiLife ! 😊 Envoie-moi un message dès que tu as besoin d'un élan de positivité !"),
                MessageEntity(senderId = 1001, receiverId = 999, content = "Merci SmiBot, c'est génial !")
            )
            messages.forEach { messageDao.insertMessage(it) }

            // 6. Seed some communities (Groups)
            val groups = listOf(
                GroupEntity(id = 1, name = "Sérénité & Yoga 🧘‍♀️", description = "Un havre de paix pour partager nos séances, respirations positives, et citations inspirantes de yoga.", topic = "Méditation & Bien-être", creatorId = 1001, avatarEmoji = "🧘‍♀️"),
                GroupEntity(id = 2, name = "Club Course à pied 🏃‍♂️", description = "Ici, on célèbre chaque kilomètre parcouru avec le sourire. Débutants comme marathoniens bienvenus !", topic = "Sport & Santé", creatorId = 1002, avatarEmoji = "🏃‍♂️"),
                GroupEntity(id = 3, name = "Rires & Humeur 🤭", description = "Partageons des blagues, anecdotes rigolotes, memes positifs et tout ce qui provoque un éclat de rire !", topic = "Humour & Joie", creatorId = 999, avatarEmoji = "🤭")
            )
            groups.forEach { groupDao.insertGroup(it) }

            // Seed group members
            val groupMembers = listOf(
                GroupMemberEntity(groupId = 1, userId = 1001),
                GroupMemberEntity(groupId = 1, userId = 1003),
                GroupMemberEntity(groupId = 2, userId = 1002),
                GroupMemberEntity(groupId = 2, userId = 1001),
                GroupMemberEntity(groupId = 3, userId = 999),
                GroupMemberEntity(groupId = 3, userId = 1001),
                GroupMemberEntity(groupId = 3, userId = 1002),
                GroupMemberEntity(groupId = 3, userId = 1003)
            )
            groupMembers.forEach { groupDao.insertGroupMember(it) }

            // Seed group posts
            val groupPosts = listOf(
                PostEntity(id = 101, userId = 1001, username = "Léa Bourdon ✨", avatarEmoji = "🧘‍♀️", content = "Une belle séance de pranayama ce matin face au lever de soleil. Prenez 5 minutes aujourd'hui pour inspirer la paix et expirer la joie ! ✨🧘‍♀️", category = "Joie", groupId = 1),
                PostEntity(id = 102, userId = 1003, username = "Clara Martin 🌸", avatarEmoji = "🌸", content = "Merci Léa pour l'inspiration de ce matin, ça m'a permis de démarrer ma journée d'un pas serein !", category = "Gratitude", groupId = 1),
                PostEntity(id = 201, userId = 1002, username = "Thomas Legrand 🏃‍♂️", avatarEmoji = "🏃‍♂️", content = "Entraînement de fractionné accompli sous une petite pluie bretonne, mais le moral est au top ! Qui court ce soir ?", category = "Défi", groupId = 2),
                PostEntity(id = 301, userId = 999, username = "SmiBot 🤖", avatarEmoji = "🤖", content = "Pourquoi les poissons vivent-ils dans de l'eau salée ? Parce que le poivre les fait éternuer ! 🤭 Allez, un petit sourire pour la journée !", category = "Humour", groupId = 3)
            )
            groupPosts.forEach { postDao.insertPost(it) }

            // 7. Seed some notifications
            val notifications = listOf(
                NotificationEntity(id = 1, userId = 1001, type = "MENTION", title = "Mentionné(e) par SmiBot !", content = "SmiBot 🤖 vous a mentionné dans une publication de bienvenue.", referenceId = 1, senderName = "SmiBot 🤖", senderEmoji = "🤖"),
                NotificationEntity(id = 2, userId = 1001, type = "MESSAGE", title = "Nouveau message de SmiBot", content = "Bonjour Léa ! Merci d'avoir rejoint SmiLife ! 😊 Envoie...", referenceId = 999, senderName = "SmiBot 🤖", senderEmoji = "🤖"),
                NotificationEntity(id = 3, userId = 1001, type = "FRIEND_REQUEST", title = "Demande d'amitié bienveillante !", content = "Thomas Legrand 🏃‍♂️ souhaite devenir votre ami SmiLife.", referenceId = 1002, senderName = "Thomas Legrand 🏃‍♂️", senderEmoji = "🏃‍♂️")
            )
            notifications.forEach { notificationDao.insertNotification(it) }
        }
    }

    private fun getRandomAvatar(): String {
        val avatars = listOf("😊", "🥳", "😎", "🤩", "🌻", "🦄", "🐶", "🚀", "🌸", "⭐", "🦊")
        return avatars.random()
    }
}
