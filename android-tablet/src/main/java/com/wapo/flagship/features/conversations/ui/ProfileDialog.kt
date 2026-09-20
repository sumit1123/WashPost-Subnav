package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpds.theme.wpdsColors
import com.washingtonpost.android.R
import com.wpds.theme.FranklinItcStandardFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    minCharacters: Int = 6,
    initialUsername: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername ?: "") }
    val trimmedUsername = username.trim()
    val isValid = trimmedUsername.length >= minCharacters
    val hasChanged = trimmedUsername != (initialUsername ?: "")
    val isSaveEnabled = isValid && hasChanged

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = wpdsColors.surface,
        title = {
            Text(
                text = "Choose a Display Name",
                style = TextStyle(
                    fontFamily = FranklinItcStandardFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = wpdsColors.onSurface
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "This is how you'll appear to others in the conversation",
                    style = TextStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = wpdsColors.onSurfaceSubtle
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = {
                        Text(
                            text = "Display Name",
                            color = wpdsColors.onSurfaceSubtle
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontSize = 16.sp,
                        color = wpdsColors.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = wpdsColors.cta,
                        unfocusedBorderColor = wpdsColors.outline,
                        cursorColor = wpdsColors.cta
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp),
                        tint = wpdsColors.onSurfaceSubtle
                    )
                    Text(
                        text = stringResource(R.string.advanced_profile_settings_web),
                        style = TextStyle(
                            fontFamily = FranklinItcStandardFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = wpdsColors.onSurfaceSubtle
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSaveEnabled) {
                        onSave(username.trim())
                    }
                },
                enabled = isSaveEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = wpdsColors.cta,
                    contentColor = wpdsColors.onCta,
                    disabledContainerColor = wpdsColors.outline,
                    disabledContentColor = wpdsColors.onSurfaceSubtle
                )
            ) {
                Text(
                    text = "Save",
                    style = TextStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = wpdsColors.onSurfaceSubtle,
                    style = TextStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontSize = 14.sp
                    )
                )
            }
        }
    )
}
