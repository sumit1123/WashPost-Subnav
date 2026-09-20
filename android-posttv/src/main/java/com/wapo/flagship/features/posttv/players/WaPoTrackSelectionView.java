package com.wapo.flagship.features.posttv.players;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.TrackNameProvider;

import com.wapo.flagship.features.posttv.R;

import java.util.Arrays;

/**
 * A view for making track selections.
 * Same as {@link androidx.media3.ui.TrackSelectionView} but provides some additional functionality:
 * <br/>
 * 1) You can show/hide the "Auto/Default" option
 * <br/>
 * 2) You can provide a {@link TrackFilter} to show/hide a particular track from the list of available tracks.
 */
@UnstableApi
public class WaPoTrackSelectionView extends LinearLayout {

    private final int selectableItemBackgroundResourceId;
    private final LayoutInflater inflater;
    private final CheckedTextView disableView;
    private final CheckedTextView defaultView;
    private final ComponentListener componentListener;

    private boolean allowAdaptiveSelections;

    private TrackNameProvider trackNameProvider;
    private CheckedTextView[][] trackViews;

    private DefaultTrackSelector trackSelector;
    private int rendererIndex;
    private TrackGroupArray trackGroups;
    private boolean isDisabled;
    private @Nullable
    DefaultTrackSelector.SelectionOverride override;
    private TrackFilter trackFilter;

