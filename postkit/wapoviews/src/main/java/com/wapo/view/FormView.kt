package com.wapo.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wpds.theme.WPDSColors

@Composable
fun FormView(
    showSearch: Boolean,
    searchPlaceHolder: String,
    onSearchTapped: () -> Unit = {}
) {

    Box(
        Modifier
            .padding(2.dp)
    ) {
        if (showSearch) {
            val colors = WPDSColors.current
            OutlinedTextField(
                readOnly = true,
                enabled = false,
                value = "",
                onValueChange = { },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.onSurface.copy(alpha = ContentAlpha.medium)
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = colors.onSurface
                ),
                placeholder = {
                    Text(
                        text = searchPlaceHolder,
                        color = colors.onSurface.copy(alpha = ContentAlpha.medium)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(418.dp)
                    .align(Alignment.Center)
                    .clickable {
                        onSearchTapped()
                    }
                    .background(Color.Unspecified)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormPreview() {
    FormView(showSearch = true, searchPlaceHolder = "Search something")
}