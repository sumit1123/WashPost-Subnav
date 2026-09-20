package com.wapo.flagship.features.sections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel;
import com.wapo.flagship.features.grid.model.SectionInlineMessage;
import com.wapo.flagship.features.pagebuilder.AudioView;
import com.wapo.flagship.features.pagebuilder.InlineAudioView;
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel;
import com.wapo.flagship.features.sections.model.Section;
import com.wapo.flagship.features.sections.viewmodels.SectionAudioMediaActivityViewModel;
import com.washingtonpost.android.recirculation.carousel.viewmodels.CarouselAudioMediaActivityViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SectionActivity {

    boolean isPhone();

    String getLiveBlogSvcUrl();

    void openBreakingNews(String url);

    void openLiveVideo(String url);

    void openLiveBlog(String url);

    void openWeb(String url);

    void logExtras(@NonNull String msg);

    void sendException(@NonNull Throwable t);

    void onRefreshSFPage(String pageName);

    void onStartSFFragment(String pageName);

    @Nullable
    SectionsPagerView getPager();

    SectionFragmentFactory getSectionFragmentFactory();

    @Nullable
    AudioView getAudioView();

    @Nullable
    InlineAudioView getInlineAudioView();

    SubscribeButton getSubscribeButton(String sectionDisplayName);

    int getPlayer2ContainerResId();

    boolean canAutoPlayCarouselVideo();

    boolean canAutoPlayInlineVideo();

    boolean hasDeviceLevelDataRestriction();

    boolean isDataUsageRestricted();

    boolean isLowDataModeEnable();
    boolean isPremiumAccount();

    @Nullable
    SectionInlineMessage getSectionInlineMessage();

    @Nullable
    SectionAudioMediaActivityViewModel getSectionAudioMediaActivityViewModel();

    @Nullable
    CarouselAudioMediaActivityViewModel getCarouselAudioMediaActivityViewModel();

    @Nullable
    PersonalizedPodcastViewModel getPersoPodcastViewModel();

    @Nullable
    PlaylistActivityViewModel getPlayListViewModel();

    boolean isAudioPlayerSheetVisible();

    @Nullable
    String getAppSection();

    List<Section> getCustomizedSections();

    boolean isActivityFinishing();

    void openWebEmbed(@NotNull String url);

    void openCustomNavSettings();

    boolean shouldSuppressAds();

}