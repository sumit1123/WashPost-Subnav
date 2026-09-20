package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.ask.models.CategoryItem
import com.wapo.flagship.features.ask.models.QuestionItem
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AskScreenTab(
    categoryItem: CategoryItem,
    isNotTablet: Boolean,
    onQuestionClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(wpdsColors.appBarBg)
    ) {

        categoryItem.questions.forEachIndexed { pos, questionItem ->
            AskScreenQuestion(questionItem, isNotTablet) {
                onQuestionClicked.invoke(it)
            }
            val margin = if (!isNotTablet) 0.dp else 16.dp

            if (pos != categoryItem.questions.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = margin),
                    color = wpdsColors.gray400
                )
            }
        }

    }
}

@Composable
fun AskScreenQuestion(
    questionItem: QuestionItem,
    isNotTablet: Boolean,
    onQuestionClicked: (String) -> Unit
) {
    val margin = if (!isNotTablet) 0.dp else 16.dp
    Row(
        modifier = Modifier
            .padding(start = margin, top = 16.dp, bottom = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .clickable(onClick = {
                questionItem.text?.let {
                    onQuestionClicked.invoke(questionItem.text)
                }
            })
    ) {
        Icon(
            modifier = Modifier,
            painter = painterResource(com.wpds.wpds.R.drawable.ai_icon),
            contentDescription = "atp icon",
            tint = Color.Unspecified,
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = questionItem.text ?: "",
            fontSize = 16.sp,
            color = wpdsColors.gray60,
            fontFamily = FontFamily(Font(com.washingtonpost.android.save.R.font.franklin_std_light))
        )
    }
}