    /**
     * Gets a pair consisting of a dialog and the {@link androidx.media3.ui.TrackSelectionView} that will be shown by it.
     *
     * @param activity      The parent activity.
     * @param title         The dialog's title.
     * @param trackSelector The track selector.
     * @param rendererIndex The index of the renderer.
     * @return The dialog and the {@link WaPoTrackSelectionView} that will be shown by it.
     */
    public static Pair<AlertDialog, WaPoTrackSelectionView> getDialog(
            Activity activity,
            CharSequence title,
            DefaultTrackSelector trackSelector,
            int rendererIndex,
            @Nullable TrackFilter trackFilter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Inflate with the builder's context to ensure the correct style is used.
        LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
        View dialogView = dialogInflater.inflate(R.layout.exo_wapo_track_selection_dialog, null);

        final WaPoTrackSelectionView selectionView = dialogView.findViewById(R.id.exo_track_selection_view);
        selectionView.init(trackSelector, rendererIndex, trackFilter);
        Dialog.OnClickListener okClickListener =
                new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectionView.applySelection();
                    }
                };

        AlertDialog dialog =
                builder
                        .setTitle(title)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.ok, okClickListener)
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
        return Pair.create(dialog, selectionView);
    }

    public WaPoTrackSelectionView(Context context) {
        this(context, null);
    }

    public WaPoTrackSelectionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("nullness")
    public WaPoTrackSelectionView(
            Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributeArray =
                context
                        .getTheme()
                        .obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        selectableItemBackgroundResourceId = attributeArray.getResourceId(0, 0);
        attributeArray.recycle();

        inflater = LayoutInflater.from(context);
        componentListener = new ComponentListener();
        trackNameProvider = new DefaultTrackNameProvider(getResources());

        // View for disabling the renderer.
        disableView =
                (CheckedTextView)
                        inflater.inflate(android.R.layout.simple_list_item_single_choice, this, false);
        disableView.setBackgroundResource(selectableItemBackgroundResourceId);
        disableView.setText(androidx.media3.ui.R.string.exo_track_selection_none);
        disableView.setEnabled(false);
        disableView.setFocusable(true);
        disableView.setOnClickListener(componentListener);
        disableView.setVisibility(View.GONE);
        addView(disableView);
        // Divider view.
        addView(inflater.inflate(androidx.media3.ui.R.layout.exo_list_divider, this, false));
        // View for clearing the override to allow the selector to use its default selection logic.
        defaultView =
                (CheckedTextView)
                        inflater.inflate(android.R.layout.simple_list_item_single_choice, this, false);
        defaultView.setBackgroundResource(selectableItemBackgroundResourceId);
        defaultView.setText(androidx.media3.ui.R.string.exo_track_selection_auto);
        defaultView.setEnabled(false);
        defaultView.setFocusable(true);
        defaultView.setOnClickListener(componentListener);
        addView(defaultView);
    }

    public void setShowDefault(boolean showDefault) {
        defaultView.setVisibility(showDefault ? View.VISIBLE : View.GONE);
    }

    /**
     * Sets whether adaptive selections (consisting of more than one track) can be made using this
     * selection view.
     *
     * <p>For the view to enable adaptive selection it is necessary both for this feature to be
     * enabled, and for the target renderer to support adaptation between the available tracks.
     *
     * @param allowAdaptiveSelections Whether adaptive selection is enabled.
     */
    public void setAllowAdaptiveSelections(boolean allowAdaptiveSelections) {
        if (this.allowAdaptiveSelections != allowAdaptiveSelections) {
            this.allowAdaptiveSelections = allowAdaptiveSelections;
            updateViews();
        }
    }

    /**
     * Sets whether an option is available for disabling the renderer.
     *
     * @param showDisableOption Whether the disable option is shown.
     */
    public void setShowDisableOption(boolean showDisableOption) {
        disableView.setVisibility(showDisableOption ? View.VISIBLE : View.GONE);
    }

    /**
     * Sets the {@link TrackNameProvider} used to generate the user visible name of each track and
     * updates the view with track names queried from the specified provider.
     *
     * @param trackNameProvider The {@link TrackNameProvider} to use.
     */
    public void setTrackNameProvider(TrackNameProvider trackNameProvider) {
        this.trackNameProvider = Assertions.checkNotNull(trackNameProvider);
        updateViews();
    }

    /**
     * Initialize the view to select tracks for a specified renderer using a {@link
     * DefaultTrackSelector}.
     *  @param trackSelector The {@link DefaultTrackSelector}.
     * @param rendererIndex The index of the renderer.
     * @param trackFilter
     */
    public void init(DefaultTrackSelector trackSelector, int rendererIndex, @Nullable TrackFilter trackFilter) {
        this.trackSelector = trackSelector;
        this.rendererIndex = rendererIndex;
        this.trackFilter = trackFilter;
        updateViews();
    }

    // Private methods.

    private void updateViews() {
        // Remove previous per-track views.
        for (int i = getChildCount() - 1; i >= 3; i--) {
            removeViewAt(i);
        }

        MappingTrackSelector.MappedTrackInfo trackInfo =
                trackSelector == null ? null : trackSelector.getCurrentMappedTrackInfo();
        if (trackSelector == null || trackInfo == null) {
            // The view is not initialized.
            disableView.setEnabled(false);
            defaultView.setEnabled(false);
            return;
        }
        disableView.setEnabled(true);
        defaultView.setEnabled(true);

        trackGroups = trackInfo.getTrackGroups(rendererIndex);

        DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
        isDisabled = parameters.getRendererDisabled(rendererIndex);
        override = parameters.getSelectionOverride(rendererIndex, trackGroups);

        // Add per-track views.
        trackViews = new CheckedTextView[trackGroups.length][];
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            boolean enableAdaptiveSelections =
                    allowAdaptiveSelections
                            && trackGroups.get(groupIndex).length > 1
                            && trackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false)
                            != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED;
            trackViews[groupIndex] = new CheckedTextView[group.length];
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (trackIndex == 0) {
                    addView(inflater.inflate(androidx.media3.ui.R.layout.exo_list_divider, this, false));
                }
                int trackViewLayoutId =
                        enableAdaptiveSelections
                                ? android.R.layout.simple_list_item_multiple_choice
                                : android.R.layout.simple_list_item_single_choice;
                CheckedTextView trackView =
                        (CheckedTextView) inflater.inflate(trackViewLayoutId, this, false);
                trackView.setBackgroundResource(selectableItemBackgroundResourceId);
                Format format = group.getFormat(trackIndex);
                trackView.setText(trackNameProvider.getTrackName(format));
                if (trackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex) == C.FORMAT_HANDLED) {
                    trackView.setFocusable(true);
                    trackView.setTag(Pair.create(groupIndex, trackIndex));
                    trackView.setOnClickListener(componentListener);
                } else {
                    trackView.setFocusable(false);
                    trackView.setEnabled(false);
                }
                trackViews[groupIndex][trackIndex] = trackView;
                addView(trackView);
                if (trackFilter != null) {
                    trackView.setVisibility(trackFilter.filter(format, trackGroups) ? View.VISIBLE : View.GONE);
                }
            }
        }

        updateViewStates();
    }

    private void updateViewStates() {
        disableView.setChecked(isDisabled);
        defaultView.setChecked(!isDisabled && override == null);
        for (int i = 0; i < trackViews.length; i++) {
            for (int j = 0; j < trackViews[i].length; j++) {
                trackViews[i][j].setChecked(
                        override != null && override.groupIndex == i && override.containsTrack(j));
            }
        }
    }

    private void applySelection() {
        DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();
        parametersBuilder.setRendererDisabled(rendererIndex, isDisabled);
        if (override != null) {
            parametersBuilder.setSelectionOverride(rendererIndex, trackGroups, override);
        } else {
            parametersBuilder.clearSelectionOverrides(rendererIndex);
        }
        trackSelector.setParameters(parametersBuilder);
    }

    private void onClick(View view) {
        if (view == disableView) {
            onDisableViewClicked();
        } else if (view == defaultView) {
            onDefaultViewClicked();
        } else {
            onTrackViewClicked(view);
        }
        updateViewStates();
    }

    private void onDisableViewClicked() {
        isDisabled = true;
        override = null;
    }

    private void onDefaultViewClicked() {
        isDisabled = false;
        override = null;
    }

    private void onTrackViewClicked(View view) {
        isDisabled = false;
        @SuppressWarnings("unchecked")
        Pair<Integer, Integer> tag = (Pair<Integer, Integer>) view.getTag();
        int groupIndex = tag.first;
        int trackIndex = tag.second;
        if (override == null || override.groupIndex != groupIndex || !allowAdaptiveSelections) {
            // A new override is being started.
            override = new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex);
        } else {
            // An existing override is being modified.
            int overrideLength = override.length;
            int[] overrideTracks = override.tracks;
            if (((CheckedTextView) view).isChecked()) {
                // Remove the track from the override.
                if (overrideLength == 1) {
                    // The last track is being removed, so the override becomes empty.
                    override = null;
                    isDisabled = true;
                } else {
                    int[] tracks = getTracksRemoving(overrideTracks, trackIndex);
                    override = new DefaultTrackSelector.SelectionOverride(groupIndex, tracks);
                }
            } else {
                int[] tracks = getTracksAdding(overrideTracks, trackIndex);
                override = new DefaultTrackSelector.SelectionOverride(groupIndex, tracks);
            }
        }
    }

    private static int[] getTracksAdding(int[] tracks, int addedTrack) {
        tracks = Arrays.copyOf(tracks, tracks.length + 1);
        tracks[tracks.length - 1] = addedTrack;
        return tracks;
    }

    private static int[] getTracksRemoving(int[] tracks, int removedTrack) {
        int[] newTracks = new int[tracks.length - 1];
        int trackCount = 0;
        for (int track : tracks) {
            if (track != removedTrack) {
                newTracks[trackCount++] = track;
            }
        }
        return newTracks;
    }

    public static int getNumberOfTracks(DefaultTrackSelector trackSelector, DefaultTrackFilter trackFilter) {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return -1;
        }
        int textRendererIndex = getTextRendererIndex(mappedTrackInfo);
        if (textRendererIndex != -1) {
            return getSubtitlesTracksCount(trackFilter, mappedTrackInfo, textRendererIndex);
        }
        return -1;
    }

    public static int getTextRendererIndex(@NonNull MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        int count = mappedTrackInfo.getRendererCount();
        for (int i = 0; i < count; i++) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a number of caption tracks that have a non-null language
     */
    private static int getSubtitlesTracksCount(DefaultTrackFilter trackFilter,
                                            MappingTrackSelector.MappedTrackInfo mappedTrackInfo,
                                            int textRendererIndex) {
        int result = 0;
        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(textRendererIndex);
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);
                if (trackFilter.filter(format, trackGroups)) {
                    result++;
                }
            }
        }
        return result;
    }

    interface TrackFilter {
        /**
         * Return true if the given format should be shown in the list of available formats. False otherwise.
         */
        boolean filter(Format format, TrackGroupArray trackGroups);
    }

    // Internal classes.

    private class ComponentListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            WaPoTrackSelectionView.this.onClick(view);
        }
    }
}
