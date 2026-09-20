package com.wapo.flagship.features.sections;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import rx.Observable;

abstract class PageFragment extends Fragment {
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof PageManagerProvider)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Context must implement %s interface to use this fragment",
                            PageManagerProvider.class.getName()
                    )
            );
        }
    }

    protected rx.Observable<? extends PageManager> getPageManagerObs() {
        FragmentActivity activity = getActivity();
        if (activity instanceof PageManagerProvider) {
            return ((PageManagerProvider) activity).getPageManager();
        } else {
            return Observable.empty();
        }
    }
}
