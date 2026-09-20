package com.wapo.view.selection;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.os.Looper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Toast;


import com.wapo.view.BuildConfig;
import com.wapo.view.R;

import java.util.ArrayList;

public class SelectionController {

    private static final boolean D = BuildConfig.DEBUG;

    private ViewGroup selectableViewGroup;

    private static final int DEFAULT_CURSOR_HEIGTH = 70;
    private static final int DEFAULT_CURSOR_WIDTH = 50;
    private static final int DEFAULT_RIGHT_HANDLE_DRAWABLE_RES = R.drawable.text_select_handle_right;
    private static final int DEFAULT_LEFT_HANDLE_DRAWABLE_RES = R.drawable.text_select_handle_left;
    private static final int DEFAULT_SELECTION_COLOR_RES = R.color.text_selection;

    private ArrayList<SelectableInfo> selectableInfos = new ArrayList<>();
    private Handle rightHandle;

    private Handle leftHandle;
    private boolean selectInProcess = false;
    private boolean needReplace = true;

    private float rightHandlePadding = 0.25f;
    private float leftHandlePadding = 0.25f;
    private GestureDetector gestureDetector;
    private HandleTouchEvent rightHandleListener;

    private HandleTouchEvent leftHandleListener;
    private SelectionCallback selectionCallback;
    private SelectionEnableListener selectionEnableListener;

    public SelectionController(ViewGroup selectableViewGroup) {
        this.selectableViewGroup = selectableViewGroup;
        initHandles();
        initHandlesEvents();
        initGesture();
    }

    public boolean isSelectionActive() {
        return selectInProcess;
    }

    private void setHandlersValues() {
        rightHandle.setDefaultValues();
        leftHandle.setDefaultValues();

        rightHandle.setHandleImage(BitmapFactory.decodeResource(selectableViewGroup.getResources(), DEFAULT_RIGHT_HANDLE_DRAWABLE_RES));
        leftHandle.setHandleImage(BitmapFactory.decodeResource(selectableViewGroup.getResources(), DEFAULT_LEFT_HANDLE_DRAWABLE_RES));
    }

    private void initHandlesEvents() {
        rightHandleListener = new HandleTouchEvent(rightHandle);
        leftHandleListener = new HandleTouchEvent(leftHandle);
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

        Rect rect = null;
        for (SelectableInfo selectableInfo : selectableInfos) {
            final Selectable stv = selectableInfo.getSelectable();
            if (stv != null && stv.getVisibility() == View.VISIBLE) {

                rect = stv.getScreenRect(rect);
                int left = rect.left;
                int right = left + stv.getWidth();
                int top = rect.top;
                int bottom = top + stv.getHeight();

                float scaleX = stv.getWidth() == 0 ? 0 : (float) rect.width() / stv.getWidth();
                float scaleY = stv.getHeight() == 0 ? 0 : (float) rect.height() / stv.getHeight();

                if (top <= y && bottom >= y) {
                    if (left <= x && right >= x) {
                        int scaledX = Math.round((x - left) / scaleX);
                        int scaledY = Math.round((y - top) / scaleY);
                        pos = stv.getOffsetForPosition(scaledX, scaledY);
                        cursorPos = totalPos + pos;
                        break;
                    } else if (x < left) {
                        int scaledY = Math.round((y - top) / scaleY);
                        pos = stv.getOffsetForPosition(Selectable.FIRST_SYMBOL, scaledY);
                        cursorPos = totalPos + pos;
                        break;
                    } else if (x > right) {
                        int scaledY = Math.round((y - top) / scaleY);
                        pos = stv.getOffsetForPosition(Selectable.LAST_SYMBOL, scaledY);
                        cursorPos = totalPos + pos;
                        break;
                    }
                }
            }

            totalPos += selectableInfo.getText().length();
        }

        if (pos == -1)
            return pos;
        else
            return cursorPos;
    }

    private void initHandles() {
        rightHandle = new Handle();
        leftHandle = new Handle();
    }

