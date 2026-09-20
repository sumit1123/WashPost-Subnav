package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

@Composable
fun Alert(title: String, description: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit, showAlert: Boolean, showCancel: Boolean = true) {
    var showDialog by remember { mutableStateOf(false) }

    if (showAlert) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            containerColor = wpdsColors.surface,
            onDismissRequest = {
                onDismiss()
                showDialog = false },
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = wpdsColors.gray0,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = description,
                    color = wpdsColors.gray0,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Button(
                        onClick = {
                            onConfirm()
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = wpdsColors.gray0,
                            contentColor = wpdsColors.gray700
                        )
                    ) {
                        Text(confirmText, color = wpdsColors.gray700, fontFamily = FontFamily(Font(
                            com.wpds.wpds.R.font.franklinitcstd_bold)), fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (showCancel) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                            },
                            shape = RoundedCornerShape(50),
                            border = ButtonDefaults.outlinedButtonBorder(
                                true
                            )
                        ) {
                            Text(
                                stringResource(com.washingtonpost.android.notifications.R.string.cancelLabel),
                                color = wpdsColors.onSurface,
                                fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            dismissButton = {},
            icon = {
            }
        )
    }
}