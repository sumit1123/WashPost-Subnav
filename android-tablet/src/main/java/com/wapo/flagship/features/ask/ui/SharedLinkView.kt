package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.Utils
import com.wapo.android.commons.util.getDaysUntilExpiryIfNearExpiration
import com.wapo.android.commons.util.toDateLong
import com.wapo.flagship.features.aixp.models.AskThePostShare
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val SHARE_LINK_EXPIRY_DAYS = 30L
const val EXPIRY_WARNING_THRESHOLD_DAYS = 5L

@Composable
fun SharedLinkView(
    askThePostShare: AskThePostShare,
    onDelete: () -> Unit = {},
    onCopy: () -> Unit = {},
) {
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.wrapContentHeight()
            .background(if (isSystemInDarkTheme()) wpdsColors.gray400Static else wpdsColors.gray700Static)
    ) {
        Row(modifier = Modifier.padding(vertical = 16.dp)) {

            Column(modifier = Modifier.padding(start = 24.dp)) {
                Text(
                    modifier = Modifier
                        .padding(bottom = 2.dp),
                    text = askThePostShare.conversationTitle ?: "",
                    fontSize = 18.sp,
                    color = wpdsColors.gray0
                )


                askThePostShare.shareCreatedDate?.let {
                    val dateLong = toDateLong(it)
                    val format =
                        stringResource(com.wapo.flagship.features.audio.R.string.date_format2)
                    val dateText = Utils.getDate(format, dateLong)

                    val daysInBetween =
                        getDaysUntilExpiryIfNearExpiration(it, EXPIRY_WARNING_THRESHOLD_DAYS, SHARE_LINK_EXPIRY_DAYS)

                    val expiredText = if (daysInBetween != null) {
                        val dayOrDays = if (daysInBetween == 1L) "day" else "days"
                        "Link expires in $daysInBetween $dayOrDays"
                    } else ""

                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = dateText ?: "",
                        fontSize = 14.sp,
                        color = wpdsColors.gray0
                    )
                    if (daysInBetween != null) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = wpdsColors.gray500,
                                    shape = RoundedCornerShape(4.dp) // Set your desired corner radius here
                                ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                                painter = painterResource(com.wpds.wpds.R.drawable.time),
                                contentDescription = "time icon"
                            )
                            Text(
                                modifier = Modifier
                                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                                text = expiredText,
                                fontSize = 14.sp,
                                color = wpdsColors.gray0
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.wrapContentWidth()
                    .align(Alignment.CenterVertically),
            ) {
                IconButton(
                    onClick = {
                        onCopy.invoke()
                        scope.launch {
                            isCopied = true
                            delay(3000L)
                            isCopied = false
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = if (isCopied) com.wapo.view.R.drawable.icon_save_checkmark else com.wapo.view.R.drawable.ic_copy),
                        contentDescription = "Copy Text",
                        modifier = Modifier.size(18.dp),
                        tint = wpdsColors.gray80
                    )
                }

                IconButton(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = {
                        onDelete.invoke()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trash),
                        contentDescription = "Copy Text",
                        modifier = Modifier.size(18.dp),
                        tint = wpdsColors.gray80,
                    )
                }
            }
        }

        Divider(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .align(Alignment.BottomCenter),
            color = wpdsColors.gray400,
            thickness = 1.dp,
        )
    }
}