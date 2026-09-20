package com.wapo.flagship.features.personalizedpodcasts.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.personalizedpodcasts.model.Option
import com.wapo.flagship.features.personalizedpodcasts.model.PodcastConfigItem
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.StyleType
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors



@Composable
fun PodcastBottomSheetContent(
    onDismiss: () -> Unit,
    personalizedPodcastViewModel: PersonalizedPodcastViewModel
) {
    val uiState by personalizedPodcastViewModel.uiState.collectAsState()
    val podcastConfigs = uiState.configMetadata

    val selectedItemsMap = remember(podcastConfigs) {
        mutableStateMapOf<String, Pair<Int, List<String>>>().apply {
            podcastConfigs?.podcastConfigItems?.forEach { config ->

                val preselectedIds = config.items
                    .filter { it.isSelected == true }
                    .map { it.id }

                put(config.title.id, Pair(config.selectionLimit ?: 1, preselectedIds) )
            }
        }
    }

    val customInput = remember {
        mutableStateOf("")
    }

    AndroidClassicTheme {
        Surface(
            color = wpdsColors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.height(28.dp))
                HeaderSection()
                Spacer(modifier = Modifier.height(28.dp))
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    //Dynamically create sections from our data list
                    podcastConfigs?.podcastConfigItems?.forEachIndexed { index, config ->
                        ConfigSection(
                            podcastConfigItem = config,
                            selectedItems = selectedItemsMap[config.title.id]?.second
                                ?: emptyList(),
                            onItemSelected = { title, newItems ->
                                selectedItemsMap[title] = Pair(config.selectionLimit ?: 1, newItems)
                            },
                            customInput = {
                                customInput.value = it
                            },
                            personalizedPodcastViewModel
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    podcastConfigs?.message?.let {
                        Text(
                            it,
                            style = TextStyle(
                                color = wpdsColors.gray0,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FranklinItcStandardFontFamily,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    GenerateButton(selectedItemsMap, onDismiss, personalizedPodcastViewModel, podcastConfigs?.canGenerate ?: true, customInput.value)
                    Spacer(modifier = Modifier.height(12.dp))
                    DismissButton(personalizedPodcastViewModel, onDismiss)
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    AndroidClassicTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start

        ) {
            Text(
                text = "Customize your podcast",
                style = TextStyle(
                    color = wpdsColors.onSurface,
                    fontSize = 18.sp,
                    lineHeight = 22.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FranklinItcStandardFontFamily,
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// Generic composable to render any config section
@Composable
fun ConfigSection(
    podcastConfigItem: PodcastConfigItem, selectedItems: List<String>,
    onItemSelected: (String, List<String>) -> Unit,
    customInput: (String) -> Unit,
    personalizedPodcastViewModel: PersonalizedPodcastViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = podcastConfigItem.title.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = wpdsColors.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (podcastConfigItem.style) {
            StyleType.CHIP.name.lowercase() -> ChipSection(
                podcastConfigItem = podcastConfigItem,
                selectedItems = selectedItems,
                onItemSelected = { newItems ->
                    onItemSelected(podcastConfigItem.title.id, newItems)
                }
            )

            StyleType.RADIO.name.lowercase() -> RadioButtonSection(
                items = podcastConfigItem.items,
                selectedItem = selectedItems.firstOrNull(),
                onItemSelected = { newItem ->
                    onItemSelected(podcastConfigItem.title.id, listOf(newItem))
                },
                personalizedPodcastViewModel = personalizedPodcastViewModel
            )

            StyleType.SLIDER.name.lowercase() -> SliderSection(
                podcastConfigItem = podcastConfigItem,
                selectedItem = selectedItems.firstOrNull(),
                onItemSelected = { newItem ->
                    onItemSelected(podcastConfigItem.title.id, listOf(newItem))
                }
            )
            else -> {
                ChipSection(
                    podcastConfigItem = podcastConfigItem,
                    selectedItems = selectedItems,
                    onItemSelected = { newItems ->
                        onItemSelected(podcastConfigItem.title.id, newItems)
                    }
                )
            }
        }

        if (podcastConfigItem.allowsCustomInput == true) {
            CustomTopicSection({input ->
                customInput(input)
            })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipSection(
    podcastConfigItem: PodcastConfigItem,
    selectedItems: List<String>,
    onItemSelected: (List<String>) -> Unit
) {
    val isMultiSelect = (podcastConfigItem.selectionLimit ?: 1) > 1
    FlowRow(
        // horizontal spacing between chips
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        // vertical spacing between the wrapped rows
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        podcastConfigItem.items.forEach { item ->
            val isSelected = item.id in selectedItems
            SelectableChip(
                text = item.displayName,
                isSelected = isSelected,
                onClick = {
                    val newSelectedItems = if (isMultiSelect) {
                        if (isSelected) selectedItems - item.id else selectedItems + item.id
                    } else {
                        if (isSelected) emptyList() else listOf(item.id)
                    }
                    onItemSelected(newSelectedItems)
                }
            )
        }
    }
}

/**
 * Renders the host voice selection using radio buttons.
 */
@Composable
fun RadioButtonSection(
    items: List<Option>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    personalizedPodcastViewModel: PersonalizedPodcastViewModel
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, wpdsColors.gray300)
                    .clickable {
                        item.url?.let { personalizedPodcastViewModel.playVoiceSample(it) }
                        onItemSelected(item.id)
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    painter = painterResource(com.wpds.wpds.R.drawable.soundwave),
                    tint = wpdsColors.gray0,
                    contentDescription = "Host voice icon"
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.displayName,
                    style = TextStyle(
                        color = wpdsColors.gray40,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FranklinItcStandardFontFamily,
                    ),
                    modifier = Modifier.weight(1f),
                )
                RadioButton(
                    selected = (item.id == selectedItem),
                    onClick = {
                        item.url?.let { personalizedPodcastViewModel.playVoiceSample(it) }
                        onItemSelected(item.id)
                    },
                    colors = RadioButtonDefaults.colors(
                        unselectedColor = wpdsColors.gray300,
                        selectedColor = wpdsColors.gray0)
                )
            }
        }
    }
}

/**
 * Renders the episode length selection using a Slider.
 * Assumes the first item is the min value and the last is the max.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderSection(
    podcastConfigItem: PodcastConfigItem,
    selectedItem: String?,
    onItemSelected: (String) -> Unit
) {
    // Safely get min/max values from the config item list
    val labels = podcastConfigItem.items.map { it.displayName }
    val items = podcastConfigItem.items
    if (items.isEmpty()) {
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val minValue = 0f
    val maxValue = (items.size - 1).toFloat()
    val steps = (items.size - 2).coerceAtLeast(0)

    val initialIndex = items.indexOfFirst { it.id == selectedItem }
        .coerceAtLeast(0)

    var sliderPosition by remember {
        mutableFloatStateOf(initialIndex.toFloat())
    }

    LaunchedEffect(selectedItem) {
        val newIndex = items.indexOfFirst { it.id == selectedItem }
            .coerceAtLeast(0)
        sliderPosition = newIndex.toFloat()
    }


    Column(modifier = Modifier.padding(horizontal = 10.dp)) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = minValue..maxValue,
            steps = steps,
            onValueChangeFinished = {
                val selectedIndex = sliderPosition.toInt()
                val selectedId = items.getOrNull(selectedIndex)?.id
                if (selectedId != null) {
                    onItemSelected(selectedId)
                }
            },
            interactionSource = interactionSource,

            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            color = wpdsColors.gray300,
                            shape = RoundedCornerShape(3.dp) // Optional: rounded corners for the track
                        )
                ) {
                    val thumbOffset = (sliderState.value - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(thumbOffset)
                            .background(
                                color = wpdsColors.gray40,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .indication(interactionSource = interactionSource, indication = null)
                        .size(30.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFF000000).copy(alpha = 0.5f),
                            spotColor = Color(0xFF000000).copy(alpha = 0.5f)
                        )
                        .background(
                            color = wpdsColors.gray700,
                            shape = CircleShape
                        )
                )
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(text = label, color = wpdsColors.gray0, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun CustomTopicSection(input: (String) -> Unit) {
    var textState by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.add_custom_topics_or_questions),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.ask_a_question_or_topic),
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = textState,
            onValueChange = {
                textState = it
                input(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.LightGray,
                unfocusedBorderColor = Color.LightGray,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )
    }
}

@Composable
fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    height: Dp = 50.dp
) {
    AndroidClassicTheme {
        val backgroundColor = if (isSelected) wpdsColors.onSurface else wpdsColors.surface
        val textColor = if (isSelected) wpdsColors.surface else wpdsColors.onSurface

        Box(
            modifier = Modifier
                .widthIn(min = 60.dp)
                .height(height)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    1.dp,
                    if (isSelected) wpdsColors.gray20 else wpdsColors.gray300,
                    RoundedCornerShape(6.dp)
                )
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(
                    color = textColor,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FranklinItcStandardFontFamily,
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun GenerateButton(
    selectedItems: SnapshotStateMap<String, Pair<Int, List<String>>>,
    dismiss: () -> Unit,
    personalizedPodcastViewModel: PersonalizedPodcastViewModel,
    canGenerate: Boolean,
    customInput: String
) {
    var showDialog by remember { mutableStateOf(false) }

    val onConfirmAction = {
        if (customInput.isNotBlank()) {
            selectedItems["topics"] = Pair(selectedItems["topics"]?.first ?: 1, listOf(customInput))
        }
        personalizedPodcastViewModel.setIsCurrentPlaceholder(true)
        personalizedPodcastViewModel.generatePodcast(selectedItems, true, canGenerate, true)
        dismiss()
    }
    Button(
        onClick = {
            showDialog = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = wpdsColors.onSurface,
            contentColor = wpdsColors.surface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (if (canGenerate) painterResource(com.wpds.wpds.R.drawable.ai_filled) else null)?.let {
                Icon(
                    painter = it,
                    tint = wpdsColors.surface,
                    contentDescription = "Generate"
                )
            }
            Text(
                text = if (canGenerate) stringResource(R.string.generate_podcast) else stringResource(
                    R.string.save_changes
                ),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    if (showDialog) {
        AlertDialog(
            containerColor = wpdsColors.surface,
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text(text = if (canGenerate) stringResource(R.string.create_a_new_episode) else stringResource(
                    R.string.you_ve_hit_today_s_limit
                ),
                    fontWeight = FontWeight.Bold,
                    color = wpdsColors.gray0,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            },
            text = {
                Text(if (canGenerate) stringResource(R.string.replace_podcast) else stringResource(R.string.podcast_daily_limit),
                    color = wpdsColors.gray0,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmAction()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = wpdsColors.gray0,
                        contentColor = wpdsColors.gray700
                    )
                ) {
                    Text(stringResource(R.string.ok_button))
                }
            },
            dismissButton = {
                if (canGenerate) {
                    Button(
                        onClick = {
                            showDialog = false
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = wpdsColors.gray0,
                            contentColor = wpdsColors.gray700
                        )
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}

@Composable
fun DismissButton(
    personalizedPodcastViewModel: PersonalizedPodcastViewModel,
    dismiss: () -> Unit,
) {
    Button(
        onClick = {
            personalizedPodcastViewModel.restorePreviousPlayback()
            dismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, wpdsColors.gray0, CircleShape)
            .height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = wpdsColors.surface,
            contentColor = wpdsColors.onSurface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.cancel),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
