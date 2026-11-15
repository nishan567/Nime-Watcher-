package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.AnimeAvatar
import com.example.domain.model.AnimeAvatarCatalog
import com.example.domain.model.AvatarCategory
import com.example.domain.model.UserProfile
import com.example.ui.theme.VibrantOrange
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun UserAvatarView(
    avatarId: String,
    photoUrl: String?,
    customAvatarPath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    borderColor: Color = VibrantOrange,
    borderWidth: Dp = 2.dp
) {
    val customFile = remember(customAvatarPath) {
        customAvatarPath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .background(Color(0xFF14161D))
    ) {
        when {
            avatarId == UserProfile.AVATAR_CUSTOM_PHOTO && customFile != null -> {
                AsyncImage(
                    model = customFile,
                    contentDescription = "Custom profile avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            avatarId == UserProfile.AVATAR_GOOGLE_PHOTO && !photoUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Google profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val preset = remember(avatarId) { AnimeAvatarCatalog.findById(avatarId) }
                AnimeAvatarCanvas(
                    avatar = preset,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AnimeAvatarCanvas(
    avatar: AnimeAvatar,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(avatar.bgStart, avatar.bgEnd),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        // Subtle atmospheric aura ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(avatar.accent.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.42f)
        )

        when (avatar.category) {
            AvatarCategory.MALE -> drawMaleCharacter(avatar, w, h)
            AvatarCategory.FEMALE -> drawFemaleCharacter(avatar, w, h)
            AvatarCategory.FANTASY -> drawFantasyCharacter(avatar, w, h)
            AvatarCategory.SAMURAI -> drawSamuraiCharacter(avatar, w, h)
            AvatarCategory.NINJA -> drawNinjaCharacter(avatar, w, h)
            AvatarCategory.CUTE -> drawCuteCharacter(avatar, w, h)
            AvatarCategory.DARK -> drawDarkCharacter(avatar, w, h)
            AvatarCategory.MINIMAL -> drawMinimalEmblem(avatar, w, h)
            AvatarCategory.ALL -> drawSamuraiCharacter(avatar, w, h)
        }
    }
}

private fun DrawScope.drawShouldersAndFace(w: Float, h: Float, cloakColor: Color, skinColor: Color) {
    val cloak = Path().apply {
        moveTo(w * 0.16f, h)
        quadraticTo(w * 0.25f, h * 0.68f, w * 0.5f, h * 0.68f)
        quadraticTo(w * 0.75f, h * 0.68f, w * 0.84f, h)
        close()
    }
    drawPath(cloak, color = cloakColor)
    drawCircle(color = skinColor, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.46f))
}

private fun DrawScope.drawAnimeEyes(w: Float, h: Float, irisColor: Color) {
    drawOval(color = irisColor, topLeft = Offset(w * 0.37f, h * 0.44f), size = Size(w * 0.08f, h * 0.055f))
    drawOval(color = irisColor, topLeft = Offset(w * 0.55f, h * 0.44f), size = Size(w * 0.08f, h * 0.055f))
    drawCircle(color = Color.White, radius = w * 0.014f, center = Offset(w * 0.40f, h * 0.46f))
    drawCircle(color = Color.White, radius = w * 0.014f, center = Offset(w * 0.58f, h * 0.46f))
}

private fun DrawScope.drawMaleCharacter(avatar: AnimeAvatar, w: Float, h: Float) {
    drawShouldersAndFace(w, h, Color(0xFF151722), Color(0xFFF5D0B8))
    // Collar accent
    drawLine(avatar.accent, Offset(w * 0.34f, h * 0.72f), Offset(w * 0.5f, h * 0.84f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    drawLine(avatar.accent, Offset(w * 0.66f, h * 0.72f), Offset(w * 0.5f, h * 0.84f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    // Spiky anime hair
    val hair = Path().apply {
        moveTo(w * 0.27f, h * 0.44f)
        lineTo(w * 0.22f, h * 0.26f)
        lineTo(w * 0.36f, h * 0.30f)
        lineTo(w * 0.44f, h * 0.14f)
        lineTo(w * 0.55f, h * 0.26f)
        lineTo(w * 0.68f, h * 0.18f)
        lineTo(w * 0.74f, h * 0.34f)
        lineTo(w * 0.80f, h * 0.42f)
        quadraticTo(w * 0.52f, h * 0.34f, w * 0.27f, h * 0.44f)
        close()
    }
    drawPath(hair, color = avatar.accent)
    drawAnimeEyes(w, h, Color(0xFF1B1F30))
}

private fun DrawScope.drawFemaleCharacter(avatar: AnimeAvatar, w: Float, h: Float) {
    // Flowing back hair
    drawOval(color = avatar.accent.copy(alpha = 0.85f), topLeft = Offset(w * 0.22f, h * 0.20f), size = Size(w * 0.56f, h * 0.60f))
    drawShouldersAndFace(w, h, Color(0xFF241B2F), Color(0xFFF9D8C6))
    // Front bangs
    val bangs = Path().apply {
        moveTo(w * 0.28f, h * 0.40f)
        quadraticTo(w * 0.5f, h * 0.16f, w * 0.72f, h * 0.40f)
        lineTo(w * 0.60f, h * 0.36f)
        lineTo(w * 0.50f, h * 0.42f)
        lineTo(w * 0.38f, h * 0.36f)
        close()
    }
    drawPath(bangs, color = avatar.accent)
    // Hairpin / crescent ornament
    drawCircle(color = avatar.secondary, radius = w * 0.045f, center = Offset(w * 0.68f, h * 0.29f))
    drawAnimeEyes(w, h, Color(0xFF3E1F47))
}

private fun DrawScope.drawFantasyCharacter(avatar: AnimeAvatar, w: Float, h: Float) {
    // Magic halo
    drawCircle(
        color = avatar.secondary.copy(alpha = 0.55f),
        radius = w * 0.34f,
        center = Offset(w * 0.5f, h * 0.43f),
        style = Stroke(width = w * 0.025f)
    )
    // Elven ears or horns
    val leftHorn = Path().apply {
        moveTo(w * 0.30f, h * 0.38f)
        lineTo(w * 0.14f, h * 0.22f)
        lineTo(w * 0.34f, h * 0.30f)
        close()
    }
    val rightHorn = Path().apply {
        moveTo(w * 0.70f, h * 0.38f)
        lineTo(w * 0.86f, h * 0.22f)
        lineTo(w * 0.66f, h * 0.30f)
        close()
    }
    drawPath(leftHorn, color = avatar.accent)
    drawPath(rightHorn, color = avatar.accent)
    drawShouldersAndFace(w, h, Color(0xFF1A2628), Color(0xFFF3D5C0))
    // Crystal forehead gem
    val crystal = Path().apply {
        moveTo(w * 0.5f, h * 0.28f)
        lineTo(w * 0.55f, h * 0.34f)
        lineTo(w * 0.5f, h * 0.40f)
        lineTo(w * 0.45f, h * 0.34f)
        close()
    }
    drawPath(crystal, color = avatar.accent)
    drawAnimeEyes(w, h, avatar.accent)
}

private fun DrawScope.drawSamuraiCharacter(avatar: AnimeAvatar, w: Float, h: Float) {
    drawShouldersAndFace(w, h, Color(0xFF1C1618), Color(0xFFEBC3AB))
    // Kabuto helmet dome
    drawArc(
        color = Color(0xFF28222B),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(w * 0.22f, h * 0.18f),
        size = Size(w * 0.56f, h * 0.42f)
    )
    // Crescent Kabuto crest
    val crest = Path().apply {
        moveTo(w * 0.5f, h * 0.27f)
        lineTo(w * 0.33f, h * 0.11f)
        lineTo(w * 0.47f, h * 0.21f)
        lineTo(w * 0.5f, h * 0.13f)
        lineTo(w * 0.53f, h * 0.21f)
        lineTo(w * 0.67f, h * 0.11f)
        close()
    }
    drawPath(crest, color = avatar.secondary)
    // Glowing eyes under helmet visor
    drawLine(avatar.accent, Offset(w * 0.36f, h * 0.46f), Offset(w * 0.44f, h * 0.47f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
    drawLine(avatar.accent, Offset(w * 0.64f, h * 0.46f), Offset(w * 0.56f, h * 0.47f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
}

private fun DrawScope.drawNinjaCharacter(avatar: AnimeAvatar, w: Float, h: Float) {
    drawShouldersAndFace(w, h, Color(0xFF161922), Color(0xFFF0C9B1))
    // Ninja hood & lower mask
    drawArc(
        color = Color(0xFF1F2330),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(w * 0.26f, h * 0.20f),
        size = Size(w * 0.48f, h * 0.40f)
    )
    drawRoundRect(
        color = Color(0xFF232736),
        topLeft = Offset(w * 0.29f, h * 0.50f),
        size = Size(w * 0.42f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
    )
    // Headband + metallic plate
    drawRect(color = avatar.accent, topLeft = Offset(w * 0.28f, h * 0.34f), size = Size(w * 0.44f, h * 0.075f))
    drawRoundRect(
        color = avatar.secondary,
        topLeft = Offset(w * 0.41f, h * 0.345f),
        size = Size(w * 0.18f, h * 0.065f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawAnimeEyes(w, h, avatar.accent)
}

private fun DrawScope.drawCuteCharacter(avatar: AnimeAvatar, w: Float, h: Float) {
    // Cat / Kitsune ears
    val leftEar = Path().apply {
        moveTo(w * 0.28f, h * 0.38f)
        lineTo(w * 0.24f, h * 0.16f)
        lineTo(w * 0.42f, h * 0.28f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(w * 0.72f, h * 0.38f)
        lineTo(w * 0.76f, h * 0.16f)
        lineTo(w * 0.58f, h * 0.28f)
        close()
    }
    drawPath(leftEar, color = avatar.accent)
    drawPath(rightEar, color = avatar.accent)
    drawShouldersAndFace(w, h, avatar.accent.copy(alpha = 0.8f), Color(0xFFFFE0CE))
    // Blush cheeks
    drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.65f), radius = w * 0.045f, center = Offset(w * 0.35f, h * 0.53f))
    drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.65f), radius = w * 0.045f, center = Offset(w * 0.65f, h * 0.53f))
    drawAnimeEyes(w, h, Color(0xFF4A2511))
}

private fun DrawScope.drawDarkCharacter(avatar: AnimeAvatar, w: Float, h: Float) {
    // Eclipse moon behind head
    drawCircle(color = avatar.accent.copy(alpha = 0.85f), radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.38f))
    drawCircle(color = avatar.bgStart, radius = w * 0.23f, center = Offset(w * 0.55f, h * 0.35f))
    drawShouldersAndFace(w, h, Color(0xFF0E0F14), Color(0xFFD8D6E0))
    // Half Oni mask or shadow veil
    val mask = Path().apply {
        moveTo(w * 0.5f, h * 0.42f)
        lineTo(w * 0.70f, h * 0.46f)
        lineTo(w * 0.64f, h * 0.66f)
        lineTo(w * 0.5f, h * 0.62f)
        close()
    }
    drawPath(mask, color = avatar.accent)
    // Glowing eye
    drawCircle(color = avatar.accent, radius = w * 0.036f, center = Offset(w * 0.40f, h * 0.47f))
}

private fun DrawScope.drawMinimalEmblem(avatar: AnimeAvatar, w: Float, h: Float) {
    // Rising sun disc
    drawCircle(color = avatar.accent, radius = w * 0.24f, center = Offset(w * 0.5f, h * 0.45f))
    if (avatar.id == "minimal_torii") {
        // Minimalist Torii gate silhouette
        drawLine(Color(0xFF141114), Offset(w * 0.24f, h * 0.34f), Offset(w * 0.76f, h * 0.34f), strokeWidth = w * 0.055f, cap = StrokeCap.Round)
        drawLine(Color(0xFF141114), Offset(w * 0.30f, h * 0.45f), Offset(w * 0.70f, h * 0.45f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
        drawLine(Color(0xFF141114), Offset(w * 0.36f, h * 0.34f), Offset(w * 0.34f, h * 0.80f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
        drawLine(Color(0xFF141114), Offset(w * 0.64f, h * 0.34f), Offset(w * 0.66f, h * 0.80f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
    } else {
        // Minimalist 5-petal geometric sakura emblem
        val cx = w * 0.5f
        val cy = h * 0.45f
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72 - 90).toDouble())
            val px = cx + (w * 0.14f * cos(angle)).toFloat()
            val py = cy + (w * 0.14f * sin(angle)).toFloat()
            drawCircle(color = avatar.secondary, radius = w * 0.085f, center = Offset(px, py))
        }
        drawCircle(color = avatar.bgStart, radius = w * 0.055f, center = Offset(cx, cy))
    }
}
