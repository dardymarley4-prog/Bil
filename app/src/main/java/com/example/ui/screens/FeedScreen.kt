package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.ui.viewmodel.SmiLifeViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: SmiLifeViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val likedPostIds by viewModel.likedPostIds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val newPostText by viewModel.newPostContent.collectAsState()
    val selectedCategory by viewModel.newPostCategory.collectAsState()

    val activeCommentPostId by viewModel.activeCommentPostId.collectAsState()
    val comments by viewModel.commentsForPost.collectAsState()
    val newCommentText by viewModel.newCommentContent.collectAsState()

    var isComposerExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Aujourd'hui, je souris car...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "SMILIFE ☀️",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Messaging) }) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Messagerie",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Active User Profile Indicator
                    currentUser?.let { user ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.avatarEmoji,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }

            // Scrollable Feed List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Compose Post Card
                item {
                    SmileComposerCard(
                        text = newPostText,
                        onTextChange = { viewModel.newPostContent.value = it },
                        category = selectedCategory,
                        onCategorySelect = { viewModel.newPostCategory.value = it },
                        isExpanded = isComposerExpanded,
                        onExpandChange = { isComposerExpanded = it },
                        onShare = {
                            viewModel.shareSmile()
                            isComposerExpanded = false
                        },
                        currentUserEmoji = currentUser?.avatarEmoji ?: "😊",
                        viewModel = viewModel
                    )
                }

                if (posts.isEmpty()) {
                    item {
                        EmptyFeedState()
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        val isLiked = likedPostIds.contains(post.id)
                        PostCard(
                            post = post,
                            isLiked = isLiked,
                            onLikeToggle = { viewModel.togglePostLike(post) },
                            onCommentClick = { viewModel.openCommentsForPost(post.id) }
                        )
                    }
                }
            }
        }

        // Floating Modal/Drawer Bottom Sheet for Thread Comments
        if (activeCommentPostId != null) {
            ModalCommentsDrawer(
                comments = comments,
                newCommentText = newCommentText,
                onCommentTextChange = { viewModel.newCommentContent.value = it },
                onSubmitComment = { viewModel.submitComment() },
                onDismiss = { viewModel.closeComments() }
            )
        }
    }
}

@Composable
fun SmileComposerCard(
    text: String,
    onTextChange: (String) -> Unit,
    category: String,
    onCategorySelect: (String) -> Unit,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    currentUserEmoji: String,
    viewModel: SmiLifeViewModel
) {
    val categories = listOf("Joie", "Gratitude", "Gentillesse", "Humour", "Défi")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = currentUserEmoji, fontSize = 18.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExpandChange(true) }
                        .padding(vertical = 8.dp)
                ) {
                    if (text.isEmpty() && !isExpanded) {
                        Text(
                            text = "Partagez une raison de sourire...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    } else if (!isExpanded) {
                        Text(
                            text = text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!isExpanded) {
                    IconButton(onClick = { onExpandChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Rédiger",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        placeholder = { Text("Qu'est-ce qui vous apporte de la joie en ce moment ? ✨") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    // Category Selector Row
                    Text(
                        text = "Catégorie du sourire :",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScrollPadding(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            val categoryColor = getCategoryColors(cat)
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelect(cat) },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = categoryColor.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = categoryColor.primary,
                                    containerColor = Color.Transparent,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = categoryColor.primary,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    // Simulated media importer selector
                    val selectedMedia by viewModel.selectedMediaUri.collectAsState()
                    val selectedType by viewModel.selectedMediaType.collectAsState()

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Média :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    Icon(Icons.Default.Clear, contentDescription = "Retirer", tint = Color.Red, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
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

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onExpandChange(false) }) {
                            Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onShare,
                            enabled = text.trim().isNotEmpty(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sourire ! ☀️", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: PostEntity,
    isLiked: Boolean,
    onLikeToggle: () -> Unit,
    onCommentClick: () -> Unit
) {
    val categoryColors = getCategoryColors(post.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            categoryColors.background,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = post.avatarEmoji, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatRelativeTime(post.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Decorative category tag
                Box(
                    modifier = Modifier
                        .background(
                            categoryColors.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.category,
                        color = categoryColors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post Content
            Text(
                text = post.content,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Render media attachment if present
            if (post.mediaUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.mediaType == "IMAGE") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)).padding(16.dp)
                        ) {
                            Text("🖼️", fontSize = 48.sp)
                            Text("Photo partagée", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Un moment magnifique partagé avec le sourire ! 🌅", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
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

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Smile button (Like)
                IconButtonRow(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    label = if (post.smileCount > 0) "${post.smileCount}" else "Sourire",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onLikeToggle
                )

                // Comment button
                IconButtonRow(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = "Commenter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onCommentClick
                )
            }
        }
    }
}

@Composable
fun IconButtonRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalCommentsDrawer(
    comments: List<CommentEntity>,
    newCommentText: String,
    onCommentTextChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sourires partagés (${comments.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Divider()

            // Comments list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "💬", fontSize = 32.sp)
                            Text(
                                text = "Aucun commentaire pour le moment.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Soyez le premier à répondre avec bienveillance !",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    items(comments) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = comment.avatarEmoji, fontSize = 16.sp)
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = comment.username,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = formatRelativeTime(comment.timestamp),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.content,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // Input comment panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = onCommentTextChange,
                    placeholder = { Text("Votre réponse bienveillante...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                IconButton(
                    onClick = onSubmitComment,
                    enabled = newCommentText.trim().isNotEmpty(),
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (newCommentText.trim().isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Envoyer",
                        tint = if (newCommentText.trim().isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFeedState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "🌤️", fontSize = 56.sp)
        Text(
            text = "Le ciel est calme ici.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Partagez la toute première raison de sourire d'aujourd'hui !",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 40.dp),
            textAlign = TextAlign.Center
        )
    }
}

// Helper: Custom utility to support swipeable or scrollable tabs row without adding heavy layout logic
@Composable
fun Modifier.horizontalScrollPadding(): Modifier {
    return this.padding(vertical = 4.dp)
}

// Data holder for categories styling
data class CategoryTheme(val primary: Color, val background: Color)

@Composable
fun getCategoryColors(category: String): CategoryTheme {
    return when (category) {
        "Joie" -> CategoryTheme(Color(0xFFFFA000), Color(0xFFFFF8E1)) // Amber
        "Gratitude" -> CategoryTheme(Color(0xFFE91E63), Color(0xFFFCE4EC)) // Pink
        "Gentillesse" -> CategoryTheme(Color(0xFF0288D1), Color(0xFFE1F5FE)) // Light Blue
        "Humour" -> CategoryTheme(Color(0xFFF57C00), Color(0xFFFFF3E0)) // Orange
        "Défi" -> CategoryTheme(Color(0xFF4CAF50), Color(0xFFE8F5E9)) // Green
        else -> CategoryTheme(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val sec = diff / 1000
    val min = sec / 60
    val hour = min / 60
    val day = hour / 24

    return when {
        sec < 60 -> "À l'instant"
        min < 60 -> "Il y a $min min"
        hour < 24 -> "Il y a $hour h"
        day == 1L -> "Hier"
        else -> "Il y a $day jours"
    }
}
