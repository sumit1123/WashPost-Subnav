package com.wapo.view.share;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.fragment.app.Fragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.wapo.view.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShareFragment extends Fragment {
    private static final String TAG = ShareFragment.class.getName();
    private static final String PARAM_INTENTS = TAG + ".intents";
    private static final String SHOW_ACTION_PARAM = TAG + ".showAction";
    private static final String PARAM_DIALOG_THEME = TAG + ".theme";
    public static final String SHARE_TYPE = TAG + ".shareType";
    private static final String SHOULD_SHOW_DIALOG = TAG + ".shouldShowDialog";

    public static final String SHARE_FRAGMENT_TAG = TAG + ".fragmentTag";

    private GridView _grid;
    private boolean _isVisible = false;
    private Dialog _dialog;
    private ActivitiesAdapter _adapter;
    private OnActivitySelectedListener _listener;
    private InstanceStateChangeListener _instanceStateChangeListener;
    private Context _context;
    private CharSequence sharedText;
    private boolean wasDismissedFromOnStop;
    OnShareFragmentDismissListener _sharedFragmentDetachListener;

    public static ShareFragment create(Intent[] intents, int theme) {
        Bundle args = new Bundle();
        args.putParcelableArray(PARAM_INTENTS, intents);
        args.putInt(PARAM_DIALOG_THEME, theme);
        ShareFragment fragment = new ShareFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _isVisible = savedInstanceState.getBoolean(SHOW_ACTION_PARAM, false);
            if (_instanceStateChangeListener != null) {
                _instanceStateChangeListener.onRestoreInstanceState(savedInstanceState);
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle args = getArguments();

        _context = args.containsKey(PARAM_DIALOG_THEME) ?
                new ContextThemeWrapper(context, args.getInt(PARAM_DIALOG_THEME)) :
                context;

        Parcelable[] intentParcelables = args.getParcelableArray(PARAM_INTENTS);
        if (intentParcelables != null) {
            List<Intent> intents = new ArrayList<>();
            for (Parcelable p : intentParcelables) {
                if (p instanceof Intent) {
                    intents.add((Intent) p);
                }
            }
            if (!intents.isEmpty()) {
                _adapter = new ActivitiesAdapter(
                        _context,
                        R.layout.fragment_share_list_item,
                        intents.toArray(new Intent[0])
                );
            }
        }
    }

    @Override
    public void onDetach() {
        _context = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);
        _grid = (GridView) view.findViewById(R.id.grid);
        int width = 2 * (int) getResources().getDimension(android.R.dimen.app_icon_size);
        _grid.setColumnWidth(width);
        _grid.setAdapter(_adapter);
        _grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (_listener != null) {
                    ActivitiesAdapter.ListItem item = _adapter.getItem(position);
                    Intent intent = (Intent) item.intent.clone();
                    intent.putExtra(ShareFragment.SHARE_TYPE, item.label);
                    _listener.onActivitySelected(intent, new HashSet<>(item.groups));
                }
            }
        });
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(SHOULD_SHOW_DIALOG)) {
                showDialog();
            }
            if (_instanceStateChangeListener != null) {
                _instanceStateChangeListener.onRestoreInstanceState(savedInstanceState);
            }
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        _grid = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.share_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);

        boolean isVisible = _isVisible && _adapter != null && !_adapter.isEmpty();
        if (shareItem != null) {
            shareItem.setVisible(isVisible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share && _adapter != null && !_adapter.isEmpty()) {
            sharedText = null;
            showDialog();
            return true;
        }
        return false;
    }

    @Override
    public void onStop() {
        if (_dialog != null) {
            wasDismissedFromOnStop=true;
            _dialog.cancel();
            _dialog = null;
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOW_ACTION_PARAM, _isVisible);
        if (_dialog != null && _dialog.isShowing()) {
            outState.putBoolean(SHOULD_SHOW_DIALOG, true);
        }
        if (_instanceStateChangeListener != null) {
            _instanceStateChangeListener.onSaveInstanceState(outState);
        }
    }

    public boolean isActionVisible() {
        return _isVisible;
    }

    public void setActionVisible(boolean isVisible) {
        _isVisible = isVisible;
    }

    public void setActivitySelectedListener(OnActivitySelectedListener _listener) {
        this._listener = _listener;
    }

    public void setInstanceStateChangeListener(InstanceStateChangeListener instanceStateChangeListener) {
        this._instanceStateChangeListener = instanceStateChangeListener;
    }

    public void setSharedFragmentDetachListener(OnShareFragmentDismissListener sharedFragmentDetachListener) {
        this._sharedFragmentDetachListener = sharedFragmentDetachListener;
    }

    public void showDialog() {
        if (_adapter == null || _adapter.isEmpty()) {
            return;
        }

        if (_adapter.getCount() > 1) {
            _dialog = getDialog();
        } else if (_listener != null) {
            ActivitiesAdapter.ListItem activityItem = _adapter.getItem(0);
            _listener.onActivitySelected((Intent) activityItem.intent.clone(), new HashSet<>(activityItem.groups));
        }
    }

    private Dialog getDialog() {
        if (_context == null) {
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(_context, getArguments().getInt(PARAM_DIALOG_THEME, 0));

        AlertDialog dialog = builder
                .setTitle(_context.getString(R.string.share_using_msg))
                .setCancelable(true)
                .setView(onCreateView(LayoutInflater.from(_context), null, null))
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (dialog != null) {
                            if (_sharedFragmentDetachListener != null && !wasDismissedFromOnStop) {
                                //Triggering Listener on dismissal
                                _sharedFragmentDetachListener.onShareFragmentDismiss();
                            }
                            dialog.dismiss();
                            resetWasDismissedFromOnStop();
                        }
                        onDestroyView();
                    }
                })
                .show();

        if (_adapter != null) {
            _adapter.setContext(dialog.getContext());
        }

        return dialog;
    }

    private void resetWasDismissedFromOnStop() {
        wasDismissedFromOnStop=false;
    }

    public interface OnActivitySelectedListener {
        void onActivitySelected(Intent intent, Set<Integer> groups);
    }

    public interface InstanceStateChangeListener {
        void onSaveInstanceState(Bundle bundle);
        void onRestoreInstanceState(Bundle bundle);
    }

    //Listener to track Share Fragment Dismisssal
    public interface OnShareFragmentDismissListener {
        void onShareFragmentDismiss();
    }

}
