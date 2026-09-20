package com.wapo.view.selection;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.washingtonpost.android.R;

import java.util.ArrayList;

/**
 * Created by a.kapitonov on 12.01.2015.
 */
public class SelectableLayoutOld extends FrameLayout {

    public static final int LAST_SYMBOL = -2;
    public static final int FIRST_SYMBOL = -3;
    private static final int DEFAULT_RIGHT_HANDLE_DRAWABLE_RES = com.wapo.view.R.drawable.text_select_handle_right;
    private static final int DEFAULT_LEFT_HANDLE_DRAWABLE_RES = com.wapo.view.R.drawable.text_select_handle_left;
    private static final int DEFAULT_SELECTION_COLOR_RES = R.color.text_selection;

    private View rightHandleView;
    private View leftHandleView;

    private int _yDelta;
    private int xx;
    private int yy;
    private int leftHandlePos = 0;
    private int rightHandlePos = 0;
    private boolean selectInProcess = false;
    private ArrayList<SelectableOld> selectables;
    private boolean needReplace = true;
    private float rightHandleCorrectX = 0;
    private float leftHandleCorrectX = 0;
    private ActionMode.Callback actionModeCallback;
    private ActionMode actionMode;

    private ShareCallback shareCallback;
    private boolean afterRestore = false;
    private float rightHandlePadding = 0.25f;
    private float leftHandlePadding = 0.25f;
    private int rightHandleDrawableRes = DEFAULT_RIGHT_HANDLE_DRAWABLE_RES;
    private int leftHandleDrawableRes = DEFAULT_LEFT_HANDLE_DRAWABLE_RES;
    private int selectionColor = DEFAULT_SELECTION_COLOR_RES;
    private boolean needChangeColor = false;

    public SelectableLayoutOld(Context context) {
        super(context);
        init(null);
    }

