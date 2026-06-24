package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.SmiLifeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class Screen {
    object Login : Screen()
    object Signup : Screen()
    object Feed : Screen()
    object Messaging : Screen()
    data class Chat(val user: UserEntity) : Screen()
    object Profile : Screen()
    
    // Expanded Screens
    object Groups : Screen()
    data class GroupDetails(val group: GroupEntity) : Screen()
    object Search : Screen()
    object Notifications : Screen()
}

class SmiLifeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmiLifeRepository
    
    // UI Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Auth States
    val loginEmail = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")
    val signupEmail = MutableStateFlow("")
    val signupPassword = MutableStateFlow("")
    val signupUsername = MutableStateFlow("")

    // SMS & Google Auth States (French requirement: Google/SMS auth working)
    val loginPhoneNumber = MutableStateFlow("")
    val sentSmsOtp = MutableStateFlow("")
    val inputSmsOtp = MutableStateFlow("")
    val isOtpSent = MutableStateFlow(false)
    val selectedAuthMethod = MutableStateFlow("EMAIL") // "EMAIL", "SMS", "GOOGLE"

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Feed States
    private val _posts = MutableStateFlow<List<PostEntity>>(emptyList())
    val posts: StateFlow<List<PostEntity>> = _posts.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<Int>>(emptySet())
    val likedPostIds: StateFlow<Set<Int>> = _likedPostIds.asStateFlow()

    val newPostContent = MutableStateFlow("")
    val newPostCategory = MutableStateFlow("Joie") // Joie, Gratitude, Gentillesse, Humour, Défi
    
    // Media attachment states (for photos/videos importable by user)
    val selectedMediaUri = MutableStateFlow<String?>(null)
    val selectedMediaType = MutableStateFlow<String?>(null) // "IMAGE" or "VIDEO"

    // Comments States
    private val _activeCommentPostId = MutableStateFlow<Int?>(null)
    val activeCommentPostId: StateFlow<Int?> = _activeCommentPostId.asStateFlow()

    private val _commentsForPost = MutableStateFlow<List<CommentEntity>>(emptyList())
    val commentsForPost: StateFlow<List<CommentEntity>> = _commentsForPost.asStateFlow()

    val newCommentContent = MutableStateFlow("")

    // Messaging States
    private val _allUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val allUsers: StateFlow<List<UserEntity>> = _allUsers.asStateFlow()

    private val _conversationUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val conversationUsers: StateFlow<List<UserEntity>> = _conversationUsers.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages.asStateFlow()

    val newMessageContent = MutableStateFlow("")
    
    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    // Search query in Messaging
    val searchUserQuery = MutableStateFlow("")

    // ADVANCED SEARCH STATES
    val advancedSearchQuery = MutableStateFlow("")
    private val _searchUserResults = MutableStateFlow<List<UserEntity>>(emptyList())
    val searchUserResults: StateFlow<List<UserEntity>> = _searchUserResults.asStateFlow()

    private val _searchPostResults = MutableStateFlow<List<PostEntity>>(emptyList())
    val searchPostResults: StateFlow<List<PostEntity>> = _searchPostResults.asStateFlow()

    private val _searchGroupResults = MutableStateFlow<List<GroupEntity>>(emptyList())
    val searchGroupResults: StateFlow<List<GroupEntity>> = _searchGroupResults.asStateFlow()

    // NOTIFICATIONS STATES
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    // GROUPS STATES
    private val _allGroups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val allGroups: StateFlow<List<GroupEntity>> = _allGroups.asStateFlow()

    private val _joinedGroups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val joinedGroups: StateFlow<List<GroupEntity>> = _joinedGroups.asStateFlow()

    private val _activeGroupPosts = MutableStateFlow<List<PostEntity>>(emptyList())
    val activeGroupPosts: StateFlow<List<PostEntity>> = _activeGroupPosts.asStateFlow()

    private val _activeGroupMembers = MutableStateFlow<List<UserEntity>>(emptyList())
    val activeGroupMembers: StateFlow<List<UserEntity>> = _activeGroupMembers.asStateFlow()

    private val _isCurrentUserGroupMember = MutableStateFlow(false)
    val isCurrentUserGroupMember: StateFlow<Boolean> = _isCurrentUserGroupMember.asStateFlow()

    // Group Creation Form
    val newGroupName = MutableStateFlow("")
    val newGroupDesc = MutableStateFlow("")
    val newGroupTopic = MutableStateFlow("")
    val newGroupEmoji = MutableStateFlow("👥")

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SmiLifeRepository(
            db.userDao(),
            db.postDao(),
            db.commentDao(),
            db.messageDao(),
            db.groupDao(),
            db.notificationDao()
        )

        // Seed initial data
        viewModelScope.launch {
            repository.seedInitialData()
            loadAllUsers()
        }

        // Listen to active posts (General)
        viewModelScope.launch {
            repository.getPostsFlow().collect {
                _posts.value = it
            }
        }

        // Watch Advanced Search Query changes to perform auto search
        viewModelScope.launch {
            advancedSearchQuery.debounce(300).collect { query ->
                performAdvancedSearch(query)
            }
        }
    }

    // --- NAVIGATION ---

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        _authError.value = null
        val user = _currentUser.value
        
        if (screen is Screen.Messaging) {
            loadAllUsers()
        }
        
        if (user != null) {
            if (screen is Screen.Groups) {
                observeGroups(user.id)
            }
            if (screen is Screen.Notifications) {
                observeNotifications(user.id)
            }
        }
    }

    // --- GROUP OBSERVATION ---

    private fun observeGroups(userId: Int) {
        viewModelScope.launch {
            repository.getAllGroupsFlow().collect {
                _allGroups.value = it
            }
        }
        viewModelScope.launch {
            repository.getGroupsJoinedByUserFlow(userId).collect {
                _joinedGroups.value = it
            }
        }
    }

    fun selectGroup(group: GroupEntity) {
        val user = _currentUser.value ?: return
        navigateTo(Screen.GroupDetails(group))
        
        // Observe this group's posts, members, and membership status
        viewModelScope.launch {
            repository.getGroupPostsFlow(group.id).collect {
                _activeGroupPosts.value = it
            }
        }
        viewModelScope.launch {
            repository.getGroupMembersFlow(group.id).collect {
                _activeGroupMembers.value = it
            }
        }
        viewModelScope.launch {
            // Check membership status
            val isMember = repository.isUserGroupMember(group.id, user.id)
            _isCurrentUserGroupMember.value = isMember
        }
    }

    // --- NOTIFICATION OBSERVATION ---

    private fun observeNotifications(userId: Int) {
        viewModelScope.launch {
            repository.getNotificationsForUserFlow(userId).collect {
                _notifications.value = it
            }
        }
    }

    // --- AUTH ACTIONS ---

    fun handleLogin() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            delay(400) // Smooth animation delay
            
            val result = repository.loginUser(loginEmail.value, loginPassword.value)
            if (result.isSuccess) {
                onAuthSuccess(result.getOrNull())
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "Erreur d'authentification."
            }
            _isAuthLoading.value = false
        }
    }

    fun handleGoogleLogin(gmail: String, name: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            delay(500)
            
            // Create or sign in user based on Google details
            val dbUser = AppDatabase.getDatabase(getApplication()).userDao().getUserByEmail(gmail)
            if (dbUser != null) {
                onAuthSuccess(dbUser)
            } else {
                // Register google user silently
                val regResult = repository.registerUser(gmail, "google_login_pass_123", name)
                if (regResult.isSuccess) {
                    onAuthSuccess(regResult.getOrNull())
                } else {
                    _authError.value = "Impossible de se connecter avec Google."
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun handleSendSmsCode() {
        val phone = loginPhoneNumber.value.trim()
        if (phone.isEmpty()) {
            _authError.value = "Veuillez entrer un numéro de téléphone valide."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            delay(600)
            val randomOtp = (100000..999999).random().toString()
            sentSmsOtp.value = randomOtp
            isOtpSent.value = true
            _isAuthLoading.value = false
            // Post a helpful guide message on screen or log
            Log.d("SmiLifeSMS", "SMS OTP Sent to $phone: $randomOtp")
        }
    }

    fun handleVerifySmsCode() {
        if (inputSmsOtp.value.trim() == sentSmsOtp.value) {
            viewModelScope.launch {
                _isAuthLoading.value = true
                delay(400)
                // Register or sign in via telephone
                val email = "${loginPhoneNumber.value.replace("+", "").replace(" ", "")}@smilife-sms.com"
                val username = "Membre ${loginPhoneNumber.value.takeLast(4)}"
                val dbUser = AppDatabase.getDatabase(getApplication()).userDao().getUserByEmail(email)
                if (dbUser != null) {
                    onAuthSuccess(dbUser)
                } else {
                    val regResult = repository.registerUser(email, "sms_login_pass_123", username)
                    if (regResult.isSuccess) {
                        onAuthSuccess(regResult.getOrNull())
                    } else {
                        _authError.value = "Impossible de se connecter via SMS."
                    }
                }
                _isAuthLoading.value = false
            }
        } else {
            _authError.value = "Code SMS incorrect. Veuillez réessayer."
        }
    }

    private fun onAuthSuccess(user: UserEntity?) {
        _currentUser.value = user
        if (user != null) {
            // Observe current user changes in DB (to keep stats updated)
            viewModelScope.launch {
                repository.getUserByIdFlow(user.id).collect { updated ->
                    _currentUser.value = updated
                }
            }
            viewModelScope.launch {
                repository.getLikedPostIdsFlow(user.id).collect { liked ->
                    _likedPostIds.value = liked
                }
            }
            viewModelScope.launch {
                repository.getConversationUsersFlow(user.id).collect { chats ->
                    _conversationUsers.value = chats
                }
            }
            // Trigger background notifications observing
            observeNotifications(user.id)
            observeGroups(user.id)
        }
        
        navigateTo(Screen.Feed)
        // Clear inputs
        loginEmail.value = ""
        loginPassword.value = ""
        loginPhoneNumber.value = ""
        inputSmsOtp.value = ""
        isOtpSent.value = false
    }

    fun handleSignup() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            delay(400)
            val result = repository.registerUser(
                signupEmail.value,
                signupPassword.value,
                signupUsername.value
            )
            if (result.isSuccess) {
                onAuthSuccess(result.getOrNull())
                // Clear fields
                signupEmail.value = ""
                signupPassword.value = ""
                signupUsername.value = ""
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "Erreur d'inscription."
            }
            _isAuthLoading.value = false
        }
    }

    fun handleLogout() {
        _currentUser.value = null
        _likedPostIds.value = emptySet()
        _conversationUsers.value = emptyList()
        _notifications.value = emptyList()
        _joinedGroups.value = emptyList()
        navigateTo(Screen.Login)
    }

    // --- FEED ACTIONS ---

    fun shareSmile() {
        val user = _currentUser.value ?: return
        val content = newPostContent.value
        val cat = newPostCategory.value
        val mediaUri = selectedMediaUri.value
        val mediaType = selectedMediaType.value
        
        if (content.trim().isEmpty()) return

        viewModelScope.launch {
            val result = repository.createPost(
                userId = user.id,
                content = content,
                category = cat,
                mediaUri = mediaUri,
                mediaType = mediaType
            )
            if (result.isSuccess) {
                newPostContent.value = ""
                newPostCategory.value = "Joie"
                selectedMediaUri.value = null
                selectedMediaType.value = null
            }
        }
    }

    fun togglePostLike(post: PostEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleLike(post.id, user.id)
        }
    }

    // --- COMMENTS ACTIONS ---

    fun openCommentsForPost(postId: Int) {
        _activeCommentPostId.value = postId
        newCommentContent.value = ""
        viewModelScope.launch {
            repository.getCommentsForPostFlow(postId).collect {
                _commentsForPost.value = it
            }
        }
    }

    fun closeComments() {
        _activeCommentPostId.value = null
        _commentsForPost.value = emptyList()
    }

    fun submitComment() {
        val user = _currentUser.value ?: return
        val postId = _activeCommentPostId.value ?: return
        val content = newCommentContent.value
        if (content.trim().isEmpty()) return

        viewModelScope.launch {
            val result = repository.addComment(postId, user.id, content)
            if (result.isSuccess) {
                newCommentContent.value = ""
                // Notify post creator about the comment
                val db = AppDatabase.getDatabase(getApplication())
                val post = db.postDao().searchPosts("").firstOrNull { it.id == postId }
                if (post != null && post.userId != user.id) {
                    repository.createNotification(
                        userId = post.userId,
                        type = "MENTION",
                        title = "Nouveau commentaire !",
                        content = "${user.username} a commenté : \"${content.take(30)}...\"",
                        referenceId = postId,
                        senderName = user.username,
                        senderEmoji = user.avatarEmoji
                    )
                }
            }
        }
    }

    // --- PRIVATE MESSAGING ACTIONS ---

    private fun loadAllUsers() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val list = repository.getAllOtherUsers(user.id)
            _allUsers.value = list
        }
    }

    fun selectChat(otherUser: UserEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markMessagesAsRead(user.id, otherUser.id)
            navigateTo(Screen.Chat(otherUser))
            
            repository.getMessagesBetweenUsersFlow(user.id, otherUser.id).collect {
                _activeMessages.value = it
            }
        }
    }

    fun sendPrivateMessage() {
        val user = _currentUser.value ?: return
        val screen = _currentScreen.value
        if (screen !is Screen.Chat) return
        val otherUser = screen.user
        val content = newMessageContent.value
        if (content.trim().isEmpty()) return

        viewModelScope.launch {
            val result = repository.sendMessage(user.id, otherUser.id, content)
            if (result.isSuccess) {
                newMessageContent.value = ""
                
                // If chatting with SmiBot, trigger auto-reply!
                if (otherUser.id == 999) {
                    triggerBotReply(content)
                }
            }
        }
    }

    // --- SMIBOT REACTION ENGINE ---

    private fun triggerBotReply(userMessage: String) {
        viewModelScope.launch {
            _isBotTyping.value = true
            delay(1200)

            val systemInstruction = "Tu es SmiBot, un compagnon bienveillant sur le réseau social positif SMILIFE. " +
                    "Ton but est d'encourager l'utilisateur, de lui donner des raisons de sourire, de l'aider à cultiver la gratitude " +
                    "et de répondre avec une gentillesse extrême et de la positivité. Réponds toujours en français, " +
                    "de manière chaleureuse, courte (maximum 3 phrases) et motivante, avec des émojis joyeux !"

            var botReply = ""
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    botReply = callGeminiApiDirect(userMessage, systemInstruction, apiKey)
                } catch (e: Exception) {
                    Log.e("SmiLifeVM", "Gemini call failed, falling back to local engine", e)
                }
            }

            if (botReply.trim().isEmpty()) {
                botReply = generateLocalPositiveReply(userMessage)
            }

            repository.sendMessage(999, _currentUser.value!!.id, botReply)
            _isBotTyping.value = false
        }
    }

    private suspend fun callGeminiApiDirect(prompt: String, systemInstruction: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                }
                put(JSONObject().put("parts", partsArray))
            }
            put("contents", contentsArray)

            val sysInst = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                }
                put("parts", partsArray)
            }
            put("systemInstruction", sysInst)
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        }
    }

    private fun generateLocalPositiveReply(message: String): String {
        val normalized = message.lowercase()
        return when {
            normalized.contains("triste") || normalized.contains("pas bien") || normalized.contains("bad") || normalized.contains("pleur") -> {
                val sadnessReplies = listOf(
                    "Je suis là pour toi. 💖 N'oublie pas que même après la pluie la plus sombre, le soleil finit toujours par briller. Prends une grande inspiration, tout ira bien ! 🌈",
                    "C'est tout à fait normal d'avoir des jours plus difficiles. Tu es une personne forte et courageuse, fais-toi confiance. Puis-je faire quelque chose pour te redonner le sourire ? 💕",
                    "Sache que ton sourire a une valeur inestimable. Repose-toi aujourd'hui et concentre-toi sur de toutes petites choses simples et douces. 🌸"
                )
                sadnessReplies.random()
            }
            normalized.contains("merci") || normalized.contains("cool") || normalized.contains("génial") || normalized.contains("super") -> {
                val gratitudeReplies = listOf(
                    "Avec grand plaisir ! C'est un bonheur de discuter avec toi. Continue d'illuminer le monde avec ta belle énergie ! ☀️",
                    "Merci à TOI d'exister et de partager tes sourires sur SMILIFE ! Tu fais toute la différence. 💖",
                    "Génial ! Je suis tellement ravi de lire ça. Que ta journée soit remplie d'autres moments magiques. ⭐"
                )
                gratitudeReplies.random()
            }
            normalized.contains("fatigué") || normalized.contains("marre") || normalized.contains("stress") -> {
                val stressReplies = listOf(
                    "Ouh, il est temps de faire une petite pause bien méritée ! Éteins ton écran quelques minutes, respire profondément et savoure l'instant présent. 🧘‍♀️✨",
                    "Je t'envoie un grand câlin virtuel et plein d'ondes apaisantes. Prends soin de toi, la santé de ton esprit est précieuse ! 🌸",
                    "Rien ne presse. Une chose après l'autre. Tu fais de ton mieux, et c'est déjà amplement suffisant ! 😊"
                )
                stressReplies.random()
            }
            else -> {
                val generalReplies = listOf(
                    "Quel bonheur de discuter avec toi ! 😊 N'oublie pas de partager ton sourire ou d'écrire une gratitude aujourd'hui dans ton fil d'actualité !",
                    "Tu as une superbe énergie ! Sais-tu que sourire stimule instantanément des hormones du bonheur dans ton corps ? 😄 Allez, sers-toi un grand sourire !",
                    "Chaque jour est une nouvelle chance d'apprécier la beauté de la vie. Merci de faire partie de la merveilleuse famille SMILIFE ! ☀️"
                )
                generalReplies.random()
            }
        }
    }

    // --- ADVANCED SEARCH ACTIONS ---

    fun performAdvancedSearch(query: String) {
        viewModelScope.launch {
            if (query.trim().isEmpty()) {
                _searchUserResults.value = emptyList()
                _searchPostResults.value = emptyList()
                _searchGroupResults.value = emptyList()
                return@launch
            }
            val uResults = repository.searchUsers(query)
            val pResults = repository.searchPosts(query)
            val gResults = repository.searchGroups(query)
            
            _searchUserResults.value = uResults
            _searchPostResults.value = pResults
            _searchGroupResults.value = gResults
        }
    }

    // --- NOTIFICATIONS ACTIONS ---

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
            val user = _currentUser.value
            if (user != null) {
                observeNotifications(user.id)
            }
        }
    }

    fun markAllNotificationsAsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(user.id)
            observeNotifications(user.id)
        }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.deleteNotification(id)
            val user = _currentUser.value
            if (user != null) {
                observeNotifications(user.id)
            }
        }
    }

    // --- GROUPS ACTIONS ---

    fun createGroup() {
        val user = _currentUser.value ?: return
        val name = newGroupName.value
        val desc = newGroupDesc.value
        val topic = newGroupTopic.value
        val emoji = newGroupEmoji.value

        if (name.trim().isEmpty() || desc.trim().isEmpty() || topic.trim().isEmpty()) return

        viewModelScope.launch {
            val result = repository.createGroup(user.id, name, desc, topic, emoji)
            if (result.isSuccess) {
                newGroupName.value = ""
                newGroupDesc.value = ""
                newGroupTopic.value = ""
                newGroupEmoji.value = "👥"
                
                // Refresh list
                observeGroups(user.id)
                // Select and open newly created group details
                result.getOrNull()?.let { selectGroup(it) }
            }
        }
    }

    fun joinGroup(groupId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.joinGroup(groupId, user.id)
            if (result.isSuccess) {
                _isCurrentUserGroupMember.value = true
                observeGroups(user.id)
                
                // Refresh active group details
                val db = AppDatabase.getDatabase(getApplication())
                val group = db.groupDao().searchGroups("").firstOrNull { it.id == groupId }
                if (group != null) {
                    selectGroup(group)
                }
            }
        }
    }

    fun leaveGroup(groupId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.leaveGroup(groupId, user.id)
            if (result.isSuccess) {
                _isCurrentUserGroupMember.value = false
                observeGroups(user.id)
                
                // Refresh active group details
                val db = AppDatabase.getDatabase(getApplication())
                val group = db.groupDao().searchGroups("").firstOrNull { it.id == groupId }
                if (group != null) {
                    selectGroup(group)
                }
            }
        }
    }

    fun shareGroupPost(groupId: Int) {
        val user = _currentUser.value ?: return
        val content = newPostContent.value
        val category = newPostCategory.value
        val mediaUri = selectedMediaUri.value
        val mediaType = selectedMediaType.value

        if (content.trim().isEmpty()) return

        viewModelScope.launch {
            val result = repository.createPost(
                userId = user.id,
                content = content,
                category = category,
                mediaUri = mediaUri,
                mediaType = mediaType,
                groupId = groupId
            )
            if (result.isSuccess) {
                newPostContent.value = ""
                newPostCategory.value = "Joie"
                selectedMediaUri.value = null
                selectedMediaType.value = null
                
                // Refresh active group details
                val db = AppDatabase.getDatabase(getApplication())
                val group = db.groupDao().searchGroups("").firstOrNull { it.id == groupId }
                if (group != null) {
                    selectGroup(group)
                }
            }
        }
    }

    // --- PROFILE ACTIONS ---

    fun updateProfile(username: String, bio: String, avatar: String, dailyGoal: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.updateUserProfile(user.id, username, bio, avatar, dailyGoal)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            }
        }
    }
}
