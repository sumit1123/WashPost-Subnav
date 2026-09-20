package com.wapo.flagship.features.audio.fragments

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.audio.R

/**
 * A Composable that renders text with custom underlining specifically for "Opinions" style labels.
 * It replicates the logic from WpTextUnderlineSpan.
 */
@Composable
fun OpinionsLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = colorResource(id = R.color.podcast_text_color),
    underlineColor: Color = colorResource(id = com.wpds.wpds.R.color.opinion_spark),
    underlineHeight: Float = 1.5f // In dp, will be converted to Px in drawBehind
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    val firstPartLeftPadding = integerResource(id = com.wapo.view.R.integer.first_part_opinion_left_padding_underline).toFloat()
    val firstPartRightPadding = integerResource(id = com.wapo.view.R.integer.first_part_opinion_right_padding_underline).toFloat()
    val secondPartLeftPadding = integerResource(id = com.wapo.view.R.integer.second_part_opinion_left_padding_underline).toFloat()
    val secondPartRightPadding = integerResource(id = com.wapo.view.R.integer.second_part_opinion_right_padding_underline).toFloat()
    
    // We use drawBehind to manually draw the rectangles based on the text layout
    Text(
        text = text,
        modifier = modifier
            .padding(bottom = 2.dp) // Give some space for the underline
            .drawBehind {
                textLayoutResult?.let { layout ->
                    if (text.isEmpty()) return@let
                    
                    // First part: index 0 to 1
                    if (text.length >= 1) {
                        val startOffset = layout.getHorizontalPosition(0, true)
                        val endOffset = layout.getHorizontalPosition(1, true)
                        val lineY = layout.getLineBottom(0) - 4f // Replicating -4f offset
                        
                        drawRect(
                            color = underlineColor,
                            topLeft = Offset(startOffset + firstPartLeftPadding, lineY),
                            size = Size(endOffset - startOffset + firstPartRightPadding - firstPartLeftPadding, underlineHeight.dp.toPx())
                        )
                    }
                    
                    // Second part: index 2 to end
                    if (text.length >= 3) {
                        val startOffset = layout.getHorizontalPosition(2, true)
                        val endOffset = layout.getHorizontalPosition(text.length, true)
                        val lineY = layout.getLineBottom(0) - 4f
                        
                        drawRect(
                            color = underlineColor,
                            topLeft = Offset(startOffset + secondPartLeftPadding, lineY),
                            size = Size(endOffset - startOffset + secondPartRightPadding - secondPartLeftPadding, underlineHeight.dp.toPx())
                        )
                    }
                }
            },
        color = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        onTextLayout = { textLayoutResult = it }
    )
}
