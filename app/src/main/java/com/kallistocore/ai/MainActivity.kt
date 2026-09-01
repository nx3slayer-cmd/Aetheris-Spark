package com.kallistocore.ai

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kallistocore.ai.ui.CompactFloatingOverlay
import com.kallistocore.ai.ui.screens.ChatScreen
import com.kallistocore.ai.ui.screens.ImageStudioScreen
import com.kallistocore.ai.ui.screens.SettingsScreen
import com.kallistocore.ai.ui.screens.VoiceStudioScreen
import com.kallistocore.ai.ui.theme.KallistoTheme
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel
import com.kallistocore.ai.ui.viewmodel.MainTab

class MainActivity : ComponentActivity() {

    private val viewModel: CompanionViewModel by viewModels()
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()

            KallistoTheme(themeSetting = currentTheme) {
                val colors = LocalKallistoColors.current
                val currentTab by viewModel.currentTab.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                ) {
                    if (isInPipMode) {
                        // Render Compact Floating Mode when running in Picture-in-Picture
                        CompactFloatingOverlay(
                            onExpand = {
                                // Exiting PiP restores full layout
                            }
                        )
                    } else {
                        // Render Main Full-Screen Bento Scaffold
                        Scaffold(
                            containerColor = colors.background,
                            topBar = {
                                MasterTopBar(
                                    onEnterFloatingMode = { triggerPictureInPicture() }
                                )
                            },
                            bottomBar = {
                                MasterBentoBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = { viewModel.selectTab(it) }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "tab_navigation"
                                ) { targetTab ->
                                    when (targetTab) {
                                        MainTab.CHAT -> ChatScreen(viewModel = viewModel)
                                        MainTab.VOICE -> VoiceStudioScreen(viewModel = viewModel)
                                        MainTab.IMAGE_STUDIO -> ImageStudioScreen(viewModel = viewModel)
                                        MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}

@Composable
fun MasterTopBar(onEnterFloatingMode: () -> Unit) {
    val colors = LocalKallistoColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.statusSuccess)
            )
            Column {
                Text(
                    text = "Kallisto Core Local AI",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Llama 3.2 • Kokoro TTS • 1GB Memory",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }

        IconButton(
            onClick = onEnterFloatingMode,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Rounded.PictureInPictureAlt,
                contentDescription = "Floating Window",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun MasterBentoBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    val colors = LocalKallistoColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(30.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BentoNavigationPill(
                label = "Chat",
                icon = Icons.Rounded.ChatBubbleOutline,
                isSelected = currentTab == MainTab.CHAT,
                onClick = { onTabSelected(MainTab.CHAT) }
            )
            BentoNavigationPill(
                label = "Voice",
                icon = Icons.Rounded.GraphicEq,
                isSelected = currentTab == MainTab.VOICE,
                onClick = { onTabSelected(MainTab.VOICE) }
            )
            BentoNavigationPill(
                label = "Studio",
                icon = Icons.Rounded.Brush,
                isSelected = currentTab == MainTab.IMAGE_STUDIO,
                onClick = { onTabSelected(MainTab.IMAGE_STUDIO) }
            )
            BentoNavigationPill(
                label = "Settings",
                icon = Icons.Rounded.Tune,
                isSelected = currentTab == MainTab.SETTINGS,
                onClick = { onTabSelected(MainTab.SETTINGS) }
            )
        }
    }
}

@Composable
fun BentoNavigationPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKallistoColors.current
    val background = if (isSelected) colors.primary else Color.Transparent
    val contentColor = if (isSelected) colors.onPrimary else colors.textSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
        if (isSelected) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}
