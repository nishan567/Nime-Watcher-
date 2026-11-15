package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.preferences.UserPreferences
import com.example.core.ui.NimeBrandMark
import com.example.core.ui.NimeGradientButton
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.UserProfileRepository
import com.example.ui.theme.NimeGradients
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber
import kotlinx.coroutines.launch

@Composable
fun WelcomeAuthScreen(
    authRepository: AuthRepository,
    profileRepository: UserProfileRepository,
    preferences: UserPreferences,
    onToggleRememberMe: (Boolean) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_welcome_banner),
                contentDescription = stringResource(R.string.brand_wordmark),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(NimeGradients.darkScrimBottom)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .widthIn(max = 520.dp)
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NimeBrandMark(size = 42.dp)
                    Column {
                        Text(
                            text = stringResource(R.string.brand_wordmark),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmAmber
                        )
                    }
                }

                Spacer(Modifier.height(175.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = VibrantOrange.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(
                            1.dp,
                            VibrantOrange.copy(alpha = 0.45f),
                            RoundedCornerShape(50)
                        )
                    ) {
                        Text(
                            text = "Stage 2 • Identity & Avatars",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmAmber,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.welcome_headline),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )

                    Text(
                        text = stringResource(R.string.welcome_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FeatureMiniPill(
                            icon = Icons.Rounded.PictureInPictureAlt,
                            label = "PiP + Audio",
                            modifier = Modifier.weight(1f)
                        )
                        FeatureMiniPill(
                            icon = Icons.Rounded.HighQuality,
                            label = "16 Avatars",
                            modifier = Modifier.weight(1f)
                        )
                        FeatureMiniPill(
                            icon = Icons.Rounded.Lock,
                            label = "Cloud Sync",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remember Me",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Keep your Google session active on next launch",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                        Switch(
                            checked = preferences.rememberMeEnabled,
                            onCheckedChange = onToggleRememberMe,
                            colors = SwitchDefaults.colors(checkedTrackColor = VibrantOrange),
                            modifier = Modifier.testTag("welcome_remember_me_switch")
                        )
                    }

                    errorMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (isSigningIn) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = VibrantOrange,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else {
                        NimeGradientButton(
                            text = stringResource(R.string.sign_in_google),
                            onClick = {
                                if (activity == null) {
                                    errorMessage = "Unable to access Activity for Google Sign-In."
                                    return@NimeGradientButton
                                }
                                errorMessage = null
                                isSigningIn = true
                                scope.launch {
                                    when (val result = authRepository.signInWithGoogleInteractive(activity)) {
                                        is AuthResult.Success -> {
                                            profileRepository.ensureProfileExists(
                                                user = result.user,
                                                defaultDarkMode = preferences.darkModeEnabled,
                                                defaultLanguage = preferences.language.code
                                            )
                                            isSigningIn = false
                                            onAuthSuccess()
                                        }
                                        AuthResult.Cancelled -> {
                                            isSigningIn = false
                                        }
                                        is AuthResult.Error -> {
                                            isSigningIn = false
                                            errorMessage = result.message
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "google_sign_in_button"
                        )
                    }

                    Text(
                        text = "Secured by Google Play Identity Services & owner-scoped Firestore rules.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureMiniPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VibrantOrange,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