    private void initGesture() {
        gestureDetector = Build.VERSION.SDK_INT < 11 ? null : new GestureDetector(selectableViewGroup.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (!selectInProcess && isEnabled()) {
                    startSelection(e);
                }
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (selectInProcess) {
                    stopSelection();
                }
                return super.onSingleTapUp(e);
            }
        });
    }

    private boolean isEnabled() {
        return selectionEnableListener == null || selectionEnableListener.isEnabled();
    }

    void addViewToSelectable(View view) {
        checkSelectableList();
        if (view instanceof Selectable){
            addSelectableToSelectableInfos((Selectable) view);
        } else if (view instanceof ViewGroup){
            findSelectableTextView((ViewGroup) view);
        }
    }

    void findSelectableTextView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++){
            View view = viewGroup.getChildAt(i);
            if (view instanceof Selectable){
                addSelectableToSelectableInfos((Selectable) view);
                continue;
            }
            if (view instanceof ViewGroup){
                findSelectableTextView((ViewGroup) view);
            }
        }
    }

    private void addSelectableToSelectableInfos(Selectable selectable) {
        boolean found = false;
        for (SelectableInfo selectableInfo : selectableInfos) {
            if (TextUtils.equals(selectableInfo.getKey(), selectable.getKey())) {
                selectableInfo.setSelectable(selectable);
                found = true;
                break;
            }
        }
        if (!found) {
            final SelectableInfo selectableInfo = new SelectableInfo(selectable);
            selectableInfos.add(selectableInfo);
        }
    }

    void checkSelectableList() {
        for (SelectableInfo selectableInfo : selectableInfos) {
            if (selectableInfo.getSelectable() != null) {
                if (!TextUtils.equals(selectableInfo.getSelectable().getKey(), selectableInfo.getKey())) {
                    selectableInfo.removeSelectable();
                    continue;
                }

                if (!isSelectableViewGroupParent((View) selectableInfo.getSelectable())) {
                    selectableInfo.removeSelectable();
                }
            }
        }

    }

    private boolean isSelectableViewGroupParent(View view) {
        ViewParent parent = view.getParent();
        while (parent != null && !parent.equals(selectableViewGroup)) {
            parent = parent.getParent();
        }
        return parent != null;
    }

    void drawHandles(Canvas canvas) {

        if (!selectInProcess) {
            return;
        }

        rightHandle.draw(canvas);
        leftHandle.draw(canvas);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }

        boolean dispatched = false;

        if (selectInProcess) {
            boolean right = rightHandleListener.onTouchHandle(ev);
            boolean left = leftHandleListener.onTouchHandle(ev);
            dispatched = right || left;
        }

        return dispatched;
    }

    private void startSelection(MotionEvent e) {
        selectInProcess = setFirstCursorsPosition(e);
        if (selectInProcess && selectionCallback != null) {
            selectionCallback.startSelection((SelectableView) selectableViewGroup);
        }
    }

    private boolean setFirstCursorsPosition(MotionEvent e) {
        setHandlersValues();
        int totalPos = 0;
        int pos = -1;
        String text = "";

        Rect rect = new Rect();

        for (SelectableInfo selectableInfo : selectableInfos) {
            final Selectable selectable = selectableInfo.getSelectable();
            if (selectable != null && selectable.getVisibility() == View.VISIBLE) {
                rect = selectable.getScreenRect(rect);

                int evX = (int) e.getRawX();
                int evY = (int) e.getRawY();

                if (rect.contains(evX, evY)) {
                    float scakeX = selectable.getWidth() == 0 ? 0 : (float)rect.width() / selectable.getWidth();
                    float scaleY = selectable.getHeight() == 0 ? 0 : (float) rect.height() / selectable.getHeight();

                    int scaledX = Math.round((evX - rect.left) / scakeX);
                    int scaledY = Math.round((evY - rect.top) / scaleY);
                    pos = selectable.getOffsetForPosition(scaledX, scaledY);
                    text = selectable.getText().toString();
                    break;
                }
            }
            totalPos += selectableInfo.getText().length();
        }
        if (pos == -1) { //view not found
            return false;
        }

        int[] handlesPosition = getHandlesPosition(text, pos);

        leftHandle.position = handlesPosition[0] + totalPos;
        rightHandle.position = handlesPosition[1] + totalPos;
        needReplace = true;
        rightHandle.correctX = 0 - rightHandle.width * rightHandlePadding;
        leftHandle.correctX = 0 -  leftHandle.width + leftHandle.width * leftHandlePadding;
        setHandleCoordinate(rightHandle);
        setHandleCoordinate(leftHandle);
        setSelectionText();

        selectableViewGroup.invalidate();

        return true;
    }

    private int[] getHandlesPosition(final String text, final int pos) {
        final int[] handlesPosition = new int[2];
        final int textLength = text.length();
        // avoiding StringIndexOutOfBoundsException
        // we're bounding by textLength - 2 to avoid selection of single last character of paragraph
        // usually, it's punctuation, like dot, question or bang
        final int checkedPos = Math.max(0, Math.min(textLength - 2, pos));
        handlesPosition[0] = 0;
        for (int i = checkedPos; i >= 0; i--) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                handlesPosition[0] = i + 1;
                break;
            }
        }

        handlesPosition[1] = textLength - 1;
        for (int i = checkedPos; i < textLength; i++) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                handlesPosition[1] = i;
                break;
            }
        }

        if (handlesPosition[0] > handlesPosition[1]) {
            final int temp = handlesPosition[0];
            handlesPosition[0] = handlesPosition[1];
            handlesPosition[1] = temp;
        }

        return handlesPosition;
    }


    private void setSelectionText() {
        int totalPos = 0;
        int start;
        int end;
        int leftCursorPos = leftHandle.position;
        int rightCursorPos = rightHandle.position;
        if (leftCursorPos > rightCursorPos) {
            rightCursorPos = leftHandle.position;
            leftCursorPos = rightHandle.position;
        }
        for (SelectableInfo selectableInfo : selectableInfos) {

            String text = selectableInfo.getText().toString();
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
            selectableInfo.selectText(start, end);
            totalPos += length;
        }
    }

    private CharSequence stopSelection() {
        if (!selectInProcess) {
            return "";
        }

        CharSequence selection = getSelection(true);

        selectInProcess = false;
        selectableViewGroup.invalidate();

        if (selectionCallback != null) {
            selectionCallback.stopSelection((SelectableView) selectableViewGroup);
        }

        return selection;
    }

    private void copyToClipBoard(String s) {
        ClipboardManager clipboard = (ClipboardManager) selectableViewGroup.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Article", s);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(selectableViewGroup.getContext(), "Selected text was copied", Toast.LENGTH_LONG).show();
    }

    private CharSequence getSelection(boolean reset) {
        SpannableStringBuilder strBuilder = new SpannableStringBuilder();
        for (SelectableInfo selectableInfo : selectableInfos) {
            final CharSequence selectedText = selectableInfo.getSelectedText();

            strBuilder.append(selectedText);

            if (!selectedText.toString().isEmpty()) {
                strBuilder.append("\n");
            }

            if (reset) {
                selectableInfo.resetSelection();
            }
        }
        if (strBuilder.length() > 0) {
            strBuilder.delete(strBuilder.length() - 1, strBuilder.length());
        }
        return strBuilder;
    }

    private void setHandleCoordinate(Handle handle) {
        Selectable textView = null;
        int totalPos = 0;
        for (SelectableInfo selectableInfo : selectableInfos) {
            String text = selectableInfo.getText().toString();
            int length = text.length();
            if (length == 0 && selectableInfo.getSelectable() != null && selectableInfo.getSelectable().getText() != null) {
                text = selectableInfo.getSelectable().getText().toString();
                length = text.length();
            }
            if (handle.position >= totalPos && handle.position < totalPos + length) {
                textView = selectableInfo.getSelectable();
                break;
            }
            totalPos += length;
        }
        if (textView == null) {
            handle.visible = false;
            return;
        }

        if (!isSelectableViewGroupParent((View)textView)) {
            handle.visible = false;
            checkSelectableList();
            return;
        }

        handle.visible = true;

        float[] coordinate = new float[2];
        coordinate = textView.getScreenPositionForOffset(handle.position - totalPos, coordinate);

        if (coordinate[0] == -1 || coordinate[1] == -1) {
            return;
        }

        Rect rect = getViewScreenRect(selectableViewGroup, null);
        float scaleX = selectableViewGroup.getWidth() == 0 ? 1 : (float) rect.width() / selectableViewGroup.getWidth();
        float scaleY = selectableViewGroup.getHeight() == 0 ? 1 : (float) rect.height() / selectableViewGroup.getHeight();

        handle.x = (coordinate[0] + handle.correctX - rect.left) / scaleX;
        handle.y = (coordinate[1] - rect.top) / scaleY;

    }

    private void checkBackground() {
        if (leftHandle.position > rightHandle.position && needReplace){
            Bitmap temp = rightHandle.handleImage;
            rightHandle.setHandleImage(leftHandle.handleImage);
            leftHandle.setHandleImage(temp);
            rightHandle.correctX = (rightHandle.correctX - rightHandle.width + rightHandle.width * rightHandlePadding * 2);
            leftHandle.correctX = (leftHandle.correctX + leftHandle.width - leftHandle.width * leftHandlePadding * 2);
            needReplace = ! needReplace;
            setHandleCoordinate(rightHandle);
            setHandleCoordinate(leftHandle);
            return;
        }
        if (leftHandle.position <= rightHandle.position && !needReplace){
            Bitmap temp = rightHandle.handleImage;
            rightHandle.setHandleImage(leftHandle.handleImage);
            leftHandle.setHandleImage(temp);
            rightHandle.correctX = (rightHandle.correctX + rightHandle.width - rightHandle.width * rightHandlePadding * 2);
            leftHandle.correctX = (leftHandle.correctX - leftHandle.width + leftHandle.width * leftHandlePadding * 2);
            needReplace = !needReplace;
            setHandleCoordinate(rightHandle);
            setHandleCoordinate(leftHandle);
        }
    }

    void checkHandlesPosition() {
        if (!selectInProcess) {
            return;
        }

        setHandleCoordinate(rightHandle);
        setHandleCoordinate(leftHandle);
        selectableViewGroup.postInvalidate();
    }

    void setSelectionCallback(SelectionCallback selectionCallback) {
        this.selectionCallback = selectionCallback;
    }

     void copyTextToClipboard() {
        copyToClipBoard(stopSelection().toString());
    }

    public String getSelectedText() {
        return stopSelection().toString();
    }

    void resetSelection() {
        stopSelection();
    }

    public void setSelectionEnableListener(SelectionEnableListener selectionEnableListener) {
        this.selectionEnableListener = selectionEnableListener;
    }

    private class Handle {
        private float x;
        private float y;
        private float width;
        private float height;
        private int position;
        private float correctX;
        private boolean isMoving;
        private boolean visible = true;
        private Bitmap handleImage;
        private Paint paint = new Paint();

        Handle() {
            setDefaultValues();
        }

        void setDefaultValues() {
            paint.setColor(Color.RED);
            x = 0;
            y = 0;
            correctX = 0;
            position = -1;
            width = DEFAULT_CURSOR_WIDTH;
            height = DEFAULT_CURSOR_HEIGTH;
            isMoving = false;
        }

        public boolean contains(float x, float y) {
            return this.x <= x
                    && this.y <= y
                    && (this.x + width) >= x
                    && (this.y + height) >= y;
        }

        public void draw(Canvas canvas) {
            if (!visible)
                return;

            if (handleImage == null) {
                canvas.drawRect(x, y, x + width, y + height, paint);
            } else {
                canvas.drawBitmap(handleImage, x, y, paint);
            }
        }

        void setHandleImage(Bitmap bitmap) {
            handleImage = bitmap;
            if(bitmap!=null) {
                width = bitmap.getWidth();
                height = bitmap.getHeight();
            }
        }
    }

    private class HandleTouchEvent{
        private int x;
        private int y;
        private int yDelta;
        private int xDelta;
        private Handle handle;

        HandleTouchEvent(Handle handle){
            this.handle = handle;
        }

        boolean onTouchHandle(MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    handle.isMoving = handle.contains(event.getX(), event.getY());
                    if (handle.isMoving) {
                        yDelta = (int) (event.getY() - handle.y + 1);
                        xDelta = (int) (event.getX() - handle.x + handle.correctX);
                        selectableViewGroup.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    handle.isMoving = false;
                    selectableViewGroup.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (handle.isMoving) {
                        x = (int) (event.getRawX() - xDelta);
                        y = (int) (event.getRawY() - yDelta);
                        int oldHandlePos = handle.position;
                        handle.position = getCursorPosition(x, y, handle.position);

                        if (handle.position != oldHandlePos) {
                            setHandleCoordinate(handle);
                            setSelectionText();
                            checkBackground();
                            selectableViewGroup.invalidate();
                        }
                    }
                    break;
            }
            return handle.isMoving;
        }
    }

    static int[] location = new int[2];
    @MainThread
    public static Rect getViewScreenRect(@NonNull View view, @Nullable Rect rect) {
        if (D) {
            if (Thread.currentThread().getId() != Looper.getMainLooper().getThread().getId()) {
                throw new RuntimeException("This method is expected to be called on the main thread only");
            }
        }

        if (rect == null) {
            rect = new Rect();
        }

        float scaleX = 1f;
        float scaleY = 1f;

        View parent = view;
        while (parent != null) {
            scaleX *= parent.getScaleX();
            scaleY *= parent.getScaleY();

            if (parent.getParent() instanceof View) {
                parent = (View) parent.getParent();
            } else {
                parent = null;
            }
        }

        view.getLocationOnScreen(location);
        rect.set(
                location[0],
                location[1],
                Math.round(location[0] + view.getWidth() * scaleX),
                Math.round(location[1] + view.getHeight() * scaleY)
        );

        return rect;
    }


    public interface SelectionEnableListener {
        boolean isEnabled();
    }

}
