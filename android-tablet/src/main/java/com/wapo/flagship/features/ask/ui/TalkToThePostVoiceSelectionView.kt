package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.settings.ASK_SAM_JUNIPER_ID
import com.wapo.flagship.features.settings.ASK_SAM_VOICE_OPTIONS
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.wpds.R

@Composable
fun VoiceSelectionView(
    selectedVoice: String?,
    onItemSelected: (String) -> Unit,
    onDoneClicked: () -> Unit
) {
    Card(
        colors = CardColors(
            containerColor = wpdsColors.wallPrimaryBg,
            contentColor = wpdsColors.primary,
            disabledContainerColor = wpdsColors.wallPrimaryBg,
            disabledContentColor = wpdsColors.wallPrimaryBg
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).background(wpdsColors.wallPrimaryBg)) {
            Spacer(modifier = Modifier.height(62.dp))
            Text(
                text = stringResource(R.string.select_voice_style),
                fontSize = 18.sp,
                lineHeight = 22.5.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                color = wpdsColors.gray40,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(36.dp))

            var selectedItem by remember { mutableStateOf(selectedVoice) }
            ASK_SAM_VOICE_OPTIONS.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, wpdsColors.gray300)
                        .clickable {
                            selectedItem = item.id
                            onItemSelected(item.id)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        painter = painterResource(R.drawable.soundwave),
                        contentDescription = "sound wave icon"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = item.displayName,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = wpdsColors.gray40,
                        fontFamily = FontFamily(Font(R.font.franklinitcstd_light))
                    )
                    RadioButton(
                        selected = (item.id == selectedItem),
                        onClick = {
                            selectedItem = item.id
                            onItemSelected(item.id)
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = wpdsColors.primary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        selectedItem?.let {
                            onDoneClicked()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = wpdsColors.gray0,
                        contentColor = wpdsColors.onPrimary,
                        disabledContainerColor = wpdsColors.gray400,
                        disabledContentColor = wpdsColors.gray100
                    ),
                    enabled = selectedItem != null,
                    modifier = Modifier.width(360.dp)
                ) {
                    Text(
                        text = stringResource(R.string.done),
                        fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
                        fontSize = 16.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
@Preview
fun PreviewVoiceSelectionView() {
    AndroidClassicTheme {
        Surface(
            color = Color.Transparent
        ) {
            VoiceSelectionView(ASK_SAM_JUNIPER_ID, {}, {})
        }
    }
}
