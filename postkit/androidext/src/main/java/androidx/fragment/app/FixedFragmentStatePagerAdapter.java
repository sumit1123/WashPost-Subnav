package androidx.fragment.app;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewpager.widget.PagerAdapter;

import com.wapo.android.commons.util.Logger;
import com.wapo.android.commons.util.ViewPagerUtils;

import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class FixedFragmentStatePagerAdapter extends PagerAdapter {
    private static final String TAG = "FragmentStatePagerAdapter";
    private static final boolean DEBUG = false;

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;

    private ArrayList<Fragment.SavedState> mSavedState = new ArrayList<Fragment.SavedState>();
    private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
    private Fragment mCurrentPrimaryItem = null;

    public FixedFragmentStatePagerAdapter(FragmentManager fm) {
        mFragmentManager = fm;
    }

    /**
     * Return the Fragment associated with a specified position.
     */
    public abstract Fragment getItem(int position);

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment f = instantiateFragment(container, position);
        Bundle savedFragmentState = f.mSavedFragmentState;
        if (savedFragmentState != null) {
            savedFragmentState.setClassLoader(f.getClass().getClassLoader());
        }
        return f;
    }

    private Fragment instantiateFragment(ViewGroup container, int position) {
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        if (mFragments.size() > position) {
            Fragment f = mFragments.get(position);
            if (f != null) {
                return f;
            }
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        Fragment fragment = getItem(position);
        Logger.v(TAG, "Adding item #" + position + ": f=" + fragment);
        if (mSavedState.size() > position) {
            Fragment.SavedState fss = mSavedState.get(position);
            if (fss != null) {
                fragment.setInitialSavedState(fss);
            }
        }
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        mFragments.set(position, fragment);
        mCurTransaction.add(container.getId(), fragment);

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        Logger.v(TAG, "Removing item #" + position + ": f=" + object
                + " v=" + ((Fragment)object).getView());
        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }

        mSavedState.set(position, fragment.isAdded()
                ? mFragmentManager.saveFragmentInstanceState(fragment) : null);
        mFragments.set(position, null);

        mCurTransaction.remove(fragment);

        if (fragment == mCurrentPrimaryItem) {
            mCurrentPrimaryItem = null;
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
                mCurrentPrimaryItem.isVisible();
                if (mCurrentPrimaryItem instanceof ViewPagerUtils.WapoPageCallbacks) {
                    ((ViewPagerUtils.WapoPageCallbacks) mCurrentPrimaryItem).onPageUnselected();
                }
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
                if (fragment instanceof ViewPagerUtils.WapoPageCallbacks) {
                    Lifecycle lifecycle = fragment.getLifecycle();
                    if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        ((ViewPagerUtils.WapoPageCallbacks) fragment).onPageSelected();
                    } else {
                        lifecycle.addObserver(new DefaultLifecycleObserver() {
                            @Override
                            public void onStart(@NonNull LifecycleOwner owner) {
                                ((ViewPagerUtils.WapoPageCallbacks) fragment).onPageSelected();
                                owner.getLifecycle().removeObserver(this);
                            }
                        });
                    }
                }
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mSavedState.size() > 0) {
            state = new Bundle();
            Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
            mSavedState.toArray(fss);
            state.putParcelableArray("states", fss);
        }
        for (int i=0; i<mFragments.size(); i++) {
            Fragment f = mFragments.get(i);
            if (f != null && f.isAdded()) {
                if (state == null) {
                    state = new Bundle();
                }
                String key = getKeyPrefix() + i;
                mFragmentManager.putFragment(state, key, f);
            }
        }
        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle)state;
            bundle.setClassLoader(loader);
            Parcelable[] fss = bundle.getParcelableArray("states");
            mSavedState.clear();
            mFragments.clear();
            if (fss != null) {
                for (int i=0; i<fss.length; i++) {
                    mSavedState.add((Fragment.SavedState)fss[i]);
                }
            }
            Iterable<String> keys = bundle.keySet();
            String prefix = getKeyPrefix();
            int prefixLength = prefix.length();
            for (String key: keys) {
                if (key.startsWith(prefix)) {
                    int index = Integer.parseInt(key.substring(prefixLength));
                    Fragment f = mFragmentManager.getFragment(bundle, key);
                    if (f != null) {
                        while (mFragments.size() <= index) {
                            mFragments.add(null);
                        }
                        f.setMenuVisibility(false);
                        mFragments.set(index, f);
                    } else {
                        Logger.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
    }

    protected String getKeyPrefix() {
        return "f";
    }

    protected List<Fragment> getFragments() {
        return mFragments;
    }

    protected void reorderFragments(SparseIntArray moves) {
        if (moves == null) {
            return;
        }

        ArrayList<Fragment> newFragments = new ArrayList<Fragment>(mFragments);
        HashSet<Integer> updates = new HashSet<Integer>();
        for (int i = 0; i < moves.size(); i++) {
            int newPos = moves.valueAt(i);
            int oldPos = moves.keyAt(i);

            if (oldPos < 0 || oldPos >= mFragments.size()) {
                continue;
            }

            if (newPos == POSITION_NONE) {
                newFragments.set(oldPos, null);
                updates.add(oldPos);
            } else if (newPos == POSITION_UNCHANGED) {
                newPos = oldPos;
            }

            if (newPos >= 0) {
                while(newPos >= newFragments.size()) {
                    newFragments.add(null);
                }

                newFragments.set(newPos, mFragments.get(oldPos));
                updates.add(newPos);

                if (!updates.contains(oldPos)) {
                    newFragments.set(oldPos, null);
                }
            }
        }
        mFragments = newFragments;
    }
}