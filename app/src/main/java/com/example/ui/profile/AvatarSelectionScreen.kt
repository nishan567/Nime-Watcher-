package com.example.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ui.NimeGradientButton
import com.example.domain.model.AnimeAvatarCatalog
import com.example.domain.model.AvatarCategory
import com.example.domain.model.UserProfile
import com.example.ui.components.AnimeAvatarCanvas
import com.example.ui.components.UserAvatarView
import com.example.ui.theme.VibrantOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionScreen(
    profile: UserProfile,
    onSaveAvatar: (avatarId: String, customPath: String?) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(AvatarCategory.ALL) }
    var selectedAvatarId by remember(profile.avatarId) { mutableStateOf(profile.avatarId) }
    var customPath by remember(profile.customAvatarPath) { mutableStateOf(profile.customAvatarPath) }
    var pendingCropUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                pendingCropUri = uri
            }
        }
    )

    val filteredAvatars = remember(selectedCategory) {
        if (selectedCategory == AvatarCategory.ALL) {
            AnimeAvatarCatalog.avatars
        } else {
            AnimeAvatarCatalog.avatars.filter { it.category == selectedCategory }
        }
    }

    pendingCropUri?.let { uri ->
        AvatarCropperDialog(
            sourceUri = uri,
            userId = profile.userId,
            onDismiss = { pendingCropUri = null },
            onCroppedSaved = { savedFilePath ->
                pendingCropUri = null
                customPath = savedFilePath
                selectedAvatarId = UserProfile.AVATAR_CUSTOM_PHOTO
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Anime Avatar") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("avatar_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("avatar_selection_screen")
        ) {
            // Preview header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    UserAvatarView(
                        avatarId = selectedAvatarId,
                        photoUrl = profile.photoUrl,
                        customAvatarPath = customPath,
                        size = 76.dp,
                        borderWidth = 3.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        val activeTitle = when (selectedAvatarId) {
                            UserProfile.AVATAR_CUSTOM_PHOTO -> "Custom Cropped Photo"
                            UserProfile.AVATAR_GOOGLE_PHOTO -> "Google Account Photo"
                            else -> AnimeAvatarCatalog.findById(selectedAvatarId).title
                        }
                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Original Nime Watcher artwork or cropped photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("upload_custom_avatar_button")
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Upload & Crop")
                            }
                            if (!profile.photoUrl.isNullOrBlank()) {
                                FilterChip(
                                    selected = selectedAvatarId == UserProfile.AVATAR_GOOGLE_PHOTO,
                                    onClick = { selectedAvatarId = UserProfile.AVATAR_GOOGLE_PHOTO },
                                    label = { Text("Google") },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("use_google_photo_chip")
                                )
                            }
                        }
                    }
                }
            }

            // 8 Category filter row + All
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AvatarCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("avatar_category_${category.id}")
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredAvatars, key = { it.id }) { avatar ->
                    val isSelected = selectedAvatarId == avatar.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { selectedAvatarId = avatar.id }
                            .padding(6.dp)
                            .testTag("avatar_item_${avatar.id}")
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AnimeAvatarCanvas(
                                avatar = avatar,
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) VibrantOrange else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = VibrantOrange,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = avatar.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) VibrantOrange else MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = avatar.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                NimeGradientButton(
                    text = "Save Avatar Selection",
                    onClick = {
                        onSaveAvatar(selectedAvatarId, customPath)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_avatar_selection_button"
                )
            }
        }
    }
}
