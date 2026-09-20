package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.AskThePostUIState
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

@Composable
fun SharedLinksView(
    askThePostUIState: AskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
) {
    val sharedLinks = askThePostUIState.sharedLinks
    var stageDeleteAllSharedLinks by remember { mutableStateOf(false) }
    var stagedDeleteShareId by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize()
            .background(wpdsColors.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(wpdsColors.surface)
        ) {
            Column {
                Text(
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp),
                    text = stringResource(R.string.shared_links_description),
                    fontSize = 14.sp,
                    color = wpdsColors.gray100
                )


                if (sharedLinks.isNotEmpty()) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            stageDeleteAllSharedLinks = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = wpdsColors.onSurface,
                        ),
                        modifier = Modifier
                            .padding(start = 24.dp, bottom = 24.dp)
                            .defaultMinSize(minHeight = 18.dp),
                        border = BorderStroke(1.dp, wpdsColors.gray300),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = "Delete all",
                            modifier = Modifier.size(16.dp),
                            tint = wpdsColors.gray0
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete all links", fontSize = 15.sp, fontFamily = FontFamily(
                                Font(com.wpds.wpds.R.font.franklinitcstd_light),
                            ),
                            color = wpdsColors.gray0
                        )
                    }
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                    ) {
                        items(items = sharedLinks) { sharedLink ->
                            SharedLinkView(sharedLink, {
                                stagedDeleteShareId = sharedLink.shareId
                            }, {
                                askThePostUIEvent.invoke(AskThePostEvent.CopyShareUrl(sharedLink.shareId))
                            })
                        }
                        item {
                            Text(
                                modifier = Modifier.padding(top = 8.dp, start = 24.dp),
                                text = stringResource(R.string.shared_expired_description),
                                fontSize = 14.sp,
                                color = wpdsColors.gray0
                            )
                        }
                    }
                } else {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 24.dp),
                        text = stringResource(R.string.shared_no_links_description),
                        fontSize = 14.sp,
                        color = wpdsColors.gray100
                    )
                }

                Alert(
                    title = if (stageDeleteAllSharedLinks) stringResource(R.string.delete_share_all_title) else stringResource(
                        R.string.delete_share_title
                    ),
                    description = if (stageDeleteAllSharedLinks) stringResource(R.string.delete_share_all_label) else stringResource(
                        R.string.delete_share_label
                    ),
                    confirmText = stringResource(
                        R.string.delete_btn
                    ),
                    onConfirm = {
                        if (stagedDeleteShareId.isNotEmpty()) {
                            askThePostUIEvent.invoke(AskThePostEvent.DeleteShareUrlOrAll(stagedDeleteShareId))
                            stagedDeleteShareId = ""
                        } else if (stageDeleteAllSharedLinks) {
                            askThePostUIEvent.invoke(AskThePostEvent.DeleteShareUrlOrAll(null))
                            stageDeleteAllSharedLinks = false
                        }
                    },
                    onDismiss = {
                        stagedDeleteShareId = ""
                        stageDeleteAllSharedLinks = false
                    },
                    stagedDeleteShareId.isNotEmpty() || stageDeleteAllSharedLinks
                )


            }
        }
    }
}