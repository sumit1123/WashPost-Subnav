package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.models.PaywallMessageItem
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

class TilesView (
    context: Context,
) : FrameLayout(context) {

    private val composeView = ComposeView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    init {
        addView(composeView)
    }

    @OptIn(ExperimentalLayoutApi::class)
    fun init(
        getSubtitle: (Product) -> String?,
        products: List<Product>,
        default: Int?,
        getPrice: (Product) -> String?,
        onSelectionChanged: (product: Product?, isChecked: Boolean, index: Int) -> Unit
    ) {
        composeView.setContent {
            AndroidClassicTheme {
                var selectedOption by remember { mutableStateOf(default ?: 0) }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    products.forEachIndexed { index, product ->
                        val cardIndex = index

                        val label = when (product) {
                            is Product.IapProduct -> product.name
                            is Product.ExternalProduct -> product.tileLabel
                            is Product.Registration -> product.tileLabel
                        }
                        val title = when (product) {
                            is Product.IapProduct -> getPrice(product)
                            is Product.ExternalProduct -> product.tileTitle
                            is Product.Registration -> product.tileTitle
                        }
                        val caption = when (product) {
                            is Product.IapProduct -> getSubtitle(product)
                            is Product.ExternalProduct -> product.tileCaption
                            is Product.Registration -> product.tileCaption
                        }
                        ToggleCard(
                            title = label,
                            price = title,
                            subtitle = caption,
                            isSelected = selectedOption == cardIndex,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxRowHeight(),
                            onClick = {
                                selectedOption = cardIndex
                                // RegistrationProduct has its own text/terms/button data,
                                // so it's treated like any other selectable product here.
                                onSelectionChanged(product, true, cardIndex)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleCard(
    title: String?,
    price: String?,
    subtitle: String?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) wpdsColors.blue100Static else wpdsColors.subtle,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = if (isSelected) 4.dp else 0.dp,
        backgroundColor = wpdsColors.surfaceHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                title ?: "",
                textAlign = TextAlign.Center,
                color = wpdsColors.blue100Static,
                fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                price ?: "",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.postoniwide_bold)),
                color = wpdsColors.gray0
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle ?: "", textAlign = TextAlign.Center, color = wpdsColors.accessible, fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.franklinitcstd_black)))
        }
    }
}