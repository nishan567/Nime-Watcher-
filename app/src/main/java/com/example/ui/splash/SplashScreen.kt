package com.example.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.ui.NimeBrandMark
import com.example.data.repository.AuthRepository
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authRepository: AuthRepository,
    rememberMeEnabled: Boolean,
    onSplashFinished: (isSignedIn: Boolean) -> Unit
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.72f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 260f),
        label = "splash_logo_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "splash_alpha"
    )

    val glowTransition = rememberInfiniteTransition(label = "orange_glow")
    val glowRadiusFactor by glowTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    LaunchedEffect(Unit) {
        visible = true
        val silentUser = if (rememberMeEnabled) {
            authRepository.attemptSilentGoogleSignIn(context)
        } else {
            null
        }
        delay(1250L)
        onSplashFinished(silentUser != null || authRepository.currentUser != null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.42f * glowRadiusFactor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VibrantOrange.copy(alpha = 0.28f),
                        EmberOrange.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .alpha(contentAlpha)
        ) {
            NimeBrandMark(
                size = 88.dp,
                modifier = Modifier.scale(logoScale)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.brand_wordmark),
                style = MaterialTheme.typography.displayLarge.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = WarmAmber,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            LinearProgressIndicator(
                color = VibrantOrange,
                trackColor = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.loading_session),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}