    public SelectableLayoutOld(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SelectableLayoutOld(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (gestureDetector != null)
            gestureDetector.onTouchEvent(ev);

        return super.onInterceptTouchEvent(ev);
    }

    private final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        public void onLongPress(MotionEvent e) {
            if (!selectInProcess) {
                startSelection(e);
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (selectInProcess) {
                stopSelection();
                if (actionMode != null)
                    actionMode.finish();

            }
            return super.onSingleTapUp(e);
        }
    });

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        addView(rightHandleView);
        addView(leftHandleView);
    }

    private void startSelection(MotionEvent e) {
        selectInProcess = setFirstCursorsPosition(e);
        if (selectInProcess && actionModeCallback != null) {
            final Context context = getContext();
            if (context instanceof AppCompatActivity) {
                final AppCompatActivity appCompatActivity = (AppCompatActivity) context;
                actionMode = appCompatActivity.startSupportActionMode(actionModeCallback);
            }
        }
    }

    private void init(AttributeSet attrs) {
        applyAttributes(attrs);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector != null)
                    gestureDetector.onTouchEvent(event);
                return true;
            }
        });
        selectInProcess = false;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), leftHandleDrawableRes);
        int w = bm.getWidth();
        int h = bm.getHeight();
        LayoutParams lp = new LayoutParams(w, h);
        leftHandleView = new View(getContext());
        setLeftHandle(leftHandleDrawableRes);
        leftHandleView.setLayoutParams(lp);

        rightHandleView = new View(getContext());
        setRightHandle(rightHandleDrawableRes);
        rightHandleView.setLayoutParams(lp);

        rightHandleView.setVisibility(View.INVISIBLE);
        leftHandleView.setVisibility(View.INVISIBLE);
        OnTouchListener cursorOnTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v1, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        v1.getParent().requestDisallowInterceptTouchEvent(true);
                        _yDelta = (int) (event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        xx = (int) (event.getRawX());
                        yy = (int) (event.getRawY() - _yDelta);
                        int oldrightCursorPos = rightHandlePos;
                        int oldleftCursorPos = leftHandlePos;
                        if (v1.equals(rightHandleView))
                            rightHandlePos = getCursorPosition(xx, yy, rightHandlePos);

                        if (v1.equals(leftHandleView))
                            leftHandlePos = getCursorPosition(xx, yy, leftHandlePos);

                        if (rightHandlePos != oldrightCursorPos || leftHandlePos != oldleftCursorPos) {
                            requestLayout();
                        }
                        break;
                }
                return true;
            }
        };

        rightHandleView.setOnTouchListener(cursorOnTouchListener);

        leftHandleView.setOnTouchListener(cursorOnTouchListener);

        actionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(com.washingtonpost.android.articles.R.menu.selection_menu, menu);
                return true;
            }
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == com.washingtonpost.android.articles.R.id.selection_menu_copy) {
                    copyToClipBoard(stopSelection().toString());
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                    return true;
                } else if (item.getItemId() == com.washingtonpost.android.articles.R.id.selection_menu_select_all) {
                    selectAll();
                    return true;
                } else if (item.getItemId() == com.washingtonpost.android.articles.R.id.selection_menu_share) {
                    copyToClipBoard(getSelection().toString());
                    shareSelection(stopSelection());
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                    return true;
                } else {
                    return false;
                }
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                stopSelection();
                actionMode = null;
            }
        };
    }

    public void setRightHandle(int rightHandleDrawableRes) {
        if (rightHandleDrawableRes == this.rightHandleDrawableRes)
            return;

        Bitmap bm = BitmapFactory.decodeResource(getResources(), rightHandleDrawableRes);
        int w = bm.getWidth();
        int h = bm.getHeight();
        rightHandleView.setLayoutParams(new LayoutParams(w, h));
        if (rightHandleDrawableRes == DEFAULT_RIGHT_HANDLE_DRAWABLE_RES){
            this.rightHandleDrawableRes = rightHandleDrawableRes;
            rightHandlePadding = 0.25f;
        } else {
            this.rightHandleDrawableRes = rightHandleDrawableRes;
            rightHandlePadding = 0;
        }
    }

    public void setLeftHandle(int leftHandleDrawableRes) {
        if (leftHandleDrawableRes == this.leftHandleDrawableRes)
            return;

        Bitmap bm = BitmapFactory.decodeResource(getResources(), leftHandleDrawableRes);
        int w = bm.getWidth();
        int h = bm.getHeight();
        leftHandleView.setLayoutParams(new LayoutParams(w, h));
        if (leftHandleDrawableRes == DEFAULT_LEFT_HANDLE_DRAWABLE_RES){
            this.leftHandleDrawableRes = leftHandleDrawableRes;
            leftHandlePadding = 0.25f;
        } else {
            this.leftHandleDrawableRes = leftHandleDrawableRes;
            leftHandlePadding = 0;
        }
    }

    private void applyAttributes(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(
                    attrs,
                    new int[]{
                            R.attr.leftHandle,
                            R.attr.rightHandle,
                            R.attr.leftHandlePos,
                            R.attr.rightHandlePos,
                            R.attr.selectionColor
                    }
            );

            try {
                int i = 0;
                if (a.hasValue(i)) {
                    setLeftHandle(a.getResourceId(i, DEFAULT_LEFT_HANDLE_DRAWABLE_RES));
                }
                i = 1;
                if (a.hasValue(i)) {
                    setRightHandle(a.getResourceId(i, DEFAULT_RIGHT_HANDLE_DRAWABLE_RES));
                }
                i = 2;
                if (a.hasValue(i)) {
                    leftHandlePadding = a.getFloat(i, 0);
                }
                i = 3;
                if (a.hasValue(i)) {
                    rightHandlePadding = a.getFloat(i, 0);
                }
                i = 4;
                if (a.hasValue(i)) {
                    selectionColor = a.getResourceId(i, R.color.text_selection);
                    needChangeColor = true;
                }
            } finally {
                a.recycle();
            }
        }
    }

    private void shareSelection(CharSequence selectedText) {
        if (shareCallback != null){
            shareCallback.share(selectedText);
        }
    }

    private void selectAll() {
        int totalLength = 0;
        for (SelectableOld stv : selectables) {
            if (stv.getVisibility() != VISIBLE)
                continue;
            totalLength += stv.getText().length();
        }
        rightHandlePos = totalLength-1;
        leftHandlePos = 0;
        rightHandleView.setBackgroundResource(rightHandleDrawableRes);
        leftHandleView.setBackgroundResource(leftHandleDrawableRes);
        needReplace = true;
        rightHandleCorrectX = 0 - rightHandleView.getWidth() * rightHandlePadding;
        leftHandleCorrectX = 0 - leftHandleView.getWidth() + leftHandleView.getWidth() * leftHandlePadding;
        drawCursor(rightHandleView, rightHandlePos, rightHandleCorrectX);
        drawCursor(leftHandleView, leftHandlePos, leftHandleCorrectX);
        setSelectionText();
    }

    private void checkBackground() {
        if (leftHandlePos > rightHandlePos && needReplace){
            rightHandleView.setBackgroundResource(leftHandleDrawableRes);
            leftHandleView.setBackgroundResource(rightHandleDrawableRes);
            rightHandleCorrectX = (rightHandleCorrectX - rightHandleView.getWidth() + rightHandleView.getWidth() * rightHandlePadding * 2);
            leftHandleCorrectX = (leftHandleCorrectX + leftHandleView.getWidth() - leftHandleView.getWidth() * leftHandlePadding * 2);
            needReplace = !needReplace;
            return;
        }
        if (leftHandlePos <= rightHandlePos && !needReplace){
            rightHandleView.setBackgroundResource(rightHandleDrawableRes);
            leftHandleView.setBackgroundResource(leftHandleDrawableRes);
            rightHandleCorrectX = (rightHandleCorrectX + rightHandleView.getWidth() - rightHandleView.getWidth() * rightHandlePadding * 2);
            leftHandleCorrectX = (leftHandleCorrectX - leftHandleView.getWidth() + leftHandleView.getWidth() * leftHandlePadding * 2);
            needReplace = !needReplace;
            return;
        }
    }


    private boolean setFirstCursorsPosition(MotionEvent e) {

        initTextViewSelectionArray();
        int totalPos = 0;
        int pos = -1;
        int cursorPos = 0;
        int[] location = new int[2];
        getLocationOnScreen(location);
        for (SelectableOld selectable : selectables){
            if (selectable.getVisibility() != View.VISIBLE)
                continue;

            int[] locationSelectable = new int[2];
            selectable.getLocationOnScreen(locationSelectable);
            int left = locationSelectable[0];
            int top = locationSelectable[1];
            int evX = (int) (e.getX() + location[0]);
            int evY = (int) (e.getY() + location[1]);
            if (selectable.isInside(evX, evY)) {
                pos = selectable.getOffsetForPosition(evX - left, evY - top, false);
                cursorPos = totalPos + pos;
                break;
            }
            totalPos += selectable.getText().length();
        }
        if (pos == -1){ //view not found
            return false;
        }


        leftHandlePos = cursorPos;
        rightHandlePos = leftHandlePos + 5;
        rightHandleView.setBackgroundResource(rightHandleDrawableRes);
        leftHandleView.setBackgroundResource(leftHandleDrawableRes);
        needReplace = true;
        rightHandleCorrectX = 0 - rightHandleView.getWidth() * rightHandlePadding;
        leftHandleCorrectX = 0 - leftHandleView.getWidth() + leftHandleView.getWidth()*leftHandlePadding;
        drawCursor(rightHandleView, rightHandlePos, rightHandleCorrectX);
        drawCursor(leftHandleView, leftHandlePos, leftHandleCorrectX);
        setSelectionText();
        return true;
    }

    private void initTextViewSelectionArray() {
        selectables = new ArrayList<SelectableOld>();
        findSelectableTextView(this);
    }

    private void findSelectableTextView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++){
            View view = viewGroup.getChildAt(i);
            if (view instanceof SelectableOld){
                selectables.add((SelectableOld) view);
                continue;
            }
            if (view instanceof ViewGroup){
                findSelectableTextView((ViewGroup)view);
            }
        }
    }

    private void drawCursor(View cursorView, int pos, float correctX) {
        SelectableOld textView = null;
        int totalPos = 0;

        for (SelectableOld stv : selectables){
            if (stv.getVisibility() != VISIBLE)
                continue;

            String text = stv.getText().toString();
            int length = text.length();
            if (pos >= totalPos && pos < totalPos + length) {
                textView = stv;
                break;
            }
            totalPos += length;
        }

        if (textView == null)
            return;
        float[] coordinate = new float[2];
        coordinate = textView.getPositionForOffset(pos - totalPos, coordinate);
        int[] location = new int[2];
        getLocationOnScreen(location);
        if (coordinate[0] == -1 || coordinate[1] == -1)
            return;

        cursorView.setVisibility(View.VISIBLE);
        float x = coordinate[0] - location[0] + correctX;
        float y = coordinate[1] - location[1];
        cursorView.setX(x);
        cursorView.setY(y);
    }

    private void setSelectionText() {
        int totalPos = 0;
        int start;
        int end;
        int leftCursorPos = this.leftHandlePos;
        int rightCursorPos = this.rightHandlePos;
        if (leftCursorPos > rightCursorPos){
            rightCursorPos = this.leftHandlePos;
            leftCursorPos = this.rightHandlePos;
        }
        for (SelectableOld stv : selectables){
            if (stv.getVisibility() != VISIBLE)
                continue;

            String text = stv.getText().toString();
            int length = text.length();
            start = 0;
            end = 0;
            if (totalPos <= leftCursorPos && leftCursorPos < totalPos + length) {
                if (totalPos < rightCursorPos && rightCursorPos <= totalPos + length) {
                    start = leftCursorPos - totalPos;
                    end = rightCursorPos - totalPos;
                } else {
                    start = leftCursorPos - totalPos;
                    end = length;
                }
            }
            if (totalPos > leftCursorPos && totalPos + length <= rightCursorPos) {
                start = 0;
                end = length;
            }
            if (totalPos > leftCursorPos && totalPos + length > rightCursorPos && totalPos < rightCursorPos) {
                start = 0;
                end = rightCursorPos - totalPos;
            }
            stv.selectText(start, end);
            totalPos += length;
        }
    }

    public CharSequence getSelection() {
        return getSelection(false);
    }

    private CharSequence getSelection(boolean reset) {
        if (selectables == null) {
            return "";
        }

        SpannableStringBuilder strBuilder = new SpannableStringBuilder();
        for (SelectableOld selectable : selectables) {
            if (selectable.getVisibility() != VISIBLE)
                continue;

            strBuilder.append(selectable.getSelectedText()).append("\n");
            if (reset) {
                selectable.selectText(0, 0);
            }
        }
        return strBuilder;
    }

    private CharSequence stopSelection() {
        CharSequence selection = getSelection(true);

        selectInProcess = false;
        rightHandleView.setVisibility(View.INVISIBLE);
        leftHandleView.setVisibility(View.INVISIBLE);
        selectables = null;

        return selection;
    }

    private void copyToClipBoard(String s) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Article", s);
        clipboard.setPrimaryClip(clip);
    }

    private int getCursorPosition(float x, float y, int currCursorPosition) {
        int cursorPos = getCursorPosition(x, y);
        if (cursorPos == -1)
            return currCursorPosition;

        return cursorPos;
    }

    private int getCursorPosition(float x, float y) {
        int totalPos = 0;
        int pos = 0;
        int cursorPos = -1;

        if (selectables != null) {
            for (SelectableOld stv : selectables) {
                if (stv.getVisibility() != VISIBLE)
                    continue;

                int[] locationFTV = new int[2];
                stv.getLocationOnScreen(locationFTV);
                int left = locationFTV[0];
                int right = left + stv.getWidth();
                int top = locationFTV[1];
                int bottom = top + stv.getHeight();
                int[] location = new int[2];
                getLocationOnScreen(location);
                if (top <= y && bottom >= y) {
                    if (left <= x && right >= x) {
                        pos = stv.getOffsetForPosition((int) (x - left), (int) (y - top), true);
                        cursorPos = totalPos + pos;
                        break;
                    } else if (x < left) {
                        pos = stv.getOffsetForPosition(FIRST_SYMBOL, (int) (y - top), true);
                        cursorPos = totalPos + pos;
                        break;
                    } else if (x > right) {
                        pos = stv.getOffsetForPosition(LAST_SYMBOL, (int) (y - top), true);
                        cursorPos = totalPos + pos;
                        break;
                    }
                }
                totalPos += stv.getText().length();
            }
        }

        if (pos == -1)
            return pos;
        else
            return cursorPos;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.rightHandlePos = rightHandlePos;
        state.leftHandlePos = leftHandlePos;
        state.needReplace = needReplace;
        state.selectInProcess = selectInProcess;
        state.rightHandleCorrectX = rightHandleCorrectX;
        state.leftHandleCorrectX = leftHandleCorrectX;
        state.rightHandlePadding = rightHandlePadding;
        state.leftHandlePadding = leftHandlePadding;
        state.rightHandleDrawableRes = rightHandleDrawableRes;
        state.leftHandleDrawableRes = leftHandleDrawableRes;
        state.selectionColor = selectionColor;

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable ss) {
        if (!(ss instanceof SavedState)) {
            super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
            return;
        }

        SavedState state = (SavedState) ss;
        super.onRestoreInstanceState(state.getSuperState());
        rightHandlePos = state.rightHandlePos;
        leftHandlePos = state.leftHandlePos;
        needReplace = state.needReplace;
        selectInProcess = state.selectInProcess;
        rightHandleCorrectX = state.rightHandleCorrectX;
        leftHandleCorrectX = state.leftHandleCorrectX;

        rightHandlePadding = state.rightHandlePadding;
        leftHandlePadding = state.leftHandlePadding;
        rightHandleDrawableRes = state.rightHandleDrawableRes;
        leftHandleDrawableRes = state.leftHandleDrawableRes;
        selectionColor = state.selectionColor;
        needChangeColor = true;
        needReplace = leftHandlePos > rightHandlePos;
        afterRestore = true;
        checkBackground();
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!selectInProcess)
            return;

        if (afterRestore) {
            afterRestore = false;
            if (actionModeCallback != null) {
                final Context context = getContext();
                if (context instanceof AppCompatActivity) {
                    final AppCompatActivity appCompatActivity = (AppCompatActivity) context;
                    actionMode = appCompatActivity.startSupportActionMode(actionModeCallback);
                }
            }
        actionMode.invalidate();
            initTextViewSelectionArray();
        }
        applySelectionColor();
        setSelectionText();
        checkBackground();
        drawCursor(rightHandleView, rightHandlePos, rightHandleCorrectX);
        drawCursor(leftHandleView, leftHandlePos, leftHandleCorrectX);
    }

    private void applySelectionColor() {
        if (needChangeColor && selectables != null  ){
            needChangeColor = false;
            for(SelectableOld selectable : selectables) {
                selectable.setColor(selectionColor);
            }
        }
    }
    public void setSelectionColor(int selectionColorRes) {
        if (selectionColorRes == selectionColor)
            return;

        needChangeColor = true;
        selectionColor = selectionColorRes;
        invalidate();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof SelectableOld){
            ((SelectableOld)child).setColor(selectionColor);
        }

        super.addView(child, index, params);
    }

    public ShareCallback getShareCallback() {
        return shareCallback;
    }

    public void setShareCallback(ShareCallback shareCallback) {
        this.shareCallback = shareCallback;
    }

    public void resetSelect() {
        stopSelection();
        if (actionMode != null)
            actionMode.finish();
    }

    public interface ShareCallback{
        void share(CharSequence text);
    }

    public static class SavedState extends BaseSavedState {
        public int rightHandlePos;
        public int leftHandlePos;
        public boolean selectInProcess;
        public boolean needReplace;
        public float rightHandleCorrectX;
        public float leftHandleCorrectX;
        public float rightHandlePadding;
        public float leftHandlePadding;
        public int rightHandleDrawableRes;
        public int leftHandleDrawableRes;
        public int selectionColor;
        public static Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public SavedState(Parcel source) {
            super(source);
            rightHandlePos = source.readInt();
            leftHandlePos = source.readInt();
            selectInProcess = source.readInt() == 1;
            needReplace = source.readInt() == 1;
            rightHandleCorrectX = source.readFloat();
            leftHandleCorrectX = source.readFloat();
            rightHandlePadding = source.readFloat();
            leftHandlePadding = source.readFloat();
            rightHandleDrawableRes = source.readInt();
            leftHandleDrawableRes = source.readInt();
            selectionColor = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(rightHandlePos);
            dest.writeInt(leftHandlePos);
            dest.writeInt(selectInProcess ? 1 : 0);
            dest.writeInt(needReplace ? 1 : 0);
            dest.writeFloat(rightHandleCorrectX);
            dest.writeFloat(leftHandleCorrectX);
            dest.writeFloat(rightHandlePadding);
            dest.writeFloat(leftHandlePadding);
            dest.writeInt(rightHandleDrawableRes);
            dest.writeInt(leftHandleDrawableRes);
            dest.writeInt(selectionColor);
        }
    }
}
