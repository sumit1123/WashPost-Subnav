package com.wapo.flagship.features.find.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.find.events.FindClickEvent
import com.wapo.flagship.features.find.model.HeaderItem
import com.wapo.flagship.features.find.model.HighlightBoxType
import com.wapo.flagship.features.find.model.HighlightItem
import com.wapo.flagship.features.find.model.SectionBarItem
import com.wapo.flagship.features.find.model.SectionBoxItem
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

@Composable
@Preview
fun Header(headerItem: HeaderItem = HeaderItem("Recents")) {
    Text(
        modifier =
            Modifier
                .padding(start = 5.dp, end = 5.dp, top = 20.dp, bottom = 12.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
        text = headerItem.title,
        style =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = wpdsColors.primary,
            ),
    )
}

@Composable
@Preview
fun SectionBox(
    sectionBoxItem: SectionBoxItem = SectionBoxItem("Politics", ""),
    onClick: (FindClickEvent) -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .padding(vertical = 5.dp, horizontal = 5.dp)
                .defaultMinSize(minHeight = 60.dp)
                .fillMaxWidth()
                .background(color = wpdsColors.findTile, shape = RoundedCornerShape(4.dp))
                .border(color = wpdsColors.findBorder, width = 1.dp, shape = RoundedCornerShape(4.dp))
                .clickable {
                    onClick(
                        FindClickEvent.SectionClick(
                            sectionBoxItem.bundleId,
                            sectionBoxItem.navType,
                        ),
                    )
                },
    ) {
        Text(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            text = sectionBoxItem.title,
            style = TextStyle(color = wpdsColors.onSecondary, fontSize = 14.sp),
        )
    }
}

@Composable
@Preview
fun HighlightBox(
    highlightItem: HighlightItem =
        HighlightItem(
            "Print Edition",
            "print_edition",
            R.drawable.find_print,
            Color.White,
            HighlightBoxType.PRINT,
            "",
            true,
        ),
    onClick: (FindClickEvent) -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .padding(5.dp)
                .defaultMinSize(minHeight = 80.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    color = highlightItem.highlightBoxType.color,
                    shape = RoundedCornerShape(4.dp),
                ).clickable {
                    onClick(FindClickEvent.HighlightClick(highlightItem.highlightBoxType))
                },
    ) {
        if (highlightItem.newLabel == true) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                label(modifier = Modifier.padding(start = 10.dp, bottom = 8.dp))
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 30.dp),
                    text = highlightItem.title,
                    style = TextStyle(color = highlightItem.textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                )
            }
        } else {
            Text(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 65.dp, top = 8.dp),
                text = highlightItem.title,
                style = TextStyle(color = highlightItem.textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            )
        }
        Image(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .height(80.dp)
                    .width(62.dp),
            painter = painterResource(highlightItem.imageId),
            contentDescription = "",
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun label(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    textColor: Color = Color.Blue,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(backgroundColor)
                .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = "NEW",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview
@Composable
fun SectionBar(
    sectionBarItem: SectionBarItem = SectionBarItem("Test", ""),
    onClick: (FindClickEvent) -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .padding(horizontal = 5.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clickable {
                    onClick(
                        FindClickEvent.SectionClick(
                            sectionBarItem.bundleId,
                            sectionBarItem.navType,
                        ),
                    )
                },
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = sectionBarItem.title,
            style = TextStyle(color = wpdsColors.gray40),
        )
        if (sectionBarItem.hasDivider) {
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = wpdsColors.gray400)
                        .align(Alignment.BottomStart),
            )
        }
    }
}
