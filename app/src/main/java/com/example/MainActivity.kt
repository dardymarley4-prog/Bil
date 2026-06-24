package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SmiLifeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SmiLifeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: SmiLifeViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val isLoggedIn = currentUser != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isLoggedIn) {
                SmiLifeBottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { targetScreen -> viewModel.navigateTo(targetScreen) }
                )
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(
                bottom = if (isLoggedIn) innerPadding.calculateBottomPadding() else MaterialTheme.paddingValuesForAuth().calculateBottomPadding()
            )

        when (currentScreen) {
            is Screen.Login -> {
                LoginScreen(
                    viewModel = viewModel,
                    modifier = screenModifier,
                    isRegisterMode = false
                )
            }
            is Screen.Signup -> {
                LoginScreen(
                    viewModel = viewModel,
                    modifier = screenModifier,
                    isRegisterMode = true
                )
            }
            is Screen.Feed -> {
                FeedScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            is Screen.Messaging, is Screen.Chat -> {
                MessagingScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            is Screen.Groups, is Screen.GroupDetails -> {
                GroupsScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            is Screen.Search -> {
                SearchScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            is Screen.Notifications -> {
                NotificationsScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            is Screen.Profile -> {
                ProfileScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
        }
    }
}

@Composable
fun SmiLifeBottomNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // Tab 1: News Feed
        val isFeedActive = currentScreen is Screen.Feed
        NavigationBarItem(
            selected = isFeedActive,
            onClick = { onNavigate(Screen.Feed) },
            icon = {
                Icon(
                    imageVector = if (isFeedActive) Icons.Default.Home else Icons.Outlined.Home,
                    contentDescription = "Fil d'actualité"
                )
            },
            label = { Text("Fil Actu", fontSize = 10.sp) }
        )

        // Tab 2: Communities / Groups
        val isGroupsActive = currentScreen is Screen.Groups || currentScreen is Screen.GroupDetails
        NavigationBarItem(
            selected = isGroupsActive,
            onClick = { onNavigate(Screen.Groups) },
            icon = {
                Icon(
                    imageVector = if (isGroupsActive) Icons.Default.People else Icons.Outlined.PeopleOutline,
                    contentDescription = "Communautés"
                )
            },
            label = { Text("Groupes", fontSize = 10.sp) }
        )

        // Tab 3: Search
        val isSearchActive = currentScreen is Screen.Search
        NavigationBarItem(
            selected = isSearchActive,
            onClick = { onNavigate(Screen.Search) },
            icon = {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Search else Icons.Default.Search,
                    contentDescription = "Recherche"
                )
            },
            label = { Text("Recherche", fontSize = 10.sp) }
        )

        // Tab 4: Notifications
        val isNotificationsActive = currentScreen is Screen.Notifications
        NavigationBarItem(
            selected = isNotificationsActive,
            onClick = { onNavigate(Screen.Notifications) },
            icon = {
                Icon(
                    imageVector = if (isNotificationsActive) Icons.Default.Notifications else Icons.Outlined.Notifications,
                    contentDescription = "Notifications"
                )
            },
            label = { Text("Notifs", fontSize = 10.sp) }
        )

        // Tab 5: User Profile
        val isProfileActive = currentScreen is Screen.Profile
        NavigationBarItem(
            selected = isProfileActive,
            onClick = { onNavigate(Screen.Profile) },
            icon = {
                Icon(
                    imageVector = if (isProfileActive) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                    contentDescription = "Mon Profil"
                )
            },
            label = { Text("Profil", fontSize = 10.sp) }
        )
    }
}

// Extension to cleanly specify zero bottom padding when unauthenticated
@Composable
fun MaterialTheme.paddingValuesForAuth(): PaddingValues {
    return PaddingValues()
}
