package com.wapo.flagship.features.sections;

import android.content.Context;
import android.text.TextUtils;

import androidx.fragment.app.FixedFragmentStatePagerAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.wapo.flagship.features.sections.model.Section;

import java.util.ArrayList;
import java.util.List;

public class SectionsFrontAdapter extends FixedFragmentStatePagerAdapter {

    private final ArrayList<Section> mSections;
    private final SectionFragmentFactory sectionFragmentFactory;

    public SectionsFrontAdapter(Context context, FragmentManager fm, List<Section> sections) {
        super(fm);
        mSections = new ArrayList<>(sections.size());
        mSections.addAll(sections);
        if (context instanceof SectionActivity) {
            sectionFragmentFactory = ((SectionActivity) context).getSectionFragmentFactory();
        } else {
            throw new IllegalArgumentException("Context must be an instance of SectionActivity");
        }
    }

    @Override
    public BaseSectionFragment getItem(int i) {
        BaseSectionFragment fragment = getFragment(i);
        if (fragment != null) {
            return fragment;
        }

        Section section = mSections.get(i);
        return sectionFragmentFactory.createFragment(section.getBundleName(), section.getName());
    }

    public BaseSectionFragment getFragment(int index) {
        List<Fragment> fragments = getFragments();
        if (fragments == null || fragments.size() <= index) {
            return null;
        }

        return (BaseSectionFragment) fragments.get(index);
    }

    @Override
    public int getCount() {
        return mSections.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mSections.get(position).getName();
    }

    @Override
    public int getItemPosition(Object object) {
        List<Fragment> fragments = getFragments();
        if (fragments == null) {
            return POSITION_NONE;
        }

        if (object instanceof BaseSectionFragment) {
            BaseSectionFragment sectionFragment = (BaseSectionFragment) object;
            String bundleName = sectionFragment.getBundleName();
            for (int i = 0; i < mSections.size(); i++) {
                Section section = mSections.get(i);
                if (TextUtils.equals(bundleName, section.getBundleName())) {
                    if (!isSectionSameType(sectionFragment, section)) return POSITION_NONE;
                    return i;
                }
            }
        }
        return POSITION_NONE;
    }

    private boolean isSectionSameType(BaseSectionFragment sectionFragment, Section section) {
        if (sectionFragmentFactory != null) {
            BaseSectionFragment newFragment = sectionFragmentFactory.createFragment(section.getBundleName(), section.getName());
            return newFragment.getClass() == sectionFragment.getClass();
        }
        return false;
    }

    public int findFirstSectionPos(String section) {
        if (mSections == null || section == null)
            return -1;

        for (int i = 0; i < getCount(); i++) {
            if (section.equals(mSections.get(i).getBundleName()) || section.equals(mSections.get(i).getBundleName().concat(".json"))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected String getKeyPrefix() {
        return "section_front_fragment_";
    }

    public void setSections(List<Section> sections) {
        mSections.clear();
        mSections.addAll(sections);
        notifyDataSetChanged();
    }

    public void onPageSelected(int pos) {
        /* no-op */
    }
}
