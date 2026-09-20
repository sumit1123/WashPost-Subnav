package com.wapo.flagship.features.sections;

import static com.wapo.flagship.features.sections.SectionsRibbonKt.sectionsRibbonSetContent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import com.wapo.android.commons.util.Logger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.wapo.android.commons.util.Utils;
import com.wapo.flagship.features.grid.MarginUpdatable;
import com.wapo.flagship.features.grid.Tracking;
import com.wapo.flagship.features.posttv.listeners.PostTvApplication;
import com.wapo.flagship.features.sections.model.Section;
import com.wapo.flagship.features.sections.model.SectionType;
import com.wapo.flagship.features.sections.tracking.SectionTrackEvent;
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory;
import com.wapo.flagship.features.sections.utils.AnimationHelper;
import com.wapo.flagship.features.sections.viewmodels.SectionNavigation;
import com.wapo.flagship.features.sections.viewmodels.SectionTrackingViewModel;
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonEvents;
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonViewModel;
import com.washingtonpost.android.sections.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class SectionFrontsFragment extends PageFragment implements ViewPager.OnPageChangeListener, MarginUpdatable {
    public static final String FRAGMENT_TAG = SectionFrontsFragment.class.getName() + ".fragmentTag";
    private static final String TAG = SectionFrontsFragment.class.getName();
    public static final String PARAM_BUNDLE_NAME = SectionFrontsFragment.class.getSimpleName() + ".bundleName";
    public static final String PARAM_SECTION_TITLE = SectionFrontsFragment.class.getSimpleName() + ".title";
    public static final String PARAM_SECTION_ID = SectionFrontsFragment.class.getSimpleName() + ".sectionId";
    private static final String PARAM_IS_TOP_STORIES = SectionFrontsFragment.class.getSimpleName() + ".isTopStories";
    protected static final int VIEW_LOADING = 0;
    protected static final int VIEW_CONTENT = 1;
    protected static final int VIEW_DATA_UNAVAILABLE = 4;
    private static final String ACTIVE_SECTION_ID = "ACTIVE_SECTION_ID";
    protected SectionsPagerView _pager;
    private View _loadingCurtain;
    private TextView _syncMsgCurtain;
    private View syncMsgAnchor;
    private ViewGroup _view;
    protected ProgressBar _loadingProgress;
    private NetworkBroadcastReceiver _connectivityReceiver;
    protected int _viewState = VIEW_LOADING;
    private SectionTrackingViewModel sectionTrackingViewModel;
    private SectionsRibbonViewModel sectionsRibbonViewModel;
    private Subscription sectionSubscription;

    public SectionFrontsFragment() {
    }

    public SectionFrontsFragment updatePageWith(String bundleName, String title, String sectionId) {
        return updatePageWith(bundleName, title, sectionId, null);
    }

    public SectionFrontsFragment updatePageWith(String bundleName, String title, String sectionId, String activeSectionId) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
            setArguments(args);
        }
        args.putString(PARAM_BUNDLE_NAME, bundleName);
        args.putString(PARAM_SECTION_TITLE, title);
        args.putString(PARAM_SECTION_ID, sectionId);
        boolean isTopStories = bundleName == null || bundleName.equals("/.");
        args.putBoolean(PARAM_IS_TOP_STORIES, isTopStories);
        args.putString(ACTIVE_SECTION_ID, activeSectionId);
        return this;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SectionActivity) {
            ((SectionActivity) context).logExtras("Attach SectionFrontFragment");
        }

    }

    @Override
    public void onDetach() {
        if (_connectivityReceiver != null) {
            //If the receiver was already unregistered or was not registered, then call to
            //unregisterReceiver throws IllegalArgumentException
            try {
                getActivity().unregisterReceiver(_connectivityReceiver);
            } catch (Exception e) {
                //Ignore Exception
            }
            _connectivityReceiver = null;
        }

        if (getActivity() instanceof SectionActivity) {
            ((SectionActivity) getActivity()).logExtras("Detach SectionFrontFragment");
        }
        super.onDetach();
    }


    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _view = (ViewGroup) inflater.inflate(R.layout.fragment_section_fronts, container, false);
        assert _view != null;

        _pager = _view.findViewById(R.id.section_fronts_sections);
        _pager.init(getChildFragmentManager());
        _pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.section_pager_margin_between_pages));
        _pager.addOnPageChangeListener(this);

        _loadingCurtain = _view.findViewById(R.id.section_fronts_loading_curtain);
        syncMsgAnchor = _view.findViewById(R.id.anchor);
        _syncMsgCurtain = _view.findViewById(R.id.section_fronts_sync_message);
        _loadingProgress = _view.findViewById(R.id.section_fronts_loading_progress);

        syncMsgAnchor.setVisibility(View.GONE);
        _syncMsgCurtain.setVisibility(View.GONE);
        _pager.setVisibility(View.GONE);
        _loadingCurtain.setVisibility(View.VISIBLE);

        _syncMsgCurtain.setMovementMethod(LinkMovementMethod.getInstance());

        return _view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Observable<? extends PageManager> pmObs = getPageManagerObs();
        sectionsRibbonViewModel = new ViewModelProvider(requireActivity()).get(SectionsRibbonViewModel.class);
        sectionTrackingViewModel = new ViewModelProvider(requireActivity()).get(SectionTrackingViewModel.class);
        observeSavedTrackingInfo();
        ComposeView sectionsRibbon = _view.findViewById(R.id.sections_ribbon);
        sectionsRibbonSetContent(
                sectionsRibbon,
                sectionsRibbonViewModel,
                createSectionTappedListener(),
                createCustomNavTappedListener()
        );

        if (pmObs != null) {
            updateContentViewState(VIEW_LOADING);

            sectionSubscription = pmObs
                    .flatMap(new Func1<PageManager, Observable<List<Section>>>() {
                        @Override
                        public Observable<List<Section>> call(PageManager pageManager) {
                            return pageManager.getPages(getSectionId()).distinctUntilChanged();
                        }
                    })
                    .filter(new Func1<List<Section>, Boolean>() {
                        @Override
                        public Boolean call(List<Section> sections) {
                            if (getSectionId() == null && sections.size() > 0) {
                                // Expected this case for Top Stories section. Update arguments.
                                Section section = sections.get(0);
                                updatePageWith(section.getBundleName(), section.getName(), section.getId());
                            }

                            // update only when bundle matches.
                            return !TextUtils.isEmpty(getSectionId()) &&
                                    sections.size() > 0 &&
                                    TextUtils.equals(getSectionId(), sections.get(0).getId());
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Section>>() {
                                   @Override
                                   public void onCompleted() {
                                   }

                                   @Override
                                   public void onError(Throwable e) {
                                       if (getActivity() == null) {
                                           return;
                                       }
                                       updateContentViewState(VIEW_DATA_UNAVAILABLE);
                                   }

                                   @Override
                                   public void onNext(List<Section> sectionList) {
                                       SectionActivity sectionActivity = ((SectionActivity) getActivity());
                                       if (sectionActivity == null) {
                                           return;
                                       }
                                       List<Section> customList = sectionActivity.getCustomizedSections();
                                       if (customList != null && !customList.isEmpty() && isHomeTab()) {
                                           loadSectionList(customList);
                                           sectionsRibbonViewModel.setShouldShowCustomNav(true);
                                           sectionsRibbonViewModel.setRibbonReady(true);
                                       } else {
                                           loadSectionList(sectionList);
                                           sectionsRibbonViewModel.setShouldShowCustomNav(false);
                                       }
                                   }
                               }
                    );
        } else {
            updateContentViewState(VIEW_DATA_UNAVAILABLE);
        }
    }

    private Function1<? super Integer, Unit> createSectionTappedListener() {
        return (index) -> {
            if (Objects.equals(sectionsRibbonViewModel.getSelectedSectionIndex(), index)) {
                scrollToTop(false);
            } else {
                Section selectedSection = sectionsRibbonViewModel.getSections().get(index);
                if (selectedSection != null) {
                    sectionsRibbonViewModel.appOpenedOnSection();
                    sectionsRibbonViewModel.setSelectedSectionIndex(index);
                    sectionTrackingViewModel.setNavigating(SectionNavigation.RIBBON_TAP);
                    loadSection(selectedSection.getId());
                }
            }
            return null;
        };
    }

    private Function0<Unit> createCustomNavTappedListener() {
        return () -> {
            SectionActivity sectionActivity = ((SectionActivity) getActivity());
            if (sectionActivity != null) {
                sectionActivity.openCustomNavSettings();
            }
            return null;
        };
    }

    private boolean isHomeTab() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            return getSectionId() == null || arguments.getBoolean(PARAM_IS_TOP_STORIES, false);
        }
        return false;
    }

    void loadSectionList(List<Section> sectionList) {
        List<Section> list = filterWebViewSections(sectionList);
        sectionsRibbonViewModel.setSections(list);
        _pager.update(list);
        if (!list.isEmpty()) {
            int index = sectionsRibbonViewModel.getSelectedSectionIndex();
            if (index >= 0 && index < list.size()) {
                _pager.setCurrentItem(index);
                sectionTrackingViewModel.setCurrentSectionBundle(list.get(index).getBundleName());
            }
        }
        updateContentViewState(list.isEmpty() ? VIEW_DATA_UNAVAILABLE : VIEW_CONTENT);
    }

    /**
     * To match iOS, prevent webview sections from displaying on section front ribbon.
     */
    private List<Section> filterWebViewSections(List<Section> sectionList) {
        List<Section> filteredList = new ArrayList<>();
        for (Section section: sectionList) {
            if (section.getSectionType() != SectionType.WEB) {
                filteredList.add(section);
            }
        }
        return filteredList;
    }

    @Override
    public void onDestroyView() {
        _pager = null;
        _loadingCurtain = null;
        _view = null;
        if (sectionSubscription != null) {
            sectionSubscription.unsubscribe();
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_pager != null && !sectionTrackingViewModel.getShouldTrackBackToFront()) {
            trackCurrentPage(_pager.getCurrentItem());
            sectionTrackingViewModel.setShouldTrackBackToFront(true);
        }
        BaseSectionFragment currentFragment = _pager != null ? _pager.getCurrentFragment() : null;
        if (currentFragment != null) {
            currentFragment.startEngagementTrace();
        }
    }

        @Override
    public void onPause() {
        super.onPause();
        // Save the currently selected section when the fragment is paused
        if (isVisible()) {
            sectionsRibbonViewModel.saveCurrentSection();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (_pager != null) {
            Section currentSection = sectionsRibbonViewModel.getCurrentSection();
            if (currentSection != null) {
                String sectionId = currentSection.getId();
                if (sectionId != null) {
                    outState.putString(ACTIVE_SECTION_ID, sectionId);
                }
            }
        }
    }

    public boolean loadSection(String sectionId) {
        int index = sectionsRibbonViewModel.getSectionIndex(sectionId);
        return loadSectionByIndex(index);
    }

    public boolean loadDefaultSection() {
        return loadSectionByIndex(sectionsRibbonViewModel.getDefaultIndex());
    }

    public boolean loadSectionByIndex(int index) {
        if (index >= 0 && _pager.getAdapter() != null && index < _pager.getAdapter().getCount()) {
            _pager.setCurrentItem(index);
            return true;
        }
        return false;
    }

    private void updateContentViewState(final int status) {

        if (_view == null) {
            return;
        }

        _viewState = status;

        switch (status) {
            case VIEW_LOADING:
                _pager.setVisibility(View.GONE);
                _syncMsgCurtain.setVisibility(View.GONE);
                _loadingCurtain.setVisibility(View.VISIBLE);

                break;

            case VIEW_CONTENT:
                SectionTrackerFactory.get(getContext()).onPagerShown(getActivity());
                AnimationHelper.fadeIn(_pager, new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        final FragmentActivity activity = getActivity();
                        if (activity != null) {
                            activity.supportInvalidateOptionsMenu();
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                AnimationHelper.fadeOut(_loadingCurtain, null);
                AnimationHelper.fadeOut(_syncMsgCurtain, null);
                syncMsgAnchor.setVisibility(View.GONE);
                break;

            case VIEW_DATA_UNAVAILABLE: {
                FragmentActivity activity = getActivity();
                if (activity instanceof ConnectivityActivity) {
                    ((ConnectivityActivity) activity).checkConnectivity();
                }
                String message = getResources().getString(R.string.articles_unable_to_load_a_content_msg);
                _syncMsgCurtain.setText(message);
                syncMsgAnchor.setVisibility(View.VISIBLE);
                AnimationHelper.fadeIn(_syncMsgCurtain, null);
                AnimationHelper.fadeOut(_loadingCurtain, null);
                AnimationHelper.fadeOut(_pager, null);
            }
            break;

        }
    }

    @Override
    public void onPageSelected(int pos) {
        Logger.d(TAG, "onPageSelected " + pos);
        final Context appContext = getContext().getApplicationContext();
        if (appContext instanceof PostTvApplication) {
            ((PostTvApplication) appContext).releaseVideoManager();
            ((PostTvApplication) appContext).getVideoManager2().releaseAllVideos();
            ((PostTvApplication) appContext).releaseVideoManager2();
        }

        if (_pager == null) {
            return;
        }

        sectionsRibbonViewModel.setSelectedSectionIndex(pos);
        sectionsRibbonViewModel.checkNewsprintVisited();

        // Delay has been added to get the fragment be prepared for bundle name and other tracking details from it.
        // VM should handle it but it needs refactoring.
        _pager.postDelayed(() -> {
            if (_pager != null) {
                trackCurrentPage(pos);
            }
        }, 500L);
    }

    private void observeSavedTrackingInfo() {
        sectionsRibbonViewModel.getSectionsRibbonEventLiveData().observe(getViewLifecycleOwner(), new Observer<SectionsRibbonEvents>() {
            @Override
            public void onChanged(SectionsRibbonEvents sectionsRibbonEvents) {
                if (sectionsRibbonEvents instanceof SectionsRibbonEvents.SendTrackingInfoEvent) {
                    Tracking tracking = ((SectionsRibbonEvents.SendTrackingInfoEvent) sectionsRibbonEvents).getTracking();
                    sectionTrackingViewModel.trackEvent(new SectionTrackEvent.PageView(tracking.getSection(), tracking), 1, "");
                }
            }
        });
    }

    private void trackCurrentPage(int pos) {
        if (_pager == null) return;
        BaseSectionFragment currentFragment = _pager.getCurrentFragment();
        if (currentFragment != null) {
            sectionTrackingViewModel.setCurrentSectionBundle(currentFragment.getBundleName());
            sectionTrackingViewModel.trackEvent(
                    new SectionTrackEvent.PageView(
                            _pager.getSectionTitle(),
                            currentFragment.getTracking()
                    ),
                    pos,
                    "SFF"
            );
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
            sectionTrackingViewModel.setNavigating(SectionNavigation.SWIPE);
        } else if (state == ViewPager.SCROLL_STATE_IDLE) {
            sectionTrackingViewModel.resetNavigating();
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    public void scrollToTop(boolean smoothScroll) {
        BaseSectionFragment currentFragment = _pager.getCurrentFragment();
        if (currentFragment != null) {
            if (smoothScroll) {
                currentFragment.smoothScrollToTop();
            } else {
                currentFragment.scrollToTop();
            }
            // TODO: Also set TopBarState.EXPANDED?
        }
    }

    @Override
    public void updateBottomMargin(int bottomMargin) {
        if (_pager != null) {
            BaseSectionFragment currentFragment = _pager.getCurrentFragment();
            if (currentFragment instanceof MarginUpdatable) {
                ((MarginUpdatable) currentFragment).updateBottomMargin(bottomMargin);
            }
        }
    }

    private class NetworkBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.isConnectedOrConnecting(context)) {
                final FragmentActivity activity = getActivity();
                if (activity == null) {
                    return;
                }

                //If the receiver was already unregistered or was not registered, then call to unregisterReceiver throws IllegalArgumentException
                try {
                    activity.unregisterReceiver(this);
                } catch (Exception e) {
                    //Ignore Exception
                }
                _connectivityReceiver = null;

                activity.supportInvalidateOptionsMenu();
            }
        }
    }

    public SectionsPagerView getPager() {
        return _pager;
    }

    @Nullable
    public String getBundleName() {
        return getArguments() != null ? getArguments().getString(PARAM_BUNDLE_NAME) : null;
    }

    @Nullable
    public String getSectionTitle() {
        return getArguments() != null ? getArguments().getString(PARAM_SECTION_TITLE) : null;
    }

    @Nullable
    public String getSectionId() {
        return getArguments() != null ? getArguments().getString(PARAM_SECTION_ID) : null;
    }

    public void setTrackSectionBack() {
        sectionTrackingViewModel.setNavigating(SectionNavigation.BACK);
    }

    public void setTrackSectionChangeTab() {
        sectionTrackingViewModel.setNavigating(SectionNavigation.TAB);
    }

    public int getTopStoriesIndex() {
        return sectionsRibbonViewModel.getDefaultIndex();
    }

}
