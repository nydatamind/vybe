package com.example.vybe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vybe.core.designsystem.DarkSurfaceGlass
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.vybeNeonGlow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale

sealed class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Search : NavItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
    object Library : NavItem("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    object Playlists : NavItem("playlists", "Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic)
}

@Composable
fun VybeBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceGlass,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x3DFFFFFF), Color(0x0AFFFFFF), Color.Transparent)
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            BottomBarTabItem(
                item = NavItem.Home,
                isSelected = currentRoute == NavItem.Home.route,
                onNavigate = onNavigate
            )

            // 2. Search
            BottomBarTabItem(
                item = NavItem.Search,
                isSelected = currentRoute == NavItem.Search.route,
                onNavigate = onNavigate
            )

            // 3. Library
            BottomBarTabItem(
                item = NavItem.Library,
                isSelected = currentRoute == NavItem.Library.route,
                onNavigate = onNavigate
            )

            // 4. Playlists
            BottomBarTabItem(
                item = NavItem.Playlists,
                isSelected = currentRoute == NavItem.Playlists.route,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
private fun BottomBarTabItem(
    item: NavItem,
    isSelected: Boolean,
    onNavigate: (String) -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) VybeEmerald else TextMutedDark,
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextMutedDark,
        animationSpec = tween(durationMillis = 200),
        label = "textColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "iconScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isSelected) onNavigate(item.route)
            }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .scale(iconScale)
        )
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(top = 2.dp)
        )
        // Subtle active indicator pill
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 14.dp, height = 2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) VybeEmerald else Color.Transparent)
        )
    }
}
