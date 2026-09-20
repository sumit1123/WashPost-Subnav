/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.grid.views.carousel

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.model.CarouselComments
import com.wapo.flagship.features.grid.model.CarouselCommentsItem
import com.wapo.flagship.features.grid.model.Link
import com.wapo.flagship.features.grid.toDp
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory
import com.wapo.flagship.features.sections.utils.UIUtils
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.sections.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.launch

class CarouselCommentsHolder(
    itemView: View, val requestListener: CarouselProvider, val parent: ViewGroup
) : GridViewHolder(itemView) {

    private var res = itemView.resources
    private val deviceDensity = res?.displayMetrics?.density!!
    private val arrowsOffsetFromSideMarginInDp = 32

    private val pagerView = itemView.findActivityOfType<GridActivity>()?.getGridEnvironment()?.getPager()
    private var wpGridView: WPGridView = parent as WPGridView
    private var composeView =
        itemView.findViewById<ComposeView?>(R.id.comments_compose_form_wrapper)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

    var onCarouselItemClicked: ((Link?, Int) -> Unit)? = null


    override fun bind(position: Int, gridAdapter: GridAdapter) {
        val comments = gridAdapter.items[position] as? CarouselComments ?: return
        val items = comments.items?.filterNotNull() ?: return

        composeView?.setContent {
            AndroidClassicTheme {
                Surface(color = Color.Unspecified) {
                    CarouselContainer(items)
                }
            }
        }
    }

    @Composable
    fun CarouselContainer(items: List<CarouselCommentsItem>) {
        val isSingleColumn = wpGridView.getColumnCount() == 1
        val sideMargin = wpGridView.getSideMargin().toDp(deviceDensity)
        val arrowsOffsetInDp = sideMargin - arrowsOffsetFromSideMarginInDp
        val contentPadding =
            if (isSingleColumn) wpGridView.getCardDividerPadding().toDp(deviceDensity) else 0

        val listState = rememberLazyListState()
        val displayLeftArrow = remember { mutableStateOf(false) }
        val displayRightArrow = remember { mutableStateOf(false) }
        val minHeight = remember { mutableIntStateOf(UIUtils.dpToPx(200f, res)) }
        val coroutineScope = rememberCoroutineScope()
        val previousVisibleItemIndex = remember { mutableIntStateOf(0) }

        LaunchedEffect(listState.isScrollInProgress) {
            snapshotFlow { listState.isScrollInProgress }.collect { isScrollInProgress ->
                if (!isScrollInProgress) {
                    val offset = listState.firstVisibleItemScrollOffset
                    val previousIndex = previousVisibleItemIndex.intValue
                    val currentIndex =
                        (listState.firstVisibleItemIndex + (if (offset > 0) 1 else 0)).coerceAtMost(
                            items.size
                        )
                    when {
                        currentIndex > previousIndex -> NAVIGATION_COMMENTS_CAROUSEL_FORWARD
                        currentIndex < previousIndex -> NAVIGATION_COMMENTS_CAROUSEL_BACK
                        else -> null
                    }?.let { navigation ->
                        SectionTrackerFactory.get(itemView.context)
                            ?.trackCommentsCarouselNavigation(navigation, currentIndex)
                        previousVisibleItemIndex.intValue = currentIndex
                    }
                }
            }
        }

        val scrollBackward = {
            coroutineScope.launch {
                if (listState.firstVisibleItemIndex > 0)
                    listState.animateScrollToItem(listState.firstVisibleItemIndex - 1)
                else
                    listState.animateScrollToItem(0)
            }
        }
        val scrollForward = {
            coroutineScope.launch {
                listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
            }
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.canScrollBackward }.collect { canScrollBackward ->
                displayLeftArrow.value = canScrollBackward
            }
        }
        LaunchedEffect(listState) {
            snapshotFlow { listState.canScrollForward }.collect { canScrollForward ->
                displayRightArrow.value = canScrollForward
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                pagerView?.setShouldAllowScroll(true)
            }
        }

        Box {
            SubComposeCarousel(items) { minHeight.intValue = it }
            Carousel(items, minHeight, sideMargin, contentPadding, listState) { link, itemIndex ->
                onCarouselItemClicked?.invoke(link, itemIndex)
            }
            if (!isSingleColumn) {
                LeftArrowButton(displayLeftArrow, arrowsOffsetInDp) { scrollBackward.invoke() }
                RightArrowButton(displayRightArrow, arrowsOffsetInDp) { scrollForward.invoke() }
            }
        }
    }

    @Composable
    private fun SubComposeCarousel(
        items: List<CarouselCommentsItem>,
        minHeight: (Int) -> Unit
    ) {
        if (items.isEmpty()) return
        var text = ""
        var authorName: String? = null
        var authorRole: String? = null
        var authorAvatarUrl: String? = null
        var reactionCount = 0
        var repliesCount = 0
        var repliesAvatarUrl: String? = null
        items.forEach {
            if (!it.text.isNullOrEmpty()) text = if (it.text.length > text.length) it.text else text
            if (!it.authorName.isNullOrEmpty()) authorName = it.authorName
            if (!it.authorRole.isNullOrEmpty()) authorRole = it.authorRole
            if (!it.authorAvatarUrl.isNullOrEmpty()) authorAvatarUrl = it.authorAvatarUrl
            if (it.reactionCount != null) reactionCount = it.reactionCount
            if (it.repliesCount != null) repliesCount = it.repliesCount
            if (!it.repliesAvatarUrls.isNullOrEmpty()) repliesAvatarUrl =
                it.repliesAvatarUrls.firstOrNull()
        }
        val dummyItem = CarouselCommentsItem(
            null,
            text,
            authorName,
            authorRole,
            authorAvatarUrl,
            reactionCount,
            repliesCount,
            listOf(repliesAvatarUrl)
        )
        SubComposeCarouselCard({ minCardHeight ->
            minHeight.invoke(minCardHeight)
        }) {
            CarouselCard(dummyItem, isSubComposingLayout = true)
        }
    }

    @Composable
    private fun SubComposeCarouselCard(
        minHeight: (Int) -> Unit,
        content: @Composable () -> Unit,
    ) {
        SubcomposeLayout { constraints ->
            val placeable = subcompose(1, content).firstOrNull()?.measure(constraints)
            minHeight.invoke(placeable?.height ?: 0)
            layout(0, 0) {}
        }
    }

    @Composable
    fun Carousel(
        items: List<CarouselCommentsItem>,
        minHeight: MutableIntState,
        sideMargin: Int, contentPadding: Int,
        listState: LazyListState,
        onClickItem: ((Link?, Int) -> Unit)? = null
    ) {
        val itemsCount = items.size

        LazyRow(
            modifier = Modifier
                .padding(horizontal = sideMargin.dp)
                .pointerInput(true) {
                    awaitEachGesture {
                        while (true) {
                            handleLazyRowTouchEvents(awaitPointerEvent(PointerEventPass.Initial))
                        }
                    }
                },
            contentPadding = PaddingValues(horizontal = contentPadding.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        ) {
            items(count = itemsCount) { index ->
                CarouselCard(items[index], minHeight.intValue) { onClickItem?.invoke(it, index) }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
    @Composable
    fun CarouselCard(
        comment: CarouselCommentsItem,
        minHeight: Int = -1,
        isSubComposingLayout: Boolean = false,
        onClick: ((Link?) -> Unit)? = null
    ) {
        val cardHeightInDp = if (isSubComposingLayout) Dp.Unspecified
        else (minHeight / AppContextUtils.getDeviceDensity()).dp

        Card(
            modifier = Modifier
                .width(width = 305.dp)
                .border(width = 1.dp, color = wpdsColors.gray300, shape = RoundedCornerShape(4.dp)),
            elevation = 0.dp,
            backgroundColor = wpdsColors.gridCardBg,
            onClick = { onClick?.invoke(comment.link) }
        ) {
            Column(
                modifier = Modifier
                    .heightIn(min = cardHeightInDp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Author Details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(comment.authorAvatarUrl, 24, isSubComposingLayout)
                    NameAndRole(comment.authorName, comment.authorRole)
                }
                // Comment Text
                Comment(comment.text)
                // Reaction and Replies
                val hasReactionsOrReplies =
                    (comment.reactionCount != null && comment.reactionCount > 0)
                            || (comment.repliesCount != null && comment.repliesCount > 0)
                FlowRow(
                    modifier = Modifier
                        .padding(top = if (hasReactionsOrReplies) 4.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Reactions(comment.reactionCount)
                    Spacer(modifier = Modifier.weight(1f))
                    Replies(comment.repliesCount, comment.repliesAvatarUrls, isSubComposingLayout)
                }
            }
        }
    }

    @Composable
    private fun NameAndRole(name: String?, role: String?) {
        val nameSize = if (role.isNullOrEmpty()) 16 else 14
        val nameLineHeight = if (role.isNullOrEmpty()) 20f else 17.5f
        Column {
            Name(name, nameSize, nameLineHeight)
            Role(role)
        }
    }

    @Composable
    private fun Name(name: String?, size: Int, lineHeight: Float) {
        if (name.isNullOrEmpty()) return
        Text(
            text = name,
            color = wpdsColors.gray20,
            fontSize = size.sp,
            letterSpacing = 0.0.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
            lineHeight = lineHeight.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()

        )
    }

    @Composable
    private fun Role(role: String?) {
        if (role.isNullOrEmpty()) return
        Text(
            text = role,
            color = wpdsColors.gray20,
            fontSize = 12.sp,
            letterSpacing = 0.0.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun Comment(text: String?) {
        if (text.isNullOrEmpty()) return
        Text(
            text = text,
            color = wpdsColors.gray20,
            fontSize = 16.sp,
            letterSpacing = 0.0.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            lineHeight = 20.sp,
            minLines = 5,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }

    @Composable
    private fun Reactions(count: Int?) {
        if (count == null || count <= 0) return
        Text(
            text = if (count == 1) "$count Reaction" else "$count Reactions",
            color = wpdsColors.gray80,
            fontSize = 14.sp,
            letterSpacing = 0.0.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            lineHeight = 17.5.sp
        )
    }

    @Composable
    private fun Replies(
        count: Int?,
        avatarUrls: List<String?>?,
        isSubComposingLayout: Boolean = false
    ) {
        if (count == null || count <= 0) return
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(avatarUrls?.firstOrNull(), 20, isSubComposingLayout)
            Text(
                text = if (count == 1) "$count Reply" else "$count Replies",
                color = wpdsColors.gray80,
                fontSize = 14.sp,
                letterSpacing = 0.0.sp,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                lineHeight = 17.5.sp
            )
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    private fun Avatar(url: String?, size: Int, isSubComposingLayout: Boolean = false) {
        if (url.isNullOrEmpty()) return
        Box(
            modifier = Modifier
                .size(with(LocalDensity.current) { size.sp.toDp() })
        ) {
            if (isSubComposingLayout) return
            GlideImage(
                model = url,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(wpdsColors.gray500)
            )
        }
    }

    @Composable
    private fun BoxScope.LeftArrowButton(
        displayState: MutableState<Boolean>,
        arrowsOffsetInDp: Int,
        onClick: () -> Unit
    ) {
        if (!displayState.value) return

        OutlinedButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = arrowsOffsetInDp.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = wpdsColors.primary
            ),
            border = BorderStroke(0.dp, Color.Transparent),
            onClick = onClick,
            content = {
                Image(
                    painter = painterResource(R.drawable.carousel_left_arrow),
                    contentDescription = "Left Arrow Button"
                )
            }
        )
    }

    @Composable
    private fun BoxScope.RightArrowButton(
        displayState: MutableState<Boolean>,
        arrowsOffsetInDp: Int,
        onClick: () -> Unit
    ) {
        if (!displayState.value) return

        OutlinedButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = -arrowsOffsetInDp.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = wpdsColors.primary
            ),
            border = BorderStroke(0.dp, Color.Transparent),
            onClick = onClick,
            content = {
                Image(
                    painter = painterResource(R.drawable.carousel_right_arrow),
                    contentDescription = "Right Arrow Button"
                )
            }
        )
    }

    private fun handleLazyRowTouchEvents(event: PointerEvent) {
        when (event.type) {
            PointerEventType.Press -> {
                pagerView?.setShouldAllowScroll(false)
            }

            PointerEventType.Release -> {
                pagerView?.setShouldAllowScroll(true)
            }
        }
    }

    companion object {
        private val TAG = CarouselCommentsHolder::class.java.toString()
        private const val NAVIGATION_COMMENTS_CAROUSEL_FORWARD = "comments_carousel_forward"
        private const val NAVIGATION_COMMENTS_CAROUSEL_BACK = "comments_carousel_back"
    }
}