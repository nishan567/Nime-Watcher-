package com.example.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.NimeGradientButton
import com.example.domain.model.UserProfile
import com.example.ui.components.UserAvatarView
import com.example.ui.theme.VibrantOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: UserProfile,
    onOpenAvatarGallery: () -> Unit,
    onSaveProfile: (
        firstName: String,
        lastName: String,
        surname: String,
        displayName: String,
        dateOfBirth: String,
        termsAccepted: Boolean
    ) -> Unit,
    onBack: () -> Unit
) {
    var firstName by remember(profile.firstName) { mutableStateOf(profile.firstName) }
    var lastName by remember(profile.lastName) { mutableStateOf(profile.lastName) }
    var surname by remember(profile.surname) { mutableStateOf(profile.surname) }
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var dateOfBirth by remember(profile.dateOfBirth) { mutableStateOf(profile.dateOfBirth) }
    var termsAccepted by remember(profile.termsAccepted) { mutableStateOf(profile.termsAccepted) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            dateOfBirth = fmt.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPolicyDialog) {
        TermsAndPrivacyDialog(onDismiss = { showPolicyDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profile.profileCompleted) "Edit Profile" else "Complete Registration") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_profile_back_button")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("edit_profile_screen")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UserAvatarView(
                        avatarId = profile.avatarId,
                        photoUrl = profile.photoUrl,
                        customAvatarPath = profile.customAvatarPath,
                        size = 76.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anime Profile Avatar",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onOpenAvatarGallery,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("change_avatar_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Choose or Upload Avatar")
                        }
                    }
                }

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it.take(40) },
                    label = { Text("First Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("first_name_input")
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it.take(40) },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("last_name_input")
                )

                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it.take(40) },
                    label = { Text("Surname *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("surname_input")
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(60) },
                    label = { Text("Display Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input")
                )

                OutlinedTextField(
                    value = profile.email,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Verified Account Email") },
                    trailingIcon = {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = "Verified", tint = VibrantOrange)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = dateOfBirth.ifBlank { "Select Date of Birth (Optional)" },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Date of Birth (Private)") },
                        trailingIcon = {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "Pick Date", tint = VibrantOrange)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dob_picker_field")
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = VibrantOrange),
                            modifier = Modifier.testTag("terms_acceptance_checkbox")
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "I accept the Nime Watcher Terms & Privacy Policy",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap to read privacy safeguards and authorized streaming terms",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibrantOrange,
                                modifier = Modifier
                                    .clickable { showPolicyDialog = true }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                validationError?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                NimeGradientButton(
                    text = if (profile.profileCompleted) "Save Profile Changes" else "Complete Registration",
                    onClick = {
                        when {
                            firstName.trim().isEmpty() -> validationError = "Please enter your first name."
                            surname.trim().isEmpty() -> validationError = "Please enter your surname."
                            displayName.trim().length < 2 -> validationError = "Display name must be at least 2 characters."
                            !termsAccepted -> validationError = "Please accept the Terms & Privacy Policy to continue."
                            else -> {
                                validationError = null
                                onSaveProfile(
                                    firstName.trim(),
                                    lastName.trim(),
                                    surname.trim(),
                                    displayName.trim(),
                                    dateOfBirth.trim(),
                                    termsAccepted
                                )
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_profile_details_button"
                )
            }
        }
    }
}

@Composable
fun TermsAndPrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terms & Privacy Policy") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "1. Authorized Content Only",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Nime Watcher streams exclusively licensed, public-domain, or explicitly authorized anime sources.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "2. Privacy & Personal Data",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Your email, full name, and optional date of birth are stored privately under your Firestore user record and are never exposed publicly to other users.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "3. Security & Account Control",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Authentication is secured via Google Identity Services and Firebase Auth. You may edit or permanently delete your account data anytime from Account & Security.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
