package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.SmiLifeViewModel
import com.example.data.model.GroupEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserEntity
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: SmiLifeViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Determine if we should show Group Details or the general groups list
    if (currentScreen is Screen.GroupDetails) {
        val group = (currentScreen as Screen.GroupDetails).group
        GroupDetailsScreen(group = group, viewModel = viewModel, modifier = modifier)
    } else {
        GroupsListScreen(viewModel = viewModel, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListScreen(
    viewModel: SmiLifeViewModel,
    modifier: Modifier = Modifier
) {
    val allGroups by viewModel.allGroups.collectAsState()
    val joinedGroups by viewModel.joinedGroups.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Groups Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Communautés 👥",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Partagez et souriez en petit comité",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Créer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Joined Groups Section
            if (joinedGroups.isNotEmpty()) {
                item {
                    Text(
                        text = "Mes communautés 🏠",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(joinedGroups) { group ->
                    GroupRowItem(group, viewModel)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Discover Groups Section
            val discoverableGroups = allGroups.filter { g -> joinedGroups.none { j -> j.id == g.id } }

            item {
                Text(
                    text = "Découvrir de nouveaux horizons 🌍",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (discoverableGroups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Vous avez rejoint toutes les communautés disponibles ! Vous pouvez en créer de nouvelles.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(discoverableGroups) { group ->
                    GroupRowItem(group, viewModel, isDiscoverMode = true)
                }
            }
        }
    }

    // CREATE GROUP DIALOG
    if (showCreateDialog) {
        val name by viewModel.newGroupName.collectAsState()
        val desc by viewModel.newGroupDesc.collectAsState()
        val topic by viewModel.newGroupTopic.collectAsState()
        val emoji by viewModel.newGroupEmoji.collectAsState()

        val emojis = listOf("👥", "🧘‍♀️", "🏃‍♂️", "🤭", "🎨", "🌍", "🐱", "☕", "🚴", "📚", "🌱")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Créer une communauté 👥", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.newGroupName.value = it },
                        label = { Text("Nom du groupe") },
                        placeholder = { Text("Ex: Les adeptes du réveil matinal") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = topic,
                        onValueChange = { viewModel.newGroupTopic.value = it },
                        label = { Text("Thématique / Sujet principal") },
                        placeholder = { Text("Ex: Motivation, Yoga, Humour, Sport") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { viewModel.newGroupDesc.value = it },
                        label = { Text("Description") },
                        placeholder = { Text("Une brève phrase pour donner envie de rejoindre...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3
                    )

                    // Emoji Picker
                    Column {
                        Text("Icône du groupe :", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(emojis) { itemEmoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (emoji == itemEmoji) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.newGroupEmoji.value = itemEmoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(itemEmoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createGroup()
                        showCreateDialog = false
                    },
                    enabled = name.trim().isNotEmpty() && desc.trim().isNotEmpty() && topic.trim().isNotEmpty()
                ) {
                    Text("Créer le groupe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun GroupRowItem(
    group: GroupEntity,
    viewModel: SmiLifeViewModel,
    isDiscoverMode: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectGroup(group) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = group.avatarEmoji, fontSize = 26.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Thème : ${group.topic}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = group.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(onClick = { viewModel.selectGroup(group) }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ouvrir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- INDIVIDUAL GROUP FEED & MEMBERS DETAILS SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    group: GroupEntity,
    viewModel: SmiLifeViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.activeGroupPosts.collectAsState()
    val members by viewModel.activeGroupMembers.collectAsState()
    val isMember by viewModel.isCurrentUserGroupMember.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Feed, 1: Members Info
    var showAddPostForm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper back navigation bar with Group info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Groups) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(group.avatarEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${members.size} membre(s) • Sujet : ${group.topic}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Join/Leave action button
                    if (isMember) {
                        OutlinedButton(
                            onClick = { viewModel.leaveGroup(group.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Quitter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.joinGroup(group.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Rejoindre ➕", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = group.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Subtabs (Publications vs Membres)
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Fil d'actualité (${posts.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Membres (${members.size})", fontWeight = FontWeight.SemiBold) }
            )
        }

        if (activeTab == 0) {
            // Group posts feed
            Box(modifier = Modifier.weight(1f)) {
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✨", fontSize = 40.sp)
                            Text(
                                text = "Aucun sourire partagé pour l'instant.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (isMember) {
                                Text(
                                    text = "Soyez le premier à illuminer cette communauté !",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showAddPostForm = true },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Partager un sourire ☀️")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isMember && !showAddPostForm) {
                            item {
                                OutlinedButton(
                                    onClick = { showAddPostForm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Partager un sourire dans ce groupe...")
                                    }
                                }
                            }
                        }

                        items(posts) { post ->
                            GroupPostItem(post, viewModel)
                        }
                    }
                }

                // Inline form to write inside group
                androidx.compose.animation.AnimatedVisibility(
                    visible = showAddPostForm && isMember,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val content by viewModel.newPostContent.collectAsState()
                    val category by viewModel.newPostCategory.collectAsState()
                    val categories = listOf("Gratitude", "Joie", "Gentillesse", "Humour", "Défi")

                    // Media upload simulated support
                    val selectedMedia by viewModel.selectedMediaUri.collectAsState()
                    val selectedType by viewModel.selectedMediaType.collectAsState()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Nouveau Post de Groupe ☀️", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                IconButton(onClick = { showAddPostForm = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                                }
                            }

                            // Content field
                            OutlinedTextField(
                                value = content,
                                onValueChange = { viewModel.newPostContent.value = it },
                                placeholder = { Text("Écrivez votre message positif...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4
                            )

                            // Category scroll selection
                            Column {
                                Text("Catégorie :", fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories) { cat ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { viewModel.newPostCategory.value = cat }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                color = if (category == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Simulated media importer selector (User requests: scrollable feed of photos/videos importable by user from device)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Média :", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (selectedMedia != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (selectedType == "IMAGE") "🖼️ Photo importée" else "🎥 Vidéo importée",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        IconButton(onClick = {
                                            viewModel.selectedMediaUri.value = null
                                            viewModel.selectedMediaType.value = null
                                        }, modifier = Modifier.size(18.dp)) {
                                            Icon(Icons.Default.Clear, contentDescription = "Retirer le média", tint = Color.Red, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            // Simulated local media library selection
                                            viewModel.selectedMediaUri.value = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=500&q=80"
                                            viewModel.selectedMediaType.value = "IMAGE"
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Text("Importer Photo 🖼️", fontSize = 10.sp)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.selectedMediaUri.value = "https://assets.mixkit.co/videos/preview/mixkit-waves-breaking-in-the-ocean-1527-large.mp4"
                                            viewModel.selectedMediaType.value = "VIDEO"
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Text("Importer Vidéo 🎥", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            // Submit row
                            Button(
                                onClick = {
                                    viewModel.shareGroupPost(group.id)
                                    showAddPostForm = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = content.trim().isNotEmpty()
                            ) {
                                Text("Publier dans le groupe 🚀", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Group Members List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(members) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(member.avatarEmoji, fontSize = 20.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(member.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }

                            IconButton(onClick = { viewModel.selectChat(member) }) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupPostItem(post: PostEntity, viewModel: SmiLifeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = post.avatarEmoji, fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Catégorie : ${post.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Post content
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Render attachment if present (User media request: scrollable feed of photos/videos importable by user)
            if (post.mediaUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Render Image placeholder
                    if (post.mediaType == "IMAGE") {
                        // Display beautiful full container graphic placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)).padding(16.dp)
                        ) {
                            Text("🖼️", fontSize = 48.sp)
                            Text("Photo partagée", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Coucher de soleil & Ondes positives 🌅", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // Display video graphic placeholder with play button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)).padding(16.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(54.dp))
                            Text("Vidéo partagée 🎥", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("Prenez une pause respiratoire positive 🌊", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.togglePostLike(post) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Smile Like",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${post.smileCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = { viewModel.openCommentsForPost(post.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Commenter",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
